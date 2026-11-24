package com.example.core.interaction

import com.example.core.math.Vector3
import kotlin.math.roundToInt

/**
 * Studio Policies v1.4 — pure, unit-testable rules behind the fixes users
 * reported from the videos:
 *
 *  - [ElementScalePolicy]: enlarge / shrink a selected element. There was
 *    NO scale operation anywhere in the app (rotate / delete / duplicate
 *    existed, scale did not), so the selection toolbar could never offer
 *    "تكبير / تصغير".
 *  - [BackPressPolicy]: the system back button used to exit the app
 *    immediately, even with a dialog open or an element selected.
 */
object ElementScalePolicy {
    const val MIN_WIDTH_DEPTH_MM = 150f
    const val MAX_WIDTH_DEPTH_MM = 3000f
    const val MIN_HEIGHT_MM = 150f
    const val MAX_HEIGHT_MM = 2700f
    const val STEP_FACTOR = 1.10f // one tap = +10% / −10%
    const val SNAP_MM = 10f

    /** Uniform scale of width/depth/height, clamped to buildable sizes, snapped to 10mm. */
    fun scaledDimensions(current: Vector3, factor: Float): Vector3 {
        val safeFactor = factor.coerceIn(0.5f, 2.0f)
        return Vector3(
            x = snap(current.x * safeFactor).coerceIn(MIN_WIDTH_DEPTH_MM, MAX_WIDTH_DEPTH_MM),
            y = snap(current.y * safeFactor).coerceIn(MIN_HEIGHT_MM, MAX_HEIGHT_MM),
            z = snap(current.z * safeFactor).coerceIn(MIN_WIDTH_DEPTH_MM, MAX_WIDTH_DEPTH_MM)
        )
    }

    private fun snap(mm: Float): Float = (mm / SNAP_MM).roundToInt() * SNAP_MM

    /** Current size as a percent of the type's default size (for the toolbar label). */
    fun scalePercent(current: Vector3, default: Vector3): Int {
        if (default.x <= 0f) return 100
        val ratio = ((current.x / default.x) + (current.z / default.z)) / 2f
        return (ratio * 100f).roundToInt().coerceIn(25, 400)
    }

    fun canScaleUp(current: Vector3): Boolean =
        current.x < MAX_WIDTH_DEPTH_MM || current.z < MAX_WIDTH_DEPTH_MM

    fun canScaleDown(current: Vector3): Boolean =
        current.x > MIN_WIDTH_DEPTH_MM || current.z > MIN_WIDTH_DEPTH_MM
}

enum class BackPressAction {
    CLOSE_DIALOG,
    CLEAR_SELECTION,
    EXIT_CLEAN_CANVAS,
    ASK_EXIT
}

object BackPressPolicy {
    /**
     * Priority order — the back button never exits the app while there is
     * something cheaper to undo first:
     *  1. an open dialog/panel closes,
     *  2. then the element selection clears,
     *  3. then clean-canvas mode exits,
     *  4. only then does the app ask "هل تريد الخروج؟" — it never exits silently.
     */
    fun nextAction(
        hasOpenDialog: Boolean,
        hasSelection: Boolean,
        isCleanCanvasMode: Boolean
    ): BackPressAction = when {
        hasOpenDialog -> BackPressAction.CLOSE_DIALOG
        hasSelection -> BackPressAction.CLEAR_SELECTION
        isCleanCanvasMode -> BackPressAction.EXIT_CLEAN_CANVAS
        else -> BackPressAction.ASK_EXIT
    }
}

/**
 * v1.5 — Smooth architectural cutaway fade.
 *
 * The old binary switch (wall visible / gone at dot < 0.08) made walls pop
 * in and out in a single frame while orbiting — the jump seen in the v1.4
 * video. The fade is linear: the wall is fully opaque at dot 0.35, fully
 * gone at dot 0.05, and fades smoothly in between. First-person walk never
 * cuts walls (the user is inside the room).
 */
object CutawayPolicy {
    const val FADE_START_DOT = 0.35f
    const val FADE_END_DOT = 0.05f

    fun fadeAlpha(facingCameraDot: Float): Float =
        ((facingCameraDot - FADE_END_DOT) / (FADE_START_DOT - FADE_END_DOT)).coerceIn(0f, 1f)

    fun isFullyCut(facingCameraDot: Float): Boolean = fadeAlpha(facingCameraDot) <= 0.02f
}
