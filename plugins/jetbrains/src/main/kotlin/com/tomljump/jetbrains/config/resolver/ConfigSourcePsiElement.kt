package com.tomljump.jetbrains.config.resolver

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.FakePsiElement

class ConfigSourcePsiElement(
    private val sourceFile: PsiFile,
    private val target: ConfigSourceTarget,
) : FakePsiElement() {
    override fun getParent(): PsiElement = sourceFile

    override fun getContainingFile(): PsiFile = sourceFile

    override fun getProject(): Project = sourceFile.project

    override fun getName(): String = target.label

    override fun getText(): String = target.label

    override fun getTextRange(): TextRange = TextRange.from(target.offset, target.label.length)

    override fun getTextOffset(): Int = target.offset

    override fun getNavigationElement(): PsiElement = this

    override fun canNavigate(): Boolean = sourceFile.virtualFile != null

    override fun canNavigateToSource(): Boolean = canNavigate()

    override fun navigate(requestFocus: Boolean) {
        val virtualFile = sourceFile.virtualFile ?: return
        OpenFileDescriptor(project, virtualFile, target.offset).navigate(requestFocus)
    }
}
