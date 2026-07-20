package com.tomljump.jetbrains.config

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.tomljump.core.ConfigKeyPath
import org.toml.lang.psi.TomlHeaderOwner
import org.toml.lang.psi.TomlInlineTable
import org.toml.lang.psi.TomlKey
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlTableHeader

object TomlConfigKeyPathExtractor {
    fun extract(element: PsiElement): ExtractedConfigKeyPath? {
        if (!isTomlFile(element)) return null
        val segment = PsiTreeUtil.getParentOfType(element, TomlKeySegment::class.java, false) ?: return null
        val segmentName = segment.name?.takeIf(String::isNotBlank) ?: return null
        val key = segment.parent as? TomlKey ?: return null
        if (key.segments.lastOrNull() != segment) return null
        val keySegments = segmentsOf(key) ?: return null

        val (pathSegments, kind) = when (val owner = key.parent) {
            is TomlTableHeader -> {
                if (hasSyntaxError(owner)) return null
                keySegments to TomlConfigReferenceKind.TABLE
            }
            is TomlKeyValue -> {
                if (PsiTreeUtil.getParentOfType(owner, TomlInlineTable::class.java, false) != null) return null
                ((containingHeaderSegments(owner) ?: return null) + keySegments) to TomlConfigReferenceKind.KEY
            }
            else -> return null
        }

        return ExtractedConfigKeyPath(
            keyPath = ConfigKeyPath.from(pathSegments),
            text = segmentName,
            rangeInElement = TextRange.from(0, segment.textLength),
            kind = kind,
        )
    }

    private fun containingHeaderSegments(entry: TomlKeyValue): List<String>? {
        val headerOwner = PsiTreeUtil.getParentOfType(entry, TomlHeaderOwner::class.java, true)
            ?: return emptyList()
        val header = headerOwner.header ?: return null
        if (hasSyntaxError(header)) return null
        val key = header.key ?: return null
        return segmentsOf(key)
    }

    private fun segmentsOf(key: TomlKey): List<String>? {
        if (PsiTreeUtil.findChildOfType(key, PsiErrorElement::class.java) != null) return null
        val segments = key.segments.mapNotNull { it.name?.takeIf(String::isNotBlank) }
        return segments.takeIf { it.size == key.segments.size && it.isNotEmpty() }
    }

    private fun hasSyntaxError(element: PsiElement): Boolean {
        return PsiTreeUtil.findChildOfType(element, PsiErrorElement::class.java) != null
    }

    private fun isTomlFile(element: PsiElement): Boolean {
        val file = element.containingFile ?: return false
        val virtualFile = file.virtualFile
        return when {
            virtualFile != null -> virtualFile.extension.equals("toml", ignoreCase = true)
            else -> file.name.endsWith(".toml", ignoreCase = true)
        }
    }
}
