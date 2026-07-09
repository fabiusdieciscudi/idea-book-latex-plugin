/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.dialogue

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import name.fabiusdieciscudi.booklatex.latex.LatexTextFlattener
import nl.hannahsten.texifyidea.index.NewDefinitionIndex
import nl.hannahsten.texifyidea.psi.LatexCommands

/**
 * Reads the dialogue commands the book defines for itself:
 *
 *     \newcommand{\sjm}[2][]{\spoken{\Jacques}{#2}{#1}}
 *     \newcommand{\Jacques}{\frenchname{Jacques}\xspace}
 *
 * and resolves the first into `\sjm` speaks for `Jacques`.
 *
 * The definitions come from TeXiFy's stub index, whose keys are the names of
 * the commands being defined. That index reaches wherever the preamble happens
 * to live -- the chapters are pulled in by a master file, and the preamble by
 * that -- and it costs one query instead of parsing every source file.
 *
 * The index cannot be read while it is being built. Callers must not remember
 * the empty map this returns in dumb mode.
 */
object DialogueDefinitionScanner {

    private val DEFINING_COMMANDS = setOf(
        "\\newcommand",
        "\\renewcommand",
        "\\providecommand",
        "\\DeclareRobustCommand",
    )

    /** Empty while the index is being built. Must be called inside a read action. */
    fun scan(project: Project): Map<String, DialogueDefinition> {
        if (DumbService.isDumb(project)) return emptyMap()

        val log = thisLogger()
        val started = System.currentTimeMillis()

        // \sjm -> (spoken, "\Jacques"), still unresolved.
        val raw = LinkedHashMap<String, Pair<DialogueKind, String>>()
        // \Jacques -> "\frenchname{Jacques}\xspace"
        val names = LinkedHashMap<String, String>()

        // The keys are the names of the defined commands: \sjm, \Jacques, ...
        NewDefinitionIndex.getAllKeys(project).forEach { definedName ->
            NewDefinitionIndex.getByName(definedName, project)
                .filter { it.name in DEFINING_COMMANDS }
                .forEach { command -> command.collectInto(raw, names, log) }
        }

        val definitions = raw.mapValues { (command, entry) ->
            DialogueDefinition(command, entry.first, LatexTextFlattener.resolve(entry.second, names))
        }

        log.info(
            "BookLaTeX: ${definitions.size} dialogue definitions, ${names.size} name macros, " +
                "in ${System.currentTimeMillis() - started} ms"
        )
        return definitions
    }

    private fun LatexCommands.collectInto(
        raw: MutableMap<String, Pair<DialogueKind, String>>,
        names: MutableMap<String, String>,
        log: com.intellij.openapi.diagnostic.Logger,
    ) {
        val required = parameterList.filter { it.requiredParam != null }
        if (required.size < 2) return

        val defined = requiredParameterText(0)?.trim().orEmpty()
        // `\email@format@audio` and friends are package internals, not the author's.
        if (!defined.startsWith("\\") || defined.contains('@')) return

        val body: PsiElement = required.last()
        val wrapper = PsiTreeUtil.findChildrenOfType(body, LatexCommands::class.java)
            .firstOrNull { DialogueKind.of(it.name) != null }

        if (wrapper == null) {
            // A plain name macro. A name takes no arguments, which keeps
            // \comment, \scene and the rest of the preamble out of the table.
            if (parameterList.any { it.optionalParam != null } || required.size != 2) return
            requiredParameterText(1)
                ?.let { LatexTextFlattener.normalizeSource(it) }
                ?.takeIf { it.isNotEmpty() }
                ?.let { names.putIfAbsent(defined, it) }
            return
        }

        val kind = DialogueKind.of(wrapper.name) ?: return
        val speaker = wrapper.requiredParameterText(0)
            ?.let { LatexTextFlattener.normalizeSource(it) }
            .orEmpty()
        // `\spoken{#5}` inside a macro that passes its own argument through.
        if (speaker.isEmpty() || speaker.startsWith("#")) return

        val previous = raw.put(defined, kind to speaker)
        if (previous != null && previous != (kind to speaker)) {
            log.warn("BookLaTeX: $defined defined twice, as $previous and as ${kind to speaker}")
        }
    }
}
