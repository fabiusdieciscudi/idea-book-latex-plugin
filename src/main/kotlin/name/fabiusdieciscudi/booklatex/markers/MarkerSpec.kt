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

/**
 * A list environment drawn by name rather than by its `\begin` and `\end`.
 *
 * `\begin{itemize}` reads as a raised `ITEMIZE`, `\end{itemize}` as a raised
 * `END ITEMIZE`, and every `\item` the environment holds as a bullet. The whole
 * `\begin{...}` and `\end{...}` fold away behind the raised marker, the way a
 * language command folds behind its code — the marker already names the
 * environment, so keeping the `{itemize}` beside it would only say it twice.
 *
 * The bullet stands on the baseline, where the ellipsis stands, rather than
 * being raised: a bullet is punctuation the list is read with, not a label on
 * it. An `\item` is only ever a bullet inside a list this knows; anywhere else
 * it is left as source, because the bullet the platform draws is the list's, and
 * a lone `\item` has no list to belong to.
 *
 * Only `itemize` for now. A second list environment is one row of [ListEnvironments].
 */
data class ListEnvironmentSpec(
    /** The environment name, as it appears between the braces of `\begin{...}`. */
    val environmentName: String,
    /** Raised in place of the whole `\begin{...}`. */
    val beginMarker: String,
    /** Raised in place of the whole `\end{...}`. */
    val endMarker: String,
    /** Drawn on the baseline in place of each `\item` the environment holds. */
    val itemMarker: String,
)

object MarkerSpecs {

    // LatexCommands.getName() keeps the leading backslash.
    val ELLIPSIS = MarkerSpec("\\ellipsis", "\u2026", requiresArgument = false)

    /**
     * An aside to the author, kept in the manuscript and marked as not being
     * part of it. The marker is a word rather than a code because there is only
     * one of these, and `NT` would have to be learnt.
     */
    val NOTE = MarkerSpec("\\note", "Note", requiresArgument = true)

    /** Both commands of every language, showing the same code. */
    private val LANGUAGES: List<MarkerSpec> = Languages.ALL.flatMap { language ->
        listOf(
            MarkerSpec(language.proseCommand, language.isoCode, requiresArgument = true),
            MarkerSpec(language.nameCommand, language.isoCode, requiresArgument = true),
        )
    }

    val ALL: List<MarkerSpec> = LANGUAGES + ELLIPSIS + NOTE

    private val BY_NAME: Map<String, MarkerSpec> = ALL.associateBy { it.commandName }

    fun forCommand(commandName: String?): MarkerSpec? = BY_NAME[commandName]
}

object WrapSpecs {

    val SINGLE_QUOTE = WrapSpec("\\sq", "\u201C", "\u201D")

    val ALL = listOf(SINGLE_QUOTE)

    fun forCommand(commandName: String?): WrapSpec? =
        ALL.firstOrNull { it.commandName == commandName }
}

object ListEnvironments {

    val ITEMIZE = ListEnvironmentSpec(
        environmentName = "itemize",
        beginMarker = "ITEMIZE",
        endMarker = "END ITEMIZE",
        itemMarker = "\u2022",
    )

    val ALL: List<ListEnvironmentSpec> = listOf(ITEMIZE)

    /**
     * The name of the command that carries the items, kept with its backslash to
     * match what LatexCommands.getName() returns.
     */
    const val ITEM_COMMAND = "\\item"

    /** The spec for an environment of this name, or null if none is drawn this way. */
    fun forEnvironment(environmentName: String?): ListEnvironmentSpec? =
        ALL.firstOrNull { it.environmentName == environmentName }
}
