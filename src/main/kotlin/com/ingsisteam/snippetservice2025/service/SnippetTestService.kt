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

        return toResponseDTO(savedTest)
    }

    @Transactional(readOnly = true)
    fun getTest(snippetId: Long, testId: Long, userId: String): TestResponseDTO {
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
        // Verificar que el snippet existe
        snippetRepository.findById(snippetId).orElse(null)
            ?: throw SnippetNotFoundException("Snippet con ID $snippetId no encontrado")

        // Verificar permisos de lectura
        if (!permissionServiceConnector.hasPermission(snippetId, userId)) {
            throw PermissionDeniedException("No tienes permisos para ver tests de este snippet")
        }

        // Obtener todos los tests del snippet
        val tests = snippetTestRepository.findBySnippetId(snippetId)

        return tests.map { toResponseDTO(it) }
    }

    fun deleteTest(snippetId: Long, testId: Long, userId: String) {
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
    }

    fun executeTest(snippetId: Long, testId: Long, userId: String): Map<String, Any> {
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

        // Ejecutar el snippet con el PrintScript service
        val executionResult = try {
            executeSnippetWithInputs(snippet, test.inputs)
        } catch (e: Exception) {
            return mapOf(
                "passed" to false,
                "expected" to test.expectedOutputs,
                "actual" to emptyList<String>(),
                "error" to (e.message ?: "Unknown error"),
            )
        }

        // Comparar los outputs
        val passed = compareOutputs(test.expectedOutputs, executionResult)

        return mapOf(
            "passed" to passed,
            "expected" to test.expectedOutputs,
            "actual" to executionResult,
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun executeSnippetWithInputs(snippet: Snippet, inputs: List<String>): List<String> {
        // Modify the snippet code to inject inputs as variables
        // For simplicity, we'll execute the snippet and capture println outputs

        // TODO: In a complete implementation, we would need:
        // 1. Parse the code to find readInput() calls
        // 2. Replace them with input values
        // 3. Execute and capture println() outputs

        // For now, we simulate a simple execution:
        // We assume the snippet only has println() without readInput()
        val outputs = mutableListOf<String>()

        // Split content into lines and search for println()
        val lines = snippet.content.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("println(") && trimmed.endsWith(");")) {
                // Extract the value between parentheses
                val content = trimmed.substring(8, trimmed.length - 2).trim()
                // Remove quotes if it's a string literal (double or single quotes)
                val output = if ((content.startsWith("\"") && content.endsWith("\"")) ||
                    (content.startsWith("'") && content.endsWith("'"))
                ) {
                    content.substring(1, content.length - 1)
                } else {
                    // If it's not a string literal, it could be a variable
                    // For now we only handle literals
                    content
                }
                outputs.add(output)
            }
        }

        return outputs
    }

    private fun compareOutputs(expected: List<String>, actual: List<String>): Boolean {
        if (expected.size != actual.size) {
            return false
        }

        for (i in expected.indices) {
            if (expected[i] != actual[i]) {
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
