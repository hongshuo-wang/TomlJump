package com.tomljump.jetbrains.config

import com.intellij.openapi.util.TextRange
import com.tomljump.core.ConfigKeyPath

enum class TomlConfigReferenceKind {
    TABLE,
    KEY,
}

data class ExtractedConfigKeyPath(
    val keyPath: ConfigKeyPath,
    val text: String,
    val rangeInElement: TextRange,
    val kind: TomlConfigReferenceKind,
)
