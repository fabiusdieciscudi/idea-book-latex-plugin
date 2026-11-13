/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex

import name.fabiusdieciscudi.booklatex.markers.Languages
import name.fabiusdieciscudi.booklatex.sound.SoundSpecs
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ResourceBundle

/**
 * A missing key throws only when the string is about to be shown, which is
 * usually in front of the user. This asks the bundle for them all, up front.
 */
class BookLatexBundleTest {

    private val bundle = ResourceBundle.getBundle("messages.BookLatexBundle")

    private val required = listOf(
        "settings.displayName",
        "settings.colorScheme.enabled",
        "settings.colorScheme.label",
        "settings.renderFont.label",
        "dialog.comment.title",
        "dialog.attributes.title",
        "dialog.attributes.field",
        "command.editComment",
        "command.editAttributes",
        "command.insertSpeaker",
        "command.markAs",
        "command.editSound",
        "dialog.sound.name",
        "dialog.sound.stop",
        "dialog.sound.range",
        "dialog.sound.nameRequired",
        "dialog.sound.nameInvalid",
        "dialog.sound.notANumber",
        "action.BookLaTeX.Insert.Ambient.text",
        "action.BookLaTeX.Insert.Sfx.text",
        "popup.speaker.title",
        "popup.speaker.item",
        "popup.speaker.literal",
        "progress.scanDefinitions",
        "action.smartRendering.text",
        "action.smartRendering.description",
        "action.BookLaTeX.ReloadDialogueDefinitions.text",
        "action.BookLaTeX.ReloadDialogueDefinitions.description",
        "group.BookLaTeX.MarkAs.text",
        "group.BookLaTeX.MarkAs.Languages.text",
        "language.nameEntry",
        "action.BookLaTeX.MarkAs.Suppressed.text",
        "action.BookLaTeX.MarkAs.Alternate.text",
        "action.BookLaTeX.MarkAs.Comment.text",
        "action.BookLaTeX.MarkAs.Note.text",
        "action.BookLaTeX.MarkAs.Newspaper.text",
        "action.BookLaTeX.MarkAs.WorkOfArt.text",
        "action.BookLaTeX.MarkAs.Acronym.text",
    )

    @Test
    fun `every key the plugin asks for is defined`() {
        val missing = required.filterNot { bundle.containsKey(it) }
        assertTrue("missing keys: $missing", missing.isEmpty())
    }

    @Test
    fun `every language has a name for the Mark As menu`() {
        val missing = Languages.ALL.map { "language.${it.name}" }.filterNot { bundle.containsKey(it) }
        assertTrue("missing keys: $missing", missing.isEmpty())
    }

    @Test
    fun `every sound has a title and every option a label`() {
        val keys = SoundSpecs.ALL.flatMap { spec ->
            listOf("dialog.sound.title.${spec.title.lowercase()}") + spec.parameters.map { "dialog.sound.${it.key}" }
        }
        val missing = keys.filterNot { bundle.containsKey(it) }
        assertTrue("missing keys: $missing", missing.isEmpty())
    }

    @Test
    fun `the keys that take arguments have placeholders`() {
        listOf("dialog.attributes.title", "command.editAttributes", "command.insertSpeaker", "command.markAs", "language.nameEntry", "command.editSound")
            .forEach { assertTrue(it, bundle.getString(it).contains("{0}")) }
        assertTrue(bundle.getString("popup.speaker.item").contains("{1}"))
    }
}
