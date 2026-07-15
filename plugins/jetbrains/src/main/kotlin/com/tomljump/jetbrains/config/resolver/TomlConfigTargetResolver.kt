package com.tomljump.jetbrains.config.resolver

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.tomljump.core.ConfigKeyPath

class TomlConfigTargetResolver(
    private val resolvers: List<LanguageConfigResolver> = listOf(
        GoConfigResolver(),
        PythonConfigResolver(),
        JavaConfigResolver(),
        TypeScriptConfigResolver(),
        JavaScriptConfigResolver(),
    ),
) {
    fun resolve(element: PsiElement, keyPath: ConfigKeyPath): List<PsiElement> {
        val project = element.project
        val candidates = resolveAll(project, keyPath)
        return preferTargetsInMatchingContainer(project, keyPath, candidates).distinctBy(::dedupeKey)
    }

    private fun resolveAll(project: Project, keyPath: ConfigKeyPath): List<PsiElement> {
        return resolvers.flatMap { resolver ->
            try {
                resolver.resolve(project, keyPath)
            } catch (error: ProcessCanceledException) {
                throw error
            } catch (error: Throwable) {
                logger.debug("TomlJump config resolver failed for ${keyPath.display}", error)
                emptyList()
            }
        }
    }

    private fun preferTargetsInMatchingContainer(
        project: Project,
        keyPath: ConfigKeyPath,
        candidates: List<PsiElement>,
    ): List<PsiElement> {
        if (keyPath.segments.size <= 1 || candidates.isEmpty()) return candidates

        val containerKeyPath = ConfigKeyPath.from(keyPath.segments.dropLast(1))
        val containerFiles = resolveAll(project, containerKeyPath)
            .mapNotNull { it.containingFile?.virtualFile?.path }
            .toSet()
        if (containerFiles.isEmpty()) return candidates

        return candidates.filter { candidate ->
            candidate.containingFile?.virtualFile?.path in containerFiles
        }
    }

    companion object {
        private val logger = Logger.getInstance(TomlConfigTargetResolver::class.java)

        private fun dedupeKey(element: PsiElement): String {
            val virtualFile = element.containingFile?.virtualFile
            return if (virtualFile != null) {
                "${virtualFile.path}:${element.textRange.startOffset}"
            } else {
                "${System.identityHashCode(element.containingFile)}:${element.textRange.startOffset}:${element.textRange.endOffset}"
            }
        }
    }
}
