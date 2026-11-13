/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.mark

import name.fabiusdieciscudi.booklatex.comment.COMMENT_COMMAND_NAME
import name.fabiusdieciscudi.booklatex.markers.ALTERNATE_COMMAND_NAME
import name.fabiusdieciscudi.booklatex.markers.Languages
import name.fabiusdieciscudi.booklatex.markers.MarkerSpecs
import name.fabiusdieciscudi.booklatex.markers.SUPPRESSED_COMMAND_NAME
import name.fabiusdieciscudi.booklatex.markers.WrapSpecs

/**
 * A command Mark As puts in, and Unmark As takes out: its name, and the bundle
 * key of the label both menus show. A language's name entry is built from two
 * keys, `language.nameEntry` around `language.french`; [nameKey] is the inner one.
 */
data class MarkableCommand(val commandName: String, val labelKey: String, val nameKey: String? = null)

object MarkableCommands {

    val ALL: List<MarkableCommand> = listOf(
        MarkableCommand(SUPPRESSED_COMMAND_NAME, "action.BookLaTeX.MarkAs.Suppressed.text"),
        MarkableCommand(ALTERNATE_COMMAND_NAME, "action.BookLaTeX.MarkAs.Alternate.text"),
        MarkableCommand(COMMENT_COMMAND_NAME, "action.BookLaTeX.MarkAs.Comment.text"),
        MarkableCommand(MarkerSpecs.NOTE.commandName, "action.BookLaTeX.MarkAs.Note.text"),
        MarkableCommand(MarkerSpecs.NEWSPAPER_NAME.commandName, "action.BookLaTeX.MarkAs.Newspaper.text"),
        MarkableCommand(MarkerSpecs.WORK_OF_ART_NAME.commandName, "action.BookLaTeX.MarkAs.WorkOfArt.text"),
        MarkableCommand(MarkerSpecs.ACRONYM.commandName, "action.BookLaTeX.MarkAs.Acronym.text"),
        MarkableCommand(WrapSpecs.SINGLE_QUOTE.commandName, "action.BookLaTeX.MarkAs.StraightQuotation.text"),
    ) + Languages.ALL.flatMap { language ->
        listOf(
            MarkableCommand(language.proseCommand, "language.${language.name}"),
            MarkableCommand(language.nameCommand, "language.nameEntry", nameKey = "language.${language.name}"),
        )
    }

    fun forCommand(commandName: String?): MarkableCommand? = ALL.firstOrNull { it.commandName == commandName }
}

/** Replace [start]..[end] with [replacement]. */
data class Unmarking(val start: Int, val end: Int, val replacement: String)

/**
 * Takes a command away and leaves what it held: `\newspapername{Il Secolo}`
 * becomes `Il Secolo`. [commandStart]..[commandEnd] is the whole command, its
 * options included; [contentStart]..[contentEnd] the text between its braces.
 *
 * With [dropPercent], a `%` right after the command goes too, when nothing but
 * whitespace follows it on its line: the one the save hook put there for a
 * command that owned its lines. Left behind, it would join the prose to the
 * next line in the book.
 */
fun unmarking(
    text: CharSequence,
    commandStart: Int,
    commandEnd: Int,
    contentStart: Int,
    contentEnd: Int,
    dropPercent: Boolean,
): Unmarking {
    var end = commandEnd
    if (dropPercent && end < text.length && text[end] == '%') {
        var index = end + 1
        while (index < text.length && text[index] != '\n' && text[index].isWhitespace()) index++
        if (index == text.length || text[index] == '\n') end++
    }
    return Unmarking(commandStart, end, text.subSequence(contentStart, contentEnd).toString())
}
