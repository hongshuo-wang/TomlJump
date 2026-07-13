package com.tomljump.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigNameNormalizerTest {
    @Test
    fun `normalizes separators and case`() {
        assertEquals("apikey", ConfigNameNormalizer.normalize("api_key"))
        assertEquals("apikey", ConfigNameNormalizer.normalize("api-key"))
        assertEquals("apikey", ConfigNameNormalizer.normalize("apiKey"))
        assertEquals("apikey", ConfigNameNormalizer.normalize("APIKey"))
    }

    @Test
    fun `normalizes url acronym forms`() {
        assertEquals("baseurl", ConfigNameNormalizer.normalize("base_url"))
        assertEquals("baseurl", ConfigNameNormalizer.normalize("baseUrl"))
        assertEquals("baseurl", ConfigNameNormalizer.normalize("BaseURL"))
    }

    @Test
    fun `matches normalized variants`() {
        assertTrue(ConfigNameNormalizer.matches("api_key", "APIKey"))
        assertTrue(ConfigNameNormalizer.matches("base-url", "baseURL"))
        assertTrue(ConfigNameNormalizer.matches("model", "Model"))
    }
}
