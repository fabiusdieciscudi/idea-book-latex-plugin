/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.markers

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.ex.FoldingModelEx
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import name.fabiusdieciscudi.booklatex.settings.BookLatexRenderingSettings
import nl.hannahsten.texifyidea.psi.LatexCommands

/**
 * Folds commands away behind short placeholders.
 *
 * A [MarkerSpec] hides the token alone and leaves the braces: `\french{du pain}`
 * reads as `FR{du pain}`. A [WrapSpec] takes the braces too: `\sq{testo}` reads
 * as `“testo”`.
 *
 * Either way the argument stays in the document, so the caret, selection,
 * completion and the spellchecker keep working inside it. The italics of
 * foreign prose are added on top by [ForeignProseItalicAnnotator], which needs
 * the text to still be there.
 *
 * Like the dialogue folds, these never expand: there is nothing under them but
 * the command, and the status bar switch shows it when wanted.
 *
 * A language marker is raised above the line instead of sitting on it, which no
 * fold placeholder can do: the fold hides the command behind nothing at all and
 * a [SuperscriptInlayRenderer] draws the code just after it. The markers that
 * belong on the baseline — the ellipsis, the quotes of `\sq` — stay ordinary
 * placeholders.
 */
class MarkerFoldPass(
    private val psiFile: PsiFile,
    private val editor: Editor,
) : TextEditorHighlightingPass(psiFile.project, editor.document) {

    private data class Fold(
        val startOffset: Int,
        val endOffset: Int,
        val marker: String,
        /** Raised above the line by an inlay rather than shown as placeholder text. */
        val superscript: Boolean = false,
    )

    private var folds: List<Fold> = emptyList()

    override fun doCollectInformation(progress: ProgressIndicator) {
        if (!BookLatexRenderingSettings.getInstance().smartRendering) {
            folds = emptyList()
            return
        }

        folds = PsiTreeUtil.findChildrenOfType(psiFile, LatexCommands::class.java)
            .flatMap { command -> command.toFolds() }
    }

    private fun LatexCommands.toFolds(): List<Fold> {
        MarkerSpecs.forCommand(name)?.let { return toMarkerFold(it) }
        WrapSpecs.forCommand(name)?.let { return toWrapFolds(it) }
        return emptyList()
    }

    private fun LatexCommands.toMarkerFold(spec: MarkerSpec): List<Fold> {
        if (spec.requiresArgument && parameterList.none { it.requiredParam != null }) return emptyList()

        val start = textRange.startOffset
        // With no arguments the token is the whole command, which is what
        // \ellipsis wants; with arguments it stops before the opening brace.
        val end = commandToken.textRange.endOffset
        if (end <= start) return emptyList()

        // The specs that take an argument are the language ones, and those are
        // the ones that go up. \ellipsis stands on the baseline where it is.
        return listOf(Fold(start, end, spec.marker, superscript = spec.requiresArgument))
    }

    private fun LatexCommands.toWrapFolds(spec: WrapSpec): List<Fold> {
        val argument = parameterList.firstOrNull { it.requiredParam != null }?.textRange ?: return emptyList()

        // The argument range carries its braces: `{` closes the opening fold and
        // `}` is the closing one.
        val openingEnd = argument.startOffset + 1
        val closingStart = argument.endOffset - 1

        // An empty argument would leave the two folds touching.
        if (openingEnd >= closingStart) return emptyList()

        return listOf(
            Fold(textRange.startOffset, openingEnd, spec.opening),
            Fold(closingStart, argument.endOffset, spec.closing),
        )
    }

    override fun doApplyInformationToEditor() {
        val foldingModel = editor.foldingModel as? FoldingModelEx ?: return
        val ours = editor.getUserData(OUR_REGIONS)
            ?: mutableListOf<FoldRegion>().also { editor.putUserData(OUR_REGIONS, it) }
        val ourInlays = editor.getUserData(OUR_INLAYS)
            ?: mutableListOf<Inlay<*>>().also { editor.putUserData(OUR_INLAYS, it) }

        // The inlays go first: they hang off offsets the folds are about to
        // rebuild, and an inlay left behind outlives the marker it was drawing.
        ourInlays.forEach { Disposer.dispose(it) }
        ourInlays.clear()

        foldingModel.runBatchFoldingOperation {
            ours.filter { it.isValid }.forEach { foldingModel.removeFoldRegion(it) }
            ours.clear()

            folds.forEach { fold ->
                val region = foldingModel.createFoldRegion(
                    fold.startOffset,
                    fold.endOffset,
                    if (fold.superscript) HIDDEN else fold.marker,
                    null,
                    true,
                )
                if (region == null) {
                    thisLogger().warn("Refused marker fold at ${fold.startOffset}..${fold.endOffset}")
                    return@forEach
                }
                if (region.isExpanded) region.isExpanded = false
                ours += region
            }
        }

        // Only once the batch has closed and the folds are collapsed. An inlay
        // inside a collapsed region is not drawn, so it is anchored to the
        // offset just past it and tied to the text that follows, which puts it
        // between the hidden command and its opening brace.
        folds.filter { it.superscript }.forEach { fold ->
            val inlay = editor.inlayModel.addInlineElement(
                fold.endOffset,
                false,
                SuperscriptInlayRenderer(fold.marker),
            )
            if (inlay == null) {
                thisLogger().warn("Refused marker inlay at ${fold.endOffset}")
                return@forEach
            }
            ourInlays += inlay
        }
    }

    private companion object {
        val OUR_REGIONS: Key<MutableList<FoldRegion>> = Key.create("booklatex.markers.regions")
        val OUR_INLAYS: Key<MutableList<Inlay<*>>> = Key.create("booklatex.markers.inlays")

        /**
         * The placeholder of a fold whose marker an inlay paints instead: empty,
         * so the marker is not drawn twice. Should the folding model refuse an
         * empty placeholder, a zero-width space is the fallback.
         */
        const val HIDDEN = ""
    }
}
