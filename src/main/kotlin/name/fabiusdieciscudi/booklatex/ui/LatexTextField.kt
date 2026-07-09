/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.ui

import com.intellij.codeInsight.AutoPopupController
import com.intellij.lang.Language
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import javax.swing.JComponent
import javax.swing.JTextArea
import javax.swing.text.JTextComponent

private const val LATEX_LANGUAGE_ID = "Latex"

/**
 * A real editor backed by an in-memory LaTeX PsiFile, which is what makes
 * TeXiFy's completion contributors fire inside it. Falls back to a plain Swing
 * field if the Latex language is somehow unavailable.
 *
 * The language is looked up by id so that this plugin never has to link
 * against TeXiFy's Language object.
 */
fun latexTextField(
    project: Project,
    value: String,
    oneLine: Boolean,
    preferredWidth: Int = 480,
    /** Grey text shown while the field is empty. Never becomes the field's value. */
    placeholder: String? = null,
): JComponent {
    val latex = Language.findLanguageByID(LATEX_LANGUAGE_ID) ?: return fallback(value, oneLine, placeholder)

    return object : LanguageTextField(latex, project, value, oneLine) {
        override fun createEditor(): EditorEx = super.createEditor().apply {
            // An EditorTextField takes its background from the surrounding form.
            // Give it the editor's own scheme so the field reads as a piece of
            // editor rather than as a text box that happens to hold LaTeX.
            val scheme = LatexEditorColorScheme.schemeForLatex()
            colorsScheme = scheme
            backgroundColor = scheme.defaultBackground

            settings.isUseSoftWraps = !oneLine
            setVerticalScrollbarVisible(!oneLine)
            // Pop the completion list up while typing, not only on Ctrl+Space.
            putUserData(AutoPopupController.ALWAYS_AUTO_POPUP, true)
        }
    }.apply {
        setPreferredWidth(preferredWidth)
        placeholder?.let {
            setPlaceholder(it)
            setShowPlaceholderWhenFocused(true)
        }
        background = LatexEditorColorScheme.schemeForLatex().defaultBackground
    }
}

/** Reads back whichever component [latexTextField] produced. */
fun JComponent.latexText(): String = when (this) {
    is EditorTextField -> text
    is JTextComponent -> text
    is JBScrollPane -> (viewport.view as? JTextComponent)?.text.orEmpty()
    else -> ""
}

private fun fallback(value: String, oneLine: Boolean, placeholder: String?): JComponent =
    if (oneLine) {
        JBTextField(value, 48).apply { placeholder?.let { emptyText.text = it } }
    } else {
        JBScrollPane(JTextArea(value, 10, 60))
    }
