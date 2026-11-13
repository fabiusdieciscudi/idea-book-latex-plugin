/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Paths

class SoundLibraryTest {

    @Test
    fun `a name is what comes before the first colon, trimmed`() {
        assertEquals(listOf("pioggia", "vento forte"), SoundLibrary.parse("pioggia: rain.wav\n  vento forte :wind.wav:extra\n"))
    }

    @Test
    fun `a hash starts a comment to the end of the line`() {
        val text = """
            # Backgrounds
            pioggia: rain.wav   # loop
            #tuono: thunder.wav
            vento: wind.wav # with: a colon in the comment
        """.trimIndent()
        assertEquals(listOf("pioggia", "vento"), SoundLibrary.parse(text))
    }

    @Test
    fun `a colon only in a comment does not make a sound`() {
        assertEquals(emptyList<String>(), SoundLibrary.parse("pioggia # rain: loop"))
    }

    @Test
    fun `lines without a colon or a name are skipped`() {
        assertEquals(listOf("pioggia"), SoundLibrary.parse("\n   \njust words\n: no name\npioggia:\n"))
    }

    @Test
    fun `each name once, in the order of the file`() {
        assertEquals(listOf("vento", "pioggia"), SoundLibrary.parse("vento: a\npioggia: b\nvento: c"))
    }

    @Test
    fun `windows line ends are fine`() {
        assertEquals(listOf("pioggia", "vento"), SoundLibrary.parse("pioggia: a\r\nvento: b\r\n"))
    }

    @Test
    fun `a relative path is taken from the project root, an absolute one as it is`() {
        assertEquals(Paths.get("/book/audio/sounds.txt"), SoundLibrary.resolve("audio/sounds.txt", "/book"))
        assertEquals(Paths.get("/etc/sounds.txt"), SoundLibrary.resolve(" /etc/sounds.txt ", "/book"))
        assertNull(SoundLibrary.resolve("audio/sounds.txt", null))
    }

    @Test
    fun `a tilde is the home directory`() {
        val home = System.getProperty("user.home")
        assertEquals(Paths.get(home, "sounds.txt"), SoundLibrary.resolve("~/sounds.txt", "/book"))
    }

    @Test
    fun `the setting is kept relative to the project root`() {
        assertEquals("audio/sounds.txt", SoundLibrary.relativize("/book/audio/sounds.txt", "/book"))
        assertEquals("audio/sounds.txt", SoundLibrary.relativize("audio/./x/../sounds.txt", "/book"))
        assertEquals("../library/sounds.txt", SoundLibrary.relativize("/library/sounds.txt", "/book"))
        assertEquals("sounds.txt", SoundLibrary.relativize(" sounds.txt ", "/book"))
    }

    @Test
    fun `without a project the setting is kept as it was given`() {
        assertEquals("/library/sounds.txt", SoundLibrary.relativize(" /library/sounds.txt ", null))
        assertEquals("", SoundLibrary.relativize("", "/book"))
    }

    @Test
    fun `no setting, no library`() {
        assertNull(SoundLibrary.resolve("", "/book"))
        assertNull(SoundLibrary.resolve("   ", "/book"))
    }
}
