/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.mark

import name.fabiusdieciscudi.booklatex.markers.Languages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MarkableTest {

    @Test
    fun `every command of Mark As can be unmarked`() {
        listOf(
            "\\suppressed", "\\alternate", "\\comment", "\\note",
            "\\newspapername", "\\workofartname", "\\acronym", "\\sq",
        ).forEach { assertNotNull(it, MarkableCommands.forCommand(it)) }
        Languages.ALL.forEach {
            assertNotNull(MarkableCommands.forCommand(it.proseCommand))
            assertNotNull(MarkableCommands.forCommand(it.nameCommand))
        }
    }

    @Test
    fun `nothing else`() {
        assertNull(MarkableCommands.forCommand("\\sjm"))
        assertNull(MarkableCommands.forCommand("\\scene"))
        assertNull(MarkableCommands.forCommand("\\textbf"))
        assertNull(MarkableCommands.forCommand(null))
        assertEquals(MarkableCommands.ALL.size, MarkableCommands.ALL.map { it.commandName }.toSet().size)
    }

    private fun unmark(text: String, command: String, dropPercent: Boolean = false): String {
        val start = text.indexOf(command)
        val open = text.indexOf('{', start)
        val end = start + command.length
        val edit = unmarking(text, start, end, open + 1, end - 1, dropPercent)
        return text.substring(0, edit.start) + edit.replacement + text.substring(edit.end)
    }

    @Test
    fun `the command goes, what it held stays`() {
        assertEquals("Lesse Il Secolo XIX.", unmark("Lesse \\newspapername{Il Secolo XIX}.", "\\newspapername{Il Secolo XIX}"))
    }

    @Test
    fun `an inner command goes alone`() {
        assertEquals(
            "\\newspapername{FOO}",
            unmark("\\newspapername{\\englishname{FOO}}", "\\englishname{FOO}"),
        )
    }

    @Test
    fun `the percent the save hook added goes with a command that owned its lines`() {
        assertEquals("Controllare la data.\nBernard", unmark("\\comment{Controllare la data.}%\nBernard", "\\comment{Controllare la data.}", dropPercent = true))
        assertEquals("Controllare.  ", unmark("\\comment{Controllare.}%  ", "\\comment{Controllare.}", dropPercent = true))
    }

    @Test
    fun `a percent that starts a real comment stays`() {
        assertEquals("Testo.% nota\n", unmark("\\suppressed{Testo.}% nota\n", "\\suppressed{Testo.}", dropPercent = true))
    }

    @Test
    fun `without dropPercent the percent stays`() {
        assertEquals("FOO%\n", unmark("\\acronym{FOO}%\n", "\\acronym{FOO}"))
    }
}
