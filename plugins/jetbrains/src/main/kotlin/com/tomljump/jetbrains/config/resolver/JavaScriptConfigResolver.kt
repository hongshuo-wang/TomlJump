package com.tomljump.jetbrains.config.resolver

class JavaScriptConfigResolver : SourcePatternResolver(setOf("js", "jsx")) {
    private val objectPattern = Regex("""(?m)^\s*(?:export\s+)?(?:const|let|var)\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*\{""")
    private val propertyPattern = Regex("""(?m)^\s*(?:"([^"]+)"|'([^']+)'|([A-Za-z_][A-Za-z0-9_]*))\s*:""")

    override fun findDeclarations(sourceText: String): List<ConfigSourceDeclaration> {
        val containerBlocks = SourceStructureScanner.braceContainers(sourceText, objectPattern)
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
