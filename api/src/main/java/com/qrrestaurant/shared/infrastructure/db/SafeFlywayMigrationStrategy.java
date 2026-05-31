package com.qrrestaurant.shared.infrastructure.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.ValidateOutput;
import org.flywaydb.core.api.output.ValidateResult;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Set;

@Component
public class SafeFlywayMigrationStrategy implements FlywayMigrationStrategy {

    private static final Set<String> MANAGED_TABLES = Set.of(
            "app_user",
            "restaurant",
            "restaurant_table",
            "category",
            "menu_item",
            "menu_composition",
            "order_table",
            "order_item"
    );

    private final JdbcTemplate jdbcTemplate;

    public SafeFlywayMigrationStrategy(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void migrate(Flyway flyway) {
        if (hasTable("flyway_schema_history")) {
            ValidateResult validationResult = flyway.validateWithResult();
            if (!validationResult.validationSuccessful) {
                if (shouldRepairLegacySeedChecksum(validationResult)) {
                    flyway.repair();
                } else {
                    flyway.validate();
                }
            }
            flyway.migrate();
            return;
        }

        Set<String> existingManagedTables = Set.copyOf(findExistingManagedTables());
        if (existingManagedTables.isEmpty()) {
            flyway.migrate();
            return;
        }

        if (existingManagedTables.containsAll(MANAGED_TABLES)) {
            flyway.baseline();
            flyway.migrate();
            return;
        }

        throw new IllegalStateException(
                "Schéma existant partiellement initialisé sans historique Flyway. Corrigez la base manuellement avant de relancer l'application.");
    }

    private boolean hasTable(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = current_schema()
                  AND table_name = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private List<String> findExistingManagedTables() {
        return jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = current_schema()
                  AND table_name IN ('app_user', 'restaurant', 'restaurant_table', 'category',
                                     'menu_item', 'menu_composition', 'order_table', 'order_item')
                """, String.class);
    }

    private boolean shouldRepairLegacySeedChecksum(ValidateResult validationResult) {
        return !validationResult.invalidMigrations.isEmpty()
                && validationResult.invalidMigrations.stream().anyMatch(this::isLegacySeedMigration)
                && validationResult.invalidMigrations.stream().allMatch(
                invalidMigration -> isLegacySeedMigration(invalidMigration) || isPendingMigration(invalidMigration));
    }

    private boolean isLegacySeedMigration(ValidateOutput invalidMigration) {
        return "2".equals(invalidMigration.version)
                && "seed test data".equalsIgnoreCase(invalidMigration.description);
    }

    private boolean isPendingMigration(ValidateOutput invalidMigration) {
        return invalidMigration.errorDetails != null
                && invalidMigration.errorDetails.errorMessage != null
                && invalidMigration.errorDetails.errorMessage.contains("not applied to database");
    }
}
