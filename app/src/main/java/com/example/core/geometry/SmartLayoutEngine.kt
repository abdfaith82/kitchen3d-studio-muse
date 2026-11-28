package com.example.core.geometry

import com.example.core.math.Vector3
import com.example.domain.model.KitchenElement
import com.example.domain.model.KitchenElementType
import com.example.domain.model.MaterialFinish
import com.example.domain.model.OpeningType
import com.example.domain.model.RoomStructure
import com.example.domain.model.WallSide
import java.util.UUID

/**
 * Smart layout generator — v1.16, Chinese batch 2 (AI智能布局 / 一键整厨方案).
 *
 * Deterministic geometric rules, no ML and no server: given the room and a
 * shape (I / L / U) it builds a complete kitchen — sink, stove + hood,
 * fridge, dishwasher, corner units, base + wall runs — and places the three
 * work-triangle anchors so the triangle stays inside the ergonomic band
 * (sink–stove 1200–2700 mm centre distance, ≥ 400 mm prep landing gap,
 * perimeter 3400–7900 mm, the same limits ErgonomicValidationEngine checks).
 *
 * How anchors are decided (two phases):
 * 1. generate() picks the anchor slots: sink on the main wall (snapped under
 *    the window when the window centre falls in the middle of the run),
 *    stove 1500 mm from the sink (1200 mm fallback), fridge at the far end
 *    of the main run — or, when the main run is too short (the U case), at
 *    the far end of a side run, reserved before that run is filled.
 * 2. buildRun() places the anchors, then fills the gaps with 600 mm units
 *    (+ one custom-width unit / filler strip) and the wall row above,
 *    skipping the hood span, tall-unit spans and window spans.
 *
 * Honest limits:
 * - The generator only replaces ELEMENTS; the room (size, walls, openings)
 *   is never changed.
 * - Door openings are reserved: no element is ever placed across a door
 *   span (plus a 200 mm swing margin). Window spans keep base cabinets
 *   (sill ≥ 900 mm) but skip wall cabinets where the window top is above
 *   the wall-cabinet bottom (1450 mm).
 * - A corner unit is a 900×900 square in plan, so its rotation is cosmetic
 *   only; it is set to the main wall's rotation.
 * - The triangle is AIMED at the optimal band, not guaranteed: a tiny room
 *   or a door cutting the main wall can force a shorter leg or drop the
 *   fridge/dishwasher; the result carries warnings and the app's own
 *   validator still runs afterwards.
 */
object SmartLayoutEngine {

    enum class SmartLayoutShape(val titleAr: String, val titleEn: String) {
        I_SHAPE("خط واحد (I)", "Single wall"),
        L_SHAPE("حرف L", "L-shape"),
        U_SHAPE("حرف U", "U-shape")
    }

    data class SmartLayoutConfig(
        val shape: SmartLayoutShape = SmartLayoutShape.L_SHAPE,
        val mainWall: WallSide = WallSide.NORTH,
        val includeDishwasher: Boolean = true,
        val includeTallPantry: Boolean = false,
        val cabinetMaterial: MaterialFinish = MaterialFinish.NATURAL_OAK,
        val countertopMaterial: MaterialFinish = MaterialFinish.CARRARA_MARBLE
    )

    data class SmartLayoutResult(
        val elements: List<KitchenElement>,
        val shapeUsed: SmartLayoutShape,
        val sinkToStoveMm: Float?,
        val trianglePerimeterMm: Float?,
        val warnings: List<String>
    )

    // ---- public entry -------------------------------------------------------

