/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.render

/**
 * Breaks [text] into lines no wider than [maxWidth], as measured by [width].
 *
 * Greedy, word by word: each line takes as many words as fit. A word too wide
 * for a line of its own -- a URL, a long command -- is broken where it
 * overflows, since leaving it whole would push it out of the box.
 *
 * Wrapping the lines it returns again, at the width of the widest of them,
 * gives the same lines back. A renderer can rely on that: it measures at the
 * width the editor allows, and paints at the width it was given.
 *
 * Words are separated by single spaces; [text] is expected to have had its
 * whitespace collapsed already.
 */
fun wrapWords(text: String, maxWidth: Int, width: (String) -> Int): List<String> {
    val lines = mutableListOf<String>()
    var line = ""

    for (word in text.split(' ').filter { it.isNotEmpty() }) {
        val candidate = if (line.isEmpty()) word else "$line $word"
        if (width(candidate) <= maxWidth) {
            line = candidate
            continue
        }

        if (line.isNotEmpty()) lines += line

        var rest = word
        while (rest.length > 1 && width(rest) > maxWidth) {
            var cut = rest.length - 1
            while (cut > 1 && width(rest.substring(0, cut)) > maxWidth) cut--
            lines += rest.substring(0, cut)
            rest = rest.substring(cut)
        }
        line = rest
    }

    if (line.isNotEmpty()) lines += line
    return lines
}
