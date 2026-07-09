/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.markers

/**
 * A command whose token is replaced by a short marker, its braces left in place.
 *
 * `\french{du pain}` reads as `FR{du pain}`: only `\french` is hidden, so the
 * braces and the phrase between them remain the document's own text. `\ellipsis`
 * carries no argument, and its token is the whole command, so it simply becomes
 * an ellipsis character.
 *
 * The language markers are generated from [Languages] rather than listed here:
 * a new language is one row of a table.
 */
data class MarkerSpec(
    val commandName: String,
    val marker: String,
    /** A marker with nothing after it says nothing: skip such a command. */
    val requiresArgument: Boolean,
)

/**
 * A command that wraps its argument in a pair of characters, braces and all.
 *
 * `\sq{testo}` reads as `“testo”`: the command token and its opening brace fold
 * into the opening quote, the closing brace into the closing one. What is left
 * between them is the document's own text, editable and spellchecked.
 */
data class WrapSpec(
    val commandName: String,
    val opening: String,
    val closing: String,
)

object MarkerSpecs {

    // LatexCommands.getName() keeps the leading backslash.
    val ELLIPSIS = MarkerSpec("\\ellipsis", "\u2026", requiresArgument = false)

    /** Both commands of every language, showing the same code. */
    private val LANGUAGES: List<MarkerSpec> = Languages.ALL.flatMap { language ->
        listOf(
            MarkerSpec(language.proseCommand, language.isoCode, requiresArgument = true),
            MarkerSpec(language.nameCommand, language.isoCode, requiresArgument = true),
        )
    }

    val ALL: List<MarkerSpec> = LANGUAGES + ELLIPSIS

    private val BY_NAME: Map<String, MarkerSpec> = ALL.associateBy { it.commandName }

    fun forCommand(commandName: String?): MarkerSpec? = BY_NAME[commandName]
}

object WrapSpecs {

    val SINGLE_QUOTE = WrapSpec("\\sq", "\u201C", "\u201D")

    val ALL = listOf(SINGLE_QUOTE)

    fun forCommand(commandName: String?): WrapSpec? =
        ALL.firstOrNull { it.commandName == commandName }
}