    fun generate(room: RoomStructure, config: SmartLayoutConfig): SmartLayoutResult {
        val warnings = mutableListOf<String>()
        val mainLen = wallLength(config.mainWall, room)
        if (mainLen < 2400f) {
            return SmartLayoutResult(
                elements = emptyList(),
                shapeUsed = config.shape,
                sinkToStoveMm = null,
                trianglePerimeterMm = null,
                warnings = listOf(
                    "الجدار الرئيسي قصير جداً (${mainLen.toInt()} مم) — التخطيط الذكي يحتاج 2400 مم على الأقل"
                )
            )
        }

        // Resolve the shape against the room: U needs depth for two 600 mm
        // runs + 1200 mm of clear floor between them; L needs the second
        // wall to hold at least a corner + one 600 mm unit.
        var shape = config.shape
        val secondWall = clockwiseNeighbour(config.mainWall)
        val thirdWall = counterClockwiseNeighbour(config.mainWall)
        if (shape == SmartLayoutShape.U_SHAPE) {
            if (wallLength(secondWall, room) < 2400f) {
                shape = SmartLayoutShape.L_SHAPE
                warnings.add(
                    "الغرفة ضيقة لشكل U (عمق ${wallLength(secondWall, room).toInt()} مم) — تم استخدام حرف L بدلاً منه"
                )
            }
        }
        if (shape == SmartLayoutShape.L_SHAPE) {
            if (wallLength(secondWall, room) < 1500f) {
                shape = SmartLayoutShape.I_SHAPE
                warnings.add("الجدار المجاور قصير جداً — تم استخدام خط واحد (I) بدلاً من L")
            }
        }

        val runWalls = when (shape) {
            SmartLayoutShape.I_SHAPE -> listOf(config.mainWall)
            SmartLayoutShape.L_SHAPE -> listOf(config.mainWall, secondWall)
            SmartLayoutShape.U_SHAPE -> listOf(config.mainWall, secondWall, thirdWall)
        }

        // Corner junctions: each junction trims 900 mm off BOTH runs that
        // meet there, and one corner unit is placed in the corner.
        val junctions = mutableListOf<CornerJunction>()
        if (runWalls.size >= 2) {
            junctions.add(cornerBetween(config.mainWall, secondWall, room))
            if (runWalls.size == 3) junctions.add(cornerBetween(config.mainWall, thirdWall, room))
        }
        val trimsByWall = junctions.flatMap { it.trims }.groupBy { it.wall }

        val elements = mutableListOf<KitchenElement>()
        for (junction in junctions) {
            elements.add(baseCornerUnit(junction, config))
            elements.add(wallCornerUnit(junction, config))
        }

        // Free segments per run wall.
        val segmentsByWall = runWalls.associateWith { wall ->
            freeSegments(wall, room, trimsByWall[wall].orEmpty())
        }
        val mainSegments = segmentsByWall[config.mainWall].orEmpty()
        val mainSegment = mainSegments.maxByOrNull { it.endMm - it.startMm }

        // ---- Phase 1: anchor slots -------------------------------------------
        val anchorsByWall = mutableMapOf<WallSide, MutableList<AnchorSpec>>()
        fun anchorsFor(wall: WallSide) = anchorsByWall.getOrPut(wall) { mutableListOf() }

        var sinkCenter: Vector3? = null
        var stoveCenter: Vector3? = null
        var fridgeCenter: Vector3? = null

        if (mainSegment != null && mainSegment.endMm - mainSegment.startMm >= 1800f) {
            val seg = mainSegment
            val segLen = seg.endMm - seg.startMm
            // Sink: prefer the window centre (when it falls in the middle of
            // the run, 35–55%), else 40% of the run. But the sink must leave
            // room for the stove 1200–1500 mm away, so search positions in
            // 100 mm steps and take the one closest to the preference that
            // actually fits a stove.
            val window = room.walls.firstOrNull { it.side == config.mainWall }
                ?.openings?.firstOrNull { it.type == OpeningType.WINDOW && it.sillHeightMm >= 900f }
            val windowCenter = window?.let { it.offsetMm + it.widthMm / 2f }
            val preferredSink = when {
                windowCenter != null &&
                    windowCenter in (seg.startMm + segLen * 0.35f)..(seg.startMm + segLen * 0.55f) ->
                    windowCenter
                else -> seg.startMm + segLen * 0.4f
            }
            val mainAnchors = anchorsFor(config.mainWall)
            val stoveFits = { sinkC: Float, stoveC: Float ->
                stoveC - 300f >= seg.startMm && stoveC + 300f <= seg.endMm
            }
            var sinkAlong: Float? = null
            var stoveAlong: Float? = null
            var step = seg.startMm + 400f
            while (step <= seg.endMm - 400f + 0.5f) {
                val stoveC = listOf(step + 1500f, step - 1500f, step + 1200f, step - 1200f)
                    .firstOrNull { stoveFits(step, it) }
                if (stoveC != null) {
                    if (sinkAlong == null || kotlin.math.abs(step - preferredSink) < kotlin.math.abs(sinkAlong - preferredSink)) {
                        sinkAlong = step
                        stoveAlong = stoveC
                    }
                }
                step += 100f
            }
            if (sinkAlong == null) {
                sinkAlong = preferredSink.coerceIn(seg.startMm + 400f, seg.endMm - 400f)
                warnings.add("لم يوجد مكان للموقد على الجدار الرئيسي — أضفه يدوياً")
            }
            val sinkCenterAlong = sinkAlong
            mainAnchors.add(AnchorSpec(AnchorKind.SINK, sinkCenterAlong - 400f, sinkCenterAlong + 400f, sinkCenterAlong))
            if (stoveAlong != null) {
                mainAnchors.add(AnchorSpec(AnchorKind.STOVE, stoveAlong - 300f, stoveAlong + 300f, stoveAlong))
            }
            // Fridge: far end of the main run, else the near end, else the
            // far end of the longest side run (reserved before it is filled).
            val fridgeHalf = 425f
            val farEnd = if (sinkCenterAlong < seg.startMm + segLen / 2f) seg.endMm - fridgeHalf else seg.startMm + fridgeHalf
            val nearEnd = if (sinkCenterAlong < seg.startMm + segLen / 2f) seg.startMm + fridgeHalf else seg.endMm - fridgeHalf
            val fridgeOnMain = listOf(farEnd, nearEnd).firstOrNull { c ->
                mainAnchors.none { it.startMm < c + fridgeHalf && it.endMm > c - fridgeHalf }
            }
            if (fridgeOnMain != null) {
                mainAnchors.add(AnchorSpec(AnchorKind.FRIDGE, fridgeOnMain - fridgeHalf, fridgeOnMain + fridgeHalf, fridgeOnMain))
            } else {
                val sideWall = runWalls.firstOrNull { it != config.mainWall }
                val sideSeg = sideWall?.let { segmentsByWall[it]?.maxByOrNull { s -> s.endMm - s.startMm } }
                val sideFridge = if (sideWall != null && sideSeg != null && sideSeg.endMm - sideSeg.startMm >= 900f) {
                    // Near end (right after the corner): keeps the work
                    // triangle tight instead of pushing the fridge to the
                    // far end of the side run.
                    val cornerAtStart = junctions.any { j ->
                        j.trims.any { it.wall == sideWall && it.atStart }
                    }
                    if (cornerAtStart) sideSeg.startMm + fridgeHalf else sideSeg.endMm - fridgeHalf
                } else null
                if (sideFridge != null && sideWall != null) {
                    anchorsFor(sideWall).add(
                        AnchorSpec(AnchorKind.FRIDGE, sideFridge - fridgeHalf, sideFridge + fridgeHalf, sideFridge)
                    )
                } else {
                    warnings.add("لم يوجد مكان للثلاجة في التخطيط — أضفها يدوياً")
                }
            }
            // Dishwasher beside the sink (either side, first free side).
            if (config.includeDishwasher) {
                val dwAlong = listOf(sinkCenterAlong - 700f, sinkCenterAlong + 700f).firstOrNull { c ->
                    c - 300f >= seg.startMm && c + 300f <= seg.endMm &&
                        mainAnchors.none { it.startMm < c + 300f && it.endMm > c - 300f }
                }
                if (dwAlong != null) {
                    mainAnchors.add(AnchorSpec(AnchorKind.DISHWASHER, dwAlong - 300f, dwAlong + 300f, dwAlong))
                }
            }
            // Optional tall pantry next to the fridge on the main run.
            if (config.includeTallPantry) {
                val fridgeSpec = mainAnchors.firstOrNull { it.kind == AnchorKind.FRIDGE }
                if (fridgeSpec != null) {
                    val dir = if (fridgeSpec.centerAlongMm > seg.startMm + segLen / 2f) -1f else 1f
                    val pantryAlong = fridgeSpec.centerAlongMm + dir * (fridgeHalf + 300f)
                    if (pantryAlong - 300f >= seg.startMm && pantryAlong + 300f <= seg.endMm &&
                        mainAnchors.none { it.startMm < pantryAlong + 300f && it.endMm > pantryAlong - 300f }
                    ) {
                        mainAnchors.add(AnchorSpec(AnchorKind.PANTRY, pantryAlong - 300f, pantryAlong + 300f, pantryAlong))
                    }
                }
            }
        } else {
            warnings.add("الجدار الرئيسي لا يتسع للحوض والموقد معاً — جرّب غرفة أكبر أو خطاً واحداً")
        }

        // ---- Phase 2: build every run ----------------------------------------
        for (wall in runWalls) {
            for (segment in segmentsByWall[wall].orEmpty()) {
                val placed = buildRun(
                    wall = wall,
                    segment = segment,
                    anchors = anchorsByWall[wall].orEmpty()
                        .filter { it.centerAlongMm in segment.startMm..segment.endMm },
                    room = room,
                    config = config
                )
                elements.addAll(placed.elements)
                if (wall == config.mainWall) {
                    sinkCenter = placed.sinkCenter ?: sinkCenter
                    stoveCenter = placed.stoveCenter ?: stoveCenter
                    fridgeCenter = placed.fridgeCenter ?: fridgeCenter
                } else if (fridgeCenter == null) {
                    fridgeCenter = placed.fridgeCenter
                }
            }
        }

        // Work-triangle summary (plan distance).
        var sinkToStove: Float? = null
        var perimeter: Float? = null
        if (sinkCenter != null && stoveCenter != null) {
            sinkToStove = planDistance(sinkCenter, stoveCenter)
            if (sinkToStove < 1200f || sinkToStove > 2700f) {
                warnings.add(
                    "المسافة حوض–موقد ${sinkToStove.toInt()} مم خارج النطاق المثالي (1200–2700 مم)"
                )
            }
            if (fridgeCenter != null) {
                perimeter = sinkToStove +
                    planDistance(sinkCenter, fridgeCenter) +
                    planDistance(stoveCenter, fridgeCenter)
                if (perimeter < 3400f || perimeter > 7900f) {
                    warnings.add(
                        "محيط مثلث العمل ${perimeter.toInt()} مم خارج النطاق المثالي (3400–7900 مم)"
                    )
                }
            }
        }

        return SmartLayoutResult(
            elements = elements,
            shapeUsed = shape,
            sinkToStoveMm = sinkToStove,
            trianglePerimeterMm = perimeter,
            warnings = warnings
        )
    }

