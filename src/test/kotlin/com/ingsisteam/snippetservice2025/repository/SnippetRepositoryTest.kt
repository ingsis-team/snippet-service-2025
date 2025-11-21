package com.ingsisteam.snippetservice2025.repository

import com.ingsisteam.snippetservice2025.model.entity.Snippet
import com.ingsisteam.snippetservice2025.model.enum.SnippetLanguage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
class SnippetRepositoryTest @Autowired constructor(
    val entityManager: TestEntityManager,
    val snippetRepository: SnippetRepository,
) {

    private val USER_ID = "testUser123"
    private val OTHER_USER_ID = "otherUser456"

    @BeforeEach
    fun setup() {
        // Clear any existing data and setup fresh data for each test
        snippetRepository.deleteAll()

        // Persist some snippets for testing
        val snippet1 = Snippet(
            name = "Test Snippet 1",
            description = "Description 1",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "println(\"Hello 1\")",
            userId = USER_ID,
            version = "1.0.0",
        )
        val snippet2 = Snippet(
            name = "Test Snippet 2",
            description = "Description 2",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "println(\"Hello 2\")",
            userId = USER_ID,
            version = "1.0.0",
        )
        val snippet3 = Snippet(
            name = "Other User Snippet",
            description = "Description for other user",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "println(\"Other User\")",
            userId = OTHER_USER_ID,
            version = "1.0.0",
        )
        entityManager.persist(snippet1)
        entityManager.persist(snippet2)
        entityManager.persist(snippet3)
        entityManager.flush()
    }

    @Test
    fun `findByUserId returns all snippets for a given user`() {
        val foundSnippets = snippetRepository.findByUserId(USER_ID)
        assertEquals(2, foundSnippets.size)
        assertTrue(foundSnippets.all { it.userId == USER_ID })
    }

    @Test
    fun `findByUserId returns empty list if no snippets found for user`() {
        val foundSnippets = snippetRepository.findByUserId("nonExistentUser")
        assertEquals(0, foundSnippets.size)
    }

    @Test
    fun `findByUserIdAndNameContainingIgnoreCase returns matching snippets for user and name`() {
        val foundSnippets = snippetRepository.findByUserIdAndNameContainingIgnoreCase(USER_ID, "snippet")
        assertEquals(2, foundSnippets.size)
        assertTrue(foundSnippets.all { it.userId == USER_ID && it.name.contains("Snippet", ignoreCase = true) })

        val foundSnippet1 = snippetRepository.findByUserIdAndNameContainingIgnoreCase(USER_ID, "test snippet 1")
        assertEquals(1, foundSnippet1.size)
        assertEquals("Test Snippet 1", foundSnippet1[0].name)
    }

    @Test
    fun `findByUserIdAndNameContainingIgnoreCase returns empty list if no matching snippets for user and name`() {
        val foundSnippets = snippetRepository.findByUserIdAndNameContainingIgnoreCase(USER_ID, "nonexistent")
        assertEquals(0, foundSnippets.size)
    }

    @Test
    fun `existsByUserIdAndName returns true if snippet with name exists for user`() {
        assertTrue(snippetRepository.existsByUserIdAndName(USER_ID, "Test Snippet 1"))
        assertFalse(snippetRepository.existsByUserIdAndName(USER_ID, "Non Existent Snippet"))
    }

    @Test
    fun `findByIdAndUserId returns snippet if found for user`() {
        val snippet1 = snippetRepository.findByUserId(USER_ID).first { it.name == "Test Snippet 1" }
        val foundSnippet = snippetRepository.findByIdAndUserId(snippet1.id, USER_ID)
        assertNotNull(foundSnippet)
        assertEquals(snippet1.id, foundSnippet?.id)
        assertEquals(USER_ID, foundSnippet?.userId)
    }

    @Test
    fun `findByIdAndUserId returns null if snippet not found for user`() {
        val foundSnippet = snippetRepository.findByIdAndUserId(999L, USER_ID) // Non-existent ID
        assertNull(foundSnippet)

        val snippet1 = snippetRepository.findByUserId(USER_ID).first { it.name == "Test Snippet 1" }
        val foundSnippetOtherUser = snippetRepository.findByIdAndUserId(snippet1.id, OTHER_USER_ID) // Wrong user
        assertNull(foundSnippetOtherUser)
    }

    @Test
    fun `save new snippet`() {
        val newSnippet = Snippet(
            name = "New Snippet",
            description = "New Desc",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "new content",
            userId = USER_ID,
            version = "1.0.0",
        )
        val savedSnippet = snippetRepository.save(newSnippet)
        assertNotNull(savedSnippet.id)
        assertEquals("New Snippet", savedSnippet.name)

        val found = snippetRepository.findByIdAndUserId(savedSnippet.id, USER_ID)
        assertNotNull(found)
    }

    @Test
    fun `update existing snippet`() {
        val snippetToUpdate = snippetRepository.findByUserId(USER_ID).first { it.name == "Test Snippet 1" }
        val originalUpdatedAt = snippetToUpdate.updatedAt // Capture original timestamp before modifications

        snippetToUpdate.name = "Updated Snippet Name"
        snippetToUpdate.content = "Updated content"

        entityManager.flush() // Ensure changes are flushed before checking updatedAt
        entityManager.clear() // Detach entity to ensure it's reloaded from DB

        val updatedSnippet = snippetRepository.save(snippetToUpdate)
        val reloadedSnippet = snippetRepository.findById(updatedSnippet.id).orElse(null)

        assertNotNull(reloadedSnippet)
        assertEquals("Updated Snippet Name", reloadedSnippet?.name)
        assertEquals("Updated content", reloadedSnippet?.content)
        assertTrue(reloadedSnippet?.updatedAt?.isAfter(originalUpdatedAt) == true)

        val found = snippetRepository.findByIdAndUserId(updatedSnippet.id, USER_ID)
        assertNotNull(found)
        assertEquals("Updated Snippet Name", found?.name)
    }

    @Test
    fun `delete snippet`() {
        val snippetToDelete = snippetRepository.findByUserId(USER_ID).first { it.name == "Test Snippet 1" }
        snippetRepository.delete(snippetToDelete)

        val found = snippetRepository.findByIdAndUserId(snippetToDelete.id, USER_ID)
        assertNull(found)
    }
}
