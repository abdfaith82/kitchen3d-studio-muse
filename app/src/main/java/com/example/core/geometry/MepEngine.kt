package com.example.core.geometry

import com.example.domain.model.KitchenElement
import com.example.domain.model.KitchenElementType
import com.example.domain.model.RoomStructure
import com.example.domain.model.UtilityPointType
import com.example.domain.model.WallSide
import kotlin.math.abs

/**
 * v1.18 — Chinese features, batch 4 (water/electric 水暖电, simplified).
 *
 * Pure, unit-testable MEP logic:
 *  1. [suggestPoints] — auto socket placement (智能插座自动布置): one socket
 *     per appliance plus worktop sockets every 1200 mm along base runs, all
 *     in the 60 cm backsplash zone (1100 mm elevation); plus a gas outlet
 *     near the cooktop and a drain near the sink when they are missing.
 *  2. [planPipes] — pipe routing: cold/hot supply from the riser (the drain
 *     point's wall position) to the sink and dishwasher, and drain lines back
 *     to the riser, as Manhattan polylines 120 mm off the wall face.
 *
 * What this does NOT do: the manual point editor (RoomSettingsDialog) and
 * the MEP layer of the PDF blueprint already exist — this engine only adds
 * the automatic layout and the routing the app was missing.
 */
object MepEngine {

    /** A point the engine suggests adding to a wall. */
    data class SuggestedPoint(
        val wallId: String,
        val wallSide: WallSide,
        val type: UtilityPointType,
        val offsetMm: Float,
        val elevationMm: Float,
        val labelAr: String,
        val reasonAr: String
    )

    /** A 2D plan point in millimetres (x east, z south). */
    data class PlanPoint(val xMm: Float, val zMm: Float)

    enum class PipeKind(val titleAr: String, val colorHex: Long) {
        SUPPLY_COLD("تغذية ماء بارد", 0xFF0284C7L),
        SUPPLY_HOT("تغذية ماء ساخن", 0xFFDC2626L),
        DRAIN("صرف", 0xFF64748BL)
    }

    /** One pipe run as a polyline on the 2D plan. */
    data class PipeRun(
        val kind: PipeKind,
        val points: List<PlanPoint>,
        val labelAr: String
    )

    data class PipePlan(
        val runs: List<PipeRun>,
        val noteAr: String?
    )

    private const val SOCKET_ELEVATION_MM = 1100f // centre of the 60 cm backsplash zone
    private const val DRAIN_ELEVATION_MM = 500f
    private const val GAS_ELEVATION_MM = 750f
    private const val DEDUP_MM = 200f
    private const val WORKTOP_SOCKET_SPACING_MM = 1200f
    private const val PIPE_INSET_MM = 120f // pipe line offset from the wall face

    private val APPLIANCE_SOCKET_TYPES = mapOf(
        KitchenElementType.BASE_SINK_UNIT to "بجانب الحوض (أجهزة سطح العمل)",
        KitchenElementType.COOKTOP_INDUCTION to "بجانب الموقد (أجهزة صغيرة)",
        KitchenElementType.BASE_UNDER_OVEN to "بجانب الفرن",
        KitchenElementType.TALL_FRIDGE_HOUSING to "للثلاجة",
        KitchenElementType.DISHWASHER_INTEGRATED to "لغسالة الصحون"
    )

    /** The wall an element stands against, from its rotation (0=N, 90=W, 180=S, 270=E). */
    fun wallOf(rotationY: Float): WallSide {
        val r = ((rotationY % 360f) + 360f) % 360f
        return when {
            r < 45f || r >= 315f -> WallSide.NORTH
            r < 135f -> WallSide.WEST
            r < 225f -> WallSide.SOUTH
            else -> WallSide.EAST
        }
    }

    /** Offset of a plan position along a wall (x for N/S, z for E/W). */
    fun offsetAlongWall(xMm: Float, zMm: Float, side: WallSide): Float =
        when (side) {
            WallSide.NORTH, WallSide.SOUTH -> xMm
            WallSide.WEST, WallSide.EAST -> zMm
        }

