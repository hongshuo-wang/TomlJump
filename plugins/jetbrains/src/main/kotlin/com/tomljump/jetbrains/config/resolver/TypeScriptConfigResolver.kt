package com.tomljump.jetbrains.config.resolver

import com.tomljump.core.ConfigKeyPath
import com.tomljump.core.ConfigNameNormalizer

class TypeScriptConfigResolver : SourcePatternResolver(setOf("ts", "tsx")) {
    private val declarationPattern =
        Regex("""(?m)^\s*(?:export\s+)?(?:interface|class|type)\s+([A-Za-z_][A-Za-z0-9_]*)\b""")
    private val propertyPattern = Regex("""(?m)^\s*(?:"([^"]+)"|'([^']+)'|([A-Za-z_][A-Za-z0-9_]*))\??\s*:""")

    override fun findTargets(sourceText: String, keyPath: ConfigKeyPath): List<ConfigSourceTarget> {
        if (keyPath.segments.size == 1) {
            val declarationMatches = declarationPattern.findAll(sourceText).mapNotNull { match ->
                val declarationName = match.groupValues[1]
                if (ConfigSourceNameMatcher.matchesContainerName(keyPath.leaf, declarationName)) {
                    ConfigSourceTarget(match.range.first + match.value.indexOf(declarationName), declarationName)
                } else {
                    null
                }
            }.toList()

            if (declarationMatches.isNotEmpty()) return declarationMatches
        }

        return propertyPattern.findAll(sourceText).mapNotNull { match ->
            val quotedDouble = match.groupValues[1]
            val quotedSingle = match.groupValues[2]
            val identifier = match.groupValues[3]
            val name = quotedDouble.ifEmpty { quotedSingle.ifEmpty { identifier } }
            if (name == keyPath.leaf || ConfigNameNormalizer.matches(keyPath.leaf, name)) {
                ConfigSourceTarget(match.range.first + match.value.indexOf(name), name)
            } else {
                null
            }
        }.toList()
    }
}
