package com.tomljump.jetbrains.config

import com.intellij.psi.PsiReference
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TomlConfigReferenceContributorTest : BasePlatformTestCase() {
    fun testCreatesReferenceForTableName() {
        myFixture.configureByText(
            "app.toml",
            """
            [ope<caret>nai]
            api_key = "secret"
            """.trimIndent(),
        )

        val reference = referenceAtCaret() ?: error("Expected TOML table reference")

        assertEquals("openai", reference.canonicalText)
        assertTrue(reference.isSoft)
    }

    fun testCreatesReferenceForKeyInsideTable() {
        myFixture.configureByText(
            "app.toml",
            """
            [openai]
            api_<caret>key = "secret"
            """.trimIndent(),
        )

        val reference = referenceAtCaret() ?: error("Expected TOML key reference")

        assertEquals("openai.api_key", reference.canonicalText)
        assertTrue(reference.isSoft)
    }

    fun testResolvesTomlKeyToGoStructTag() {
        myFixture.addFileToProject(
            "config.go",
            """
            package config

            type OpenAIConfig struct {
                APIKey string `toml:"api_key"`
            }
            """.trimIndent(),
        )
        myFixture.configureByText(
            "app.toml",
            """
            [openai]
            api_<caret>key = "secret"
            """.trimIndent(),
        )

        val resolved = referenceAtCaret()?.resolve()

        assertEquals("APIKey", resolved?.text)
    }

    fun testDoesNotCreateReferenceInNonTomlFile() {
        myFixture.configureByText(
            "config.txt",
            """
            [ope<caret>nai]
            api_key = "secret"
            """.trimIndent(),
        )

        assertNull(referenceAtCaret())
    }

    fun testDoesNotCreateReferenceForStringValue() {
        myFixture.configureByText(
            "app.toml",
            """
            [openai]
            api_key = "sec<caret>ret"
            """.trimIndent(),
        )

        assertNull(referenceAtCaret())
    }

    private fun referenceAtCaret(): PsiReference? {
        val offset = myFixture.editor.caretModel.offset
        return myFixture.file.findReferenceAt(offset)
    }
}
