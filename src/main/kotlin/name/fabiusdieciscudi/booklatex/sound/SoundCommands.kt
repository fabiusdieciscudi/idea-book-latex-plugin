/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.sound

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import name.fabiusdieciscudi.booklatex.settings.BookLatexRenderingSettings
import nl.hannahsten.texifyidea.psi.LatexCommands
import java.io.IOException
import java.nio.file.Files

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
     * The names of the sounds in the library file of the settings, for the
     * dialog to offer: the same list for `\ambient` and `\sfx`, since both draw
     * on BookAssistant's one library. Read afresh each time, so an edit to the
     * file shows the next time the dialog opens. No library, or one that cannot
     * be read, offers nothing, and the name is typed by hand.
     */
    fun libraryNames(project: Project): List<String> {
        val setting = BookLatexRenderingSettings.getInstance().soundLibraryPath
        val path = SoundLibrary.resolve(setting, project.basePath) ?: return emptyList()
        return try {
            SoundLibrary.parse(Files.readString(path))
        } catch (e: IOException) {
            thisLogger().warn("BookLaTeX: cannot read the sound library at $path", e)
            emptyList()
        }
    }
}
