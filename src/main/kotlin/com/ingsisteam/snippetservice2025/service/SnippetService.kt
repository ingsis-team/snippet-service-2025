package com.ingsisteam.snippetservice2025.service

import com.ingsisteam.snippetservice2025.model.dto.CreateSnippetDTO
import com.ingsisteam.snippetservice2025.model.dto.SnippetResponseDTO
import com.ingsisteam.snippetservice2025.model.dto.UpdateSnippetDTO
import com.ingsisteam.snippetservice2025.model.entity.Snippet
import com.ingsisteam.snippetservice2025.model.enum.SnippetLanguage
import com.ingsisteam.snippetservice2025.repository.SnippetRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SnippetService(
    private val snippetRepository: SnippetRepository,
) {

    fun createSnippet(createSnippetDTO: CreateSnippetDTO, userId: String): SnippetResponseDTO {
        // Verificar que no exista otro snippet con el mismo nombre para este usuario
        if (snippetRepository.existsByUserIdAndName(userId, createSnippetDTO.name)) {
            throw IllegalArgumentException("Ya existe un snippet con el nombre '${createSnippetDTO.name}'")
        }

        val snippet = Snippet(
            name = createSnippetDTO.name,
            description = createSnippetDTO.description,
            language = createSnippetDTO.language,
            content = createSnippetDTO.content,
            userId = userId,
        )

        val savedSnippet = snippetRepository.save(snippet)
        return toResponseDTO(savedSnippet)
    }

    @Transactional(readOnly = true)
    fun getSnippet(id: Long, userId: String): SnippetResponseDTO {
        val snippet = snippetRepository.findByIdAndUserId(id, userId)
            ?: throw NoSuchElementException("Snippet con ID $id no encontrado")
        return toResponseDTO(snippet)
    }

    @Transactional(readOnly = true)
    fun getAllSnippets(userId: String): List<SnippetResponseDTO> {
        return snippetRepository.findByUserId(userId).map { toResponseDTO(it) }
    }

    @Transactional(readOnly = true)
    fun getAllSnippetsPaginated(userId: String, pageable: Pageable): Page<SnippetResponseDTO> {
        return snippetRepository.findByUserId(userId, pageable).map { toResponseDTO(it) }
    }

    @Transactional(readOnly = true)
    fun getSnippetsByLanguage(userId: String, language: SnippetLanguage): List<SnippetResponseDTO> {
        return snippetRepository.findByUserIdAndLanguage(userId, language).map { toResponseDTO(it) }
    }

    @Transactional(readOnly = true)
    fun searchSnippetsByName(userId: String, name: String): List<SnippetResponseDTO> {
        return snippetRepository.findByUserIdAndNameContainingIgnoreCase(userId, name)
            .map { toResponseDTO(it) }
    }

    fun updateSnippet(id: Long, updateSnippetDTO: UpdateSnippetDTO, userId: String): SnippetResponseDTO {
        val snippet = snippetRepository.findByIdAndUserId(id, userId)
            ?: throw NoSuchElementException("Snippet con ID $id no encontrado")

        // Verificar si se está cambiando el nombre y si ya existe otro con ese nombre
        updateSnippetDTO.name?.let { newName ->
            if (newName != snippet.name && snippetRepository.existsByUserIdAndName(userId, newName)) {
                throw IllegalArgumentException("Ya existe un snippet con el nombre '$newName'")
            }
            snippet.name = newName
        }

        updateSnippetDTO.description?.let { snippet.description = it }
        updateSnippetDTO.language?.let { snippet.language = it }
        updateSnippetDTO.content?.let { snippet.content = it }

        val savedSnippet = snippetRepository.save(snippet)
        return toResponseDTO(savedSnippet)
    }

    fun deleteSnippet(id: Long, userId: String) {
        val snippet = snippetRepository.findByIdAndUserId(id, userId)
            ?: throw NoSuchElementException("Snippet con ID $id no encontrado")

        snippetRepository.delete(snippet)
    }

    private fun toResponseDTO(snippet: Snippet): SnippetResponseDTO {
        return SnippetResponseDTO(
            id = snippet.id,
            name = snippet.name,
            description = snippet.description,
            language = snippet.language,
            content = snippet.content,
            userId = snippet.userId,
            createdAt = snippet.createdAt,
            updatedAt = snippet.updatedAt,
        )
    }
}
