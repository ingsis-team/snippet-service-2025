package com.ingsisteam.snippetservice2025.exception

data class SyntaxValidationException(
    val rule: String,
    val line: Int,
    val column: Int,
    override val message: String,
) : RuntimeException("Syntax error at line $line, column $column: $message. Rule violated: $rule")
