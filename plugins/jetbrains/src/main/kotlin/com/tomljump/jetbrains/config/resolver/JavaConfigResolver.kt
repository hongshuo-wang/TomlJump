package com.tomljump.jetbrains.config.resolver

class JavaConfigResolver : SourcePatternResolver(setOf("java")) {
    private val classPattern =
        Regex("""(?m)^\s*(?:public\s+)?(?:final\s+)?class\s+([A-Za-z_][A-Za-z0-9_]*)\b[^\{;]*\{""")
    private val annotatedFieldPattern =
        Regex("""(?s)@JsonProperty\("([^"]+)"\)\s+(?:private|public|protected)?\s*(?:final\s+)?[A-Za-z0-9_<>, ?]+\s+([A-Za-z_][A-Za-z0-9_]*)""")
    private val fieldPattern =
        Regex("""(?m)^\s*(?:private|public|protected)?\s*(?:final\s+)?[A-Za-z0-9_<>, ?]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*(?:=|;)""")

    override fun findDeclarations(sourceText: String): List<ConfigSourceDeclaration> {
        val containerBlocks = SourceStructureScanner.braceContainers(sourceText, classPattern)
        val containers = containerBlocks.map(SourceContainerBlock::declaration)
        val annotated = annotatedFieldPattern.findAll(sourceText).mapNotNull { match ->
            val alias = match.groupValues[1]
            val name = match.groupValues[2]
            val offset = match.range.first + match.value.lastIndexOf(name)
            if (!SourceStructureScanner.isCodeOffset(sourceText, offset)) return@mapNotNull null
            val owner = SourceStructureScanner.directOwnerAt(sourceText, containerBlocks, offset)
                ?: return@mapNotNull null
            ConfigSourceDeclaration(
                offset = offset,
                label = name,
                kind = ConfigSourceDeclarationKind.FIELD,
                aliases = setOf(alias),
                ownerLabel = owner.label,
            )
        }.toList()
        val annotatedOffsets = annotated.mapTo(mutableSetOf()) { it.offset }
        val fields = fieldPattern.findAll(sourceText).mapNotNull { match ->
            val name = match.groupValues[1]
            val offset = match.range.first + match.value.lastIndexOf(name)
            if (offset in annotatedOffsets) return@mapNotNull null
            if (!SourceStructureScanner.isCodeOffset(sourceText, offset)) return@mapNotNull null
            val owner = SourceStructureScanner.directOwnerAt(sourceText, containerBlocks, offset)
                ?: return@mapNotNull null
            ConfigSourceDeclaration(
                offset = offset,
                label = name,
                kind = ConfigSourceDeclarationKind.FIELD,
                ownerLabel = owner.label,
            )
        }.toList()

        return containers + annotated + fields
    }
}
