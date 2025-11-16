# Multi-stage build per ottimizzazione immagine Docker

# STAGE 1: BUILD
# Usa Java 17 JDK per compilare l'applicazione (allineato con pom.xml)
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copia solo i file Maven necessari (layer caching per dipendenze)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Download dipendenze Maven (layer cacheable - non cambia spesso)
RUN ./mvnw dependency:go-offline -B

# Copia il codice sorgente
COPY src ./src

# Build dell'applicazione (skip test per velocità build Docker)
RUN ./mvnw clean package -DskipTests

# STAGE 2: RUNTIME
# Usa Java 17 JRE Alpine per immagine finale leggera (~150MB vs ~500MB)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copia solo il JAR compilato dallo stage di build
COPY --from=build /app/target/padel-app-0.0.1-SNAPSHOT.jar app.jar

# Esponi porta 5000 (configurata in application.properties)
EXPOSE 5000

# Health check per verificare stato applicazione (BusyBox-compatible)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD wget -qO- http://localhost:5000/actuator/health || exit 1

# Avvio applicazione con JVM ottimizzata per container
ENTRYPOINT ["java", "-jar", "app.jar"]
