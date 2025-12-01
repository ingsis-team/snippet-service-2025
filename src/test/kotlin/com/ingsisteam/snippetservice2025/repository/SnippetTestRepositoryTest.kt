package com.ingsisteam.snippetservice2025.repository

import com.ingsisteam.snippetservice2025.model.entity.Snippet
import com.ingsisteam.snippetservice2025.model.entity.SnippetTest
import com.ingsisteam.snippetservice2025.model.enum.SnippetLanguage
import com.ingsisteam.snippetservice2025.model.enum.TestStatus
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
class SnippetTestRepositoryTest @Autowired constructor(
    val entityManager: TestEntityManager,
    val snippetRepository: SnippetRepository, // Need SnippetRepository to persist Snippets
    val snippetTestRepository: SnippetTestRepository,
) {

    private lateinit var testSnippet: Snippet
    private lateinit var otherSnippet: Snippet
    private val USER_ID = "testUser123"

    @BeforeEach
    fun setup() {
        // Clear any existing data and setup fresh data for each test
        snippetTestRepository.deleteAll()
        snippetRepository.deleteAll()

        // Persist a parent snippet
        testSnippet = Snippet(
            name = "Parent Snippet",
            description = "Description",
            language = SnippetLanguage.PRINTSCRIPT,
            userId = USER_ID,
            version = "1.0.0",
        )
        entityManager.persist(testSnippet)

        otherSnippet = Snippet(
            name = "Other Parent Snippet",
            description = "Description",
            language = SnippetLanguage.PRINTSCRIPT,
            userId = USER_ID,
            version = "1.0.0",
        )
        entityManager.persist(otherSnippet)

        // Persist some SnippetTests for testing
        val snippetTest1 = SnippetTest(
            snippetId = testSnippet.id,
            name = "Test 1 for Parent Snippet",
            inputs = listOf("input1"),
            expectedOutputs = listOf("output1"),
            expectedStatus = TestStatus.VALID,
        )
        val snippetTest2 = SnippetTest(
            snippetId = testSnippet.id,
            name = "Test 2 for Parent Snippet",
            inputs = listOf("input2"),
            expectedOutputs = listOf("output2"),
            expectedStatus = TestStatus.INVALID,
        )
        val otherSnippetTest = SnippetTest(
            snippetId = otherSnippet.id,
            name = "Test for Other Parent Snippet",
            inputs = listOf("input3"),
            expectedOutputs = listOf("output3"),
            expectedStatus = TestStatus.VALID,
        )
        entityManager.persist(snippetTest1)
        entityManager.persist(snippetTest2)
        entityManager.persist(otherSnippetTest)
        entityManager.flush()
    }

    @Test
    fun `findBySnippetId returns all tests for a given snippet`() {
        val foundTests = snippetTestRepository.findBySnippetId(testSnippet.id)
        assertEquals(2, foundTests.size)
        assertTrue(foundTests.all { it.snippetId == testSnippet.id })
    }

    @Test
    fun `findBySnippetId returns empty list if no tests found for snippet`() {
        val foundTests = snippetTestRepository.findBySnippetId("non-existent-id") // Non-existent snippet ID
        assertEquals(0, foundTests.size)
    }

    @Test
    fun `findByIdAndSnippetId returns specific test if found for snippet`() {
        val test1 = snippetTestRepository.findBySnippetId(testSnippet.id).first { it.name == "Test 1 for Parent Snippet" }
        val foundTest = snippetTestRepository.findByIdAndSnippetId(test1.id, testSnippet.id)
        assertNotNull(foundTest)
        assertEquals(test1.id, foundTest?.id)
        assertEquals(testSnippet.id, foundTest?.snippetId)
    }

    @Test
    fun `findByIdAndSnippetId returns null if test not found for snippet`() {
        val foundTest = snippetTestRepository.findByIdAndSnippetId("non-existent-id", testSnippet.id) // Non-existent test ID
        assertNull(foundTest)

        val test1 = snippetTestRepository.findBySnippetId(testSnippet.id).first { it.name == "Test 1 for Parent Snippet" }
        val foundTestOtherSnippet = snippetTestRepository.findByIdAndSnippetId(test1.id, otherSnippet.id) // Wrong snippet ID
        assertNull(foundTestOtherSnippet)
    }

    @Test
    fun `existsBySnippetIdAndName returns true if test with name exists for snippet`() {
        assertTrue(snippetTestRepository.existsBySnippetIdAndName(testSnippet.id, "Test 1 for Parent Snippet"))
        assertFalse(snippetTestRepository.existsBySnippetIdAndName(testSnippet.id, "Non Existent Test"))
    }

    @Test
    fun `save new snippet test`() {
        val newSnippetTest = SnippetTest(
            snippetId = testSnippet.id,
            name = "New Test",
            inputs = listOf("new input"),
            expectedOutputs = listOf("new output"),
            expectedStatus = TestStatus.VALID,
        )
        val savedSnippetTest = snippetTestRepository.save(newSnippetTest)
        assertNotNull(savedSnippetTest.id)
        assertEquals("New Test", savedSnippetTest.name)

        val found = snippetTestRepository.findByIdAndSnippetId(savedSnippetTest.id, testSnippet.id)
        assertNotNull(found)
    }

    @Test
    fun `update existing snippet test`() {
        val testToUpdate = snippetTestRepository.findBySnippetId(testSnippet.id).first { it.name == "Test 1 for Parent Snippet" }
        testToUpdate.name = "Updated Test Name"
        testToUpdate.expectedStatus = TestStatus.INVALID

        entityManager.flush() // Ensure changes are flushed before checking updatedAt
        entityManager.clear() // Detach entity to ensure it's reloaded from DB

        val updatedSnippetTest = snippetTestRepository.save(testToUpdate)
        val reloadedSnippetTest = snippetTestRepository.findById(updatedSnippetTest.id).orElse(null)

        assertNotNull(reloadedSnippetTest)
        assertEquals("Updated Test Name", reloadedSnippetTest?.name)
        assertEquals(TestStatus.INVALID, reloadedSnippetTest?.expectedStatus)
    }

    @Test
    fun `delete snippet test`() {
        val testToDelete = snippetTestRepository.findBySnippetId(testSnippet.id).first { it.name == "Test 1 for Parent Snippet" }
        snippetTestRepository.delete(testToDelete)

        val found = snippetTestRepository.findByIdAndSnippetId(testToDelete.id, testSnippet.id)
        assertNull(found)
    }
}
