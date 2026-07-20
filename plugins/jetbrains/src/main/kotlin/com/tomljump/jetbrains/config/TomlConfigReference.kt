package com.tomljump.jetbrains.config

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.tomljump.core.ConfigKeyPath
import com.tomljump.jetbrains.config.resolver.TomlConfigTargetResolver

class TomlConfigReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val keyPath: ConfigKeyPath,
    private val kind: TomlConfigReferenceKind,
    private val resolver: TomlConfigTargetResolver = TomlConfigTargetResolver(),
) : PsiReferenceBase.Poly<PsiElement>(element, rangeInElement, true) {
    override fun resolve(): PsiElement? = resolver.resolve(element, keyPath, kind).firstOrNull()

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return resolver.resolve(element, keyPath, kind)
            .map(::PsiElementResolveResult)
            .toTypedArray()
    }

    override fun getCanonicalText(): String = keyPath.display

    override fun getVariants(): Array<Any> = emptyArray()
}
