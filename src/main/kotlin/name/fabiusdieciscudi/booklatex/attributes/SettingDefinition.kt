/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.attributes

/**
 * One `\setting...` command the book defines for itself.
 *
 * Two faces of the same thing, and the whole point of the chooser: the author
 * picks by [label], which is the place as it will be printed, and the source
 * receives [command], which is what a chapter is supposed to contain.
 */
data class SettingDefinition(
    /** With its backslash: `\settingSpiaggiaAgay`. */
    val command: String,
    /** The expansion, flattened: `Spiaggia di Agay, Var`. */
    val label: String,
)
