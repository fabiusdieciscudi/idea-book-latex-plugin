/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.markers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * The commands whose braces vanish and whose prose changes face. Only the table
 * is checked here; that the fold hides the wrapping and the annotator sets the
 * face is the platform's, and is looked at by running the IDE.
 */
class StyleSpecsTest {

    @Test
    fun `textbf is bold, emph is italic`() {
        assertSame(StyleSpecs.TEXTBF, StyleSpecs.forCommand("\\textbf"))
        assertSame(StyleSpecs.EMPH, StyleSpecs.forCommand("\\emph"))
        assertEquals(ProseStyle.BOLD, StyleSpecs.TEXTBF.style)
        assertEquals(ProseStyle.ITALIC, StyleSpecs.EMPH.style)
    }

    @Test
    fun `an unknown command has no spec`() {
        assertNull(StyleSpecs.forCommand("\\textit"))
        assertNull(StyleSpecs.forCommand(null))
    }

    @Test
    fun `the commands carry their backslash`() {
        StyleSpecs.ALL.forEach { assertEquals('\\', it.commandName.first()) }
    }

    @Test
    fun `no command is defined twice`() {
        val names = StyleSpecs.ALL.map { it.commandName }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `a styled command is drawn by no other mechanism`() {
        // \textbf and \emph fold and change face through StyleSpecs alone. Were
        // they also a marker, a wrap or the item command, the fold pass would try
        // to draw the same command two ways at once.
        StyleSpecs.ALL.map { it.commandName }.forEach { command ->
            assertNull(MarkerSpecs.forCommand(command))
            assertNull(WrapSpecs.forCommand(command))
            assertEquals(false, command == ListEnvironments.ITEM_COMMAND)
        }
    }
}
