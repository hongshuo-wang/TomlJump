package com.tomljump.jetbrains.config.resolver

import com.tomljump.core.ConfigKeyPath
import com.tomljump.core.ConfigNameNormalizer

class JavaScriptConfigResolver : SourcePatternResolver(setOf("js", "jsx")) {
    private val objectPattern = Regex("""(?m)^\s*(?:export\s+)?(?:const|let|var)\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*\{""")
    private val propertyPattern = Regex("""(?m)^\s*(?:"([^"]+)"|'([^']+)'|([A-Za-z_][A-Za-z0-9_]*))\s*:""")

    override fun findTargets(sourceText: String, keyPath: ConfigKeyPath): List<ConfigSourceTarget> {
        if (keyPath.segments.size == 1) {
            val objectMatches = objectPattern.findAll(sourceText).mapNotNull { match ->
                val objectName = match.groupValues[1]
                if (ConfigSourceNameMatcher.matchesContainerName(keyPath.leaf, objectName)) {
                    ConfigSourceTarget(match.range.first + match.value.indexOf(objectName), objectName)
                } else {
                    null
                }
            }.toList()

            if (objectMatches.isNotEmpty()) return objectMatches
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
