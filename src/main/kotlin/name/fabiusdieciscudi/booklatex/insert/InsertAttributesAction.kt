/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.insert

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import name.fabiusdieciscudi.booklatex.BookLatexBundle
import name.fabiusdieciscudi.booklatex.attributes.AttributeArgument
import name.fabiusdieciscudi.booklatex.attributes.AttributesDialog
import name.fabiusdieciscudi.booklatex.attributes.StripSpec
import name.fabiusdieciscudi.booklatex.attributes.StripSpecs

/**
 * Inserts a `\scene[...]`, `\beat[...]` or `\shot[...]` through the same dialog
 * that edits one. Attributes left empty are simply not written, so confirming an
 * untouched dialog inserts the bare command.
 */
abstract class InsertAttributesAction(private val spec: StripSpec) : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = isLatexEditor(event)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return

        val empty = spec.keys.associateWith { "" }
        val dialog = AttributesDialog(project, spec, empty)
        if (!dialog.showAndGet()) return

        val argument = AttributeArgument.render(spec, dialog.values(), emptyMap(), indentAtCaret(editor))
        insertAtCaret(
            project,
            editor,
            psiFile,
            BookLatexBundle.message("command.insert", spec.commandName),
            spec.commandName + argument,
        )
    }
}

class InsertSceneAction : InsertAttributesAction(StripSpecs.SCENE)

class InsertBeatAction : InsertAttributesAction(StripSpecs.BEAT)

class InsertShotAction : InsertAttributesAction(StripSpecs.SHOT)
