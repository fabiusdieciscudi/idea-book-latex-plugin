/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.mark

import com.intellij.openapi.util.TextRange
import name.fabiusdieciscudi.booklatex.document.insideLatexComment
import name.fabiusdieciscudi.booklatex.document.ownsItsLines

/**
 * Where a selection can be wrapped in a one-argument command without breaking
 * the LaTeX around it.
 *
 * A selection is made with the mouse, and the mouse knows nothing of braces. So
 * before `\suppressed{` and `}` go in, the selection is trimmed and checked:
 *
 *  - Whitespace at either end stays outside. A line selected whole carries its
 *    newline, and the indentation before it, and neither belongs in the
 *    argument: the closing brace stays on the last line of prose, where the
 *    save hook can find it and give it its `%`.
 *  - A comment on the last line stays outside too, the closing brace going in
 *    before its `%`. Without this the brace would be commented out; and the
 *    lines of this manuscript end in `%` more often than not.
 *  - The rest is refused rather than guessed at: a selection that starts in a
 *    comment, cuts a command name in two, holds an unmatched brace, or
 *    separates a command from its arguments. Wrapping any of those would turn
 *    a well-formed document into one that does not compile, or worse, into one
 *    that compiles to something else.
 *
 * Refused is not reported: the menu entry is simply disabled. The author can
 * see what was selected, and a selection off by a brace is easy to redo.
 */
object SelectionWrap {

    const val CLOSING = "}"

    fun opening(commandName: String): String = "$commandName{"

    /**
     * The part of [start]..[end] of [text] that can be wrapped, or `null` if
     * there is nothing to wrap or it cannot be wrapped safely.
     */
    fun rangeToWrap(text: CharSequence, start: Int, end: Int): TextRange? {
        var (s, e) = trim(text, start, end) ?: return null

        if (insideLatexComment(text, s)) return null

        // A comment on the last line: the brace goes in before it.
        val comment = commentStart(text, lineStart(text, e), e)
        if (comment >= 0) {
            if (comment <= s) return null
            e = comment
            val trimmed = trim(text, s, e) ?: return null
            s = trimmed.first
            e = trimmed.second
        }

        if (splitsCommandName(text, s) || splitsCommandName(text, e)) return null
        if (!isBalanced(text, s, e)) return null
        if (startsWithDetachedArgument(text, s) || endsBeforeItsArguments(text, e)) return null

        return TextRange(s, e)
    }

    /**
     * As [rangeToWrap], but only for a block of whole lines: what is left
     * outside the braces on the first line may be indentation and nothing else,
     * on the last line whitespace or a `%` comment. Those are the lines a
     * command must own to be drawn as a block -- a `\comment` as a sticky note
     * -- so a selection that would wrap less than that is refused.
     */
    fun lineBlockToWrap(text: CharSequence, start: Int, end: Int): TextRange? {
        val range = rangeToWrap(text, start, end) ?: return null
        val before = text.subSequence(lineStart(text, range.startOffset), range.startOffset).toString()
        val after = text.subSequence(range.endOffset, lineEnd(text, range.endOffset)).toString()
        return if (ownsItsLines(before, after)) range else null
    }

    private fun lineEnd(text: CharSequence, offset: Int): Int {
        var index = offset
        while (index < text.length && text[index] != '\n') index++
        return index
    }

    private fun trim(text: CharSequence, start: Int, end: Int): Pair<Int, Int>? {
        var s = start
        var e = end
        while (s < e && text[s].isWhitespace()) s++
        while (e > s && text[e - 1].isWhitespace()) e--
        return if (s < e) s to e else null
    }

    private fun lineStart(text: CharSequence, offset: Int): Int {
        var index = offset
        while (index > 0 && text[index - 1] != '\n') index--
        return index
    }

    /** The first unescaped `%` in [from]..[to], or -1. */
    private fun commentStart(text: CharSequence, from: Int, to: Int): Int {
        var index = from
        while (index < to) {
            when (text[index]) {
                '\\' -> index++
                '%' -> return index
            }
            index++
        }
        return -1
    }

