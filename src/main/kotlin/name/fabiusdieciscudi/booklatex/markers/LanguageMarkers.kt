/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.markers

/**
 * The foreign languages the book marks up, and the code shown in their place.
 *
 * Every language contributes two commands, and they differ only in typography:
 * `\french{...}` is foreign prose and leans, `\frenchname{...}` is a name and
 * stands upright. Both show the same code, because both say the same thing
 * about the language.
 *
 * The codes are ISO 639-1 where one exists. Two do not:
 *  - Ligurian has none, so its ISO 639-3 code is used.
 *  - Provençal has none either, and is classified under Occitan.
 */
data class LanguageSpec(val name: String, val isoCode: String) {

    /** `\french`: foreign prose, set in italics. */
    val proseCommand: String get() = "\\$name"

    /** `\frenchname`: a name, left upright. */
    val nameCommand: String get() = "\\${name}name"
}

object Languages {

    val ALL = listOf(
        LanguageSpec("catalan", "CA"),
        LanguageSpec("dutch", "NL"),
        LanguageSpec("english", "EN"),
        LanguageSpec("french", "FR"),
        LanguageSpec("german", "DE"),
        LanguageSpec("irish", "GA"),
        LanguageSpec("japanese", "JA"),
        LanguageSpec("latin", "LA"),
        LanguageSpec("ligurian", "LIJ"),
        LanguageSpec("provencal", "OC"),
        LanguageSpec("spanish", "ES"),
        LanguageSpec("swedish", "SV"),
        LanguageSpec("turkish", "TR"),
    )

    /** The commands whose contents are foreign prose, and therefore italic. */
    val PROSE_COMMANDS: Set<String> = ALL.map { it.proseCommand }.toSet()
}
