/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.settings

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import name.fabiusdieciscudi.booklatex.BookLatexBundle
import name.fabiusdieciscudi.booklatex.ui.LatexEditorColorScheme
import name.fabiusdieciscudi.booklatex.ui.RenderFonts
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JPanel

/** Settings | Tools | Book LaTeX */
class BookLatexConfigurable : Configurable {

    private val enabled = JBCheckBox(BookLatexBundle.message("settings.colorScheme.enabled"))
    private val schemeName = ComboBox<String>()
    private val fontFamily = ComboBox<String>()

    private val settings get() = BookLatexRenderingSettings.getInstance()

    override fun getDisplayName(): String = BookLatexBundle.message("settings.displayName")

    override fun createComponent(): JComponent {
        enabled.addActionListener { schemeName.isEnabled = enabled.isSelected }

        return FormBuilder.createFormBuilder()
            .addComponent(enabled)
            .addLabeledComponent(BookLatexBundle.message("settings.colorScheme.label"), schemeName)
            .addSeparator()
            .addLabeledComponent(BookLatexBundle.message("settings.renderFont.label"), fontFamily)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean =
        enabled.isSelected != settings.latexColorScheme ||
            schemeName.selectedItem != settings.latexColorSchemeName ||
            fontFamily.selectedItem != settings.renderFontFamily

    override fun apply() {
        settings.latexColorScheme = enabled.isSelected
        (schemeName.selectedItem as? String)?.let { settings.latexColorSchemeName = it }
        (fontFamily.selectedItem as? String)?.let { settings.renderFontFamily = it }

        LatexEditorColorScheme.applyToOpenEditors()
        // The blocks are painted, and their width was measured with the old
        // font: only a fresh pass can size them again.
        ProjectManager.getInstance().openProjects
            .filterNot { it.isDisposed }
            .forEach { DaemonCodeAnalyzer.getInstance(it).restart() }
    }

    override fun reset() {
        // Rebuilt every time: the user may have added a scheme, or a font, since.
        fill(schemeName, LatexEditorColorScheme.availableSchemeNames(), settings.latexColorSchemeName)
        fill(fontFamily, RenderFonts.availableFamilies(), settings.renderFontFamily)

        enabled.isSelected = settings.latexColorScheme
        schemeName.isEnabled = enabled.isSelected
    }

    /** Keeps the stored value in the list even when it no longer exists, so that
     *  opening this page cannot quietly change it. */
    private fun fill(combo: ComboBox<String>, available: List<String>, current: String) {
        val items = if (current in available) available else available + current
        combo.model = DefaultComboBoxModel(items.toTypedArray())
        combo.selectedItem = current
    }
}
