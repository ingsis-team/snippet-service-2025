package com.ingsisteam.snippetservice2025.model.entity

import com.ingsisteam.snippetservice2025.model.enum.TestStatus
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "snippet_tests")
data class SnippetTest(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String = "",

    @Column(name = "snippet_id", nullable = false)
    val snippetId: String,

    @Column(nullable = false)
    var name: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "test_inputs", joinColumns = [JoinColumn(name = "test_id")])
    @Column(name = "input_value", columnDefinition = "TEXT")
    var inputs: List<String> = mutableListOf(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "test_outputs", joinColumns = [JoinColumn(name = "test_id")])
    @Column(name = "output_value", columnDefinition = "TEXT")
    var expectedOutputs: List<String> = mutableListOf(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var expectedStatus: TestStatus,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    constructor() : this(
        snippetId = "",
        name = "",
        inputs = mutableListOf(),
        expectedOutputs = mutableListOf(),
        expectedStatus = TestStatus.VALID,
    )
}
