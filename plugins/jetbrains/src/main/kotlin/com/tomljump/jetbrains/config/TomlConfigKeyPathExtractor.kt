package com.tomljump.jetbrains.config

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.tomljump.core.ConfigKeyPath

object TomlConfigKeyPathExtractor {
    private val tableLinePattern = Regex("""^\s*\[([^]]*)]\s*(?:#.*)?$""")
    private val tableSegmentPattern = Regex("""[A-Za-z0-9_-]+""")
    private val keyPattern = Regex("""^\s*([A-Za-z0-9_-]+)\s*=""")

    fun extract(element: PsiElement): ExtractedConfigKeyPath? {
        if (!isTomlFile(element)) return null
        val text = element.text ?: return null
        if (!isConfigIdentifier(text)) return null

        val fileText = element.containingFile?.text ?: return null
        val elementStart = element.textRange.startOffset
        val line = lineAt(fileText, elementStart)
        val lineText = fileText.substring(line.start, line.end)
        val offsetInLine = elementStart - line.start

        tableLinePattern.matchEntire(lineText)?.let { match ->
            val tableText = match.groupValues[1]
            val tableSegments = parseTableSegments(tableText) ?: return null
            val tableStart = lineText.indexOf(tableText)
            val tableEnd = tableStart + tableText.length
            if (offsetInLine in tableStart until tableEnd && tableText.split('.').contains(text)) {
                return ExtractedConfigKeyPath(
                    keyPath = ConfigKeyPath.from(tableSegments),
                    text = text,
                    rangeInElement = TextRange.from(0, text.length),
                )
            }
        }

        keyPattern.find(lineText)?.let { match ->
            val keyText = match.groupValues[1]
            val keyStart = lineText.indexOf(keyText)
            val keyEnd = keyStart + keyText.length
            if (offsetInLine in keyStart until keyEnd && keyText == text) {
                val tableSegments = currentTableSegments(fileText, line.start)
                return ExtractedConfigKeyPath(
                    keyPath = ConfigKeyPath.from(tableSegments + keyText),
                    text = keyText,
                    rangeInElement = TextRange.from(0, text.length),
                )
            }
        }

        return null
    }

    private fun currentTableSegments(fileText: String, beforeOffset: Int): List<String> {
        val before = fileText.substring(0, beforeOffset)
        var current = emptyList<String>()
        before.lineSequence().forEach { line ->
            tableLinePattern.matchEntire(line.trim())?.let { match ->
                current = parseTableSegments(match.groupValues[1]) ?: emptyList()
            }
        }
        return current
    }

    private fun parseTableSegments(tableText: String): List<String>? {
        val segments = tableText.split('.')
        if (segments.isEmpty() || segments.any { !tableSegmentPattern.matches(it) }) {
            return null
        }
        return segments
    }

    private fun lineAt(fileText: String, offset: Int): LineRange {
        val start = fileText.lastIndexOf('\n', (offset - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val end = fileText.indexOf('\n', offset).let { if (it < 0) fileText.length else it }
        return LineRange(start, end)
    }

    private fun isConfigIdentifier(value: String): Boolean {
        return value.isNotEmpty() && value.all { it.isLetterOrDigit() || it == '_' || it == '-' }
    }

    private fun isTomlFile(element: PsiElement): Boolean {
        val file = element.containingFile ?: return false
        val virtualFile = file.virtualFile
        return when {
            virtualFile != null -> virtualFile.extension.equals("toml", ignoreCase = true)
            else -> file.name.endsWith(".toml", ignoreCase = true)
        }
    }

    private data class LineRange(val start: Int, val end: Int)
}
