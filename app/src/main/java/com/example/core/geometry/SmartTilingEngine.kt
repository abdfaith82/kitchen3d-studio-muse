package com.example.core.geometry

import com.example.domain.model.KitchenElement
import com.example.domain.model.RoomStructure
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Smart tiling calculator — v1.16, Chinese batch 2 (智能铺砖).
 *
 * Pure geometry, no server: given a floor area (the room) or a backsplash
 * strip (the wall-adjacent base runs), a tile size, a pattern and a grout
 * width, it counts full tiles, cut tiles, tiles saved by skipping the area
 * under floor-standing cabinets, and the offcut waste percentage.
 *
 * Honest limits:
 * - STRAIGHT and BRICK_HALF are computed cell-by-cell exactly (each grid
 *   cell's kept rectangle is intersected with the area). DIAGONAL_45 is an
 *   ESTIMATE: the straight grid plus the industry-standard 15% extra
 *   allowance for angled cuts — the UI labels it as such.
 * - Waste % = offcut area / bought tile area. Grout gaps are not waste:
 *   they are part of the covered area.
 * - A cell counts as hidden (saved) when ≥ 90% of it sits under a
 *   floor-standing cabinet footprint. Tiles are assumed to run under wall
 *   cabinets (they hang above the floor) and under the fridge housing.
 * - Backsplash length = the summed wall-adjacent length of base cabinets.
 *   If the kitchen has no base cabinets against a wall, the length is 0
 *   and the result carries a note.
 */
object SmartTilingEngine {

    enum class TilingPattern(val titleAr: String, val descriptionAr: String) {
        STRAIGHT("مستقيم", "صفوف متوازية بدون إزاحة"),
        BRICK_HALF("طوب (إزاحة نصف بلاطة)", "كل صف مزاح نصف بلاطة عن السابق"),
        DIAGONAL_45("مائل 45° (تقديري)", "قطع مائل — يضاف 15% هدر تقديري للقص")
    }

    enum class TilingSurface(val titleAr: String) {
        FLOOR("أرضية"),
        BACKSPLASH("وزرة جدارية (فوق سطح العمل)")
    }

    data class TilingRequest(
        val surface: TilingSurface,
        val tileWidthMm: Float,
        val tileHeightMm: Float,
        val groutMm: Float = 3f,
        val pattern: TilingPattern = TilingPattern.STRAIGHT,
        val sparePercent: Float = 10f,
        val pricePerTile: Float = 0f
    )

    data class TilingResult(
        val areaWidthMm: Float,
        val areaDepthMm: Float,
        val areaM2: Float,
        val fullTiles: Int,
        val cutTiles: Int,
        val hiddenTiles: Int,
        val totalToBuy: Int,
        val wastePercent: Float,
        val cost: Float,
        val noteAr: String? = null
    )

    private data class GridCount(
        val full: Int,
        val cut: Int,
        val offcutAreaMm2: Float,
        val keptAreaMm2: Float
    )

    // ---- public entry -------------------------------------------------------

    fun calculateFloor(
        room: RoomStructure,
        elements: List<KitchenElement>,
        request: TilingRequest
    ): TilingResult {
        val grid = countGrid(
            areaW = room.widthMm,
            areaH = room.depthMm,
            request = request
        )
        val hidden = countHiddenFloorTiles(room, elements, request)
        return finish(request, room.widthMm, room.depthMm, grid, hidden, null)
    }

    fun calculateBacksplash(
        room: RoomStructure,
        elements: List<KitchenElement>,
        request: TilingRequest
    ): TilingResult {
        val runLength = backsplashRunLengthMm(room, elements)
        if (runLength <= 0f) {
            return TilingResult(
                areaWidthMm = 0f,
                areaDepthMm = 600f,
                areaM2 = 0f,
                fullTiles = 0,
                cutTiles = 0,
                hiddenTiles = 0,
                totalToBuy = 0,
                wastePercent = 0f,
                cost = 0f,
                noteAr = "لا توجد خزائن سفلية ملاصقة للجدار — أضف وحدات سفلية أولاً"
            )
        }
        val grid = countGrid(areaW = runLength, areaH = 600f, request = request)
        return finish(request, runLength, 600f, grid, 0, null)
    }

    // ---- grid counting --------------------------------------------------------

    /**
     * Cell-by-cell grid count. The grid origin is the area's top-left corner
     * (north-west). Brick rows are offset by half a tile, so the first cell
     * of an odd row starts half a tile outside the area.
     */
    private fun countGrid(areaW: Float, areaH: Float, request: TilingRequest): GridCount {
        val tileW = request.tileWidthMm
        val tileH = request.tileHeightMm
        val pitchW = tileW + request.groutMm
        val pitchH = tileH + request.groutMm
        val rows = ceil((areaH - tileH) / pitchH).toInt() + 1
        var full = 0
        var cut = 0
        var offcut = 0f
        var kept = 0f
        for (row in 0 until rows) {
            val y0 = row * pitchH
            val y1 = min(y0 + tileH, areaH)
            val rowHeight = y1 - y0
            if (rowHeight <= 0f) continue
            val offset = if (request.pattern == TilingPattern.BRICK_HALF && row % 2 == 1) tileW / 2f else 0f
            // First tile starts at -offset; columns follow every pitchW.
            var col = 0
            while (true) {
                val x0 = col * pitchW - offset
                val x1 = x0 + tileW
                if (x0 >= areaW) break
                val keptW = min(x1, areaW) - max(x0, 0f)
                if (keptW <= 0f) {
                    col++
                    continue
                }
                val keptArea = keptW * rowHeight
                val tileArea = tileW * tileH
                kept += keptArea
                if (keptW >= tileW - 0.5f && rowHeight >= tileH - 0.5f) {
                    full++
                } else {
                    cut++
                    offcut += tileArea - keptArea
                }
                col++
            }
        }
        return GridCount(full, cut, offcut, kept)
    }

    /** Grid cells ≥ 90% covered by a floor-standing footprint are not bought. */
    private fun countHiddenFloorTiles(
        room: RoomStructure,
        elements: List<KitchenElement>,
        request: TilingRequest
    ): Int {
        val footprints = elements
            .filter { it.position.y < 100f }
            .map { it.getAABB() }
        if (footprints.isEmpty()) return 0
        val tileW = request.tileWidthMm
        val tileH = request.tileHeightMm
        val pitchW = tileW + request.groutMm
        val pitchH = tileH + request.groutMm
        val rows = ceil((room.depthMm - tileH) / pitchH).toInt() + 1
        var hidden = 0
        for (row in 0 until rows) {
            val y0 = row * pitchH
            val y1 = min(y0 + tileH, room.depthMm)
            if (y1 - y0 <= 0f) continue
            val offset = if (request.pattern == TilingPattern.BRICK_HALF && row % 2 == 1) tileW / 2f else 0f
            var col = 0
            while (true) {
                val x0 = col * pitchW - offset
                val x1 = x0 + tileW
                if (x0 >= room.widthMm) break
                val cellX0 = max(x0, 0f)
                val cellX1 = min(x1, room.widthMm)
                val cellY0 = max(y0, 0f)
                val cellY1 = y1
                val cellArea = (cellX1 - cellX0) * (cellY1 - cellY0)
                if (cellArea > 0f) {
                    var covered = 0f
                    for (fp in footprints) {
                        val ox = min(cellX1, fp.max.x) - max(cellX0, fp.min.x)
                        val oz = min(cellY1, fp.max.z) - max(cellY0, fp.min.z)
                        if (ox > 0f && oz > 0f) covered += ox * oz
                    }
                    if (covered / cellArea >= 0.9f) hidden++
                }
                col++
            }
        }
        return hidden
    }

    /** Summed wall-adjacent length of base cabinets (the backsplash strip). */
    fun backsplashRunLengthMm(room: RoomStructure, elements: List<KitchenElement>): Float {
        var total = 0f
        for (el in elements) {
            if (el.position.y >= 100f) continue
            val aabb = el.getAABB()
            val touchesWall = aabb.min.x <= 10f || aabb.max.x >= room.widthMm - 10f ||
                aabb.min.z <= 10f || aabb.max.z >= room.depthMm - 10f
            if (!touchesWall) continue
            // Length along the wall it touches.
            val along = if (aabb.min.x <= 10f || aabb.max.x >= room.widthMm - 10f) {
                aabb.max.z - aabb.min.z
            } else {
                aabb.max.x - aabb.min.x
            }
            total += along
        }
        return total
    }

    // ---- finishing ------------------------------------------------------------

    private fun finish(
        request: TilingRequest,
        areaW: Float,
        areaH: Float,
        grid: GridCount,
        hidden: Int,
        noteAr: String?
    ): TilingResult {
        val tileArea = request.tileWidthMm * request.tileHeightMm
        val base = grid.full + grid.cut - hidden
        val diagonalExtra = if (request.pattern == TilingPattern.DIAGONAL_45) {
            ceil(base * 0.15f).toInt()
        } else 0
        val beforeSpare = base + diagonalExtra
        val spare = ceil(beforeSpare * request.sparePercent / 100f).toInt()
        val totalToBuy = beforeSpare + spare
        val boughtArea = totalToBuy * tileArea
        // Waste = offcut pieces + the diagonal allowance, over bought area.
        // Spare tiles are not waste until they are cut.
        val waste = if (boughtArea > 0f) {
            (grid.offcutAreaMm2 + diagonalExtra * tileArea) / boughtArea * 100f
        } else 0f
        val cost = if (request.pricePerTile > 0f) totalToBuy * request.pricePerTile else 0f
        return TilingResult(
            areaWidthMm = areaW,
            areaDepthMm = areaH,
            areaM2 = areaW * areaH / 1_000_000f,
            fullTiles = grid.full,
            cutTiles = grid.cut,
            hiddenTiles = hidden,
            totalToBuy = totalToBuy,
            wastePercent = waste,
            cost = cost,
            noteAr = noteAr
        )
    }
}