    // ---- geometry helpers ---------------------------------------------------

    data class WallInterval(val startMm: Float, val endMm: Float)

    private enum class AnchorKind { SINK, STOVE, FRIDGE, DISHWASHER, PANTRY }

    private data class AnchorSpec(
        val kind: AnchorKind,
        val startMm: Float,
        val endMm: Float,
        val centerAlongMm: Float
    )

    private data class CornerPlacement(
        val wall: WallSide,
        val atStart: Boolean // true = corner at the run's start end
    )

    private data class CornerJunction(
        val centerX: Float,
        val centerZ: Float,
        val rotation: Float,
        val trims: List<CornerPlacement>
    )

    private data class RunPlacement(
        val elements: List<KitchenElement>,
        val sinkCenter: Vector3?,
        val stoveCenter: Vector3?,
        val fridgeCenter: Vector3?
    )

    fun wallLength(side: WallSide, room: RoomStructure): Float = when (side) {
        WallSide.NORTH, WallSide.SOUTH -> room.widthMm
        WallSide.EAST, WallSide.WEST -> room.depthMm
    }

    /** Clockwise neighbour: NORTH→EAST→SOUTH→WEST→NORTH. */
    fun clockwiseNeighbour(side: WallSide): WallSide = when (side) {
        WallSide.NORTH -> WallSide.EAST
        WallSide.EAST -> WallSide.SOUTH
        WallSide.SOUTH -> WallSide.WEST
        WallSide.WEST -> WallSide.NORTH
    }

