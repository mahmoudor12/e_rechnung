# Basis-Image mit Java 21
FROM openjdk:21-slim

# Arbeitsverzeichnis im Container
WORKDIR /app

# Kopiere die JAR-Datei aus dem Maven-Build
COPY target/*.jar app.jar

# Port freigeben (Spring Boot Standard)
EXPOSE 8081

# Startbefehl
ENTRYPOINT ["java", "-jar", "app.jar"]