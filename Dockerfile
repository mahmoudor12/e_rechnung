# ========================================
# STAGE 1: Build mit Maven
# ========================================
FROM maven:3.9-eclipse-temurin-21 AS build

# Arbeitsverzeichnis
WORKDIR /app

# 1. Nur pom.xml kopieren (für Dependency-Caching)
COPY pom.xml .
RUN mvn dependency:go-offline

# 2. Quellcode kopieren und bauen
COPY src ./src
RUN mvn clean package -DskipTests

# ========================================
# STAGE 2: Runtime-Image (klein)
# ========================================
FROM eclipse-temurin:21-jre-alpine

# Arbeitsverzeichnis
WORKDIR /app

# JAR aus der Build-Stufe kopieren
COPY --from=build /app/target/*.jar app.jar

# Port freigeben
EXPOSE 8081

# Startbefehl
ENTRYPOINT ["java", "-jar", "app.jar"]