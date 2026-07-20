package com.tomljump.jetbrains.config.resolver

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.tomljump.core.ConfigKeyPath
import com.tomljump.jetbrains.config.TomlConfigReferenceKind
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LanguageConfigResolverTest : BasePlatformTestCase() {
    fun testGoResolverExposesConfigDeclarationsForReverseNavigation() {
        val source = """
            package config

            type OpenAIConfig struct {
                APIKey string `toml:"api_key" json:"apiKey"`
            }
        """.trimIndent()

        val declarations = GoConfigResolver().declarationsIn(source)

        val container = declarations.single { it.label == "OpenAIConfig" }
        assertEquals(ConfigSourceDeclarationKind.CONTAINER, container.kind)
        assertTrue(container.aliases.isEmpty())

        val field = declarations.single { it.label == "APIKey" }
        assertEquals(ConfigSourceDeclarationKind.FIELD, field.kind)
        assertEquals(setOf("api_key", "apiKey"), field.aliases)
    }

    fun testSourceResolverReportsSupportedFileExtensions() {
        val resolver = TypeScriptConfigResolver()

        assertTrue(resolver.supportsExtension("ts"))
        assertTrue(resolver.supportsExtension("TSX"))
        assertFalse(resolver.supportsExtension("js"))
        assertFalse(resolver.supportsExtension(null))
    }

    fun testGoResolverFindsTomlTag() {
        myFixture.addFileToProject(
            "config.go",
            """
            package config

            type OpenAIConfig struct {
                APIKey string `toml:"api_key"`
            }
            """.trimIndent(),
        )

        val targets = GoConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai", "api_key"))

        assertEquals(listOf("APIKey"), targets.map { it.text })
    }

    fun testGoResolverFindsNormalizedFieldName() {
        myFixture.addFileToProject(
            "config.go",
            """
            package config

            type OpenAIConfig struct {
                BaseURL string
            }
            """.trimIndent(),
        )

        val targets = GoConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai", "base_url"))

        assertEquals(listOf("BaseURL"), targets.map { it.text })
    }

    fun testGoResolverFindsStructTypeForTable() {
        myFixture.addFileToProject(
            "config.go",
            """
            package config

            type OpenAIConfig struct {
                APIKey string
            }
            """.trimIndent(),
        )

        val targets = GoConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai"))

        assertEquals(listOf("OpenAIConfig"), targets.map { it.text })
    }

    fun testPythonResolverFindsAnnotatedField() {
        myFixture.addFileToProject(
            "config.py",
            """
            from dataclasses import dataclass

            @dataclass
            class OpenAIConfig:
                api_key: str
            """.trimIndent(),
        )

        val targets = PythonConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai", "api_key"))

        assertEquals(listOf("api_key"), targets.map { it.text })
    }

    fun testPythonResolverNavigationElementKeepsFieldOffset() {
        val file = myFixture.addFileToProject(
            "config.py",
            """
            class OpenAIConfig:
                api_key: str
            """.trimIndent(),
        )
        val expectedOffset = file.text.indexOf("api_key")

        val target = PythonConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai", "api_key")).single()

        assertEquals(expectedOffset, target.textOffset)
        assertEquals(expectedOffset, target.navigationElement.textOffset)
    }

    fun testPythonResolverFindsClassForTable() {
        myFixture.addFileToProject(
            "config.py",
            """
            class OpenAIConfig:
                api_key: str
            """.trimIndent(),
        )

        val targets = PythonConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai"))

        assertEquals(listOf("OpenAIConfig"), targets.map { it.text })
    }

    fun testPythonResolverFindsFieldAfterMultilineClassHeader() {
        myFixture.addFileToProject(
            "config.py",
            """
            class OpenAIConfig(
                BaseSettings,
            ):
                model: str
            """.trimIndent(),
        )

        val targets = PythonConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai", "model"))

        assertEquals(listOf("model"), targets.map { it.text })
    }

    fun testJavaResolverFindsJsonPropertyAnnotation() {
        myFixture.addFileToProject(
            "OpenAIConfig.java",
            """
            package config;

            class OpenAIConfig {
                @JsonProperty("api_key")
                private String apiKey;
            }
            """.trimIndent(),
        )

        val targets = JavaConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai", "api_key"))

        assertEquals(listOf("apiKey"), targets.map { it.text })
    }

    fun testJavaResolverNavigationElementKeepsFieldOffset() {
        val file = myFixture.addFileToProject(
            "OpenAIConfig.java",
            """
            package config;

            class OpenAIConfig {
                @JsonProperty("api_key")
                private String apiKey;
            }
            """.trimIndent(),
        )
        val expectedOffset = file.text.indexOf("apiKey")

        val target = JavaConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai", "api_key")).single()

        assertEquals(expectedOffset, target.textOffset)
        assertEquals(expectedOffset, target.navigationElement.textOffset)
    }

    fun testJavaResolverFindsClassForTable() {
        myFixture.addFileToProject(
            "OpenAIConfig.java",
            """
            package config;

            class OpenAIConfig {
                private String apiKey;
            }
            """.trimIndent(),
        )

        val targets = JavaConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai"))

        assertEquals(listOf("OpenAIConfig"), targets.map { it.text })
    }

    fun testTypeScriptResolverFindsInterfaceProperty() {
        myFixture.addFileToProject(
            "config.ts",
            """
            interface OpenAIConfig {
                apiKey: string
            }
            """.trimIndent(),
        )

        val targets = TypeScriptConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai", "api_key"))

        assertEquals(listOf("apiKey"), targets.map { it.text })
    }

    fun testTypeScriptResolverNavigationElementKeepsPropertyOffset() {
        val file = myFixture.addFileToProject(
            "config.ts",
            """
            interface OpenAIConfig {
                apiKey: string
            }
            """.trimIndent(),
        )
        val expectedOffset = file.text.indexOf("apiKey")

        val target = TypeScriptConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai", "api_key")).single()

        assertEquals(expectedOffset, target.textOffset)
        assertEquals(expectedOffset, target.navigationElement.textOffset)
    }

    fun testTypeScriptResolverFindsInterfaceForTable() {
        myFixture.addFileToProject(
            "config.ts",
            """
            interface OpenAIConfig {
                apiKey: string
            }
            """.trimIndent(),
        )

        val targets = TypeScriptConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai"))

        assertEquals(listOf("OpenAIConfig"), targets.map { it.text })
    }

    fun testJavaScriptResolverFindsQuotedObjectProperty() {
        myFixture.addFileToProject(
            "config.js",
            """
            const openai = {
                "api_key": ""
            }
            """.trimIndent(),
        )

        val targets = JavaScriptConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai", "api_key"))

        assertEquals(listOf("api_key"), targets.map { it.text.trim('"') })
    }

    fun testJavaScriptResolverNavigationElementKeepsPropertyOffset() {
        val file = myFixture.addFileToProject(
            "config.js",
            """
            const openai = {
                "api_key": ""
            }
            """.trimIndent(),
        )
        val expectedOffset = file.text.indexOf("api_key")

        val target = JavaScriptConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai", "api_key")).single()

        assertEquals(expectedOffset, target.textOffset)
        assertEquals(expectedOffset, target.navigationElement.textOffset)
    }

    fun testJavaScriptResolverFindsObjectForTable() {
        myFixture.addFileToProject(
            "config.js",
            """
            const openaiConfig = {
                "api_key": ""
            }
            """.trimIndent(),
        )

        val targets = JavaScriptConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("openai"))

        assertEquals(listOf("openaiConfig"), targets.map { it.text })
    }

    fun testPythonResolverUsesOwningContainerForDuplicateFieldNames() {
        val file = myFixture.addFileToProject(
            "config.py",
            """
            class OpenAIConfig:
                token: str

            class DatabaseConfig:
                token: str
            """.trimIndent(),
        )

        val target = PythonConfigResolver().resolve(
            myFixture.project,
            ConfigKeyPath.of("database", "token"),
        ).single()

        assertEquals(file.text.lastIndexOf("token"), target.textOffset)
    }

    fun testJavaResolverUsesOwningContainerForDuplicateFieldNames() {
        val file = myFixture.addFileToProject(
            "Config.java",
            """
            class OpenAIConfig {
                String token;
            }

            class DatabaseConfig {
                String token;
            }
            """.trimIndent(),
        )

        val target = JavaConfigResolver().resolve(
            myFixture.project,
            ConfigKeyPath.of("database", "token"),
        ).single()

        assertEquals(file.text.lastIndexOf("token"), target.textOffset)
    }

    fun testTypeScriptResolverUsesOwningContainerForDuplicateFieldNames() {
        val file = myFixture.addFileToProject(
            "config.ts",
            """
            interface OpenAIConfig {
                token: string
            }

            interface DatabaseConfig {
                token: string
            }
            """.trimIndent(),
        )

        val target = TypeScriptConfigResolver().resolve(
            myFixture.project,
            ConfigKeyPath.of("database", "token"),
        ).single()

        assertEquals(file.text.lastIndexOf("token"), target.textOffset)
    }

    fun testJavaScriptResolverUsesOwningContainerForDuplicateFieldNames() {
        val file = myFixture.addFileToProject(
            "config.js",
            """
            const openAIConfig = {
                token: "openai"
            }

            const databaseConfig = {
                token: "database"
            }
            """.trimIndent(),
        )

        val target = JavaScriptConfigResolver().resolve(
            myFixture.project,
            ConfigKeyPath.of("database", "token"),
        ).single()

        assertEquals(file.text.lastIndexOf("token"), target.textOffset)
    }

    fun testPythonResolverIgnoresDeclarationLikeTextInsideTripleQuotedString() {
        val tripleQuote = "\"\"\""
        myFixture.addFileToProject(
            "config.py",
            """
            documentation = ${tripleQuote}A single " quote is valid inside a triple-quoted string.
            class DatabaseConfig:
                token: str
            $tripleQuote
            """.trimIndent(),
        )

        val targets = PythonConfigResolver().resolve(
            myFixture.project,
            ConfigKeyPath.of("database", "token"),
        )

        assertTrue(targets.isEmpty())
    }

    fun testJavaResolverIgnoresDeclarationLikeTextInsideTextBlock() {
        val tripleQuote = "\"\"\""
        myFixture.addFileToProject(
            "Config.java",
            """
            class DatabaseConfig {
                String example = $tripleQuote
                    A single " quote is valid inside a text block.
                    String token;
                    $tripleQuote;
            }
            """.trimIndent(),
        )

        val targets = JavaConfigResolver().resolve(
            myFixture.project,
            ConfigKeyPath.of("database", "token"),
        )

        assertTrue(targets.isEmpty())
    }

    fun testTargetResolverRethrowsProcessCanceledException() {
        myFixture.configureByText("app.toml", "[openai]")
        val resolver = TomlConfigTargetResolver(
            resolvers = listOf(
                object : LanguageConfigResolver {
                    override fun resolve(
                        project: Project,
                        keyPath: ConfigKeyPath,
                        kind: TomlConfigReferenceKind?,
                    ): List<PsiElement> {
                        throw ProcessCanceledException()
                    }
                },
            ),
        )

        assertFailsWith<ProcessCanceledException> {
            resolver.resolve(myFixture.file, ConfigKeyPath.of("openai"))
        }
    }

    fun testTargetResolverPrefersTargetsInFileContainingMatchingTableTarget() {
        myFixture.addFileToProject(
            "go/config.go",
            """
            package config

            type GoServiceConfig struct {
                Schema string `toml:"schema"`
            }
            """.trimIndent(),
        )
        myFixture.addFileToProject(
            "typescript/config.ts",
            """
            export interface TypeScriptServiceConfig {
                schema: string
            }
            """.trimIndent(),
        )
        myFixture.configureByText("app.toml", "[typescript_service]\nschema = \"./schemas/user.json\"")

        val targets = TomlConfigTargetResolver().resolve(myFixture.file, ConfigKeyPath.of("typescript_service", "schema"))

        assertEquals(listOf(true), targets.map { it.containingFile.virtualFile.path.endsWith("/typescript/config.ts") })
        assertEquals(listOf("schema"), targets.map { it.text })
    }

    fun testTargetResolverStaysUnresolvedWhenNoMatchingTableTargetExists() {
        myFixture.addFileToProject(
            "go/config.go",
            """
            package config

            type AppConfig struct {
                Schema string `toml:"schema"`
            }
            """.trimIndent(),
        )
        myFixture.configureByText("app.toml", "[unknown]\nschema = \"./schemas/user.json\"")

        val targets = TomlConfigTargetResolver().resolve(myFixture.file, ConfigKeyPath.of("unknown", "schema"))

        assertTrue(targets.isEmpty())
    }

    fun testFieldResolutionUsesOwningContainerInSameFile() {
        myFixture.addFileToProject(
            "go/config.go",
            """
            package config

            type OpenAIConfig struct {
                APIKey string `toml:"api_key"`
            }

            type DatabaseConfig struct {
                DatabaseAPIKey string `toml:"api_key"`
            }
            """.trimIndent(),
        )

        val targets = GoConfigResolver().resolve(myFixture.project, ConfigKeyPath.of("database", "api_key"))

        assertEquals(listOf("DatabaseAPIKey"), targets.map { it.text })
    }

    fun testNestedTableUsesNearestContainerName() {
        myFixture.addFileToProject(
            "config.go",
            """
            package config

            type DatabaseConfig struct {
                Host string
            }
            """.trimIndent(),
        )

        val targets = GoConfigResolver().resolve(
            myFixture.project,
            ConfigKeyPath.of("app", "database"),
            TomlConfigReferenceKind.TABLE,
        )

        assertEquals(listOf("DatabaseConfig"), targets.map { it.text })
    }

    fun testNestedFieldUsesNearestContainerName() {
        myFixture.addFileToProject(
            "config.go",
            """
            package config

            type DatabaseConfig struct {
                Host string
            }
            """.trimIndent(),
        )

        val targets = GoConfigResolver().resolve(
            myFixture.project,
            ConfigKeyPath.of("app", "database", "host"),
        )

        assertEquals(listOf("Host"), targets.map { it.text })
    }

    fun testNestedFieldWithMismatchedNearestContainerStaysUnresolved() {
        myFixture.addFileToProject(
            "config.go",
            """
            package config

            type Credentials struct {
                Host string
            }
            """.trimIndent(),
        )

        val targets = GoConfigResolver().resolve(
            myFixture.project,
            ConfigKeyPath.of("app", "database", "host"),
        )

        assertTrue(targets.isEmpty())
    }

    fun testSourcePatternResolverIgnoresInvalidOffsetsAndDeduplicatesTargets() {
        myFixture.addFileToProject(
            "config.go",
            """
            package config

            type OpenAIConfig struct {
                APIKey string
            }
            """.trimIndent(),
        )
        val resolver = object : SourcePatternResolver(setOf("go")) {
            override fun findTargets(
                sourceText: String,
                keyPath: ConfigKeyPath,
                kind: TomlConfigReferenceKind?,
            ): List<ConfigSourceTarget> {
                val offset = sourceText.indexOf("APIKey")
                return listOf(
                    ConfigSourceTarget(-1, "before-start"),
                    ConfigSourceTarget(offset, "APIKey"),
                    ConfigSourceTarget(offset, "APIKey duplicate"),
                    ConfigSourceTarget(sourceText.length, "eof"),
                )
            }
        }

        val targets = resolver.resolve(myFixture.project, ConfigKeyPath.of("openai", "api_key"))

        assertEquals(listOf("APIKey"), targets.map { it.text })
    }
}
