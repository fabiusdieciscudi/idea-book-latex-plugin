/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.dialogue

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import name.fabiusdieciscudi.booklatex.BookLatexBundle
import name.fabiusdieciscudi.booklatex.document.insideLatexComment

/**
 * Typing an opening guillemet offers the speakers the book defines.
 *
 * Choosing one replaces the character with `\sjm{}` and drops the caret between
 * the braces -- whereupon the dialogue fold paints the guillemet back, this time
 * as a placeholder with a name in front of it.
 *
 * A plain popup rather than a completion lookup: `«` is not a word, and the
 * prefix matching that would make a lookup behave is more delicate than the
 * gesture deserves.
 *
 * The offset of the typed character is remembered as a plain number, not a
 * RangeMarker. The popup holds the keyboard while it is open, so the document
 * cannot move underneath it; the character is checked once more before being
 * replaced, and that is enough.
 */
class SpeakerPopupTypedHandler : TypedHandlerDelegate() {

    override fun charTyped(char: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (char != GUILLEMET) return Result.CONTINUE
        if (file.language.id != LATEX_LANGUAGE_ID) return Result.CONTINUE

        val document = editor.document
        val offset = editor.caretModel.offset
        if (offset < 1 || document.charsSequence[offset - 1] != GUILLEMET) return Result.CONTINUE
        if (insideLatexComment(document.charsSequence, offset - 1)) return Result.CONTINUE

        // Empty while the index is still being built. Leave the character alone.
        val definitions = DialogueDefinitions.getInstance(project).definitions()
        if (definitions.isEmpty()) return Result.CONTINUE

        val choices = definitions.values
            .sortedWith(compareBy({ it.speaker }, { it.command }))
            .map<DialogueDefinition, Choice> { Choice.Speaker(it) } + Choice.Literal

        val guillemetOffset = offset - 1

        ApplicationManager.getApplication().invokeLater(
            {
                if (!editor.isDisposed) showPopup(project, editor, choices, guillemetOffset)
            },
            project.disposed,
        )

        return Result.CONTINUE
    }

    private fun showPopup(project: Project, editor: Editor, choices: List<Choice>, guillemetOffset: Int) {
        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(choices)
            .setTitle(BookLatexBundle.message("popup.speaker.title"))
            .setNamerForFiltering { it.label }
            .setItemChosenCallback { choice -> onChosen(project, editor, choice, guillemetOffset) }
            .createPopup()
            .showInBestPositionFor(editor)
    }

    private fun onChosen(project: Project, editor: Editor, choice: Choice, guillemetOffset: Int) {
        val definition = (choice as? Choice.Speaker)?.definition ?: return

        val document = editor.document
        val stillThere = guillemetOffset < document.textLength &&
            document.charsSequence[guillemetOffset] == GUILLEMET
        if (!stillThere) {
            thisLogger().warn("BookLaTeX: the guillemet at $guillemetOffset is gone, nothing inserted")
            return
        }

        val text = "${definition.command}{}"
        WriteCommandAction.runWriteCommandAction(project, BookLatexBundle.message("command.insertSpeaker", definition.command), null, {
            document.replaceString(guillemetOffset, guillemetOffset + 1, text)
            // Between the braces.
            editor.caretModel.moveToOffset(guillemetOffset + text.length - 1)
            PsiDocumentManager.getInstance(project).commitDocument(document)
        })
    }

    private sealed class Choice {

        abstract val label: String

        override fun toString(): String = label

        class Speaker(val definition: DialogueDefinition) : Choice() {
            override val label: String =
                BookLatexBundle.message("popup.speaker.item", definition.speaker, definition.command)
        }

        object Literal : Choice() {
            override val label: String = BookLatexBundle.message("popup.speaker.literal")
        }
    }

    private companion object {
        const val GUILLEMET = '\u00AB'
        const val LATEX_LANGUAGE_ID = "Latex"
    }
}
