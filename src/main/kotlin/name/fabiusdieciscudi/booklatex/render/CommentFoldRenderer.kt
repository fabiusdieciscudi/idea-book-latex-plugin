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
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D

/**
 * Paints a `\comment{...}` block as a rounded yellow box in a smaller font.
 *
 * A custom fold region is the only way to get a smaller font: TextAttributes
 * carries no font size, so an annotator could give us the yellow background
 * but never the smaller text.
 */
class CommentFoldRenderer(private val text: String) : CustomFoldRegionRenderer {

    override fun calcWidthInPixels(region: CustomFoldRegion): Int {
        val metrics = region.editor.contentComponent.getFontMetrics(fontOf(region.editor))
        return metrics.stringWidth(text) + 2 * HORIZONTAL_PADDING
    }

    override fun calcHeightInPixels(region: CustomFoldRegion): Int {
        val metrics = region.editor.contentComponent.getFontMetrics(fontOf(region.editor))
        return metrics.height + 2 * VERTICAL_PADDING
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

        g.font = fontOf(region.editor)
        g.color = FOREGROUND
        val baseline = targetRegion.y + VERTICAL_PADDING + g.fontMetrics.ascent
        g.drawString(text, (targetRegion.x + HORIZONTAL_PADDING).toFloat(), baseline.toFloat())
    }

    private fun fontOf(editor: Editor): Font = RenderFonts.font(editor, FONT_SCALE)

    private companion object {
        const val HORIZONTAL_PADDING = 8
        const val VERTICAL_PADDING = 4
        const val ARC = 8.0
        const val FONT_SCALE = 0.8f

        // Not a JBColor: a sticky note is the same colour under every theme.
        // On Darcula this is loud on purpose.
        val BACKGROUND: Color = Color(0xFFF59D)
        val FOREGROUND: Color = Color.BLACK
    }
}
