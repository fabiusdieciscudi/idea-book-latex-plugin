/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every character one unit wide: a width is a length. */
class WordWrapTest {

    private fun wrap(text: String, maxWidth: Int) = wrapWords(text, maxWidth) { it.length }

    @Test
    fun `a short text is one line`() {
        assertEquals(listOf("controllare la data"), wrap("controllare la data", 40))
    }

    @Test
    fun `a long text breaks between words`() {
        assertEquals(
            listOf("controllare la", "data di questa", "scena"),
            wrap("controllare la data di questa scena", 14),
        )
    }

    @Test
    fun `a line may be exactly as wide as the limit`() {
        assertEquals(listOf("abc def", "ghi"), wrap("abc def ghi", 7))
    }

    @Test
    fun `a word wider than a line is broken where it overflows`() {
        assertEquals(listOf("vedi", "https://es", "empio.org/", "pagina ora"), wrap("vedi https://esempio.org/pagina ora", 10))
    }

    @Test
    fun `the rest of a broken word shares its line with what follows`() {
        assertEquals(listOf("abcdef", "gh ij"), wrap("abcdefgh ij", 6))
    }

    @Test
    fun `nothing gives no lines`() {
        assertEquals(emptyList<String>(), wrap("", 10))
        assertEquals(emptyList<String>(), wrap("   ", 10))
    }

    @Test
    fun `a limit too narrow for any character still makes progress`() {
        assertEquals(listOf("a", "b", "c"), wrap("abc", 0))
    }

    @Test
    fun `wrapping again at the widest line gives the same lines`() {
        val text = "Jacques deve sembrare stanco qui, ma non troppo: il lettore deve capire " +
            "che ha dormito poco, non che è malato. Controllare con il capitolo 3."
        for (limit in 8..60) {
            val lines = wrap(text, limit)
            val widest = lines.maxOf { it.length }
            assertEquals("at $limit", lines, wrap(text, widest))
            assertTrue("at $limit", lines.all { it.length <= limit })
        }
    }
}