    fun counterClockwiseNeighbour(side: WallSide): WallSide = when (side) {
        WallSide.NORTH -> WallSide.WEST
        WallSide.WEST -> WallSide.SOUTH
        WallSide.SOUTH -> WallSide.EAST
        WallSide.EAST -> WallSide.NORTH
    }

    fun wallRotation(side: WallSide): Float = when (side) {
        WallSide.NORTH -> 0f
        WallSide.SOUTH -> 180f
        WallSide.WEST -> 90f
        WallSide.EAST -> 270f
    }

    /** Centre of an element sitting [depthFromWallMm] off [side], [alongMm] along it. */
    fun placeOnWall(
        side: WallSide,
        alongMm: Float,
        depthFromWallMm: Float,
        room: RoomStructure,
        yMm: Float = 0f
    ): Vector3 = when (side) {
        WallSide.NORTH -> Vector3(alongMm, yMm, depthFromWallMm)
        WallSide.SOUTH -> Vector3(alongMm, yMm, room.depthMm - depthFromWallMm)
        WallSide.WEST -> Vector3(depthFromWallMm, yMm, alongMm)
        WallSide.EAST -> Vector3(room.widthMm - depthFromWallMm, yMm, alongMm)
    }

    private fun planDistance(a: Vector3, b: Vector3): Float {
        val dx = a.x - b.x
        val dz = a.z - b.z
        return kotlin.math.sqrt(dx * dx + dz * dz)
    }

