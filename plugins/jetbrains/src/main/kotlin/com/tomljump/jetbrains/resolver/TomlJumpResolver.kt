package com.tomljump.jetbrains.resolver

import com.intellij.psi.PsiElement
import com.tomljump.core.ReferenceKind
import com.tomljump.core.TomlReference

class TomlJumpResolver(
    private val filePathResolver: FilePathResolver = FilePathResolver(),
    private val symbolShapeResolver: SymbolShapeResolver = SymbolShapeResolver(),
) {
    fun resolve(element: PsiElement, reference: TomlReference): List<PsiElement> {
        return when (reference.kind) {
            ReferenceKind.FILE_PATH -> filePathResolver.resolve(element, reference)
            ReferenceKind.CLASS_OR_MODULE,
            ReferenceKind.CALLABLE,
            -> symbolShapeResolver.resolve(element, reference)
        }
    }
}
