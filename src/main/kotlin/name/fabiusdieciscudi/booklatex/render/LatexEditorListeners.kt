/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.render

import com.intellij.openapi.editor.CustomFoldRegion
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseListener
import name.fabiusdieciscudi.booklatex.attributes.editAttributes
import name.fabiusdieciscudi.booklatex.comment.editComment

/** Attaches the click handling to every editor that opens. */
class LatexEditorListener : EditorFactoryListener {

    override fun editorCreated(event: EditorFactoryEvent) {
        event.editor.addEditorMouseListener(LatexFoldClickListener())
    }
}

/** A double click on any rendered region opens its editing dialog. */
private class LatexFoldClickListener : EditorMouseListener {

    override fun mouseClicked(event: EditorMouseEvent) {
        if (event.area != EditorMouseEventArea.EDITING_AREA) return
        if (event.mouseEvent.clickCount != 2) return

        val region = event.collapsedFoldRegion as? CustomFoldRegion ?: return
        if (!region.isValid) return

        when (region.renderer) {
            is CommentFoldRenderer -> {
                event.consume()
                editComment(event.editor, region.startOffset, region.endOffset)
            }

            is AttributeStripRenderer -> {
                event.consume()
                editAttributes(event.editor, region.startOffset, region.endOffset)
            }
        }
    }
}
