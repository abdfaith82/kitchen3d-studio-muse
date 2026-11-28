package com.example

import com.example.core.geometry.MepEngine
import com.example.core.math.Vector3
import com.example.domain.model.KitchenElement
import com.example.domain.model.KitchenElementType
import com.example.domain.model.RoomStructure
import com.example.domain.model.UtilityPointType
import com.example.domain.model.Wall
import com.example.domain.model.WallSide
import com.example.domain.model.WallUtilityPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MepEngineTest {

    private fun cleanRoom(): RoomStructure {
        val walls = listOf(
            Wall(id = "wall_north", side = WallSide.NORTH, lengthMm = 4200f, heightMm = 2700f),
            Wall(id = "wall_south", side = WallSide.SOUTH, lengthMm = 4200f, heightMm = 2700f),
            Wall(id = "wall_west", side = WallSide.WEST, lengthMm = 3600f, heightMm = 2700f),
            Wall(id = "wall_east", side = WallSide.EAST, lengthMm = 3600f, heightMm = 2700f)
        )
        return RoomStructure(widthMm = 4200f, depthMm = 3600f, heightMm = 2700f, walls = walls)
    }

    private fun elem(id: String, type: KitchenElementType, x: Float, z: Float, w: Float = 600f) =
        KitchenElement(
            id = id, name = id, type = type,
            position = Vector3(x, 0f, z),
            dimensions = Vector3(w, 850f, 600f)
        )

    @Test
    fun `worktop sockets every 1200mm along a base run`() {
        val room = cleanRoom()
        val elements = listOf(
            elem("b1", KitchenElementType.BASE_SINGLE_DOOR, 600f, 300f),
            elem("b2", KitchenElementType.BASE_SINGLE_DOOR, 1200f, 300f),
            elem("b3", KitchenElementType.BASE_SINGLE_DOOR, 1800f, 300f)
        )
        val suggestions = MepEngine.suggestPoints(room, elements)
        // Run 300..2100 -> one socket at 900, in the 60cm zone at 1100mm.
        assertEquals(1, suggestions.size)
        assertEquals(UtilityPointType.ELECTRICAL_SOCKET, suggestions[0].type)
        assertEquals(900f, suggestions[0].offsetMm, 0.01f)
        assertEquals(1100f, suggestions[0].elevationMm, 0.01f)
        assertEquals(WallSide.NORTH, suggestions[0].wallSide)
    }

    @Test
    fun `appliance sockets plus gas and drain when missing`() {
        val room = cleanRoom()
        val elements = listOf(
            elem("sink", KitchenElementType.BASE_SINK_UNIT, 1500f, 300f, w = 800f),
            elem("cooktop", KitchenElementType.COOKTOP_INDUCTION, 2800f, 300f)
        )
        val suggestions = MepEngine.suggestPoints(room, elements)
        val pairs = suggestions.map { it.type to it.offsetMm }.toSet()
        assertTrue(pairs.contains(UtilityPointType.ELECTRICAL_SOCKET to 1800f))
        assertTrue(pairs.contains(UtilityPointType.ELECTRICAL_SOCKET to 3100f))
        assertTrue(pairs.contains(UtilityPointType.GAS_OUTLET to 2800f))
        assertTrue(pairs.contains(UtilityPointType.PLUMBING_DRAIN to 1500f))
        val gas = suggestions.first { it.type == UtilityPointType.GAS_OUTLET }
        assertEquals(750f, gas.elevationMm, 0.01f)
        val drain = suggestions.first { it.type == UtilityPointType.PLUMBING_DRAIN }
        assertEquals(500f, drain.elevationMm, 0.01f)
    }

    @Test
    fun `existing points are never duplicated`() {
        val north = Wall(
            id = "wall_north", side = WallSide.NORTH, lengthMm = 4200f, heightMm = 2700f,
            utilityPoints = listOf(
                WallUtilityPoint(id = "u1", type = UtilityPointType.ELECTRICAL_SOCKET, offsetFromWallStartMm = 1800f)
            )
        )
        val room = cleanRoom().copy(walls = cleanRoom().walls.map { if (it.side == WallSide.NORTH) north else it })
        val elements = listOf(elem("sink", KitchenElementType.BASE_SINK_UNIT, 1500f, 300f, w = 800f))
        val suggestions = MepEngine.suggestPoints(room, elements)
        // The sink socket at 1800 is skipped (existing); the drain is still suggested.
        assertTrue(suggestions.none { it.type == UtilityPointType.ELECTRICAL_SOCKET })
        assertTrue(suggestions.any { it.type == UtilityPointType.PLUMBING_DRAIN })
    }

    @Test
    fun `pipe runs from riser to sink and back`() {
        val north = Wall(
            id = "wall_north", side = WallSide.NORTH, lengthMm = 4200f, heightMm = 2700f,
            utilityPoints = listOf(
                WallUtilityPoint(id = "d1", type = UtilityPointType.PLUMBING_DRAIN, offsetFromWallStartMm = 2500f)
            )
        )
        val room = cleanRoom().copy(walls = cleanRoom().walls.map { if (it.side == WallSide.NORTH) north else it })
        val elements = listOf(elem("sink", KitchenElementType.BASE_SINK_UNIT, 1500f, 300f, w = 800f))
        val plan = MepEngine.planPipes(room, elements)
        assertNull(plan.noteAr)
        assertEquals(3, plan.runs.size)
        val cold = plan.runs.first { it.kind == MepEngine.PipeKind.SUPPLY_COLD }
        assertEquals(2500f, cold.points.first().xMm, 0.01f)
        assertEquals(120f, cold.points.first().zMm, 0.01f)
        assertEquals(1500f, cold.points.last().xMm, 0.01f)
        assertEquals(300f, cold.points.last().zMm, 0.01f)
    }

    @Test
    fun `dishwasher adds a supply branch and a drain branch`() {
        val north = Wall(
            id = "wall_north", side = WallSide.NORTH, lengthMm = 4200f, heightMm = 2700f,
            utilityPoints = listOf(
                WallUtilityPoint(id = "d1", type = UtilityPointType.PLUMBING_DRAIN, offsetFromWallStartMm = 2500f)
            )
        )
        val room = cleanRoom().copy(walls = cleanRoom().walls.map { if (it.side == WallSide.NORTH) north else it })
        val elements = listOf(
            elem("sink", KitchenElementType.BASE_SINK_UNIT, 1500f, 300f, w = 800f),
            elem("dw", KitchenElementType.DISHWASHER_INTEGRATED, 2800f, 300f)
        )
        val plan = MepEngine.planPipes(room, elements)
        assertEquals(5, plan.runs.size)
    }

    @Test
    fun `no sink or no drain gives an empty plan with a note`() {
        val room = cleanRoom()
        val noSink = MepEngine.planPipes(room, emptyList())
        assertTrue(noSink.runs.isEmpty())
        assertTrue(noSink.noteAr != null)
        val noDrain = MepEngine.planPipes(room, listOf(elem("sink", KitchenElementType.BASE_SINK_UNIT, 1500f, 300f, w = 800f)))
        assertTrue(noDrain.runs.isEmpty())
        assertTrue(noDrain.noteAr != null)
    }

    @Test
    fun `route along walls goes through the shared corner`() {
        val room = cleanRoom()
        val route = MepEngine.routeAlongWalls(room, WallSide.NORTH, 1000f, WallSide.EAST, 1000f)
        assertEquals(3, route.size)
        assertEquals(1000f, route[0].xMm, 0.01f)
        assertEquals(120f, route[0].zMm, 0.01f)
        assertEquals(4080f, route[1].xMm, 0.01f)
        assertEquals(120f, route[1].zMm, 0.01f)
        assertEquals(4080f, route[2].xMm, 0.01f)
        assertEquals(1000f, route[2].zMm, 0.01f)
        val sameWall = MepEngine.routeAlongWalls(room, WallSide.NORTH, 500f, WallSide.NORTH, 2000f)
        assertEquals(2, sameWall.size)
    }
}
