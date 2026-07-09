/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.document

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.ProjectLocator
import com.intellij.psi.PsiDocumentManager

/**
 * Appends the missing `%` to every rendered command when the file is saved.
 *
 * The same hook the platform uses to strip trailing whitespace on save. Edits
 * run back to front so earlier insertions do not shift later offsets, and
 * undo-transparently, so the history does not gain a step the user never took.
 *
 * Documents with no file behind them -- the in-memory LaTeX fields of our own
 * dialogs, for instance -- have no project and drop out here.
 */
class TrailingPercentOnSave : FileDocumentManagerListener {

    override fun beforeDocumentSaving(document: Document) {
        if (!document.isWritable) return

        val virtualFile = FileDocumentManager.getInstance().getFile(document) ?: return
        val project = ProjectLocator.getInstance().guessProjectForFile(virtualFile) ?: return
        if (project.isDisposed) return

        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        psiDocumentManager.commitDocument(document)
        val psiFile = psiDocumentManager.getPsiFile(document) ?: return
        if (psiFile.language.id != LATEX_LANGUAGE_ID) return

        val ranges = collectMissingPercents(document, psiFile)
        if (ranges.isEmpty()) return

        CommandProcessor.getInstance().runUndoTransparentAction {
            ApplicationManager.getApplication().runWriteAction {
                ranges.sortedByDescending { it.startOffset }.forEach { range ->
                    document.replaceString(range.startOffset, range.endOffset, PERCENT)
                }
            }
        }
    }

    private companion object {
        const val LATEX_LANGUAGE_ID = "Latex"
    }
}
