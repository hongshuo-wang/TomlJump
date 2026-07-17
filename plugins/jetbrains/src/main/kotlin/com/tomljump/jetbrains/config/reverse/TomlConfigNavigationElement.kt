package com.tomljump.jetbrains.config.reverse

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.FakePsiElement
import com.intellij.psi.presentation.java.SymbolPresentationUtil
import javax.swing.Icon

internal class TomlConfigNavigationElement(
    private val target: TomlConfigNavigationTarget,
) : FakePsiElement() {
    private val delegate: PsiElement = target.element

    override fun getParent(): PsiElement? = delegate.parent

    override fun getName(): String = presentableText

    override fun getPresentableText(): String = when (target.kind) {
        TomlConfigNavigationTargetKind.TABLE -> "[${target.keyPath.segments.joinToString(".")}]"
        TomlConfigNavigationTargetKind.KEY -> target.keyPath.segments.joinToString(".")
    }

    override fun getLocationString(): String =
        SymbolPresentationUtil.getFilePathPresentation(delegate.containingFile)

    override fun getIcon(unused: Boolean): Icon? = delegate.getIcon(0)

    override fun getLanguage(): Language = delegate.language

    override fun getProject(): Project = delegate.project

    override fun getManager(): PsiManager = delegate.manager

    override fun getContainingFile(): PsiFile = delegate.containingFile

    override fun getTextRange(): TextRange = delegate.textRange

    override fun getStartOffsetInParent(): Int = delegate.startOffsetInParent

    override fun getTextLength(): Int = delegate.textLength

    override fun getTextOffset(): Int = delegate.textOffset

    override fun getText(): String = delegate.text

    override fun textToCharArray(): CharArray = delegate.textToCharArray()

    override fun isValid(): Boolean = delegate.isValid

    override fun getNavigationElement(): PsiElement = delegate

    override fun getOriginalElement(): PsiElement = delegate.originalElement

    override fun toString(): String = "TomlConfigNavigationElement(${delegate.containingFile.name}:$presentableText)"
}
