package com.tomljump.jetbrains.config

import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TomlConfigKeyPathExtractorTest : BasePlatformTestCase() {
    fun testExtractsTablePath() {
        myFixture.configureByText(
            "app.toml",
            """
            [openai]
            api_key = "secret"
            """.trimIndent(),
        )

        val extracted = TomlConfigKeyPathExtractor.extract(findElementAt("openai"))

        assertEquals(listOf("openai"), extracted?.keyPath?.segments)
        assertEquals("openai", extracted?.text)
    }

    fun testExtractsKeyPathInsideTable() {
        myFixture.configureByText(
            "app.toml",
            """
            [openai]
            api_key = "secret"
            """.trimIndent(),
        )

        val extracted = TomlConfigKeyPathExtractor.extract(findElementAt("api_key"))

        assertEquals(listOf("openai", "api_key"), extracted?.keyPath?.segments)
        assertEquals("api_key", extracted?.text)
    }

    fun testExtractsNestedTableKeyPath() {
        myFixture.configureByText(
            "app.toml",
            """
            [servers.production]
            host = "127.0.0.1"
            """.trimIndent(),
        )

        val extracted = TomlConfigKeyPathExtractor.extract(findElementAt("host"))

        assertEquals(listOf("servers", "production", "host"), extracted?.keyPath?.segments)
        assertEquals("host", extracted?.text)
    }

    fun testExtractsKeyPathBelowTableWithTrailingComment() {
        myFixture.configureByText(
            "app.toml",
            """
            [servers.production] # production server
            host = "127.0.0.1"
            """.trimIndent(),
        )

        val extracted = TomlConfigKeyPathExtractor.extract(findElementAt("host"))

        assertEquals(listOf("servers", "production", "host"), extracted?.keyPath?.segments)
        assertEquals("host", extracted?.text)
    }

    fun testExtractsFullDottedTablePathFromEitherTableSegment() {
        myFixture.configureByText(
            "app.toml",
            """
            [servers.production]
            host = "127.0.0.1"
            """.trimIndent(),
        )

        val servers = TomlConfigKeyPathExtractor.extract(findElementAt("servers"))
        val production = TomlConfigKeyPathExtractor.extract(findElementAt("production"))

        assertEquals(listOf("servers", "production"), servers?.keyPath?.segments)
        assertEquals("servers", servers?.text)
        assertEquals(0, servers?.rangeInElement?.startOffset)
        assertEquals("servers".length, servers?.rangeInElement?.endOffset)
        assertEquals(listOf("servers", "production"), production?.keyPath?.segments)
        assertEquals("production", production?.text)
        assertEquals(0, production?.rangeInElement?.startOffset)
        assertEquals("production".length, production?.rangeInElement?.endOffset)
    }

    fun testMalformedDottedTableDoesNotReusePreviousTable() {
        myFixture.configureByText(
            "app.toml",
            """
            [openai]
            model = "deepseek"
            [servers.]
            host = "127.0.0.1"
            """.trimIndent(),
        )

        val table = TomlConfigKeyPathExtractor.extract(findElementAt("servers"))
        val key = TomlConfigKeyPathExtractor.extract(findElementAt("host"))

        assertNull(table)
        assertEquals(listOf("host"), key?.keyPath?.segments)
    }

    fun testIgnoresStringValue() {
        myFixture.configureByText(
            "app.toml",
            """
            [openai]
            api_key = "secret"
            """.trimIndent(),
        )

        assertNull(TomlConfigKeyPathExtractor.extract(findElementAt("secret")))
    }

    private fun findElementAt(marker: String): PsiElement {
        val offset = myFixture.file.text.indexOf(marker)
        check(offset >= 0) { "Missing marker: $marker" }
        return myFixture.file.findElementAt(offset) ?: error("No PSI element at offset $offset")
    }
}
