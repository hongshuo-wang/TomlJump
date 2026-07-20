package com.tomljump.jetbrains

import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.tomljump.core.ConfigKeyPath
import com.tomljump.jetbrains.config.resolver.GoConfigResolver
import com.tomljump.jetbrains.config.reverse.SourceToTomlResolver
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavigationPerformanceTest : BasePlatformTestCase() {
    fun testForwardNavigationWithinLatencyBudgetFor250SourceFiles() {
        repeat(CANDIDATE_FILE_COUNT - 1) { index ->
            myFixture.addFileToProject(
                "generated/source-$index.go",
                "package generated\n\ntype Unrelated${index}Config struct {\n    Value string\n}",
            )
        }
        myFixture.addFileToProject(
            "generated/database.go",
            """
            package generated

            type DatabaseConfig struct {
                Host string
            }
            """.trimIndent(),
        )
        val resolver = GoConfigResolver()
        var targets: List<PsiElement> = emptyList()

        val coldMillis = measureTimeMillis {
            targets = resolver.resolve(project, ConfigKeyPath.of("database", "host"))
        }
        val warmMillis = measureTimeMillis {
            targets = resolver.resolve(project, ConfigKeyPath.of("database", "host"))
        }

        assertEquals(listOf("Host"), targets.map { it.text })
        assertWithinBudget("cold forward", coldMillis, COLD_NAVIGATION_BUDGET_MS)
        assertWithinBudget("warm forward", warmMillis, WARM_NAVIGATION_BUDGET_MS)
    }

    fun testReverseNavigationWithinLatencyBudgetFor250TomlFiles() {
        repeat(CANDIDATE_FILE_COUNT - 1) { index ->
            myFixture.addFileToProject(
                "generated/config-$index.toml",
                "[unrelated_$index]\nvalue = \"$index\"",
            )
        }
        myFixture.addFileToProject("generated/database.toml", "[database]\nhost = \"localhost\"")
        val source = myFixture.configureByText(
            "config.go",
            """
            package config

            type DatabaseConfig struct {
                Ho<caret>st string
            }
            """.trimIndent(),
        )
        val sourceOffset = myFixture.editor.caretModel.offset
        val resolver = SourceToTomlResolver()
        var targets: List<PsiElement> = emptyList()

        val coldMillis = measureTimeMillis {
            targets = resolver.resolve(source, sourceOffset)
        }
        val warmMillis = measureTimeMillis {
            targets = resolver.resolve(source, sourceOffset)
        }

        assertEquals(listOf("host"), targets.map { it.text })
        assertWithinBudget("cold reverse", coldMillis, COLD_NAVIGATION_BUDGET_MS)
        assertWithinBudget("warm reverse", warmMillis, WARM_NAVIGATION_BUDGET_MS)
    }

    private fun assertWithinBudget(label: String, actualMillis: Long, budgetMillis: Long) {
        assertTrue(
            actualMillis <= budgetMillis,
            "$label navigation took ${actualMillis}ms; budget is ${budgetMillis}ms",
        )
    }

    companion object {
        private const val CANDIDATE_FILE_COUNT = 250
        private const val COLD_NAVIGATION_BUDGET_MS = 2_000L
        private const val WARM_NAVIGATION_BUDGET_MS = 1_000L
    }
}
