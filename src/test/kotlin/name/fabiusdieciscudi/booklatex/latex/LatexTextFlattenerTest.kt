/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.latex

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Every case here was once a bug on screen, or is the shape of a definition
 * taken verbatim from a real preamble.
 */
class LatexTextFlattenerTest {

    private val names = mapOf(
        // Written across lines, each closed by the % that swallows the newline.
        "\\Jacques" to normalized("%\n  \\frenchname{Jacques}\\xspace%\n"),
        "\\JeanMarie" to normalized("\\frenchname{Jean-Marie}\\xspace"),
        "\\Eidith" to normalized("\\frenchname{Eidith}\\xspace"),
        "\\MacInnes" to normalized("\\frenchname{MacInnes}\\xspace"),
        "\\Roberto" to normalized("Roberto\\xspace"),
        "\\MariaDelDivinCuore" to normalized("Maria del Divin Cuore\\xspace"),
    )

    private fun normalized(text: String) = LatexTextFlattener.normalizeSource(text)

    private fun resolve(text: String) = LatexTextFlattener.resolve(text, names)

    @Test
    fun `a name macro resolves to the name it prints`() {
        assertEquals("Jacques", resolve("\\Jacques"))
        assertEquals("Roberto", resolve("\\Roberto"))
    }

    @Test
    fun `a command keeps only its argument`() {
        assertEquals("Jacques", resolve("\\frenchname{Jacques}"))
        assertEquals("Jean-Marie", resolve("\\textbf{\\frenchname{Jean-Marie}}"))
        assertEquals("Jacques", resolve("\\frenchname {Jacques}"))
    }

    /** `\src` once read as "Robertoxspace": \xspace lost its backslash. */
    @Test
    fun `typography with no argument prints nothing`() {
        assertEquals("Roberto", resolve("\\Roberto\\xspace"))
        assertEquals("Jacques", resolve("\\Jacques\\ignorespaces"))
    }

    /** `\sjm` once read as "%": the body of \Jacques is a paragraph of source. */
    @Test
    fun `comments and line breaks are stripped from a body`() {
        assertEquals("Jacques", resolve("%\n \\frenchname{Jacques}\\xspace%\n"))
    }

    @Test
    fun `a title keeps the name after it`() {
        assertEquals("Padre Jean-Marie", resolve("Padre \\JeanMarie"))
        assertEquals("Suor Maria del Divin Cuore", resolve("Suor \\MariaDelDivinCuore"))
    }

    @Test
    fun `a speaker may be spelled with two name macros`() {
        assertEquals("Eidith MacInnes", resolve("\\Eidith \\MacInnes"))
    }

    /** A capitalised unknown command is far likelier a name than a helper. */
    @Test
    fun `an unresolved name keeps its letters`() {
        assertEquals("Frate Mariano", resolve("Frate \\Mariano"))
        assertEquals("Monsignor Ludovisi", resolve("Monsignor \\Ludovisi"))
        assertEquals("Sconosciuto", resolve("\\Sconosciuto"))
    }

    @Test
    fun `plain text passes through`() {
        assertEquals("Ginecologa", resolve("Ginecologa"))
        assertEquals("Avventore Siena 1", resolve("Avventore Siena 1"))
    }

    /** Stripping comments after flattening ate the rest of the line. */
    @Test
    fun `an escaped percent is a percent, not a comment`() {
        assertEquals("50% sconto", resolve("50\\% sconto"))
    }

    @Test
    fun `nesting stops at a sane depth instead of looping`() {
        val cyclic = mapOf("\\A" to "\\B", "\\B" to "\\A")
        assertEquals("", LatexTextFlattener.resolve("\\A", cyclic))
    }

    /** The command is dropped, its text survives: better half a name than none. */
    @Test
    fun `an unbalanced brace does not run away`() {
        assertEquals("Jacques", resolve("\\frenchname{Jacques"))
    }

    @Test
    fun `matchingBrace finds the closing brace across nesting`() {
        val text = "\\a{b{c}d}e"
        assertEquals(8, LatexTextFlattener.matchingBrace(text, 2))
        assertEquals(-1, LatexTextFlattener.matchingBrace("{unclosed", 0))
    }
}
