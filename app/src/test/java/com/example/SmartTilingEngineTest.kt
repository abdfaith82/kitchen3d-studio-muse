package com.example

import com.example.core.geometry.SmartTilingEngine
import com.example.core.geometry.SmartTilingEngine.TilingPattern
import com.example.core.geometry.SmartTilingEngine.TilingRequest
import com.example.core.geometry.SmartTilingEngine.TilingSurface
import com.example.core.math.Vector3
import com.example.domain.model.KitchenElement
import com.example.domain.model.KitchenElementType
import com.example.domain.model.MaterialFinish
import com.example.domain.model.RoomStructure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v1.16 — smart tiling (智能铺砖): cell-by-cell counts for straight and
 * brick patterns, the diagonal estimate, under-cabinet savings, and the
 * backsplash run length. Hand-computed expectations for the default
 * 4200×3600 room with 600×600 tiles and 3 mm grout:
 * straight → 7 cols × 6 rows = 42 tiles (30 full, 12 cut);
 * brick   → 45 tiles (30 full, 15 cut).
 */
class SmartTilingEngineTest {

    private val room = RoomStructure(widthMm = 4200f, depthMm = 3600f, heightMm = 2700f)

    private fun floorReq(
        pattern: TilingPattern = TilingPattern.STRAIGHT,
        tileW: Float = 600f,
        tileH: Float = 600f
    ) = TilingRequest(
        surface = TilingSurface.FLOOR,
        tileWidthMm = tileW,
        tileHeightMm = tileH,
        groutMm = 3f,
        pattern = pattern,
        sparePercent = 10f
    )

    @Test
    fun straightFloorCountsMatchHandCalculation() {
        val r = SmartTilingEngine.calculateFloor(room, emptyList(), floorReq())
        assertEquals(30, r.fullTiles)
        assertEquals(12, r.cutTiles)
        assertEquals(0, r.hiddenTiles)
        // 42 tiles + 10% spare (5) = 47.
        assertEquals(47, r.totalToBuy)
        assertEquals(15.12f, r.areaM2, 0.01f)
        assertTrue("waste ${r.wastePercent}", r.wastePercent in 0.5f..1.5f)
    }

    @Test
    fun brickFloorCountsMatchHandCalculation() {
        val r = SmartTilingEngine.calculateFloor(room, emptyList(), floorReq(TilingPattern.BRICK_HALF))
        assertEquals(30, r.fullTiles)
        assertEquals(15, r.cutTiles)
        // 45 tiles + 10% spare (5) = 50.
        assertEquals(50, r.totalToBuy)
    }

    @Test
    fun diagonalBuysMoreThanStraight() {
        val straight = SmartTilingEngine.calculateFloor(room, emptyList(), floorReq())
        val diagonal = SmartTilingEngine.calculateFloor(room, emptyList(), floorReq(TilingPattern.DIAGONAL_45))
        assertTrue(diagonal.totalToBuy > straight.totalToBuy)
        assertTrue(diagonal.wastePercent > straight.wastePercent)
    }

    @Test
    fun cabinetFootprintSavesOneFullTile() {
        // One 600×600 base cabinet exactly over grid cell (0,0).
        val cab = KitchenElement(
            id = "cab1",
            name = "cab1",
            type = KitchenElementType.BASE_SINGLE_DOOR,
            position = Vector3(300f, 0f, 300f),
            dimensions = Vector3(600f, 850f, 600f),
            material = MaterialFinish.NATURAL_OAK
        )
        val r = SmartTilingEngine.calculateFloor(room, listOf(cab), floorReq())
        assertEquals(1, r.hiddenTiles)
        // 42 - 1 + 10% spare (5) = 46.
        assertEquals(46, r.totalToBuy)
    }

    @Test
    fun backsplashUsesBaseRunLength() {
        val cabs = listOf(
            KitchenElement(
                id = "b1", name = "b1", type = KitchenElementType.BASE_SINGLE_DOOR,
                position = Vector3(300f, 0f, 300f),
                dimensions = Vector3(600f, 850f, 600f),
                material = MaterialFinish.NATURAL_OAK
            ),
            KitchenElement(
                id = "b2", name = "b2", type = KitchenElementType.BASE_SINGLE_DOOR,
                position = Vector3(900f, 0f, 300f),
                dimensions = Vector3(600f, 850f, 600f),
                material = MaterialFinish.NATURAL_OAK
            )
        )
        val r = SmartTilingEngine.calculateBacksplash(
            room, cabs,
            TilingRequest(
                surface = TilingSurface.BACKSPLASH,
                tileWidthMm = 300f, tileHeightMm = 600f,
                groutMm = 3f, pattern = TilingPattern.STRAIGHT, sparePercent = 10f
            )
        )
        assertEquals(1200f, r.areaWidthMm, 0.5f)
        assertEquals(600f, r.areaDepthMm, 0.5f)
        assertEquals(3, r.fullTiles)
        assertEquals(1, r.cutTiles)
        // 4 tiles + 10% spare (1) = 5.
        assertEquals(5, r.totalToBuy)
    }

    @Test
    fun backsplashWithNoCabinetsCarriesANote() {
        val r = SmartTilingEngine.calculateBacksplash(room, emptyList(), floorReq().copy(surface = TilingSurface.BACKSPLASH))
        assertEquals(0, r.totalToBuy)
        assertNotNull(r.noteAr)
    }
}
