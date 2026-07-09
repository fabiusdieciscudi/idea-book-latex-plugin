/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.document

const val COMMENT_CHAR = "%"

/**
 * True when everything from [offset] to the end of its line is commented out.
 *
 * An unescaped `%` earlier on the line opens the comment; an escaped `\%` does
 * not, which is why this walks the line instead of searching for a character.
 */
fun insideLatexComment(text: CharSequence, offset: Int): Boolean {
    var index = offset - 1
    while (index >= 0 && text[index] != '\n') {
        if (text[index] == '%' && (index == 0 || text[index - 1] != '\\')) return true
        index--
    }
    return false
}

/**
 * Whether a command owns the lines it sits on, given what precedes it on its
 * first line and what follows it on its last.
 *
 * Indentation before is fine. After it, only whitespace or a `%` comment may
 * follow: neither produces output, and `\comment{...}%` is the idiomatic way to
 * swallow the trailing newline.
 */
fun ownsItsLines(before: String, after: String): Boolean {
    if (before.isNotBlank()) return false
    val trailing = after.trimStart()
    return trailing.isEmpty() || trailing.startsWith(COMMENT_CHAR)
}
