/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.attributes

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import name.fabiusdieciscudi.booklatex.BookLatexBundle
import name.fabiusdieciscudi.booklatex.ui.DialogSizes
import name.fabiusdieciscudi.booklatex.ui.latexText
import name.fabiusdieciscudi.booklatex.ui.latexTextField

/**
 * Standard modal editor for the attributes of a rendered command.
 *
 * The fields are a third of the main window wide on first opening; the geometry
 * is remembered per command afterwards, since \scene carries four attributes
 * and \beat three.
 */
class AttributesDialog(
    project: Project,
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
            builder.addLabeledComponent(BookLatexBundle.message("dialog.attributes.field", key.replaceFirstChar { it.uppercase() }), field)
        }
        return builder.panel
    }

    override fun getPreferredFocusedComponent(): JComponent? = fields[spec.keys.first()]

    /** An empty field stays empty: the key will not be written to the source. */
    fun values(): Map<String, String> = fields.mapValues { (_, field) -> field.latexText().trim() }
}
