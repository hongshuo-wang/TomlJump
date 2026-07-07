package com.tomljump.jetbrains

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.ext.TomlLiteralKind
import org.toml.lang.psi.ext.kind

data class ExtractedTomlString(
    val value: String,
    val rangeInElement: TextRange,
)

object TomlStringValueExtractor {
    fun extract(element: PsiElement): ExtractedTomlString? {
        if (element !is TomlLiteral || element.kind !is TomlLiteralKind.String) {
            return null
        }

        val text = element.text ?: return null
        if (text.length < 2) {
            return null
        }

        if (text.startsWith("\"\"\"") || text.startsWith("'''")) {
            return null
        }

        val quote = text.first()
        if ((quote != '"' && quote != '\'') || text.last() != quote) {
            return null
        }

        return ExtractedTomlString(
            value = text.substring(1, text.lastIndex),
            rangeInElement = TextRange.from(1, text.length - 2),
        )
    }
}