    /** Along-wall size of an element (x for N/S walls, z for E/W walls). */
    private fun alongSize(el: KitchenElement, side: WallSide): Float =
        when (side) {
            WallSide.NORTH, WallSide.SOUTH -> el.dimensions.x
            WallSide.WEST, WallSide.EAST -> el.dimensions.z
        }

    /** Plan point on a wall line, [insetMm] inside the room from the wall face. */
    fun wallPlanPoint(room: RoomStructure, side: WallSide, offsetMm: Float, insetMm: Float = PIPE_INSET_MM): PlanPoint =
        when (side) {
            WallSide.NORTH -> PlanPoint(offsetMm, insetMm)
            WallSide.SOUTH -> PlanPoint(offsetMm, room.depthMm - insetMm)
            WallSide.WEST -> PlanPoint(insetMm, offsetMm)
            WallSide.EAST -> PlanPoint(room.widthMm - insetMm, offsetMm)
        }

    /** Manhattan route between two wall-line points, via the shared corner(s). */
    fun routeAlongWalls(room: RoomStructure, fromSide: WallSide, fromOffset: Float, toSide: WallSide, toOffset: Float): List<PlanPoint> {
        val start = wallPlanPoint(room, fromSide, fromOffset)
        val end = wallPlanPoint(room, toSide, toOffset)
        if (fromSide == toSide) return listOf(start, end)
        val order = listOf(WallSide.NORTH, WallSide.EAST, WallSide.SOUTH, WallSide.WEST)
        val fromIdx = order.indexOf(fromSide)
        val toIdx = order.indexOf(toSide)
        val clockwiseSteps = (toIdx - fromIdx + 4) % 4
        val counterSteps = (fromIdx - toIdx + 4) % 4
        val clockwise = clockwiseSteps <= counterSteps
        val sides = mutableListOf<WallSide>()
        var idx = fromIdx
        val steps = if (clockwise) clockwiseSteps else counterSteps
        repeat(steps) {
            idx = if (clockwise) (idx + 1) % 4 else (idx + 3) % 4
            sides.add(order[idx])
        }
        // Corner between side A and side B, inset from both walls.
        fun corner(a: WallSide, b: WallSide): PlanPoint {
            val x = when {
                a == WallSide.EAST || b == WallSide.EAST -> room.widthMm - PIPE_INSET_MM
                else -> PIPE_INSET_MM
            }
            val z = when {
                a == WallSide.SOUTH || b == WallSide.SOUTH -> room.depthMm - PIPE_INSET_MM
                else -> PIPE_INSET_MM
            }
            return PlanPoint(x, z)
        }
        val points = mutableListOf(start)
        var prev = fromSide
        for (side in sides) {
            // Every wall transition passes through the shared corner — even
            // the last one, so the path never cuts diagonally across the room.
            points.add(corner(prev, side))
            prev = side
        }
        points.add(end)
        return points
    }

