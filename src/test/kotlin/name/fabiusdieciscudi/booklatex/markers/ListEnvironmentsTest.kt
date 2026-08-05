/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.markers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The list environments drawn by name. Only the table is checked here; whether a
 * `\begin` folds and an `\item` finds its list is the platform's, and is looked
 * at by running the IDE, the same as every other marker.
 */
class ListEnvironmentsTest {

    @Test
    fun `itemize is drawn, an unknown environment is not`() {
        assertSame(ListEnvironments.ITEMIZE, ListEnvironments.forEnvironment("itemize"))
        assertNull(ListEnvironments.forEnvironment("enumerate"))
        assertNull(ListEnvironments.forEnvironment(null))
    }

    @Test
    fun `the name is matched without its braces or backslash`() {
        // \begin{itemize} gives "itemize" through the env identifier, so the
        // table is keyed on the bare name, not on \begin or on {itemize}.
        assertEquals("itemize", ListEnvironments.ITEMIZE.environmentName)
        assertNull(ListEnvironments.forEnvironment("{itemize}"))
        assertNull(ListEnvironments.forEnvironment("\\begin{itemize}"))
    }

    @Test
    fun `the begin and end markers name the environment`() {
        assertEquals("ITEMIZE", ListEnvironments.ITEMIZE.beginMarker)
        assertEquals("END ITEMIZE", ListEnvironments.ITEMIZE.endMarker)
    }

    @Test
    fun `an item is a bullet`() {
        assertEquals("\u2022", ListEnvironments.ITEMIZE.itemMarker)
    }

    @Test
    fun `the item command carries its backslash`() {
        // LatexCommands.getName() keeps the leading backslash, so the constant it
        // is compared against has to as well.
        assertEquals("\\item", ListEnvironments.ITEM_COMMAND)
    }

    @Test
    fun `no environment is defined twice`() {
        val names = ListEnvironments.ALL.map { it.environmentName }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `an item is not also a marker command`() {
        // \item is drawn by the list it sits in, not by a spec of its own: were it
        // both, the fold pass would try to draw it twice.
        assertNull(MarkerSpecs.forCommand(ListEnvironments.ITEM_COMMAND))
        assertTrue(WrapSpecs.forCommand(ListEnvironments.ITEM_COMMAND) == null)
    }
}
