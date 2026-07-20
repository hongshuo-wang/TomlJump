package com.tomljump.jetbrains.entrypoint

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.tomljump.jetbrains.config.resolver.ConfigSourcePsiElement
import com.tomljump.jetbrains.config.resolver.ConfigSourceTarget
import com.tomljump.jetbrains.config.resolver.SourceStructureScanner

class PythonEntryPointResolver {
    fun resolveModule(element: PsiElement, qualifier: String): List<PsiElement> {
        return resolve(element.project, qualifier, member = null)
    }

    fun resolveCallable(element: PsiElement, qualifier: String, member: String): List<PsiElement> {
        return resolve(element.project, qualifier, member)
    }

    private fun resolve(project: Project, qualifier: String, member: String?): List<PsiElement> {
        return ApplicationManager.getApplication().runReadAction(
            Computable { resolveInReadAction(project, qualifier, member) },
        )
    }

    private fun resolveInReadAction(
        project: Project,
        qualifier: String,
        member: String?,
    ): List<PsiElement> {
        if (DumbService.isDumb(project)) return emptyList()
        val modulePath = qualifier.replace('.', '/')
        val scope = GlobalSearchScope.projectScope(project)
        return FilenameIndex.getAllFilesByExt(project, "py", scope)
            .asSequence()
            .take(MAX_PYTHON_FILES)
            .filter { virtualFile ->
                ProgressManager.checkCanceled()
                val path = virtualFile.path.replace('\\', '/')
                path.endsWith("/$modulePath.py") || path.endsWith("/$modulePath/__init__.py")
            }
            .filter { it.length <= MAX_PYTHON_FILE_BYTES }
            .mapNotNull { virtualFile ->
                ProgressManager.checkCanceled()
                PsiManager.getInstance(project).findFile(virtualFile)
            }
            .mapNotNull { psiFile ->
                if (member == null) {
                    psiFile
                } else {
                    val target = findTopLevelCallable(psiFile.text, member) ?: return@mapNotNull null
                    ConfigSourcePsiElement(psiFile, target)
                }
            }
            .distinctBy { target ->
                val file = target.containingFile.virtualFile
                "${file?.path}:${target.textOffset}"
            }
            .toList()
    }

    private fun findTopLevelCallable(sourceText: String, member: String): ConfigSourceTarget? {
        val pattern = Regex(
            "(?m)^(?:async[ \\t]+)?def[ \\t]+(${Regex.escape(member)})[ \\t]*\\(",
        )
        return pattern.findAll(sourceText).firstNotNullOfOrNull { match ->
            val offset = match.range.first + match.value.indexOf(member)
            if (!SourceStructureScanner.isCodeOffset(sourceText, offset, hashLineComments = true)) {
                null
            } else {
                ConfigSourceTarget(offset, member)
            }
        }
    }

    companion object {
        private const val MAX_PYTHON_FILES = 500
        private const val MAX_PYTHON_FILE_BYTES = 1_000_000L
    }
}
