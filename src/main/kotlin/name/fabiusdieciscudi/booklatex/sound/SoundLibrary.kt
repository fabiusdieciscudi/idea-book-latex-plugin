/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.sound

import java.io.File
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths

/**
 * The file of the sounds BookAssistant knows, named in *Settings | Tools | Book
 * LaTeX* by a path relative to the project's root: the dialog of `\ambient` and `\sfx` offers its names, and nothing else
 * (see [SoundCommands.libraryNames]).
 *
 * One sound per line, its name before the first colon:
 *
 * ```
 * # Backgrounds
 * pioggia: rain_loop.wav
 * vento forte: wind_02.wav   # spaces are fine in a name
 * ```
 *
 * A `#` starts a comment to the end of the line. Lines without a colon, or with
 * nothing before it, are skipped. The names come in the order of the file, each
 * once.
 */
object SoundLibrary {

    fun parse(text: String): List<String> =
        text.lineSequence()
            .map { it.substringBefore(COMMENT) }
            .filter { SEPARATOR in it }
            .map { it.substringBefore(SEPARATOR).trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()

    /**
     * The library as a path: `~` stands for the home directory, and a relative
     * path is taken from [basePath], the project's root, so one setting serves
     * every manuscript that keeps its library in the same place.
     */
    fun resolve(setting: String, basePath: String?): Path? {
        val text = setting.trim()
        if (text.isEmpty()) return null
        return try {
            val expanded = if (text == "~" || text.startsWith("~/")) System.getProperty("user.home") + text.substring(1) else text
            val path = Paths.get(expanded)
            when {
                path.isAbsolute -> path
                basePath != null -> Paths.get(basePath).resolve(path)
                else -> null
            }
        } catch (e: InvalidPathException) {
            null
        }
    }

    /**
     * The library as the settings keep it: relative to [basePath], the project's
     * root, with forward slashes whatever the system, so the same setting finds
     * the library in every manuscript laid out alike. What cannot be made
     * relative -- no project, another drive -- is kept as it was given.
     */
    fun relativize(setting: String, basePath: String?): String {
        val text = setting.trim()
        if (basePath == null) return text
        val path = resolve(text, basePath) ?: return text
        return try {
            Paths.get(basePath).normalize().relativize(path.normalize()).toString().replace(File.separatorChar, '/')
        } catch (e: IllegalArgumentException) {
            text
        } catch (e: InvalidPathException) {
            text
        }
    }

    private const val SEPARATOR = ':'
    private const val COMMENT = '#'
}
