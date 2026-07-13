package com.tomljump.jetbrains.config.resolver

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.tomljump.core.ConfigKeyPath
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals

class LanguageConfigResolverTest : BasePlatformTestCase() {
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

    fun testTargetResolverRethrowsProcessCanceledException() {
        myFixture.configureByText("app.toml", "[openai]")
        val resolver = TomlConfigTargetResolver(
            resolvers = listOf(
                object : LanguageConfigResolver {
                    override fun resolve(project: Project, keyPath: ConfigKeyPath): List<PsiElement> {
                        throw ProcessCanceledException()
                    }
                },
            ),
        )

        assertFailsWith<ProcessCanceledException> {
            resolver.resolve(myFixture.file, ConfigKeyPath.of("openai"))
        }
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
            override fun findTargets(sourceText: String, keyPath: ConfigKeyPath): List<ConfigSourceTarget> {
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
