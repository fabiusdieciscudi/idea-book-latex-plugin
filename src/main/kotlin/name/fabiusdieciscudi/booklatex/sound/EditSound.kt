/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.sound

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import name.fabiusdieciscudi.booklatex.BookLatexBundle
import name.fabiusdieciscudi.booklatex.insert.isLatexEditor
import nl.hannahsten.texifyidea.psi.LatexCommands

/** Double-click on a strip: the sound command within those offsets. */
fun editSoundWithin(editor: Editor, startOffset: Int, endOffset: Int) =
    editSound(editor) { it.textRange.startOffset >= startOffset && it.textRange.endOffset <= endOffset }

/** Double-click on a pill: the sound command it stands just past. */
fun editSoundEndingAt(editor: Editor, offset: Int) =
    editSound(editor) { it.textRange.endOffset == offset }

/**
 * Opens [SoundDialog] on a sound command and, on confirmation, writes the whole
 * command again -- on one line, by full names -- and the `%` after it.
 */
private fun editSound(editor: Editor, which: (LatexCommands) -> Boolean) {
    val project = editor.project ?: return
    val document = editor.document
    val psiDocumentManager = PsiDocumentManager.getInstance(project)
    val psiFile = psiDocumentManager.getPsiFile(document) ?: return

    val command = SoundCommands.all(psiFile).firstOrNull(which) ?: return
    val spec = SoundSpecs.forCommand(command.name) ?: return
    val initial = SoundCommands.read(command, spec) ?: return

    val dialog = SoundDialog(project, spec, initial, SoundCommands.namesUsed(project, spec, psiFile))
    if (!dialog.showAndGet()) return

    val text = SoundArguments.render(spec, dialog.settings())
    // Survives anything that happened to the document while the dialog was open.
    val marker = document.createRangeMarker(command.textRange)

    WriteCommandAction.runWriteCommandAction(
        project,
        BookLatexBundle.message("command.editSound", spec.commandName),
        null,
        {
            if (marker.isValid) {
                val start = marker.startOffset
                document.replaceString(start, marker.endOffset, text)
                applySoundPercent(document, start, start + text.length, spec)
                psiDocumentManager.commitDocument(document)
            }
        },
        psiFile,
    )
    marker.dispose()
}

/** The `%` after a sound command, by [SoundPercent]'s rules. */
fun applySoundPercent(document: Document, commandStart: Int, commandEnd: Int, spec: SoundSpec) {
    SoundPercent.edit(document.charsSequence, commandStart, commandEnd, spec)
        ?.let { document.replaceString(it.start, it.end, it.replacement) }
}

/**
 * Insert | Ambient Sound… and Insert | Sound Effect…: the dialog, empty, and the
 * command at the caret. An `\ambient` typed into the middle of a line takes
 * what follows it to the next line, as saving would.
 */
abstract class InsertSoundAction(private val spec: SoundSpec) : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = isLatexEditor(event)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return
        val document = editor.document

        val dialog = SoundDialog(project, spec, SoundSettings.empty(), SoundCommands.namesUsed(project, spec, psiFile))
        if (!dialog.showAndGet()) return
        val text = SoundArguments.render(spec, dialog.settings())

        WriteCommandAction.runWriteCommandAction(
            project,
            BookLatexBundle.message("command.insert", spec.commandName),
            null,
            {
                val offset = editor.caretModel.offset
                document.insertString(offset, text)
                editor.caretModel.moveToOffset(offset + text.length)
                applySoundPercent(document, offset, offset + text.length, spec)
                PsiDocumentManager.getInstance(project).commitDocument(document)
            },
            psiFile,
        )
    }
}

class InsertAmbientAction : InsertSoundAction(SoundSpecs.AMBIENT)

class InsertSfxAction : InsertSoundAction(SoundSpecs.SFX)
