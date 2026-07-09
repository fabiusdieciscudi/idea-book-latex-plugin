/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.comment

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel
import name.fabiusdieciscudi.booklatex.BookLatexBundle
import name.fabiusdieciscudi.booklatex.ui.DialogSizes
import name.fabiusdieciscudi.booklatex.ui.latexText
import name.fabiusdieciscudi.booklatex.ui.latexTextField

/**
 * A single multi-line LaTeX editor for the body of `\comment{...}`.
 *
 * A third of the main window on first opening; whatever the user last dragged
 * it to, afterwards.
 */
class CommentDialog(project: Project, initial: String) : DialogWrapper(project, true) {

    private val initialSize: Dimension =
        DialogSizes.fractionOfMainWindow(project, DialogSizes.THIRD, DialogSizes.THIRD)

    private val field = latexTextField(project, initial, oneLine = false, preferredWidth = initialSize.width)

    init {
        title = BookLatexBundle.message("dialog.comment.title")
        init()
    }

    override fun getDimensionServiceKey(): String = DIMENSION_KEY

    override fun createCenterPanel(): JComponent = JPanel(BorderLayout()).apply {
        preferredSize = initialSize
        add(field, BorderLayout.CENTER)
    }

    override fun getPreferredFocusedComponent(): JComponent = field

    fun text(): String = field.latexText()

    private companion object {
        const val DIMENSION_KEY = "BookLaTeX.CommentDialog"
    }
}
