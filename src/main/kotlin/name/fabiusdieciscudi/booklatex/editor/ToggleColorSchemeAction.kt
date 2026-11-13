/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.editor

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.ProjectManager
import name.fabiusdieciscudi.booklatex.BookLatexBundle
import name.fabiusdieciscudi.booklatex.settings.BookLatexRenderingSettings
import name.fabiusdieciscudi.booklatex.ui.LatexEditorColorScheme

/**
 * The page, on and off: the plugin's own colour scheme -- the white paper, the
 * editor font and its size -- or the IDE's, in every LaTeX editor at once.
 *
 * The same setting as *Use a dedicated colour scheme in LaTeX editors* under
 * *Settings | Tools | Book LaTeX*, to the left of the master switch so that the source
 * can be looked at as source without opening the settings. The renderings are
 * the master switch's business and stay as they are; they are drawn again,
 * since they take their size from the editor's font.
 */
class ToggleColorSchemeAction : ToggleAction(
    { BookLatexBundle.message("action.colorScheme.text") },
    { BookLatexBundle.message("action.colorScheme.description") },
    AllIcons.General.ReaderMode,
), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun isSelected(event: AnActionEvent): Boolean =
        BookLatexRenderingSettings.getInstance().latexColorScheme

    override fun setSelected(event: AnActionEvent, state: Boolean) {
        val settings = BookLatexRenderingSettings.getInstance()
        if (settings.latexColorScheme == state) return
        settings.latexColorScheme = state

        LatexEditorColorScheme.applyToOpenEditors()
        // As on Apply in the settings page: the blocks were measured with the
        // other scheme's font, and only a fresh pass sizes them again.
        ProjectManager.getInstance().openProjects
            .filterNot { it.isDisposed }
            .forEach { DaemonCodeAnalyzer.getInstance(it).restart() }
    }
}
