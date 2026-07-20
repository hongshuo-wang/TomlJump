package com.tomljump.jetbrains.config

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import org.toml.lang.psi.TomlKeySegment

class TomlConfigReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(TomlKeySegment::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext,
                ): Array<PsiReference> {
                    val extracted = TomlConfigKeyPathExtractor.extract(element) ?: return PsiReference.EMPTY_ARRAY
                    return arrayOf(
                        TomlConfigReference(
                            element = element,
                            rangeInElement = extracted.rangeInElement,
                            keyPath = extracted.keyPath,
                            kind = extracted.kind,
                        ),
                    )
                }
            },
        )
    }
}
