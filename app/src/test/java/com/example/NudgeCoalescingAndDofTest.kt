package com.example

import com.example.core.command.Command
import com.example.core.command.CommandCategory
import com.example.core.command.CommandHistory
import com.example.core.filament.DepthOfFieldConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v1.13 regression tests — the two v1.13 mechanics that used to be wrong
 * in ways only visible on the phone:
 *  - CommandHistory.replaceTop / peekTop (nudge coalescing): ten nudge
 *    taps used to push ten undo entries; a burst now merges into one entry
 *    whose undo restores the pose from BEFORE the first tap.
 *  - DepthOfFieldConfig.blurAmount (DoF in the export): the aperture used
 *    to be stored but never reach the render; f/2.8 must blur, f/8+ must not.
 */
class NudgeCoalescingAndDofTest {

    private class FakeCommand(val id: String) : Command {
        var executed = false
        var undone = false
        override val title: String = id
        override val description: String = ""
        override val category: CommandCategory = CommandCategory.FLOORPLAN_TRANSFORM
        override fun execute() {
            executed = true
        }

        override fun undo() {
            undone = true
        }
    }

    // --- CommandHistory.replaceTop / peekTop (nudge coalescing) ---

    @Test
    fun replaceTopSwapsTheTopEntry() {
        val history = CommandHistory()
        val first = FakeCommand("first")
        val second = FakeCommand("second")
        history.execute(first)
        assertEquals(first, history.peekTop())
        assertTrue(history.replaceTop(second))
        assertEquals(second, history.peekTop())
        // Undo reverts the REPLACEMENT, not the original.
        assertTrue(history.undo())
        assertTrue(second.undone)
        assertFalse(first.undone)
    }

    @Test
    fun replaceTopOnEmptyStackReturnsFalse() {
        val history = CommandHistory()
        assertFalse(history.replaceTop(FakeCommand("x")))
        assertEquals(null, history.peekTop())
    }

    @Test
    fun peekTopFollowsUndo() {
        val history = CommandHistory()
        val first = FakeCommand("first")
        val second = FakeCommand("second")
        history.execute(first)
        history.execute(second)
        history.undo()
        assertEquals(first, history.peekTop())
    }

    // --- DepthOfFieldConfig.exportBlurRadiusPx (DoF in the export) ---
    // blurAmount() = (aperture*0.7 + focus*0.3) * bokeh, so with the default
    // focus 2400 mm / bokeh 0.5: f/2.8 -> 0.336, f/8 -> 0.06. The export
    // radius is aperture-only: f/8+ is exactly 0.

    @Test
    fun fastApertureBlursStronglyAt4K() {
        assertEquals(12f, DepthOfFieldConfig(apertureFStop = 1.4f).exportBlurRadiusPx(3840), 0.01f)
        assertEquals(9.45f, DepthOfFieldConfig(apertureFStop = 2.8f).exportBlurRadiusPx(3840), 0.05f)
    }

    @Test
    fun slowApertureDoesNotBlur() {
        assertEquals(0f, DepthOfFieldConfig(apertureFStop = 8f).exportBlurRadiusPx(3840), 0.001f)
        assertEquals(0f, DepthOfFieldConfig(apertureFStop = 16f).exportBlurRadiusPx(3840), 0.001f)
    }

    @Test
    fun midApertureBlursPartially() {
        assertEquals(4.36f, DepthOfFieldConfig(apertureFStop = 5.6f).exportBlurRadiusPx(3840), 0.05f)
    }

    @Test
    fun radiusScalesWithWidth() {
        assertEquals(4.73f, DepthOfFieldConfig(apertureFStop = 2.8f).exportBlurRadiusPx(1920), 0.05f)
    }

    @Test
    fun blurAmountMatchesItsFormula() {
        // focus 0 isolates the aperture term: (0.788*0.7) * 0.5 = 0.276.
        assertEquals(0.276f, DepthOfFieldConfig(apertureFStop = 2.8f, focusDistanceMm = 0f).blurAmount(), 0.005f)
        assertEquals(0f, DepthOfFieldConfig(apertureFStop = 8f, focusDistanceMm = 0f).blurAmount(), 0.001f)
    }

    @Test
    fun shallowFlagMatchesAperture() {
        assertTrue(DepthOfFieldConfig(apertureFStop = 2.8f).isShallow)
        assertFalse(DepthOfFieldConfig(apertureFStop = 5.6f).isShallow)
    }
}
