package com.tomljump.jetbrains.resolver

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.tomljump.core.TomlReference

class FilePathResolver {
    fun resolve(element: PsiElement, reference: TomlReference): List<PsiElement> {
        val containingFile = element.containingFile ?: return emptyList()
        val baseDirectory = containingFile.virtualFile?.parent ?: return emptyList()
        val targetFile = baseDirectory.findFileByRelativePath(reference.lookupValue) ?: return emptyList()
        if (!targetFile.exists() || targetFile.isDirectory) {
            return emptyList()
        }

        val targetPsi = PsiManager.getInstance(element.project).findFile(targetFile) ?: return emptyList()
        return listOf(targetPsi)
    }
}
