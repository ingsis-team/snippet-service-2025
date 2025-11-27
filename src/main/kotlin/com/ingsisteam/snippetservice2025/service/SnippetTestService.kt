package com.ingsisteam.snippetservice2025.service

import com.ingsisteam.snippetservice2025.connector.AssetServiceConnector
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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SnippetTestService(
    private val snippetTestRepository: SnippetTestRepository,
    private val snippetRepository: SnippetRepository,
    private val permissionServiceConnector: PermissionServiceConnector,
    private val printScriptServiceConnector: PrintScriptServiceConnector,
    private val assetServiceConnector: AssetServiceConnector,
) {
    private val logger = LoggerFactory.getLogger(SnippetTestService::class.java)

    fun createTest(snippetId: String, createTestDTO: CreateTestDTO, userId: String): TestResponseDTO {
        logger.info("Creating test '{}' for snippet {} by user: {}", createTestDTO.name, snippetId, userId)

        // Verificar que el snippet existe
        snippetRepository.findById(snippetId).orElse(null)
            ?: throw SnippetNotFoundException("Snippet con ID $snippetId no encontrado")

        // Verificar permisos de escritura
        if (!permissionServiceConnector.hasPermission(snippetId, userId)) {
            logger.warn("User {} does not have permission to create tests on snippet {}", userId, snippetId)
            throw PermissionDeniedException("No tienes permisos para crear tests en este snippet")
        }

        // Verificar que no exista otro test con el mismo nombre para este snippet
        if (snippetTestRepository.existsBySnippetIdAndName(snippetId, createTestDTO.name)) {
            logger.warn("Duplicate test name '{}' for snippet {}", createTestDTO.name, snippetId)
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
        logger.info("Test created successfully: ID={}, name='{}', snippetId={}", savedTest.id, savedTest.name, snippetId)

        return toResponseDTO(savedTest)
    }

    @Transactional(readOnly = true)
    fun getTest(snippetId: String, testId: String, userId: String): TestResponseDTO {
        logger.debug("Fetching test {} for snippet {} by user: {}", testId, snippetId, userId)

        // Verificar que el snippet existe
        snippetRepository.findById(snippetId).orElse(null)
            ?: throw SnippetNotFoundException("Snippet con ID $snippetId no encontrado")

        // Verificar permisos de lectura
        if (!permissionServiceConnector.hasPermission(snippetId, userId)) {
            logger.warn("User {} does not have permission to view tests on snippet {}", userId, snippetId)
            throw PermissionDeniedException("No tienes permisos para ver tests de este snippet")
        }

        // Buscar el test
        val test = snippetTestRepository.findByIdAndSnippetId(testId, snippetId)
            ?: throw TestNotFoundException("Test con ID $testId no encontrado para el snippet $snippetId")

        logger.debug("Test {} retrieved successfully", testId)
        return toResponseDTO(test)
    }

    @Transactional(readOnly = true)
    fun getTestsBySnippet(snippetId: String, userId: String): List<TestResponseDTO> {
        logger.debug("Fetching all tests for snippet {} by user: {}", snippetId, userId)

        // Verificar que el snippet existe
        snippetRepository.findById(snippetId).orElse(null)
            ?: throw SnippetNotFoundException("Snippet con ID $snippetId no encontrado")

        // Verificar permisos de lectura
        if (!permissionServiceConnector.hasPermission(snippetId, userId)) {
            logger.warn("User {} does not have permission to view tests on snippet {}", userId, snippetId)
            throw PermissionDeniedException("No tienes permisos para ver tests de este snippet")
        }

        // Obtener todos los tests del snippet
        val tests = snippetTestRepository.findBySnippetId(snippetId)
        logger.debug("Found {} tests for snippet {}", tests.size, snippetId)

        return tests.map { toResponseDTO(it) }
    }

    fun deleteTest(snippetId: String, testId: String, userId: String) {
        logger.info("Deleting test {} from snippet {} by user: {}", testId, snippetId, userId)

        // Verificar que el snippet existe
        snippetRepository.findById(snippetId).orElse(null)
            ?: throw SnippetNotFoundException("Snippet con ID $snippetId no encontrado")

        // Verificar permisos de escritura
        if (!permissionServiceConnector.hasPermission(snippetId, userId)) {
            logger.warn("User {} does not have permission to delete tests on snippet {}", userId, snippetId)
            throw PermissionDeniedException("No tienes permisos para eliminar tests de este snippet")
        }

        // Buscar el test
        val test = snippetTestRepository.findByIdAndSnippetId(testId, snippetId)
            ?: throw TestNotFoundException("Test con ID $testId no encontrado para el snippet $snippetId")

        // Eliminar el test
        snippetTestRepository.delete(test)
        logger.info("Test {} deleted successfully", testId)
    }

    fun executeTest(snippetId: String, testId: String, userId: String): Map<String, Any> {
        logger.info("Executing test {} for snippet {} by user: {}", testId, snippetId, userId)

        // Verificar que el snippet existe y obtenerlo
        val snippet = snippetRepository.findById(snippetId).orElse(null)
            ?: throw SnippetNotFoundException("Snippet con ID $snippetId no encontrado")

        // Verificar permisos de lectura
        if (!permissionServiceConnector.hasPermission(snippetId, userId)) {
            logger.warn("User {} does not have permission to execute tests on snippet {}", userId, snippetId)
            throw PermissionDeniedException("No tienes permisos para ejecutar tests de este snippet")
        }

        // Buscar el test
        val test = snippetTestRepository.findByIdAndSnippetId(testId, snippetId)
            ?: throw TestNotFoundException("Test con ID $testId no encontrado para el snippet $snippetId")

        logger.debug(
            "Running snippet with {} inputs, expecting {} outputs, expectedStatus: {}",
            test.inputs.size,
            test.expectedOutputs.size,
            test.expectedStatus,
        )

        // Ejecutar el snippet con el PrintScript service
        var executionResult: List<String>
        var executionError: String?
        var executionFailed = false

        try {
            executionResult = executeSnippetWithInputs(snippet, test.inputs)
            executionError = null
            logger.debug("Snippet executed successfully with {} outputs", executionResult.size)
        } catch (e: Exception) {
            executionResult = emptyList()
            executionError = e.message ?: "Unknown error"
            executionFailed = true
            logger.warn("Snippet execution failed: {}", executionError)
        }

        // Determinar si el test pasó basándose en el expectedStatus
        val passed = when (test.expectedStatus) {
            com.ingsisteam.snippetservice2025.model.enum.TestStatus.VALID -> {
                // Si se espera que sea válido, debe ejecutarse sin errores y los outputs deben coincidir
                !executionFailed && compareOutputs(test.expectedOutputs, executionResult)
            }
            com.ingsisteam.snippetservice2025.model.enum.TestStatus.INVALID -> {
                // Si se espera que sea inválido, debe fallar la ejecución o los outputs no deben coincidir
                executionFailed || !compareOutputs(test.expectedOutputs, executionResult)
            }
        }

        val resultMessage = when {
            test.expectedStatus == com.ingsisteam.snippetservice2025.model.enum.TestStatus.VALID && passed ->
                "Test pasó: ejecución exitosa con outputs esperados"
            test.expectedStatus == com.ingsisteam.snippetservice2025.model.enum.TestStatus.VALID && !passed ->
                if (executionFailed) {
                    "Test falló: se esperaba ejecución exitosa pero falló"
                } else {
                    "Test falló: outputs no coinciden con los esperados"
                }
            test.expectedStatus == com.ingsisteam.snippetservice2025.model.enum.TestStatus.INVALID && passed ->
                "Test pasó: se esperaba fallo y el snippet falló como se esperaba"
            else ->
                "Test falló: se esperaba fallo pero el snippet se ejecutó exitosamente"
        }

        logger.info("Test {} execution completed: {} - {}", testId, if (passed) "PASSED" else "FAILED", resultMessage)

        return buildMap {
            put("passed", passed)
            put("expectedStatus", test.expectedStatus.name)
            put("expectedOutputs", test.expectedOutputs)
            put("actualOutputs", executionResult)
            put("executionFailed", executionFailed)
            put("message", resultMessage)
            if (executionError != null) {
                put("error", executionError)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun executeSnippetWithInputs(snippet: Snippet, inputs: List<String>): List<String> {
        // Modify the snippet code to inject inputs as variables
        // For simplicity, we'll execute the snippet and capture println outputs

        // TODO: In a complete implementation, we would need:
        // 1. Parse the code to find readInput() calls
        // 2. Replace them with input values
        // 3. Execute and capture println() outputs

        // Retrieve content from asset service
        val content = assetServiceConnector.getSnippet(snippet.id)
            ?: throw RuntimeException("No se pudo recuperar el contenido del snippet desde el servicio de assets")

        // For now, we simulate a simple execution:
        // We assume the snippet only has println() without readInput()
        val outputs = mutableListOf<String>()

        // Split content into lines and search for println()
        val lines = content.lines()
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
            logger.warn("Size mismatch: expected {}, got {}", expected.size, actual.size)
            return false
        }

        for (i in expected.indices) {
            if (expected[i] != actual[i]) {
                logger.warn("Output mismatch at index {}: expected '{}', got '{}'", i, expected[i], actual[i])
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
