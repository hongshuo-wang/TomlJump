package com.tomljump.jetbrains.config.resolver

import com.tomljump.core.ConfigKeyPath

class JavaScriptConfigResolver : SourcePatternResolver(setOf("js", "jsx")) {
    override fun findTargets(sourceText: String, keyPath: ConfigKeyPath): List<ConfigSourceTarget> = emptyList()
}
