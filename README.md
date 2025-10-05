# Snippet Service 2025 - User Story #1

## Descripción
Servicio para gestionar snippets de código desarrollado con Spring Boot y Kotlin.

## Ejecutar el Proyecto

### Con Docker
```bash
docker-compose up -d --build
```

### Desarrollo Local
```bash
./gradlew bootRun
```

### Acceder a Swagger
Una vez ejecutándose: http://localhost:8080/swagger-ui.html

## Tecnologías

- **Spring Boot 3.5.6** - Framework
- **Kotlin 1.9.25** - Lenguaje  
- **Spring Data JPA** - Persistencia
- **PostgreSQL** - Base de datos
- **Swagger/OpenAPI** - Documentación
- **ktlint** - Linting automático
- **Multipart File Upload** - Subida de archivos