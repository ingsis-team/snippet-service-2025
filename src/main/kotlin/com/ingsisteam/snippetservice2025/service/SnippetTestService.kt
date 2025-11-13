package com.ingsisteam.snippetservice2025.service

import com.ingsisteam.snippetservice2025.connector.PermissionServiceConnector
import com.ingsisteam.snippetservice2025.connector.PrintScriptServiceConnector
import com.ingsisteam.snippetservice2025.exception.PermissionDeniedException
import com.ingsisteam.snippetservice2025.exception.SnippetNotFoundException
import com.ingsisteam.snippetservice2025.exception.TestNotFoundException
import com.ingsisteam.snippetservice2025.model.dto.CreateTestDTO
import com.ingsisteam.snippetservice2025.model.dto.TestResponseDTO
import com.ingsisteam.snippetservice2025.model.entity.Snippet
import com.ingsisteam.snippetservice2025.model.entity.SnippetTest
import com.ingsisteam.snippetservice2025.repository.SnippetRepository
import com.ingsisteam.snippetservice2025.repository.SnippetTestRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SnippetTestService(
    private val snippetTestRepository: SnippetTestRepository,
    private val snippetRepository: SnippetRepository,
    private val permissionServiceConnector: PermissionServiceConnector,
    private val printScriptServiceConnector: PrintScriptServiceConnector,
) {

    fun createTest(snippetId: Long, createTestDTO: CreateTestDTO, userId: String): TestResponseDTO {
        println("📝 [CREATE TEST] Creating test '${createTestDTO.name}' for snippet $snippetId by user $userId")

        // Verificar que el snippet existe
        snippetRepository.findById(snippetId).orElse(null)
            ?: throw SnippetNotFoundException("Snippet con ID $snippetId no encontrado")

        // Verificar permisos de escritura
        if (!permissionServiceConnector.hasPermission(snippetId, userId)) {
            throw PermissionDeniedException("No tienes permisos para crear tests en este snippet")
        }

        // Verificar que no exista otro test con el mismo nombre para este snippet
        if (snippetTestRepository.existsBySnippetIdAndName(snippetId, createTestDTO.name)) {
            throw IllegalArgumentException("Ya existe un test con el nombre '${createTestDTO.name}' para este snippet")
        }

        // Crear el test
        val test = SnippetTest(
            snippetId = snippetId,
            name = createTestDTO.name,
            inputs = createTestDTO.inputs,
            expectedOutputs = createTestDTO.expectedOutputs,
            expectedStatus = createTestDTO.expectedStatus,
        )

        val savedTest = snippetTestRepository.save(test)
        println("✅ [CREATE TEST] Test created successfully with ID: ${savedTest.id}")

        return toResponseDTO(savedTest)
    }

    @Transactional(readOnly = true)
    fun getTest(snippetId: Long, testId: Long, userId: String): TestResponseDTO {
        println("🔍 [GET TEST] Getting test $testId for snippet $snippetId by user $userId")

        // Verificar que el snippet existe
        snippetRepository.findById(snippetId).orElse(null)
            ?: throw SnippetNotFoundException("Snippet con ID $snippetId no encontrado")

        // Verificar permisos de lectura
        if (!permissionServiceConnector.hasPermission(snippetId, userId)) {
            throw PermissionDeniedException("No tienes permisos para ver tests de este snippet")
        }

        // Buscar el test
        val test = snippetTestRepository.findByIdAndSnippetId(testId, snippetId)
            ?: throw TestNotFoundException("Test con ID $testId no encontrado para el snippet $snippetId")

        return toResponseDTO(test)
    }

    @Transactional(readOnly = true)
    fun getTestsBySnippet(snippetId: Long, userId: String): List<TestResponseDTO> {
        println("📋 [GET TESTS] Getting all tests for snippet $snippetId by user $userId")

        // Verificar que el snippet existe
        snippetRepository.findById(snippetId).orElse(null)
            ?: throw SnippetNotFoundException("Snippet con ID $snippetId no encontrado")

        // Verificar permisos de lectura
        if (!permissionServiceConnector.hasPermission(snippetId, userId)) {
            throw PermissionDeniedException("No tienes permisos para ver tests de este snippet")
        }

        // Obtener todos los tests del snippet
        val tests = snippetTestRepository.findBySnippetId(snippetId)
        println("✅ [GET TESTS] Found ${tests.size} tests for snippet $snippetId")

        return tests.map { toResponseDTO(it) }
    }

    fun deleteTest(snippetId: Long, testId: Long, userId: String) {
        println("🗑️ [DELETE TEST] Deleting test $testId for snippet $snippetId by user $userId")

        // Verificar que el snippet existe
        snippetRepository.findById(snippetId).orElse(null)
            ?: throw SnippetNotFoundException("Snippet con ID $snippetId no encontrado")

        // Verificar permisos de escritura
        if (!permissionServiceConnector.hasPermission(snippetId, userId)) {
            throw PermissionDeniedException("No tienes permisos para eliminar tests de este snippet")
        }

        // Buscar el test
        val test = snippetTestRepository.findByIdAndSnippetId(testId, snippetId)
            ?: throw TestNotFoundException("Test con ID $testId no encontrado para el snippet $snippetId")

        // Eliminar el test
        snippetTestRepository.delete(test)
        println("✅ [DELETE TEST] Test deleted successfully")
    }

    fun executeTest(snippetId: Long, testId: Long, userId: String): Map<String, Any> {
        println("🚀 [EXECUTE TEST] Executing test $testId for snippet $snippetId by user $userId")

        // Verificar que el snippet existe y obtenerlo
        val snippet = snippetRepository.findById(snippetId).orElse(null)
            ?: throw SnippetNotFoundException("Snippet con ID $snippetId no encontrado")

        // Verificar permisos de lectura
        if (!permissionServiceConnector.hasPermission(snippetId, userId)) {
            throw PermissionDeniedException("No tienes permisos para ejecutar tests de este snippet")
        }

        // Buscar el test
        val test = snippetTestRepository.findByIdAndSnippetId(testId, snippetId)
            ?: throw TestNotFoundException("Test con ID $testId no encontrado para el snippet $snippetId")

        println("📝 [EXECUTE TEST] Running snippet with ${test.inputs.size} inputs, expecting ${test.expectedOutputs.size} outputs")

        // Ejecutar el snippet con el PrintScript service
        val executionResult = try {
            executeSnippetWithInputs(snippet, test.inputs)
        } catch (e: Exception) {
            println("❌ [EXECUTE TEST] Execution failed: ${e.message}")
            return mapOf(
                "passed" to false,
                "expected" to test.expectedOutputs,
                "actual" to emptyList<String>(),
                "error" to (e.message ?: "Unknown error"),
            )
        }

        // Comparar los outputs
        val passed = compareOutputs(test.expectedOutputs, executionResult)

        println("${if (passed) "✅" else "❌"} [EXECUTE TEST] Test ${if (passed) "PASSED" else "FAILED"}")

        return mapOf(
            "passed" to passed,
            "expected" to test.expectedOutputs,
            "actual" to executionResult,
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun executeSnippetWithInputs(snippet: Snippet, inputs: List<String>): List<String> {
        // Modificar el código del snippet para inyectar los inputs como variables
        // Por simplicidad, ejecutaremos el snippet y capturaremos los outputs de println

        // TODO: En una implementación completa, necesitaríamos:
        // 1. Parsear el código para encontrar llamadas a readInput()
        // 2. Reemplazarlas con los valores de inputs
        // 3. Ejecutar y capturar los outputs de println()

        // Por ahora, simulamos una ejecución simple:
        // Asumimos que el snippet solo tiene println() sin readInput()
        val outputs = mutableListOf<String>()

        // Dividir el contenido en líneas y buscar println()
        val lines = snippet.content.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("println(") && trimmed.endsWith(");")) {
                // Extraer el valor entre paréntesis
                val content = trimmed.substring(8, trimmed.length - 2).trim()
                // Quitar comillas si es string literal (double o single quotes)
                val output = if ((content.startsWith("\"") && content.endsWith("\"")) ||
                    (content.startsWith("'") && content.endsWith("'"))
                ) {
                    content.substring(1, content.length - 1)
                } else {
                    // Si no es string literal, podría ser una variable
                    // Por ahora solo manejamos literales
                    content
                }
                outputs.add(output)
            }
        }

        return outputs
    }

    private fun compareOutputs(expected: List<String>, actual: List<String>): Boolean {
        if (expected.size != actual.size) {
            println("⚠️ [COMPARE] Size mismatch: expected ${expected.size}, got ${actual.size}")
            return false
        }

        for (i in expected.indices) {
            if (expected[i] != actual[i]) {
                println("⚠️ [COMPARE] Output mismatch at index $i: expected '${expected[i]}', got '${actual[i]}'")
                return false
            }
        }

        return true
    }

    private fun toResponseDTO(test: SnippetTest): TestResponseDTO {
        return TestResponseDTO(
            id = test.id,
            snippetId = test.snippetId,
            name = test.name,
            inputs = test.inputs,
            expectedOutputs = test.expectedOutputs,
            expectedStatus = test.expectedStatus,
            createdAt = test.createdAt,
            updatedAt = test.updatedAt,
        )
    }
}
