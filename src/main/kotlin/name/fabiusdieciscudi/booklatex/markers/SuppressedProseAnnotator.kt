/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.markers

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import name.fabiusdieciscudi.booklatex.settings.BookLatexRenderingSettings
import nl.hannahsten.texifyidea.psi.LatexCommands
import java.awt.Color
import java.awt.Font

/** `\suppressed{...}`: prose the author has cut but is keeping. */
const val SUPPRESSED_COMMAND_NAME = "\\suppressed"

/** `\alternate{...}`: another version of a passage, kept beside the one in use. */
const val ALTERNATE_COMMAND_NAME = "\\alternate"

/**
 * Every command whose argument the preamble swallows (`\newcommand{...}[1]{}`):
 * prose that is in the source but never in the book.
 */
val SUPPRESSED_COMMAND_NAMES: Set<String> = setOf(SUPPRESSED_COMMAND_NAME, ALTERNATE_COMMAND_NAME)

/**
 * Sets the prose of `\suppressed{...}` and `\alternate{...}` in light grey and
 * strikes it through.
 *
 * The same bargain as [NoteProseAnnotator]: colour and a strike are things
 * TextAttributes can carry, so the text stays the document's own -- editable,
 * selectable, spellchecked -- and only looks cut. It reads as struck-out prose
 * the author is done with but has not thrown away, and the `%` the save hook
 * appends after the closing brace keeps the line it owns from becoming a blank
 * one in the typeset book.
 *
 * The two commands mean different things to the author -- a cut, an alternative
 * -- but the same thing to the book, which prints neither: so their prose looks
 * the same, and what tells them apart is the word [MarkerFoldPass] raises in
 * place of the command, `Suppressed` or `Alternate`.
 *
 * The braces are left in place: a `\suppressed` or an `\alternate` may hold
 * whole paragraphs and other commands, and the strike runs across all of it --
 * whatever the commands inside draw is cut along with the words around them.
 */
class SuppressedProseAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is LatexCommands || element.name !in SUPPRESSED_COMMAND_NAMES) return
        if (!BookLatexRenderingSettings.getInstance().smartRendering) return

        val argument = element.parameterList.firstOrNull { it.requiredParam != null }?.textRange ?: return
        // Nothing between the braces, nothing to cut.
        if (argument.length <= 2) return

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(TextRange(argument.startOffset + 1, argument.endOffset - 1))
            .textAttributes(SUPPRESSED)
            .create()
    }

    private companion object {
        /**
         * Light enough to read as set aside on white paper, dark enough to read
         * at all. Defined in code rather than in the scheme so the strike travels
         * with it: a scheme entry would have to carry the effect too or lose it.
         */
        val LIGHT_GREY = Color(0xA6, 0xA6, 0xA6)

        val SUPPRESSED: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "BOOKLATEX_SUPPRESSED_PROSE",
            TextAttributes(LIGHT_GREY, null, LIGHT_GREY, EffectType.STRIKEOUT, Font.PLAIN),
        )
    }
}
