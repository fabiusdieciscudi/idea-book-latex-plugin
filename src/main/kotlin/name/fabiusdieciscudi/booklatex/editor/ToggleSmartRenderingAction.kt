/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.editor

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware
import name.fabiusdieciscudi.booklatex.BookLatexBundle
import name.fabiusdieciscudi.booklatex.settings.BookLatexRenderingSettings

/**
 * The master switch, among the editor's tab actions.
 *
 * The same application-wide flag that used to sit in the status bar, in a place
 * it can be reached without leaving the text.
 */
class ToggleSmartRenderingAction : ToggleAction(
    { BookLatexBundle.message("action.smartRendering.text") },
    { BookLatexBundle.message("action.smartRendering.description") },
    AllIcons.General.LayoutPreviewOnly,
), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun isSelected(event: AnActionEvent): Boolean =
        BookLatexRenderingSettings.getInstance().smartRendering

    override fun setSelected(event: AnActionEvent, state: Boolean) {
        BookLatexRenderingSettings.getInstance().smartRendering = state
    }
}
