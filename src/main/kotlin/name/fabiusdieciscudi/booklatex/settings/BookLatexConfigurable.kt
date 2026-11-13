/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.settings

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import name.fabiusdieciscudi.booklatex.BookLatexBundle
import name.fabiusdieciscudi.booklatex.sound.SoundLibrary
import name.fabiusdieciscudi.booklatex.ui.LatexEditorColorScheme
import name.fabiusdieciscudi.booklatex.ui.RenderFonts
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JPanel

/** Settings | Tools | Book LaTeX */
/**
 * *Settings | Tools | Book LaTeX*.
 *
 * Registered for each project, though every preference here is the IDE's: the
 * sound library is named relative to the project's root, and the page needs the
 * project to know where that is.
 */
class BookLatexConfigurable(private val project: Project) : Configurable {

    private val enabled = JBCheckBox(BookLatexBundle.message("settings.colorScheme.enabled"))
    private val schemeName = ComboBox<String>()
    private val editorFontFamily = ComboBox<String>()
    private val editorFontSize = JBIntSpinner(LatexEditorColorScheme.DEFAULT_FONT_SIZE, MIN_FONT_SIZE, MAX_FONT_SIZE)
    private val fontFamily = ComboBox<String>()
    private val apostrophes = JBCheckBox(BookLatexBundle.message("settings.apostrophes.label"))
    private val soundLibrary = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(SoundLibraryBrowseListener(project))
    }

    private val settings get() = BookLatexRenderingSettings.getInstance()

    override fun getDisplayName(): String = BookLatexBundle.message("settings.displayName")

    override fun createComponent(): JComponent {
        enabled.addActionListener {
            val on = enabled.isSelected
            schemeName.isEnabled = on
            editorFontFamily.isEnabled = on
            editorFontSize.isEnabled = on
        }

        return FormBuilder.createFormBuilder()
            .addComponent(enabled)
            .addLabeledComponent(BookLatexBundle.message("settings.colorScheme.label"), schemeName)
            .addLabeledComponent(BookLatexBundle.message("settings.editorFont.label"), editorFontFamily)
            .addLabeledComponent(BookLatexBundle.message("settings.editorFontSize.label"), editorFontSize)
            .addSeparator()
            .addLabeledComponent(BookLatexBundle.message("settings.renderFont.label"), fontFamily)
            .addSeparator()
            .addComponent(apostrophes)
            .addSeparator()
            .addLabeledComponent(BookLatexBundle.message("settings.soundLibrary.label"), soundLibrary)
            .addTooltip(BookLatexBundle.message("settings.soundLibrary.comment"))
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean =
        enabled.isSelected != settings.latexColorScheme ||
            schemeName.selectedItem != settings.latexColorSchemeName ||
            editorFontFamily.selectedItem != settings.editorFontFamily ||
            editorFontSize.number != settings.editorFontSize ||
            fontFamily.selectedItem != settings.renderFontFamily ||
            apostrophes.isSelected != settings.normalizeApostrophes ||
            storedSoundLibrary() != settings.soundLibraryPath

    override fun apply() {
        settings.latexColorScheme = enabled.isSelected
        (schemeName.selectedItem as? String)?.let { settings.latexColorSchemeName = it }
        (editorFontFamily.selectedItem as? String)?.let { settings.editorFontFamily = it }
        settings.editorFontSize = editorFontSize.number
        (fontFamily.selectedItem as? String)?.let { settings.renderFontFamily = it }
        settings.normalizeApostrophes = apostrophes.isSelected
        settings.soundLibraryPath = storedSoundLibrary()

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
        fill(editorFontFamily, RenderFonts.availableFamilies(), settings.editorFontFamily)
        fill(fontFamily, RenderFonts.availableFamilies(), settings.renderFontFamily)
        editorFontSize.value = settings.editorFontSize
        apostrophes.isSelected = settings.normalizeApostrophes
        soundLibrary.text = settings.soundLibraryPath

        enabled.isSelected = settings.latexColorScheme
        schemeName.isEnabled = enabled.isSelected
        editorFontFamily.isEnabled = enabled.isSelected
        editorFontSize.isEnabled = enabled.isSelected
    }

    /** Keeps the stored value in the list even when it no longer exists, so that
     *  opening this page cannot quietly change it. */
    private fun fill(combo: ComboBox<String>, available: List<String>, current: String) {
        val items = if (current in available) available else available + current
        combo.model = DefaultComboBoxModel(items.toTypedArray())
        combo.selectedItem = current
    }

    /** The library as it is stored: relative to the root of the project, however it was typed. */
    private fun storedSoundLibrary(): String = SoundLibrary.relativize(soundLibrary.text, project.basePath)

    companion object {
        // Wide enough for a page-sized face and then some; the editor clamps
        // anything odder than this on its own. The buttons beside the master
        // switch keep to the same range.
        const val MIN_FONT_SIZE = 6
        const val MAX_FONT_SIZE = 96
    }
}

/**
 * The picker of the sound library: it opens on the library already chosen, or
 * else on the project's root, and writes back the path relative to that root.
 */
private class SoundLibraryBrowseListener(private val project: Project) :
    TextBrowseFolderListener(FileChooserDescriptorFactory.singleFile(), project) {

    override fun getInitialFile(): VirtualFile? {
        val current = SoundLibrary.resolve(componentText, project.basePath)
            ?.let { LocalFileSystem.getInstance().findFileByNioFile(it) }
        return current ?: project.guessProjectDir()
    }

    override fun chosenFileToResultingText(chosenFile: VirtualFile): String =
        SoundLibrary.relativize(chosenFile.path, project.basePath)
}
