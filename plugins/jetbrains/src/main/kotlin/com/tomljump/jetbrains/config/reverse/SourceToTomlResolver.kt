package com.tomljump.jetbrains.config.reverse

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.tomljump.core.ConfigNameNormalizer
import com.tomljump.jetbrains.config.TomlConfigKeyPathExtractor
import com.tomljump.jetbrains.config.resolver.ConfigSourceDeclaration
import com.tomljump.jetbrains.config.resolver.ConfigSourceDeclarationKind
import com.tomljump.jetbrains.config.resolver.ConfigSourceNameMatcher
import com.tomljump.jetbrains.config.resolver.GoConfigResolver
import com.tomljump.jetbrains.config.resolver.JavaConfigResolver
import com.tomljump.jetbrains.config.resolver.JavaScriptConfigResolver
import com.tomljump.jetbrains.config.resolver.PythonConfigResolver
import com.tomljump.jetbrains.config.resolver.SourcePatternResolver
import com.tomljump.jetbrains.config.resolver.TypeScriptConfigResolver
import org.toml.lang.psi.TomlKey
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlTableHeader

class SourceToTomlResolver(
    private val sourceResolvers: List<SourcePatternResolver> = listOf(
        GoConfigResolver(),
        PythonConfigResolver(),
        JavaConfigResolver(),
        TypeScriptConfigResolver(),
        JavaScriptConfigResolver(),
    ),
) {
    fun resolve(sourceFile: PsiFile, sourceOffset: Int): List<PsiElement> {
        return ApplicationManager.getApplication().runReadAction(
            Computable { resolveInReadAction(sourceFile, sourceOffset) },
        )
    }

    private fun resolveInReadAction(sourceFile: PsiFile, sourceOffset: Int): List<PsiElement> {
        val project = sourceFile.project
        if (DumbService.isDumb(project)) return emptyList()
        val extension = sourceFile.virtualFile?.extension ?: sourceFile.name.substringAfterLast('.', "")
        val sourceResolver = sourceResolvers.firstOrNull { it.supportsExtension(extension) } ?: return emptyList()
        val allDeclarations = sourceResolver.declarationsIn(sourceFile.text)
        val declarations = allDeclarations
            .filter { it.containsOffset(sourceOffset) }
        if (declarations.isEmpty()) return emptyList()

        val tomlTargets = collectTomlTargets(project)
        return declarations
            .flatMap { declaration -> matchTargets(declaration, tomlTargets) }
            .map(::TomlConfigNavigationElement)
            .distinctBy(::dedupeKey)
    }

    private fun matchTargets(
        declaration: ConfigSourceDeclaration,
        tomlTargets: List<TomlConfigNavigationTarget>,
    ): List<TomlConfigNavigationTarget> {
        if (declaration.kind == ConfigSourceDeclarationKind.CONTAINER) {
            return tomlTargets.filter { target ->
                target.kind == TomlConfigNavigationTargetKind.TABLE &&
                    ConfigSourceNameMatcher.matchesContainerName(target.keyPath.leaf, declaration.label)
            }
        }

        val ownerLabel = declaration.ownerLabel ?: return emptyList()
        return tomlTargets.filter { target ->
            if (target.kind != TomlConfigNavigationTargetKind.KEY || target.keyPath.segments.size < 2) {
                return@filter false
            }
            val containerName = target.keyPath.segments[target.keyPath.segments.lastIndex - 1]
            val fieldMatches = if (declaration.aliases.isNotEmpty()) {
                target.keyPath.leaf in declaration.aliases
            } else {
                ConfigNameNormalizer.matches(target.keyPath.leaf, declaration.label)
            }
            fieldMatches && ConfigSourceNameMatcher.matchesContainerName(containerName, ownerLabel)
        }
    }

    private fun collectTomlTargets(project: Project): List<TomlConfigNavigationTarget> {
        val scope = GlobalSearchScope.projectScope(project)
        return FilenameIndex.getAllFilesByExt(project, "toml", scope)
            .asSequence()
            .take(MAX_TOML_FILES)
            .flatMap { virtualFile ->
                ProgressManager.checkCanceled()
                if (virtualFile.length > MAX_TOML_FILE_BYTES) return@flatMap emptySequence()
                val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return@flatMap emptySequence()
                PsiTreeUtil.collectElementsOfType(psiFile, TomlKeySegment::class.java)
                    .asSequence()
                    .mapNotNull(::toNavigationTarget)
            }
            .distinctBy { target -> dedupeKey(target.element) }
            .toList()
    }

    private fun toNavigationTarget(element: TomlKeySegment): TomlConfigNavigationTarget? {
        ProgressManager.checkCanceled()
        val key = element.parent as? TomlKey ?: return null
        if (key.segments.lastOrNull() != element) return null
        val extracted = TomlConfigKeyPathExtractor.extract(element) ?: return null
        val kind = when {
            PsiTreeUtil.getParentOfType(element, TomlTableHeader::class.java, false) != null ->
                TomlConfigNavigationTargetKind.TABLE
            PsiTreeUtil.getParentOfType(element, TomlKeyValue::class.java, false) != null ->
                TomlConfigNavigationTargetKind.KEY
            else -> return null
        }
        return TomlConfigNavigationTarget(element, extracted.keyPath, kind)
    }

    private fun dedupeKey(element: PsiElement): String {
        val navigationElement = element.navigationElement
        val virtualFile = navigationElement.containingFile?.virtualFile
        return if (virtualFile != null) {
            "${virtualFile.path}:${navigationElement.textRange.startOffset}"
        } else {
            "${System.identityHashCode(navigationElement.containingFile)}:${navigationElement.textRange.startOffset}"
        }
    }

    companion object {
        private const val MAX_TOML_FILES = 500
        private const val MAX_TOML_FILE_BYTES = 1_000_000L
    }
}
