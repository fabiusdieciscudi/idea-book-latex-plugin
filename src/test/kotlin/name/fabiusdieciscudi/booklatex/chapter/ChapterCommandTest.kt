/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.chapter

import com.intellij.openapi.util.TextRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules that decide whether a file gets the chapter view, and where its three
 * parts are. Ranges only: the parser is not what is being tested here.
 */
class ChapterCommandTest {

    /** `\chapterwithsummary{Uno}{Due}{Tre}` laid out by hand. */
    private val whole = TextRange(0, 34)
    private val braced = listOf(TextRange(19, 24), TextRange(24, 29), TextRange(29, 34))

    @Test
    fun `three arguments give three ranges, braces excluded`() {
        val chapter = ChapterCommand.of(whole, braced)!!

        assertEquals(TextRange(20, 23), chapter.title)
        assertEquals(TextRange(25, 28), chapter.summary)
        assertEquals(TextRange(30, 33), chapter.text)
        assertEquals(whole, chapter.commandRange)
    }

    @Test
    fun `too few arguments are not a chapter`() {
        assertNull(ChapterCommand.of(whole, braced.take(2)))
        assertNull(ChapterCommand.of(whole, emptyList()))
    }

    @Test
    fun `too many arguments are not a chapter`() {
        assertNull(ChapterCommand.of(whole, braced + TextRange(34, 39)))
    }

    @Test
    fun `an empty argument is allowed and gives an empty range`() {
        val empty = listOf(TextRange(19, 24), TextRange(24, 26), TextRange(26, 31))
        val chapter = ChapterCommand.of(whole, empty)!!

        assertEquals(TextRange(25, 25), chapter.summary)
        assertTrue(chapter.summary.isEmpty)
    }

    @Test
    fun `an argument too short to hold its own braces is refused`() {
        val broken = listOf(TextRange(19, 24), TextRange(24, 25), TextRange(25, 30))
        assertNull(ChapterCommand.of(whole, broken))
    }

    @Test
    fun `the command keeps its backslash`() {
        assertEquals("\\chapterwithsummary", ChapterCommand.COMMAND_NAME)
    }
}
