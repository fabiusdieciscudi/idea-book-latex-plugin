/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.attributes

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import name.fabiusdieciscudi.booklatex.BookLatexBundle
import name.fabiusdieciscudi.booklatex.ui.DialogSizes
import name.fabiusdieciscudi.booklatex.ui.latexText
import name.fabiusdieciscudi.booklatex.ui.latexTextField
import name.fabiusdieciscudi.booklatex.ui.setLatexText

/**
 * Standard modal editor for the attributes of a rendered command.
 *
 * The fields are a third of the main window wide on first opening; the geometry
 * is remembered per command afterwards, since \scene carries four attributes
 * and \beat three.
 *
 * The setting is the one field that is not free text in practice: a book returns
 * to the same places, and each of them is a command in the preamble. It gets a
 * button onto [SettingChooserDialog], which shows those places by the name they
 * print under and writes back the command. Typing into the field directly still
 * works, so nothing is taken away -- least of all the first time a place is
 * used, when there is no command to choose yet.
 */
class AttributesDialog(
    private val project: Project,
    private val spec: StripSpec,
    initial: Map<String, String>,
    /** Shown greyed while a field is empty, and never written by confirming. */
    placeholders: Map<String, String> = emptyMap(),
) : DialogWrapper(project, true) {

    private val fields: Map<String, JComponent> =
        // Only the width is taken from the frame: the height is however many
        // rows the spec has.
        DialogSizes.widthFractionOfMainWindow(project, DialogSizes.THIRD).let { width ->
            spec.keys.associateWith { key ->
                latexTextField(
                    project,
                    initial[key].orEmpty(),
                    oneLine = true,
                    preferredWidth = width,
                    placeholder = placeholders[key],
                )
            }
        }

    init {
        title = BookLatexBundle.message("dialog.attributes.title", spec.commandName.removePrefix("\\").replaceFirstChar { it.uppercase() })
        init()
    }

    override fun getDimensionServiceKey(): String =
        "BookLaTeX.AttributesDialog.${spec.commandName.removePrefix("\\")}"

    override fun createCenterPanel(): JComponent {
        val builder = FormBuilder.createFormBuilder()
        fields.forEach { (key, field) ->
            val component = if (key == SETTING_KEY) withChooser(field) else field
            builder.addLabeledComponent(BookLatexBundle.message("dialog.attributes.field", key.replaceFirstChar { it.uppercase() }), component)
        }
        return builder.panel
    }

    /**
     * The field with a button beside it. Only the wrapper is new: [fields] keeps
     * the text component itself, so [values] reads what it always read.
     */
    private fun withChooser(field: JComponent): JComponent = JPanel(BorderLayout()).apply {
        add(field, BorderLayout.CENTER)
        add(
            JButton(BookLatexBundle.message("dialog.setting.choose")).apply {
                toolTipText = BookLatexBundle.message("dialog.setting.choose.tooltip")
                addActionListener { chooseSetting(field) }
            },
            BorderLayout.EAST,
        )
    }

    /** Cancelling leaves the field exactly as it was. */
    private fun chooseSetting(field: JComponent) {
        val definitions = ReadAction.compute<List<SettingDefinition>, RuntimeException> {
            SettingDefinitions.getInstance(project).definitions()
        }

        val chooser = SettingChooserDialog(project, definitions)
        if (!chooser.showAndGet()) return
        chooser.chosen()?.let { field.setLatexText(it.command) }
    }

    override fun getPreferredFocusedComponent(): JComponent? = fields[spec.keys.first()]

    /** An empty field stays empty: the key will not be written to the source. */
    fun values(): Map<String, String> = fields.mapValues { (_, field) -> field.latexText().trim() }

    private companion object {
        /** Keyed by name, so a future spec carrying a setting gets the button too. */
        const val SETTING_KEY = "setting"
    }
}
