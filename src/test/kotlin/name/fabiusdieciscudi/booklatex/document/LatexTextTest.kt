/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.document

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LatexTextTest {

    /** The offset given is where the interesting text starts. */
    @Test
    fun `an unescaped percent earlier on the line opens a comment`() {
        assertTrue(insideLatexComment("% da finire...", 11))
        assertTrue(insideLatexComment("riga uno\n% nota...", 15))
    }

    @Test
    fun `an escaped percent does not`() {
        assertFalse(insideLatexComment("Sconto 50\\% e poi...", 16))
    }

    @Test
    fun `a comment does not reach across a line break`() {
        assertFalse(insideLatexComment("Aspetta...", 7))
        assertFalse(insideLatexComment("riga uno\ntesto...", 14))
    }

    /** All sixteen \comment commands of a real chapter were once rejected here. */
    @Test
    fun `a trailing percent still leaves the command owning its lines`() {
        assertTrue(ownsItsLines(before = "    ", after = "%"))
        assertTrue(ownsItsLines(before = "    ", after = ""))
        assertTrue(ownsItsLines(before = "", after = "  % nota"))
    }

    @Test
    fun `real content on either side means the lines are not ours`() {
        assertFalse(ownsItsLines(before = "testo ", after = ""))
        assertFalse(ownsItsLines(before = "  ", after = "}"))
        assertFalse(ownsItsLines(before = "", after = "\\ldots"))
    }
}
