package com.example.domain.model

import com.example.core.filament.CinematicTimeOfDay
import com.example.core.units.MeasurementUnitSystem

/**
 * Global Studio v2.0 — the six reference kitchens.
 *
 * Each entry is traced from one of the six reference photos Abd sent
 * (23 Nov 2026), in the order they arrived. Dimensions are read off the
 * photo where the photo is annotated, and are honest showroom estimates
 * where the photo has no ruler — the dialog labels them as such in the
 * README, and [dimensionsAreMeasured] tells the UI which is which.
 *
 *  1. COMPACT_L_300 — photo 1, annotated in Portuguese: 3,00 m long,
 *     2,40 m deep, 2,50 m high, upper cabinets 98 cm, fridge 1,90 m,
 *     microwave niche 60×45 cm, counter 60 cm deep.
 *  2. CLASSIC_WHITEWASH_L — photo 2: big whitewashed-wood L, black
 *     granite, ornate crown + floral glass, yellow LED cove ceiling.
 *  3. CHAMPAGNE_SHOWCASE — photo 3: champagne high-gloss, black-frame
 *     bronze-glass showcase with vertical LED strips, marble floor.
 *  4. WHITE_SHAKER_U — photo 4: white shaker/louvred U, black
 *     countertop, black-frame glass cabinets, pendant + tray ceiling.
 *  5. LIGHT_OAK_NICHES — photo 5: light oak, speckled granite, open
 *     wall niches, tall unit with microwave niche, island.
 *  6. MODERN_ISLAND_BAR — photo 6: cream + wood island with lit
 *     niches, black countertop, glass showcase, marble floor, LED cove.
 *
 * Applying a preset sets room size + layout + wall/floor + cabinet and
 * countertop finishes on every element + lighting + camera in one tap
 * (ViewModel.applyReferencePreset). That closes the v1.1 gap where a
 * style changed wall/floor only and left cabinets as a text suggestion.
 */
