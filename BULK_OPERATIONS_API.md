# 📚 Documentación de Nuevos Endpoints - Operaciones Masivas

## 🎯 Resumen

Se han implementado 3 nuevos endpoints para operaciones masivas:

1. **POST** `/api/snippets/{snippetId}/tests/run-all` - Ejecutar todos los tests de un snippet
2. **POST** `/format/all` - Formatear todos los snippets del usuario (OWNER)
3. **POST** `/lint/all` - Lintear todos los snippets del usuario (OWNER)

---

## 1️⃣ Ejecutar Todos los Tests de un Snippet

### Endpoint
```
POST http://localhost:8080/api/snippets/{snippetId}/tests/run-all
```

### Headers
```
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

### URL Parameters
- `snippetId` (String, requerido): ID del snippet cuyos tests se van a ejecutar

### Request Body
```
No requiere body
```

### cURL Example
```bash
curl -X POST "http://localhost:8080/api/snippets/123e4567-e89b-12d3-a456-426614174000/tests/run-all" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json"
```

### Response (200 OK)
```json
{
  "snippetId": "123e4567-e89b-12d3-a456-426614174000",
  "totalTests": 5,
  "passedTests": 4,
  "failedTests": 1,
  "results": [
    {
      "testId": "test-001",
      "testName": "Test suma básica",
      "passed": true,
      "actualOutputs": ["5", "10"],
      "expectedOutputs": ["5", "10"],
      "errors": []
    },
    {
      "testId": "test-002",
      "testName": "Test división",
      "passed": false,
      "actualOutputs": ["2.5"],
      "expectedOutputs": ["2"],
      "errors": []
    },
    {
      "testId": "test-003",
      "testName": "Test con error",
      "passed": false,
      "actualOutputs": [],
      "expectedOutputs": ["resultado"],
      "errors": ["Error de ejecución: división por cero"]
    }
  ]
}
```

### Response Fields
- `snippetId`: ID del snippet
- `totalTests`: Total de tests ejecutados
- `passedTests`: Cantidad de tests que pasaron
- `failedTests`: Cantidad de tests que fallaron
- `results`: Array con el resultado de cada test
  - `testId`: ID del test
  - `testName`: Nombre del test
  - `passed`: Boolean indicando si el test pasó
  - `actualOutputs`: Outputs reales obtenidos
  - `expectedOutputs`: Outputs esperados
  - `errors`: Array de mensajes de error (vacío si no hubo errores)

### Casos de Uso
- ✅ Verificar que todos los tests de un snippet pasan antes de un deploy
- ✅ Validación automática después de modificar un snippet
- ✅ Reporte de calidad de código

### Códigos de Respuesta
- `200 OK` - Tests ejecutados exitosamente (independientemente de si pasaron o fallaron)
- `401 Unauthorized` - Token JWT inválido o ausente
- `403 Forbidden` - Usuario sin permisos para ejecutar tests en este snippet
- `404 Not Found` - Snippet no encontrado

---

## 2️⃣ Formatear Todos los Snippets del Usuario

### Endpoint
```
POST http://localhost:8080/format/all
```

### Headers
```
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

### Request Body
```
No requiere body
```

### cURL Example
```bash
curl -X POST "http://localhost:8080/format/all" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json"
```

### Response (200 OK)
```json
{
  "totalSnippets": 10,
  "successfullyFormatted": 8,
  "failed": 2,
  "results": [
    {
      "snippetId": "snippet-001",
      "snippetName": "Calculadora",
      "success": true,
      "errorMessage": null
    },
    {
      "snippetId": "snippet-002",
      "snippetName": "Fibonacci",
      "success": true,
      "errorMessage": null
    },
    {
      "snippetId": "snippet-003",
      "snippetName": "Snippet con error",
      "success": false,
      "errorMessage": "Snippet no encontrado con id: snippet-003"
    }
  ]
}
```

### Response Fields
- `totalSnippets`: Total de snippets del usuario (OWNER)
- `successfullyFormatted`: Cantidad de snippets formateados exitosamente
- `failed`: Cantidad de snippets que fallaron al formatear
- `results`: Array con el resultado de cada snippet
  - `snippetId`: ID del snippet
  - `snippetName`: Nombre del snippet
  - `success`: Boolean indicando si se formateó exitosamente
  - `errorMessage`: Mensaje de error (null si fue exitoso)

### Comportamiento
- ✅ Solo formatea snippets donde el usuario es **OWNER**
- ✅ Usa las reglas de formateo configuradas por el usuario
- ✅ Continúa con el siguiente snippet si uno falla
- ✅ No modifica el contenido en la base de datos, solo lo formatea

### Casos de Uso
- ✅ Formatear todos los snippets después de cambiar reglas de formateo
- ✅ Estandarizar el código de todos los snippets del usuario
- ✅ Preparación masiva de snippets para un proyecto

### Códigos de Respuesta
- `200 OK` - Operación completada (revisa el campo `failed` para ver si hubo errores)
- `401 Unauthorized` - Token JWT inválido o ausente

---

## 3️⃣ Lintear Todos los Snippets del Usuario

### Endpoint
```
POST http://localhost:8080/lint/all
```

