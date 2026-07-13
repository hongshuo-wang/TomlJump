package com.tomljump.jetbrains.config.resolver

import com.tomljump.core.ConfigKeyPath
import com.tomljump.core.ConfigNameNormalizer

class GoConfigResolver : SourcePatternResolver(setOf("go")) {
    private val typePattern = Regex("""(?m)^\s*type\s+([A-Za-z_][A-Za-z0-9_]*)\s+struct\b""")
    private val tagPattern = Regex("""(?m)^\s*([A-Za-z_][A-Za-z0-9_]*)\s+[^`\n]+`([^`]+)`""")
    private val fieldPattern = Regex("""(?m)^\s*([A-Za-z_][A-Za-z0-9_]*)\s+[A-Za-z_][A-Za-z0-9_.*\[\]]*""")
    private val aliasPattern = Regex("""(?:toml|json|mapstructure|yaml):"([^",]+)""")

    override fun findTargets(sourceText: String, keyPath: ConfigKeyPath): List<ConfigSourceTarget> {
        val leaf = keyPath.leaf
        val tagMatches = tagPattern.findAll(sourceText).mapNotNull { match ->
            val fieldName = match.groupValues[1]
            val tagText = match.groupValues[2]
            val aliases = aliasPattern.findAll(tagText).map { it.groupValues[1] }
            if (aliases.any { it == leaf }) {
                ConfigSourceTarget(match.range.first + match.value.indexOf(fieldName), fieldName)
            } else {
                null
            }
        }.toList()

        if (tagMatches.isNotEmpty()) return tagMatches

        if (keyPath.segments.size == 1) {
            val typeMatches = typePattern.findAll(sourceText).mapNotNull { match ->
                val typeName = match.groupValues[1]
                if (ConfigSourceNameMatcher.matchesContainerName(leaf, typeName)) {
                    ConfigSourceTarget(match.range.first + match.value.indexOf(typeName), typeName)
                } else {
                    null
                }
            }.toList()

            if (typeMatches.isNotEmpty()) return typeMatches
        }

        return fieldPattern.findAll(sourceText).mapNotNull { match ->
            val fieldName = match.groupValues[1]
            if (ConfigNameNormalizer.matches(leaf, fieldName)) {
                ConfigSourceTarget(match.range.first + match.value.indexOf(fieldName), fieldName)
            } else {
                null
            }
        }.toList()
    }
}
