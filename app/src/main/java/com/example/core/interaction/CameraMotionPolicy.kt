package com.example.core.interaction

import kotlin.math.sqrt

/**
 * Camera Motion Policy v1.6 — pure, unit-testable rules for how the 3D
 * perspective moves under the finger.
 *
 * The v1.5 fix only smoothed the PANEL buttons; the main interaction — the
 * drag on the 3D view itself — was never touched, and it was wrong in three
 * ways (all verified by projecting a fixed north-wall point through the
 * exact Viewport3D camera math, see /tmp/cam_sim.py):
 *
 *  1. ORBIT DRAG WAS INVERTED ON BOTH AXES. Dragging right moved the scene
 *     left (wall point 678.9 → 667.6 px) and dragging down moved it up
 *     (830.9 → 822.8 px). The old mapping was dyaw = -pan.x*0.30,
 *     dpitch = +pan.y*0.30. A touch UI must be direct manipulation: the
 *     scene follows the finger. Fixed mapping: dyaw = +pan.x*0.30,
 *     dpitch = -pan.y*0.30 (verified: drag right → 690.0 px, drag down →
 *     839.2 px — the point follows the finger).
 *
 *  2. TWO-FINGER PAN X WAS INVERTED. A +100 target shift moves the scene
 *     left on screen (678.9 → 661.3 px) while Y already followed the finger
 *     (+100 → 842.2 px, down). So only X is negated here.
 *
 *  3. THE FLING KEPT SPINNING THE CAMERA 40–70° AFTER RELEASE. The inertia
 *     loop applied the per-gesture-event delta as a per-frame delta with a
 *     slow 0.93 decay: total extra rotation ≈ v / 0.07, i.e. 41° for a
 *     modest 3°/frame flick and 69.5° for 5°/frame. The velocity is now
 *     capped at 1.5°/frame with a 0.88 decay → at most ~11.4° of overshoot
 *     no matter how hard the flick is.
 *
 *  4. FIRST-PERSON LOOK YAW WAS INVERTED. Drag right increased walkLookYaw,
 *     which rotates the view toward +X — but facing south (+Z), +X is on
 *     the LEFT, so dragging right looked left. Fixed: dyaw = -pan.x*0.28.
 *     Pitch was already correct (drag down = look down) and is unchanged.
 */
object CameraMotionPolicy {
    const val ORBIT_DRAG_YAW_PER_PX = 0.30f
    const val ORBIT_DRAG_PITCH_PER_PX = 0.30f
    const val WALK_LOOK_YAW_PER_PX = 0.28f
    const val FLING_MAX_DEG_PER_FRAME = 1.5f
    const val FLING_DECAY_PER_FRAME = 0.88f

    /** One-finger orbit drag → (deltaYaw, deltaPitch) in degrees. */
    fun orbitDeltaForDrag(panX: Float, panY: Float): Pair<Float, Float> =
        Pair(panX * ORBIT_DRAG_YAW_PER_PX, -panY * ORBIT_DRAG_PITCH_PER_PX)

    /** First-person look drag → (deltaYaw, deltaPitch) in degrees. */
    fun walkLookDeltaForDrag(panX: Float, panY: Float): Pair<Float, Float> =
        Pair(-panX * WALK_LOOK_YAW_PER_PX, panY * WALK_LOOK_YAW_PER_PX)

    /** Two-finger pan drag → (deltaPanX, deltaPanY) in millimetres. */
    fun panDeltaForDrag(panX: Float, panY: Float): Pair<Float, Float> =
        Pair(-panX, panY)

    /** Cap a fling velocity (degrees per frame) so the post-release spin
     *  stays bounded. Returns (vx, vy) with the same direction. */
    fun cappedFlingVelocity(vx: Float, vy: Float): Pair<Float, Float> {
        val dist = sqrt(vx * vx + vy * vy)
        if (dist <= FLING_MAX_DEG_PER_FRAME || dist <= 0f) return Pair(vx, vy)
        val scale = FLING_MAX_DEG_PER_FRAME / dist
        return Pair(vx * scale, vy * scale)
    }
}
