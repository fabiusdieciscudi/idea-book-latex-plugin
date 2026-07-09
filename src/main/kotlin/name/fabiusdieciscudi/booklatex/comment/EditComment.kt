/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.comment

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import name.fabiusdieciscudi.booklatex.BookLatexBundle
import name.fabiusdieciscudi.booklatex.document.ensureTrailingPercent
import nl.hannahsten.texifyidea.psi.LatexCommands

/** LatexCommands.getName() keeps the leading backslash. */
const val COMMENT_COMMAND_NAME = "\\comment"

/**
 * Opens the multi-line editor for the `\comment` inside the given offsets and,
 * on confirmation, replaces its required argument.
 *
 * Must be called on the EDT: the dialog is modal.
 */
fun editComment(editor: Editor, startOffset: Int, endOffset: Int) {
    val project = editor.project ?: return
    val document = editor.document
    val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return

    val command = PsiTreeUtil.findChildrenOfType(psiFile, LatexCommands::class.java)
        .firstOrNull {
            it.name == COMMENT_COMMAND_NAME &&
                it.textRange.startOffset >= startOffset &&
                it.textRange.endOffset <= endOffset
        } ?: return

    // The braces are part of this range, so they are rewritten along with the body.
    val argumentRange = command.parameterList.firstOrNull { it.requiredParam != null }?.textRange ?: return
    val dialog = CommentDialog(project, command.requiredParameterText(0).orEmpty())
    if (!dialog.showAndGet()) return

    val body = dialog.text().trim()
    val commandMarker = document.createRangeMarker(command.textRange).apply { isGreedyToRight = true }

    WriteCommandAction.runWriteCommandAction(
        project,
        BookLatexBundle.message("command.editComment"),
        null,
        {
            document.replaceString(argumentRange.startOffset, argumentRange.endOffset, "{$body}")
            ensureTrailingPercent(document, commandMarker)
            PsiDocumentManager.getInstance(project).commitDocument(document)
        },
        psiFile,
    )
    commandMarker.dispose()
}
