/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.render

/**
 * A whole-line `%` comment that reads as a horizontal rule.
 *
 * `%%%%%%` alone on a line is a divider, drawn as a blue line the width of the
 * editor. `%%%%%% SCENA %%%%%%` is the same divider with a label, drawn raised
 * and blue in front of the line, the `%` fill on either side dropped.
 *
 * [caption] is null for a bare rule and, for a labelled one, the words between
 * the runs of `%`, trimmed and with runs of space collapsed. Either way the `%`
 * stay in the file: this decides only what is drawn, never what is written.
 *
 * The rule is on ranges of text alone -- what makes a divider a divider rather
 * than an ordinary comment -- so it can be tested without an editor. Whether the
 * line it sits on is really a comment, and where on screen it lands, is the pass
 * that uses this.
 */
data class CommentRule(val caption: String?) {

    companion object {

        private const val PERCENT = '%'

        /** A run of two: the fill that tells a divider from a one-`%` comment. */
        private const val FILL = "%%"

        private val WHITESPACE = Regex("\\s+")

        /**
         * The rule a line draws, or null if it draws none.
         *
         * A divider is a line whose content is `%` and space with a run of at
         * least two `%` somewhere -- that run is the fill the eye reads as a line.
         * `% una nota` has one `%` and is left alone; so is `% sconto 50%`, whose
         * two `%` never touch. A label is only a label when it is walled off by
         * `%` on both sides, so the line still ends in one: `%% da rivedere`,
         * which does not, keeps its own line as a comment.
         */
        fun of(line: String): CommentRule? {
            val trimmed = line.trim()
            if (!trimmed.startsWith(PERCENT)) return null
            if (!trimmed.contains(FILL)) return null

            // Only % and space: a bare rule, no label.
            if (trimmed.all { it == PERCENT || it.isWhitespace() }) return CommentRule(null)

            // A label sits between two runs of %, so the line ends in one.
            if (!trimmed.endsWith(PERCENT)) return null

            val caption = trimmed
                .dropWhile { it == PERCENT }
                .dropLastWhile { it == PERCENT }
                .replace(WHITESPACE, " ")
                .trim()

            return if (caption.isEmpty()) CommentRule(null) else CommentRule(caption)
        }
    }
}
