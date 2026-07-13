package com.tomljump.jetbrains.config.resolver

import com.tomljump.core.ConfigNameNormalizer

internal object ConfigSourceNameMatcher {
    fun matchesContainerName(configName: String, sourceName: String): Boolean {
        val normalizedConfig = ConfigNameNormalizer.normalize(configName)
        val normalizedSource = ConfigNameNormalizer.normalize(sourceName)
        return normalizedSource == normalizedConfig || normalizedSource == "${normalizedConfig}config"
    }
}
