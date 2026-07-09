/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.insert

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import name.fabiusdieciscudi.booklatex.BookLatexBundle
import name.fabiusdieciscudi.booklatex.comment.COMMENT_COMMAND_NAME
import name.fabiusdieciscudi.booklatex.comment.CommentDialog

/** Inserts a `\comment{...}` through the same dialog that edits one. */
class InsertCommentAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = isLatexEditor(event)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return

        val dialog = CommentDialog(project, "")
        if (!dialog.showAndGet()) return

        val body = dialog.text().trim()
        insertAtCaret(
            project,
            editor,
            psiFile,
            BookLatexBundle.message("command.insert", COMMENT_COMMAND_NAME),
            "$COMMENT_COMMAND_NAME{$body}",
            // An empty comment is left open, with the caret inside it.
            caretBackFromEnd = if (body.isEmpty()) 1 else 0,
        )
    }
}
