/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.ui

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import name.fabiusdieciscudi.booklatex.settings.BookLatexRenderingSettings

/**
 * Gives LaTeX sources their own colour scheme, one editor at a time.
 *
 * Settings offers a single scheme for the whole IDE, which is why this needs a
 * plugin at all. The API is finer: an Editor carries its own scheme, so a .tex
 * and a .kt can sit side by side under different ones. Nothing global is
 * touched, so nothing has to be put back when another file is opened.
 */
object LatexEditorColorScheme {

    /** The name inside colorSchemes/LaTeX.xml. The IDE lists schemes by it. */
    const val DEFAULT_SCHEME_NAME = "LaTeX"

    private val SOURCE_EXTENSIONS = setOf("tex", "sty", "cls")

    /** Editors with no file behind them -- our dialogs' LaTeX fields -- are skipped. */
    fun applyTo(editor: Editor) {
        val editorEx = editor as? EditorEx ?: return
        val file = FileDocumentManager.getInstance().getFile(editorEx.document) ?: return
        if (file.extension?.lowercase() !in SOURCE_EXTENSIONS) return

        val target = targetScheme()
        if (editorEx.colorsScheme === target) return

        editorEx.colorsScheme = target
        // Assigning the scheme changes the model. Without this the highlighter
        // and the view keep painting with the colours they were built with.
        editorEx.reinitSettings()
    }

    /** Called when a preference changes: the open editors have to catch up. */
    fun applyToOpenEditors() {
        EditorFactory.getInstance().allEditors.forEach { applyTo(it) }
    }

    /** Every scheme the user could pick, as shown in Settings. */
    fun availableSchemeNames(): List<String> =
        EditorColorsManager.getInstance().allSchemes.map { it.name }

    /**
     * The scheme a LaTeX editor should wear -- including the ones inside our own
     * dialogs, which are editors too and were being handed the global scheme.
     */
    fun schemeForLatex(): EditorColorsScheme = targetScheme()

    private fun targetScheme(): EditorColorsScheme {
        val manager = EditorColorsManager.getInstance()
        val settings = BookLatexRenderingSettings.getInstance()
        if (!settings.latexColorScheme) return manager.globalScheme

        val name = settings.latexColorSchemeName
        return manager.getScheme(name) ?: manager.globalScheme.also {
            thisLogger().warn(
                "BookLaTeX: no colour scheme named '$name'. Available: ${availableSchemeNames()}"
            )
        }
    }
}
