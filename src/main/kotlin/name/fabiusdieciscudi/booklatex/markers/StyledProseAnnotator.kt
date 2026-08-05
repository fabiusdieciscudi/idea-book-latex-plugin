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
 * Sets the prose of `\textbf{...}` in bold and of `\emph{...}` in italic, both
 * in the blue the plugin draws its renderings in.
 *
 * A kinder cousin of [ForeignProseItalicAnnotator]: font style and colour are
 * both things TextAttributes can carry, so the text stays the document's own --
 * editable, spellchecked -- and only changes how it looks. [MarkerFoldPass]
 * hides the command and the braces around it; this leaves nothing on screen but
 * the prose, thickened or leaning, in the blue that says it is a rendering
 * rather than a word set in bold by hand.
 *
 * Unlike foreign prose, which stays black because its `FR` carries the blue
 * beside it, bold and italic have no marker of their own: the blue has nowhere
 * to go but onto the prose.
 *
 * `\emph` is set in a plain italic, not toggled against the surrounding face the
 * way LaTeX would: an emphasis inside an emphasis is rare enough in a manuscript
 * that reading it as italic-within-italic is no loss, and the alternative is to
 * track a face the fold has already hidden.
 */
class StyledProseAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is LatexCommands) return
        val spec = StyleSpecs.forCommand(element.name) ?: return
        if (!BookLatexRenderingSettings.getInstance().smartRendering) return

        val argument = element.parameterList.firstOrNull { it.requiredParam != null }?.textRange ?: return
        // Nothing between the braces, nothing to set in a face.
        if (argument.length <= 2) return

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(TextRange(argument.startOffset + 1, argument.endOffset - 1))
            .textAttributes(spec.style.attributes())
            .create()
    }

    private fun ProseStyle.attributes(): TextAttributesKey = when (this) {
        ProseStyle.BOLD -> BOLD
        ProseStyle.ITALIC -> ITALIC
    }

    private companion object {
        /**
         * The blue every rendering in this plugin is drawn in, taken from the
         * folded-text foreground the scheme gives the markers. Foreign prose
         * stays black because its `FR` already carries the blue beside it; bold
         * and italic have no marker of their own, so the blue moves onto the
         * prose, which is what tells the eye it is a rendering and not something
         * the author set in bold by hand. The scheme overrides it: this is the
         * fallback other schemes get.
         */
        val RENDERED_BLUE = Color(0x08, 0x41, 0xD8)

        val BOLD: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "BOOKLATEX_BOLD_PROSE",
            TextAttributes(RENDERED_BLUE, null, null, null, Font.BOLD),
        )

        val ITALIC: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "BOOKLATEX_EMPH_PROSE",
            TextAttributes(RENDERED_BLUE, null, null, null, Font.ITALIC),
        )
    }
}
