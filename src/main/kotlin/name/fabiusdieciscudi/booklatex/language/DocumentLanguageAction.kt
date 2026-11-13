/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.language

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import name.fabiusdieciscudi.booklatex.BookLatexBundle
import javax.swing.JComponent

/**
 * The drop-down before the gear: the language of the document in this editor,
 * and the choice of another.
 *
 * Made by the editor for its own file, rather than reading the file from the
 * data context: the strip of tab actions belongs to one editor, and so does the
 * drop-down.
 */
class DocumentLanguageAction(
    private val project: Project,
    private val file: VirtualFile,
) : ComboBoxAction(), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(event: AnActionEvent) {
        val language = DocumentLanguageMappings.getInstance(project).languageOf(file)
        event.presentation.text = BookLatexBundle.message(language.bundleKey)
        event.presentation.description = BookLatexBundle.message("action.documentLanguage.description")
    }

    override fun createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup =
        DefaultActionGroup(DocumentLanguage.entries.map { ChooseLanguageAction(project, file, it) })
}

/** One language of the drop-down, ticked when it is the document's. */
private class ChooseLanguageAction(
    private val project: Project,
    private val file: VirtualFile,
    private val language: DocumentLanguage,
) : ToggleAction({ BookLatexBundle.message(language.bundleKey) }), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun isSelected(event: AnActionEvent): Boolean =
        DocumentLanguageMappings.getInstance(project).languageOf(file) == language

    override fun setSelected(event: AnActionEvent, state: Boolean) {
        if (state) DocumentLanguageMappings.getInstance(project).setLanguage(file, language)
    }
}
