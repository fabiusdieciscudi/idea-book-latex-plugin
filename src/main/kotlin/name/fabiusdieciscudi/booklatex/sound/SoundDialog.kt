/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.sound

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import name.fabiusdieciscudi.booklatex.BookLatexBundle
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * The options of an `\ambient` or an `\sfx`, one widget each.
 *
 *  - The name: a closed list, the names of the sound library named in the
 *    settings (see [SoundLibrary]); a sound BookAssistant does not have would
 *    only fail later, in the audio book. It is required -- the preamble refuses
 *    a sound without one. A command whose name has since left the library keeps
 *    it, at the top of the list, with a warning; and a library name that would
 *    break the command around it is refused.
 *  - For `\ambient`, a box that stops the background instead, writing
 *    `\ambient{off}`; ticked, it disables everything else.
 *  - A number: a text field with its unit beside it, checked as it is typed
 *    against a range wide enough for any reasonable use. Text rather than a
 *    spinner, because a spinner formats by locale, and in Italian it would write
 *    `0,5` where TeX reads `0.5`. Empty leaves the option out.
 *  - A switch: a check box, set to the default when the option is absent.
 *
 * Options the dialog does not know are kept as they were, unseen.
 */
class SoundDialog(
    project: Project,
    private val spec: SoundSpec,
    initial: SoundSettings,
    knownNames: List<String>,
) : DialogWrapper(project, true) {

    private val stopped = initial.isStop(spec)

    private val libraryNames = knownNames

    private val currentName = if (stopped) "" else initial.name.trim()

    private val nameField = ComboBox(
        (if (currentName.isNotEmpty() && currentName !in libraryNames) listOf(currentName) + libraryNames else libraryNames)
            .toTypedArray(),
    ).apply {
        isEditable = false
        // A new command starts with nothing chosen, rather than with the first sound.
        selectedItem = currentName.ifEmpty { null }
    }

    private val stopBox: JBCheckBox? = spec.stopName?.let {
        JBCheckBox(BookLatexBundle.message("dialog.sound.stop"), stopped)
    }

    private val numberFields: Map<NumberParameter, JBTextField> =
        spec.parameters.filterIsInstance<NumberParameter>().associateWith { parameter ->
            JBTextField(initial.numbers[parameter.key].orEmpty(), NUMBER_COLUMNS).apply {
                toolTipText = rangeText(parameter)
            }
        }

    private val switchBoxes: Map<SwitchParameter, JBCheckBox> =
        spec.parameters.filterIsInstance<SwitchParameter>().associateWith { parameter ->
            JBCheckBox(BookLatexBundle.message("dialog.sound.${parameter.key}"), initial.switch(parameter))
        }

    private val preserved = initial.preserved

    init {
        title = BookLatexBundle.message("dialog.sound.title.${spec.title.lowercase()}")
        stopBox?.addActionListener { updateEnabled() }
        init()
        updateEnabled()
        initValidation()
    }

    override fun getDimensionServiceKey(): String = "BookLaTeX.SoundDialog.${spec.title.lowercase()}"

    override fun createCenterPanel(): JComponent {
        val builder = FormBuilder.createFormBuilder()
            .addLabeledComponent(BookLatexBundle.message("dialog.sound.name"), nameField)
        stopBox?.let { builder.addComponent(it) }

        spec.parameters.forEach { parameter ->
            when (parameter) {
                is NumberParameter -> builder.addLabeledComponent(
                    BookLatexBundle.message("dialog.sound.${parameter.key}"),
                    withUnit(numberFields.getValue(parameter), parameter.unit),
                )
                is SwitchParameter -> builder.addComponent(switchBoxes.getValue(parameter))
            }
        }
        return builder.panel
    }

    override fun getPreferredFocusedComponent(): JComponent = nameField

    override fun doValidateAll(): List<ValidationInfo> {
        if (isStopped()) return emptyList()

        val problems = mutableListOf<ValidationInfo>()
        val name = nameText()
        when {
            name.isEmpty() && libraryNames.isEmpty() ->
                problems += ValidationInfo(BookLatexBundle.message("dialog.sound.noLibrary"), nameField)
            name.isEmpty() -> problems += ValidationInfo(BookLatexBundle.message("dialog.sound.nameRequired"), nameField)
            name.any { it in FORBIDDEN_IN_NAME } ->
                problems += ValidationInfo(BookLatexBundle.message("dialog.sound.nameInvalid"), nameField)
            name !in libraryNames ->
                problems += ValidationInfo(BookLatexBundle.message("dialog.sound.notInLibrary"), nameField)
                    .asWarning()
                    .withOKEnabled()
        }

        numberFields.forEach { (parameter, field) ->
            when (parameter.problem(field.text)) {
                NumberParameter.Problem.NOT_A_NUMBER ->
                    problems += ValidationInfo(BookLatexBundle.message("dialog.sound.notANumber"), field)
                NumberParameter.Problem.OUT_OF_RANGE ->
                    problems += ValidationInfo(rangeText(parameter), field)
                null -> Unit
            }
        }
        return problems
    }

    /** What the dialog says, to be written back with [SoundArguments.render]. */
    fun settings(): SoundSettings {
        val stopName = spec.stopName
        if (isStopped() && stopName != null) return SoundSettings(stopName, preserved = preserved)

        return SoundSettings(
            name = nameText(),
            numbers = numberFields
                .map { (parameter, field) -> parameter.key to field.text.trim() }
                .filter { (_, value) -> value.isNotEmpty() }
                .toMap(),
            switches = switchBoxes.map { (parameter, box) -> parameter.key to box.isSelected }.toMap(),
            preserved = preserved,
        )
    }

    private fun isStopped(): Boolean = stopBox?.isSelected == true

    private fun nameText(): String = (nameField.selectedItem as? String).orEmpty().trim()

    private fun updateEnabled() {
        val enabled = !isStopped()
        nameField.isEnabled = enabled
        numberFields.values.forEach { it.isEnabled = enabled }
        switchBoxes.values.forEach { it.isEnabled = enabled }
    }

    private fun withUnit(field: JComponent, unit: String): JComponent =
        JPanel(BorderLayout(JBUI.scale(UNIT_GAP), 0)).apply {
            add(field, BorderLayout.CENTER)
            add(JBLabel(unit), BorderLayout.EAST)
        }

    private fun rangeText(parameter: NumberParameter): String =
        BookLatexBundle.message("dialog.sound.range", parameter.min.plain(), parameter.max.plain(), parameter.unit)

    /** `-60`, not `-60.0`; `0.5` as it is. */
    private fun Double.plain(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

    private companion object {
        const val NUMBER_COLUMNS = 8
        const val UNIT_GAP = 6

        /** What would close the command's braces, escape something, or comment the rest out. */
        const val FORBIDDEN_IN_NAME = "{}\\%"
    }
}
