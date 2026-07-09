/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.dialogue

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project

/**
 * The project's dialogue commands, scanned once and kept.
 *
 * Not a CachedValue on the PSI: that would rescan on every keystroke. The
 * preamble changes far less often than the prose, so the map is built on first
 * use and rebuilt only when asked.
 *
 * Nothing is remembered while the index is being built, or the emptiness of
 * that moment would outlive it.
 */
@Service(Service.Level.PROJECT)
class DialogueDefinitions(private val project: Project) {

    @Volatile
    private var cached: Map<String, DialogueDefinition>? = null

    /** Empty while indexing. Must be called inside a read action. */
    fun definitions(): Map<String, DialogueDefinition> {
        cached?.let { return it }
        if (DumbService.isDumb(project)) return emptyMap()
        return DialogueDefinitionScanner.scan(project).also { cached = it }
    }

    /** Must be called inside a read action. */
    fun reload(): Map<String, DialogueDefinition> {
        if (DumbService.isDumb(project)) return emptyMap()
        return DialogueDefinitionScanner.scan(project).also { cached = it }
    }

    companion object {
        fun getInstance(project: Project): DialogueDefinitions = project.service()
    }
}
