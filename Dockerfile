# Basis-Image mit Eclipse Temurin (OpenJDK 21)
FROM eclipse-temurin:21-jre-alpine

# Arbeitsverzeichnis im Container
WORKDIR /app

# Kopiere die JAR-Datei aus dem lokalen Build
COPY target/*.jar app.jar

# Port freigeben (Spring Boot Standard)
EXPOSE 8081

# Startbefehl
ENTRYPOINT ["java", "-jar", "app.jar"]