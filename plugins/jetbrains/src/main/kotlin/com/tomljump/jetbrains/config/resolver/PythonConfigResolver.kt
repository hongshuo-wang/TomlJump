package com.tomljump.jetbrains.config.resolver

import com.tomljump.core.ConfigKeyPath
import com.tomljump.core.ConfigNameNormalizer

class PythonConfigResolver : SourcePatternResolver(setOf("py")) {
    private val classPattern = Regex("""(?m)^\s*class\s+([A-Za-z_][A-Za-z0-9_]*)\s*[\(:]""")
    private val fieldPattern = Regex("""(?m)^\s*([A-Za-z_][A-Za-z0-9_]*)\s*(?::|=)""")

    override fun findTargets(sourceText: String, keyPath: ConfigKeyPath): List<ConfigSourceTarget> {
        if (keyPath.segments.size == 1) {
            val classMatches = classPattern.findAll(sourceText).mapNotNull { match ->
                val className = match.groupValues[1]
                if (ConfigSourceNameMatcher.matchesContainerName(keyPath.leaf, className)) {
                    ConfigSourceTarget(match.range.first + match.value.indexOf(className), className)
                } else {
                    null
                }
            }.toList()

            if (classMatches.isNotEmpty()) return classMatches
        }

        return fieldPattern.findAll(sourceText).mapNotNull { match ->
            val name = match.groupValues[1]
            if (ConfigNameNormalizer.matches(keyPath.leaf, name)) {
                ConfigSourceTarget(match.range.first + match.value.indexOf(name), name)
            } else {
                null
            }
        }.toList()
    }
}
