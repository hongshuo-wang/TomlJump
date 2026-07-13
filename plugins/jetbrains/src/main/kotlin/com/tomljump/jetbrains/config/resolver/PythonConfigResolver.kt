package com.tomljump.jetbrains.config.resolver

import com.tomljump.core.ConfigKeyPath

class PythonConfigResolver : SourcePatternResolver(setOf("py")) {
    override fun findTargets(sourceText: String, keyPath: ConfigKeyPath): List<ConfigSourceTarget> = emptyList()
}
