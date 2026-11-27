package com.example

import com.example.core.geometry.ErgonomicRuleCode
import com.example.core.geometry.ErgonomicValidationEngine
import com.example.core.math.Vector3
import com.example.domain.model.KitchenElement
import com.example.domain.model.KitchenElementType
import com.example.domain.model.RoomStructure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v1.15 — door-swing ARC interference (干涉检测) + drawer pull-out blockage.
 * Both checks fill the same blind spot of the straight-line corridor check:
 * it ignores an obstacle whose lateral overlap with the front corridor is
 * <= 80 mm, but a hinged leaf (quarter-disc) or a sliding drawer (straight
 * rectangle) still runs into it.
 */
class DoorSwingArcTest {

    private val room = RoomStructure(widthMm = 4200f, depthMm = 3600f, heightMm = 2700f)

    private fun cab(
        id: String,
        type: KitchenElementType,
        x: Float,
        z: Float,
        w: Float = 600f,
        d: Float = 600f,
        divisions: Int = 0
    ) = KitchenElement(
        id = id,
        name = id,
        type = type,
        position = Vector3(x, 0f, z),
        dimensions = Vector3(w, 850f, d),
        customDivisionsCount = divisions
    )

    @Test
    fun arcHitsSideSliverThatCorridorCheckIgnores() {
        // A: single-door base cabinet against the north wall, facing south.
        val a = cab("a", KitchenElementType.BASE_SINGLE_DOOR, 1500f, 300f)
        // B: narrow pull-out standing in front of A's right edge — only
        // 50 mm of lateral overlap with A's front corridor, so the
        // corridor check skips it, but it sits inside the door's arc.
        val b = cab("b", KitchenElementType.BASE_BOTTLE_RACK_15, 1775f, 950f, w = 150f)
        val report = ErgonomicValidationEngine.validate(room, listOf(a, b))

        val arc = report.issues.filter { it.code == ErgonomicRuleCode.DOOR_SWING_ARC_COLLISION }
        assertEquals(1, arc.size)
        assertEquals(listOf("a", "b"), arc[0].affectedElementIds.sorted())
        assertEquals(
            0,
            report.issues.count { it.code == ErgonomicRuleCode.INSUFFICIENT_DOOR_CLEARANCE }
        )
    }

    @Test
    fun arcIgnoresGrazingCornerAtNormalLJunction() {
        // A as above; B is a corner unit properly joined at the end of the
        // run — its corner exactly grazes the arc (600 mm = leaf width).
        // The 25 mm penetration rule must not flag a normal L-junction.
        val a = cab("a", KitchenElementType.BASE_SINGLE_DOOR, 1500f, 300f)
        val b = cab("b", KitchenElementType.BASE_CORNER_L, 2260f, 450f, w = 900f, d = 900f)
        val report = ErgonomicValidationEngine.validate(room, listOf(a, b))

        assertEquals(
            0,
            report.issues.count { it.code == ErgonomicRuleCode.DOOR_SWING_ARC_COLLISION }
        )
    }

    @Test
    fun arcHitsWideLeafBeyondCorridorReach() {
        // A: 1200 mm cabinet with a single 1200 mm leaf (divisions = 1).
        // B sits 800 mm in front — beyond the 650 mm corridor requirement,
        // so the corridor check stays silent, but the 1200 mm leaf sweeps
        // into it.
        val a = cab("a", KitchenElementType.BASE_WIDE_DOUBLE_120, 1500f, 300f, w = 1200f, divisions = 1)
        val b = cab("b", KitchenElementType.BASE_SINGLE_DOOR, 1500f, 1700f)
        val report = ErgonomicValidationEngine.validate(room, listOf(a, b))

        val arc = report.issues.filter { it.code == ErgonomicRuleCode.DOOR_SWING_ARC_COLLISION }
        assertEquals(1, arc.size)
        assertTrue(arc[0].affectedElementIds.containsAll(listOf("a", "b")))
        assertEquals(
            0,
            report.issues.count { it.code == ErgonomicRuleCode.INSUFFICIENT_DOOR_CLEARANCE }
        )
    }

    @Test
    fun drawerPullOutBlockedInSideSliver() {
        // A: 3-drawer base. B sits in the same sliver as test 1: the
        // corridor check skips it (50 mm lateral overlap), but the drawer
        // (500 mm travel) runs straight into it.
        val a = cab("a", KitchenElementType.BASE_3_DRAWERS, 1500f, 300f)
        val b = cab("b", KitchenElementType.BASE_BOTTLE_RACK_15, 1775f, 950f, w = 150f)
        val report = ErgonomicValidationEngine.validate(room, listOf(a, b))

        val drawer = report.issues.filter { it.code == ErgonomicRuleCode.DRAWER_PULL_OUT_BLOCKED }
        assertEquals(1, drawer.size)
        assertEquals(listOf("a", "b"), drawer[0].affectedElementIds.sorted())
        assertEquals(
            0,
            report.issues.count { it.code == ErgonomicRuleCode.DOOR_SWING_ARC_COLLISION }
        )
    }

    @Test
    fun drawerNotBlockedBeyondTravel() {
        // Same drawer unit, obstacle 800 mm in front — beyond the 500 mm
        // travel, so the drawer opens freely.
        val a = cab("a", KitchenElementType.BASE_3_DRAWERS, 1500f, 300f)
        val b = cab("b", KitchenElementType.BASE_SINGLE_DOOR, 1500f, 1700f)
        val report = ErgonomicValidationEngine.validate(room, listOf(a, b))

        assertEquals(
            0,
            report.issues.count { it.code == ErgonomicRuleCode.DRAWER_PULL_OUT_BLOCKED }
        )
    }
}
