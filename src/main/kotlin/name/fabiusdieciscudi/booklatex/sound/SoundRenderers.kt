/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.sound

import com.intellij.openapi.editor.CustomFoldRegion
import com.intellij.openapi.editor.CustomFoldRegionRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import name.fabiusdieciscudi.booklatex.ui.RenderFonts
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A sound command that owns its lines, drawn as a strip in the manner of a
 * scene's: `AMBIENT  pioggia  LOUDNESS -6 dB  FADE 3 s`, in two thirds of the
 * editor's type.
 *
 * The strip is as wide as what it says, not the editor: a sound is a note in the
 * margin of the story, not a heading across it. It stands in the middle of the
 * line, like a break between sections. Only a strip that would not fit is
 * stopped at the editor's edges, and what is left over is cut with an ellipsis.
 *
 * The region itself spans the editor, so that the band can be centred in it:
 * a region is laid out from the left margin, and only its painting can move.
 *
 * And it is thinner than the line it stands on. The platform never makes a
 * custom fold region shorter than a line of the editor, whatever the renderer
 * asks for (`CustomFoldRegionImpl.doUpdate`); the way around that is internal
 * API. So the region keeps its line, and the band is painted only as tall as
 * its text, in the middle of it, the rest left white: it reads as the space
 * between two paragraphs. Its corners are rounded by a share of its height, so
 * they stay round at any size.
 */
class SoundStripRenderer(
    private val spec: SoundSpec,
    private val fields: List<SoundField>,
) : CustomFoldRegionRenderer {

    override fun calcWidthInPixels(region: CustomFoldRegion): Int =
        (region.editor.scrollingModel.visibleArea.width - 2 * HORIZONTAL_PADDING).coerceAtLeast(MIN_WIDTH)

    override fun calcHeightInPixels(region: CustomFoldRegion): Int =
        bandHeight(SoundRow(region.editor, spec, fields)).roundToInt()

    override fun paint(region: CustomFoldRegion, g: Graphics2D, targetRegion: Rectangle2D, textAttributes: TextAttributes) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val row = SoundRow(region.editor, spec, fields)
        val available = region.widthInPixels.toDouble()
        val width = (row.width + 2.0 * HORIZONTAL_PADDING).coerceAtMost(available)
        val left = targetRegion.x + (available - width) / 2.0
        val band = bandHeight(row).coerceAtMost(region.heightInPixels.toDouble())
        val top = targetRegion.y + (region.heightInPixels - band) / 2.0
        val arc = band * ROUNDING

        g.color = spec.background
        g.fill(RoundRectangle2D.Double(left, top, width, band, arc, arc))

        row.paint(
            g,
            x = left + HORIZONTAL_PADDING,
            baseline = top + VERTICAL_PADDING + row.ascent,
            limit = left + width - HORIZONTAL_PADDING,
        )
    }

    private fun bandHeight(row: SoundRow): Double = row.height + 2 * VERTICAL_PADDING

    private companion object {
        const val HORIZONTAL_PADDING = 8
        const val VERTICAL_PADDING = 5.0 * SoundRow.SIZE
        const val MIN_WIDTH = 240

        /** The diameter of the corners, as a share of the band's height: a third of it rounds each corner. */
        const val ROUNDING = 0.66
    }
}

/**
 * An `\sfx` inside a sentence, drawn as a coloured pill in its place: the same
 * row as a strip, as wide as it needs and no wider, centred on the line. The
 * command itself is folded away behind it.
 */
