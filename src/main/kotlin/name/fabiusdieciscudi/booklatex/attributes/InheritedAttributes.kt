/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.attributes

import nl.hannahsten.texifyidea.psi.LatexCommands

/**
 * A `\scene` that does not say when it happens happens when the last one did.
 *
 * The value is looked up backwards in the file being edited, among the commands
 * of the same kind, and it is only ever *shown*: painted in the strip, offered
 * as a placeholder in the dialog, never written into the source. An inheritance
 * that materialises as soon as you look at it is not an inheritance.
 *
 * A command that is not rendered -- one sharing its line with prose -- still
 * defines what follows it. Rendering is a way of looking; this is the text.
 */
object InheritedAttributes {

    /**
     * The last value each inheritable key was explicitly given, among the
     * commands of [spec] that start before [offset].
     *
     * [commands] must be ordered by their position in the file.
     */
    fun before(
        commands: List<LatexCommands>,
        spec: StripSpec,
        offset: Int,
    ): Map<String, String> {
        if (spec.inheritableKeys.isEmpty()) return emptyMap()

        val last = mutableMapOf<String, String>()
        for (command in commands) {
            if (command.textRange.startOffset >= offset) break
            if (command.name != spec.commandName) continue

            val values = CommandAttributes.read(command, spec)
            spec.inheritableKeys.forEach { key ->
                values[key]?.takeIf { it.isNotEmpty() }?.let { last[key] = it }
            }
        }
        return last
    }

    /** Fills the empty inheritable keys of [values], and says which it filled. */
    fun applyTo(values: Map<String, String>, fallback: Map<String, String>, spec: StripSpec): Inherited {
        val filled = values.toMutableMap()
        val inherited = mutableSetOf<String>()

        spec.inheritableKeys.forEach { key ->
            if (filled[key].isNullOrEmpty()) {
                fallback[key]?.let {
                    filled[key] = it
                    inherited += key
                }
            }
        }
        return Inherited(filled, inherited)
    }

    data class Inherited(val values: Map<String, String>, val keys: Set<String>)
}
