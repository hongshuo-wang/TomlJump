package com.tomljump.jetbrains.config.resolver

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.tomljump.core.ConfigKeyPath
import com.tomljump.jetbrains.config.TomlConfigReferenceKind

interface LanguageConfigResolver {
    fun resolve(
        project: Project,
        keyPath: ConfigKeyPath,
        kind: TomlConfigReferenceKind? = null,
    ): List<PsiElement>
}
