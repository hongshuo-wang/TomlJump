package com.tomljump.jetbrains.config

import com.intellij.openapi.util.TextRange
import com.tomljump.core.ConfigKeyPath

data class ExtractedConfigKeyPath(
    val keyPath: ConfigKeyPath,
    val text: String,
    val rangeInElement: TextRange,
)