    /** Corner shared by two adjacent walls: its plan centre + the 900 mm trim on each run. */
    private fun cornerBetween(main: WallSide, other: WallSide, room: RoomStructure): CornerJunction {
        val corner = when (main to other) {
            WallSide.NORTH to WallSide.EAST -> "NE"
            WallSide.NORTH to WallSide.WEST -> "NW"
            WallSide.SOUTH to WallSide.EAST -> "SE"
            WallSide.SOUTH to WallSide.WEST -> "SW"
            WallSide.EAST to WallSide.SOUTH -> "SE"
            WallSide.EAST to WallSide.NORTH -> "NE"
            WallSide.WEST to WallSide.SOUTH -> "SW"
            else -> "NW" // WEST→NORTH
        }
        val (cx, cz) = when (corner) {
            "NE" -> (room.widthMm - 450f) to 450f
            "NW" -> 450f to 450f
            "SE" -> (room.widthMm - 450f) to (room.depthMm - 450f)
            else -> 450f to (room.depthMm - 450f) // SW
        }
        // A wall's along-axis origin: NORTH/SOUTH at x=0 (west end),
        // WEST/EAST at z=0 (north end). The junction trims the end that
        // holds the corner: NW trims the start of NORTH and WEST, NE trims
        // the end of NORTH and the start of EAST, and so on.
        fun atStartFor(wall: WallSide): Boolean = when (wall) {
            WallSide.NORTH -> corner == "NW"
            WallSide.SOUTH -> corner == "SW"
            WallSide.WEST -> corner == "NW"
            WallSide.EAST -> corner == "NE"
        }
        return CornerJunction(
            centerX = cx,
            centerZ = cz,
            rotation = wallRotation(main),
            trims = listOf(
                CornerPlacement(main, atStartFor(main)),
                CornerPlacement(other, atStartFor(other))
            )
        )
    }

