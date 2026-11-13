/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.dialogue

/**
 * What is drawn above a line of dialogue: the speaker, and after a comma what
 * the optional argument says -- `\sjm[sottovoce]{...}` reads `Jacques, sottovoce`.
 *
 * [optional] is the argument as written, square brackets and all, or `null`.
 * Its whitespace is collapsed, since the label is a single line; an empty one
 * adds nothing.
 */
fun speakerLabel(speaker: String, optional: String?): String {
    val extra = optional
        ?.trim()
        ?.removePrefix("[")
        ?.removeSuffix("]")
        ?.split(Regex("\\s+"))
        ?.filter { it.isNotEmpty() }
        ?.joinToString(" ")
        .orEmpty()
    return if (extra.isEmpty()) speaker else "$speaker, $extra"
}
