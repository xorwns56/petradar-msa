# Build Stage
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
COPY gradlew ./
COPY gradle ./gradle/
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# Runtime Stage
FROM eclipse-temurin:17-jre-jammy
RUN groupadd -g 1001 appgroup && useradd -u 1001 -g appgroup --no-create-home appuser
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
RUN chown -R appuser:appgroup /app
USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
