# Multi-stage build para optimizar el tamaño de la imagen final
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app
COPY gradle gradle
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .

# Descargar dependencias primero (para aprovechar cache de Docker)
RUN gradle dependencies --no-daemon

# Copiar código fuente y compilar
COPY src src
RUN gradle bootJar --no-daemon

# Imagen final más liviana
FROM openjdk:21-jre-slim

RUN apt-get update && apt-get install -y postgresql-client && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copiar el JAR compilado desde la etapa anterior
COPY --from=builder /app/build/libs/snippet-service-2025-0.0.1-SNAPSHOT.jar app.jar

# Crear usuario no-root por seguridad
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
