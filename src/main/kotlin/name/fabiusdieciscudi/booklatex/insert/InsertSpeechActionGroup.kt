/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.insert

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runReadAction
import name.fabiusdieciscudi.booklatex.BookLatexBundle
import name.fabiusdieciscudi.booklatex.dialogue.DialogueDefinition
import name.fabiusdieciscudi.booklatex.dialogue.DialogueDefinitions

/**
 * One entry per speaker the book defines, built when the menu opens.
 *
 * No dialog: a line of dialogue is written, not filled in. The command is
 * inserted empty and the caret lands between its braces, whereupon the fold
 * shows the speaker's name and an opening guillemet.
 *
 * The list is empty while the project is being indexed, and the submenu hides
 * itself rather than lie.
 */
class InsertSpeechActionGroup : ActionGroup() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = isLatexEditor(event) && speakers(event).isNotEmpty()
        event.presentation.isPopupGroup = true
    }

    override fun getChildren(event: AnActionEvent?): Array<AnAction> {
        val definitions = event?.let { speakers(it) } ?: return AnAction.EMPTY_ARRAY
        return definitions.map { InsertSpeechAction(it) }.toTypedArray()
    }

    private fun speakers(event: AnActionEvent): List<DialogueDefinition> {
        val project = event.project ?: return emptyList()
        return runReadAction { DialogueDefinitions.getInstance(project).definitions() }
            .values
            .sortedWith(compareBy({ it.speaker }, { it.command }))
    }
}

/** Inserts `\sjm{}`, caret between the braces. */
private class InsertSpeechAction(
    private val definition: DialogueDefinition,
) : AnAction(BookLatexBundle.message("popup.speaker.item", definition.speaker, definition.command)) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return

        insertAtCaret(
            project,
            editor,
            psiFile,
            BookLatexBundle.message("command.insertSpeaker", definition.command),
            "${definition.command}{}",
            caretBackFromEnd = 1,
        )
    }
}