### Headers
```
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

### Request Body
```
No requiere body
```

### cURL Example
```bash
curl -X POST "http://localhost:8080/lint/all" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json"
```

### Response (200 OK)
```json
{
  "totalSnippets": 10,
  "snippetsWithIssues": 3,
  "snippetsWithoutIssues": 7,
  "results": [
    {
      "snippetId": "snippet-001",
      "snippetName": "Calculadora",
      "issuesCount": 0,
      "issues": []
    },
    {
      "snippetId": "snippet-002",
      "snippetName": "Fibonacci",
      "issuesCount": 2,
      "issues": [
        {
          "rule": "identifier_format",
          "line": 5,
          "column": 10,
          "message": "Variable name should be in camelCase format"
        },
        {
          "rule": "lineBreak",
          "line": 12,
          "column": 1,
          "message": "Expected 2 line breaks, found 1"
        }
      ]
    },
    {
      "snippetId": "snippet-003",
      "snippetName": "Parser JSON",
      "issuesCount": 1,
      "issues": [
        {
          "rule": "spaceAroundEquals",
          "line": 3,
          "column": 8,
          "message": "Expected space around '=' operator"
        }
      ]
    }
  ]
}
```

### Response Fields
- `totalSnippets`: Total de snippets del usuario (OWNER)
- `snippetsWithIssues`: Cantidad de snippets con issues de linting
- `snippetsWithoutIssues`: Cantidad de snippets sin issues
- `results`: Array con el resultado de cada snippet
  - `snippetId`: ID del snippet
  - `snippetName`: Nombre del snippet
  - `issuesCount`: Cantidad de issues encontrados
  - `issues`: Array de issues (vacío si no hay issues)
    - `rule`: Nombre de la regla violada
    - `line`: Línea donde ocurre el issue
    - `column`: Columna donde ocurre el issue
    - `message`: Descripción del issue

### Comportamiento
- ✅ Solo analiza snippets donde el usuario es **OWNER**
- ✅ Usa las reglas de linting configuradas por el usuario
- ✅ Continúa con el siguiente snippet si uno falla
- ✅ No modifica el contenido, solo analiza

### Casos de Uso
- ✅ Auditoría de calidad de código de todos los snippets
- ✅ Identificar snippets que necesitan refactoring
- ✅ Reporte de cumplimiento de estándares de código

### Códigos de Respuesta
- `200 OK` - Operación completada
- `401 Unauthorized` - Token JWT inválido o ausente

---

## 📊 Comparación de Endpoints

| Característica | Run All Tests | Format All | Lint All |
|----------------|--------------|------------|----------|
| **URL** | `/api/snippets/{snippetId}/tests/run-all` | `/format/all` | `/lint/all` |
| **Método** | POST | POST | POST |
| **Scope** | Un snippet específico | Todos los snippets del usuario | Todos los snippets del usuario |
| **Requiere snippetId** | ✅ Sí | ❌ No | ❌ No |
| **Requiere Body** | ❌ No | ❌ No | ❌ No |
| **Solo OWNER** | ❌ No (cualquier permiso) | ✅ Sí | ✅ Sí |
| **Modifica contenido** | ❌ No | ❌ No | ❌ No |

---

## 🔐 Autenticación

Todos los endpoints requieren un token JWT válido en el header `Authorization`:

```bash
Authorization: Bearer <tu-token-jwt>
```

El `userId` se extrae automáticamente del token JWT (campo `sub`).

---

## 💡 Ejemplos de Integración en Frontend

### JavaScript/TypeScript

```typescript
// 1. Ejecutar todos los tests de un snippet
async function runAllTests(snippetId: string, token: string) {
  const response = await fetch(
    `http://localhost:8080/api/snippets/${snippetId}/tests/run-all`,
    {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    }
  );
  return await response.json();
}

// 2. Formatear todos los snippets
async function formatAllSnippets(token: string) {
  const response = await fetch('http://localhost:8080/format/all', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });
  return await response.json();
}

// 3. Lintear todos los snippets
async function lintAllSnippets(token: string) {
  const response = await fetch('http://localhost:8080/lint/all', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });
  return await response.json();
}
```

### React Hook Example

```typescript
import { useState } from 'react';

function useBulkOperations(token: string) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const runAllTests = async (snippetId: string) => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(
        `http://localhost:8080/api/snippets/${snippetId}/tests/run-all`,
        {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const data = await response.json();
      return data;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const formatAll = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch('http://localhost:8080/format/all', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const data = await response.json();
      return data;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const lintAll = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch('http://localhost:8080/lint/all', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const data = await response.json();
      return data;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  return { runAllTests, formatAll, lintAll, loading, error };
}
```

---

## ⚠️ Notas Importantes

1. **Permisos**: 
   - `run-all` requiere permisos de lectura en el snippet
   - `format/all` y `lint/all` solo procesan snippets donde eres OWNER

2. **Performance**: 
   - Estas operaciones pueden tardar dependiendo de la cantidad de snippets/tests
   - Se recomienda mostrar un indicador de carga en el frontend

3. **Errores Parciales**: 
   - Si un snippet/test falla, la operación continúa con los siguientes
   - Revisa el campo `results` para ver detalles de cada item

4. **Sin Modificación**: 
   - Ninguno de estos endpoints modifica el contenido de los snippets
   - Solo ejecutan validaciones y análisis

---

## 🎯 Flujo de Trabajo Recomendado

### Para Testing de un Snippet
```
1. Crear tests → POST /api/snippets/{id}/tests
2. Ejecutar todos los tests → POST /api/snippets/{id}/tests/run-all
3. Revisar resultados y corregir snippet si es necesario
```

### Para Estandarización de Código
```
1. Configurar reglas de formateo → POST /rules/format
2. Formatear todos los snippets → POST /format/all
3. Revisar resultados y corregir errores si los hay
```

### Para Auditoría de Calidad
```
1. Configurar reglas de linting → POST /rules/lint
2. Lintear todos los snippets → POST /lint/all
3. Revisar issues encontrados
4. Corregir snippets con issues
5. Volver a lintear para verificar
```

