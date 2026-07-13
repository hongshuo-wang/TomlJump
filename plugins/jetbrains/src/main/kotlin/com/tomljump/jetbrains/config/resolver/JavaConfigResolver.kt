package com.tomljump.jetbrains.config.resolver

import com.tomljump.core.ConfigKeyPath
import com.tomljump.core.ConfigNameNormalizer

class JavaConfigResolver : SourcePatternResolver(setOf("java")) {
    private val classPattern =
        Regex("""(?m)^\s*(?:public\s+)?(?:final\s+)?class\s+([A-Za-z_][A-Za-z0-9_]*)\b""")
    private val annotatedFieldPattern =
        Regex("""(?s)@JsonProperty\("([^"]+)"\)\s+(?:private|public|protected)?\s*(?:final\s+)?[A-Za-z0-9_<>, ?]+\s+([A-Za-z_][A-Za-z0-9_]*)""")
    private val fieldPattern =
        Regex("""(?m)^\s*(?:private|public|protected)?\s*(?:final\s+)?[A-Za-z0-9_<>, ?]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*(?:=|;)""")

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

        val annotated = annotatedFieldPattern.findAll(sourceText).mapNotNull { match ->
            val alias = match.groupValues[1]
            val name = match.groupValues[2]
            if (alias == keyPath.leaf) {
                ConfigSourceTarget(match.range.first + match.value.lastIndexOf(name), name)
            } else {
                null
            }
        }.toList()

        if (annotated.isNotEmpty()) return annotated

        return fieldPattern.findAll(sourceText).mapNotNull { match ->
            val name = match.groupValues[1]
            if (ConfigNameNormalizer.matches(keyPath.leaf, name)) {
                ConfigSourceTarget(match.range.first + match.value.lastIndexOf(name), name)
            } else {
                null
            }
        }.toList()
    }
}
