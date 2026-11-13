/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.sound

/** Replace [start]..[end] with [replacement]. */
data class SoundTextEdit(val start: Int, val end: Int, val replacement: String)

/**
 * The `%` after a sound command, which keeps the newline after it from becoming
 * a space in the book.
 *
 * The two commands follow different rules, because they stand in different
 * places:
 *
 *  - `\sfx` gets one only when nothing but whitespace follows it on its line,
 *    whatever precedes it: an effect inside a sentence, with more of the
 *    sentence after it, is left alone.
 *  - `\ambient` always gets one, because nothing may follow it on its line. If
 *    something does, the line is broken after the `%`, and what followed moves
 *    to a line of its own, indented as the command's was.
 *
 * Trailing whitespace before the `%` goes. A command already followed by a `%`,
 * or by a comment, is left as it is.
 */
object SoundPercent {

    fun edit(text: CharSequence, commandStart: Int, commandEnd: Int, spec: SoundSpec): SoundTextEdit? {
        var lineEnd = commandEnd
        while (lineEnd < text.length && text[lineEnd] != '\n') lineEnd++
        val rest = text.subSequence(commandEnd, lineEnd)

        if (rest.isBlank()) return SoundTextEdit(commandEnd, lineEnd, PERCENT)
        if (rest.trimStart().startsWith(PERCENT)) return null
        if (!spec.endsItsLine) return null

        var restStart = commandEnd
        while (restStart < lineEnd && text[restStart].isWhitespace()) restStart++
        return SoundTextEdit(commandEnd, restStart, PERCENT + "\n" + indentation(text, commandStart))
    }

    /** The whitespace that opens the line [offset] is on. */
    private fun indentation(text: CharSequence, offset: Int): String {
        var lineStart = offset
        while (lineStart > 0 && text[lineStart - 1] != '\n') lineStart--
        var indentEnd = lineStart
        while (indentEnd < text.length && (text[indentEnd] == ' ' || text[indentEnd] == '\t')) indentEnd++
        return text.subSequence(lineStart, indentEnd).toString()
    }

    private const val PERCENT = "%"
}
