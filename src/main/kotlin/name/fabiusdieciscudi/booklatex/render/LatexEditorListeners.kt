/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.render

import com.intellij.openapi.editor.CustomFoldRegion
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.VisibleAreaEvent
import com.intellij.openapi.editor.event.VisibleAreaListener
import name.fabiusdieciscudi.booklatex.attributes.editAttributes
import name.fabiusdieciscudi.booklatex.comment.editComment

/** Attaches the click handling, and the re-measuring on resize, to every editor that opens. */
class LatexEditorListener : EditorFactoryListener {

    override fun editorCreated(event: EditorFactoryEvent) {
        event.editor.addEditorMouseListener(LatexFoldClickListener())
        event.editor.scrollingModel.addVisibleAreaListener(RenderedWidthListener(event.editor))
    }
}

/**
 * Measures the renderings again when the editor changes width.
 *
 * A custom fold region asks its renderer for a size once, when it is made, and
 * keeps it. The comment notes wrap to the width of the editor and the strips and
 * rules span it, so a size taken from a wider or narrower window is wrong: a
 * note would keep the line count, and the height, of the old width. Scrolling
 * changes the visible area too, but not its width, and is ignored.
 *
 * The first change has no old rectangle: the regions may have been measured
 * before the editor was laid out, at no width at all, so that one counts.
 */
private class RenderedWidthListener(private val editor: Editor) : VisibleAreaListener {

    override fun visibleAreaChanged(event: VisibleAreaEvent) {
        val old = event.oldRectangle
        if (old != null && old.width == event.newRectangle.width) return

        val foldingModel = editor.foldingModel
        val regions = foldingModel.allFoldRegions
            .filterIsInstance<CustomFoldRegion>()
            .filter { it.isWidthDependent() }
        if (regions.isEmpty()) return

        foldingModel.runBatchFoldingOperation {
            regions.filter { it.isValid }.forEach { it.update() }
        }
    }

    private fun CustomFoldRegion.isWidthDependent(): Boolean =
        renderer is CommentFoldRenderer || renderer is AttributeStripRenderer || renderer is HorizontalRuleRenderer
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
