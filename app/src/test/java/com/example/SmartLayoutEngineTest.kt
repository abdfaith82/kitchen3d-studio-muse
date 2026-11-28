package com.example

import com.example.core.geometry.SmartLayoutEngine
import com.example.core.geometry.SmartLayoutEngine.SmartLayoutConfig
import com.example.core.geometry.SmartLayoutEngine.SmartLayoutShape
import com.example.domain.model.KitchenElementType
import com.example.domain.model.RoomStructure
import com.example.domain.model.WallSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v1.16 — smart layout generator (AI智能布局): I / L / U from the room's
 * own walls, with the sink / stove / fridge placed by work-triangle rules.
 * Invariants checked here: anchors exist, nothing leaves the room, no two
 * floor-standing footprints overlap, the sink–stove leg and the triangle
 * perimeter stay inside the ergonomic band, corners are placed for L/U,
 * and no element is ever placed across a door opening.
 */
class SmartLayoutEngineTest {

    private val room = RoomStructure(widthMm = 4200f, depthMm = 3600f, heightMm = 2700f)

    private fun hasType(result: SmartLayoutEngine.SmartLayoutResult, type: KitchenElementType) =
        result.elements.any { it.type == type }

    private fun assertLayoutSound(result: SmartLayoutEngine.SmartLayoutResult) {
        // Every floor-standing element inside the room and pairwise disjoint.
        val floor = result.elements.filter { it.position.y < 100f }
        for (el in floor) {
            val a = el.getAABB()
            assertTrue("${el.type} min.x ${a.min.x} < 0", a.min.x >= -1f)
            assertTrue("${el.type} max.x ${a.max.x} > 4200", a.max.x <= room.widthMm + 1f)
            assertTrue("${el.type} min.z ${a.min.z} < 0", a.min.z >= -1f)
            assertTrue("${el.type} max.z ${a.max.z} > 3600", a.max.z <= room.depthMm + 1f)
        }
        for (i in floor.indices) {
            for (j in i + 1 until floor.size) {
                val a = floor[i].getAABB()
                val b = floor[j].getAABB()
                val overlapX = minOf(a.max.x, b.max.x) - maxOf(a.min.x, b.min.x)
                val overlapZ = minOf(a.max.z, b.max.z) - maxOf(a.min.z, b.min.z)
                assertTrue(
                    "${floor[i].type} overlaps ${floor[j].type} by ${overlapX}x${overlapZ} mm",
                    overlapX <= 1f || overlapZ <= 1f
                )
            }
        }
    }

    @Test
    fun lShapeDefaultRoomHasAllAnchorsAndCleanTriangle() {
        val result = SmartLayoutEngine.generate(
            room, SmartLayoutConfig(SmartLayoutShape.L_SHAPE, WallSide.NORTH)
        )
        assertTrue("warnings: ${result.warnings}", result.warnings.isEmpty())
        assertEquals(SmartLayoutShape.L_SHAPE, result.shapeUsed)
        assertTrue(hasType(result, KitchenElementType.BASE_SINK_UNIT))
        assertTrue(hasType(result, KitchenElementType.BASE_UNDER_OVEN))
        assertTrue(hasType(result, KitchenElementType.COOKTOP_INDUCTION))
        assertTrue(hasType(result, KitchenElementType.RANGE_HOOD_BOX))
        assertTrue(hasType(result, KitchenElementType.TALL_FRIDGE_HOUSING))
        assertTrue(hasType(result, KitchenElementType.DISHWASHER_INTEGRATED))
        assertEquals(1, result.elements.count { it.type == KitchenElementType.BASE_CORNER_L })
        val sinkToStove = result.sinkToStoveMm!!
        assertTrue("sink-stove $sinkToStove", sinkToStove in 1200f..2700f)
        val perimeter = result.trianglePerimeterMm!!
        assertTrue("perimeter $perimeter", perimeter in 3400f..7900f)
        assertLayoutSound(result)
    }

    @Test
    fun iShapeHasNoCornersAndCleanTriangle() {
        val result = SmartLayoutEngine.generate(
            room, SmartLayoutConfig(SmartLayoutShape.I_SHAPE, WallSide.NORTH)
        )
        assertTrue("warnings: ${result.warnings}", result.warnings.isEmpty())
        assertEquals(0, result.elements.count { it.type == KitchenElementType.BASE_CORNER_L })
        assertTrue(hasType(result, KitchenElementType.BASE_SINK_UNIT))
        assertTrue(hasType(result, KitchenElementType.TALL_FRIDGE_HOUSING))
        assertTrue(result.trianglePerimeterMm!! in 3400f..7900f)
        assertLayoutSound(result)
    }

    @Test
    fun uShapeHasTwoCornersAndCleanTriangle() {
        val result = SmartLayoutEngine.generate(
            room, SmartLayoutConfig(SmartLayoutShape.U_SHAPE, WallSide.NORTH)
        )
        assertTrue("warnings: ${result.warnings}", result.warnings.isEmpty())
        assertEquals(2, result.elements.count { it.type == KitchenElementType.BASE_CORNER_L })
        assertTrue(hasType(result, KitchenElementType.BASE_SINK_UNIT))
        assertTrue(hasType(result, KitchenElementType.BASE_UNDER_OVEN))
        assertTrue(hasType(result, KitchenElementType.TALL_FRIDGE_HOUSING))
        assertTrue(result.trianglePerimeterMm!! in 3400f..7900f)
        assertLayoutSound(result)
    }

    @Test
    fun uShapeFallsBackToLWhenRoomTooNarrow() {
        val narrow = RoomStructure(widthMm = 4200f, depthMm = 2000f, heightMm = 2700f)
        val result = SmartLayoutEngine.generate(
            narrow, SmartLayoutConfig(SmartLayoutShape.U_SHAPE, WallSide.NORTH)
        )
        assertEquals(SmartLayoutShape.L_SHAPE, result.shapeUsed)
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun tinyMainWallReturnsEmptyWithWarning() {
        val tiny = RoomStructure(widthMm = 2000f, depthMm = 3600f, heightMm = 2700f)
        val result = SmartLayoutEngine.generate(
            tiny, SmartLayoutConfig(SmartLayoutShape.L_SHAPE, WallSide.NORTH)
        )
        assertTrue(result.elements.isEmpty())
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun southMainWallNeverBlocksTheDoor() {
        // Default room: south wall has a 900 mm door at offset 600–1500.
        val result = SmartLayoutEngine.generate(
            room, SmartLayoutConfig(SmartLayoutShape.I_SHAPE, WallSide.SOUTH)
        )
        assertTrue(hasType(result, KitchenElementType.BASE_SINK_UNIT))
        assertTrue(hasType(result, KitchenElementType.BASE_UNDER_OVEN))
        for (el in result.elements) {
            if (el.position.y >= 100f) continue
            val a = el.getAABB()
            val onSouthWall = a.max.z > room.depthMm - 600f
            if (!onSouthWall) continue
            val crossesDoor = a.min.x < 1700f && a.max.x > 400f
            assertTrue("${el.type} crosses the south door span", !crossesDoor)
        }
        assertLayoutSound(result)
    }
}
