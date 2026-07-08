package com.tomljump.jetbrains

import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TomlStringValueExtractorTest : BasePlatformTestCase() {
    fun testExtractsDoubleQuotedTomlStringValue() {
        myFixture.configureByText(
            "app.toml",
            """
            schema = "./schemas/user.json"
            """.trimIndent(),
        )

        val leaf = findElementAt("./schemas/user.json")
        val extracted = firstExtractableAncestor(leaf) ?: error("Expected extractable TOML string")

        assertEquals("./schemas/user.json", extracted.value)
        assertEquals(1, extracted.rangeInElement.startOffset)
        assertEquals("\"./schemas/user.json\"".length - 1, extracted.rangeInElement.endOffset)
    }

    fun testIgnoresUnquotedNonStringValue() {
        myFixture.configureByText(
            "app.toml",
            """
            enabled = true
            """.trimIndent(),
        )

        val leaf = findElementAt("true")
        val extracted = firstExtractableAncestor(leaf)

        assertNull(extracted)
    }

    private fun findElementAt(marker: String): PsiElement {
        val offset = myFixture.file.text.indexOf(marker)
        check(offset >= 0) { "Missing marker: $marker" }
        return myFixture.file.findElementAt(offset) ?: error("No PSI element at offset $offset")
    }

    private fun firstExtractableAncestor(start: PsiElement): ExtractedTomlString? {
        var current: PsiElement? = start
        while (current != null) {
            val extracted = TomlStringValueExtractor.extract(current)
            if (extracted != null) {
                return extracted
            }
            current = current.parent
        }
        return null
    }
}
