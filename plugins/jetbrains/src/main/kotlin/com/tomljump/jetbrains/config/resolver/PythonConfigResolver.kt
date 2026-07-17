package com.tomljump.jetbrains.config.resolver

class PythonConfigResolver : SourcePatternResolver(setOf("py")) {
    private val classPattern =
        Regex("""(?ms)^class[ \t]+([A-Za-z_][A-Za-z0-9_]*)[ \t]*(?:\([^)]*\))?[ \t]*:""")
    private val fieldPattern = Regex("""(?m)^([ \t]*)([A-Za-z_][A-Za-z0-9_]*)\s*(?::|=)""")

    override fun findDeclarations(sourceText: String): List<ConfigSourceDeclaration> {
        val blocks = classPattern.findAll(sourceText).mapNotNull { match ->
            pythonContainerBlock(sourceText, match)
        }.toList()
        val containers = blocks.map(PythonContainerBlock::declaration)
        val fields = fieldPattern.findAll(sourceText).mapNotNull { match ->
            val indentation = indentationWidth(match.groupValues[1])
            val name = match.groupValues[2]
            val offset = match.range.first + match.value.indexOf(name)
            if (!SourceStructureScanner.isCodeOffset(sourceText, offset, hashLineComments = true)) {
                return@mapNotNull null
            }
            val owner = blocks
                .asSequence()
                .filter { offset in it.bodyStartOffset until it.bodyEndOffset }
                .filter { indentation == it.directBodyIndentation }
                .minByOrNull { it.bodyEndOffset - it.bodyStartOffset }
                ?: return@mapNotNull null
            ConfigSourceDeclaration(
                offset = offset,
                label = name,
                kind = ConfigSourceDeclarationKind.FIELD,
                ownerLabel = owner.declaration.label,
            )
        }
        return containers + fields.toList()
    }

    private fun pythonContainerBlock(sourceText: String, match: MatchResult): PythonContainerBlock? {
        if (!SourceStructureScanner.isCodeOffset(sourceText, match.range.first, hashLineComments = true)) {
            return null
        }
        val classIndentation = 0
        val className = match.groupValues[1]
        val bodyStartOffset = sourceText.indexOf('\n', match.range.last).let { newline ->
            if (newline < 0) return null else newline + 1
        }
        var bodyEndOffset = sourceText.length
        var directBodyIndentation: Int? = null
        var lineStart = bodyStartOffset
        while (lineStart < sourceText.length) {
            val lineEnd = sourceText.indexOf('\n', lineStart).let { if (it < 0) sourceText.length else it }
            val line = sourceText.substring(lineStart, lineEnd)
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith('#')) {
                val indentation = indentationWidth(line.takeWhile { it == ' ' || it == '\t' })
                if (indentation <= classIndentation) {
                    bodyEndOffset = lineStart
                    break
                }
                directBodyIndentation = minOf(directBodyIndentation ?: indentation, indentation)
            }
            lineStart = lineEnd + 1
        }
        return PythonContainerBlock(
            declaration = ConfigSourceDeclaration(
                offset = match.range.first + match.value.indexOf(className),
                label = className,
                kind = ConfigSourceDeclarationKind.CONTAINER,
            ),
            bodyStartOffset = bodyStartOffset,
            bodyEndOffset = bodyEndOffset,
            directBodyIndentation = directBodyIndentation ?: return null,
        )
    }

    private fun indentationWidth(indentation: String): Int {
        return indentation.sumOf { if (it == '\t') TAB_WIDTH else 1 }
    }

    private data class PythonContainerBlock(
        val declaration: ConfigSourceDeclaration,
        val bodyStartOffset: Int,
        val bodyEndOffset: Int,
        val directBodyIndentation: Int,
    )

    companion object {
        private const val TAB_WIDTH = 4
    }
}
