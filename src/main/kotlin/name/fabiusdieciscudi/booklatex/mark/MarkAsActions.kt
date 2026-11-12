/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.mark

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import name.fabiusdieciscudi.booklatex.BookLatexBundle
import name.fabiusdieciscudi.booklatex.insert.isLatexEditor
import name.fabiusdieciscudi.booklatex.markers.ALTERNATE_COMMAND_NAME
import name.fabiusdieciscudi.booklatex.markers.Languages
import name.fabiusdieciscudi.booklatex.markers.MarkerSpecs
import name.fabiusdieciscudi.booklatex.markers.SUPPRESSED_COMMAND_NAME

/**
 * The Mark As submenu of the editor's context menu: shown in a LaTeX editor,
 * enabled only when something is selected, since there is nothing to mark
 * otherwise.
 */
class MarkAsMenuGroup : DefaultActionGroup() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val visible = isLatexEditor(event)
        event.presentation.isVisible = visible
        event.presentation.isEnabled = visible &&
            event.getData(CommonDataKeys.EDITOR)?.caretModel?.allCarets?.any { it.hasSelection() } == true
    }
}

/**
 * Wraps the selection in a one-argument command: `testo` becomes
 * `\suppressed{testo}`.
 *
 * What goes between the braces is decided by [SelectionWrap]: whitespace and a
 * trailing comment stay outside, and a selection that would break the document
 * disables the entry instead of being wrapped. Every caret with a selection is
 * wrapped, bottom to top so that each insertion leaves the offsets above it
 * alone, all in one undoable command; afterwards each caret sits just past the
 * closing brace it added.
 *
 * The two kinds of entry are built two ways. Suppressed and Alternate are
 * registered in plugin.xml, and take their text from the bundle by id; the
 * languages are built by [MarkAsLanguageActionGroup], and are given theirs.
 */
open class MarkAsAction : AnAction {

    private val commandName: String

    protected constructor(commandName: String) : super() {
        this.commandName = commandName
    }

    constructor(commandName: String, text: String) : super(text) {
        this.commandName = commandName
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        event.presentation.isVisible = isLatexEditor(event)
        event.presentation.isEnabled = editor != null && isLatexEditor(event) && canWrap(editor)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return
        val document = editor.document
        val opening = SelectionWrap.opening(commandName)

        WriteCommandAction.runWriteCommandAction(
            project,
            BookLatexBundle.message("command.markAs", commandName),
            null,
            {
                editor.caretModel.runForEachCaret({ caret ->
                    if (!caret.hasSelection()) return@runForEachCaret
                    val range = SelectionWrap.rangeToWrap(
                        document.charsSequence,
                        caret.selectionStart,
                        caret.selectionEnd,
                    ) ?: return@runForEachCaret

                    // The closing brace first, so the opening one does not move it.
                    document.insertString(range.endOffset, SelectionWrap.CLOSING)
                    document.insertString(range.startOffset, opening)
                    caret.removeSelection()
                    caret.moveToOffset(range.endOffset + opening.length + SelectionWrap.CLOSING.length)
                }, true)
                PsiDocumentManager.getInstance(project).commitDocument(document)
            },
            psiFile,
        )
    }

    /** Something selected, and every selection wrappable: a partial wrap would be a surprise. */
    private fun canWrap(editor: Editor): Boolean {
        val text = editor.document.charsSequence
        val selected = editor.caretModel.allCarets.filter { it.hasSelection() }
        return selected.isNotEmpty() &&
            selected.all { SelectionWrap.rangeToWrap(text, it.selectionStart, it.selectionEnd) != null }
    }
}

/** Mark As | Suppressed: `\suppressed{...}`, prose cut but kept. */
class MarkAsSuppressedAction : MarkAsAction(SUPPRESSED_COMMAND_NAME)

/** Mark As | Alternate: `\alternate{...}`, another version of a passage. */
class MarkAsAlternateAction : MarkAsAction(ALTERNATE_COMMAND_NAME)

/** Mark As | Newspaper: `\newspapername{...}`, the name of a newspaper. */
class MarkAsNewspaperAction : MarkAsAction(MarkerSpecs.NEWSPAPER_NAME.commandName)

/** Mark As | Work of Art: `\workofartname{...}`, the title of a book, a painting, a film. */
class MarkAsWorkOfArtAction : MarkAsAction(MarkerSpecs.WORK_OF_ART_NAME.commandName)

/**
 * Mark As | Language: two entries per language the book marks up, in
 * alphabetical order of its name.
 *
 * *French* wraps the selection in the prose command, `\french{...}`, set in
 * italics; *French name* in the name command, `\frenchname{...}`, left upright.
 * The two sit side by side, so the choice between them is made where the
 * language is chosen. The entries are generated from [Languages], so a new
 * language is in the menu as soon as it is in the table.
 *
 * A popup of its own, declared so in plugin.xml: thirty entries would bury
 * Suppressed and Alternate if they stood in the Mark As menu itself.
 */
class MarkAsLanguageActionGroup : ActionGroup() {

    private val entries: Array<AnAction> by lazy {
        Languages.ALL
            .map { it to BookLatexBundle.message("language.${it.name}") }
            .sortedBy { (_, name) -> name }
            .flatMap { (language, name) ->
                listOf(
                    MarkAsAction(language.proseCommand, name),
                    MarkAsAction(language.nameCommand, BookLatexBundle.message("language.nameEntry", name)),
                )
            }
            .toTypedArray()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun getChildren(event: AnActionEvent?): Array<AnAction> = entries
}
