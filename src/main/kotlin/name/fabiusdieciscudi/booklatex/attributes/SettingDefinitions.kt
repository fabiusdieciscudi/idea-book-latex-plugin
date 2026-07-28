/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.attributes

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project

/**
 * The project's places, scanned once and kept.
 *
 * The same bargain as the dialogue definitions: the preamble changes far less
 * often than the prose, so the list is built on first use and rebuilt only when
 * asked. Nothing is remembered while the index is being built, or the emptiness
 * of that moment would outlive it.
 */
@Service(Service.Level.PROJECT)
class SettingDefinitions(private val project: Project) {

    @Volatile
    private var cached: List<SettingDefinition>? = null

    /** Empty while indexing. Must be called inside a read action. */
    fun definitions(): List<SettingDefinition> {
        cached?.let { return it }
        if (DumbService.isDumb(project)) return emptyList()
        return SettingDefinitionScanner.scan(project).also { cached = it }
    }

    /** Must be called inside a read action. */
    fun reload(): List<SettingDefinition> {
        if (DumbService.isDumb(project)) return emptyList()
        return SettingDefinitionScanner.scan(project).also { cached = it }
    }

    companion object {
        fun getInstance(project: Project): SettingDefinitions = project.service()
    }
}
