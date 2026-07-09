/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.ui

import com.intellij.openapi.editor.Editor
import name.fabiusdieciscudi.booklatex.settings.BookLatexRenderingSettings
import java.awt.Font
import java.awt.GraphicsEnvironment

/**
 * The font the painted renderings use: sticky notes and attribute strips.
 *
 * Not the editor's font. Those blocks are not source: they are a rendering of
 * it, and a proportional face sets them apart from the monospaced prose behind
 * them at a glance. The family is a preference; the size follows the editor's,
 * so the blocks grow and shrink with Ctrl+Wheel.
 */
object RenderFonts {

    /** A logical family, so it resolves to something on every platform. */
    const val DEFAULT_FAMILY: String = Font.SANS_SERIF

    fun font(editor: Editor, scale: Float, bold: Boolean = false): Font {
        val family = BookLatexRenderingSettings.getInstance().renderFontFamily
        val style = if (bold) Font.BOLD else Font.PLAIN
        val size = editor.colorsScheme.editorFontSize.toFloat()
        return Font(family, style, size.toInt()).deriveFont(size * scale)
    }

    /** The logical families first: they always resolve, whatever is installed. */
    fun availableFamilies(): List<String> {
        val installed = GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.toList()
        val logical = listOf(Font.SANS_SERIF, Font.SERIF, Font.MONOSPACED)
        return logical + installed.filterNot { it in logical }
    }
}
