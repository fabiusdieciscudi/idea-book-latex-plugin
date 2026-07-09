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
import name.fabiusdieciscudi.booklatex.comment.COMMENT_COMMAND_NAME
import name.fabiusdieciscudi.booklatex.document.ownsItsLines
import name.fabiusdieciscudi.booklatex.settings.BookLatexRenderingSettings
import nl.hannahsten.texifyidea.psi.LatexCommands

/**
 * Collects the `\comment{...}` commands that own the lines they sit on and
 * replaces them with custom fold regions painted by [CommentFoldRenderer].
 *
 * Custom fold regions are line-based, hence the "own lines" restriction: a
 * `\comment` in the middle of a sentence is left as plain source.
 */
class CommentRenderPass(
    private val psiFile: PsiFile,
    private val editor: Editor,
) : TextEditorHighlightingPass(psiFile.project, editor.document) {

    private data class Block(val startLine: Int, val endLine: Int, val text: String)

    private var blocks: List<Block> = emptyList()

    override fun doCollectInformation(progress: ProgressIndicator) {
        if (!BookLatexRenderingSettings.getInstance().smartRendering) {
            blocks = emptyList()
            return
        }

        val document = editor.document
        blocks = PsiTreeUtil.findChildrenOfType(psiFile, LatexCommands::class.java)
            .filter { it.name == COMMENT_COMMAND_NAME }
            .mapNotNull { command -> command.toBlock(document) }
    }

    private fun LatexCommands.toBlock(document: Document): Block? {
        val range = textRange
        if (range.endOffset > document.textLength) return null

        val startLine = document.getLineNumber(range.startOffset)
        val endLine = document.getLineNumber(range.endOffset)

        val before = document.getText(TextRange(document.getLineStartOffset(startLine), range.startOffset))
        val after = document.getText(TextRange(range.endOffset, document.getLineEndOffset(endLine)))
        if (!ownsItsLines(before, after)) return null

        val raw = requiredParameterText(0)
        if (raw == null) {
            thisLogger().warn("No required argument on $COMMENT_COMMAND_NAME at offset ${range.startOffset}")
            return null
        }

        // The body may now span several lines; the note paints a single one.
        val body = raw.replace(WHITESPACE, " ").trim()
        if (body.isEmpty()) return null

        return Block(startLine, endLine, body)
    }

    override fun doApplyInformationToEditor() {
        val foldingModel = editor.foldingModel as? FoldingModelEx ?: return

        foldingModel.runBatchFoldingOperation {
            // Drop only the regions we own; other folding builders keep theirs.
            foldingModel.allFoldRegions
                .filterIsInstance<CustomFoldRegion>()
                .filter { it.renderer is CommentFoldRenderer }
                .forEach { foldingModel.removeFoldRegion(it) }

            blocks.forEach { block ->
                val region = foldingModel.addCustomLinesFolding(
                    block.startLine,
                    block.endLine,
                    CommentFoldRenderer(block.text),
                )
                if (region == null) {
                    thisLogger().warn("Refused fold region for lines ${block.startLine}..${block.endLine}")
                }
            }
        }
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