    /** Free along-axis intervals on a wall after door reservations + corner trims. */
    private fun freeSegments(
        side: WallSide,
        room: RoomStructure,
        corners: List<CornerPlacement>
    ): List<WallInterval> {
        val len = wallLength(side, room)
        var intervals = listOf(WallInterval(0f, len))
        // Door openings: reserve the span + 200 mm swing margin each side.
        val wall = room.walls.firstOrNull { it.side == side }
        for (opening in wall?.openings.orEmpty()) {
            if (opening.type != OpeningType.DOOR) continue
            val blockStart = (opening.offsetMm - 200f).coerceAtLeast(0f)
            val blockEnd = (opening.endOffsetMm + 200f).coerceAtMost(len)
            intervals = intervals.flatMap { iv ->
                when {
                    blockEnd <= iv.startMm || blockStart >= iv.endMm -> listOf(iv)
                    blockStart <= iv.startMm && blockEnd >= iv.endMm -> emptyList()
                    blockStart <= iv.startMm -> listOf(WallInterval(blockEnd, iv.endMm))
                    blockEnd >= iv.endMm -> listOf(WallInterval(iv.startMm, blockStart))
                    else -> listOf(
                        WallInterval(iv.startMm, blockStart),
                        WallInterval(blockEnd, iv.endMm)
                    )
                }
            }
        }
        // Corner trims: 900 mm at the junction end.
        for (corner in corners) {
            intervals = intervals.map { iv ->
                if (corner.atStart) WallInterval(iv.startMm + 900f, iv.endMm)
                else WallInterval(iv.startMm, iv.endMm - 900f)
            }.filter { it.endMm - it.startMm >= 300f }
        }
        return intervals.filter { it.endMm - it.startMm >= 300f }
    }

    // ---- run building ---------------------------------------------------------

    private fun baseCornerUnit(junction: CornerJunction, config: SmartLayoutConfig): KitchenElement =
        KitchenElement(
            id = "sl_corner_${UUID.randomUUID().toString().take(8)}",
            name = "خزانة ركنية زاوية",
            type = KitchenElementType.BASE_CORNER_L,
            position = Vector3(junction.centerX, 0f, junction.centerZ),
            rotationY = junction.rotation,
            dimensions = Vector3(900f, 850f, 900f),
            material = config.cabinetMaterial,
            countertopMaterial = config.countertopMaterial
        )

    private fun wallCornerUnit(junction: CornerJunction, config: SmartLayoutConfig): KitchenElement =
        KitchenElement(
            id = "sl_wcorner_${UUID.randomUUID().toString().take(8)}",
            name = "خزانة ركنية علوية",
            type = KitchenElementType.WALL_CORNER_L,
            position = Vector3(junction.centerX, 1450f, junction.centerZ),
            rotationY = junction.rotation,
            dimensions = Vector3(650f, 720f, 650f),
            material = config.cabinetMaterial
        )

    private fun baseUnit(
        side: WallSide,
        alongCenterMm: Float,
        widthMm: Float,
        type: KitchenElementType,
        name: String,
        room: RoomStructure,
        config: SmartLayoutConfig,
        countertop: Boolean = true
    ): KitchenElement = KitchenElement(
        id = "sl_${UUID.randomUUID().toString().take(8)}",
        name = name,
        type = type,
        position = placeOnWall(side, alongCenterMm, 300f, room),
        rotationY = wallRotation(side),
        dimensions = Vector3(widthMm, type.defaultDimensions.y, 600f),
        material = config.cabinetMaterial,
        countertopMaterial = if (countertop) config.countertopMaterial else null
    )

    private fun wallUnit(
        side: WallSide,
        alongCenterMm: Float,
        widthMm: Float,
        room: RoomStructure,
        config: SmartLayoutConfig
    ): KitchenElement = KitchenElement(
        id = "sl_${UUID.randomUUID().toString().take(8)}",
        name = "خزانة علوية",
        type = KitchenElementType.WALL_SINGLE_DOOR,
        position = placeOnWall(side, alongCenterMm, 175f, room, 1450f),
        rotationY = wallRotation(side),
        dimensions = Vector3(widthMm, 720f, 350f),
        material = config.cabinetMaterial
    )

