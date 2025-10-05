# 1. Construir JAR con Gradle
FROM gradle:8.5-jdk21 AS builder
WORKDIR /home/gradle/project

# Copiar archivos de configuración de Gradle
COPY build.gradle settings.gradle gradle/ ./
RUN gradle dependencies --no-daemon

# Copiar el código fuente y construir el JAR
COPY src src
RUN gradle bootJar --no-daemon

# 2. Imagen final ligera
FROM openjdk:21-slim

# Instalar cliente de PostgreSQL y limpiar cache de APT
RUN apt-get update && apt-get install -y --no-install-recommends postgresql-client \
    && rm -rf /var/lib/apt/lists/*

# Crear un usuario no root para seguridad
RUN groupadd -r spring && useradd -r -g spring spring

# Establecer directorio de trabajo
WORKDIR /app

# Copiar el JAR generado desde la etapa anterior
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

# Cambiar al usuario sin privilegios
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
