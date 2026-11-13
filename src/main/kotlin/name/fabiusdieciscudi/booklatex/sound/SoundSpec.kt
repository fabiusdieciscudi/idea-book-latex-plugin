/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.sound

import java.awt.Color

/**
 * One option of a sound command, as the preamble defines it with `\define@key`.
 *
 * The book prints nothing for a sound: the options are read by BookAssistant,
 * when the book is set for audio. So their values are numbers and switches, not
 * prose, and each has a widget that knows what it holds.
 */
sealed interface SoundParameter {
    /** The full name, which is the one written back. */
    val key: String
}

/**
 * A number of [unit]s, between [min] and [max].
 *
 * Written with a dot for decimals whatever the locale, since that is what TeX
 * reads: the field takes text, and checks it, rather than formatting a number.
 */
data class NumberParameter(
    override val key: String,
    val unit: String,
    val min: Double,
    val max: Double,
    /** Other names the preamble accepts for the same key: `l` for `loudness`. Read, never written. */
    val aliases: Set<String> = emptySet(),
) : SoundParameter {

    enum class Problem { NOT_A_NUMBER, OUT_OF_RANGE }

    /** What is wrong with [text] as a value, or `null` if it will do. Blank is fine: the key is left out. */
    fun problem(text: String): Problem? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        if (!NUMBER.matches(trimmed)) return Problem.NOT_A_NUMBER
        val value = trimmed.toDouble()
        return if (value < min || value > max) Problem.OUT_OF_RANGE else null
    }

    private companion object {
        /** Optionally signed, a dot for decimals, no exponent: what a person types. */
        val NUMBER = Regex("""[+-]?(\d+(\.\d*)?|\.\d+)""")
    }
}

/**
 * A switch, [default] when absent.
 *
 * Only a value other than the default is written: switched on, the bare key
 * (`overlay`, which the preamble reads as `true`); switched off, `key=off`
 * (`repeat=off`). Reading accepts `true`/`on` and `false`/`off` alike.
 */
data class SwitchParameter(
    override val key: String,
    val default: Boolean,
) : SoundParameter

/**
 * `\ambient` or `\sfx`: a required sound name, preceded by options in square
 * brackets.
 *
 * The two differ in their options and in where they may stand. An ambient
 * background starts a paragraph of its own in the audio book, so it is always
 * a line of its own here too: anything that follows it on its line is moved to
 * the next one on save. An effect may sit inside a sentence, and is drawn
 * there as a coloured pill rather than as a strip.
 */
data class SoundSpec(
    val commandName: String,
    val parameters: List<SoundParameter>,
    /** The name that is not a sound: `\ambient{off}` stops the background. */
    val stopName: String?,
    /** Whether the command must own the end of its line: see [SoundPercent]. */
    val endsItsLine: Boolean,
    val background: Color,
    val labelColor: Color,
    val valueColor: Color,
) {
    /** `AMBIENT`, `SFX`: what the strip and the pill open with, in bold. */
    val title: String get() = commandName.removePrefix("\\").uppercase()

    /** The parameter called [key] or known by it as an alias. */
    fun parameter(key: String): SoundParameter? =
        parameters.firstOrNull { it.key == key || (it is NumberParameter && key in it.aliases) }
}

object SoundSpecs {

    private val LOUDNESS = NumberParameter("loudness", "dB", -60.0, 20.0, aliases = setOf("l"))
    private val FADE = NumberParameter("fade", "s", 0.0, 60.0)
    private val SKIP = NumberParameter("skip", "s", 0.0, 3600.0)
    private val CUT = NumberParameter("cut", "s", 0.0, 3600.0)
    private val DURATION = NumberParameter("duration", "s", 0.0, 3600.0)
    private val DELAY = NumberParameter("delay", "s", 0.0, 600.0)

    /** In the order the preamble writes them into the tag. */
    val AMBIENT = SoundSpec(
        commandName = "\\ambient",
        parameters = listOf(LOUDNESS, FADE, SwitchParameter("repeat", default = true), SKIP, CUT, DURATION, DELAY),
        stopName = "off",
        endsItsLine = true,
        // A dark teal: a sound, not a part of the story, and not the scenes' blue.
        background = Color(0x1D5C57),
        labelColor = Color(0x9FD6CF),
        valueColor = Color.WHITE,
    )

    val SFX = SoundSpec(
        commandName = "\\sfx",
        parameters = listOf(SwitchParameter("overlay", default = false), LOUDNESS, FADE, SKIP, CUT, DURATION, DELAY),
        stopName = null,
        endsItsLine = false,
        // A light leaf green: the same family as the background, so the two read
        // as sounds, light enough to tell apart from it and to find in a paragraph.
        background = Color(0xBFE3B4),
        labelColor = Color(0x2F5D2A),
        valueColor = Color.BLACK,
    )

    val ALL = listOf(AMBIENT, SFX)

    fun forCommand(commandName: String?): SoundSpec? = ALL.firstOrNull { it.commandName == commandName }
}
