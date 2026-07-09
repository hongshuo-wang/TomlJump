package com.tomljump.jetbrains

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import com.tomljump.core.ReferenceKind
import com.tomljump.core.TomlReferenceClassifier
import com.tomljump.jetbrains.resolver.TomlJumpResolver
import org.toml.lang.psi.TomlLiteral

class TomlJumpReferenceContributor : PsiReferenceContributor() {
    private val classifier = TomlReferenceClassifier()
    private val resolver = TomlJumpResolver()

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(TomlLiteral::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext,
                ): Array<PsiReference> {
                    if (!isTomlFile(element)) {
                        return PsiReference.EMPTY_ARRAY
                    }

                    val extracted = TomlStringValueExtractor.extract(element) ?: return PsiReference.EMPTY_ARRAY
                    val reference = classifier.classify(extracted.value) ?: return PsiReference.EMPTY_ARRAY
                    if (reference.kind != ReferenceKind.FILE_PATH) {
                        return PsiReference.EMPTY_ARRAY
                    }

                    return arrayOf(
                        TomlJumpStringReference(
                            element = element,
                            rangeInElement = extracted.rangeInElement,
                            tomlReference = reference,
                            resolver = resolver,
                        ),
                    )
                }
            },
        )
    }

    private fun isTomlFile(element: PsiElement): Boolean {
        val file = element.containingFile ?: return false
        val virtualFile = file.virtualFile
        return when {
            virtualFile != null -> virtualFile.extension.equals("toml", ignoreCase = true)
            else -> file.name.endsWith(".toml", ignoreCase = true)
        }
    }
}
