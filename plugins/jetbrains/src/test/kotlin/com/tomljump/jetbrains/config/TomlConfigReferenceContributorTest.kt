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

    fun testCreatesReferenceForQuotedKeyInsideNestedTable() {
        myFixture.configureByText(
            "app.toml",
            """
            [app."data.base"]
            "api.<caret>key" = "secret"
            """.trimIndent(),
        )

        val reference = referenceAtCaret() ?: error("Expected quoted TOML key reference")

        assertEquals("app.data.base.api.key", reference.canonicalText)
        assertTrue(reference.isSoft)
    }

    fun testDoesNotCreateReferenceForNonLeafDottedSegment() {
        myFixture.configureByText(
            "app.toml",
            """
            [a<caret>pp.database]
            host = "localhost"
            """.trimIndent(),
        )

        assertNull(referenceAtCaret())
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

    fun testResolvesTomlTableToGoStructType() {
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
            [ope<caret>nai]
            api_key = "secret"
            """.trimIndent(),
        )

        val resolved = referenceAtCaret()?.resolve()

        assertEquals("OpenAIConfig", resolved?.text)
    }

    fun testResolvesNestedTomlTableUsingNearestContainer() {
        myFixture.addFileToProject(
            "config.go",
            """
            package config

            type DatabaseConfig struct {
                Host string
            }
            """.trimIndent(),
        )
        myFixture.configureByText(
            "app.toml",
            """
            [app.data<caret>base]
            host = "localhost"
            """.trimIndent(),
        )

        val resolved = referenceAtCaret()?.resolve()

        assertEquals("DatabaseConfig", resolved?.text)
    }

    fun testNestedTomlKeyDoesNotResolveAsContainer() {
        myFixture.addFileToProject(
            "config.go",
            """
            package config

            type AppConfig struct {
                Database string
            }

            type DatabaseConfig struct {
                Host string
            }
            """.trimIndent(),
        )
        myFixture.configureByText(
            "app.toml",
            """
            [app]
            data<caret>base = "primary"
            """.trimIndent(),
        )

        val resolved = referenceAtCaret()?.resolve()

        assertEquals("Database", resolved?.text)
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
