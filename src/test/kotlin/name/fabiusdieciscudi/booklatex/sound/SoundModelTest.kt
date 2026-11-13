/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundModelTest {

    private val ambient = SoundSpecs.AMBIENT
    private val sfx = SoundSpecs.SFX

    private fun roundTrip(spec: SoundSpec, name: String, optional: String?): String =
        SoundArguments.render(spec, SoundArguments.read(spec, name, optional))

    // --- Reading and writing ---

    @Test
    fun `the examples of the preamble come back as they were written`() {
        assertEquals("\\ambient[loudness=-6, fade=3]{pioggia}", roundTrip(ambient, "pioggia", "[loudness=-6, fade=3]"))
        assertEquals("\\ambient[repeat=off]{applausi}", roundTrip(ambient, "applausi", "[repeat=off]"))
        assertEquals("\\ambient{off}", roundTrip(ambient, "off", null))
        assertEquals("\\sfx[overlay, loudness=-3]{tuono}", roundTrip(sfx, "tuono", "[overlay, loudness=-3]"))
        assertEquals("\\sfx[fade=0.5]{tuono}", roundTrip(sfx, "tuono", "[fade=0.5]"))
        assertEquals("\\sfx[skip=1.5, duration=4]{tuono}", roundTrip(sfx, "tuono", "[skip=1.5, duration=4]"))
        assertEquals("\\sfx[overlay, delay=2]{tuono}", roundTrip(sfx, "tuono", "[overlay, delay=2]"))
    }

    @Test
    fun `options are written in the order of the spec`() {
        assertEquals("\\sfx[overlay, loudness=-3, delay=2]{tuono}", roundTrip(sfx, "tuono", "[delay=2, loudness=-3, overlay]"))
    }

    @Test
    fun `an alias is written back by its full name`() {
        assertEquals("\\sfx[loudness=-3]{tuono}", roundTrip(sfx, "tuono", "[l=-3]"))
        assertEquals("-3", SoundArguments.read(sfx, "tuono", "[l=-3]").numbers["loudness"])
    }

    @Test
    fun `braces around a value and spaces anywhere are taken away`() {
        assertEquals("\\ambient[loudness=-6]{pioggia}", roundTrip(ambient, " pioggia ", "[ loudness = {-6} ]"))
    }

    @Test
    fun `a switch at its default is not written`() {
        assertEquals("\\ambient{pioggia}", roundTrip(ambient, "pioggia", "[repeat=on]"))
        assertEquals("\\sfx{tuono}", roundTrip(sfx, "tuono", "[overlay=false]"))
        assertEquals("\\sfx[overlay]{tuono}", roundTrip(sfx, "tuono", "[overlay=true]"))
    }

    @Test
    fun `an empty number is left out`() {
        assertEquals("\\sfx{tuono}", roundTrip(sfx, "tuono", "[loudness=]"))
    }

    @Test
    fun `unknown options are kept as written, after the known ones`() {
        assertEquals("\\sfx[loudness=-3, pan=left, mute]{tuono}", roundTrip(sfx, "tuono", "[pan=left, loudness=-3, mute]"))
        // repeat belongs to \ambient only.
        assertEquals("\\sfx[repeat=off]{tuono}", roundTrip(sfx, "tuono", "[repeat=off]"))
    }

    @Test
    fun `a switch with a value it does not understand is kept as written`() {
        assertEquals("\\ambient[repeat=maybe]{pioggia}", roundTrip(ambient, "pioggia", "[repeat=maybe]"))
    }

    @Test
    fun `commas inside braces do not split options`() {
        assertEquals(listOf(SoundOption("note", "a, b"), SoundOption("overlay", null)), SoundArguments.parse("[note={a, b}, overlay]"))
    }

    // --- Stopping ---

    @Test
    fun `off stops the background, and only the background`() {
        assertTrue(SoundSettings("off").isStop(ambient))
        assertFalse(SoundSettings("off").isStop(sfx))
        assertFalse(SoundSettings("pioggia").isStop(ambient))
    }

    // --- Validation ---

    @Test
    fun `numbers are checked, blank is fine`() {
        val loudness = ambient.parameter("loudness") as NumberParameter
        assertNull(loudness.problem(""))
        assertNull(loudness.problem("-6"))
        assertNull(loudness.problem("+3"))
        assertNull(loudness.problem("-0.5"))
        assertNull(loudness.problem(".5"))
        assertEquals(NumberParameter.Problem.NOT_A_NUMBER, loudness.problem("0,5"))
        assertEquals(NumberParameter.Problem.NOT_A_NUMBER, loudness.problem("forte"))
        assertEquals(NumberParameter.Problem.NOT_A_NUMBER, loudness.problem("1e3"))
        assertEquals(NumberParameter.Problem.OUT_OF_RANGE, loudness.problem("-100"))
        assertEquals(NumberParameter.Problem.OUT_OF_RANGE, loudness.problem("30"))

        val fade = sfx.parameter("fade") as NumberParameter
        assertEquals(NumberParameter.Problem.OUT_OF_RANGE, fade.problem("-1"))
        assertNull(fade.problem("0"))
    }

    // --- Display ---

    @Test
    fun `a strip shows the name, then the options with a value`() {
        val settings = SoundArguments.read(sfx, "tuono", "[delay=2, overlay, l=-3]")
        assertEquals(
            listOf(
                SoundField(null, "tuono"),
                SoundField("OVERLAY", null),
                SoundField("LOUDNESS", "-3 dB"),
                SoundField("DELAY", "2 s"),
            ),
            SoundDisplay.fields(sfx, settings),
        )
    }

    @Test
    fun `a switch off its default reads as such`() {
        assertEquals(
            listOf(SoundField(null, "applausi"), SoundField("NO REPEAT", null)),
            SoundDisplay.fields(ambient, SoundArguments.read(ambient, "applausi", "[repeat=off]")),
        )
    }

    @Test
    fun `off reads as stop`() {
        assertEquals(listOf(SoundField(null, "stop")), SoundDisplay.fields(ambient, SoundSettings("off")))
    }

    // --- The trailing percent ---

    private fun afterSave(spec: SoundSpec, text: String, command: String): String {
        val start = text.indexOf(command)
        val edit = SoundPercent.edit(text, start, start + command.length, spec) ?: return text
        return text.substring(0, edit.start) + edit.replacement + text.substring(edit.end)
    }

    @Test
    fun `an effect at the end of its line gets a percent, wherever it starts`() {
        assertEquals("Il cielo \\sfx{tuono}%\ne poi.", afterSave(sfx, "Il cielo \\sfx{tuono}   \ne poi.", "\\sfx{tuono}"))
        assertEquals("\\sfx{tuono}%", afterSave(sfx, "\\sfx{tuono}", "\\sfx{tuono}"))
    }

    @Test
    fun `an effect inside a sentence is left alone`() {
        val text = "Il cielo \\sfx{tuono} rimbombò."
        assertEquals(text, afterSave(sfx, text, "\\sfx{tuono}"))
    }

    @Test
    fun `an ambient background at the end of its line gets a percent`() {
        assertEquals("    \\ambient{pioggia}%\nEra notte.", afterSave(ambient, "    \\ambient{pioggia} \nEra notte.", "\\ambient{pioggia}"))
    }

    @Test
    fun `text after an ambient background moves to a line of its own`() {
        assertEquals(
            "    \\ambient{pioggia}%\n    Era notte.",
            afterSave(ambient, "    \\ambient{pioggia}  Era notte.", "\\ambient{pioggia}"),
        )
    }

    @Test
    fun `a percent or a comment already there is left alone`() {
        listOf("\\ambient{pioggia}%\n", "\\ambient{pioggia} % fondo\n", "\\sfx{tuono}%").forEach { text ->
            val command = text.substringBefore('}') + "}"
            val spec = if (text.startsWith("\\ambient")) ambient else sfx
            assertEquals(text, afterSave(spec, text, command))
        }
    }
}
