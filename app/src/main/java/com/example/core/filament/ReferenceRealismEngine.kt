package com.example.core.filament

import com.example.core.math.Vector3
import com.example.domain.model.ReferenceKitchenPreset

/**
 * Global Studio v2.0 — photorealism upgrade traced from the reference photos.
 *
 * What the photos actually show, and what this engine encodes:
 *  - Photo 3 & 6: polished marble floors act like mirrors — high
 *    [RealismSettings.floorReflection], low roughness on the floor.
 *  - Photo 3 & 6: vertical LED strips inside the bronze-glass showcase
 *    and under the island niches — [ledGlow] near 1.0, warm 3000K.
 *  - Photo 2: warm yellow cove ceiling, soft wide shadows, almost no
 *    hard sun — high [softShadowRadius], high [ambientOcclusion].
 *  - Photo 4: black galaxy granite + black-frame glass — needs
 *    [reflections] and [glassRefraction] or the black reads flat.
 *  - Photo 1: bright midday white — low bloom, ACES on, denoiser on.
 *
 * Pure math + data, no Android dependency. The settings are stored in
 * the ViewModel and applied to the live lighting simulation immediately
 * (sun/LED/camera) and to the 4K export post-process in full
 * ([CinematicPostProcessConfig] via [toPostProcessConfig]). Honest limit,
 * kept from v1.1: the live Canvas preview cannot run a true path tracer,
 * so bloom / grain / AO are previewed as graded values and rendered in
 * full only at export — the dialog says so.
 */
enum class RenderQuality(
    val titleAr: String,
    val titleEn: String,
    val samplesPerPixel: Int,
    val maxExportPx: Int
) {
    PREVIEW("معاينة سريعة", "Fast preview", 16, 1920),
    BALANCED("متوازنة", "Balanced", 64, 2560),
    ULTRA("فائقة", "Ultra", 256, 3840),
    FOUR_K("سينمائية 4K", "Cinematic 4K", 512, 3840);

    fun summaryAr(): String = "$titleAr — $samplesPerPixel عينة/بكسل، حتى ${maxExportPx}p"
}

data class RealismSettings(
    val softShadows: Boolean = true,
    val softShadowRadius: Float = 0.65f, // 0..1 — wide soft penumbra like photo 2
    val ambientOcclusion: Float = 0.70f, // 0..1 — contact darkening in corners
    val contactShadows: Boolean = true,
    val reflections: Float = 0.75f, // 0..1 — chrome / steel / glass
    val floorReflection: Float = 0.60f, // 0..1 — polished marble mirror (photos 3, 6)
    val ledGlow: Float = 0.70f, // 0..1.5 — LED strip halo (photos 3, 6)
    val acesToneMapping: Boolean = true,
    val denoiser: Boolean = true,
    val glassRefraction: Boolean = true,
    val bloom: Float = 0.65f, // 0..1.5
    val vignette: Float = 0.35f, // 0..1
    val filmGrain: Float = 0.10f, // 0..1
    val quality: RenderQuality = RenderQuality.BALANCED
) {
    /** Everything clamped to its physical range — a slider can never break the render. */
    fun sanitized(): RealismSettings = copy(
        softShadowRadius = softShadowRadius.coerceIn(0f, 1f),
        ambientOcclusion = ambientOcclusion.coerceIn(0f, 1f),
        reflections = reflections.coerceIn(0f, 1f),
        floorReflection = floorReflection.coerceIn(0f, 1f),
        ledGlow = ledGlow.coerceIn(0f, 1.5f),
        bloom = bloom.coerceIn(0f, 1.5f),
        vignette = vignette.coerceIn(0f, 1f),
        filmGrain = filmGrain.coerceIn(0f, 1f)
    )

    /** Post-process values the existing 4K exporter already understands. */
    fun toPostProcessConfig(): CinematicPostProcessConfig = CinematicPostProcessConfig(
        bloomIntensity = (bloom + ledGlow * 0.25f).coerceIn(0f, 1.5f),
        vignetteStrength = vignette.coerceIn(0f, 1f),
        warmthShift = 0.14f,
        contrastBoost = 1.14f,
        cinematicLetterbox = false,
        includeCountertopStaging = true
    )

    fun summaryAr(): String {
        val parts = mutableListOf<String>()
        if (softShadows) parts += "ظلال ناعمة ${(softShadowRadius * 100).toInt()}%"
        parts += "AO ${(ambientOcclusion * 100).toInt()}%"
        parts += "انعكاس أرضية ${(floorReflection * 100).toInt()}%"
        parts += "LED ${(ledGlow * 100).toInt()}%"
        if (acesToneMapping) parts += "ACES"
        parts += quality.titleAr
        return parts.joinToString(" • ")
    }
}

object ReferenceRealismEngine {

