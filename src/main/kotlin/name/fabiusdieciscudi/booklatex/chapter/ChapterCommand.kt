/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.chapter

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import nl.hannahsten.texifyidea.psi.LatexCommands

/**
 * Where the three parts of a chapter are, in a file that has exactly one.
 *
 * `\chapterwithsummary{title}{summary}{text}` is what the chapter view is built
 * around: a line for the title, a pane for the summary, a pane for the text. All
 * three are ranges of the file's own document rather than copies of it, because
 * the panes edit the document directly -- there is nothing to synchronise, and
 * nothing that can drift.
 *
 * Ranges rather than PSI elements, deliberately. The editor holding this outlives
 * any given parse, and a stale PsiElement is a leak that reads as a bug much
 * later; a range is just two numbers, and the editor recomputes them when the
 * document changes.
 *
 * A file with no such command, or with two of them, has no chapter view: [find]
 * returns null and the editor stays what it has always been. That rule lives
 * here rather than in the editor, so that it is one line to read and one test to
 * write.
 */
data class ChapterCommand(
    /** The whole command, braces and all: what the panes fold away around themselves. */
    val commandRange: TextRange,
    val title: TextRange,
    val summary: TextRange,
    val text: TextRange,
) {

    companion object {

        /** LatexCommands.getName() keeps the leading backslash. */
        const val COMMAND_NAME = "\\chapterwithsummary"

        private const val ARGUMENTS = 3

        /**
         * The chapter of [file], or null if it does not have exactly one.
         */
        fun find(file: PsiFile): ChapterCommand? {
            val command = PsiTreeUtil.findChildrenOfType(file, LatexCommands::class.java)
                .filter { it.name == COMMAND_NAME }
                // Two chapters in one file are not a chapter: fall back rather
                // than pick one and be wrong half the time.
                .singleOrNull() ?: return null

            val required = command.parameterList
                .filter { it.requiredParam != null }
                .map { it.textRange }

            return of(command.textRange, required)
        }

        /**
         * The same rules, on ranges alone. Split out from [find] so that what is
         * worth testing can be tested without a parser.
         *
         * [required] are the ranges of the required arguments, braces included,
         * in source order.
         */
        fun of(commandRange: TextRange, required: List<TextRange>): ChapterCommand? {
            // Optional arguments are filtered out before this point, so anything
            // other than three means the command is not the one we know.
            if (required.size != ARGUMENTS) return null

            val inside = required.map { it.inside() ?: return null }
            return ChapterCommand(commandRange, inside[0], inside[1], inside[2])
        }

        /**
         * What the braces hold. An empty argument gives an empty range, which is
         * allowed: a chapter whose summary has not been written yet still has a
         * title and a text, and refusing it would take the view away from the
         * author exactly when they are about to write.
         */
        private fun TextRange.inside(): TextRange? {
            if (length < 2) return null
            return TextRange(startOffset + 1, endOffset - 1)
        }
    }
}
