package com.example

import com.example.core.filament.RealismSettings
import com.example.core.filament.ReferenceMaterials
import com.example.core.filament.ReferenceRealismEngine
import com.example.core.filament.RenderQuality
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.AppStrings
import com.example.core.i18n.KitchenRegion
import com.example.core.math.Vector3
import com.example.core.units.MeasurementUnitSystem
import com.example.domain.model.ReferenceKitchenPreset
import com.example.domain.model.RoomStructure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Global Studio v2.0 — pure-Kotlin guarantees, no Android needed:
 * every dialog string exists in all 8 languages, every reference
 * preset has sane real-world dimensions, and every realism preset
 * stays inside its physical range.
 */
class GlobalRealismTest {

    @Test
    fun testAllDialogStringsExistInAllLanguages() {
        val missing = AppStrings.missingKeys()
        assertTrue("Missing translations: $missing", missing.isEmpty())
        assertTrue(AppStrings.isComplete())
        for (lang in AppLanguage.entries) {
            for (key in AppStrings.ALL_KEYS) {
                val value = AppStrings.t(key, lang)
                assertTrue("Blank $lang:$key", value.isNotBlank())
                assertTrue("Untranslated fallback leaked for $lang:$key", value != key)
            }
        }
        assertEquals(8, AppLanguage.entries.size)
        assertEquals("الاستوديو العالمي 🌍", AppStrings.t(AppStrings.GLOBAL_TITLE, AppLanguage.ARABIC))
        assertEquals("Global Studio 🌍", AppStrings.t(AppStrings.GLOBAL_TITLE, AppLanguage.ENGLISH))
    }

    @Test
    fun testSixReferencePresetsHaveRealWorldDimensions() {
        assertEquals(6, ReferenceKitchenPreset.entries.size)
        for (preset in ReferenceKitchenPreset.entries) {
            assertTrue("${preset.name} width", preset.widthMm in 2400f..6000f)
            assertTrue("${preset.name} depth", preset.depthMm in 2000f..5000f)
            assertTrue("${preset.name} height", preset.heightMm in 2400f..3200f)
            assertTrue("${preset.name} fridge", preset.fridgeHeightMm in 1800f..2000f)
            assertTrue("${preset.name} upper", preset.upperCabinetHeightMm in 600f..1100f)
            assertTrue("${preset.name} kelvin", preset.lightingKelvin in 2200f..6500f)
            assertTrue("${preset.name} summary", preset.summaryAr().isNotBlank())
            assertTrue("${preset.name} features", preset.featuresAr().isNotBlank())
        }
        // Photo 1 is the measured one: 3.00 × 2.40 × 2.50 m, fridge 1.90 m, upper 98 cm.
        val compact = ReferenceKitchenPreset.COMPACT_L_300
        assertEquals(3000f, compact.widthMm, 0.01f)
        assertEquals(2400f, compact.depthMm, 0.01f)
        assertEquals(2500f, compact.heightMm, 0.01f)
        assertEquals(1900f, compact.fridgeHeightMm, 0.01f)
        assertEquals(980f, compact.upperCabinetHeightMm, 0.01f)
        assertTrue(compact.dimensionsAreMeasured)
    }

    @Test
    fun testPresetApplyToChangesRoomSizeAndFinishes() {
        val room = RoomStructure(widthMm = 4200f, depthMm = 3600f, heightMm = 2700f)
        val applied = ReferenceKitchenPreset.CHAMPAGNE_SHOWCASE.applyTo(room)
        assertEquals(4200f, applied.widthMm, 0.01f)
        assertEquals(3600f, applied.depthMm, 0.01f)
        assertEquals(2800f, applied.heightMm, 0.01f)
        assertEquals(
            ReferenceKitchenPreset.CHAMPAGNE_SHOWCASE.layoutShape,
            applied.layoutShape
        )
        assertEquals(
            ReferenceKitchenPreset.CHAMPAGNE_SHOWCASE.wallFinish,
            applied.wallFinish
        )
        assertEquals(
            ReferenceKitchenPreset.CHAMPAGNE_SHOWCASE.floorMaterial,
            applied.floorMaterial
        )
        val compact = ReferenceKitchenPreset.COMPACT_L_300.applyTo(room)
        assertEquals(3000f, compact.widthMm, 0.01f)
        assertEquals(2500f, compact.heightMm, 0.01f)
    }

