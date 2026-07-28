/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.attributes

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import name.fabiusdieciscudi.booklatex.BookLatexBundle
import name.fabiusdieciscudi.booklatex.ui.DialogSizes
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * Picks a place from the ones the preamble defines.
 *
 * The list shows the expansions, in reading order; what leaves through [chosen]
 * is the command. Cancelling returns nothing and the field it was opened from is
 * left as it was.
 *
 * A filter rather than a speed search: a book accumulates hundreds of these, and
 * typing two words of a half-remembered place is how one is actually found.
 */
class SettingChooserDialog(
    project: Project,
    private val all: List<SettingDefinition>,
) : DialogWrapper(project, true) {

    private val model = DefaultListModel<SettingDefinition>()

    private val list = JBList(model).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>,
                value: Any?,
                index: Int,
                selected: Boolean,
                focused: Boolean,
            ): Component = super.getListCellRendererComponent(
                list,
                (value as? SettingDefinition)?.label ?: value,
                index,
                selected,
                focused,
            )
        }
        addListSelectionListener { isOKActionEnabled = selectedValue != null }
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.clickCount == 2 && selectedValue != null) doOKAction()
            }
        })
    }

    private val filter = JBTextField().apply {
        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = refill()
            override fun removeUpdate(e: DocumentEvent) = refill()
            override fun changedUpdate(e: DocumentEvent) = refill()
        })
    }

    private val preferred = DialogSizes.fractionOfMainWindow(project, DialogSizes.THIRD, HEIGHT_FRACTION)

    init {
        title = BookLatexBundle.message("dialog.setting.title")
        refill()
        init()
        isOKActionEnabled = list.selectedValue != null
    }

    override fun getDimensionServiceKey(): String = "BookLaTeX.SettingChooserDialog"

    override fun createCenterPanel(): JComponent = JPanel(BorderLayout()).apply {
        preferredSize = Dimension(preferred.width, preferred.height)

        if (all.isEmpty()) {
            add(JLabel(BookLatexBundle.message("dialog.setting.empty")), BorderLayout.CENTER)
            return@apply
        }

        val head = JPanel(BorderLayout())
        head.add(JLabel(BookLatexBundle.message("dialog.setting.filter")), BorderLayout.WEST)
        head.add(filter, BorderLayout.CENTER)
        add(head, BorderLayout.NORTH)
        add(JBScrollPane(list), BorderLayout.CENTER)
    }

    override fun getPreferredFocusedComponent(): JComponent? = if (all.isEmpty()) null else filter

    /** The command to write, or null if the dialog was cancelled or empty. */
    fun chosen(): SettingDefinition? = list.selectedValue

    /**
     * Every word typed has to appear somewhere in the label, in any order:
     * "agay spiaggia" finds the beach as readily as "spiaggia agay".
     */
    private fun refill() {
        val words = filter.text.trim().lowercase().split(' ').filter { it.isNotEmpty() }
        val kept = if (words.isEmpty()) all else all.filter { definition ->
            val label = definition.label.lowercase()
            words.all { label.contains(it) }
        }

        val previous = list.selectedValue
        model.clear()
        kept.forEach { model.addElement(it) }

        when {
            previous != null && kept.contains(previous) -> list.setSelectedValue(previous, true)
            kept.isNotEmpty() -> list.selectedIndex = 0
        }
        isOKActionEnabled = list.selectedValue != null
    }

    private companion object {
        /** Taller than it is wide: a list of places is read down. */
        const val HEIGHT_FRACTION = 0.5
    }
}
