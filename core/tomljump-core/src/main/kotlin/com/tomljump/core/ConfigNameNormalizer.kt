package com.tomljump.core

object ConfigNameNormalizer {
    fun normalize(value: String): String {
        return value
            .replace(Regex("[^A-Za-z0-9]"), "")
            .lowercase()
    }

    fun matches(configName: String, codeName: String): Boolean {
        return normalize(configName) == normalize(codeName)
    }
}
