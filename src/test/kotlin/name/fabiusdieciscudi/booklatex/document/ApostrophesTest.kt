/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which characters count as an apostrophe to put back, and where they are. The
 * replacement itself is one character for one, so the offsets are all the save
 * hook needs; those are what is checked here.
 */
class ApostrophesTest {

    @Test
    fun `the smart apostrophe and its kin are alternatives`() {
        assertTrue(Apostrophes.isAlternative('\u2019'))
        assertTrue(Apostrophes.isAlternative('\u02BC'))
        assertTrue(Apostrophes.isAlternative('\uFF07'))
    }

    @Test
    fun `a plain apostrophe is not an alternative to itself`() {
        assertFalse(Apostrophes.isAlternative('\''))
    }

    @Test
    fun `the opening quote and the accents are left alone`() {
        // The left single quote is an opening quote, whose LaTeX is a backtick;
        // the grave is already that backtick; the acute is an accent. None of
        // them is a right-single-quote apostrophe, so none is replaced.
        assertFalse(Apostrophes.isAlternative('\u2018')) // ‘ left single quote
        assertFalse(Apostrophes.isAlternative('`'))      // grave accent
        assertFalse(Apostrophes.isAlternative('\u00B4')) // ´ acute accent
    }

    @Test
    fun `offsets point at every alternative, ascending`() {
        // "l’aria" then a fullwidth one: positions 1 and 7.
        val text = "l\u2019aria \uFF07x"
        assertEquals(listOf(1, 7), Apostrophes.offsetsIn(text))
    }

    @Test
    fun `plain text has no offsets to change`() {
        assertEquals(emptyList<Int>(), Apostrophes.offsetsIn("l'aria, `quote', 50%"))
    }

    @Test
    fun `replacing at the offsets yields plain apostrophes`() {
        val text = "d\u2019accordo"
        val rebuilt = buildString {
            append(text)
            Apostrophes.offsetsIn(text).forEach { setCharAt(it, Apostrophes.PLAIN) }
        }
        assertEquals("d'accordo", rebuilt)
    }
}
