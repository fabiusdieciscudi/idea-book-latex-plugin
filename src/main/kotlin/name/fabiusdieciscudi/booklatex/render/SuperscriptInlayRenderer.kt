/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.render

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
 * A [baseline] string, when given, is drawn after the raised one and on the
 * line, at the editor's own size. It is there for the punctuation that belongs
 * to the prose and only happens to be rendered in the same place: a fold cannot
 * hold half its placeholder above the line and half on it, so whatever has to
 * straddle the two is drawn here, together.
 *
 * The colour is still read from [EditorColors.FOLDED_TEXT_ATTRIBUTES], so both
 * keep the blue the colour scheme gives every other rendered command and stay
 * configurable from Settings | Editor | Color Scheme.
 */
class SuperscriptInlayRenderer(
    private val raised: String,
    private val baseline: String = "",
) : EditorCustomElementRenderer {

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val editor = inlay.editor
        val components = editor.contentComponent
        return components.getFontMetrics(markerFont(editor)).stringWidth(raised) +
            components.getFontMetrics(proseFont(editor)).stringWidth(baseline)
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

        g2.color = editor.colorsScheme.getAttributes(EditorColors.FOLDED_TEXT_ATTRIBUTES)?.foregroundColor
            ?: textAttributes.foregroundColor
            ?: editor.colorsScheme.defaultForeground

        // The baseline of the surrounding prose, and above it the one the raised
        // text sits on: climbed by a share of the ascent. Both are measured on
        // the editor's own font rather than on the marker's, because what the
        // marker has to line up with is the text.
        val proseMetrics = editor.contentComponent.getFontMetrics(proseFont(editor))
        val proseBaseline = (targetRegion.y + proseMetrics.ascent).toFloat()
        val raisedBaseline = proseBaseline - proseMetrics.ascent * RAISE

        var x = targetRegion.x.toFloat()

        val raisedFont = markerFont(editor)
        g2.font = raisedFont
        g2.drawString(raised, x, raisedBaseline)
        x += editor.contentComponent.getFontMetrics(raisedFont).stringWidth(raised)

        if (baseline.isNotEmpty()) {
            g2.font = proseFont(editor)
            g2.drawString(baseline, x, proseBaseline)
        }
    }

    private fun proseFont(editor: Editor): Font = editor.colorsScheme.getFont(EditorFontType.PLAIN)

    /** The editor's own face, shrunk: a marker is prose, not a painted block. */
    private fun markerFont(editor: Editor): Font {
        val prose = proseFont(editor)
        return prose.deriveFont(prose.size2D * SCALE)
    }

    private companion object {
        /** Small enough to read as a marker, large enough to read at all. */
        const val SCALE = 0.70f

        /** The share of the ascent the baseline climbs. */
        const val RAISE = 0.38f
    }
}