enum class ReferenceKitchenPreset(
    val titleAr: String,
    val titleEn: String,
    val photoAr: String,
    val widthMm: Float,
    val depthMm: Float,
    val heightMm: Float,
    val layoutShape: RoomLayoutShape,
    val wallFinish: MaterialFinish,
    val floorMaterial: MaterialFinish,
    val cabinetFinish: MaterialFinish,
    val countertopFinish: MaterialFinish,
    val hardwareAr: String,
    val hardwareEn: String,
    val lightingKelvin: Float,
    val timeOfDay: CinematicTimeOfDay,
    val cameraYaw: Float,
    val cameraPitch: Float,
    val cameraZoom: Float,
    val fridgeHeightMm: Float,
    val upperCabinetHeightMm: Float,
    val hasIsland: Boolean,
    val hasGlassShowcase: Boolean,
    val hasOpenNiches: Boolean,
    val dimensionsAreMeasured: Boolean
) {
    COMPACT_L_300(
        titleAr = "مطبخ L مدمج 3.00 × 2.40 م 🇧🇷",
        titleEn = "Compact L Kitchen 3.00 × 2.40 m",
        photoAr = "الصورة 1 — مخطط بأبعاد برتغالية: طول 3.00 م، عمق 2.40 م، ارتفاع 2.50 م",
        widthMm = 3000f, depthMm = 2400f, heightMm = 2500f,
        layoutShape = RoomLayoutShape.L_SHAPE,
        wallFinish = MaterialFinish.WALL_WARM_IVORY,
        floorMaterial = MaterialFinish.MARBLE_WHITE_FLOOR,
        cabinetFinish = MaterialFinish.CAPPUCCINO_MATTE,
        countertopFinish = MaterialFinish.CARRARA_MARBLE,
        hardwareAr = "مقابض ألمنيوم مخفية",
        hardwareEn = "Recessed aluminium handles",
        lightingKelvin = 4000f,
        timeOfDay = CinematicTimeOfDay.MIDDAY_SUN,
        cameraYaw = 35f, cameraPitch = 30f, cameraZoom = 1.0f,
        fridgeHeightMm = 1900f, upperCabinetHeightMm = 980f,
        hasIsland = false, hasGlassShowcase = false, hasOpenNiches = false,
        dimensionsAreMeasured = true
    ),
    CLASSIC_WHITEWASH_L(
        titleAr = "كلاسيكي خشب أبيض مغسول — حرف L",
        titleEn = "Classic Whitewashed Wood L",
        photoAr = "الصورة 2 — خشب أبيض مغسول، جرانيت أسود، زخارف تاجية وزجاج مزهر، سقف LED أصفر",
        widthMm = 5400f, depthMm = 4200f, heightMm = 3000f,
        layoutShape = RoomLayoutShape.L_SHAPE,
        wallFinish = MaterialFinish.WALL_WARM_IVORY,
        floorMaterial = MaterialFinish.PARQUET_OAK_FLOOR,
        cabinetFinish = MaterialFinish.ASH_WOOD,
        countertopFinish = MaterialFinish.BLACK_GRANITE,
        hardwareAr = "مقابض خشبية داكنة كلاسيكية",
        hardwareEn = "Dark classic wooden handles",
        lightingKelvin = 3000f,
        timeOfDay = CinematicTimeOfDay.NIGHT_LUXURY,
        cameraYaw = 40f, cameraPitch = 28f, cameraZoom = 0.7f,
        fridgeHeightMm = 1900f, upperCabinetHeightMm = 900f,
        hasIsland = false, hasGlassShowcase = true, hasOpenNiches = false,
        dimensionsAreMeasured = false
    ),
    CHAMPAGNE_SHOWCASE(
        titleAr = "شامبين لامع + فيترينة زجاجية مضيئة",
        titleEn = "Champagne Gloss + Lit Glass Showcase",
        photoAr = "الصورة 3 — شامبين لامع، فيترينة برونزية بإطار أسود وشرائط LED عمودية، أرضية رخام",
        widthMm = 4200f, depthMm = 3600f, heightMm = 2800f,
        layoutShape = RoomLayoutShape.RECTANGULAR,
        wallFinish = MaterialFinish.WALL_GREIGE_CASHMERE,
        floorMaterial = MaterialFinish.MARBLE_WHITE_FLOOR,
        cabinetFinish = MaterialFinish.CHAMPAGNE_GLOSS,
        countertopFinish = MaterialFinish.CARRARA_MARBLE,
        hardwareAr = "مقابض مخفية بدون بروز",
        hardwareEn = "Handleless, flush fronts",
        lightingKelvin = 3000f,
        timeOfDay = CinematicTimeOfDay.NIGHT_LUXURY,
        cameraYaw = 30f, cameraPitch = 25f, cameraZoom = 1.1f,
        fridgeHeightMm = 1850f, upperCabinetHeightMm = 900f,
        hasIsland = false, hasGlassShowcase = true, hasOpenNiches = false,
        dimensionsAreMeasured = false
    ),
    WHITE_SHAKER_U(
        titleAr = "أبيض شيكر — حرف U",
        titleEn = "White Shaker U-Shape",
        photoAr = "الصورة 4 — أبيض شيكر بشرائح، سطح أسود، خزائن زجاجية بإطار أسود، ثريا وسقف متدرج",
        widthMm = 4800f, depthMm = 3600f, heightMm = 2800f,
        layoutShape = RoomLayoutShape.U_SHAPE,
        wallFinish = MaterialFinish.WALL_PEARL_GREY,
        floorMaterial = MaterialFinish.MARBLE_WHITE_FLOOR,
        cabinetFinish = MaterialFinish.WHITE_HIGH_GLOSS,
        countertopFinish = MaterialFinish.BLACK_GRANITE,
        hardwareAr = "مقابض سوداء طويلة",
        hardwareEn = "Long black bar handles",
        lightingKelvin = 4000f,
        timeOfDay = CinematicTimeOfDay.GOLDEN_HOUR,
        cameraYaw = 0f, cameraPitch = 30f, cameraZoom = 0.8f,
        fridgeHeightMm = 1900f, upperCabinetHeightMm = 900f,
        hasIsland = false, hasGlassShowcase = true, hasOpenNiches = false,
        dimensionsAreMeasured = false
    ),
    LIGHT_OAK_NICHES(
        titleAr = "بلوط فاتح + رفوف مفتوحة",
        titleEn = "Light Oak + Open Niches",
        photoAr = "الصورة 5 — بلوط فاتح، جرانيت مرقّط، رفوف جدارية مفتوحة، وحدة طويلة بمكان ميكروويف، جزيرة",
        widthMm = 4200f, depthMm = 3400f, heightMm = 2700f,
        layoutShape = RoomLayoutShape.L_SHAPE,
        wallFinish = MaterialFinish.WALL_SOFT_TAUPE,
        floorMaterial = MaterialFinish.MARBLE_WHITE_FLOOR,
        cabinetFinish = MaterialFinish.NATURAL_OAK,
        countertopFinish = MaterialFinish.BLACK_GRANITE,
        hardwareAr = "مقابض سوداء عمودية",
        hardwareEn = "Vertical black handles",
        lightingKelvin = 3500f,
        timeOfDay = CinematicTimeOfDay.MIDDAY_SUN,
        cameraYaw = 35f, cameraPitch = 30f, cameraZoom = 1.0f,
        fridgeHeightMm = 1900f, upperCabinetHeightMm = 850f,
        hasIsland = true, hasGlassShowcase = false, hasOpenNiches = true,
        dimensionsAreMeasured = false
    ),
    MODERN_ISLAND_BAR(
        titleAr = "عصري بجزيرة وبار مضيء",
        titleEn = "Modern Island + Lit Bar",
        photoAr = "الصورة 6 — جزيرة كريمي/خشب برفوف مضيئة، سطح أسود، فيترينة زجاجية، أرضية رخام، سقف LED",
        widthMm = 5200f, depthMm = 4200f, heightMm = 3000f,
        layoutShape = RoomLayoutShape.RECTANGULAR,
        wallFinish = MaterialFinish.WALL_WARM_IVORY,
        floorMaterial = MaterialFinish.MARBLE_WHITE_FLOOR,
        cabinetFinish = MaterialFinish.WHITE_HIGH_GLOSS,
        countertopFinish = MaterialFinish.BLACK_GRANITE,
        hardwareAr = "مقابض سوداء مخفية",
        hardwareEn = "Hidden black handles",
        lightingKelvin = 3000f,
        timeOfDay = CinematicTimeOfDay.NIGHT_LUXURY,
        cameraYaw = 35f, cameraPitch = 28f, cameraZoom = 0.75f,
        fridgeHeightMm = 1900f, upperCabinetHeightMm = 900f,
        hasIsland = true, hasGlassShowcase = true, hasOpenNiches = true,
        dimensionsAreMeasured = false
    );

    /** Room size + layout + wall/floor for this preset (undoable via commands). */
    fun applyTo(room: RoomStructure): RoomStructure =
        room.updateDimensions(widthMm, depthMm, heightMm)
            .applyLayoutShape(layoutShape)
            .copy(wallFinish = wallFinish, floorMaterial = floorMaterial)

    fun dimensionsText(unitSystem: MeasurementUnitSystem): String =
        "${MeasurementUnitSystem.formatLength(widthMm, unitSystem)} × " +
            "${MeasurementUnitSystem.formatLength(depthMm, unitSystem)} × " +
            "${MeasurementUnitSystem.formatLength(heightMm, unitSystem)}"

    fun featuresAr(): String {
        val parts = mutableListOf<String>()
        if (hasIsland) parts += "جزيرة"
        if (hasGlassShowcase) parts += "فيترينة زجاجية"
        if (hasOpenNiches) parts += "رفوف مفتوحة"
        parts += "ثلاجة ${(fridgeHeightMm / 1000f)} م"
        parts += "علوي ${(upperCabinetHeightMm / 10f).toInt()} سم"
        if (dimensionsAreMeasured) parts += "أبعاد مقاسة من الصورة"
        return parts.joinToString(" • ")
    }

    fun summaryAr(): String =
        "$titleAr — ${dimensionsText(MeasurementUnitSystem.METRIC)} • " +
            "خزائن: ${cabinetFinish.name} • سطح: ${countertopFinish.name} • " +
            "إضاءة: ${lightingKelvin.toInt()}K • ${timeOfDay.titleAr}"
}