    /** One-tap realism for each reference photo — the photo's light, in numbers. */
    fun forPreset(preset: ReferenceKitchenPreset): RealismSettings = when (preset) {
        ReferenceKitchenPreset.COMPACT_L_300 -> RealismSettings(
            softShadows = true, softShadowRadius = 0.45f,
            ambientOcclusion = 0.55f, contactShadows = true,
            reflections = 0.60f, floorReflection = 0.55f,
            ledGlow = 0.40f, acesToneMapping = true, denoiser = true,
            glassRefraction = false, bloom = 0.45f, vignette = 0.22f,
            filmGrain = 0.05f, quality = RenderQuality.BALANCED
        )
        ReferenceKitchenPreset.CLASSIC_WHITEWASH_L -> RealismSettings(
            softShadows = true, softShadowRadius = 0.90f,
            ambientOcclusion = 0.80f, contactShadows = true,
            reflections = 0.45f, floorReflection = 0.30f,
            ledGlow = 0.85f, acesToneMapping = true, denoiser = true,
            glassRefraction = true, bloom = 0.75f, vignette = 0.40f,
            filmGrain = 0.12f, quality = RenderQuality.ULTRA
        )
        ReferenceKitchenPreset.CHAMPAGNE_SHOWCASE -> RealismSettings(
            softShadows = true, softShadowRadius = 0.60f,
            ambientOcclusion = 0.70f, contactShadows = true,
            reflections = 0.90f, floorReflection = 0.90f,
            ledGlow = 1.0f, acesToneMapping = true, denoiser = true,
            glassRefraction = true, bloom = 0.85f, vignette = 0.38f,
            filmGrain = 0.08f, quality = RenderQuality.ULTRA
        )
        ReferenceKitchenPreset.WHITE_SHAKER_U -> RealismSettings(
            softShadows = true, softShadowRadius = 0.55f,
            ambientOcclusion = 0.65f, contactShadows = true,
            reflections = 0.80f, floorReflection = 0.75f,
            ledGlow = 0.60f, acesToneMapping = true, denoiser = true,
            glassRefraction = true, bloom = 0.60f, vignette = 0.30f,
            filmGrain = 0.07f, quality = RenderQuality.BALANCED
        )
        ReferenceKitchenPreset.LIGHT_OAK_NICHES -> RealismSettings(
            softShadows = true, softShadowRadius = 0.50f,
            ambientOcclusion = 0.60f, contactShadows = true,
            reflections = 0.55f, floorReflection = 0.60f,
            ledGlow = 0.45f, acesToneMapping = true, denoiser = true,
            glassRefraction = false, bloom = 0.50f, vignette = 0.25f,
            filmGrain = 0.06f, quality = RenderQuality.BALANCED
        )
        ReferenceKitchenPreset.MODERN_ISLAND_BAR -> RealismSettings(
            softShadows = true, softShadowRadius = 0.65f,
            ambientOcclusion = 0.75f, contactShadows = true,
            reflections = 0.85f, floorReflection = 0.85f,
            ledGlow = 1.0f, acesToneMapping = true, denoiser = true,
            glassRefraction = true, bloom = 0.80f, vignette = 0.35f,
            filmGrain = 0.08f, quality = RenderQuality.ULTRA
        )
    }

    /** ACES filmic tone map (Narkowicz approximation, same curve family as the tone map in PbrLightingEngine). */
    fun acesToneMapApprox(color: Vector3): Vector3 {
        fun lobe(x: Float): Float {
            val v = x.coerceIn(0f, 10f)
            return ((v * (2.51f * v + 0.03f)) / (v * (2.43f * v + 0.59f) + 0.14f)).coerceIn(0f, 1f)
        }
        return Vector3(lobe(color.x), lobe(color.y), lobe(color.z))
    }

    /** Shadow penumbra width in mm for a light at [lightDistanceMm]. */
    fun shadowPenumbraMm(softShadowRadius: Float, lightDistanceMm: Float): Float =
        (softShadowRadius.coerceIn(0f, 1f) * lightDistanceMm * 0.02f).coerceIn(0f, 120f)

    /** Exposure multiplier for a quality level: Ultra/4K can afford a darker, richer grade. */
    fun exposureMultiplierFor(quality: RenderQuality): Float = when (quality) {
        RenderQuality.PREVIEW -> 1.0f
        RenderQuality.BALANCED -> 1.0f
        RenderQuality.ULTRA -> 0.94f
        RenderQuality.FOUR_K -> 0.90f
    }

    /** Suggested samples for a device tier — never promise 4K on a low-end phone. */
    fun recommendedQuality(isHighEndDevice: Boolean): RenderQuality =
        if (isHighEndDevice) RenderQuality.ULTRA else RenderQuality.BALANCED
}

/**
 * Eight new PBR finishes traced from the reference photos, on top of the
 * 22 in [RealisticMaterialLibrary] (10 original + 12 Cinematic Pro).
 * Total realistic catalogue after v2.0: 30 finishes.
 */
