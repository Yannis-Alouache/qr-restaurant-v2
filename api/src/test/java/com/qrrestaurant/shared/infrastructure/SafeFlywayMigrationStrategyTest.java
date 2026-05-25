package com.qrrestaurant.shared.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SafeFlywayMigrationStrategyTest {

    private static final String DEMO_OWNER_PASSWORD_HASH = "$2b$10$x5Gp0EfduzLpOIOxh2QfKewhZuj7bAEGjcQSuQCMk9DqtQQKqVPCa";

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    private final DataSource dataSource = new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(),
            POSTGRES.getUsername(),
            POSTGRES.getPassword()
    );

    private final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

    @BeforeEach
    void resetSchema() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS public CASCADE");
            statement.execute("CREATE SCHEMA public");
            statement.execute("GRANT ALL ON SCHEMA public TO " + POSTGRES.getUsername());
            statement.execute("GRANT ALL ON SCHEMA public TO public");
        }
    }

    @Test
    void shouldMigrateAnEmptyDatabaseWithoutCreatingABaselineMarker() {
        Flyway flyway = newFlyway();

        new SafeFlywayMigrationStrategy(dataSource).migrate(flyway);

        assertEquals("6", flyway.info().current().getVersion().getVersion());
        assertEquals("SQL", jdbcTemplate.queryForObject(
                "SELECT type FROM flyway_schema_history ORDER BY installed_rank LIMIT 1",
                String.class));
    }

    @Test
    void shouldBaselineACompleteLegacySchemaAndThenApplyTheMissingRepairMigrations() {
        executeMigrationScript("db/migration/V1__create_initial_schema.sql");
        executeMigrationScript("db/migration/V2__seed_test_data.sql");

        Flyway flyway = newFlyway();

        new SafeFlywayMigrationStrategy(dataSource).migrate(flyway);

        assertEquals("6", flyway.info().current().getVersion().getVersion());
        assertEquals("BASELINE", jdbcTemplate.queryForObject(
                "SELECT type FROM flyway_schema_history ORDER BY installed_rank LIMIT 1",
                String.class));
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'restaurant'
                  AND constraint_name = 'uk_restaurant_user_id'
                """, Integer.class));
        assertEquals(DEMO_OWNER_PASSWORD_HASH, jdbcTemplate.queryForObject("""
                SELECT password
                FROM app_user
                WHERE email = 'owner@test.com'
                """, String.class));
    }

    @Test
    void shouldRejectABrokenLegacySchemaInsteadOfSilentlyBaseliningIt() {
        jdbcTemplate.execute("""
                CREATE TABLE app_user (
                    id UUID PRIMARY KEY,
                    email TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);

        Flyway flyway = newFlyway();

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> new SafeFlywayMigrationStrategy(dataSource).migrate(flyway));

        assertEquals(
                "Schéma existant partiellement initialisé sans historique Flyway. Corrigez la base manuellement avant de relancer l'application.",
                thrown.getMessage());
    }

    @Test
    void shouldRepairTheKnownLegacySeedChecksumBeforeMigratingForward() throws IOException {
        Path legacyMigrations = Files.createTempDirectory("legacy-migrations");
        copyMigration(legacyMigrations, "V1__create_initial_schema.sql");
        copyMigration(legacyMigrations, "V2__seed_test_data.sql");
        Files.writeString(
                legacyMigrations.resolve("V2__seed_test_data.sql"),
                Files.readString(legacyMigrations.resolve("V2__seed_test_data.sql")) + "\n-- legacy checksum drift\n"
        );

        Flyway legacyFlyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("filesystem:" + legacyMigrations)
                .baselineVersion("4")
                .baselineDescription("legacy-safe-baseline")
                .load();
        legacyFlyway.migrate();

        Integer checksumBeforeRepair = jdbcTemplate.queryForObject("""
                SELECT checksum
                FROM flyway_schema_history
                WHERE version = '2'
                """, Integer.class);

        new SafeFlywayMigrationStrategy(dataSource).migrate(newFlyway());

        Integer checksumAfterRepair = jdbcTemplate.queryForObject("""
                SELECT checksum
                FROM flyway_schema_history
                WHERE version = '2'
                """, Integer.class);

        assertEquals("6", newFlyway().info().current().getVersion().getVersion());
        org.junit.jupiter.api.Assertions.assertNotEquals(checksumBeforeRepair, checksumAfterRepair);
        deleteDirectory(legacyMigrations);
    }

    private Flyway newFlyway() {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineVersion("4")
                .baselineDescription("legacy-safe-baseline")
                .load();
    }

    private void executeMigrationScript(String path) {
        new ResourceDatabasePopulator(new ClassPathResource(path)).execute(dataSource);
    }

    private void copyMigration(Path targetDirectory, String fileName) throws IOException {
        Files.writeString(
                targetDirectory.resolve(fileName),
                new ClassPathResource("db/migration/" + fileName).getContentAsString(java.nio.charset.StandardCharsets.UTF_8)
        );
    }

    private void deleteDirectory(Path directory) throws IOException {
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });
        }
    }
}
