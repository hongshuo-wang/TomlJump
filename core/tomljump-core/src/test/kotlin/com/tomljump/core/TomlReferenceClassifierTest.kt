package com.tomljump.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TomlReferenceClassifierTest {
    private val classifier = TomlReferenceClassifier()

    @Test
    fun `classifies relative file path`() {
        val reference = classifier.classify("./schemas/user.json")

        assertEquals(ReferenceKind.FILE_PATH, reference?.kind)
        assertEquals("./schemas/user.json", reference?.rawValue)
        assertEquals("schemas/user.json", reference?.lookupValue)
    }

    @Test
    fun `classifies workspace relative file path`() {
        val reference = classifier.classify("templates/email.html")

        assertEquals(ReferenceKind.FILE_PATH, reference?.kind)
        assertEquals("templates/email.html", reference?.lookupValue)
    }

    @Test
    fun `classifies python callable reference`() {
        val reference = classifier.classify("app.user:create_user")

        assertEquals(ReferenceKind.CALLABLE, reference?.kind)
        assertEquals("app.user", reference?.qualifier)
        assertEquals("create_user", reference?.member)
    }

    @Test
    fun `classifies java class reference`() {
        val reference = classifier.classify("com.example.PaymentClient")

        assertEquals(ReferenceKind.CLASS_OR_MODULE, reference?.kind)
        assertEquals("com.example.PaymentClient", reference?.lookupValue)
    }

    @Test
    fun `ignores arbitrary prose`() {
        assertNull(classifier.classify("hello world"))
    }
}
