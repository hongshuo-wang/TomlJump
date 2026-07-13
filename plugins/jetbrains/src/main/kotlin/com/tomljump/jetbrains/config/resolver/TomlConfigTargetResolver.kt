package com.tomljump.jetbrains.config.resolver

import com.intellij.psi.PsiElement
import com.tomljump.core.ConfigKeyPath

class TomlConfigTargetResolver {
    fun resolve(element: PsiElement, keyPath: ConfigKeyPath): List<PsiElement> = emptyList()
}
