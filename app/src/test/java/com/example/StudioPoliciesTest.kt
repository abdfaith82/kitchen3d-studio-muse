package com.example

import com.example.core.interaction.BackPressAction
import com.example.core.interaction.BackPressPolicy
import com.example.core.interaction.CutawayPolicy
import com.example.core.interaction.ElementScalePolicy
import com.example.core.math.Vector3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v1.4 policies: scale clamps + back-button priority.
 */
class StudioPoliciesTest {

    @Test
    fun testScaleUpAndDownByTenPercent() {
        val dims = Vector3(600f, 900f, 500f)
        val up = ElementScalePolicy.scaledDimensions(dims, 1.10f)
        assertEquals(660f, up.x, 0.01f)
        assertEquals(990f, up.y, 0.01f)
        assertEquals(550f, up.z, 0.01f)
        val down = ElementScalePolicy.scaledDimensions(dims, 1f / 1.10f)
        assertEquals(550f, down.x, 0.01f)
        assertEquals(820f, down.y, 0.01f)
        assertEquals(450f, down.z, 0.01f)
    }

    @Test
    fun testScaleClampsToBuildableSizes() {
        val tiny = ElementScalePolicy.scaledDimensions(Vector3(160f, 160f, 160f), 0.5f)
        assertEquals(ElementScalePolicy.MIN_WIDTH_DEPTH_MM, tiny.x, 0.01f)
        assertEquals(ElementScalePolicy.MIN_HEIGHT_MM, tiny.y, 0.01f)
        val huge = ElementScalePolicy.scaledDimensions(Vector3(2900f, 2600f, 2900f), 2.0f)
        assertEquals(ElementScalePolicy.MAX_WIDTH_DEPTH_MM, huge.x, 0.01f)
        assertEquals(ElementScalePolicy.MAX_HEIGHT_MM, huge.y, 0.01f)
        // Extreme factors are rejected, never crash the render.
        val absurd = ElementScalePolicy.scaledDimensions(Vector3(600f, 900f, 500f), 99f)
        assertTrue(absurd.x <= ElementScalePolicy.MAX_WIDTH_DEPTH_MM)
    }

    @Test
    fun testScaleSnapsToTenMillimetres() {
        val scaled = ElementScalePolicy.scaledDimensions(Vector3(603f, 907f, 501f), 1.10f)
        assertEquals(0f, scaled.x % 10f, 0.01f)
        assertEquals(0f, scaled.y % 10f, 0.01f)
        assertEquals(0f, scaled.z % 10f, 0.01f)
    }

    @Test
    fun testScalePercentLabel() {
        assertEquals(100, ElementScalePolicy.scalePercent(Vector3(600f, 900f, 500f), Vector3(600f, 900f, 500f)))
        assertEquals(110, ElementScalePolicy.scalePercent(Vector3(660f, 990f, 550f), Vector3(600f, 900f, 500f)))
        assertEquals(100, ElementScalePolicy.scalePercent(Vector3(600f, 900f, 500f), Vector3(0f, 0f, 0f)))
    }

    @Test
    fun testBackButtonNeverExitsSilently() {
        assertEquals(BackPressAction.CLOSE_DIALOG, BackPressPolicy.nextAction(true, true, true))
        assertEquals(BackPressAction.CLEAR_SELECTION, BackPressPolicy.nextAction(false, true, true))
        assertEquals(BackPressAction.EXIT_CLEAN_CANVAS, BackPressPolicy.nextAction(false, false, true))
        assertEquals(BackPressAction.ASK_EXIT, BackPressPolicy.nextAction(false, false, false))
    }

    @Test
    fun testScaleButtonsDisableAtLimits() {
        assertFalse(ElementScalePolicy.canScaleUp(Vector3(3000f, 900f, 3000f)))
        assertTrue(ElementScalePolicy.canScaleUp(Vector3(600f, 900f, 500f)))
        assertFalse(ElementScalePolicy.canScaleDown(Vector3(150f, 150f, 150f)))
        assertTrue(ElementScalePolicy.canScaleDown(Vector3(600f, 900f, 500f)))
    }

    // v1.5 — the cutaway must fade smoothly, never pop: full wall at
    // dot 0.35+, gone at dot 0.05-, linear in between (dot 0.2 -> 0.5).
    @Test
    fun testCutawayFadesGraduallyInsteadOfPopping() {
        assertEquals(1f, CutawayPolicy.fadeAlpha(0.5f), 0.001f)
        assertEquals(1f, CutawayPolicy.fadeAlpha(0.35f), 0.001f)
        assertEquals(0.5f, CutawayPolicy.fadeAlpha(0.2f), 0.001f)
        assertEquals(0f, CutawayPolicy.fadeAlpha(0.05f), 0.001f)
        assertEquals(0f, CutawayPolicy.fadeAlpha(-0.4f), 0.001f)
        assertTrue(CutawayPolicy.isFullyCut(0.0f))
        assertFalse(CutawayPolicy.isFullyCut(0.2f))
    }
}
