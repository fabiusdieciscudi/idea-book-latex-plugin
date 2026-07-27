/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.markers

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import name.fabiusdieciscudi.booklatex.settings.BookLatexRenderingSettings
import nl.hannahsten.texifyidea.psi.LatexCommands
import java.awt.Color
import java.awt.Font

/**
 * Sets the prose of `\note{...}` in dark red and italic, the marker above it
 * left blue and upright.
 *
 * The same bargain as [ForeignProseItalicAnnotator], and for the same reason:
 * colour and font style are things TextAttributes can carry and size is not, so
 * the note is coloured and leaned rather than shrunk, and stays the document's
 * own text -- editable, selectable, spellchecked -- instead of becoming a
 * painted block. The italic sets an aside apart from the prose even where the
 * red is hard to tell from black, on a dark theme or a tired screen.
 *
 * The braces are left alone. They are what tells the eye where the note ends,
 * and colouring them too would blur that.
 */
class NoteProseAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is LatexCommands || element.name != MarkerSpecs.NOTE.commandName) return
        if (!BookLatexRenderingSettings.getInstance().smartRendering) return

        val argument = element.parameterList.firstOrNull { it.requiredParam != null }?.textRange ?: return
        // Nothing between the braces, nothing to colour.
        if (argument.length <= 2) return

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(TextRange(argument.startOffset + 1, argument.endOffset - 1))
            .textAttributes(NOTE_PROSE)
            .create()
    }

    private companion object {
        /**
         * Dark enough to read as an aside on white paper rather than as an
         * error. The scheme overrides it: this is what other schemes fall back
         * to.
         */
        val DARK_RED = Color(0x8B, 0x1A, 0x1A)

        val NOTE_PROSE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "BOOKLATEX_NOTE_PROSE",
            TextAttributes(DARK_RED, null, null, null, Font.ITALIC),
        )
    }
}
