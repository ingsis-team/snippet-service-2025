package com.ingsisteam.snippetservice2025.config

import com.ingsisteam.snippetservice2025.model.dto.external.Rule

object DefaultRulesConfig {

    fun getDefaultFormattingRules(): List<Rule> {
        return listOf(
            Rule(
                id = "spaceBeforeColon",
                name = "Space Before Colon",
                isActive = true,
                value = true,
            ),
            Rule(
                id = "spaceAfterColon",
                name = "Space After Colon",
                isActive = true,
                value = true,
            ),
            Rule(
                id = "equalSpaces",
                name = "Equal Spaces",
                isActive = true,
                value = true,
            ),
            Rule(
                id = "printLineBreaks",
                name = "Print Line Breaks",
                isActive = true,
                value = 1,
            ),
            Rule(
                id = "indentInsideBraces",
                name = "Indent Inside Braces",
                isActive = true,
                value = 4,
            ),
        )
    }

    fun getDefaultLintingRules(): List<Rule> {
        return listOf(
            Rule(
                id = "identifier_format",
                name = "Identifier Format",
                isActive = true,
                value = "camel case",
            ),
            Rule(
                id = "mandatory_variable_or_literal_in_println",
                name = "Mandatory Variable or Literal in Println",
                isActive = true,
                value = true,
            ),
            Rule(
                id = "mandatory_variable_or_literal_in_readInput",
                name = "Mandatory Variable or Literal in ReadInput",
                isActive = true,
                value = true,
            ),
        )
    }
}
