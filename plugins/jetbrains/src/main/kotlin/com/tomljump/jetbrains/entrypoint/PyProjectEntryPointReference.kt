package com.tomljump.jetbrains.entrypoint

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult

class PyProjectEntryPointReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val qualifier: String,
    private val member: String?,
    private val resolver: PythonEntryPointResolver,
) : PsiReferenceBase.Poly<PsiElement>(element, rangeInElement, true) {
    override fun resolve(): PsiElement? = resolveTargets().firstOrNull()

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return resolveTargets().map(::PsiElementResolveResult).toTypedArray()
    }

    override fun getVariants(): Array<Any> = emptyArray()

    private fun resolveTargets(): List<PsiElement> {
        return if (member == null) {
            resolver.resolveModule(element, qualifier)
        } else {
            resolver.resolveCallable(element, qualifier, member)
        }
    }
}
