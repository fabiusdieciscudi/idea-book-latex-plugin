/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.language

import com.intellij.lang.PerFileMappingsBase
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * The language a document is written in: one of those the preamble's
 * `\booklang@set` accepts, since each brings its own typographic conventions --
 * the quotes of `\spoken` and `\sq`, the style of `\thought` and
 * `\newspapername`. The foreign languages marked inside the prose, `\french` and
 * the rest, are another matter: see [name.fabiusdieciscudi.booklatex.markers.Languages].
 *
 * [babelName] is what the preamble calls it, and what is stored.
 */
enum class DocumentLanguage(val babelName: String) {
    ITALIAN("italian"),
    FRENCH("french"),
    ENGLISH("english");

    /** The key of its name in the bundle, shared with the languages of Mark As. */
    val bundleKey: String get() = "language.$babelName"

    companion object {
        /** The preamble's own default, and so a file's until another is chosen. */
        val DEFAULT = ITALIAN
    }
}

/**
 * Which language each `.tex` file of the project is in, kept in
 * `.idea/booklatex.xml` -- with the project, so it travels with the manuscript
 * under version control.
 *
 * The platform's own per-file mappings, the ones that keep the encodings and the
 * SQL dialects: a file keeps its language when it is renamed or moved, and a
 * deletion is undone with its language. A file never chosen for is in
 * [DocumentLanguage.DEFAULT]; so every file has a language, written down or not.
 */
@Service(Service.Level.PROJECT)
@State(name = "BookLatexDocumentLanguages", storages = [Storage("booklatex.xml")])
class DocumentLanguageMappings(project: Project) : PerFileMappingsBase<DocumentLanguage>(project) {

    override fun getAvailableValues(): List<DocumentLanguage> = DocumentLanguage.entries

    override fun serialize(language: DocumentLanguage): String = language.babelName

    /** The language is the plugin's business, not the parser's. */
    override fun shouldReparseFiles(): Boolean = false

    fun languageOf(file: VirtualFile): DocumentLanguage = getMapping(file) ?: DocumentLanguage.DEFAULT

    fun setLanguage(file: VirtualFile, language: DocumentLanguage) = setMapping(file, language)

    companion object {
        fun getInstance(project: Project): DocumentLanguageMappings = project.service()
    }
}
