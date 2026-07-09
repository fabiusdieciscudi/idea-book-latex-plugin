/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.attributes

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import name.fabiusdieciscudi.booklatex.BookLatexBundle
import name.fabiusdieciscudi.booklatex.document.ensureTrailingPercent
import nl.hannahsten.texifyidea.psi.LatexCommands

/**
 * Opens the attribute dialog for the rendered command inside the given offsets
 * and, on confirmation, rewrites its optional argument.
 *
 * Must be called on the EDT: the dialog is modal.
 */
fun editAttributes(editor: Editor, startOffset: Int, endOffset: Int) {
    val project = editor.project ?: return
    val document = editor.document
    val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return

    val command = PsiTreeUtil.findChildrenOfType(psiFile, LatexCommands::class.java)
        .firstOrNull {
            StripSpecs.forCommand(it.name) != null &&
                it.textRange.startOffset >= startOffset &&
                it.textRange.endOffset <= endOffset
        } ?: return
    val spec = StripSpecs.forCommand(command.name) ?: return

    // Shown greyed, not filled in: confirming an untouched field writes nothing.
    val commands = PsiTreeUtil.findChildrenOfType(psiFile, LatexCommands::class.java)
        .sortedBy { it.textRange.startOffset }
    val inherited = InheritedAttributes.before(commands, spec, command.textRange.startOffset)

    val dialog = AttributesDialog(project, spec, CommandAttributes.read(command, spec), inherited)
    if (!dialog.showAndGet()) return

    // Everything the PSI knows must be read before the document is touched.
    val preserved = CommandAttributes.preservedKeys(command, spec)
    val existingRange = CommandAttributes.optionalArgumentRange(command)
    val insertionOffset = CommandAttributes.insertionOffset(command)
    val commandStart = command.textRange.startOffset
    val lineStart = document.getLineStartOffset(document.getLineNumber(commandStart))
    val baseIndent = document.getText(TextRange(lineStart, commandStart))

    val rendered = AttributeArgument.render(spec, dialog.values(), preserved, baseIndent)

    // Survives the rewrite that invalidates the PSI.
    val commandMarker = document.createRangeMarker(command.textRange).apply { isGreedyToRight = true }

    WriteCommandAction.runWriteCommandAction(
        project,
        BookLatexBundle.message("command.editAttributes", spec.commandName),
        null,
        {
            when {
                existingRange != null ->
                    document.replaceString(existingRange.startOffset, existingRange.endOffset, rendered)
                rendered.isNotEmpty() ->
                    document.insertString(insertionOffset, rendered)
            }
            ensureTrailingPercent(document, commandMarker)
            PsiDocumentManager.getInstance(project).commitDocument(document)
        },
        psiFile,
    )
    commandMarker.dispose()
}
