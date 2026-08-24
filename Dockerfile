# ========================================
# STAGE 1: Build mit Maven
# ========================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# ========================================
# STAGE 2: Runtime-Image (klein)
# ========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# 🔥 Heap-Speicher für KoSIT erhöhen
ENV JAVA_OPTS="-Xmx512m"

EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]