    /**
     * Builds one free segment of one run: places the segment's anchors, fills
     * the gaps between them with base units, and the wall row above (skipping
     * the hood span, tall-unit spans and window spans).
     */
    private fun buildRun(
        wall: WallSide,
        segment: WallInterval,
        anchors: List<AnchorSpec>,
        room: RoomStructure,
        config: SmartLayoutConfig
    ): RunPlacement {
        val elements = mutableListOf<KitchenElement>()
        var sinkCenter: Vector3? = null
        var stoveCenter: Vector3? = null
        var fridgeCenter: Vector3? = null

        for (anchor in anchors.sortedBy { it.startMm }) {
            when (anchor.kind) {
                AnchorKind.SINK -> {
                    val el = baseUnit(wall, anchor.centerAlongMm, 800f, KitchenElementType.BASE_SINK_UNIT, "وحدة حوض الغسيل بالخلاط", room, config)
                    elements.add(el)
                    sinkCenter = el.position
                }
                AnchorKind.STOVE -> {
                    val oven = baseUnit(wall, anchor.centerAlongMm, 600f, KitchenElementType.BASE_UNDER_OVEN, "خزانة فرن سفلي بفتحة تهوية", room, config)
                    elements.add(oven)
                    stoveCenter = oven.position
                    elements.add(
                        KitchenElement(
                            id = "sl_${UUID.randomUUID().toString().take(8)}",
                            name = "مسطح طهي سيراميك حثي",
                            type = KitchenElementType.COOKTOP_INDUCTION,
                            position = placeOnWall(wall, anchor.centerAlongMm, 300f, room, 850f),
                            rotationY = wallRotation(wall),
                            dimensions = Vector3(600f, 55f, 520f),
                            material = config.cabinetMaterial
                        )
                    )
                    elements.add(
                        KitchenElement(
                            id = "sl_${UUID.randomUUID().toString().take(8)}",
                            name = "شفاط مطبخ",
                            type = KitchenElementType.RANGE_HOOD_BOX,
                            position = placeOnWall(wall, anchor.centerAlongMm, 250f, room, 1620f),
                            rotationY = wallRotation(wall),
                            dimensions = Vector3(900f, 420f, 500f),
                            material = config.cabinetMaterial
                        )
                    )
                }
                AnchorKind.FRIDGE -> {
                    val el = KitchenElement(
                        id = "sl_${UUID.randomUUID().toString().take(8)}",
                        name = "إطار دولاب ثلاجة مدمجة",
                        type = KitchenElementType.TALL_FRIDGE_HOUSING,
                        position = placeOnWall(wall, anchor.centerAlongMm, 325f, room),
                        rotationY = wallRotation(wall),
                        dimensions = Vector3(850f, 2170f, 650f),
                        material = config.cabinetMaterial
                    )
                    elements.add(el)
                    fridgeCenter = el.position
                }
                AnchorKind.DISHWASHER -> {
                    elements.add(
                        baseUnit(wall, anchor.centerAlongMm, 600f, KitchenElementType.DISHWASHER_INTEGRATED, "غسالة صحون مدمجة", room, config, countertop = false)
                    )
                }
                AnchorKind.PANTRY -> {
                    elements.add(
                        KitchenElement(
                            id = "sl_${UUID.randomUUID().toString().take(8)}",
                            name = "دولاب مؤونة وتخزين عمودي",
                            type = KitchenElementType.TALL_PANTRY,
                            position = placeOnWall(wall, anchor.centerAlongMm, 300f, room),
                            rotationY = wallRotation(wall),
                            dimensions = Vector3(600f, 2170f, 600f),
                            material = config.cabinetMaterial
                        )
                    )
                }
            }
        }

        // Fill the gaps between anchors with base units.
        val sortedAnchors = anchors.sortedBy { it.startMm }
        var cursor = segment.startMm
        for (anchor in sortedAnchors) {
            if (anchor.startMm > cursor) fillBaseGap(WallInterval(cursor, anchor.startMm), wall, room, config, elements)
            cursor = maxOf(cursor, anchor.endMm)
        }
        if (segment.endMm > cursor) fillBaseGap(WallInterval(cursor, segment.endMm), wall, room, config, elements)

        // Wall cabinets over the same segment, skipping the hood span, tall
        // spans and window spans whose top is above the wall-cabinet bottom.
        val blocked = mutableListOf<WallInterval>()
        for (anchor in anchors) {
            when (anchor.kind) {
                AnchorKind.STOVE -> blocked.add(WallInterval(anchor.centerAlongMm - 450f, anchor.centerAlongMm + 450f))
                AnchorKind.FRIDGE -> blocked.add(WallInterval(anchor.startMm, anchor.endMm))
                AnchorKind.PANTRY -> blocked.add(WallInterval(anchor.startMm, anchor.endMm))
                else -> Unit
            }
        }
        val wallEl = room.walls.firstOrNull { it.side == wall }
        for (opening in wallEl?.openings.orEmpty()) {
            if (opening.type == OpeningType.WINDOW && opening.lintelHeightMm > 1450f) {
                blocked.add(WallInterval(opening.offsetMm, opening.endOffsetMm))
            }
        }
        var wCursor = segment.startMm
        for (b in blocked.sortedBy { it.startMm }) {
            if (b.startMm > wCursor) fillWallGap(WallInterval(wCursor, b.startMm), wall, room, config, elements)
            wCursor = maxOf(wCursor, b.endMm)
        }
        if (segment.endMm > wCursor) fillWallGap(WallInterval(wCursor, segment.endMm), wall, room, config, elements)

        return RunPlacement(elements, sinkCenter, stoveCenter, fridgeCenter)
    }

