/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.attributes

import java.awt.Color

/**
 * Everything that distinguishes one rendered command from another: which keys
 * it carries and how its strip looks. Adding a fourth command means adding a
 * constant here and nothing else.
 *
 * Colours are plain AWT rather than JBColor: these strips are meant to read the
 * same under every theme, like the sticky note.
 */
data class StripSpec(
    val commandName: String,
    val keys: List<String>,
    val background: Color,
    val labelColor: Color,
    val valueColor: Color,
    val emptyValueColor: Color,
    /**
     * Keys that fall back to the last command of this kind that defined them.
     * An inherited value is painted, never written: see [InheritedAttributes].
     */
    val inheritableKeys: List<String> = emptyList(),
    /** Between "written here" and "absent". Defaults to the latter. */
    val inheritedValueColor: Color = emptyValueColor,
)

object StripSpecs {

    val SCENE = StripSpec(
        commandName = "\\scene",
        keys = listOf("name", "setting", "date", "time"),
        background = Color(0x1F4E79),
        labelColor = Color(0x9EC5E8),
        valueColor = Color.WHITE,
        emptyValueColor = Color(0x7FA3C4),
        // A scene without a date happens in the last one that had one.
        inheritableKeys = listOf("date", "time"),
        inheritedValueColor = Color(0xA8C3DA),
    )

    val BEAT = StripSpec(
        commandName = "\\beat",
        keys = listOf("name", "driver", "pov"),
        background = Color(0x9FC5E8),
        labelColor = Color.BLACK,
        valueColor = Color.BLACK,
        // Black on light blue would not read as "absent".
        emptyValueColor = Color(0x5B6B7A),
    )

    val SHOT = StripSpec(
        commandName = "\\shot",
        keys = listOf("focus", "pov", "sense", "framing"),
        background = Color(0xCFE2F3),
        labelColor = Color.BLACK,
        valueColor = Color.BLACK,
        emptyValueColor = Color(0x6E7F8E),
    )

    val ALL = listOf(SCENE, BEAT, SHOT)

    fun forCommand(commandName: String?): StripSpec? =
        ALL.firstOrNull { it.commandName == commandName }
}
