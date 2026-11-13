/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.sound

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import nl.hannahsten.texifyidea.psi.LatexCommands

/** Where the PSI meets [SoundArguments]: finding sound commands, and reading them. */
object SoundCommands {

    /** Every `\ambient` and `\sfx` in [psiFile], in the order of the file. */
    fun all(psiFile: PsiFile): List<LatexCommands> =
        PsiTreeUtil.findChildrenOfType(psiFile, LatexCommands::class.java)
            .filter { SoundSpecs.forCommand(it.name) != null }
            .sortedBy { it.textRange.startOffset }

    /** What [command] says, or `null` while it has no `{name}` yet: half-typed, it is left as source. */
    fun read(command: LatexCommands, spec: SoundSpec): SoundSettings? {
        val name = command.requiredParameterText(0) ?: return null
        val optional = command.parameterList.firstOrNull { it.optionalParam != null }?.text
        return SoundArguments.read(spec, name, optional)
    }

    /**
     * The names already given to [spec]'s command anywhere in the project, for the
     * dialog to offer. The plugin cannot see BookAssistant's library; the
     * manuscript is the next best thing. While the project is being indexed only
     * [current] is looked at.
     */
    fun namesUsed(project: Project, spec: SoundSpec, current: PsiFile): List<String> =
        ReadAction.compute<List<String>, RuntimeException> {
            val files = try {
                FilenameIndex.getAllFilesByExt(project, TEX_EXTENSION, GlobalSearchScope.projectScope(project))
                    .mapNotNull { PsiManager.getInstance(project).findFile(it) }
            } catch (e: IndexNotReadyException) {
                emptyList()
            }

            (files + current).distinct()
                .flatMap { file -> PsiTreeUtil.findChildrenOfType(file, LatexCommands::class.java) }
                .filter { it.name == spec.commandName }
                .mapNotNull { it.requiredParameterText(0)?.trim() }
                .filter { it.isNotEmpty() && it != spec.stopName }
                .distinct()
                .sorted()
        }

    private const val TEX_EXTENSION = "tex"
}
