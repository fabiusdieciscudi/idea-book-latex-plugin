/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.sound

/** One item of the `[...]` group, as written: `loudness=-3`, or a bare `overlay`. */
data class SoundOption(val key: String, val value: String?) {
    val text: String get() = if (value == null) key else "$key=$value"
}

/**
 * Everything a sound command says, read from its source or from the dialog.
 *
 * [numbers] and [switches] are keyed by the parameter's full name, whatever the
 * source called it; a number that is absent or blank is left out. [preserved]
 * are the options the spec does not know, kept as written, in order.
 */
data class SoundSettings(
    val name: String,
    val numbers: Map<String, String> = emptyMap(),
    val switches: Map<String, Boolean> = emptyMap(),
    val preserved: List<SoundOption> = emptyList(),
) {
    fun isStop(spec: SoundSpec): Boolean = spec.stopName != null && name.trim() == spec.stopName

    fun switch(parameter: SwitchParameter): Boolean = switches[parameter.key] ?: parameter.default

    companion object {
        fun empty(): SoundSettings = SoundSettings(name = "")
    }
}

/**
 * Reads the `[...]` of a sound command, and writes the whole command back.
 *
 * Written on one line, options in the order of the spec and by their full
 * names -- `l=-3` comes back as `loudness=-3` -- without braces around the
 * values, which are numbers: `\sfx[overlay, loudness=-3]{tuono}`. An `\sfx`
 * may sit inside a sentence, and a command spread over lines would break it.
 */
object SoundArguments {

    /** The options of [optional], the text between the square brackets (or with them). */
    fun parse(optional: String): List<SoundOption> {
        val inner = optional.trim().removePrefix("[").removeSuffix("]")
        return splitTopLevel(inner).mapNotNull { item ->
            val trimmed = item.trim()
            if (trimmed.isEmpty()) return@mapNotNull null
            val equals = trimmed.indexOf('=')
            if (equals < 0) SoundOption(trimmed, null)
            else SoundOption(trimmed.substring(0, equals).trim(), unbrace(trimmed.substring(equals + 1).trim()))
        }
    }

    fun read(spec: SoundSpec, name: String, optional: String?): SoundSettings {
        val numbers = mutableMapOf<String, String>()
        val switches = mutableMapOf<String, Boolean>()
        val preserved = mutableListOf<SoundOption>()

        for (option in optional?.let { parse(it) }.orEmpty()) {
            when (val parameter = spec.parameter(option.key)) {
                is NumberParameter -> numbers[parameter.key] = option.value.orEmpty()
                is SwitchParameter -> {
                    val value = switchValue(option.value)
                    if (value != null) switches[parameter.key] = value else preserved += option
                }
                null -> preserved += option
            }
        }
        return SoundSettings(name.trim(), numbers, switches, preserved)
    }

    fun render(spec: SoundSpec, settings: SoundSettings): String {
        val written = spec.parameters.mapNotNull { parameter ->
            when (parameter) {
                is NumberParameter -> settings.numbers[parameter.key]?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { "${parameter.key}=$it" }
                is SwitchParameter -> settings.switch(parameter)
                    .takeIf { it != parameter.default }
                    ?.let { on -> if (on) parameter.key else "${parameter.key}=off" }
            }
        } + settings.preserved.map { it.text }

        val options = if (written.isEmpty()) "" else written.joinToString(", ", "[", "]")
        return "${spec.commandName}$options{${settings.name.trim()}}"
    }

    private fun switchValue(value: String?): Boolean? = when (value?.trim()?.lowercase()) {
        null, "true", "on" -> true
        "false", "off" -> false
        else -> null
    }

    private fun unbrace(value: String): String =
        if (value.startsWith("{") && value.endsWith("}")) value.substring(1, value.length - 1).trim() else value

    /** Splits at the commas that are not inside braces. */
    private fun splitTopLevel(text: String): List<String> {
        val items = mutableListOf<String>()
        var depth = 0
        var start = 0
        text.forEachIndexed { index, char ->
            when (char) {
                '{' -> depth++
                '}' -> depth--
                ',' -> if (depth == 0) {
                    items += text.substring(start, index)
                    start = index + 1
                }
            }
        }
        items += text.substring(start)
        return items
    }
}

/** A piece of a strip or a pill: a bold label, a value, or both. */
data class SoundField(val label: String?, val value: String?)

/**
 * What a strip or a pill shows after the title: the sound's name, then every
 * option with a value, in the order of the spec. Options without one are left
 * out, unlike a scene's, which shows a dash: a sound has a handful of options
 * and most are seldom set.
 */
object SoundDisplay {

    fun fields(spec: SoundSpec, settings: SoundSettings): List<SoundField> {
        if (settings.isStop(spec)) return listOf(SoundField(null, STOP))

        val options = spec.parameters.mapNotNull { parameter ->
            when (parameter) {
                is NumberParameter -> settings.numbers[parameter.key]?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { SoundField(parameter.key.uppercase(), "$it ${parameter.unit}") }
                is SwitchParameter -> settings.switch(parameter)
                    .takeIf { it != parameter.default }
                    ?.let { on -> SoundField(if (on) parameter.key.uppercase() else "NO ${parameter.key.uppercase()}", null) }
            }
        } + settings.preserved.map { SoundField(it.key.uppercase(), it.value) }

        return listOf(SoundField(null, settings.name.trim())) + options
    }

    /** How `\ambient{off}` reads. */
    const val STOP = "stop"
}
