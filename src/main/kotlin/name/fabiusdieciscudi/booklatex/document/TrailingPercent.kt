/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.document

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import name.fabiusdieciscudi.booklatex.attributes.StripSpecs
import name.fabiusdieciscudi.booklatex.comment.COMMENT_COMMAND_NAME
import nl.hannahsten.texifyidea.psi.LatexCommands

const val PERCENT = "%"

/** Every command this plugin renders, and therefore expects a `%` to follow. */
val RENDERED_COMMAND_NAMES: Set<String> =
    StripSpecs.ALL.map { it.commandName }.toSet() + COMMENT_COMMAND_NAME

/**
 * Guarantees the `%` that swallows the newline after a rendered command.
 *
 * Call from inside the write action that rewrote the command, passing a marker
 * created *before* the rewrite: the PSI is stale by then, the marker is not.
 */
fun ensureTrailingPercent(document: Document, commandRange: RangeMarker) {
    if (!commandRange.isValid) return
    val end = commandRange.endOffset
    val lineEnd = document.getLineEndOffset(document.getLineNumber(end))
    if (end > lineEnd) return

    if (document.getText(TextRange(end, lineEnd)).isBlank()) {
        document.replaceString(end, lineEnd, PERCENT)
    }
}

/**
 * The `[end of command, end of line]` ranges to overwrite with a `%`.
 *
 * A command is skipped unless it owns its lines: anything but whitespace before
 * it, or anything at all after it, and the line is not ours to touch. A line
 * that already ends in `%` therefore falls out on its own, since `%` is not
 * blank.
 */
fun collectMissingPercents(document: Document, psiFile: PsiFile): List<TextRange> =
    PsiTreeUtil.findChildrenOfType(psiFile, LatexCommands::class.java)
        .filter { it.name in RENDERED_COMMAND_NAMES }
        .mapNotNull { command ->
            val range = command.textRange
            if (range.endOffset > document.textLength) return@mapNotNull null

            val startLine = document.getLineNumber(range.startOffset)
            val lineStart = document.getLineStartOffset(startLine)
            if (document.getText(TextRange(lineStart, range.startOffset)).isNotBlank()) return@mapNotNull null

            val lineEnd = document.getLineEndOffset(document.getLineNumber(range.endOffset))
            if (range.endOffset > lineEnd) return@mapNotNull null
            if (!document.getText(TextRange(range.endOffset, lineEnd)).isBlank()) return@mapNotNull null

            TextRange(range.endOffset, lineEnd)
        }
