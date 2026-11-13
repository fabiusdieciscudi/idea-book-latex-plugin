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
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import name.fabiusdieciscudi.booklatex.BookLatexBundle
import name.fabiusdieciscudi.booklatex.document.RENDERED_COMMAND_NAMES
import name.fabiusdieciscudi.booklatex.insert.isLatexEditor
import nl.hannahsten.texifyidea.psi.LatexCommands

/**
 * The Unmark As submenu, just below Mark As: the commands of Mark As that hold
 * the caret, innermost first. In `\newspapername{\englishname{FOO}}` with the
 * caret in `FOO`, *English name* and then *Newspaper*.
 *
 * Only without a selection: a selection is what Mark As works on, the caret is
 * what Unmark As does. A command holds the caret when the caret is between its
 * braces -- just after the opening one and just before the closing one count.
 */
class UnmarkAsMenuGroup : ActionGroup() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val visible = isLatexEditor(event)
        event.presentation.isVisible = visible
        event.presentation.isEnabled = visible && enclosingCommands(event).isNotEmpty()
    }

    override fun getChildren(event: AnActionEvent?): Array<AnAction> =
        event?.let { enclosingCommands(it) }.orEmpty()
            .map { (pointer, markable) -> UnmarkAction(pointer, label(markable)) }
            .toTypedArray()

    private fun enclosingCommands(event: AnActionEvent): List<Pair<SmartPsiElementPointer<LatexCommands>, MarkableCommand>> {
        if (!isLatexEditor(event)) return emptyList()
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return emptyList()
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return emptyList()
        if (editor.caretModel.allCarets.any { it.hasSelection() }) return emptyList()

        val offset = editor.caretModel.offset
        val leaf = psiFile.findElementAt(offset) ?: psiFile.findElementAt(offset - 1) ?: return emptyList()

        return generateSequence(leaf) { it.parent }
            .takeWhile { it !is PsiFile }
            .filterIsInstance<LatexCommands>()
            .mapNotNull { command ->
                val markable = MarkableCommands.forCommand(command.name) ?: return@mapNotNull null
                val argument = command.requiredArgumentRange() ?: return@mapNotNull null
                if (offset <= argument.startOffset || offset >= argument.endOffset) return@mapNotNull null
                SmartPointerManager.createPointer(command) to markable
            }
            .toList()
    }

    private fun label(markable: MarkableCommand): String {
        val nameKey = markable.nameKey
        return if (nameKey == null) BookLatexBundle.message(markable.labelKey)
        else BookLatexBundle.message(markable.labelKey, BookLatexBundle.message(nameKey))
    }
}

/** The argument of [this], braces included. */
private fun LatexCommands.requiredArgumentRange() = parameterList.firstOrNull { it.requiredParam != null }?.textRange

/**
 * One entry of Unmark As: takes its command away, keeping what it held, and
 * leaves the caret where it was in that text.
 */
private class UnmarkAction(
    private val pointer: SmartPsiElementPointer<LatexCommands>,
    text: String,
) : AnAction(text) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val command = pointer.element ?: return
        val argument = command.requiredArgumentRange() ?: return
        val document = editor.document
        val range = command.textRange

        val edit = unmarking(
            document.charsSequence,
            commandStart = range.startOffset,
            commandEnd = range.endOffset,
            contentStart = argument.startOffset + 1,
            contentEnd = argument.endOffset - 1,
            dropPercent = command.name in RENDERED_COMMAND_NAMES,
        )
        // The caret was in the content; it moves back by what preceded the content.
        val caret = editor.caretModel.offset - (argument.startOffset + 1 - range.startOffset)

        WriteCommandAction.runWriteCommandAction(
            project,
            BookLatexBundle.message("command.unmarkAs", command.name.orEmpty()),
            null,
            {
                document.replaceString(edit.start, edit.end, edit.replacement)
                editor.caretModel.moveToOffset(caret.coerceIn(edit.start, edit.start + edit.replacement.length))
                PsiDocumentManager.getInstance(project).commitDocument(document)
            },
            command.containingFile,
        )
    }
}
