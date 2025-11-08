package com.ingsisteam.snippetservice2025.service

import com.ingsisteam.snippetservice2025.connector.PermissionServiceConnector
import com.ingsisteam.snippetservice2025.exception.PermissionDeniedException
import com.ingsisteam.snippetservice2025.exception.SnippetNotFoundException
import com.ingsisteam.snippetservice2025.exception.TestNotFoundException
import com.ingsisteam.snippetservice2025.model.dto.CreateTestDTO
import com.ingsisteam.snippetservice2025.model.dto.TestResponseDTO
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
