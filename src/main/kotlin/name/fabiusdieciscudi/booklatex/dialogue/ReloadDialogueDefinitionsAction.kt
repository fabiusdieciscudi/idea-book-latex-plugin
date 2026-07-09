/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.dialogue

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressManager
import name.fabiusdieciscudi.booklatex.BookLatexBundle

/**
 * Rescans the preamble after it changes, and writes what it found to the log.
 */
class ReloadDialogueDefinitionsAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val log = thisLogger()

        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            {
                val definitions = ApplicationManager.getApplication()
                    .runReadAction<Map<String, DialogueDefinition>> {
                        DialogueDefinitions.getInstance(project).reload()
                    }

                log.warn("BookLaTeX: ${definitions.size} dialogue definitions in '${project.name}'")
                definitions.values
                    .sortedBy { it.command }
                    .forEach { log.warn("BookLaTeX: ${it.command} -> ${it.kind.name.lowercase()} -> ${it.speaker}") }

                val unresolved = definitions.values.filter { it.speaker.contains('\\') }
                if (unresolved.isNotEmpty()) {
                    log.warn("BookLaTeX: speakers still holding a macro: ${unresolved.map { it.speaker }}")
                }
            },
            BookLatexBundle.message("progress.scanDefinitions"),
            true,
            project,
        )

        DaemonCodeAnalyzer.getInstance(project).restart()
    }
}