    /**
     * Suggested utility points for the current kitchen. Existing points of the
     * same type within [DEDUP_MM] on the same wall are never duplicated.
     */
    fun suggestPoints(room: RoomStructure, elements: List<KitchenElement>): List<SuggestedPoint> {
        val out = mutableListOf<SuggestedPoint>()
        val wallsBySide = room.walls.associateBy { it.side }

        fun wallId(side: WallSide): String? = wallsBySide[side]?.id
        fun hasNearby(side: WallSide, type: UtilityPointType, offsetMm: Float, withinMm: Float): Boolean {
            val wall = wallsBySide[side] ?: return false
            return wall.utilityPoints.any {
                it.type == type && abs(it.offsetFromWallStartMm - offsetMm) < withinMm
            } || out.any {
                it.wallSide == side && it.type == type && abs(it.offsetMm - offsetMm) < withinMm
            }
        }
        fun suggest(side: WallSide, type: UtilityPointType, offsetMm: Float, elevationMm: Float, labelAr: String, reasonAr: String, withinMm: Float = DEDUP_MM) {
            val id = wallId(side) ?: return
            val wallLen = wallsBySide[side]?.lengthMm ?: return
            val off = offsetMm.coerceIn(100f, (wallLen - 100f).coerceAtLeast(100f))
            if (hasNearby(side, type, off, withinMm)) return
            out.add(SuggestedPoint(id, side, type, off, elevationMm, labelAr, reasonAr))
        }

        // 1. One socket per appliance, 300 mm to the side of its centre.
        for ((type, reason) in APPLIANCE_SOCKET_TYPES) {
            val el = elements.firstOrNull { it.type == type } ?: continue
            val side = wallOf(el.rotationY)
            val centre = offsetAlongWall(el.position.x, el.position.z, side)
            val wallLen = wallsBySide[side]?.lengthMm ?: continue
            val off = if (centre + 300f <= wallLen - 100f) centre + 300f else centre - 300f
            suggest(side, UtilityPointType.ELECTRICAL_SOCKET, off, SOCKET_ELEVATION_MM, "مقبس ${el.name}", reason)
        }

        // 2. Worktop sockets every 1200 mm along each base-cabinet run.
        val baseTypes = setOf(
            KitchenElementType.BASE_SINGLE_DOOR, KitchenElementType.BASE_DOUBLE_DOOR,
            KitchenElementType.BASE_SINK_UNIT, KitchenElementType.BASE_UNDER_OVEN,
            KitchenElementType.BASE_CORNER_L, KitchenElementType.DISHWASHER_INTEGRATED
        )
        val basesByWall = elements.filter { it.type in baseTypes }
            .groupBy { wallOf(it.rotationY) }
        for ((side, bases) in basesByWall) {
            val sorted = bases.sortedBy { offsetAlongWall(it.position.x, it.position.z, side) }
            // Group into runs: consecutive cabinets with a gap under 150 mm.
            val runs = mutableListOf<MutableList<KitchenElement>>()
            for (el in sorted) {
                val last = runs.lastOrNull()?.lastOrNull()
                val gap = if (last == null) Float.MAX_VALUE else {
                    val lastEnd = offsetAlongWall(last.position.x, last.position.z, side) + alongSize(last, side) / 2f
                    val start = offsetAlongWall(el.position.x, el.position.z, side) - alongSize(el, side) / 2f
                    start - lastEnd
                }
                if (last == null || gap > 150f) runs.add(mutableListOf(el)) else runs.last().add(el)
            }
            for (run in runs) {
                val first = run.first()
                val lastEl = run.last()
                val runStart = offsetAlongWall(first.position.x, first.position.z, side) - alongSize(first, side) / 2f
                val runEnd = offsetAlongWall(lastEl.position.x, lastEl.position.z, side) + alongSize(lastEl, side) / 2f
                var pos = runStart + 600f
                while (pos <= runEnd - 300f) {
                    suggest(side, UtilityPointType.ELECTRICAL_SOCKET, pos, SOCKET_ELEVATION_MM, "مقبس سطح عمل", "مقبس كل 1200 مم على سطح العمل")
                    pos += WORKTOP_SOCKET_SPACING_MM
                }
            }
        }

        // 3. Gas outlet near the cooktop, when missing.
        val cooktop = elements.firstOrNull { it.type == KitchenElementType.COOKTOP_INDUCTION }
        if (cooktop != null) {
            val side = wallOf(cooktop.rotationY)
            val centre = offsetAlongWall(cooktop.position.x, cooktop.position.z, side)
            suggest(side, UtilityPointType.GAS_OUTLET, centre, GAS_ELEVATION_MM, "مخرج غاز للموقد", "مخرج غاز بجانب الموقد", withinMm = 600f)
        }

        // 4. Drain near the sink, when missing.
        val sink = elements.firstOrNull { it.type == KitchenElementType.BASE_SINK_UNIT }
        if (sink != null) {
            val side = wallOf(sink.rotationY)
            val centre = offsetAlongWall(sink.position.x, sink.position.z, side)
            suggest(side, UtilityPointType.PLUMBING_DRAIN, centre, DRAIN_ELEVATION_MM, "نقطة صرف للحوض", "صرف تحت الحوض", withinMm = 900f)
        }

        return out.sortedWith(compareBy({ it.wallSide.ordinal }, { it.offsetMm }))
    }

