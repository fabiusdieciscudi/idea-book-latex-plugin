/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.markers

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints

/**
 * Paints a short marker raised above the line, in place of the command it stands for.
 *
 * The folding model can only put a plain string where a command was, drawn on
 * the same baseline as the prose and with the scheme's folded-text attributes.
 * There is no attribute for a superscript, so a marker that is to sit above the
 * line cannot be a fold placeholder at all: the fold hides the command and this
 * renderer, hung off an inline inlay just after it, draws the marker instead.
 *
 * The colour is still read from [EditorColors.FOLDED_TEXT_ATTRIBUTES], so the
 * marker keeps the blue the colour scheme gives every other rendered command
 * and stays configurable from Settings | Editor | Color Scheme.
 */
class SuperscriptInlayRenderer(private val text: String) : EditorCustomElementRenderer {

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val editor = inlay.editor
        return editor.contentComponent.getFontMetrics(markerFont(editor)).stringWidth(text)
    }

    override fun paint(
        inlay: Inlay<*>,
        g: Graphics,
        targetRegion: Rectangle,
        textAttributes: TextAttributes,
    ) {
        // Rendering hints and a baseline that falls between two pixels are both
        // Graphics2D's: plain Graphics draws strings on whole pixels only. The
        // editor always hands one over, and there is nothing to draw without it.
        val g2 = g as? Graphics2D ?: return
        val editor = inlay.editor

        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON,
        )

        g2.font = markerFont(editor)
        g2.color = editor.colorsScheme.getAttributes(EditorColors.FOLDED_TEXT_ATTRIBUTES)?.foregroundColor
            ?: textAttributes.foregroundColor
            ?: editor.colorsScheme.defaultForeground

        // The baseline of the surrounding prose, climbed by a share of its ascent.
        // Measured on the editor's own font rather than on the marker's: the
        // marker is smaller, and what it has to line up with is the text.
        val proseAscent = editor.contentComponent
            .getFontMetrics(editor.colorsScheme.getFont(EditorFontType.PLAIN))
            .ascent
        val baseline = targetRegion.y + proseAscent - proseAscent * RAISE

        g2.drawString(text, targetRegion.x.toFloat(), baseline)
    }

    /** The editor's own face, shrunk: a marker is prose, not a painted block. */
    private fun markerFont(editor: Editor): Font {
        val prose = editor.colorsScheme.getFont(EditorFontType.PLAIN)
        return prose.deriveFont(prose.size2D * SCALE)
    }

    private companion object {
        /** Small enough to read as a marker, large enough to read at all. */
        const val SCALE = 0.70f

        /** The share of the ascent the baseline climbs. */
        const val RAISE = 0.38f
    }
}
