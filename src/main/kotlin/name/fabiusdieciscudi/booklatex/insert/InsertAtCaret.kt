/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.insert

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

const val LATEX_LANGUAGE_ID = "Latex"

/** True when the event points at an editor showing a LaTeX file. */
fun isLatexEditor(event: AnActionEvent): Boolean =
    event.getData(CommonDataKeys.EDITOR) != null &&
        event.getData(CommonDataKeys.PSI_FILE)?.language?.id == LATEX_LANGUAGE_ID

/**
 * Inserts [text] where the caret is, then leaves the caret [caretBackFromEnd]
 * characters short of the end -- one, to land between a pair of braces.
 */
fun insertAtCaret(
    project: Project,
    editor: Editor,
    psiFile: PsiFile,
    commandName: String,
    text: String,
    caretBackFromEnd: Int = 0,
) {
    val document = editor.document
    val offset = editor.caretModel.offset

    WriteCommandAction.runWriteCommandAction(
        project,
        commandName,
        null,
        {
            document.insertString(offset, text)
            editor.caretModel.moveToOffset(offset + text.length - caretBackFromEnd)
            PsiDocumentManager.getInstance(project).commitDocument(document)
        },
        psiFile,
    )
}

/**
 * The whitespace between the start of the caret's line and the caret, when
 * there is nothing else there. A multi-line command inserted at an indented
 * caret should keep that indent; one inserted mid-sentence has none to keep.
 */
fun indentAtCaret(editor: Editor): String {
    val document = editor.document
    val offset = editor.caretModel.offset
    val lineStart = document.getLineStartOffset(document.getLineNumber(offset))
    val before = document.getText(TextRange(lineStart, offset))
    return if (before.isBlank()) before else ""
}
