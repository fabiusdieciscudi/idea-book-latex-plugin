/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.render

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.CustomFoldRegion
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.FoldingModelEx
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.psi.PsiFile
import name.fabiusdieciscudi.booklatex.settings.BookLatexRenderingSettings

/**
 * Finds the lines that are `%` dividers and folds each behind a rule.
 *
 * The work is on lines of text, not on the PSI: a divider is a property of a
 * whole line, and the plugin already reads comments straight from the document
 * elsewhere. Only lines that hold a `%` are looked at closely; [CommentRule]
 * decides the rest, and lines it turns down keep their source.
 *
 * Like the comment notes and the attribute strips, these are custom line
 * foldings, so they take a whole line and can be told apart from other builders'
 * regions by their renderer when the time comes to replace them.
 */
class CommentRulePass(
    private val psiFile: PsiFile,
    private val editor: Editor,
) : TextEditorHighlightingPass(psiFile.project, editor.document) {

    private data class Rule(val line: Int, val caption: String?)

    private var rules: List<Rule> = emptyList()

    override fun doCollectInformation(progress: ProgressIndicator) {
        if (!BookLatexRenderingSettings.getInstance().smartRendering) {
            rules = emptyList()
            return
        }

        val document = editor.document
        val chars = document.immutableCharSequence

        val found = ArrayList<Rule>()
        for (line in 0 until document.lineCount) {
            val start = document.getLineStartOffset(line)
            val end = document.getLineEndOffset(line)
            // A line with no % cannot be a rule; skip it without building a string.
            if (!hasPercent(chars, start, end)) continue

            val rule = CommentRule.of(chars.subSequence(start, end).toString()) ?: continue
            found += Rule(line, rule.caption)
        }
        rules = found
    }

    private fun hasPercent(chars: CharSequence, start: Int, end: Int): Boolean {
        for (index in start until end) if (chars[index] == '%') return true
        return false
    }

    override fun doApplyInformationToEditor() {
        val foldingModel = editor.foldingModel as? FoldingModelEx ?: return

        foldingModel.runBatchFoldingOperation {
            // Drop only the rules we own; other folding builders keep theirs.
            foldingModel.allFoldRegions
                .filterIsInstance<CustomFoldRegion>()
                .filter { it.renderer is HorizontalRuleRenderer }
                .forEach { foldingModel.removeFoldRegion(it) }

            rules.forEach { rule ->
                val region = foldingModel.addCustomLinesFolding(
                    rule.line,
                    rule.line,
                    HorizontalRuleRenderer(rule.caption),
                )
                if (region == null) {
                    thisLogger().warn("Refused comment-rule fold for line ${rule.line}")
                }
            }
        }
    }
}
