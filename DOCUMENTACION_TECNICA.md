# Documentación Técnica - Snippet Service 2025

## Tabla de Contenidos
1. [Descripción General](#descripción-general)
2. [Arquitectura y Tecnologías](#arquitectura-y-tecnologías)
3. [Modelo de Capas](#modelo-de-capas)
4. [Base de Datos](#base-de-datos)
5. [Modelos y Entidades](#modelos-y-entidades)
6. [Servicios Externos y Conectores](#servicios-externos-y-conectores)
7. [Endpoints y Flujos](#endpoints-y-flujos)
8. [Autenticación y Seguridad](#autenticación-y-seguridad)
9. [Anotaciones Importantes](#anotaciones-importantes)
10. [Configuración y Variables de Entorno](#configuración-y-variables-de-entorno)
11. [Ejecución del Proyecto](#ejecución-del-proyecto)

---

## Descripción General

**Snippet Service 2025** es un microservicio para la gestión de snippets de código desarrollado con **Spring Boot** y **Kotlin**. Permite a los usuarios crear, leer, actualizar y eliminar fragmentos de código con validación de sintaxis, análisis estático, formateo automático, y gestión de permisos.

### Características Principales
- ✅ CRUD completo de snippets
- ✅ Validación de sintaxis mediante servicio externo
- ✅ Formateo y linting automático
- ✅ Sistema de tests para snippets
- ✅ Gestión de permisos (owner, reader)
- ✅ Compartir snippets entre usuarios
- ✅ Almacenamiento de contenido en servicio de assets
- ✅ Autenticación JWT con Auth0
- ✅ Monitoreo con New Relic
- ✅ Documentación automática con Swagger/OpenAPI

---

## Arquitectura y Tecnologías

### Stack Tecnológico

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **Kotlin** | 1.9.25 | Lenguaje de programación principal |
| **Spring Boot** | 3.5.6 | Framework backend |
| **Spring Data JPA** | 3.5.6 | Capa de persistencia ORM |
| **PostgreSQL** | 17 | Base de datos relacional |
| **Spring Security** | 3.5.6 | Autenticación y autorización |
| **OAuth2 Resource Server** | 3.5.6 | Validación de tokens JWT |
| **Auth0** | - | Proveedor de identidad (IdP) |
| **WebFlux/WebClient** | 3.5.6 | Cliente HTTP reactivo para servicios externos |
| **Swagger/OpenAPI** | 2.2.0 | Documentación interactiva de API |
| **Gradle** | 8.x | Gestión de dependencias y build |
| **Docker** | 3.9 | Containerización |
| **ktlint** | 0.50.0 | Linter y formateador de código Kotlin |
| **JaCoCo** | 0.8.11 | Cobertura de código |
| **New Relic** | - | Observabilidad y monitoreo |

### Servicios Externos Integrados

1. **Permission Service**: Gestión de permisos y roles sobre snippets
2. **PrintScript Service**: Validación, formateo, linting y testing de código
3. **Asset Service**: Almacenamiento del contenido de los snippets
4. **Auth0**: Autenticación y gestión de usuarios

---

## Modelo de Capas

El proyecto sigue una **arquitectura en capas** (Layered Architecture) siguiendo las mejores prácticas de Spring Boot:

```
┌─────────────────────────────────────────┐
│         CONTROLLERS (API Layer)         │  ← Manejo de peticiones HTTP
├─────────────────────────────────────────┤
│         SERVICES (Business Logic)       │  ← Lógica de negocio
├─────────────────────────────────────────┤
│         CONNECTORS (External APIs)      │  ← Comunicación con servicios externos
├─────────────────────────────────────────┤
│      REPOSITORIES (Data Access)         │  ← Acceso a base de datos
├─────────────────────────────────────────┤
│         ENTITIES (Domain Models)        │  ← Modelos de dominio (JPA)
└─────────────────────────────────────────┘
```

### Descripción de Capas

#### 1. **Controllers** (`controller/`)
- Exponen endpoints REST
- Validan datos de entrada
- Manejan autenticación JWT
- Transforman DTOs a respuestas HTTP
- **Anotaciones clave**: `@RestController`, `@RequestMapping`, `@PostMapping`, `@GetMapping`, `@PutMapping`, `@DeleteMapping`

#### 2. **Services** (`service/`)
- Contienen la lógica de negocio
- Orquestan llamadas a repositorios y conectores
- Manejan transacciones
- **Anotaciones clave**: `@Service`, `@Transactional`

#### 3. **Connectors** (`connector/`)
- Clientes para servicios externos (HTTP)
- Usan `WebClient` (reactivo) para llamadas asíncronas
- Manejan errores de comunicación
- **Anotaciones clave**: `@Service`

#### 4. **Repositories** (`repository/`)
- Acceso a base de datos mediante JPA
- Consultas automáticas y personalizadas
- **Anotaciones clave**: `@Repository`, hereda de `JpaRepository`

#### 5. **Entities** (`model/entity/`)
- Mapeo objeto-relacional (ORM)
- Representan tablas de base de datos
- **Anotaciones clave**: `@Entity`, `@Table`, `@Id`, `@Column`

#### 6. **DTOs** (`model/dto/`)
- Transferencia de datos entre capas
- Validaciones con Bean Validation
- **Anotaciones clave**: `@NotBlank`, `@NotNull`, `@Valid`

---

## Base de Datos

### Motor: PostgreSQL 17

### Esquema de Tablas

#### Tabla `snippets`

| Columna | Tipo | Restricciones | Descripción |
|---------|------|---------------|-------------|
| `id` | UUID (String) | PRIMARY KEY | Identificador único generado automáticamente |
| `name` | VARCHAR | NOT NULL | Nombre del snippet |
| `description` | TEXT | - | Descripción del snippet |
| `language` | VARCHAR (ENUM) | NOT NULL | Lenguaje de programación (PRINTSCRIPT) |
| `user_id` | VARCHAR | NOT NULL | ID del usuario propietario |
| `version` | VARCHAR | NOT NULL | Versión del lenguaje (ej: "1.0", "1.1") |
| `created_at` | TIMESTAMP | NOT NULL | Fecha de creación |
| `updated_at` | TIMESTAMP | NOT NULL | Última actualización |

**Índices**: 
- PRIMARY KEY en `id`
- Índice compuesto en (`user_id`, `name`) para búsquedas rápidas

**Nota importante**: El **contenido del snippet NO se almacena en la base de datos**. Se delega al **Asset Service** para almacenamiento escalable.

#### Tabla `snippet_tests`

| Columna | Tipo | Restricciones | Descripción |
|---------|------|---------------|-------------|
| `id` | UUID (String) | PRIMARY KEY | Identificador único del test |
| `snippet_id` | UUID (String) | NOT NULL, FOREIGN KEY | Referencia al snippet |
| `name` | VARCHAR | NOT NULL | Nombre descriptivo del test |
| `input` | TEXT | - | Inputs del test (array JSON) |
| `expected_output` | TEXT | - | Outputs esperados (array JSON) |
| `created_at` | TIMESTAMP | NOT NULL | Fecha de creación |

**Relaciones**:
- `snippet_id` → `snippets.id` (Many-to-One)

---

## Modelos y Entidades

### Entidad Principal: `Snippet`

```kotlin
@Entity
@Table(name = "snippets")
data class Snippet(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",
    
    @Column(nullable = false)
    var name: String,
    
    @Column(columnDefinition = "TEXT")
    var description: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var language: SnippetLanguage,
    
    @Column(name = "user_id", nullable = false)
    var userId: String,
    
    @Column(nullable = false)
    var version: String,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
```

### DTOs Principales

#### CreateSnippetDTO (Desde Editor)
```kotlin
data class CreateSnippetDTO(
    val name: String,              // Nombre del snippet
    val description: String,        // Descripción
    val language: SnippetLanguage,  // PRINTSCRIPT
    val content: String,            // Código fuente
    val version: String             // Versión del lenguaje
)
```

#### CreateSnippetFileDTO (Desde Archivo)
```kotlin
data class CreateSnippetFileDTO(
    val name: String,
    val description: String,
    val language: SnippetLanguage,
    val file: MultipartFile,        // Archivo subido
    val version: String
)
```

#### SnippetResponseDTO
```kotlin
data class SnippetResponseDTO(
    val id: String,
    val name: String,
    val description: String,
    val language: SnippetLanguage,
    val content: String,            // Obtenido desde Asset Service
    val userId: String,
    val version: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val compliance: String          // Estado de cumplimiento
)
```

### Enumeraciones

#### SnippetLanguage
```kotlin
enum class SnippetLanguage {
    PRINTSCRIPT
}
```

---

## Servicios Externos y Conectores

### Patrón de Conectores

Los **Conectores** son clases especializadas que encapsulan la comunicación con servicios externos usando **WebClient** (cliente HTTP reactivo de Spring WebFlux).

#### Ventajas:
- ✅ Separación de responsabilidades
- ✅ Fácil testing con mocks
- ✅ Manejo centralizado de errores
- ✅ Reutilización de lógica de comunicación

### 1. AssetServiceConnector

**Propósito**: Almacenar y recuperar el contenido de los snippets.

**Métodos principales**:
```kotlin
fun storeSnippet(snippetId: String, content: String): Boolean
fun getSnippet(snippetId: String): String?
fun deleteSnippet(snippetId: String): Boolean
```

**Flujo**:
1. Construye URL dinámica resolviendo nombres de host con DNS
2. Envía contenido con Bearer token (JWT)
3. Maneja errores de conectividad

**URL configurada**: `${ASSET_URL}` (ej: `http://asset-service:8080`)

---

### 2. PermissionServiceConnector

**Propósito**: Gestionar permisos de acceso a snippets.

**Métodos principales**:
```kotlin
fun createPermission(snippetId: String, userId: String, role: String): PermissionResponse?
fun checkPermission(snippetId: String, userId: String): PermissionCheckResponse
fun hasWritePermission(snippetId: String, userId: String): Boolean
```

**Roles soportados**:
- `OWNER`: Creador del snippet (lectura + escritura + compartir)
- `READER`: Usuario con quien se compartió (solo lectura)

**Endpoints consumidos**:
- `POST /api/permissions` - Crear permiso
- `GET /api/permissions/check?snippetId=X&userId=Y` - Verificar permiso
- `GET /api/permissions/write-check?snippetId=X&userId=Y` - Verificar escritura

---

### 3. PrintScriptServiceConnector

**Propósito**: Validación sintáctica, formateo, linting y testing de código PrintScript.

**Métodos principales**:
```kotlin
fun validateSnippet(content: String, language: String, version: String): ValidationResponse
fun formatSnippet(userId: String, content: String): String
fun lintSnippet(userId: String, content: String): List<SCAOutput>
fun testSnippet(userId: String, content: String, inputs: List<String>, outputs: List<String>): String
fun triggerAutomaticFormatting(snippetId: String, userId: String, content: String)
fun triggerAutomaticLinting(snippetId: String, userId: String, content: String)
```

**Endpoints consumidos**:
- `POST /validate` - Validar sintaxis
- `POST /format` - Formatear código
- `POST /analyze` - Analizar con linter
- `POST /test` - Ejecutar test
- `POST /runner` - Ejecutar código

---

### 4. Auth0Connector

**Propósito**: Obtener información de usuarios desde Auth0.

**Métodos principales**:
```kotlin
fun getUsers(search: String?): List<Auth0UserDTO>
```

**Autenticación**: Management API Token (machine-to-machine)

---

## Endpoints y Flujos

### 1. Snippet Controller (`/api/snippets`)

#### **POST /api/snippets** (multipart/form-data)
**Crear snippet desde archivo**

**Request**:
```
Content-Type: multipart/form-data

name: "MiSnippet"
description: "Descripción"
language: "PRINTSCRIPT"
version: "1.1"
file: [archivo.ps]
```

**Flujo**:
1. Extrae `userId` del JWT
2. Valida que no exista snippet con mismo nombre para ese usuario
3. Lee contenido del archivo
4. **Valida sintaxis** con PrintScript Service
5. Guarda entidad `Snippet` en BD (sin content)
6. **Almacena contenido** en Asset Service
7. **Crea permiso OWNER** en Permission Service
8. Dispara formateo/linting/testing automático (opcional)
9. Retorna `SnippetResponseDTO`

**Respuesta** (201 Created):
```json
{
  "id": "uuid-123",
  "name": "MiSnippet",
  "description": "Descripción",
  "language": "PRINTSCRIPT",
  "content": "let x: number = 5;",
  "userId": "auth0|abc123",
  "version": "1.1",
  "createdAt": "2025-12-02T10:30:00",
  "updatedAt": "2025-12-02T10:30:00",
  "compliance": "compliant"
}
```

**Rollback**: Si falla la creación de permisos, se elimina el snippet de BD y Asset Service.

---

#### **POST /api/snippets** (application/json)
**Crear snippet desde editor**

**Request**:
```json
{
  "name": "HolaMundo",
  "description": "Primer snippet",
  "language": "PRINTSCRIPT",
  "content": "println(\"Hola Mundo\");",
  "version": "1.1"
}
```

**Flujo**: Idéntico al anterior, pero sin leer archivo.

---

#### **GET /api/snippets/{id}**
**Obtener snippet por ID**

**Flujo**:
1. Verifica permiso con Permission Service
2. Busca snippet en BD
3. Obtiene contenido desde Asset Service
4. Retorna `SnippetResponseDTO`

**Respuesta** (200 OK):
```json
{
  "id": "uuid-123",
  "name": "HolaMundo",
  "content": "println(\"Hola Mundo\");",
  ...
}
```

---

#### **GET /api/snippets?name={filtro}**
**Listar snippets del usuario**

**Query Params**:
- `name` (opcional): Filtro por nombre (búsqueda parcial, case-insensitive)

**Flujo**:
1. Busca snippets del usuario en BD
2. Filtra por nombre si se especifica
3. Para cada snippet, obtiene contenido desde Asset Service
4. Retorna lista de `SnippetResponseDTO`

**Respuesta** (200 OK):
```json
[
  {
    "id": "uuid-1",
    "name": "Snippet1",
    "content": "...",
    ...
  },
  {
    "id": "uuid-2",
    "name": "Snippet2",
    "content": "...",
    ...
  }
]
```

---

#### **PUT /api/snippets/{id}** (application/json)
**Actualizar snippet desde editor**

**Request**:
```json
{
  "name": "NuevoNombre",
  "description": "Nueva descripción",
  "content": "println(\"Actualizado\");"
}
```

**Flujo**:
1. Verifica permiso de **escritura** con Permission Service
2. Valida sintaxis del nuevo contenido
3. Actualiza entidad en BD
4. Actualiza contenido en Asset Service
5. Dispara formateo/linting/testing automático
6. Retorna `SnippetResponseDTO` actualizado

---

#### **DELETE /api/snippets/{id}**
**Eliminar snippet**

**Flujo**:
1. Verifica permiso de escritura
2. Elimina contenido de Asset Service
3. Elimina snippet de BD
4. (Los permisos se eliminan en Permission Service por cascada)

**Respuesta** (200 OK):
```json
{
  "success": true,
  "message": "Snippet eliminado exitosamente",
  "timestamp": "2025-12-02T11:00:00"
}
```

---

#### **GET /api/snippets/users?search={filtro}**
**Obtener usuarios disponibles para compartir**

**Flujo**:
1. Llama a Auth0Connector para obtener usuarios
2. Filtra por nombre/email si se especifica
3. Retorna lista de `Auth0UserDTO`

---

#### **POST /api/snippets/share**
**Compartir snippet con usuario**

**Request**:
```json
{
  "snippetId": "uuid-123",
  "targetUserId": "auth0|xyz789"
}
```

**Flujo**:
1. Verifica que el usuario actual sea **OWNER**
2. Crea permiso de **READER** en Permission Service para el usuario objetivo
3. Retorna confirmación

**Respuesta** (200 OK):
```json
{
  "snippetId": "uuid-123",
  "targetUserId": "auth0|xyz789",
  "role": "READER",
  "sharedAt": "2025-12-02T11:15:00"
}
```

---

### 2. Code Analysis Controller (`/format`, `/lint`, `/rules`)

#### **POST /format**
**Formatear snippet**

**Request**:
```json
{
  "snippetId": "uuid-123"
}
```

**Flujo**:
1. Verifica permiso sobre el snippet
2. Obtiene contenido desde Asset Service
3. Obtiene reglas de formateo del usuario
4. Llama a PrintScript Service para formatear
5. Retorna código formateado (NO lo guarda automáticamente)

**Respuesta** (200 OK):
```json
{
  "formattedContent": "let x: number = 5;\nprintln(x);"
}
```

---

#### **POST /lint**
**Analizar snippet con linter**

**Request**:
```json
{
  "snippetId": "uuid-123"
}
```

**Respuesta** (200 OK):
```json
{
  "issues": [
    {
      "rule": "identifier-format",
      "line": 3,
      "column": 5,
      "message": "Variable name should be in camelCase"
    }
  ]
}
```

---

#### **GET /rules/format**
**Obtener reglas de formateo del usuario**

**Respuesta** (200 OK):
```json
[
  {
    "name": "printLineBreakBeforePrintln",
    "isActive": true,
    "value": null
  },
  {
    "name": "indentSpaces",
    "isActive": true,
    "value": 4
  }
]
```

---

### 3. Snippet Test Controller (`/api/snippets/{snippetId}/tests`)

#### **POST /api/snippets/{snippetId}/tests**
**Crear test para snippet**

**Request**:
```json
{
  "name": "Test suma",
  "inputs": ["5", "10"],
  "expectedOutputs": ["15"]
}
```

**Flujo**:
1. Verifica permiso de escritura
2. Valida que no exista test con mismo nombre
3. Guarda test en BD
4. Retorna `TestResponseDTO`

---

#### **GET /api/snippets/{snippetId}/tests/{testId}**
**Obtener test específico**

---

#### **GET /api/snippets/{snippetId}/tests**
**Listar tests del snippet**

---

#### **DELETE /api/snippets/{snippetId}/tests/{testId}**
**Eliminar test**

---

### 4. Language Controller (`/api/languages`)

#### **GET /api/languages**
**Obtener lenguajes soportados**

**Respuesta** (200 OK):
```json
[
  {
    "id": "printscript",
    "name": "PrintScript",
    "extension": "ps",
    "description": "Lenguaje educativo PrintScript"
  }
]
```

---

## Autenticación y Seguridad

### OAuth2 + JWT (Auth0)

#### Configuración de Seguridad

```kotlin
@Configuration
@EnableWebSecurity
class OAuth2ResourceServerSecurityConfiguration {
    
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.authorizeHttpRequests {
            it
                .requestMatchers("/").permitAll()
                .requestMatchers("/api-docs/**", "/swagger-ui/**").permitAll()
                .anyRequest().authenticated()
        }
        .oauth2ResourceServer { it.jwt(withDefaults()) }
        .cors { it.configurationSource(corsConfigurationSource) }
        .csrf { it.disable() }
        return http.build()
    }
}
```

#### Flujo de Autenticación

1. **Frontend** obtiene token JWT de Auth0
2. **Frontend** envía token en header: `Authorization: Bearer <token>`
3. **Spring Security** valida token:
   - Verifica firma con clave pública de Auth0
   - Valida `issuer` (Auth0 domain)
   - Valida `audience` (API identifier)
   - Valida expiración
4. Extrae `subject` (userId) del token
5. Controller usa `@AuthenticationPrincipal Jwt jwt` para acceder al token

#### Extracción de Usuario

```kotlin
private fun getUserId(jwt: Jwt?): String {
    val userId = jwt?.subject ?: throw UnauthorizedException("Usuario no autenticado")
    if (userId == "test-user@example.com") {
        throw UnauthorizedException("Usuario mockeado no permitido")
    }
    return userId
}
```

---

### CORS (Cross-Origin Resource Sharing)

**Orígenes permitidos**:
- `http://localhost:5173` (Vite dev)
- `http://localhost:5174`
- `http://localhost:3000` (React dev)
- `http://snippet-prueba.duckdns.org`
- `https://snippet-prueba.duckdns.org`

**Métodos permitidos**: GET, POST, PUT, DELETE, OPTIONS, PATCH

**Credenciales**: Habilitadas (cookies, Authorization header)

---

## Anotaciones Importantes

### Spring Boot Core

| Anotación | Descripción | Capa |
|-----------|-------------|------|
| `@SpringBootApplication` | Punto de entrada de la aplicación | Main |
| `@Configuration` | Clase de configuración de beans | Config |
| `@Bean` | Declara un bean gestionado por Spring | Config |
| `@Value("${property}")` | Inyecta valor de configuración | Cualquiera |

### Capa Controller

| Anotación | Descripción |
|-----------|-------------|
| `@RestController` | Controlador REST (combina `@Controller` + `@ResponseBody`) |
| `@RequestMapping("/path")` | Mapea rutas base del controlador |
| `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` | Mapea métodos HTTP específicos |
| `@PathVariable` | Extrae variable de la URL (`/api/snippets/{id}`) |
| `@RequestParam` | Extrae query parameter (`?name=valor`) |
| `@RequestBody` | Deserializa JSON del body a objeto |
| `@ModelAttribute` | Maneja multipart/form-data |
| `@AuthenticationPrincipal` | Inyecta el usuario autenticado (JWT) |

### Capa Service

| Anotación | Descripción |
|-----------|-------------|
| `@Service` | Marca clase como servicio (lógica de negocio) |
| `@Transactional` | Gestiona transacciones de BD (rollback automático en excepciones) |

### Capa Repository

| Anotación | Descripción |
|-----------|-------------|
| `@Repository` | Marca clase como repositorio (acceso a datos) |
| Hereda de `JpaRepository<Entity, ID>` | Proporciona CRUD automático |

### Capa Entity

| Anotación | Descripción |
|-----------|-------------|
| `@Entity` | Marca clase como entidad JPA |
| `@Table(name = "tabla")` | Especifica nombre de tabla |
| `@Id` | Marca campo como clave primaria |
| `@GeneratedValue` | Genera valor automáticamente (UUID, AUTO_INCREMENT) |
| `@Column` | Configura columna (nullable, unique, length, etc.) |
| `@Enumerated(EnumType.STRING)` | Guarda enum como string en BD |
| `@PreUpdate` | Callback antes de actualizar entidad |

### Validaciones (Bean Validation)

| Anotación | Descripción |
|-----------|-------------|
| `@NotNull` | Campo no puede ser null |
| `@NotBlank` | String no puede ser null, vacío o solo espacios |
| `@Valid` | Activa validación recursiva en objeto |

### Swagger/OpenAPI

| Anotación | Descripción |
|-----------|-------------|
| `@Tag` | Agrupa endpoints en la documentación |
| `@Operation` | Describe operación (summary, description) |
| `@ApiResponses` | Documenta códigos de respuesta HTTP |
| `@Parameter` | Describe parámetro de entrada |

---

## Configuración y Variables de Entorno

### Archivo: `application.properties`

```properties
# Base de datos
spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update  # Crea/actualiza esquema automáticamente

# Servicios externos
permission.url=${PERMISSION_URL:http://localhost:8081}
printscript.url=${PRINTSCRIPT_URL:http://localhost:8082}
asset.url=${ASSET_URL:api:8080}

# Auth0
auth0.audience=${AUTH0_AUDIENCE:}
spring.security.oauth2.resourceserver.jwt.issuer-uri=${AUTH_SERVER_URI:}
```

### Variables de Entorno (Docker Compose)

| Variable | Ejemplo | Descripción |
|----------|---------|-------------|
| `DB_HOST` | `snippet-db-dev` | Host de PostgreSQL |
| `DB_PORT` | `5432` | Puerto de PostgreSQL |
| `DB_NAME` | `snippetdb` | Nombre de base de datos |
| `DB_USER` | `snippetuser` | Usuario de BD |
| `DB_PASSWORD` | `password123` | Contraseña de BD |
| `PERMISSION_URL` | `http://permission-api:8081` | URL del Permission Service |
| `PRINTSCRIPT_URL` | `http://printscript-api:8082` | URL del PrintScript Service |
| `ASSET_URL` | `http://asset-api:8080` | URL del Asset Service |
| `AUTH0_AUDIENCE` | `https://api.snippet-service.com` | Audience del token JWT |
| `AUTH_SERVER_URI` | `https://dev-xyz.auth0.com/` | Issuer de Auth0 |
| `API_PORT` | `8080` | Puerto expuesto del servicio |

---

## Ejecución del Proyecto

### Prerequisitos
- Docker Desktop
- Java 21 (para desarrollo local)
- PostgreSQL 17 (si se ejecuta sin Docker)

### Con Docker (Recomendado)

1. **Configurar variables de entorno**:
   Crear archivo `.env` en la raíz:
   ```bash
   ENVIRONMENT=dev
   API_PORT=8080
   DB_HOST=snippet-db-dev
   DB_PORT=5432
   DB_NAME=snippetdb
   DB_USER=snippetuser
   DB_PASSWORD=secretpass
   POSTGRES_USER=snippetuser
   POSTGRES_PASSWORD=secretpass
   POSTGRES_DB=snippetdb
   PERMISSION_URL=http://permission-api-dev:8080
   PRINTSCRIPT_URL=http://printscript-api-dev:8080
   ASSET_URL=http://asset-api-dev:8080
   AUTH0_AUDIENCE=your-audience
   AUTH_SERVER_URI=https://your-tenant.auth0.com/
   ```

2. **Levantar servicios**:
   ```bash
   docker-compose up -d --build
   ```

3. **Verificar logs**:
   ```bash
   docker logs -f snippet-api-dev
   ```

4. **Acceder a Swagger**:
   http://localhost:8080/swagger-ui.html

---

### Desarrollo Local (Sin Docker)

1. **Configurar base de datos PostgreSQL local**

2. **Exportar variables de entorno**:
   ```bash
   export DB_HOST=localhost
   export DB_PORT=5432
   export DB_NAME=snippetdb
   export DB_USER=postgres
   export DB_PASSWORD=postgres
   export PERMISSION_URL=http://localhost:8081
   export PRINTSCRIPT_URL=http://localhost:8082
   export ASSET_URL=http://localhost:8083
   ```

3. **Ejecutar aplicación**:
   ```bash
   ./gradlew bootRun
   ```

---

### Testing

```bash
# Ejecutar tests
./gradlew test

# Ver reporte de cobertura JaCoCo
./gradlew jacocoTestReport
# Resultado en: build/reports/jacoco/test/html/index.html

# Verificar cobertura mínima (80%)
./gradlew jacocoTestCoverageVerification

# Ejecutar linter
./gradlew ktlintCheck

# Auto-formatear código
./gradlew ktlintFormat
```

---

## Flujo Completo: Crear Snippet

### Diagrama de Secuencia

```
Usuario → Frontend → Snippet Service → PrintScript Service
                  ↓                  ↓
               Asset Service    Permission Service
```

### Pasos Detallados

1. **Usuario** escribe código en el editor del frontend
2. **Frontend** envía POST a `/api/snippets` con JWT
3. **Snippet Service**:
   - a. Valida JWT y extrae `userId`
   - b. Verifica que no exista snippet duplicado
   - c. Llama a **PrintScript Service** → `POST /validate` con el código
   - d. Si sintaxis es válida:
     - Guarda entidad `Snippet` en **PostgreSQL** (sin content)
     - Llama a **Asset Service** → `POST /snippet/{id}` con el contenido
     - Llama a **Permission Service** → `POST /api/permissions` con role OWNER
   - e. Si algún paso falla:
     - **Rollback**: Elimina snippet de BD y Asset Service
   - f. Dispara formateo/linting/testing automático en background
   - g. Retorna `SnippetResponseDTO` con status 201
4. **Frontend** muestra snippet creado

### Manejo de Errores

| Error | Código HTTP | Mensaje |
|-------|-------------|---------|
| Token inválido | 401 | "Usuario no autenticado" |
| Nombre duplicado | 400 | "Ya existe un snippet con ese nombre" |
| Sintaxis inválida | 400 | "Error de sintaxis: línea X, columna Y" |
| Asset Service down | 500 | "No se pudo almacenar el contenido" |
| Permission Service down | 500 | "No se pudo crear el permiso" |

---

## Observabilidad y Monitoreo

### New Relic

Configurado para monitorear:
- ✅ Tiempos de respuesta de endpoints
- ✅ Errores y excepciones
- ✅ Throughput (peticiones/segundo)
- ✅ Llamadas a base de datos
- ✅ Llamadas a servicios externos

**Activación**: Agente New Relic inyectado mediante `JAVA_TOOL_OPTIONS` en Docker.

### Logging

**Framework**: SLF4J + Logback

**Niveles**:
- `DEBUG`: Detalles de request/response bodies
- `INFO`: Operaciones importantes (crear snippet, actualizar, etc.)
- `WARN`: Situaciones anómalas recuperables
- `ERROR`: Errores críticos

**Configuración**: `src/main/resources/logback.xml`

---

## Glosario

| Término | Definición |
|---------|------------|
| **Snippet** | Fragmento de código fuente almacenado |
| **Asset Service** | Servicio externo para almacenar contenido de snippets |
| **Permission Service** | Servicio externo para gestionar permisos de acceso |
| **PrintScript** | Lenguaje de programación educativo soportado |
| **Compliance** | Estado de cumplimiento de reglas de formateo/linting |
| **JWT** | JSON Web Token usado para autenticación |
| **WebClient** | Cliente HTTP reactivo de Spring WebFlux |
| **DTO** | Data Transfer Object (objeto de transferencia de datos) |
| **JPA** | Java Persistence API (ORM) |
| **Bean Validation** | Framework de validación de datos (JSR-303) |

---

## Contacto y Soporte

Para preguntas o problemas, consultar:
- **README.md**: Guía rápida de inicio
- **RULES_API_EXAMPLES.md**: Ejemplos de reglas de formateo/linting
- **HOOKS.md**: Configuración de Git hooks
- **Swagger UI**: http://localhost:8080/swagger-ui.html (documentación interactiva)

---

**Última actualización**: 2025-12-02  
**Versión del servicio**: 0.0.1-SNAPSHOT

