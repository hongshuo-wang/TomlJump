package com.tomljump.jetbrains.resolver

import com.intellij.psi.PsiElement
import com.tomljump.core.TomlReference

class SymbolShapeResolver {
    fun resolve(element: PsiElement, reference: TomlReference): List<PsiElement> {
        return emptyList()
    }
}
