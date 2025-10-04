package com.ingsisteam.snippetservice2025.model.entity

import com.ingsisteam.snippetservice2025.model.enum.SnippetLanguage
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "snippets")
data class Snippet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var language: SnippetLanguage,

    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String,

    @Column(name = "user_id", nullable = false)
    var userId: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    // Constructor sin argumentos requerido por JPA
    constructor() : this(
        name = "",
        description = "",
        language = SnippetLanguage.PRINTSCRIPT,
        content = "",
        userId = "",
    )

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
