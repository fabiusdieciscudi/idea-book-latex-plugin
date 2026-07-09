/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.dialogue

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.editor.ex.FoldingModelEx
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import name.fabiusdieciscudi.booklatex.settings.BookLatexRenderingSettings
import nl.hannahsten.texifyidea.psi.LatexCommands

/**
 * Renders `\sjm{Sono appena arrivato.}` as `Jacques: «Sono appena arrivato.»`
 * without painting anything.
 *
 * The text to show is already the text in the source, so it is left alone: only
 * `\sjm{` and the closing brace are folded away behind placeholders. The body
 * between them stays live text, which is why the caret, selection, completion
 * and the spellchecker all keep working inside a line of dialogue.
 *
 * The regions are created here rather than by a FoldingBuilder. A builder's
 * descriptors are a CachedValue on the PsiFile: with rendering switched off it
 * returns nothing, that empty result carries no dependency able to invalidate
 * it, and switching back on never re-runs it. A highlighting pass has no cache
 * and the daemon restart already re-runs it, which is how the three other
 * rendered commands have always worked.
 *
 * Neither fold can be expanded. There is nothing under them the reader wants,
 * and a click that expanded one of the two would show a source that does not
 * exist. To see the command whole, turn smart rendering off.
 */
class DialogueRenderPass(
    private val psiFile: PsiFile,
    private val editor: Editor,
) : TextEditorHighlightingPass(psiFile.project, editor.document) {

    private data class Fold(val startOffset: Int, val endOffset: Int, val placeholder: String)

    private var folds: List<Fold> = emptyList()

    override fun doCollectInformation(progress: ProgressIndicator) {
        if (!BookLatexRenderingSettings.getInstance().smartRendering) {
            folds = emptyList()
            return
        }

        val definitions = DialogueDefinitions.getInstance(psiFile.project).definitions()
        folds = PsiTreeUtil.findChildrenOfType(psiFile, LatexCommands::class.java)
            .flatMap { command -> command.toFolds(definitions) }
    }

    private fun LatexCommands.toFolds(definitions: Map<String, DialogueDefinition>): List<Fold> {
        val definition = definitions[name] ?: return emptyList()
        val argument = parameterList.firstOrNull { it.requiredParam != null }?.textRange ?: return emptyList()

        // The argument range carries its braces: `{` closes the opening fold and
        // `}` is the closing one.
        val openingEnd = argument.startOffset + 1
        val closingStart = argument.endOffset - 1

        // An empty body would leave the two folds touching.
        if (openingEnd >= closingStart) return emptyList()

        return listOf(
            Fold(textRange.startOffset, openingEnd, "${definition.speaker}: ${definition.kind.openingQuote}"),
            Fold(closingStart, argument.endOffset, definition.kind.closingQuote),
        )
    }

    override fun doApplyInformationToEditor() {
        val foldingModel = editor.foldingModel as? FoldingModelEx ?: return
        val ours = editor.getUserData(OUR_REGIONS)
            ?: mutableListOf<FoldRegion>().also { editor.putUserData(OUR_REGIONS, it) }

        foldingModel.runBatchFoldingOperation {
            ours.filter { it.isValid }.forEach { foldingModel.removeFoldRegion(it) }
            ours.clear()

            folds.forEach { fold ->
                val region = foldingModel.createFoldRegion(
                    fold.startOffset,
                    fold.endOffset,
                    fold.placeholder,
                    null,
                    true,
                )
                if (region == null) {
                    thisLogger().warn("Refused dialogue fold at ${fold.startOffset}..${fold.endOffset}")
                    return@forEach
                }
                if (region.isExpanded) region.isExpanded = false
                ours += region
            }
        }
    }

    private companion object {
        val OUR_REGIONS: Key<MutableList<FoldRegion>> = Key.create("booklatex.dialogue.regions")
    }
}
