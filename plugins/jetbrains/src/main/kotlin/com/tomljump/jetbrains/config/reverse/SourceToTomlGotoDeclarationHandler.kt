package com.tomljump.jetbrains.config.reverse

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiElement

class SourceToTomlGotoDeclarationHandler(
    private val resolver: SourceToTomlResolver = SourceToTomlResolver(),
) : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor,
    ): Array<PsiElement>? {
        val sourceFile = sourceElement?.containingFile ?: return null
        return try {
            val sourceOffset = sourceIdentifierOffset(sourceFile.text, offset)
            resolver.resolve(sourceFile, sourceOffset)
                .takeIf(List<PsiElement>::isNotEmpty)
                ?.toTypedArray()
        } catch (error: ProcessCanceledException) {
            throw error
        } catch (error: Throwable) {
            logger.debug("TomlJump reverse config navigation failed in ${sourceFile.name}", error)
            null
        }
    }

    companion object {
        private val logger = Logger.getInstance(SourceToTomlGotoDeclarationHandler::class.java)

        private fun sourceIdentifierOffset(sourceText: String, offset: Int): Int {
            if (sourceText.getOrNull(offset)?.isSourceNameCharacter() == true) return offset
            val previousOffset = offset - 1
            return if (sourceText.getOrNull(previousOffset)?.isSourceNameCharacter() == true) {
                previousOffset
            } else {
                offset
            }
        }

        private fun Char.isSourceNameCharacter(): Boolean {
            return isLetterOrDigit() || this == '_' || this == '$' || this == '-'
        }
    }
}
