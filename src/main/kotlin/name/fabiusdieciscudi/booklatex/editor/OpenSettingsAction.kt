/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.editor

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import name.fabiusdieciscudi.booklatex.BookLatexBundle
import name.fabiusdieciscudi.booklatex.settings.BookLatexConfigurable

/**
 * The gear beside the master switch: *Settings | Tools | Book LaTeX*, opened on
 * the plugin's own page rather than wherever the settings were left.
 */
class OpenSettingsAction : AnAction(
    { BookLatexBundle.message("action.openSettings.text") },
    { BookLatexBundle.message("action.openSettings.description") },
    AllIcons.General.Settings,
), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(event: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(event.project, BookLatexConfigurable::class.java)
    }
}
