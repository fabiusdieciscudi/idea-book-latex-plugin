/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.attributes

import org.junit.Assert.assertEquals
import org.junit.Test

class AttributeArgumentTest {

    private val spec = StripSpecs.SCENE

    private fun render(values: Map<String, String>, preserved: Map<String, String> = emptyMap()) =
        AttributeArgument.render(spec, values, preserved, baseIndent = "")

    @Test
    fun `an empty value keeps its key out of the source`() {
        val text = render(mapOf("name" to "X", "setting" to "", "date" to "\\sceneToday", "time" to "alba"))
        assertEquals(
            """
            [
                name={X},
                date={\sceneToday},
                time={alba}
            ]
            """.trimIndent(),
            text,
        )
    }

    @Test
    fun `keys the plugin does not know are kept, after the ones it does`() {
        val text = render(mapOf("name" to "X"), preserved = mapOf("mood" to "cupo"))
        assertEquals(
            """
            [
                name={X},
                mood={cupo}
            ]
            """.trimIndent(),
            text,
        )
    }

    @Test
    fun `nothing left means no optional argument at all`() {
        assertEquals("", render(spec.keys.associateWith { "" }))
        assertEquals("", render(emptyMap()))
    }

    @Test
    fun `the group is indented under the command`() {
        val text = AttributeArgument.render(spec, mapOf("name" to "X"), emptyMap(), baseIndent = "  ")
        assertEquals("[\n      name={X}\n  ]", text)
    }

    @Test
    fun `values are trimmed`() {
        assertEquals("[\n    name={X}\n]", render(mapOf("name" to "  X  ")))
    }
}
