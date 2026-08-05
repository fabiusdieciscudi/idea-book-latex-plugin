/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.document

/**
 * The unicode characters a manuscript picks up where LaTeX wants a plain `'`.
 *
 * A phone keyboard, a word processor's autocorrect, a paste from somewhere else:
 * all of them turn a typewriter apostrophe into a prettier one, and LaTeX, which
 * reads `'` as the right single quote, does not know what to do with it. On save
 * they are put back.
 *
 * Only the characters that are unambiguously an apostrophe are here, because
 * each is replaced by `'`, the right single quote, and only a right single quote
 * should become one:
 *  - the left single quote `‘` is an *opening* quote, whose LaTeX is a backtick,
 *    not `'`, so mapping it here would be wrong even when it stands in for a
 *    leading apostrophe;
 *  - the grave accent `` ` `` is already the opening quote LaTeX wants, and is
 *    left alone;
 *  - the acute accent `´` is too often meant as an accent to touch on sight.
 *
 * A new one is a row of [ALTERNATIVES].
 */
object Apostrophes {

    /** The right single quote LaTeX reads as an apostrophe: U+0027. */
    const val PLAIN = '\''

    val ALTERNATIVES: Set<Char> = setOf(
        '\u2019', // ’ RIGHT SINGLE QUOTATION MARK — the "smart" apostrophe
        '\u02BC', // ʼ MODIFIER LETTER APOSTROPHE
        '\uFF07', // ＇ FULLWIDTH APOSTROPHE
    )

    fun isAlternative(character: Char): Boolean = character in ALTERNATIVES

    /**
     * The offsets of [text] that hold an alternative apostrophe, ascending.
     *
     * Offsets rather than a rewritten string: the replacement is one character
     * for one, so nothing shifts, and the caller can edit the document in place
     * and leave every other character -- and the caret sitting among them --
     * where it was.
     */
    fun offsetsIn(text: CharSequence): List<Int> =
        text.indices.filter { text[it] in ALTERNATIVES }
}
