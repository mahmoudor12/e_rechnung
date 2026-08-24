# ========================================
# STAGE 2: Runtime-Image
# ========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# 🔥 Heap-Speicher für KoSIT erhöhen
ENV JAVA_OPTS="-Xmx512m"

EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]