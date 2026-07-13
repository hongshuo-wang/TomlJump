package com.tomljump.jetbrains.config.resolver

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.tomljump.core.ConfigKeyPath
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LanguageConfigResolverTest : BasePlatformTestCase() {
    fun testTemporaryGoResolverReturnsNoTargets() {
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

        assertTrue(targets.isEmpty())
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
