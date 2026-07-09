package com.tomljump.jetbrains

import com.intellij.psi.PsiReference
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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

    private fun referenceAtCaret(): PsiReference? {
        val offset = myFixture.editor.caretModel.offset
        return myFixture.file.findReferenceAt(offset)
    }
}
