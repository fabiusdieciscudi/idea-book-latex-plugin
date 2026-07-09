/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.dialogue

/** Spoken words take guillemets, thoughts take curly double quotes. */
enum class DialogueKind(
    val wrapperName: String,
    val openingQuote: String,
    val closingQuote: String,
) {
    SPOKEN("\\spoken", "\u00AB", "\u00BB"),
    THOUGHT("\\thought", "\u201C", "\u201D"),
    ;

    companion object {
        fun of(commandName: String?): DialogueKind? = entries.firstOrNull { it.wrapperName == commandName }
    }
}

/** `\sjm` speaks for Jacques, aloud. */
data class DialogueDefinition(
    val command: String,
    val kind: DialogueKind,
    val speaker: String,
)