class SoundPillRenderer(
    private val spec: SoundSpec,
    private val fields: List<SoundField>,
) : EditorCustomElementRenderer {

    override fun calcWidthInPixels(inlay: Inlay<*>): Int =
        SoundRow(inlay.editor, spec, fields).width + 2 * (HORIZONTAL_PADDING + MARGIN)

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        val g2 = g as? Graphics2D ?: return
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val row = SoundRow(inlay.editor, spec, fields)
        val height = (row.height + 2 * VERTICAL_PADDING).coerceAtMost(targetRegion.height)
        val top = targetRegion.y + (targetRegion.height - height) / 2.0
        val left = targetRegion.x + MARGIN.toDouble()
        val width = targetRegion.width - 2.0 * MARGIN

        g2.color = spec.background
        g2.fill(RoundRectangle2D.Double(left, top, width, height.toDouble(), height.toDouble(), height.toDouble()))

        row.paint(
            g2,
            x = left + HORIZONTAL_PADDING,
            baseline = top + (height - row.height) / 2.0 + row.ascent,
            limit = left + width - HORIZONTAL_PADDING,
        )
    }

    private companion object {
        const val HORIZONTAL_PADDING = 6
        const val VERTICAL_PADDING = 1
        /** Between the pill and the words on either side of it. */
        const val MARGIN = 2
    }
}

/** The title and the fields side by side: what a strip and a pill have in common. */
internal class SoundRow(editor: Editor, private val spec: SoundSpec, private val fields: List<SoundField>) {

    private val titleFont = RenderFonts.font(editor, VALUE_SCALE, bold = true)
    private val labelFont = RenderFonts.font(editor, LABEL_SCALE, bold = true)
    private val valueFont = RenderFonts.font(editor, VALUE_SCALE)

    private val titleMetrics: FontMetrics = editor.contentComponent.getFontMetrics(titleFont)
    private val labelMetrics: FontMetrics = editor.contentComponent.getFontMetrics(labelFont)
    private val valueMetrics: FontMetrics = editor.contentComponent.getFontMetrics(valueFont)

    val height: Int get() = max(titleMetrics.height, valueMetrics.height)
    val ascent: Int get() = max(titleMetrics.ascent, valueMetrics.ascent)

    val width: Int
        get() = titleMetrics.stringWidth(spec.title) + fields.sumOf { FIELD_GAP + fieldWidth(it) }

    private fun fieldWidth(field: SoundField): Int {
        val label = field.label?.let { labelMetrics.stringWidth(it) } ?: 0
        val value = field.value?.let { valueMetrics.stringWidth(it) } ?: 0
        val gap = if (field.label != null && field.value != null) LABEL_GAP else 0
        return label + gap + value
    }

    /** Left to right from [x]; what does not fit before [limit] is cut, with an ellipsis. */
    fun paint(g: Graphics2D, x: Double, baseline: Double, limit: Double) {
        var cursor = x
        val y = baseline.toFloat()

        g.font = titleFont
        g.color = spec.valueColor
        g.drawString(spec.title, cursor.toFloat(), y)
        cursor += titleMetrics.stringWidth(spec.title)

        for (field in fields) {
            cursor += FIELD_GAP
            if (cursor >= limit) break

            field.label?.let { label ->
                g.font = labelFont
                g.color = spec.labelColor
                val shown = fit(label, labelMetrics, (limit - cursor).toInt())
                g.drawString(shown, cursor.toFloat(), y)
                cursor += labelMetrics.stringWidth(shown)
                if (field.value != null) cursor += LABEL_GAP
            }
            field.value?.let { value ->
                if (cursor >= limit) return
                g.font = valueFont
                g.color = spec.valueColor
                val shown = fit(value, valueMetrics, (limit - cursor).toInt())
                g.drawString(shown, cursor.toFloat(), y)
                cursor += valueMetrics.stringWidth(shown)
            }
        }
    }

    private fun fit(text: String, metrics: FontMetrics, maxWidth: Int): String {
        if (maxWidth <= 0) return ""
        if (metrics.stringWidth(text) <= maxWidth) return text
        var candidate = text
        while (candidate.isNotEmpty() && metrics.stringWidth("$candidate…") > maxWidth) candidate = candidate.dropLast(1)
        return "$candidate…"
    }

    companion object {
        /** Two thirds of the type the scene strips were first drawn at, as they are now. */
        const val SIZE = 2f / 3f
        const val LABEL_SCALE = 0.72f * SIZE
        const val VALUE_SCALE = 0.85f * SIZE
        const val LABEL_GAP = 4
        const val FIELD_GAP = 12
    }
}
