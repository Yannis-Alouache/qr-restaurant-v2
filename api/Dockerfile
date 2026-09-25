# Image API QR Restaurant : build Maven puis runtime JRE minimal.
# Construction : docker build -t qr-restaurant-api ./api (depuis la racine du repo)

# --- Étape de build -----------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache des dépendances : le pom seul suffit à cette couche.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

# --- Étape d'exécution --------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

# curl : requis par le HEALTHCHECK
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --system --no-create-home api
USER api

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

# Le healthcheck consomme l'endpoint actuator (permitAll, cf. SecurityConfig).
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -sf http://localhost:8080/actuator/health | grep -q '"status":"UP"'

ENTRYPOINT ["java", "-jar", "app.jar"]
