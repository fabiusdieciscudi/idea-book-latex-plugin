/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.ui

import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener

/** Applies the LaTeX colour scheme to every LaTeX editor as it opens. */
class LatexColorSchemeListener : EditorFactoryListener {

    override fun editorCreated(event: EditorFactoryEvent) {
        LatexEditorColorScheme.applyTo(event.editor)
    }
}
