package com.tomljump.jetbrains.config.reverse

import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SourceToTomlResolverTest : BasePlatformTestCase() {
    fun testExplicitAliasResolvesOnlyExactTomlKey() {
        myFixture.addFileToProject("app.toml", "[openai]\napi_key = \"secret\"")
        myFixture.addFileToProject("legacy.toml", "[legacy]\napiKey = \"secret\"")
        val source = configureSource(
            "config.go",
            """
            package config

            type OpenAIConfig struct {
                API<caret>Key string `toml:"api_key"`
            }
            """.trimIndent(),
        )

        val targets = SourceToTomlResolver().resolve(source.file, source.offset)

        assertEquals(listOf("api_key"), targets.map { it.text })
        assertEquals(listOf("app.toml"), targets.map { it.containingFile.name })
    }

    fun testExplicitAliasOutsideMatchingContainerStaysUnresolved() {
        myFixture.addFileToProject("database.toml", "[database]\napi_key = \"secret\"")
        val source = configureSource(
            "config.go",
            """
            package config

            type OpenAIConfig struct {
                API<caret>Key string `toml:"api_key"`
            }
            """.trimIndent(),
        )

        val targets = SourceToTomlResolver().resolve(source.file, source.offset)

        assertTrue(targets.isEmpty())
    }

    fun testNormalizedFieldRequiresAndUsesMatchingContainer() {
        myFixture.addFileToProject("app.toml", "[openai]\nbase_url = \"https://example.com\"")
        val source = configureSource(
            "config.go",
            """
            package config

            type OpenAIConfig struct {
                Base<caret>URL string
            }
            """.trimIndent(),
        )

        val targets = SourceToTomlResolver().resolve(source.file, source.offset)

        assertEquals(listOf("base_url"), targets.map { it.text })
    }

    fun testNormalizedFieldWithoutMatchingContainerStaysUnresolved() {
        myFixture.addFileToProject("app.toml", "[openai]\nbase_url = \"https://example.com\"")
        val source = configureSource(
            "config.go",
            """
            package config

            type Credentials struct {
                Base<caret>URL string
            }
            """.trimIndent(),
        )

        val targets = SourceToTomlResolver().resolve(source.file, source.offset)

        assertTrue(targets.isEmpty())
    }

    fun testNormalizedFieldWithMultipleSourceContainersStaysUnresolved() {
        myFixture.addFileToProject("app.toml", "[openai]\nbase_url = \"https://example.com\"")
        val source = configureSource(
            "config.go",
            """
            package config

            type OpenAIConfig struct {
                Model string
            }

            type Credentials struct {
                Base<caret>URL string
            }
            """.trimIndent(),
        )

        val targets = SourceToTomlResolver().resolve(source.file, source.offset)

        assertTrue(targets.isEmpty())
    }

    fun testReturnsMatchingKeysFromMultipleTomlFiles() {
        myFixture.addFileToProject("dev.toml", "[openai]\nmodel = \"dev\"")
        myFixture.addFileToProject("prod.toml", "[openai]\nmodel = \"prod\"")
        val source = configureSource(
            "config.py",
            """
            class OpenAIConfig:
                mo<caret>del: str
            """.trimIndent(),
        )

        val targets = SourceToTomlResolver().resolve(source.file, source.offset)

        assertEquals(listOf("dev.toml", "prod.toml"), targets.map { it.containingFile.name }.sorted())
        assertEquals(listOf("model", "model"), targets.map { it.text })
    }

    fun testContainerDeclarationResolvesToTopLevelTomlTable() {
        myFixture.addFileToProject("app.toml", "[openai]\nmodel = \"gpt\"")
        val source = configureSource(
            "config.ts",
            """
            interface Open<caret>AIConfig {
              model: string
            }
            """.trimIndent(),
        )

        val targets = SourceToTomlResolver().resolve(source.file, source.offset)

        assertEquals(listOf("openai"), targets.map { it.text })
    }

    fun testNestedContainerResolvesToNestedTomlTableLeaf() {
        myFixture.addFileToProject("app.toml", "[app.database]\nhost = \"localhost\"")
        val source = configureSource(
            "config.go",
            """
            package config

            type Data<caret>baseConfig struct {
                Host string
            }
            """.trimIndent(),
        )

        val targets = SourceToTomlResolver().resolve(source.file, source.offset)

        assertEquals(listOf("database"), targets.map { it.text })
    }

    fun testNestedFieldResolvesUsingNearestTomlContainer() {
        myFixture.addFileToProject("app.toml", "[app.database]\nhost = \"localhost\"")
        val source = configureSource(
            "config.go",
            """
            package config

            type DatabaseConfig struct {
                Ho<caret>st string
            }
            """.trimIndent(),
        )

        val targets = SourceToTomlResolver().resolve(source.file, source.offset)

        assertEquals(listOf("host"), targets.map { it.text })
    }

    fun testRootDottedKeyResolvesOnlyToLeafSegment() {
        myFixture.addFileToProject("app.toml", "app.database.host = \"localhost\"")
        val source = configureSource(
            "config.go",
            """
            package config

            type DatabaseConfig struct {
                Ho<caret>st string
            }
            """.trimIndent(),
        )

        val targets = SourceToTomlResolver().resolve(source.file, source.offset)

        assertEquals(listOf("host"), targets.map { it.text })
    }

    fun testArrayTableFieldResolvesToTomlKey() {
        myFixture.addFileToProject("app.toml", "[[products]]\nname = \"Hammer\"")
        val source = configureSource(
            "config.go",
            """
            package config

            type ProductsConfig struct {
                Na<caret>me string
            }
            """.trimIndent(),
        )

        val targets = SourceToTomlResolver().resolve(source.file, source.offset)

        assertEquals(listOf("name"), targets.map { it.text })
    }

    fun testNestedFieldWithMismatchedNearestContainerStaysUnresolved() {
        myFixture.addFileToProject("app.toml", "[app.database]\nhost = \"localhost\"")
        val source = configureSource(
            "config.go",
            """
            package config

            type Credentials struct {
                Ho<caret>st string
            }
            """.trimIndent(),
        )

        val targets = SourceToTomlResolver().resolve(source.file, source.offset)

        assertTrue(targets.isEmpty())
    }

    fun testPythonMethodLocalStaysUnresolved() {
        myFixture.addFileToProject("app.toml", "[openai]\nmodel = \"gpt\"")
        val source = configureSource(
            "config.py",
            """
            class OpenAIConfig:
                model: str

                def load(self):
                    mo<caret>del = "local"
            """.trimIndent(),
        )

        val targets = SourceToTomlResolver().resolve(source.file, source.offset)

        assertTrue(targets.isEmpty())
    }

    fun testJavaMethodLocalStaysUnresolved() {
        myFixture.addFileToProject("app.toml", "[openai]\nmodel = \"gpt\"")
        val source = configureSource(
            "OpenAIConfig.java",
            """
            class OpenAIConfig {
                private String model;

                void load() {
                    String mo<caret>del;
                }
            }
            """.trimIndent(),
        )

        val targets = SourceToTomlResolver().resolve(source.file, source.offset)

        assertTrue(targets.isEmpty())
    }

    fun testTypeScriptNestedObjectPropertyStaysUnresolved() {
        myFixture.addFileToProject("app.toml", "[openai]\nmodel = \"gpt\"")
        val source = configureSource(
            "config.ts",
            """
            interface OpenAIConfig {
              model: string
            }

            const runtime = {
              mo<caret>del: "local"
            }
            """.trimIndent(),
        )

        val targets = SourceToTomlResolver().resolve(source.file, source.offset)

        assertTrue(targets.isEmpty())
    }

    fun testJavaScriptNestedObjectPropertyStaysUnresolved() {
        myFixture.addFileToProject("app.toml", "[openai]\nmodel = \"gpt\"")
        val source = configureSource(
            "config.js",
            """
            const openAIConfig = {
              model: "config",
              nested: {
                mo<caret>del: "local"
              }
            }
            """.trimIndent(),
        )

        val targets = SourceToTomlResolver().resolve(source.file, source.offset)

        assertTrue(targets.isEmpty())
    }

    fun testFunctionLocalJavaScriptContainerStaysUnresolved() {
        myFixture.addFileToProject("app.toml", "[openai]\nmodel = \"gpt\"")
        val source = configureSource(
            "config.js",
            """
            function buildConfig() {
              const openAIConfig = {
                mo<caret>del: "local"
              }
              return openAIConfig
            }
            """.trimIndent(),
        )

        val targets = SourceToTomlResolver().resolve(source.file, source.offset)

        assertTrue(targets.isEmpty())
    }

    fun testCommentedGoFieldStaysUnresolved() {
        myFixture.addFileToProject("app.toml", "[openai]\nmodel = \"gpt\"")
        val source = configureSource(
            "config.go",
            """
            package config

            type OpenAIConfig struct {
                /*
                Mo<caret>del string
                */
            }
            """.trimIndent(),
        )

        val targets = SourceToTomlResolver().resolve(source.file, source.offset)

        assertTrue(targets.isEmpty())
    }

    fun testCommentedGoContainerStaysUnresolved() {
        myFixture.addFileToProject("app.toml", "[openai]\nmodel = \"gpt\"")
        val source = configureSource(
            "config.go",
            """
            package config

            /*
            type Open<caret>AIConfig struct {
                Model string
            }
            */
            """.trimIndent(),
        )

        val targets = SourceToTomlResolver().resolve(source.file, source.offset)

        assertTrue(targets.isEmpty())
    }

    fun testNestedPythonClassStaysUnresolved() {
        myFixture.addFileToProject("app.toml", "[openai]\nmodel = \"gpt\"")
        val source = configureSource(
            "config.py",
            """
            def build_config():
                class OpenAIConfig:
                    mo<caret>del: str
                return OpenAIConfig()
            """.trimIndent(),
        )

        val targets = SourceToTomlResolver().resolve(source.file, source.offset)

        assertTrue(targets.isEmpty())
    }

    fun testPythonTripleQuotedContainerStaysUnresolved() {
        myFixture.addFileToProject("app.toml", "[openai]\nmodel = \"gpt\"")
        val source = configureSource(
            "config.py",
            """
            EXAMPLE = ${"\"\"\""}
            class OpenAIConfig:
                mo<caret>del: str
            ${"\"\"\""}
            """.trimIndent(),
        )

        val targets = SourceToTomlResolver().resolve(source.file, source.offset)

        assertTrue(targets.isEmpty())
    }

    private fun configureSource(path: String, textWithCaret: String): SourceAtOffset {
        val offset = textWithCaret.indexOf(CARET)
        check(offset >= 0) { "Missing caret marker" }
        val file = myFixture.configureByText(path, textWithCaret.replace(CARET, ""))
        return SourceAtOffset(file, offset)
    }

    private data class SourceAtOffset(val file: PsiFile, val offset: Int)

    companion object {
        private const val CARET = "<caret>"
    }
}
