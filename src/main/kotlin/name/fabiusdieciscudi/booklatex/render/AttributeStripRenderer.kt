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
import kotlin.math.roundToInt

/**
 * A full-width strip showing the attributes of a command side by side, after the
 * command's own name in bold: `SCENE  NAME Il porto  SETTING Genova ...`.
 *
 * The type is two thirds of what it once was, and so are the margins above and
 * below it and the rounding of the corners: the whole strip shrinks, not just
 * the words in it. A strip is a signpost between stretches of prose, not prose:
 * it has to be legible, not to compete.
 *
 * The platform never makes a custom fold region shorter than a line of the
 * editor, whatever [calcHeightInPixels] returns (`CustomFoldRegionImpl.doUpdate`);
 * going below that takes internal API. So the region keeps its line, and the
 * band is painted only as tall as its text, in the middle of it, with white
 * above and below: it reads as the space between two paragraphs.
 */
class AttributeStripRenderer(
    private val spec: StripSpec,
    private val values: Map<String, String>,
    /** Painted a shade dimmer: shown here, written somewhere earlier. */
    private val inherited: Set<String> = emptySet(),
) : CustomFoldRegionRenderer {

    override fun calcWidthInPixels(region: CustomFoldRegion): Int =
        (region.editor.scrollingModel.visibleArea.width - 2 * HORIZONTAL_PADDING).coerceAtLeast(MIN_WIDTH)

    override fun calcHeightInPixels(region: CustomFoldRegion): Int = bandHeight(region.editor).roundToInt()

    /** The height of the coloured band: the text and its margins, however tall the region is made. */
    private fun bandHeight(editor: Editor): Double {
        val component = editor.contentComponent
        val tallest = maxOf(
            component.getFontMetrics(titleFont(editor)).height,
            component.getFontMetrics(valueFont(editor)).height,
        )
        return tallest + 2 * VERTICAL_PADDING
    }

    override fun paint(
        region: CustomFoldRegion,
        g: Graphics2D,
        targetRegion: Rectangle2D,
        textAttributes: TextAttributes,
    ) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val width = region.widthInPixels.toDouble()
        val band = bandHeight(region.editor).coerceAtMost(region.heightInPixels.toDouble())
        val top = targetRegion.y + (region.heightInPixels - band) / 2.0
        g.color = spec.background
        g.fill(RoundRectangle2D.Double(targetRegion.x, top, width, band, ARC, ARC))

        val titleMetrics = g.getFontMetrics(titleFont(region.editor))
        val labelMetrics = g.getFontMetrics(labelFont(region.editor))
        val valueMetrics = g.getFontMetrics(valueFont(region.editor))
        val baseline = top + VERTICAL_PADDING + maxOf(titleMetrics.ascent, valueMetrics.ascent)

        // Plain horizontal flow: each label and value takes the width it needs,
        // one after the other. Nothing is padded out to a fixed share of the strip.
        val limit = targetRegion.x + width - HORIZONTAL_PADDING
        var x = targetRegion.x + HORIZONTAL_PADDING

        // The command's name first, in the value colour and the value size, bold:
        // it heads the strip, and the labels after it are smaller still.
        g.font = titleFont(region.editor)
        g.color = spec.valueColor
        g.drawString(spec.title, x.toFloat(), baseline.toFloat())
        x += titleMetrics.stringWidth(spec.title) + FIELD_GAP

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

    private fun titleFont(editor: Editor): Font = RenderFonts.font(editor, VALUE_SCALE, bold = true)

    private fun labelFont(editor: Editor): Font = RenderFonts.font(editor, LABEL_SCALE, bold = true)

    private fun valueFont(editor: Editor): Font = RenderFonts.font(editor, VALUE_SCALE)

    private companion object {
        /**
         * Two thirds of the size the strip was first drawn at. Scales the type
         * and everything that sets the strip's height, so the strip keeps its
         * proportions.
         */
        const val SIZE = 2f / 3f

        const val HORIZONTAL_PADDING = 8
        const val VERTICAL_PADDING = 5.0 * SIZE
        const val ARC = 6.0 * SIZE
        const val MIN_WIDTH = 240
        const val LABEL_GAP = 6
        const val FIELD_GAP = 20

        /** Of the editor's font size: first 0.72 and 0.85, now two thirds of those. */
        const val LABEL_SCALE = 0.72f * SIZE
        const val VALUE_SCALE = 0.85f * SIZE
        const val EMPTY_PLACEHOLDER = "—"
    }
}
