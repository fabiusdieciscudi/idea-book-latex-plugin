/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.render

import com.intellij.openapi.editor.CustomFoldRegion
import com.intellij.openapi.editor.CustomFoldRegionRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.TextAttributes
import name.fabiusdieciscudi.booklatex.attributes.StripSpec
import name.fabiusdieciscudi.booklatex.ui.RenderFonts
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D

/** A full-width strip showing the attributes of a command side by side. */
class AttributeStripRenderer(
    private val spec: StripSpec,
    private val values: Map<String, String>,
    /** Painted a shade dimmer: shown here, written somewhere earlier. */
    private val inherited: Set<String> = emptySet(),
) : CustomFoldRegionRenderer {

    override fun calcWidthInPixels(region: CustomFoldRegion): Int =
        (region.editor.scrollingModel.visibleArea.width - 2 * HORIZONTAL_PADDING).coerceAtLeast(MIN_WIDTH)

    override fun calcHeightInPixels(region: CustomFoldRegion): Int =
        region.editor.contentComponent.getFontMetrics(valueFont(region.editor)).height + 2 * VERTICAL_PADDING

    override fun paint(
        region: CustomFoldRegion,
        g: Graphics2D,
        targetRegion: Rectangle2D,
        textAttributes: TextAttributes,
    ) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val width = region.widthInPixels.toDouble()
        val height = region.heightInPixels.toDouble()
        g.color = spec.background
        g.fill(RoundRectangle2D.Double(targetRegion.x, targetRegion.y, width, height, ARC, ARC))

        val labelMetrics = g.getFontMetrics(labelFont(region.editor))
        val valueMetrics = g.getFontMetrics(valueFont(region.editor))
        val baseline = targetRegion.y + VERTICAL_PADDING + valueMetrics.ascent

        // Plain horizontal flow: each label and value takes the width it needs,
        // one after the other. Nothing is padded out to a fixed share of the strip.
        val limit = targetRegion.x + width - HORIZONTAL_PADDING
        var x = targetRegion.x + HORIZONTAL_PADDING

        for (key in spec.keys) {
            if (x >= limit) break

            val label = key.uppercase()
            g.font = labelFont(region.editor)
            g.color = spec.labelColor
            g.drawString(label, x.toFloat(), baseline.toFloat())
            x += labelMetrics.stringWidth(label) + LABEL_GAP

            val value = values[key].orEmpty()
            val shown = fit(value.ifEmpty { EMPTY_PLACEHOLDER }, valueMetrics, (limit - x).toInt())
            g.font = valueFont(region.editor)
            g.color = when {
                value.isEmpty() -> spec.emptyValueColor
                key in inherited -> spec.inheritedValueColor
                else -> spec.valueColor
            }
            g.drawString(shown, x.toFloat(), baseline.toFloat())
            x += valueMetrics.stringWidth(shown) + FIELD_GAP
        }
    }

    private fun fit(text: String, metrics: FontMetrics, maxWidth: Int): String {
        if (maxWidth <= 0) return ""
        if (metrics.stringWidth(text) <= maxWidth) return text
        var candidate = text
        while (candidate.isNotEmpty() && metrics.stringWidth("$candidate…") > maxWidth) {
            candidate = candidate.dropLast(1)
        }
        return "$candidate…"
    }

    private fun labelFont(editor: Editor): Font = RenderFonts.font(editor, LABEL_SCALE, bold = true)

    private fun valueFont(editor: Editor): Font = RenderFonts.font(editor, VALUE_SCALE)

    private companion object {
        const val HORIZONTAL_PADDING = 8
        const val VERTICAL_PADDING = 5
        const val ARC = 6.0
        const val MIN_WIDTH = 240
        const val LABEL_GAP = 6
        const val FIELD_GAP = 20
        const val LABEL_SCALE = 0.72f
        const val VALUE_SCALE = 0.85f
        const val EMPTY_PLACEHOLDER = "—"
    }
}
