package com.ingsisteam.snippetservice2025.service

import com.ingsisteam.snippetservice2025.connector.PermissionServiceConnector
import com.ingsisteam.snippetservice2025.connector.PrintScriptServiceConnector
import com.ingsisteam.snippetservice2025.exception.PermissionDeniedException
import com.ingsisteam.snippetservice2025.exception.SnippetNotFoundException
import com.ingsisteam.snippetservice2025.exception.TestNotFoundException
import com.ingsisteam.snippetservice2025.model.dto.CreateTestDTO
import com.ingsisteam.snippetservice2025.model.dto.RunTestResponseDTO
import com.ingsisteam.snippetservice2025.model.dto.TestResponseDTO
import com.ingsisteam.snippetservice2025.model.entity.SnippetTest
import com.ingsisteam.snippetservice2025.repository.SnippetRepository
import com.ingsisteam.snippetservice2025.repository.SnippetTestRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

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
        val snippet = snippetRepository.findById(snippetId).orElse(null)
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
        val snippet = snippetRepository.findById(snippetId).orElse(null)
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

    fun runTest(snippetId: Long, testId: Long, userId: String): RunTestResponseDTO {
        println("▶️ [RUN TEST] Running test $testId for snippet $snippetId by user $userId")

        // Verificar que el snippet existe
        val snippet = snippetRepository.findById(snippetId).orElse(null)
            ?: throw SnippetNotFoundException("Snippet con ID $snippetId no encontrado")

        // Verificar permisos de lectura
        if (!permissionServiceConnector.hasPermission(snippetId, userId)) {
            throw PermissionDeniedException("No tienes permisos para ejecutar tests de este snippet")
        }

        // Buscar el test
        val test = snippetTestRepository.findByIdAndSnippetId(testId, snippetId)
            ?: throw TestNotFoundException("Test con ID $testId no encontrado para el snippet $snippetId")

        println("🧪 [RUN TEST] Test '${test.name}' found. Running with ${test.inputs.size} inputs")

        // Preparar el input del test (concatenar todos los inputs)
        val testInput = test.inputs.joinToString("\n")

        // Generar correlationId único
        val correlationId = UUID.randomUUID().toString()

        // Ejecutar el snippet con el input del test
        val runResponse = printScriptServiceConnector.runSnippet(
            userId = userId,
            snippetId = snippetId.toString(),
            language = snippet.language.name.lowercase(),
            version = snippet.version,
            input = testInput,
            correlationId = correlationId,
        )

        println("📤 [RUN TEST] Received output: ${runResponse.output}")

        // Verificar si el output coincide con el esperado
        val expectedOutput = test.expectedOutputs.joinToString("\n")
        val actualOutput = runResponse.output.trim()
        val passed = actualOutput == expectedOutput.trim()

        val message = if (passed) {
            "El test pasó exitosamente"
        } else {
            "El test falló. Output esperado: '$expectedOutput', Output obtenido: '$actualOutput'"
        }

        println("✅ [RUN TEST] Test ${if (passed) "PASSED" else "FAILED"}")

        return RunTestResponseDTO(
            testId = test.id,
            testName = test.name,
            snippetId = snippetId,
            output = actualOutput,
            passed = passed,
            message = message,
        )
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
