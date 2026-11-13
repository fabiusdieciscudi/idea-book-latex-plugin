/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.editor

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.ProjectManager
import name.fabiusdieciscudi.booklatex.BookLatexBundle
import name.fabiusdieciscudi.booklatex.settings.BookLatexConfigurable
import name.fabiusdieciscudi.booklatex.settings.BookLatexRenderingSettings
import name.fabiusdieciscudi.booklatex.ui.LatexEditorColorScheme
import javax.swing.Icon

/**
 * The two buttons after the gear: the plugin's own editor font, a point larger
 * or a point smaller.
 *
 * The same setting as *Editor font size* under *Settings | Tools | Book LaTeX*,
 * so the change lasts, applies to every LaTeX editor at once, and shows in the
 * settings page next time it is opened. Not Ctrl+wheel, which zooms one editor
 * and forgets it.
 *
 * That size belongs to the plugin's colour scheme: with the scheme turned off the
 * editors wear the IDE's, and the buttons are disabled, since they would change
 * nothing that can be seen.
 */
abstract class FontSizeAction(
    private val step: Int,
    textKey: String,
    descriptionKey: String,
    icon: Icon,
) : AnAction({ BookLatexBundle.message(textKey) }, { BookLatexBundle.message(descriptionKey) }, icon), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(event: AnActionEvent) {
        val settings = BookLatexRenderingSettings.getInstance()
        event.presentation.isEnabled = settings.latexColorScheme && resized(settings.editorFontSize) != settings.editorFontSize
    }

    override fun actionPerformed(event: AnActionEvent) {
        val settings = BookLatexRenderingSettings.getInstance()
        val size = resized(settings.editorFontSize)
        if (size == settings.editorFontSize) return
        settings.editorFontSize = size

        LatexEditorColorScheme.applyToOpenEditors()
        // The blocks are painted, and were measured with the old size: only a
        // fresh pass sizes them again. As the settings page does on Apply.
        ProjectManager.getInstance().openProjects
            .filterNot { it.isDisposed }
            .forEach { DaemonCodeAnalyzer.getInstance(it).restart() }
    }

    private fun resized(size: Int): Int =
        (size + step).coerceIn(BookLatexConfigurable.MIN_FONT_SIZE, BookLatexConfigurable.MAX_FONT_SIZE)
}

class IncreaseFontSizeAction : FontSizeAction(
    +1,
    "action.increaseFontSize.text",
    "action.increaseFontSize.description",
    AllIcons.General.ZoomIn,
)

class DecreaseFontSizeAction : FontSizeAction(
    -1,
    "action.decreaseFontSize.text",
    "action.decreaseFontSize.description",
    AllIcons.General.ZoomOut,
)