object ReferenceMaterials {
    val WHITEWASH_OAK = PbrMaterial(
        id = "pbr_whitewash_oak",
        nameAr = "بلوط أبيض مغسول (صورة 2)",
        nameEn = "Whitewashed Oak",
        baseColor = Vector3(0.78f, 0.74f, 0.68f),
        roughness = 0.58f, metallic = 0.0f, reflectance = 0.42f, bumpDepth = 0.65f
    )
    val CHAMPAGNE_HIGH_GLOSS = PbrMaterial(
        id = "pbr_champagne_high_gloss",
        nameAr = "شامبين لامع مرآتي (صورة 3)",
        nameEn = "Champagne High-Gloss Lacquer",
        baseColor = Vector3(0.88f, 0.80f, 0.62f),
        roughness = 0.07f, metallic = 0.05f, reflectance = 0.60f,
        clearcoat = 1.0f, clearcoatRoughness = 0.03f
    )
    val WHITE_SHAKER_LACQUER = PbrMaterial(
        id = "pbr_white_shaker_lacquer",
        nameAr = "أبيض شيكر لاكيه (صورة 4)",
        nameEn = "White Shaker Lacquer",
        baseColor = Vector3(0.95f, 0.95f, 0.94f),
        roughness = 0.22f, metallic = 0.0f, reflectance = 0.55f,
        clearcoat = 0.70f, clearcoatRoughness = 0.10f, bumpDepth = 0.35f
    )
    val LIGHT_OAK_HONEY = PbrMaterial(
        id = "pbr_light_oak_honey",
        nameAr = "بلوط فاتح عسلي (صورة 5)",
        nameEn = "Light Honey Oak",
        baseColor = Vector3(0.72f, 0.58f, 0.38f),
        roughness = 0.55f, metallic = 0.0f, reflectance = 0.42f, bumpDepth = 0.50f
    )
    val BLACK_GALAXY_GRANITE_POLISHED = PbrMaterial(
        id = "pbr_black_galaxy_polished",
        nameAr = "جرانيت جالاكسي أسود مصقول (صور 2 و4 و6)",
        nameEn = "Polished Black Galaxy Granite",
        baseColor = Vector3(0.05f, 0.05f, 0.06f),
        roughness = 0.08f, metallic = 0.10f, reflectance = 0.62f,
        clearcoat = 1.0f, clearcoatRoughness = 0.03f
    )
    val CARRARA_MARBLE_FLOOR = PbrMaterial(
        id = "pbr_carrara_marble_floor",
        nameAr = "أرضية رخام كرارا مصقولة (صور 3 و6)",
        nameEn = "Polished Carrara Marble Floor",
        baseColor = Vector3(0.93f, 0.93f, 0.92f),
        roughness = 0.06f, metallic = 0.0f, reflectance = 0.65f,
        clearcoat = 1.0f, clearcoatRoughness = 0.03f, ior = 1.56f
    )
    val BRONZE_VITRINE_GLASS = PbrMaterial(
        id = "pbr_bronze_vitrine_glass",
        nameAr = "زجاج فيترينة برونزي بإطار أسود (صور 3 و4 و6)",
        nameEn = "Bronze Vitrine Glass, Black Frame",
        baseColor = Vector3(0.38f, 0.28f, 0.18f),
        roughness = 0.05f, metallic = 0.0f, reflectance = 0.58f,
        clearcoat = 0.9f, transmission = 0.75f, ior = 1.52f
    )
    val WARM_BRASS_LED = PbrMaterial(
        id = "pbr_warm_brass_led",
        nameAr = "نحاس دافئ مضيء LED 3000K",
        nameEn = "Warm Brass LED 3000K",
        baseColor = Vector3(0.85f, 0.68f, 0.30f),
        roughness = 0.25f, metallic = 1.0f, reflectance = 0.5f,
        emissiveColor = Vector3(1.0f, 0.75f, 0.40f), emissiveIntensity = 1.0f
    )

    val NEW_REFERENCE_MATERIALS: List<PbrMaterial> = listOf(
        WHITEWASH_OAK, CHAMPAGNE_HIGH_GLOSS, WHITE_SHAKER_LACQUER, LIGHT_OAK_HONEY,
        BLACK_GALAXY_GRANITE_POLISHED, CARRARA_MARBLE_FLOOR, BRONZE_VITRINE_GLASS, WARM_BRASS_LED
    )

    /** 22 existing + 8 reference-traced = 30 realistic PBR finishes. */
    val ALL_WITH_REFERENCES: List<PbrMaterial> =
        RealisticMaterialLibrary.ALL_REALISTIC_MATERIALS + NEW_REFERENCE_MATERIALS

    fun findById(id: String): PbrMaterial? =
        ALL_WITH_REFERENCES.firstOrNull { it.id == id }
}
