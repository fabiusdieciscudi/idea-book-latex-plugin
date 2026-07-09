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

/**
 * The platform's text editor, plus one tab action.
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

    override fun getTabActions(): ActionGroup = DefaultActionGroup(ToggleSmartRenderingAction())
}
