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
import java.awt.Font

/**
 * Sets foreign prose in italics: `\french{...}`, `\latin{...}`, and the rest.
 *
 * The one place in this plugin where plain highlighting is enough: font style
 * is among the three things TextAttributes can carry, unlike font size. So the
 * text stays the document's own -- editable, spellchecked -- and only leans.
 *
 * The `...name` variants are left upright: a name is not foreign prose.
 */
class ForeignProseItalicAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is LatexCommands || element.name !in Languages.PROSE_COMMANDS) return
        if (!BookLatexRenderingSettings.getInstance().smartRendering) return

        val argument = element.parameterList.firstOrNull { it.requiredParam != null }?.textRange ?: return
        // The braces stay upright; only what they hold leans.
        if (argument.length <= 2) return

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(TextRange(argument.startOffset + 1, argument.endOffset - 1))
            .textAttributes(ITALIC)
            .create()
    }

    private companion object {
        val ITALIC: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            "BOOKLATEX_FOREIGN_PROSE",
            TextAttributes(null, null, null, null, Font.ITALIC),
        )
    }
}
