/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.attributes

import com.intellij.openapi.util.TextRange
import nl.hannahsten.texifyidea.psi.LatexCommands

/**
 * Reading and rewriting the key=value optional argument of a rendered command.
 *
 * On save the whole bracket group is regenerated rather than patched value by
 * value: that is the only way a key absent from the source can appear, and a
 * key emptied by the user can disappear. Keys outside the spec are carried
 * over untouched.
 */
object CommandAttributes {

    /** Every key of the spec, missing ones mapped to the empty string. */
    fun read(command: LatexCommands, spec: StripSpec): Map<String, String> {
        val source = rawMap(command)
        return spec.keys.associateWith { source[it].orEmpty() }
    }

    /** Keys present in the source that the spec does not offer for editing. */
    fun preservedKeys(command: LatexCommands, spec: StripSpec): Map<String, String> =
        rawMap(command).filterKeys { it !in spec.keys }

    private fun rawMap(command: LatexCommands): Map<String, String> =
        command.optionalParameterTextMap()
            .mapValues { (_, value) -> value.trim().removeSurrounding("{", "}").trim() }

    /**
     * The text range of the `[...]` group, or null when the command carries no
     * optional argument at all and one has to be inserted.
     */
    fun optionalArgumentRange(command: LatexCommands): TextRange? =
        command.parameterList.firstOrNull { it.optionalParam != null }?.textRange

    /** Where an absent optional argument goes: right after the command token. */
    fun insertionOffset(command: LatexCommands): Int = command.commandToken.textRange.endOffset
}
