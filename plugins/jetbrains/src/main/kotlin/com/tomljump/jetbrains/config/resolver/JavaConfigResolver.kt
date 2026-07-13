package com.tomljump.jetbrains.config.resolver

import com.tomljump.core.ConfigKeyPath

class JavaConfigResolver : SourcePatternResolver(setOf("java")) {
    override fun findTargets(sourceText: String, keyPath: ConfigKeyPath): List<ConfigSourceTarget> = emptyList()
}
