package com.tomljump.jetbrains.config.resolver

class GoConfigResolver : SourcePatternResolver(setOf("go")) {
    private val typePattern = Regex("""(?m)^\s*type\s+([A-Za-z_][A-Za-z0-9_]*)\s+struct\s*\{""")
    private val tagPattern = Regex("""(?m)^\s*([A-Za-z_][A-Za-z0-9_]*)\s+[^`\n]+`([^`]+)`""")
    private val fieldPattern = Regex("""(?m)^\s*([A-Za-z_][A-Za-z0-9_]*)\s+[A-Za-z_][A-Za-z0-9_.*\[\]]*""")
    private val aliasPattern = Regex("""(?:toml|json|mapstructure|yaml):"([^",]+)""")

    override fun findDeclarations(sourceText: String): List<ConfigSourceDeclaration> {
        val containerBlocks = SourceStructureScanner.braceContainers(sourceText, typePattern)
        val containers = containerBlocks.map(SourceContainerBlock::declaration)

        val taggedFields = tagPattern.findAll(sourceText).mapNotNull { match ->
            val fieldName = match.groupValues[1]
            val tagText = match.groupValues[2]
            val offset = match.range.first + match.value.indexOf(fieldName)
            if (!SourceStructureScanner.isCodeOffset(sourceText, offset)) return@mapNotNull null
            val owner = SourceStructureScanner.directOwnerAt(sourceText, containerBlocks, offset)
                ?: return@mapNotNull null
            ConfigSourceDeclaration(
                offset = offset,
                label = fieldName,
                kind = ConfigSourceDeclarationKind.FIELD,
                aliases = aliasPattern.findAll(tagText).map { it.groupValues[1] }.toSet(),
                ownerLabel = owner.label,
            )
        }.toList()
        val taggedOffsets = taggedFields.mapTo(mutableSetOf()) { it.offset }
        val fields = fieldPattern.findAll(sourceText).mapNotNull { match ->
            val fieldName = match.groupValues[1]
            val offset = match.range.first + match.value.indexOf(fieldName)
            if (offset in taggedOffsets) return@mapNotNull null
            if (!SourceStructureScanner.isCodeOffset(sourceText, offset)) return@mapNotNull null
            val owner = SourceStructureScanner.directOwnerAt(sourceText, containerBlocks, offset)
                ?: return@mapNotNull null
            ConfigSourceDeclaration(
                offset = offset,
                label = fieldName,
                kind = ConfigSourceDeclarationKind.FIELD,
                ownerLabel = owner.label,
            )
        }.toList()

        return containers + taggedFields + fields
    }
}
