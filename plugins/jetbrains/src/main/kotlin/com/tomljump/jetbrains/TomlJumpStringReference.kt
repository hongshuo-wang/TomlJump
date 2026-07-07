package com.tomljump.jetbrains

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiFile
import com.tomljump.core.TomlReference
import com.tomljump.jetbrains.resolver.TomlJumpResolver

class TomlJumpStringReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val tomlReference: TomlReference,
    private val resolver: TomlJumpResolver,
) : PsiReferenceBase.Poly<PsiElement>(element, rangeInElement, false) {
    override fun resolve(): PsiElement? = resolver.resolve(element, tomlReference).firstOrNull()

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return resolver.resolve(element, tomlReference)
            .map(::PsiElementResolveResult)
            .toTypedArray()
    }

    override fun getVariants(): Array<Any> = emptyArray()

    override fun isReferenceTo(target: PsiElement): Boolean {
        val resolved = resolve() ?: return false
        if (resolved == target) {
            return true
        }

        return virtualFileOf(resolved) != null && virtualFileOf(resolved) == virtualFileOf(target)
    }

    private fun virtualFileOf(target: PsiElement) = when (target) {
        is PsiFile -> target.virtualFile
        else -> target.containingFile?.virtualFile
    }
}
