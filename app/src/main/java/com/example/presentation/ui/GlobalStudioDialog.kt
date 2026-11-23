package com.example.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.filament.RealismSettings
import com.example.core.filament.ReferenceRealismEngine
import com.example.core.filament.RenderQuality
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.AppStrings
import com.example.core.i18n.KitchenRegion
import com.example.core.units.MeasurementUnitSystem
import com.example.domain.model.ReferenceKitchenPreset
import com.example.presentation.viewmodel.KitchenEditorUiState
import com.example.presentation.viewmodel.KitchenEditorViewModel
import java.util.Locale

/**
 * Global Studio v2.0 — one dialog, three studios:
 *  1) 🌍 عالمي: 8 لغات + متري/إمبراطوري + 5 مناطق بمعاييرها وعملاتها.
 *  2) 📸 مرجعية: 6 مطابخ من الصور المرجعية الست — تطبيق واحد يضبط
 *     الأبعاد والخامات والإضاءة والكاميرا والواقعية معاً.
 *  3) ✨ واقعية: ظلال ناعمة، AO، انعكاسات، توهج LED، ACES، جودة رندر.
 *
 * The dialog itself is localised through [AppStrings]: switching language
 * re-renders every label in this dialog immediately. The rest of the app
 * stays Arabic for now — the dialog says so honestly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalStudioDialog(
    state: KitchenEditorUiState,
    viewModel: KitchenEditorViewModel,
    onDismiss: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val lang = state.appLanguage
    fun tr(key: String): String = AppStrings.t(key, lang)
    val tabs = listOf(tr(AppStrings.TAB_GLOBAL), tr(AppStrings.TAB_REFERENCE), tr(AppStrings.TAB_REALISM))
    var appliedPresetId by remember { mutableStateOf(state.appliedReferencePresetId) }
    var realism by remember { mutableStateOf(state.realismSettings) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(AppStrings.GLOBAL_TITLE),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = tr(AppStrings.CLOSE), tint = Color.White)
                    }
                }
                Text(
                    text = tr(AppStrings.GLOBAL_SUBTITLE),
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                TabRow(selectedTabIndex = tab, containerColor = Color(0xFF1E293B)) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = tab == index,
                            onClick = { tab = index },
                            text = { Text(title, color = Color.White, fontSize = 13.sp) }
                        )
                    }
                }
                when (tab) {
                    0 -> GlobalTab(state, viewModel, ::tr)
                    1 -> ReferenceTab(
                        state = state,
                        appliedPresetId = appliedPresetId,
                        tr = ::tr,
                        onApply = { preset ->
                            viewModel.applyReferencePreset(preset)
                            appliedPresetId = preset.name
                            realism = ReferenceRealismEngine.forPreset(preset)
                        }
                    )
                    else -> RealismTab(
                        realism = realism,
                        tr = ::tr,
                        onRealismChange = { realism = it },
                        onApply = { viewModel.applyRealismSettings(realism.sanitized()) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GlobalTab(
    state: KitchenEditorUiState,
    viewModel: KitchenEditorViewModel,
    tr: (String) -> String
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(tr(AppStrings.LANGUAGE), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(tr(AppStrings.LANGUAGE_NOTE), color = Color(0xFFCBD5E1), fontSize = 12.sp)
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AppLanguage.entries) { language ->
                    val selected = language == state.appLanguage
                    Button(
                        onClick = { viewModel.setAppLanguage(language) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) Color(0xFF22D3EE) else Color(0xFF334155),
                            contentColor = if (selected) Color.Black else Color.White
                        )
                    ) { Text("${language.nativeName} (${language.code})", fontSize = 13.sp) }
                }
            }
        }
        item {
            Text(tr(AppStrings.UNITS), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.setUnitSystem(MeasurementUnitSystem.METRIC) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.unitSystem == MeasurementUnitSystem.METRIC) Color(0xFF22D3EE) else Color(0xFF334155),
                        contentColor = if (state.unitSystem == MeasurementUnitSystem.METRIC) Color.Black else Color.White
                    )
                ) { Text("متري Metric (cm / m²)", fontSize = 13.sp) }
                Button(
                    onClick = { viewModel.setUnitSystem(MeasurementUnitSystem.IMPERIAL) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.unitSystem == MeasurementUnitSystem.IMPERIAL) Color(0xFF22D3EE) else Color(0xFF334155),
                        contentColor = if (state.unitSystem == MeasurementUnitSystem.IMPERIAL) Color.Black else Color.White
                    )
                ) { Text("إمبراطوري Imperial (in / ft²)", fontSize = 13.sp) }
            }
        }
        item {
            Text(tr(AppStrings.REGION), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        items(KitchenRegion.entries) { region ->
            val selected = region == state.kitchenRegion
            Card(
                onClick = { viewModel.setKitchenRegion(region) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) Color(0xFF164E63) else Color(0xFF1E293B)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(region.titleAr, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(region.titleEn, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    Text(
                        "${tr(AppStrings.CURRENCY)}: ${region.currencyCode} (${region.currencySymbol}) • " +
                            "${tr(AppStrings.COUNTERTOP_HEIGHT)}: ${region.countertopHeightMm.toInt()} مم • " +
                            "${tr(AppStrings.WALKWAY)}: ${region.minWalkwayMm.toInt()} مم",
                        color = Color(0xFFCBD5E1), fontSize = 12.sp
                    )
                    Text(region.standardsSummaryAr(), color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ReferenceTab(
    state: KitchenEditorUiState,
    appliedPresetId: String?,
    tr: (String) -> String,
    onApply: (ReferenceKitchenPreset) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(tr(AppStrings.REF_TITLE), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(tr(AppStrings.REF_SUBTITLE), color = Color(0xFFCBD5E1), fontSize = 12.sp)
        }
        items(ReferenceKitchenPreset.entries) { preset ->
            val applied = appliedPresetId == preset.name
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (applied) Color(0xFF164E63) else Color(0xFF1E293B)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(preset.titleAr, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(preset.titleEn, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    Text(preset.photoAr, color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text(
                        "${tr(AppStrings.REF_DIMENSIONS)}: ${preset.dimensionsText(state.unitSystem)}",
                        color = Color.White, fontSize = 13.sp
                    )
                    Text(
                        "${tr(AppStrings.REF_CABINETS)}: ${preset.cabinetFinish.name}",
                        color = Color(0xFFCBD5E1), fontSize = 12.sp
                    )
                    Text(
                        "${tr(AppStrings.REF_COUNTERTOP)}: ${preset.countertopFinish.name}",
                        color = Color(0xFFCBD5E1), fontSize = 12.sp
                    )
                    Text(
                        "${tr(AppStrings.REF_LIGHTING)}: ${preset.lightingKelvin.toInt()}K • ${preset.timeOfDay.titleAr}",
                        color = Color(0xFFCBD5E1), fontSize = 12.sp
                    )
                    Text(
                        "${tr(AppStrings.REF_FEATURES)}: ${preset.featuresAr()}",
                        color = Color(0xFFCBD5E1), fontSize = 12.sp
                    )
                    Button(
                        onClick = { onApply(preset) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (applied) Color(0xFF10B981) else Color(0xFF22D3EE),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (applied) tr(AppStrings.REF_APPLIED) else tr(AppStrings.REF_APPLY), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RealismTab(
    realism: RealismSettings,
    tr: (String) -> String,
    onRealismChange: (RealismSettings) -> Unit,
    onApply: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(tr(AppStrings.REAL_TITLE), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(tr(AppStrings.REAL_SUBTITLE), color = Color(0xFFCBD5E1), fontSize = 12.sp)
        }
        item {
            Text(tr(AppStrings.QUALITY), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RenderQuality.entries) { quality ->
                    val selected = quality == realism.quality
                    Button(
                        onClick = { onRealismChange(realism.copy(quality = quality)) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) Color(0xFF22D3EE) else Color(0xFF334155),
                            contentColor = if (selected) Color.Black else Color.White
                        )
                    ) { Text(quality.titleAr, fontSize = 12.sp) }
                }
            }
            Text(realism.quality.summaryAr(), color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
        item { RealismSlider(tr(AppStrings.SOFT_SHADOWS), realism.softShadowRadius, 0f..1f) { onRealismChange(realism.copy(softShadowRadius = it)) } }
        item { RealismSlider(tr(AppStrings.AMBIENT_OCCLUSION), realism.ambientOcclusion, 0f..1f) { onRealismChange(realism.copy(ambientOcclusion = it)) } }
        item { RealismSlider(tr(AppStrings.REFLECTIONS), realism.reflections, 0f..1f) { onRealismChange(realism.copy(reflections = it)) } }
        item { RealismSlider(tr(AppStrings.FLOOR_REFLECTION), realism.floorReflection, 0f..1f) { onRealismChange(realism.copy(floorReflection = it)) } }
        item { RealismSlider(tr(AppStrings.LED_GLOW), realism.ledGlow, 0f..1.5f) { onRealismChange(realism.copy(ledGlow = it)) } }
        item { RealismSlider("Bloom", realism.bloom, 0f..1.5f) { onRealismChange(realism.copy(bloom = it)) } }
        item {
            RealismSwitch(tr(AppStrings.SOFT_SHADOWS), realism.softShadows) { onRealismChange(realism.copy(softShadows = it)) }
            RealismSwitch(tr(AppStrings.CONTACT_SHADOWS), realism.contactShadows) { onRealismChange(realism.copy(contactShadows = it)) }
            RealismSwitch(tr(AppStrings.ACES), realism.acesToneMapping) { onRealismChange(realism.copy(acesToneMapping = it)) }
            RealismSwitch(tr(AppStrings.DENOISER), realism.denoiser) { onRealismChange(realism.copy(denoiser = it)) }
            RealismSwitch(tr(AppStrings.GLASS_REFRACTION), realism.glassRefraction) { onRealismChange(realism.copy(glassRefraction = it)) }
        }
        item {
            Text(realism.summaryAr(), color = Color(0xFFCBD5E1), fontSize = 12.sp)
            Button(
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22D3EE), contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) { Text(tr(AppStrings.APPLY_SETTINGS), fontSize = 14.sp) }
        }
    }
}

@Composable
private fun RealismSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 13.sp)
            Text(
                String.format(Locale.US, "%.0f%%", value / range.endInclusive * 100f),
                color = Color(0xFFCBD5E1), fontSize = 12.sp
            )
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun RealismSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
