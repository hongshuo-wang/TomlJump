package com.tomljump.jetbrains.config.reverse

import com.intellij.psi.PsiElement
import com.tomljump.core.ConfigKeyPath

internal enum class TomlConfigNavigationTargetKind {
    TABLE,
    KEY,
}

internal data class TomlConfigNavigationTarget(
    val element: PsiElement,
    val keyPath: ConfigKeyPath,
    val kind: TomlConfigNavigationTargetKind,
)
