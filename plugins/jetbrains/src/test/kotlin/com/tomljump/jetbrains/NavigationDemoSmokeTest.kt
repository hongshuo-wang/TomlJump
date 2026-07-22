package com.tomljump.jetbrains

import com.intellij.psi.PsiReference
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.tomljump.jetbrains.config.reverse.SourceToTomlGotoDeclarationHandler
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals

class NavigationDemoSmokeTest : BasePlatformTestCase() {
    fun testDemoTomlKeyResolvesToGoSource() {
        myFixture.addFileToProject("go/config.go", demoText("go/config.go"))
        myFixture.configureByText("app.toml", withCaret(demoText("app.toml"), "go_token"))

        val resolved = referenceAtCaret()?.resolve() ?: error("Expected demo TOML key to resolve")

        assertEquals("GoToken", resolved.text)
        assertEquals("config.go", resolved.containingFile.name)
    }

    fun testDemoGoSourceResolvesBackToToml() {
        myFixture.addFileToProject("app.toml", demoText("app.toml"))
        myFixture.configureByText(
            "config.go",
            withCaret(demoText("go/config.go"), "GoToken", useLastOccurrence = true),
        )

        val targets = SourceToTomlGotoDeclarationHandler()
            .getGotoDeclarationTargets(
                myFixture.file.findElementAt(myFixture.caretOffset) ?: error("Missing demo source element"),
                myFixture.caretOffset,
                myFixture.editor,
            )
            .orEmpty()

        assertEquals(listOf("go_token"), targets.map { it.text })
        assertEquals(listOf("app.toml"), targets.map { it.containingFile.name })
    }

    fun testDemoRelativePathResolvesToProjectFile() {
        myFixture.addFileToProject("schemas/user.json", demoText("schemas/user.json"))
        myFixture.configureByText(
            "app.toml",
            withCaret(demoText("app.toml"), "./schemas/user.json"),
        )

        val resolved = referenceAtCaret()?.resolve() ?: error("Expected demo file path to resolve")

        assertEquals("user.json", resolved.containingFile.name)
    }

    fun testDemoPyProjectCallableResolvesToPythonSource() {
        myFixture.addFileToProject("python/cli.py", demoText("python/cli.py"))
        myFixture.configureByText(
            "pyproject.toml",
            withCaret(demoText("pyproject.toml"), "main"),
        )

        val resolved = referenceAtCaret()?.resolve() ?: error("Expected demo callable to resolve")

        assertEquals("main", resolved.text)
        assertEquals("cli.py", resolved.containingFile.name)
    }

    private fun referenceAtCaret(): PsiReference? {
        return myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
    }

    private fun demoText(relativePath: String): String {
        return Files.readString(demoRoot.resolve(relativePath))
    }

    private fun withCaret(text: String, needle: String, useLastOccurrence: Boolean = false): String {
        val index = if (useLastOccurrence) text.lastIndexOf(needle) else text.indexOf(needle)
        check(index >= 0) { "Missing '$needle' in navigation demo" }
        val caretOffset = index + needle.length / 2
        return text.substring(0, caretOffset) + "<caret>" + text.substring(caretOffset)
    }

    companion object {
        private val demoRoot: Path by lazy {
            generateSequence(Path.of("").toAbsolutePath().normalize()) { it.parent }
                .map { it.resolve("examples/navigation-demo") }
                .firstOrNull { Files.isDirectory(it) }
                ?: error("Cannot locate examples/navigation-demo from ${Path.of("").toAbsolutePath()}")
        }
    }
}
