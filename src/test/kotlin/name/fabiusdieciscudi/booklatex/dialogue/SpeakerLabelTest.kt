/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.dialogue

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeakerLabelTest {

    @Test
    fun `without an optional argument the label is the speaker`() {
        assertEquals("Jacques", speakerLabel("Jacques", null))
    }

    @Test
    fun `the optional argument follows the speaker after a comma`() {
        assertEquals("Jacques, sottovoce", speakerLabel("Jacques", "[sottovoce]"))
    }

    @Test
    fun `its whitespace is collapsed`() {
        assertEquals("Jacques, a voce bassa", speakerLabel("Jacques", "[  a voce\n    bassa ]"))
    }

    @Test
    fun `an empty one adds nothing`() {
        assertEquals("Jacques", speakerLabel("Jacques", "[]"))
        assertEquals("Jacques", speakerLabel("Jacques", "[   ]"))
    }
}
