/*
 * © Copyright 2026-present by Fabius Dieciscudi. Licensed under the MIT License, see LICENSE.
 *
 */

package name.fabiusdieciscudi.booklatex.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * What makes a `%` line a divider, and what its label is. The rule is on text
 * alone; where the divider lands on screen is the pass's, not tested here.
 */
class CommentRuleTest {

    private fun captionOf(line: String): String? {
        val rule = CommentRule.of(line)
        requireNotNull(rule) { "expected a rule for: $line" }
        return rule.caption
    }

    @Test
    fun `a run of percents is a bare rule`() {
        assertEquals(null, captionOf("%%%%%%"))
        assertEquals(null, captionOf("%%"))
    }

    @Test
    fun `percents and spaces are still a bare rule`() {
        assertEquals(null, captionOf("%% %% %%"))
        assertEquals(null, captionOf("   %%%%   "))
    }

    @Test
    fun `a flanked label becomes the caption`() {
        assertEquals("COMMENTO", captionOf("%%%%%% COMMENTO %%%%%%"))
    }

    @Test
    fun `the flanking spaces are optional`() {
        assertEquals("COMMENTO", captionOf("%%%%%%COMMENTO%%%%%%"))
    }

    @Test
    fun `a label keeps its words and its case, its spaces collapsed`() {
        assertEquals("Scena prima", captionOf("%%%%   Scena    prima   %%%%"))
    }

    @Test
    fun `leading indentation does not matter`() {
        assertEquals(null, captionOf("    %%%%%%"))
        assertEquals("X", captionOf("\t%%%% X %%%%"))
    }

    @Test
    fun `one percent and prose is an ordinary comment`() {
        assertNull(CommentRule.of("% una nota"))
        assertNull(CommentRule.of("%"))
    }

    @Test
    fun `two percents that never touch are not a rule`() {
        // The percents of a comment that happens to mention a percentage do not
        // form the run a divider is made of.
        assertNull(CommentRule.of("% sconto 50%"))
    }

    @Test
    fun `a label has to be walled off on both sides`() {
        // A run of percents and then prose, with nothing closing it, is a comment.
        assertNull(CommentRule.of("%% da rivedere"))
        assertNull(CommentRule.of("%%%% titolo"))
    }

    @Test
    fun `a line that is not a comment is never a rule`() {
        assertNull(CommentRule.of("testo %%%%"))
        assertNull(CommentRule.of("x = 1 %%%%"))
        assertNull(CommentRule.of(""))
        assertNull(CommentRule.of("   "))
    }

    @Test
    fun `an escaped percent does not open a comment`() {
        // The line starts with a backslash, not a percent, so it is prose.
        assertNull(CommentRule.of("\\%%%%"))
    }
}
