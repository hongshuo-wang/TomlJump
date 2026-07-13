package com.tomljump.jetbrains.config.resolver

import com.tomljump.core.ConfigKeyPath
import com.tomljump.core.ConfigNameNormalizer

class JavaConfigResolver : SourcePatternResolver(setOf("java")) {
    private val annotatedFieldPattern =
        Regex("""(?s)@JsonProperty\("([^"]+)"\)\s+(?:private|public|protected)?\s*(?:final\s+)?[A-Za-z0-9_<>, ?]+\s+([A-Za-z_][A-Za-z0-9_]*)""")
    private val fieldPattern =
        Regex("""(?m)^\s*(?:private|public|protected)?\s*(?:final\s+)?[A-Za-z0-9_<>, ?]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*(?:=|;)""")

    override fun findTargets(sourceText: String, keyPath: ConfigKeyPath): List<ConfigSourceTarget> {
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
