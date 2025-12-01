# Multi-stage build: Build JAR first, then create runtime image
FROM gradle:8.5-jdk21 AS builder
WORKDIR /home/gradle/project

# Copy wrapper & build files first for cache
COPY --chown=gradle:gradle gradlew ./
COPY --chown=gradle:gradle gradle gradle
COPY --chown=gradle:gradle settings.gradle* build.gradle* ./
RUN chmod +x ./gradlew && ./gradlew --no-daemon --version

# Copy sources and build
COPY --chown=gradle:gradle src src
RUN ./gradlew --no-daemon -x test bootJar

# Runtime stage
FROM eclipse-temurin:21-jre

RUN apt-get update && apt-get install -y --no-install-recommends postgresql-client \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd -r spring && useradd -r -g spring spring

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

# Copy New Relic agent and config
COPY --chown=spring:spring newrelic/newrelic.jar /newrelic/newrelic.jar
COPY --chown=spring:spring newrelic/newrelic.yml /newrelic/newrelic.yml

USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
