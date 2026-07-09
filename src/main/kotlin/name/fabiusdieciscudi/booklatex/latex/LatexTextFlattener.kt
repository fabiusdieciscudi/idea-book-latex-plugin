/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.latex

/**
 * Reduces a fragment of LaTeX to the text it would print.
 *
 * Nothing here knows about the PSI, the editor or the platform. It is the layer
 * that has produced every wrong string this plugin has ever shown, and the only
 * one a unit test can hold accountable.
 */
object LatexTextFlattener {

    /** A `%` runs to the end of its line, unless it is escaped. */
    private val COMMENT = Regex("""(?<!\\)%.*""")

    private val WHITESPACE = Regex("""\s+""")

    /** A name spelled with names, spelled with names. Four levels is generous. */
    private const val MAX_DEPTH = 4

    /**
     * Strips LaTeX comments and folds the line breaks away.
     *
     * A name is often written across several lines, each ended by the `%` that
     * swallows the newline:
     *
     *     \newcommand{\Jacques}{%
     *       \frenchname{Jacques}\xspace%
     *     }
     *
     * Taken verbatim, that body is not a name but a paragraph of source. Must
     * run before flattening: afterwards an escaped `\%` has become a bare `%`
     * and would be mistaken for a comment.
     */
    fun normalizeSource(text: String): String =
        text.lineSequence()
            .joinToString(" ") { COMMENT.replace(it, "") }
            .replace(WHITESPACE, " ")
            .trim()

    /** Normalizes, flattens, and squeezes the spaces the flattening left behind. */
    fun resolve(text: String, names: Map<String, String>): String =
        flatten(normalizeSource(text), names, 0).replace(WHITESPACE, " ").trim()

    /**
     * A command keeps only its argument: `\frenchname{Jacques}` is Jacques, and
     * the language it is set in is none of our business. A command with no
     * argument stands for a name if [names] knows it, and otherwise for nothing
     * -- `\xspace` prints no letters.
     *
     * The exception is an unknown command whose name is capitalised. That is far
     * more likely a character we failed to collect than a typesetting helper, so
     * its letters are kept: a wrong name on screen is easier to notice than a
     * missing one.
     */
    fun flatten(text: String, names: Map<String, String>, depth: Int = 0): String {
        if (depth > MAX_DEPTH) return ""

        val out = StringBuilder()
        var i = 0

        while (i < text.length) {
            val char = text[i]
            when {
                char == '\\' && i + 1 < text.length && text[i + 1].isLetter() -> {
                    var end = i + 1
                    while (end < text.length && text[end].isLetter()) end++
                    val command = text.substring(i, end)

                    var braceAt = end
                    while (braceAt < text.length && text[braceAt] == ' ') braceAt++

                    if (braceAt < text.length && text[braceAt] == '{') {
                        val close = matchingBrace(text, braceAt)
                        if (close < 0) {
                            i = end
                        } else {
                            out.append(flatten(text.substring(braceAt + 1, close), names, depth + 1))
                            i = close + 1
                        }
                    } else {
                        val body = names[command]
                        when {
                            body != null -> out.append(flatten(body, names, depth + 1))
                            command[1].isUpperCase() -> out.append(command.substring(1))
                        }
                        i = end
                    }
                }

                // \%, \&, \_: the character itself.
                char == '\\' && i + 1 < text.length -> {
                    out.append(text[i + 1])
                    i += 2
                }

                char == '{' || char == '}' -> i++

                else -> {
                    out.append(char)
                    i++
                }
            }
        }

        return out.toString()
    }

    /** Index of the `}` closing the `{` at [open], or -1. */
    fun matchingBrace(text: String, open: Int): Int {
        var depth = 0
        for (i in open until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }
}
