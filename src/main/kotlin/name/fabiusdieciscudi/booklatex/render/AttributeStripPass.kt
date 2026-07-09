/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.render

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.CustomFoldRegion
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.FoldingModelEx
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import name.fabiusdieciscudi.booklatex.attributes.CommandAttributes
import name.fabiusdieciscudi.booklatex.attributes.InheritedAttributes
import name.fabiusdieciscudi.booklatex.attributes.StripSpec
import name.fabiusdieciscudi.booklatex.attributes.StripSpecs
import name.fabiusdieciscudi.booklatex.document.ownsItsLines
import name.fabiusdieciscudi.booklatex.settings.BookLatexRenderingSettings
import nl.hannahsten.texifyidea.psi.LatexCommands

/** Replaces every rendered command that owns its lines with its strip. */
class AttributeStripPass(
    private val psiFile: PsiFile,
    private val editor: Editor,
) : TextEditorHighlightingPass(psiFile.project, editor.document) {

    private data class Strip(
        val startLine: Int,
        val endLine: Int,
        val spec: StripSpec,
        val values: Map<String, String>,
        /** Keys whose value was not written here, but taken from an earlier command. */
        val inherited: Set<String>,
    )

    private var strips: List<Strip> = emptyList()

    override fun doCollectInformation(progress: ProgressIndicator) {
        if (!BookLatexRenderingSettings.getInstance().smartRendering) {
            strips = emptyList()
            return
        }

        val document = editor.document
        // Inheritance reads backwards, so the order has to be the file's.
        val commands = PsiTreeUtil.findChildrenOfType(psiFile, LatexCommands::class.java)
            .sortedBy { it.textRange.startOffset }

        strips = commands.mapNotNull { command ->
            val spec = StripSpecs.forCommand(command.name) ?: return@mapNotNull null
            command.toStrip(document, spec, commands)
        }
    }

    private fun LatexCommands.toStrip(document: Document, spec: StripSpec, commands: List<LatexCommands>): Strip? {
        val range = textRange
        if (range.endOffset > document.textLength) return null

        val startLine = document.getLineNumber(range.startOffset)
        val endLine = document.getLineNumber(range.endOffset)

        val before = document.getText(TextRange(document.getLineStartOffset(startLine), range.startOffset))
        val after = document.getText(TextRange(range.endOffset, document.getLineEndOffset(endLine)))
        if (!ownsItsLines(before, after)) return null

        val written = CommandAttributes.read(this, spec)
        val fallback = InheritedAttributes.before(commands, spec, textRange.startOffset)
        val (values, inherited) = InheritedAttributes.applyTo(written, fallback, spec)

        return Strip(startLine, endLine, spec, values, inherited)
    }

    override fun doApplyInformationToEditor() {
        val foldingModel = editor.foldingModel as? FoldingModelEx ?: return

        foldingModel.runBatchFoldingOperation {
            foldingModel.allFoldRegions
                .filterIsInstance<CustomFoldRegion>()
                .filter { it.renderer is AttributeStripRenderer }
                .forEach { foldingModel.removeFoldRegion(it) }

            strips.forEach { strip ->
                val region = foldingModel.addCustomLinesFolding(
                    strip.startLine,
                    strip.endLine,
                    AttributeStripRenderer(strip.spec, strip.values, strip.inherited),
                )
                if (region == null) {
                    thisLogger().warn(
                        "Refused ${strip.spec.commandName} strip for lines ${strip.startLine}..${strip.endLine}"
                    )
                }
            }
        }
    }
}
