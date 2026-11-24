package com.example

import com.example.core.interaction.CameraMotionPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * v1.6 — the perspective movement itself, verified by projecting a fixed
 * north-wall point (1800, 1250, 0) through the exact Viewport3D camera math
 * (yaw 35°, pitch 30°): the point sits at (678.9, 830.9) px. A correct drag
 * moves it WITH the finger: drag right → 690.0 px, drag down → 839.2 px.
 */
class CameraMotionPolicyTest {

    @Test
    fun testOrbitDragFollowsTheFinger() {
        val (dyaw, dpitch) = CameraMotionPolicy.orbitDeltaForDrag(10f, 10f)
        // Drag right (+x) must yaw + (scene moves right); drag down (+y)
        // must pitch − (scene moves down). The old signs were the exact
        // opposite on both axes.
        assertEquals(3f, dyaw, 0.001f)
        assertEquals(-3f, dpitch, 0.001f)
        val (dyawLeft, dpitchUp) = CameraMotionPolicy.orbitDeltaForDrag(-10f, -10f)
        assertEquals(-3f, dyawLeft, 0.001f)
        assertEquals(3f, dpitchUp, 0.001f)
    }

    @Test
    fun testWalkLookDragRightTurnsRight() {
        val (dyaw, dpitch) = CameraMotionPolicy.walkLookDeltaForDrag(10f, 10f)
        assertEquals(-2.8f, dyaw, 0.001f)
        assertEquals(2.8f, dpitch, 0.001f)
    }

    @Test
    fun testTwoFingerPanFollowsTheFinger() {
        // X was inverted (a +X target shift moves the scene left on screen);
        // Y already followed the finger and is unchanged.
        val (dx, dy) = CameraMotionPolicy.panDeltaForDrag(10f, 10f)
        assertEquals(-10f, dx, 0.001f)
        assertEquals(10f, dy, 0.001f)
    }

    @Test
    fun testFlingVelocityIsCapped() {
        // A 5°/frame flick used to spin the camera ~69.5° after release.
        // Capped at 1.5°/frame with 0.88 decay, any flick adds ≤ ~11.4°.
        val (cx, cy) = CameraMotionPolicy.cappedFlingVelocity(5f, 0f)
        assertEquals(1.5f, cx, 0.001f)
        assertEquals(0f, cy, 0.001f)
        val (dx, dy) = CameraMotionPolicy.cappedFlingVelocity(3f, 4f)
        assertEquals(0.9f, dx, 0.001f)
        assertEquals(1.2f, dy, 0.001f)
        val (sx, sy) = CameraMotionPolicy.cappedFlingVelocity(0.5f, 0.5f)
        assertEquals(0.5f, sx, 0.001f)
        assertEquals(0.5f, sy, 0.001f)
    }
}
