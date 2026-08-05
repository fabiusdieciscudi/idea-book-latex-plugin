/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.render

import com.intellij.openapi.editor.CustomFoldRegion
import com.intellij.openapi.editor.CustomFoldRegionRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.TextAttributes
import name.fabiusdieciscudi.booklatex.ui.RenderFonts
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D

/**
 * Paints a `%` divider as a blue line the width of the editor, its label, if
 * there is one, raised and blue in front of it.
 *
 * The blue is the same the folded-text attributes give every other rendering, so
 * the line, the label, a language code and a sticky note's placeholder all speak
 * with one colour, and all follow the scheme if it is changed. A custom fold
 * region is used, as the attribute strips are, because only it can take the whole
 * width of the editor and stand in for a line rather than a run of characters.
 *
 * The label is drawn in [RenderFonts], the sans the painted renderings share,
 * and lifted clear of the line so it reads as a heading on the divider rather
 * than as a word the line runs through.
 */
class HorizontalRuleRenderer(private val caption: String?) : CustomFoldRegionRenderer {

    override fun calcWidthInPixels(region: CustomFoldRegion): Int =
        (region.editor.scrollingModel.visibleArea.width - 2 * HORIZONTAL_PADDING).coerceAtLeast(MIN_WIDTH)

    override fun calcHeightInPixels(region: CustomFoldRegion): Int = region.editor.lineHeight

    override fun paint(
        region: CustomFoldRegion,
        g: Graphics2D,
        targetRegion: Rectangle2D,
        textAttributes: TextAttributes,
    ) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val editor = region.editor
        g.color = renderedBlue(editor)

        val left = targetRegion.x + HORIZONTAL_PADDING
        val right = targetRegion.x + region.widthInPixels - HORIZONTAL_PADDING
        // A shade below centre, to leave the raised label room above it.
        val ruleY = targetRegion.y + region.heightInPixels * RULE_HEIGHT_FRACTION

        var ruleStart = left
        if (!caption.isNullOrEmpty()) {
            val font = RenderFonts.font(editor, CAPTION_SCALE)
            g.font = font
            // The baseline just above the line, so the whole label sits over it.
            val baseline = ruleY - CAPTION_LIFT
            g.drawString(caption, left.toFloat(), baseline.toFloat())
            ruleStart = left + g.fontMetrics.stringWidth(caption) + CAPTION_GAP
        }

        if (ruleStart < right) {
            g.stroke = BasicStroke(THICKNESS)
            g.draw(Line2D.Double(ruleStart, ruleY, right, ruleY))
        }
    }

    /** The folded-text foreground the scheme gives the markers, or a blue fallback. */
    private fun renderedBlue(editor: Editor): Color =
        editor.colorsScheme.getAttributes(EditorColors.FOLDED_TEXT_ATTRIBUTES)?.foregroundColor
            ?: FALLBACK_BLUE

    private companion object {
        const val HORIZONTAL_PADDING = 8
        const val MIN_WIDTH = 240

        /** Thin enough to read as a rule, not a bar. */
        const val THICKNESS = 1.25f

        /** Where the line sits within the row: a little below the middle. */
        const val RULE_HEIGHT_FRACTION = 0.62

        const val CAPTION_SCALE = 0.72f

        /** How far the label's baseline is lifted above the line, in pixels. */
        const val CAPTION_LIFT = 2.0

        /** The gap between the label and the start of the line. */
        const val CAPTION_GAP = 8

        /** The same blue as the scheme's folded text, for schemes that leave it unset. */
        val FALLBACK_BLUE: Color = Color(0x08, 0x41, 0xD8)
    }
}
