package com.tomljump.jetbrains.entrypoint

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.tomljump.core.ReferenceKind
import com.tomljump.core.TomlReference
import com.tomljump.jetbrains.ExtractedTomlString
import com.tomljump.jetbrains.config.TomlConfigKeyPathExtractor
import org.toml.lang.psi.TomlArrayTable
import org.toml.lang.psi.TomlKeyValue

object PyProjectScriptContext {
    fun entryPoint(
        element: PsiElement,
        extracted: ExtractedTomlString,
    ): TomlReference? {
        if (element.containingFile?.name != "pyproject.toml") return null
        val keyValue = PsiTreeUtil.getParentOfType(element, TomlKeyValue::class.java, false) ?: return null
        if (PsiTreeUtil.getParentOfType(keyValue, TomlArrayTable::class.java, true) != null) return null
        val leafSegment = keyValue.key.segments.lastOrNull() ?: return null
        val keyPath = TomlConfigKeyPathExtractor.extract(leafSegment)?.keyPath ?: return null
        if (keyPath.segments.dropLast(1) != PROJECT_SCRIPTS_PATH) return null

        val separatorIndex = extracted.value.indexOf(':')
        if (separatorIndex <= 0 || separatorIndex == extracted.value.lastIndex) return null
        if (extracted.value.indexOf(':', separatorIndex + 1) >= 0) return null

        val qualifier = extracted.value.substring(0, separatorIndex)
        val member = extracted.value.substring(separatorIndex + 1)
        if (!qualifier.split('.').all(::isIdentifier) || !isIdentifier(member)) return null
        return TomlReference(
            rawValue = extracted.value,
            lookupValue = extracted.value,
            kind = ReferenceKind.CALLABLE,
            qualifier = qualifier,
            member = member,
        )
    }

    private fun isIdentifier(value: String): Boolean {
        return value.isNotEmpty() &&
            (value.first() == '_' || value.first().isAsciiLetter()) &&
            value.drop(1).all { it == '_' || it.isDigit() || it.isAsciiLetter() }
    }

    private fun Char.isAsciiLetter(): Boolean = this in 'A'..'Z' || this in 'a'..'z'

    private val PROJECT_SCRIPTS_PATH = listOf("project", "scripts")
}
