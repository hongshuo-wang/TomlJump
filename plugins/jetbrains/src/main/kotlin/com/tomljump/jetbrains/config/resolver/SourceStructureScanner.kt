package com.tomljump.jetbrains.config.resolver

internal data class SourceContainerBlock(
    val declaration: ConfigSourceDeclaration,
    val openBraceOffset: Int,
    val closeBraceOffset: Int,
) {
    fun containsBodyOffset(offset: Int): Boolean {
        return offset in (openBraceOffset + 1) until closeBraceOffset
    }
}

internal object SourceStructureScanner {
    fun braceContainers(sourceText: String, pattern: Regex): List<SourceContainerBlock> {
        return pattern.findAll(sourceText).mapNotNull { match ->
            if (braceDepthAt(sourceText, 0, match.range.first) != 0) return@mapNotNull null
            val label = match.groupValues[1]
            val openBraceOffset = match.range.first + match.value.lastIndexOf('{')
            val closeBraceOffset = findMatchingBrace(sourceText, openBraceOffset) ?: return@mapNotNull null
            SourceContainerBlock(
                declaration = ConfigSourceDeclaration(
                    offset = match.range.first + match.value.indexOf(label),
                    label = label,
                    kind = ConfigSourceDeclarationKind.CONTAINER,
                ),
                openBraceOffset = openBraceOffset,
                closeBraceOffset = closeBraceOffset,
            )
        }.toList()
    }

    fun directOwnerAt(
        sourceText: String,
        containers: List<SourceContainerBlock>,
        offset: Int,
    ): ConfigSourceDeclaration? {
        return containers
            .asSequence()
            .filter { it.containsBodyOffset(offset) }
            .filter { braceDepthAt(sourceText, it.openBraceOffset + 1, offset) == 0 }
            .minByOrNull { it.closeBraceOffset - it.openBraceOffset }
            ?.declaration
    }

    fun isCodeOffset(
        sourceText: String,
        offset: Int,
        hashLineComments: Boolean = false,
    ): Boolean {
        return braceDepthAt(sourceText, 0, offset, hashLineComments) != null
    }

    private fun findMatchingBrace(sourceText: String, openBraceOffset: Int): Int? {
        if (openBraceOffset !in sourceText.indices || sourceText[openBraceOffset] != '{') return null

        var depth = 1
        return scanCode(sourceText, openBraceOffset + 1, sourceText.length) { char ->
            when (char) {
                '{' -> depth += 1
                '}' -> depth -= 1
            }
            depth == 0
        }.stopOffset
    }

    private fun braceDepthAt(
        sourceText: String,
        startOffset: Int,
        endOffset: Int,
        hashLineComments: Boolean = false,
    ): Int? {
        var depth = 0
        val result = scanCode(sourceText, startOffset, endOffset, hashLineComments) { char ->
            when (char) {
                '{' -> depth += 1
                '}' -> depth -= 1
            }
            false
        }
        return depth.takeIf { result.finalState == ScanState.CODE }
    }

    private fun scanCode(
        sourceText: String,
        startOffset: Int,
        endOffset: Int,
        hashLineComments: Boolean = false,
        stopAfter: (Char) -> Boolean,
    ): ScanResult {
        var state = ScanState.CODE
        var escaped = false
        var index = startOffset
        while (index < endOffset) {
            val char = sourceText[index]
            val next = sourceText.getOrNull(index + 1)
            when (state) {
                ScanState.CODE -> when {
                    hashLineComments && char == '#' -> state = ScanState.LINE_COMMENT
                    char == '/' && next == '/' -> {
                        state = ScanState.LINE_COMMENT
                        index += 1
                    }
                    char == '/' && next == '*' -> {
                        state = ScanState.BLOCK_COMMENT
                        index += 1
                    }
                    char == '\'' -> state = ScanState.SINGLE_QUOTE
                    char == '"' -> state = ScanState.DOUBLE_QUOTE
                    char == '`' -> state = ScanState.BACKTICK
                    stopAfter(char) -> return ScanResult(index, state)
                }
                ScanState.LINE_COMMENT -> if (char == '\n') state = ScanState.CODE
                ScanState.BLOCK_COMMENT -> if (char == '*' && next == '/') {
                    state = ScanState.CODE
                    index += 1
                }
                ScanState.SINGLE_QUOTE,
                ScanState.DOUBLE_QUOTE,
                ScanState.BACKTICK,
                -> {
                    val closing = when (state) {
                        ScanState.SINGLE_QUOTE -> '\''
                        ScanState.DOUBLE_QUOTE -> '"'
                        ScanState.BACKTICK -> '`'
                        else -> error("Unexpected scan state")
                    }
                    when {
                        escaped -> escaped = false
                        char == '\\' -> escaped = true
                        char == closing -> state = ScanState.CODE
                    }
                }
            }
            index += 1
        }
        return ScanResult(null, state)
    }

    private data class ScanResult(
        val stopOffset: Int?,
        val finalState: ScanState,
    )

    private enum class ScanState {
        CODE,
        LINE_COMMENT,
        BLOCK_COMMENT,
        SINGLE_QUOTE,
        DOUBLE_QUOTE,
        BACKTICK,
    }
}
