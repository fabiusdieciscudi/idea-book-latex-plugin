/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.markers

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import name.fabiusdieciscudi.booklatex.document.insideLatexComment

/**
 * Turns three typed dots into `\ellipsis`, which the marker fold then shows as
 * a single ellipsis character.
 *
 * Runs only on the character just typed, so pasted text is untouched -- a
 * pasted `...` is more likely someone else's prose than a decision to change
 * this document's markup.
 */
class EllipsisTypedHandler : TypedHandlerDelegate() {

    override fun charTyped(char: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (char != DOT) return Result.CONTINUE
        if (file.language.id != LATEX_LANGUAGE_ID) return Result.CONTINUE

        val document = editor.document
        val offset = editor.caretModel.offset
        if (offset < DOTS.length) return Result.CONTINUE

        val start = offset - DOTS.length
        if (document.charsSequence.subSequence(start, offset).toString() != DOTS) return Result.CONTINUE
        if (insideLatexComment(document.charsSequence, start)) return Result.CONTINUE

        val replacement = MarkerSpecs.ELLIPSIS.commandName
        document.replaceString(start, offset, replacement)
        editor.caretModel.moveToOffset(start + replacement.length)
        PsiDocumentManager.getInstance(project).commitDocument(document)

        return Result.STOP
    }

    private companion object {
        const val DOT = '.'
        const val DOTS = "..."
        const val LATEX_LANGUAGE_ID = "Latex"
    }
}
