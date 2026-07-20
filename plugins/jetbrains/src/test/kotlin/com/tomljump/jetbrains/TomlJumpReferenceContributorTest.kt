package com.tomljump.jetbrains

import com.intellij.psi.PsiReference
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TomlJumpReferenceContributorTest : BasePlatformTestCase() {
    fun testResolvesRelativeFilePathFromTomlString() {
        myFixture.addFileToProject("schemas/user.json", """{"type":"object"}""")
        myFixture.configureByText(
            "app.toml",
            """
            schema = "./sche<caret>mas/user.json"
            """.trimIndent(),
        )

        val reference = referenceAtCaret()
        val resolved = reference?.resolve() ?: error("Expected reference to resolve")

        assertNotNull(reference)
        assertEquals("user.json", resolved.containingFile.name)
    }

    fun testResolvesRelativeFilePathFromUppercaseTomlExtension() {
        myFixture.addFileToProject("schemas/user.json", """{"type":"object"}""")
        myFixture.configureByText(
            "APP.TOML",
            """
            schema = "./sche<caret>mas/user.json"
            """.trimIndent(),
        )

        val reference = referenceAtCaret()
        val resolved = reference?.resolve() ?: error("Expected reference to resolve")

        assertNotNull(reference)
        assertEquals("user.json", resolved.containingFile.name)
    }

    fun testDoesNotCreateReferenceForArbitraryProseString() {
        myFixture.configureByText(
            "app.toml",
            """
            message = "hello <caret>world"
            """.trimIndent(),
        )

        assertNull(referenceAtCaret())
    }

    fun testDoesNotCreateReferenceForUnsupportedJavaStyleSymbolString() {
        myFixture.configureByText(
            "app.toml",
            """
            factory = "com.example.Pay<caret>mentClient"
            """.trimIndent(),
        )

        assertNull(referenceAtCaret())
    }

    fun testDoesNotCreateReferenceForUnsupportedCallableString() {
        myFixture.configureByText(
            "app.toml",
            """
            handler = "app.user:cre<caret>ate_user"
            """.trimIndent(),
        )

        assertNull(referenceAtCaret())
    }

    fun testResolvesPyProjectScriptModuleSegment() {
        myFixture.addFileToProject(
            "package/module.py",
            """
            def main():
                return 0
            """.trimIndent(),
        )
        myFixture.configureByText(
            "pyproject.toml",
            """
            [project.scripts]
            cli = "package.mo<caret>dule:main"
            """.trimIndent(),
        )

        val resolved = referenceAtCaret()?.resolve() ?: error("Expected module reference to resolve")

        assertEquals("module.py", resolved.containingFile.name)
    }

    fun testResolvesPyProjectScriptCallableSegment() {
        val file = myFixture.addFileToProject(
            "package/module.py",
            """
            def main():
                return 0
            """.trimIndent(),
        )
        myFixture.configureByText(
            "pyproject.toml",
            """
            [project.scripts]
            cli = "package.module:ma<caret>in"
            """.trimIndent(),
        )

        val resolved = referenceAtCaret()?.resolve() ?: error("Expected callable reference to resolve")

        assertEquals("main", resolved.text)
        assertEquals("module.py", resolved.containingFile.name)
        assertEquals(file.text.indexOf("main"), resolved.textOffset)
    }

    fun testResolvesAsyncPyProjectScriptCallable() {
        myFixture.addFileToProject(
            "package/module.py",
            """
            async def main():
                return 0
            """.trimIndent(),
        )
        myFixture.configureByText(
            "pyproject.toml",
            """
            [project.scripts]
            cli = "package.module:ma<caret>in"
            """.trimIndent(),
        )

        val resolved = referenceAtCaret()?.resolve() ?: error("Expected async callable reference to resolve")

        assertEquals("main", resolved.text)
    }

    fun testResolvesPyProjectScriptPackageModule() {
        myFixture.addFileToProject(
            "package/commands/__init__.py",
            """
            def main():
                return 0
            """.trimIndent(),
        )
        myFixture.configureByText(
            "pyproject.toml",
            """
            [project.scripts]
            cli = "package.comm<caret>ands:main"
            """.trimIndent(),
        )

        val resolved = referenceAtCaret()?.resolve() ?: error("Expected package module reference to resolve")

        assertEquals("__init__.py", resolved.containingFile.name)
    }

    fun testResolvesSingleSegmentPyProjectScriptModule() {
        myFixture.addFileToProject("cli.py", "def main():\n    return 0")
        myFixture.configureByText(
            "pyproject.toml",
            """
            [project.scripts]
            cli = "c<caret>li:main"
            """.trimIndent(),
        )

        val resolved = referenceAtCaret()?.resolve() ?: error("Expected single-segment module to resolve")

        assertEquals("cli.py", resolved.containingFile.name)
    }

    fun testReturnsMultiplePyProjectScriptModuleTargets() {
        myFixture.addFileToProject("root-a/package/module.py", "def main():\n    return 0")
        myFixture.addFileToProject("root-b/package/module.py", "def main():\n    return 0")
        myFixture.configureByText(
            "pyproject.toml",
            """
            [project.scripts]
            cli = "package.mo<caret>dule:main"
            """.trimIndent(),
        )

        val reference = referenceAtCaret() as? PsiPolyVariantReference
            ?: error("Expected a polyvariant module reference")

        assertEquals(2, reference.multiResolve(false).size)
    }

    fun testMissingPyProjectScriptModuleStaysUnresolved() {
        myFixture.configureByText(
            "pyproject.toml",
            """
            [project.scripts]
            cli = "missing.mo<caret>dule:main"
            """.trimIndent(),
        )

        val reference = referenceAtCaret() ?: error("Expected a soft module reference")

        assertNull(reference.resolve())
        assertTrue(reference.isSoft)
    }

    fun testMissingPyProjectScriptCallableStaysUnresolved() {
        myFixture.addFileToProject("package/module.py", "def other():\n    return 0")
        myFixture.configureByText(
            "pyproject.toml",
            """
            [project.scripts]
            cli = "package.module:ma<caret>in"
            """.trimIndent(),
        )

        val reference = referenceAtCaret() ?: error("Expected a soft callable reference")

        assertNull(reference.resolve())
        assertTrue(reference.isSoft)
    }

    fun testNestedPyProjectScriptCallableStaysUnresolved() {
        myFixture.addFileToProject(
            "package/module.py",
            """
            def wrapper():
                def main():
                    return 0
                return main()
            """.trimIndent(),
        )
        myFixture.configureByText(
            "pyproject.toml",
            """
            [project.scripts]
            cli = "package.module:ma<caret>in"
            """.trimIndent(),
        )

        val reference = referenceAtCaret() ?: error("Expected a soft callable reference")

        assertNull(reference.resolve())
    }

    fun testDoesNotCreateEntryPointReferenceOutsideProjectScripts() {
        myFixture.addFileToProject("package/module.py", "def main():\n    return 0")
        myFixture.configureByText(
            "pyproject.toml",
            """
            [tool.runner]
            cli = "package.module:ma<caret>in"
            """.trimIndent(),
        )

        assertNull(referenceAtCaret())
    }

    fun testDoesNotCreateEntryPointReferenceInNonPyProjectTomlFile() {
        myFixture.addFileToProject("package/module.py", "def main():\n    return 0")
        myFixture.configureByText(
            "app.toml",
            """
            [project.scripts]
            cli = "package.module:ma<caret>in"
            """.trimIndent(),
        )

        assertNull(referenceAtCaret())
    }

    fun testDoesNotCreateEntryPointReferenceInProjectScriptsArrayTable() {
        myFixture.addFileToProject("package/module.py", "def main():\n    return 0")
        myFixture.configureByText(
            "pyproject.toml",
            """
            [[project.scripts]]
            cli = "package.module:ma<caret>in"
            """.trimIndent(),
        )

        assertNull(referenceAtCaret())
    }

    fun testResolvesRootDottedProjectScriptCallable() {
        myFixture.addFileToProject("package/module.py", "def main():\n    return 0")
        myFixture.configureByText(
            "pyproject.toml",
            """
            project.scripts.cli = "package.module:ma<caret>in"
            """.trimIndent(),
        )

        val resolved = referenceAtCaret()?.resolve() ?: error("Expected dotted-key callable reference to resolve")

        assertEquals("main", resolved.text)
    }

    private fun referenceAtCaret(): PsiReference? {
        val offset = myFixture.editor.caretModel.offset
        return myFixture.file.findReferenceAt(offset)
    }
}
