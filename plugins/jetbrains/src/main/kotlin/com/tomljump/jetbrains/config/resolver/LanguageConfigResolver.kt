package com.tomljump.jetbrains.config.resolver

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.tomljump.core.ConfigKeyPath

interface LanguageConfigResolver {
    fun resolve(project: Project, keyPath: ConfigKeyPath): List<PsiElement>
}
