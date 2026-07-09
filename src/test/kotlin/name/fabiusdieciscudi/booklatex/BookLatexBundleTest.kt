/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex

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
        "popup.speaker.title",
        "popup.speaker.item",
        "popup.speaker.literal",
        "progress.scanDefinitions",
        "action.smartRendering.text",
        "action.smartRendering.description",
        "action.BookLaTeX.ReloadDialogueDefinitions.text",
        "action.BookLaTeX.ReloadDialogueDefinitions.description",
    )

    @Test
    fun `every key the plugin asks for is defined`() {
        val missing = required.filterNot { bundle.containsKey(it) }
        assertTrue("missing keys: $missing", missing.isEmpty())
    }

    @Test
    fun `the keys that take arguments have placeholders`() {
        listOf("dialog.attributes.title", "command.editAttributes", "command.insertSpeaker")
            .forEach { assertTrue(it, bundle.getString(it).contains("{0}")) }
        assertTrue(bundle.getString("popup.speaker.item").contains("{1}"))
    }
}
