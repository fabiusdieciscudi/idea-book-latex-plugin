/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.markers

/** A dash TeX will make of hyphens: [start]..[end] of the text it was found in, drawn as [glyph]. */
data class Dash(val start: Int, val end: Int, val glyph: String)

/**
 * The dashes in [text], as TeX's ligatures make them: `---` an em dash, `--` an
 * en dash, a single hyphen a hyphen.
 *
 * A longer run is read the way TeX reads it, three at a time from the left:
 * `----` is an em dash and a hyphen, `-----` an em dash and an en dash. The
 * single hyphens are not returned: they are drawn as they are.
 */
fun dashes(text: CharSequence): List<Dash> {
    val found = mutableListOf<Dash>()
    var index = 0
    while (index < text.length) {
        if (text[index] != '-') {
            index++
            continue
        }
        var runEnd = index
        while (runEnd < text.length && text[runEnd] == '-') runEnd++

        var cursor = index
        while (runEnd - cursor >= 3) {
            found += Dash(cursor, cursor + 3, EM_DASH)
            cursor += 3
        }
        if (runEnd - cursor == 2) found += Dash(cursor, cursor + 2, EN_DASH)
        index = runEnd
    }
    return found
}

const val EM_DASH = "—"
const val EN_DASH = "–"
