/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.attributes

/**
 * Renders the `[...]` group of a rendered command, one key per line.
 *
 * The whole group is regenerated rather than patched value by value: that is
 * the only way a key absent from the source can appear, and a key emptied by
 * the user can disappear. Keys outside the spec are carried over untouched,
 * after the ones it knows.
 */
object AttributeArgument {

    fun render(
        spec: StripSpec,
        values: Map<String, String>,
        preserved: Map<String, String>,
        baseIndent: String,
    ): String {
        val entries = buildList {
            spec.keys.forEach { key ->
                val value = values[key]?.trim().orEmpty()
                if (value.isNotEmpty()) add(key to value)
            }
            preserved.forEach { (key, value) -> if (value.isNotEmpty()) add(key to value) }
        }
        if (entries.isEmpty()) return ""

        val inner = "$baseIndent    "
        return entries.joinToString(
            separator = ",\n",
            prefix = "[\n",
            postfix = "\n$baseIndent]",
        ) { (key, value) -> "$inner$key={$value}" }
    }
}