    @Test
    fun testRealismPresetsStayInPhysicalRange() {
        for (preset in ReferenceKitchenPreset.entries) {
            val settings = ReferenceRealismEngine.forPreset(preset).sanitized()
            assertTrue("${preset.name} AO", settings.ambientOcclusion in 0f..1f)
            assertTrue("${preset.name} floor", settings.floorReflection in 0f..1f)
            assertTrue("${preset.name} LED", settings.ledGlow in 0f..1.5f)
            assertTrue("${preset.name} bloom", settings.bloom in 0f..1.5f)
            val post = settings.toPostProcessConfig()
            assertTrue("${preset.name} post bloom", post.bloomIntensity in 0f..1.5f)
            assertTrue("${preset.name} summary", settings.summaryAr().isNotBlank())
        }
        // The showcase photos need the strongest floor reflection + LED glow.
        val showcase = ReferenceRealismEngine.forPreset(ReferenceKitchenPreset.CHAMPAGNE_SHOWCASE)
        assertTrue(showcase.floorReflection >= 0.85f)
        assertTrue(showcase.ledGlow >= 0.95f)
        assertTrue(showcase.reflections >= 0.85f)
    }

    @Test
    fun testAcesToneMapKeepsValuesInRange() {
        val mapped = ReferenceRealismEngine.acesToneMapApprox(Vector3(2.0f, 1.0f, 0.2f))
        assertTrue(mapped.x in 0f..1f)
        assertTrue(mapped.y in 0f..1f)
        assertTrue(mapped.z in 0f..1f)
        assertTrue("Bright highlights compress but stay brightest", mapped.x >= mapped.y)
        assertTrue("Shadow penumbra grows with radius", ReferenceRealismEngine.shadowPenumbraMm(1.0f, 3000f) > ReferenceRealismEngine.shadowPenumbraMm(0.0f, 3000f))
    }

    @Test
    fun testReferenceMaterialsAreUniqueAndTotalThirty() {
        val all = ReferenceMaterials.ALL_WITH_REFERENCES
        assertEquals(30, all.size)
        assertEquals(30, all.map { it.id }.toSet().size)
        assertEquals(8, ReferenceMaterials.NEW_REFERENCE_MATERIALS.size)
        assertTrue(ReferenceMaterials.findById("pbr_champagne_high_gloss") != null)
        assertTrue(ReferenceMaterials.findById("pbr_bronze_vitrine_glass") != null)
        for (material in all) {
            assertTrue("${material.id} roughness", material.roughness in 0.02f..1.0f)
            assertTrue("${material.id} metallic", material.metallic in 0f..1f)
        }
    }

    @Test
    fun testRegionsCarrySaneStandards() {
        assertEquals(5, KitchenRegion.entries.size)
        for (region in KitchenRegion.entries) {
            assertTrue("${region.name} countertop", region.countertopHeightMm in 800f..950f)
            assertTrue("${region.name} walkway", region.minWalkwayMm in 900f..1300f)
            assertTrue("${region.name} currency", region.currencyCode.length == 3)
            assertTrue("${region.name} summary", region.standardsSummaryAr().isNotBlank())
        }
        assertEquals(MeasurementUnitSystem.IMPERIAL, KitchenRegion.NORTH_AMERICA.defaultUnits)
        assertEquals(MeasurementUnitSystem.METRIC, KitchenRegion.BRAZIL_LATAM.defaultUnits)
        assertEquals(AppLanguage.PORTUGUESE, KitchenRegion.BRAZIL_LATAM.defaultLanguage)
        assertEquals(914f, KitchenRegion.NORTH_AMERICA.countertopHeightMm, 0.01f)
    }

    @Test
    fun testRenderQualityOrdering() {
        assertTrue(RenderQuality.PREVIEW.samplesPerPixel < RenderQuality.BALANCED.samplesPerPixel)
        assertTrue(RenderQuality.BALANCED.samplesPerPixel < RenderQuality.ULTRA.samplesPerPixel)
        assertTrue(RenderQuality.ULTRA.samplesPerPixel < RenderQuality.FOUR_K.samplesPerPixel)
        assertEquals(RenderQuality.BALANCED, ReferenceRealismEngine.recommendedQuality(false))
        assertEquals(RenderQuality.ULTRA, ReferenceRealismEngine.recommendedQuality(true))
        assertTrue(RealismSettings().sanitized().ambientOcclusion in 0f..1f)
    }
}
