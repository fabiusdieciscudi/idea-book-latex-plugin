/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.markers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The twenty-six language commands are generated from thirteen rows. These are
 * the invariants that generation has to keep.
 */
class LanguageMarkersTest {

    @Test
    fun `every language yields a prose command and a name command`() {
        assertEquals(13, Languages.ALL.size)

        val fromLanguages = Languages.ALL.flatMap { listOf(it.proseCommand, it.nameCommand) }
        assertEquals(2 * Languages.ALL.size, fromLanguages.toSet().size)

        // Which of the two the specs are is checked below; that all of them are
        // there is checked here. Counting the whole list instead would make this
        // test fail for every command added that has nothing to do with a
        // language, which is not what it is about.
        assertTrue(MarkerSpecs.ALL.map { it.commandName }.containsAll(fromLanguages))
    }

    @Test
    fun `no command is defined twice`() {
        val names = MarkerSpecs.ALL.map { it.commandName }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `both commands of a language show the same code`() {
        Languages.ALL.forEach { language ->
            assertEquals(
                MarkerSpecs.forCommand(language.proseCommand)?.marker,
                MarkerSpecs.forCommand(language.nameCommand)?.marker,
            )
        }
    }

    @Test
    fun `only the prose commands are italicised`() {
        Languages.ALL.forEach { language ->
            assertTrue(language.proseCommand in Languages.PROSE_COMMANDS)
            assertTrue(language.nameCommand !in Languages.PROSE_COMMANDS)
        }
    }

    @Test
    fun `a code is short and upper case`() {
        Languages.ALL.forEach { language ->
            assertTrue(language.isoCode.isNotBlank())
            assertTrue(language.isoCode.length in 2..3)
            assertEquals(language.isoCode.uppercase(), language.isoCode)
        }
    }

    @Test
    fun `the commands carry their backslash`() {
        assertEquals("\\french", Languages.ALL.first { it.name == "french" }.proseCommand)
        assertEquals("\\frenchname", Languages.ALL.first { it.name == "french" }.nameCommand)
        assertEquals("FR", MarkerSpecs.forCommand("\\french")?.marker)
    }

    @Test
    fun `the ellipsis needs no argument, a language does`() {
        assertEquals("\u2026", MarkerSpecs.ELLIPSIS.marker)
        assertTrue(!MarkerSpecs.ELLIPSIS.requiresArgument)
        assertTrue(MarkerSpecs.forCommand("\\latin")!!.requiresArgument)
    }

    @Test
    fun `the note carries a word and needs an argument`() {
        assertEquals("Note", MarkerSpecs.NOTE.marker)
        assertTrue(MarkerSpecs.NOTE.requiresArgument)
        assertEquals(MarkerSpecs.NOTE, MarkerSpecs.forCommand("\\note"))
    }

    @Test
    fun `an unknown command has no spec`() {
        assertNull(MarkerSpecs.forCommand("\\klingon"))
        assertNull(MarkerSpecs.forCommand(null))
        assertNotNull(WrapSpecs.forCommand("\\sq"))
    }
}