    /** Fill a base gap with 600 mm units + one custom-width unit / filler strip. */
    private fun fillBaseGap(
        gap: WallInterval,
        wall: WallSide,
        room: RoomStructure,
        config: SmartLayoutConfig,
        out: MutableList<KitchenElement>
    ) {
        var cursor = gap.startMm
        var remaining = gap.endMm - gap.startMm
        while (remaining >= 600f) {
            val useDouble = remaining in 850f..950f
            val w = if (useDouble) 900f else 600f
            val type = if (useDouble) KitchenElementType.BASE_DOUBLE_DOOR else KitchenElementType.BASE_SINGLE_DOOR
            val name = if (useDouble) "خزانة درفتين" else "خزانة باب مفرد"
            out.add(baseUnit(wall, cursor + w / 2f, w, type, name, room, config))
            cursor += w
            remaining -= w
        }
        if (remaining >= 300f) {
            out.add(
                baseUnit(
                    wall, cursor + remaining / 2f, remaining,
                    KitchenElementType.BASE_SINGLE_DOOR,
                    "خزانة باب مفرد (${remaining.toInt()} مم)",
                    room, config
                )
            )
        } else if (remaining >= 50f) {
            out.add(
                KitchenElement(
                    id = "sl_${UUID.randomUUID().toString().take(8)}",
                    name = "شريحة فيلر",
                    type = KitchenElementType.BASE_FILLER_STRIP,
                    position = placeOnWall(wall, cursor + remaining / 2f, 300f, room),
                    rotationY = wallRotation(wall),
                    dimensions = Vector3(remaining, 850f, 600f),
                    material = config.cabinetMaterial
                )
            )
        }
    }

    /** Fill a wall gap with 600 mm wall units + one custom-width unit. */
    private fun fillWallGap(
        gap: WallInterval,
        wall: WallSide,
        room: RoomStructure,
        config: SmartLayoutConfig,
        out: MutableList<KitchenElement>
    ) {
        var cursor = gap.startMm
        var remaining = gap.endMm - gap.startMm
        while (remaining >= 600f) {
            out.add(wallUnit(wall, cursor + 300f, 600f, room, config))
            cursor += 600f
            remaining -= 600f
        }
        if (remaining >= 300f) {
            out.add(wallUnit(wall, cursor + remaining / 2f, remaining, room, config))
        }
    }
}
