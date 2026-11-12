/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.render

import com.intellij.openapi.editor.CustomFoldRegion
import com.intellij.openapi.editor.CustomFoldRegionRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.TextAttributes
import name.fabiusdieciscudi.booklatex.ui.RenderFonts
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import kotlin.math.roundToInt

/**
 * Paints a `\comment{...}` block as a rounded yellow box in a smaller font.
 *
 * A custom fold region is the only way to get a smaller font: TextAttributes
 * carries no font size, so an annotator could give us the yellow background
 * but never the smaller text.
 *
 * The text wraps. A short comment gets a box as wide as itself, like a sticky
 * note; a long one gets as many lines as it needs within the width of the
 * editor, and a box as tall as those lines -- nothing is cut off. When the
 * editor is resized the box is measured again (see [RenderedWidthListener]).
 *
 * The type is two thirds of what it once was, and so are the margins above and
 * below it and the rounding of the corners, as for [AttributeStripRenderer].
 */
class CommentFoldRenderer(private val text: String) : CustomFoldRegionRenderer {

    override fun calcWidthInPixels(region: CustomFoldRegion): Int {
        val metrics = metricsOf(region.editor)
        val widest = linesOf(metrics, maxTextWidth(region.editor)).maxOfOrNull { metrics.stringWidth(it) } ?: 0
        return widest + 2 * HORIZONTAL_PADDING
    }

    override fun calcHeightInPixels(region: CustomFoldRegion): Int {
        val metrics = metricsOf(region.editor)
        val lines = linesOf(metrics, maxTextWidth(region.editor)).size.coerceAtLeast(1)
        return (lines * metrics.height + 2 * VERTICAL_PADDING).roundToInt()
    }

    override fun paint(
        region: CustomFoldRegion,
        g: Graphics2D,
        targetRegion: Rectangle2D,
        textAttributes: TextAttributes,
    ) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g.color = BACKGROUND
        g.fill(
            RoundRectangle2D.Double(
                targetRegion.x,
                targetRegion.y,
                region.widthInPixels.toDouble(),
                region.heightInPixels.toDouble(),
                ARC,
                ARC,
            )
        )

        // Wrapped at the width the box was given, with the metrics it was
        // measured with, so the lines painted are the lines that were counted.
        val metrics = metricsOf(region.editor)
        val lines = linesOf(metrics, region.widthInPixels - 2 * HORIZONTAL_PADDING)

        g.font = fontOf(region.editor)
        g.color = FOREGROUND
        val x = (targetRegion.x + HORIZONTAL_PADDING).toFloat()
        var baseline = targetRegion.y + VERTICAL_PADDING + metrics.ascent
        for (line in lines) {
            g.drawString(line, x, baseline.toFloat())
            baseline += metrics.height
        }
    }

    private fun linesOf(metrics: FontMetrics, maxWidth: Int): List<String> =
        wrapWords(text, maxWidth) { metrics.stringWidth(it) }

    /** The editor's width, less the margins on either side of the box and inside it. */
    private fun maxTextWidth(editor: Editor): Int =
        (editor.scrollingModel.visibleArea.width - 2 * HORIZONTAL_PADDING).coerceAtLeast(MIN_WIDTH) -
            2 * HORIZONTAL_PADDING

    private fun metricsOf(editor: Editor): FontMetrics = editor.contentComponent.getFontMetrics(fontOf(editor))

    private fun fontOf(editor: Editor): Font = RenderFonts.font(editor, FONT_SCALE)

    private companion object {
        /** Two thirds of the size the note was first drawn at: type, vertical margins, corners. */
        const val SIZE = 2f / 3f

        const val HORIZONTAL_PADDING = 8
        const val VERTICAL_PADDING = 4.0 * SIZE
        const val ARC = 8.0 * SIZE
        const val MIN_WIDTH = 240

        /** Of the editor's font size: first 0.8, now two thirds of that. */
        const val FONT_SCALE = 0.8f * SIZE

        // Not a JBColor: a sticky note is the same colour under every theme.
        // On Darcula this is loud on purpose.
        val BACKGROUND: Color = Color(0xFFF59D)
        val FOREGROUND: Color = Color.BLACK
    }
}
