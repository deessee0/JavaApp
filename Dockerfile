FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copia il progetto
COPY . .

# Build con Maven wrapper o Maven installato nell'immagine
RUN ./mvnw clean package -DskipTests

# Secondo stage: immagine runtime più leggera
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/padel-app-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
