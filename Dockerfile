# Multi-stage build per ottimizzazione immagine Docker e supporto Test
# Questo Dockerfile definisce 4 stage:
# 1. base:    Setup delle dipendenze (cacheable)
# 2. test:    Esecuzione della test suite
# 3. build:   Compilazione del JAR finale
# 4. runtime: Immagine leggera per l'esecuzione

# ==========================================
# STAGE 1: BASE (Dipendenze)
# ==========================================
FROM eclipse-temurin:17-jdk AS base
WORKDIR /app

# Copia wrapper Maven e file di configurazione
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Rendi eseguibile il wrapper Maven
RUN chmod +x mvnw

# Scarica le dipendenze (questo layer verrà cachato se pom.xml non cambia)
# Con BuildKit mount cache, le dipendenze vengono cachate tra i build
RUN --mount=type=cache,target=/root/.m2 ./mvnw dependency:go-offline -B

# Copia il codice sorgente
COPY src ./src

# ==========================================
# STAGE 2: TEST (Esecuzione Test Suite)
# ==========================================
FROM base AS test
# Esegue i test e genera il report di coverage
# Se i test falliscono, il build Docker si ferma qui
RUN --mount=type=cache,target=/root/.m2 ./mvnw test jacoco:report

# ==========================================
# STAGE 3: BUILD (Compilazione JAR)
# ==========================================
FROM base AS build
# Compila l'applicazione saltando i test (già eseguiti nello stage precedente)
RUN --mount=type=cache,target=/root/.m2 ./mvnw clean package -DskipTests

# ==========================================
# STAGE 4: RUNTIME (Immagine Finale)
# ==========================================
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Crea un utente non-root per sicurezza
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copia il JAR compilato dallo stage di build
COPY --from=build /app/target/padel-app-0.0.1-SNAPSHOT.jar app.jar

# Esponi porta 5000 (configurata in application.properties)
EXPOSE 5000

# Health check per verificare stato applicazione
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD wget -qO- http://localhost:5000/actuator/health || exit 1

# Avvio applicazione
ENTRYPOINT ["java", "-jar", "app.jar"]
