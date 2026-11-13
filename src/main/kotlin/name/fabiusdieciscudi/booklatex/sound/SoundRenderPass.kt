/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.sound

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.CustomFoldRegion
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.ex.FoldingModelEx
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import name.fabiusdieciscudi.booklatex.document.ownsItsLines
import name.fabiusdieciscudi.booklatex.settings.BookLatexRenderingSettings
import nl.hannahsten.texifyidea.psi.LatexCommands

/**
 * Draws `\ambient` and `\sfx`.
 *
 * A command that owns its lines -- nothing before it on its first line but
 * indentation, nothing after it on its last but whitespace or a `%` -- becomes
 * a strip the width of the editor, like a scene. Anywhere else, which for an
 * `\sfx` means inside a sentence, the command folds away behind nothing and a
 * pill is drawn in its place, the way a language code is drawn beside the
 * phrase it marks.
 *
 * A command without its `{name}` yet is left as source: it is being typed.
 */
class SoundRenderPass(
    private val psiFile: PsiFile,
    private val editor: Editor,
) : TextEditorHighlightingPass(psiFile.project, editor.document) {

    private data class Strip(val startLine: Int, val endLine: Int, val spec: SoundSpec, val fields: List<SoundField>)

    private data class Pill(val startOffset: Int, val endOffset: Int, val spec: SoundSpec, val fields: List<SoundField>)

    private var strips: List<Strip> = emptyList()
    private var pills: List<Pill> = emptyList()

    override fun doCollectInformation(progress: ProgressIndicator) {
        strips = emptyList()
        pills = emptyList()
        if (!BookLatexRenderingSettings.getInstance().smartRendering) return

        val document = editor.document
        val found = SoundCommands.all(psiFile).mapNotNull { command -> command.toDrawing(document) }
        strips = found.filterIsInstance<Strip>()
        pills = found.filterIsInstance<Pill>()
    }

    private fun LatexCommands.toDrawing(document: Document): Any? {
        val spec = SoundSpecs.forCommand(name) ?: return null
        val settings = SoundCommands.read(this, spec) ?: return null
        val range = textRange
        if (range.endOffset > document.textLength || range.isEmpty) return null
        val fields = SoundDisplay.fields(spec, settings)

        val startLine = document.getLineNumber(range.startOffset)
        val endLine = document.getLineNumber(range.endOffset)
        val before = document.getText(TextRange(document.getLineStartOffset(startLine), range.startOffset))
        val after = document.getText(TextRange(range.endOffset, document.getLineEndOffset(endLine)))

        return if (ownsItsLines(before, after)) Strip(startLine, endLine, spec, fields)
        else Pill(range.startOffset, range.endOffset, spec, fields)
    }

    override fun doApplyInformationToEditor() {
        val foldingModel = editor.foldingModel as? FoldingModelEx ?: return
        val ourFolds = editor.getUserData(OUR_FOLDS)
            ?: mutableListOf<FoldRegion>().also { editor.putUserData(OUR_FOLDS, it) }
        val ourInlays = editor.getUserData(OUR_INLAYS)
            ?: mutableListOf<Inlay<*>>().also { editor.putUserData(OUR_INLAYS, it) }

        // The inlays first: they hang off offsets the folds are about to rebuild.
        ourInlays.forEach { Disposer.dispose(it) }
        ourInlays.clear()

        foldingModel.runBatchFoldingOperation {
            foldingModel.allFoldRegions
                .filterIsInstance<CustomFoldRegion>()
                .filter { it.renderer is SoundStripRenderer }
                .forEach { foldingModel.removeFoldRegion(it) }
            ourFolds.filter { it.isValid }.forEach { foldingModel.removeFoldRegion(it) }
            ourFolds.clear()

            strips.forEach { strip ->
                val region = foldingModel.addCustomLinesFolding(
                    strip.startLine,
                    strip.endLine,
                    SoundStripRenderer(strip.spec, strip.fields),
                )
                if (region == null) {
                    thisLogger().warn("Refused ${strip.spec.commandName} strip for lines ${strip.startLine}..${strip.endLine}")
                }
            }

            pills.forEach { pill ->
                val region = foldingModel.createFoldRegion(pill.startOffset, pill.endOffset, HIDDEN, null, true)
                if (region == null) {
                    thisLogger().warn("Refused ${pill.spec.commandName} fold at ${pill.startOffset}..${pill.endOffset}")
                    return@forEach
                }
                if (region.isExpanded) region.isExpanded = false
                ourFolds += region
            }
        }

        // Once the folds are collapsed. An inlay inside a collapsed region is not
        // drawn, so it hangs just past the command and is tied to the text that
        // follows, as the language codes of MarkerFoldPass are.
        pills.forEach { pill ->
            val inlay = editor.inlayModel.addInlineElement(pill.endOffset, false, SoundPillRenderer(pill.spec, pill.fields))
            if (inlay == null) {
                thisLogger().warn("Refused ${pill.spec.commandName} pill at ${pill.endOffset}")
                return@forEach
            }
            ourInlays += inlay
        }
    }

    private companion object {
        val OUR_FOLDS: Key<MutableList<FoldRegion>> = Key.create("booklatex.sound.folds")
        val OUR_INLAYS: Key<MutableList<Inlay<*>>> = Key.create("booklatex.sound.inlays")

        /** The command hides behind nothing; the pill is what is seen. */
        const val HIDDEN = ""
    }
}

class SoundRenderPassFactory : TextEditorHighlightingPassFactory, TextEditorHighlightingPassFactoryRegistrar {

    override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
        registrar.registerTextEditorHighlightingPass(this, null, null, false, -1)
    }

    override fun createHighlightingPass(psiFile: PsiFile, editor: Editor): TextEditorHighlightingPass? {
        if (psiFile.language.id != LATEX_LANGUAGE_ID) return null
        return SoundRenderPass(psiFile, editor)
    }

    private companion object {
        const val LATEX_LANGUAGE_ID = "Latex"
    }
}
