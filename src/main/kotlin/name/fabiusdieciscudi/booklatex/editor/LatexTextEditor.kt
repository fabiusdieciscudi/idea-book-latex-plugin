/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.editor

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import name.fabiusdieciscudi.booklatex.language.DocumentLanguageAction

/**
 * The platform's text editor, plus six tab actions: the switch for the
 * plugin's colour scheme, the master switch, the two buttons that make its font
 * larger and smaller, the drop-down of the document's language, and last the
 * gear that opens the plugin's settings.
 *
 * Those buttons in the top right of a Markdown editor come from
 * `getTabActions()`, a method of the FileEditor itself. There is no extension
 * point for that strip: to put something there, one has to be the editor.
 *
 * Which is all this class does. Everything else is inherited, untouched.
 */
class LatexTextEditor(
    project: Project,
    file: VirtualFile,
    provider: TextEditorProvider,
) : PsiAwareTextEditorImpl(project, file, provider) {

    override fun getTabActions(): ActionGroup = DefaultActionGroup(
        ToggleColorSchemeAction(),
        ToggleSmartRenderingAction(),
        IncreaseFontSizeAction(),
        DecreaseFontSizeAction(),
        // project and file: TextEditorImpl's own, protected.
        DocumentLanguageAction(project, file),
        OpenSettingsAction(),
    )
}