    /**
     * Braces matched, and nothing left hanging at the end. Escaped braces do
     * not count, nor do braces in a comment; a backslash with nothing after it
     * would escape the closing brace.
     */
    private fun isBalanced(text: CharSequence, start: Int, end: Int): Boolean {
        var depth = 0
        var index = start
        while (index < end) {
            when (text[index]) {
                '\\' -> {
                    if (index + 1 >= end) return false
                    index += 2
                    continue
                }
                '%' -> {
                    var newline = index
                    while (newline < end && text[newline] != '\n') newline++
                    if (newline >= end) return false
                    index = newline + 1
                    continue
                }
                '{' -> depth++
                '}' -> if (--depth < 0) return false
            }
            index++
        }
        return depth == 0
    }

    /** Whether [offset] falls inside a command name: `\fre|nch`, or just after its backslash. */
    private fun splitsCommandName(text: CharSequence, offset: Int): Boolean {
        if (offset <= 0 || offset >= text.length) return false
        if (startsCommand(text, offset - 1)) return true
        if (!text[offset].isTexLetter() || !text[offset - 1].isTexLetter()) return false
        return endsCommandName(text, offset)
    }

    /**
     * Whether the selection opens on an argument whose command is outside it,
     * as `{du pain}` in `\french{du pain}`: the command would take
     * `\suppressed` as its argument. A group after a closing brace is refused
     * too, since it may be the second argument of a command; wrapping a plain
     * group there is rare enough to lose.
     */
    private fun startsWithDetachedArgument(text: CharSequence, start: Int): Boolean {
        if (text[start] != '{' && text[start] != '[') return false
        val before = previousNonBlank(text, start)
        if (before < 0) return false
        return text[before] == '}' || text[before] == ']' || endsCommand(text, before + 1)
    }

    /** The mirror of [startsWithDetachedArgument]: the selection closes on a command whose argument follows. */
    private fun endsBeforeItsArguments(text: CharSequence, end: Int): Boolean {
        val after = nextNonBlank(text, end)
        if (after >= text.length || (text[after] != '{' && text[after] != '[')) return false
        val last = text[end - 1]
        return last == '}' || last == ']' || endsCommand(text, end)
    }

    /** Whether the text just before [offset] is a command: a name, or a single escaped character. */
    private fun endsCommand(text: CharSequence, offset: Int): Boolean {
        if (offset < 2) return false
        if (startsCommand(text, offset - 2)) return true
        return text[offset - 1].isTexLetter() && endsCommandName(text, offset)
    }

    /** Whether the letters just before [offset] are preceded by a backslash that opens a command. */
    private fun endsCommandName(text: CharSequence, offset: Int): Boolean {
        var index = offset - 1
        while (index >= 0 && text[index].isTexLetter()) index--
        return index >= 0 && startsCommand(text, index)
    }

    /** Whether the backslash at [index] opens a command, rather than being the second half of a `\\`. */
    private fun startsCommand(text: CharSequence, index: Int): Boolean {
        if (text[index] != '\\') return false
        var count = 0
        var cursor = index
        while (cursor >= 0 && text[cursor] == '\\') {
            count++
            cursor--
        }
        return count % 2 == 1
    }

    /** Spaces, tabs and at most one newline: what TeX skips between a command and its arguments. */
    private fun previousNonBlank(text: CharSequence, offset: Int): Int {
        var index = offset - 1
        var newlines = 0
        while (index >= 0 && text[index].isWhitespace()) {
            if (text[index] == '\n' && ++newlines > 1) return -1
            index--
        }
        return index
    }

    private fun nextNonBlank(text: CharSequence, offset: Int): Int {
        var index = offset
        var newlines = 0
        while (index < text.length && text[index].isWhitespace()) {
            if (text[index] == '\n' && ++newlines > 1) return text.length
            index++
        }
        return index
    }

    private fun Char.isTexLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'
}
