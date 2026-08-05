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
import name.fabiusdieciscudi.booklatex.settings.BookLatexRenderingSettings

/**
 * Replaces the alternative apostrophes with a plain `'` when the file is saved.
 *
 * A sibling of [TrailingPercentOnSave], on the same hook and in the same spirit:
 * a small, undo-transparent normalisation the file needs to stay valid LaTeX. It
 * is its own listener rather than a branch of that one because the concerns are
 * separate -- one is about the `%` at the end of a line, this is about a
 * character anywhere in it -- and this one needs no PSI at all, only the text.
 *
 * The file is recognised by its extension, since there is nothing to parse: a
 * `’` is a `’` whether or not the document has been committed. Documents with no
 * file behind them -- our dialogs' own LaTeX fields -- drop out here.
 *
 * On by default, and turned off from Settings | Tools | Book LaTeX for an author
 * who would rather keep the apostrophes they typed.
 */
class ApostropheOnSave : FileDocumentManagerListener {

    override fun beforeDocumentSaving(document: Document) {
        if (!document.isWritable) return
        if (!BookLatexRenderingSettings.getInstance().normalizeApostrophes) return

        val virtualFile = FileDocumentManager.getInstance().getFile(document) ?: return
        if (virtualFile.extension?.lowercase() !in SOURCE_EXTENSIONS) return

        val offsets = Apostrophes.offsetsIn(document.charsSequence)
        if (offsets.isEmpty()) return

        CommandProcessor.getInstance().runUndoTransparentAction {
            ApplicationManager.getApplication().runWriteAction {
                // One character in, one out, so no offset shifts under another;
                // back to front all the same, to read like its trailing-% sibling.
                offsets.asReversed().forEach { offset ->
                    document.replaceString(offset, offset + 1, PLAIN)
                }
            }
        }
    }

    private companion object {
        val SOURCE_EXTENSIONS = setOf("tex", "sty", "cls")
        val PLAIN = Apostrophes.PLAIN.toString()
    }
}
