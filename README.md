# Snippet Service 2025

## Description
Service for managing code snippets developed with Spring Boot and Kotlin. Provides functionality for creating, reading, updating, and deleting code snippets with syntax validation and permission management.

## Features

- **Snippet Management**: Create, read, update, and delete code snippets
- **File Upload**: Upload snippets via multipart file upload
- **JSON-based Operations**: Create and update snippets directly from editor (JSON)
- **Syntax Validation**: Validate snippet syntax before saving
- **Permission Integration**: Integrates with Permission Service for access control
- **Language Support**: Supports multiple programming languages (PrintScript, etc.)
- **Pagination**: List snippets with pagination support

## Running the Project

### With Docker
```bash
docker-compose up -d --build
```

### Local Development
```bash
./gradlew bootRun
```

### Access Swagger
Once running: http://localhost:8080/swagger-ui.html

## Main Endpoints

- `GET /api/snippets` - List all snippets (with pagination)
- `GET /api/snippets/{id}` - Get snippet by ID
- `POST /api/snippets` - Create snippet (multipart file upload)
- `POST /api/snippets` (JSON) - Create snippet from editor
- `PUT /api/snippets/{id}` - Update snippet (multipart file upload)
- `PUT /api/snippets/{id}` (JSON) - Update snippet from editor
- `DELETE /api/snippets/{id}` - Delete snippet

## Technologies

- **Spring Boot 3.5.6** - Framework
- **Kotlin 1.9.25** - Language
- **Spring Data JPA** - Persistence
- **PostgreSQL** - Database
- **Swagger/OpenAPI** - Documentation
- **ktlint** - Automatic linting
- **Multipart File Upload** - File upload support
- **JWT Authentication** - Token-based authentication (Auth0)

## Configuration

The service runs on port **8080** by default.

Environment variables (configured in `docker-compose.yml`):
- `DB_HOST`: PostgreSQL host
- `DB_PORT`: PostgreSQL port
- `DB_NAME`: Database name
- `DB_USER`: PostgreSQL user
- `DB_PASSWORD`: PostgreSQL password
- `PERMISSION_URL`: Permission Service URL (for permission checks)
- `PRINTSCRIPT_URL`: PrintScript Service URL (for syntax validation)

## Integration

### Permission Service
The service integrates with Permission Service to check write permissions before allowing snippet updates. The integration endpoint:
- `GET /api/permissions/write-check?snippetId={id}&userId={userId}`

### PrintScript Service
The service can integrate with PrintScript Service for syntax validation (currently commented out, ready for activation).

## Docker Build

The Dockerfile uses a multi-stage build:
1. **Builder stage**: Builds the JAR using Gradle
2. **Runtime stage**: Creates a lightweight runtime image with the JAR

This ensures the service builds correctly in CI/CD environments where pre-built JARs are not available.

## Development Notes

- Authentication is temporarily disabled for local testing (can be enabled in `OAuth2ResourceServerSecurityConfiguration.kt`)
- CORS is configured to allow requests from `localhost:5173`, `5174`, and `3000`
- The service includes comprehensive error handling with detailed error messages
- Language enum conversion is case-insensitive (handles both uppercase and lowercase)
