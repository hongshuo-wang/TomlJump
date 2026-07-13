package com.tomljump.jetbrains.config.resolver

import com.tomljump.core.ConfigKeyPath

class GoConfigResolver : SourcePatternResolver(setOf("go")) {
    override fun findTargets(sourceText: String, keyPath: ConfigKeyPath): List<ConfigSourceTarget> = emptyList()
}