    /**
     * Pipe runs for the current kitchen. Supply (cold + hot) starts at the
     * riser — the existing drain point — and runs to the sink and dishwasher;
     * drain lines run back to the riser. Empty with a note when there is no
     * sink or no drain point to start from.
     */
    fun planPipes(room: RoomStructure, elements: List<KitchenElement>): PipePlan {
        val sink = elements.firstOrNull { it.type == KitchenElementType.BASE_SINK_UNIT }
            ?: return PipePlan(emptyList(), "لا يوجد حوض — أضف وحدة حوض أولاً لتوليد مسارات الأنابيب.")
        val drainWall = room.walls.firstOrNull { wall ->
            wall.utilityPoints.any { it.type == UtilityPointType.PLUMBING_DRAIN }
        } ?: return PipePlan(emptyList(), "لا توجد نقطة صرف — أضف نقطة سباكة وصرف من إعدادات الغرفة أولاً.")
        val drainPoint = drainWall.utilityPoints.first { it.type == UtilityPointType.PLUMBING_DRAIN }
        val riserSide = drainWall.side
        val riserOffset = drainPoint.offsetFromWallStartMm

        val sinkSide = wallOf(sink.rotationY)
        val sinkOffset = offsetAlongWall(sink.position.x, sink.position.z, sinkSide)
        val sinkCentre = PlanPoint(sink.position.x, sink.position.z)
        val dishwasher = elements.firstOrNull { it.type == KitchenElementType.DISHWASHER_INTEGRATED }
        val dwSide = dishwasher?.let { wallOf(it.rotationY) }
        val dwOffset = dishwasher?.let { offsetAlongWall(it.position.x, it.position.z, dwSide!!) }
        val dwCentre = dishwasher?.let { PlanPoint(it.position.x, it.position.z) }

        // Wall-line path from the riser to a fixture, plus the stub into it.
        fun supplyPath(toSide: WallSide, toOffset: Float, toCentre: PlanPoint): List<PlanPoint> {
            val line = routeAlongWalls(room, riserSide, riserOffset, toSide, toOffset)
            return line + toCentre
        }
        fun drainPath(fromCentre: PlanPoint, fromSide: WallSide, fromOffset: Float): List<PlanPoint> {
            val line = routeAlongWalls(room, fromSide, fromOffset, riserSide, riserOffset)
            return listOf(fromCentre) + line
        }

        val runs = mutableListOf<PipeRun>()
        val sinkSupply = supplyPath(sinkSide, sinkOffset, sinkCentre)
        runs.add(PipeRun(PipeKind.SUPPLY_COLD, sinkSupply, "ماء بارد: المصدر ← الحوض"))
        runs.add(PipeRun(PipeKind.SUPPLY_HOT, sinkSupply, "ماء ساخن: المصدر ← الحوض"))
        runs.add(PipeRun(PipeKind.DRAIN, drainPath(sinkCentre, sinkSide, sinkOffset), "صرف: الحوض ← نقطة الصرف"))
        if (dishwasher != null && dwSide != null && dwOffset != null && dwCentre != null) {
            // Branch: continue the cold supply along the wall to the dishwasher.
            val branch = routeAlongWalls(room, sinkSide, sinkOffset, dwSide, dwOffset) + dwCentre
            runs.add(PipeRun(PipeKind.SUPPLY_COLD, branch, "ماء بارد: الحوض ← غسالة الصحون"))
            runs.add(PipeRun(PipeKind.DRAIN, drainPath(dwCentre, dwSide, dwOffset), "صرف: غسالة الصحون ← نقطة الصرف"))
        }
        return PipePlan(runs, null)
    }
}
