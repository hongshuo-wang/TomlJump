package com.tomljump.jetbrains.config.resolver

import com.tomljump.core.ConfigKeyPath

class TypeScriptConfigResolver : SourcePatternResolver(setOf("ts", "tsx")) {
    override fun findTargets(sourceText: String, keyPath: ConfigKeyPath): List<ConfigSourceTarget> = emptyList()
}
