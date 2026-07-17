package com.tomljump.jetbrains.config.resolver

class TypeScriptConfigResolver : SourcePatternResolver(setOf("ts", "tsx")) {
    private val declarationPatterns = listOf(
        Regex("""(?m)^\s*(?:export\s+)?(?:interface|class)\s+([A-Za-z_][A-Za-z0-9_]*)\b[^\{;]*\{"""),
        Regex("""(?m)^\s*(?:export\s+)?type\s+([A-Za-z_][A-Za-z0-9_]*)\b[^=;]*=\s*\{"""),
    )
    private val propertyPattern = Regex("""(?m)^\s*(?:"([^"]+)"|'([^']+)'|([A-Za-z_][A-Za-z0-9_]*))\??\s*:""")

    override fun findDeclarations(sourceText: String): List<ConfigSourceDeclaration> {
        val containerBlocks = declarationPatterns
            .flatMap { SourceStructureScanner.braceContainers(sourceText, it) }
            .sortedBy { it.declaration.offset }
        val containers = containerBlocks.map(SourceContainerBlock::declaration)
        val fields = propertyPattern.findAll(sourceText).mapNotNull { match ->
            val quotedDouble = match.groupValues[1]
            val quotedSingle = match.groupValues[2]
            val identifier = match.groupValues[3]
            val name = quotedDouble.ifEmpty { quotedSingle.ifEmpty { identifier } }
            val offset = match.range.first + match.value.indexOf(name)
            val declarationOffset = match.range.first + match.value.indexOfFirst { !it.isWhitespace() }
            if (!SourceStructureScanner.isCodeOffset(sourceText, declarationOffset)) return@mapNotNull null
            val owner = SourceStructureScanner.directOwnerAt(sourceText, containerBlocks, declarationOffset)
                ?: return@mapNotNull null
            ConfigSourceDeclaration(
                offset = offset,
                label = name,
                kind = ConfigSourceDeclarationKind.FIELD,
                ownerLabel = owner.label,
            )
        }
        return containers + fields.toList()
    }
}
