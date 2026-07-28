/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.attributes

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import name.fabiusdieciscudi.booklatex.latex.LatexTextFlattener
import nl.hannahsten.texifyidea.index.NewDefinitionIndex
import nl.hannahsten.texifyidea.psi.LatexCommands
import java.text.Collator

/**
 * Reads the places the book defines for itself:
 *
 *     \newcommand{\settingSpiaggiaAgay}{Spiaggia di \frenchname{Agay}, \frenchname{Var}}
 *
 * and offers them as `\settingSpiaggiaAgay` shown as "Spiaggia di Agay, Var".
 *
 * The same index and the same flattening as [name.fabiusdieciscudi.booklatex.dialogue.DialogueDefinitionScanner],
 * for the same reasons: one query instead of parsing every source file, and a
 * label with the markup taken out of it.
 *
 * Sorted by what is shown rather than by the command, because that is the order
 * the author reads, and with a Collator rather than by code point -- Nîmes
 * belongs next to Nice, not after Zurigo.
 *
 * The index cannot be read while it is being built. Callers must not remember
 * the empty list this returns in dumb mode.
 */
object SettingDefinitionScanner {

    /** Anything longer than this and starting with it is a place. */
    const val PREFIX = "\\setting"

    private val DEFINING_COMMANDS = setOf(
        "\\newcommand",
        "\\renewcommand",
        "\\providecommand",
        "\\DeclareRobustCommand",
    )

    /** Empty while the index is being built. Must be called inside a read action. */
    fun scan(project: Project): List<SettingDefinition> {
        if (DumbService.isDumb(project)) return emptyList()

        val log = thisLogger()
        val started = System.currentTimeMillis()

        // Keyed by command so that a place defined twice is taken once.
        val found = LinkedHashMap<String, String>()

        NewDefinitionIndex.getAllKeys(project).forEach { definedName ->
            NewDefinitionIndex.getByName(definedName, project)
                .filter { it.name in DEFINING_COMMANDS }
                .forEach { it.collectInto(found) }
        }

        val definitions = found
            .map { (command, label) -> SettingDefinition(command, label) }
            .sortedWith(compareBy(Collator.getInstance()) { it.label })

        log.info("BookLaTeX: ${definitions.size} settings in ${System.currentTimeMillis() - started} ms")
        return definitions
    }

    private fun LatexCommands.collectInto(found: MutableMap<String, String>) {
        val required = parameterList.filter { it.requiredParam != null }
        if (required.size < 2) return

        // The name is read from the argument rather than from the index key, so
        // that nothing here depends on the shape the key happens to have.
        val defined = requiredParameterText(0)?.trim().orEmpty()
        if (!defined.startsWith(PREFIX) || defined.length == PREFIX.length) return
        // `\setting@internal` and friends belong to a package, not to the book.
        if (defined.contains('@')) return

        val body = requiredParameterText(1) ?: return
        // No name macros to resolve against: a place is written out, and a
        // \Jacques inside one would be a character, not a location.
        val label = LatexTextFlattener.resolve(body, emptyMap())
        if (label.isEmpty()) return

        found.putIfAbsent(defined, label)
    }
}
