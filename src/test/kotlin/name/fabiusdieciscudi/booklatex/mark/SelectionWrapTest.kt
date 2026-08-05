/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.mark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The selection is written between `⟦` and `⟧` in each example -- brackets
 * LaTeX itself never uses -- and they are taken out before the text is handed
 * over.
 */
class SelectionWrapTest {

    /** What would be wrapped, or `null` if nothing would. */
    private fun wrapped(marked: String): String? {
        val start = marked.indexOf('⟦')
        val end = marked.indexOf('⟧') - 1
        val text = marked.replace("⟦", "").replace("⟧", "")
        return SelectionWrap.rangeToWrap(text, start, end)?.substring(text)
    }

    /** The whole document after wrapping in `\suppressed`, or `null`. */
    private fun result(marked: String): String? {
        val start = marked.indexOf('⟦')
        val end = marked.indexOf('⟧') - 1
        val text = marked.replace("⟦", "").replace("⟧", "")
        val range = SelectionWrap.rangeToWrap(text, start, end) ?: return null
        return text.substring(0, range.startOffset) +
            SelectionWrap.opening("\\suppressed") +
            text.substring(range.startOffset, range.endOffset) +
            SelectionWrap.CLOSING +
            text.substring(range.endOffset)
    }

    @Test
    fun `plain prose is wrapped as it is`() {
        assertEquals("Era una notte \\suppressed{buia e} tempestosa.", result("Era una notte ⟦buia e⟧ tempestosa."))
    }

    @Test
    fun `whitespace at either end stays outside`() {
        assertEquals("buia", wrapped("Era una ⟦ buia ⟧notte."))
        assertEquals("Pioveva.", wrapped("    ⟦    Pioveva.\n⟧Fine."))
    }

    @Test
    fun `a whole line keeps its indentation and its newline outside`() {
        assertEquals("    \\suppressed{Pioveva.}\nFine.", result("⟦    Pioveva.\n⟧Fine."))
    }

    @Test
    fun `nothing but whitespace is nothing to wrap`() {
        assertNull(wrapped("Era⟦   ⟧notte."))
        assertNull(wrapped("Era⟦⟧notte."))
    }

    @Test
    fun `a comment on the last line stays outside`() {
        assertEquals("\\suppressed{\\sjm{Ciao.}}%\n", result("⟦\\sjm{Ciao.}%\n⟧"))
        assertEquals("\\suppressed{Pioveva.} % da rivedere\n", result("⟦Pioveva. % da rivedere\n⟧"))
    }

    @Test
    fun `comments on lines before the last are inside`() {
        assertEquals("Uno.%\nDue.", wrapped("⟦Uno.%\nDue.⟧"))
    }

    @Test
    fun `an escaped percent is prose`() {
        assertEquals("il 10\\% in più", wrapped("⟦il 10\\% in più⟧"))
    }

    @Test
    fun `a selection starting in a comment is refused`() {
        assertNull(wrapped("Testo % ⟦nota⟧\n"))
        assertNull(wrapped("⟦% nota⟧\n"))
    }

    @Test
    fun `whole commands and groups are wrapped`() {
        assertEquals("\\french{du pain}", wrapped("Comprò ⟦\\french{du pain}⟧ e uscì."))
        assertEquals("\\textbf{a} e {b}", wrapped("⟦\\textbf{a} e {b}⟧."))
    }

    @Test
    fun `an unmatched brace is refused`() {
        assertNull(wrapped("\\french{du ⟦pain} e⟧ altro"))
        assertNull(wrapped("altro ⟦e \\french{du⟧ pain}"))
    }

    @Test
    fun `escaped braces do not count`() {
        assertEquals("a \\{ b", wrapped("⟦a \\{ b⟧"))
    }

    @Test
    fun `a command name cut in two is refused`() {
        assertNull(wrapped("\\fre⟦nch{du pain}⟧"))
        assertNull(wrapped("⟦testo \\fre⟧nch{du pain}"))
        assertNull(wrapped("\\⟦french{du pain}⟧"))
    }

    @Test
    fun `a selection ending on a lone backslash is refused`() {
        assertNull(wrapped("⟦testo \\⟧% resto"))
    }

    @Test
    fun `a line break is prose`() {
        assertEquals("Uno.\\\\", wrapped("⟦Uno.\\\\⟧\nDue."))
    }

    @Test
    fun `an argument without its command is refused`() {
        assertNull(wrapped("\\french⟦{du pain}⟧"))
        assertNull(wrapped("\\scene[notte]⟦{strada}⟧"))
    }

    @Test
    fun `a command without its argument is refused`() {
        assertNull(wrapped("⟦e poi \\french⟧{du pain}"))
        assertNull(wrapped("⟦\\scene[notte]⟧{strada}"))
    }

    @Test
    fun `a group after a blank line is not an argument`() {
        assertEquals("{testo}", wrapped("\\french{a}\n\n⟦{testo}⟧"))
    }
}
