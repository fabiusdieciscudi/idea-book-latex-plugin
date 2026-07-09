/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jdom.Element

/**
 * Hands LaTeX files an editor of ours, differing from the platform's in exactly
 * one respect: it contributes a tab action.
 *
 * Implements FileEditorProvider directly rather than extending
 * TextEditorProvider. That class is itself a DefaultPlatformFileEditorProvider,
 * so a subclass asking for HIDE_DEFAULT_EDITOR hides itself, and the file opens
 * in no editor at all -- silently, since nothing threw.
 *
 * Editor state is delegated to the platform's provider, or the caret and the
 * scroll position would be forgotten every time a file is reopened.
 */
class LatexTextEditorProvider : FileEditorProvider, DumbAware {

    private val delegate: TextEditorProvider get() = TextEditorProvider.getInstance()

    override fun accept(project: Project, file: VirtualFile): Boolean =
        !file.isDirectory && file.extension?.lowercase() in SOURCE_EXTENSIONS

    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        LatexTextEditor(project, file, delegate)

    override fun getEditorTypeId(): String = EDITOR_TYPE_ID

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

    override fun readState(element: Element, project: Project, file: VirtualFile): FileEditorState =
        delegate.readState(element, project, file)

    override fun writeState(state: FileEditorState, project: Project, element: Element) =
        delegate.writeState(state, project, element)

    private companion object {
        const val EDITOR_TYPE_ID = "BookLaTeX-text-editor"
        val SOURCE_EXTENSIONS = setOf("tex", "sty", "cls")
    }
}
