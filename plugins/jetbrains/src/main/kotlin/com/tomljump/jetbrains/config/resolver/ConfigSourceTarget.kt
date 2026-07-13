package com.tomljump.jetbrains.config.resolver

data class ConfigSourceTarget(
    /**
     * Offset of the source declaration identifier in the exact text passed to
     * `findTargets`. The label must fit inside that text at this offset.
     * Invalid ranges are ignored by `SourcePatternResolver`.
     */
    val offset: Int,
    val label: String,
)
