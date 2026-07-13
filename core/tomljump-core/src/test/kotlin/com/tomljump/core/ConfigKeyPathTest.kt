package com.tomljump.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class ConfigKeyPathTest {
    @Test
    fun `creates table path from non blank segments`() {
        val path = ConfigKeyPath.of("openai")

        assertEquals(listOf("openai"), path.segments)
        assertEquals("openai", path.leaf)
        assertEquals("openai", path.display)
    }

    @Test
    fun `creates nested key path`() {
        val path = ConfigKeyPath.of("servers", "production", "host")

        assertEquals(listOf("servers", "production", "host"), path.segments)
        assertEquals("host", path.leaf)
        assertEquals("servers.production.host", path.display)
    }

    @Test
    fun `uses value equality for identical segments`() {
        val first = ConfigKeyPath.of("openai", "api_key")
        val second = ConfigKeyPath.of("openai", "api_key")

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertNotEquals(first, ConfigKeyPath.of("openai", "model"))
    }

    @Test
    fun `trims path segments`() {
        val path = ConfigKeyPath.of(" openai ", " api_key ")

        assertEquals(listOf("openai", "api_key"), path.segments)
    }

    @Test
    fun `rejects empty path`() {
        assertFailsWith<IllegalArgumentException> {
            ConfigKeyPath.of()
        }
    }

    @Test
    fun `rejects blank segment`() {
        assertFailsWith<IllegalArgumentException> {
            ConfigKeyPath.of("openai", " ")
        }
    }
}
