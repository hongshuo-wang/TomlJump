package com.tomljump.jetbrains.config.resolver

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.tomljump.core.ConfigKeyPath

abstract class SourcePatternResolver(
    private val extensions: Set<String>,
) : LanguageConfigResolver {
    final override fun resolve(project: Project, keyPath: ConfigKeyPath): List<PsiElement> {
        return ReadAction.compute<List<PsiElement>, RuntimeException> {
            resolveInReadAction(project, keyPath)
        }
    }

    private fun resolveInReadAction(project: Project, keyPath: ConfigKeyPath): List<PsiElement> {
        // Navigation can be retried after indexing; returning no targets keeps unresolved keys quiet.
        if (DumbService.isDumb(project)) return emptyList()
        val scope = GlobalSearchScope.projectScope(project)
        return extensions.flatMap { extension ->
            ProgressManager.checkCanceled()
            FilenameIndex.getAllFilesByExt(project, extension, scope)
                .asSequence()
                .take(MAX_FILES_PER_EXTENSION)
                .flatMap { virtualFile ->
                    ProgressManager.checkCanceled()
                    if (virtualFile.length > MAX_SOURCE_FILE_BYTES) return@flatMap emptySequence()
                    val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return@flatMap emptySequence()
                    val text = psiFile.text
                    findTargets(text, keyPath).mapNotNull { target ->
                        ProgressManager.checkCanceled()
                        if (!isValidTargetRange(target, text)) return@mapNotNull null
                        psiFile.findElementAt(target.offset)?.let { leaf ->
                            val element = targetElement(leaf)
                            if (element.text == target.label) {
                                element
                            } else {
                                ConfigSourcePsiElement(psiFile, target)
                            }
                        }
                    }.asSequence()
                }
                .toList()
        }.distinctBy(::dedupeKey)
    }

    /**
     * Returns candidate declaration offsets in `sourceText`. Results may be
     * unsorted and duplicated; `SourcePatternResolver` validates offsets and
     * deduplicates mapped PSI targets.
     */
    protected abstract fun findTargets(sourceText: String, keyPath: ConfigKeyPath): List<ConfigSourceTarget>

    private fun targetElement(leaf: PsiElement): PsiElement {
        var current = leaf
        while (current.parent != null && current.parent.textRange == current.textRange) {
            current = current.parent
        }
        return current
    }

    private fun isValidTargetRange(target: ConfigSourceTarget, sourceText: String): Boolean {
        if (target.label.isEmpty()) return false
        if (target.offset !in sourceText.indices) return false
        if (target.offset > sourceText.length - target.label.length) return false
        return sourceText.regionMatches(target.offset, target.label, 0, target.label.length)
    }

    private fun dedupeKey(element: PsiElement): String {
        val virtualFile = element.containingFile?.virtualFile
        return if (virtualFile != null) {
            "${virtualFile.path}:${element.textRange.startOffset}"
        } else {
            "${System.identityHashCode(element.containingFile)}:${element.textRange.startOffset}:${element.textRange.endOffset}"
        }
    }

    companion object {
        private const val MAX_FILES_PER_EXTENSION = 500
        private const val MAX_SOURCE_FILE_BYTES = 1_000_000L
    }
}
