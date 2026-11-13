/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.markers

import org.junit.Assert.assertEquals
import org.junit.Test

class DashesTest {

    @Test
    fun `two hyphens are an en dash, three an em dash`() {
        assertEquals(listOf(Dash(5, 7, EN_DASH)), dashes("pagg.--12"))
        assertEquals(listOf(Dash(5, 8, EM_DASH)), dashes("disse---e"))
    }

    @Test
    fun `a single hyphen is left as it is`() {
        assertEquals(emptyList<Dash>(), dashes("sotto-voce"))
        assertEquals(emptyList<Dash>(), dashes("nessun trattino"))
    }

    @Test
    fun `longer runs are read three at a time, as TeX reads them`() {
        assertEquals(listOf(Dash(0, 3, EM_DASH)), dashes("----"))
        assertEquals(listOf(Dash(0, 3, EM_DASH), Dash(3, 5, EN_DASH)), dashes("-----"))
        assertEquals(listOf(Dash(0, 3, EM_DASH), Dash(3, 6, EM_DASH)), dashes("------"))
    }

    @Test
    fun `several runs in one word`() {
        assertEquals(listOf(Dash(1, 3, EN_DASH), Dash(4, 7, EM_DASH)), dashes("a--b---c"))
    }
}
