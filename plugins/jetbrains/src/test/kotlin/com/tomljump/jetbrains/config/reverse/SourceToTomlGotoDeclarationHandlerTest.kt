package com.tomljump.jetbrains.config.reverse

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.navigation.NavigationItem
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SourceToTomlGotoDeclarationHandlerTest : BasePlatformTestCase() {
    fun testReturnsTomlTargetAtSupportedSourceDeclaration() {
        myFixture.addFileToProject("app.toml", "[openai]\napi_key = \"secret\"")
        myFixture.configureByText(
            "config.go",
            """
            package config

            type OpenAIConfig struct {
                API<caret>Key string `toml:"api_key"`
            }
            """.trimIndent(),
        )

        val targets = targetsAtCaret()

        assertEquals(listOf("api_key"), targets.map { it.text })
        assertEquals(listOf("app.toml"), targets.map { it.containingFile.name })
    }

    fun testReturnsNoTargetsForArbitrarySourceIdentifier() {
        myFixture.addFileToProject("app.toml", "[openai]\napi_key = \"secret\"")
        myFixture.configureByText(
            "config.go",
            """
            package config

            func API<caret>Key() string {
                return "value"
            }
            """.trimIndent(),
        )

        assertTrue(targetsAtCaret().isEmpty())
    }

    fun testMultipleTargetsExposeSemanticAndFilePresentation() {
        myFixture.addFileToProject(
            "config/multiple-primary.toml",
            "[multi_target]\nshared_token = \"primary\"",
        )
        myFixture.addFileToProject(
            "config/multiple-secondary.toml",
            "[multi_target]\nshared_token = \"secondary\"",
        )
        myFixture.configureByText(
            "config.go",
            """
            package config

            type MultiTarget<caret>Config struct {
                SharedToken string `toml:"shared_token"`
            }
            """.trimIndent(),
        )

        val targets = targetsAtCaret().sortedBy { it.containingFile.name }
        val presentations = targets.map { target ->
            (target as NavigationItem).presentation ?: error("Missing navigation presentation")
        }

        assertEquals(listOf("[multi_target]", "[multi_target]"), presentations.map { it.presentableText })
        assertEquals(
            listOf("config/multiple-primary.toml", "config/multiple-secondary.toml"),
            presentations.map { it.locationString },
        )
        assertEquals(
            listOf("multiple-primary.toml", "multiple-secondary.toml"),
            targets.map { it.navigationElement.containingFile.name },
        )
    }

    fun testKeyTargetExposesFullSemanticPathAndFilePresentation() {
        myFixture.addFileToProject("config/app.toml", "[openai]\napi_key = \"secret\"")
        myFixture.configureByText(
            "config.go",
            """
            package config

            type OpenAIConfig struct {
                API<caret>Key string `toml:"api_key"`
            }
            """.trimIndent(),
        )

        val target = targetsAtCaret().single()
        val presentation = (target as NavigationItem).presentation
            ?: error("Missing navigation presentation")

        assertEquals("openai.api_key", presentation.presentableText)
        assertEquals("config/app.toml", presentation.locationString)
        assertEquals("api_key", target.navigationElement.text)
        assertEquals("app.toml", target.navigationElement.containingFile.name)
    }

    fun testResolvesWhenCaretOffsetIsAtIdentifierEnd() {
        myFixture.addFileToProject("app.toml", "[openai]\napi_key = \"secret\"")
        myFixture.configureByText(
            "config.go",
            """
            package config

            type OpenAIConfig struct {
                APIKey<caret> string `toml:"api_key"`
            }
            """.trimIndent(),
        )
        val identifier = myFixture.file.findElementAt(myFixture.caretOffset - 1)
            ?: error("Missing source identifier before caret")

        val targets = SourceToTomlGotoDeclarationHandler()
            .getGotoDeclarationTargets(
                identifier,
                myFixture.caretOffset,
                myFixture.editor,
            )
            .orEmpty()
            .toList()

        assertEquals(listOf("api_key"), targets.map { it.text })
    }

    fun testHandlerIsRegistered() {
        assertTrue(
            GotoDeclarationHandler.EP_NAME.extensionList.any {
                it is SourceToTomlGotoDeclarationHandler
            },
        )
    }

    private fun targetsAtCaret() = SourceToTomlGotoDeclarationHandler()
        .getGotoDeclarationTargets(
            myFixture.file.findElementAt(myFixture.caretOffset) ?: error("Missing source element at caret"),
            myFixture.caretOffset,
            myFixture.editor,
        )
        .orEmpty()
        .toList()
}
