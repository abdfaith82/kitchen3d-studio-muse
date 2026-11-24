package com.example.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.camera.CameraMode
import com.example.core.units.MeasurementUnitSystem
import com.example.presentation.viewmodel.ActiveDialog
import com.example.presentation.viewmodel.KitchenEditorUiState
import com.example.presentation.viewmodel.ViewMode

/**
 * Global Options v1.4 — "خيارات من التطبيقات العالمية".
 *
 * One place for the options global kitchen apps (Planner 5D, Homestyler,
 * IKEA Home Planner, RoomSketcher) all expose on their main screens, which
 * in this app were scattered across a dozen different dialogs or had no
 * visible switch at all:
 *  - view: 3D perspective / 2D CAD plan / split screen,
 *  - live dimension labels on/off, smart snapping on/off,
 *  - metric / imperial units,
 *  - walkthrough (first-person) mode and VR gyro mode,
 *  - turntable auto-rotate and the one-tap realistic view,
 *  - shortcuts to the Global Studio (languages & regions) and Export.
 *
 * Every row here wires an existing ViewModel function — nothing here is
 * a mock: toggling a row changes the real studio state immediately.
 */
@Composable
fun GlobalOptionsDialog(
    state: KitchenEditorUiState,
    onDismiss: () -> Unit,
    onSetViewMode: (ViewMode) -> Unit,
    onToggleLiveDimensions: () -> Unit,
    onToggleSmartSnapping: () -> Unit,
    onSetUnitSystem: (MeasurementUnitSystem) -> Unit,
    onSetCameraMode: (CameraMode) -> Unit,
    onToggleAutoRotate: () -> Unit,
    onRealisticView: () -> Unit,
    onOpenDialog: (ActiveDialog) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("global_options_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚙️ خيارات عالمية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8))
                }
            }
        },
        text = {
            // v1.5 — audit: AlertDialog text does not scroll by itself, so
            // the long option list could clip at the bottom on small phones.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "خيارات التطبيقات العالمية في مكان واحد — كل صف يغيّر الاستوديو فوراً.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                // View mode (like Planner 5D / Homestyler top switch)
                Text("العرض", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ViewMode.entries.forEach { mode ->
                        val selected = state.viewMode == mode
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) Color(0xFF0284C7) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (selected) Color(0xFF38BDF8) else Color(0xFF334155)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSetViewMode(mode) }
                                .testTag("global_option_view_${mode.name.lowercase()}")
                        ) {
                            Text(
                                text = mode.title,
                                color = if (selected) Color.White else Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Units (metric / imperial — like RoomSketcher / IKEA planner)
                Text("وحدات القياس", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MeasurementUnitSystem.entries.forEach { system ->
                        val selected = state.unitSystem == system
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) Color(0xFF0284C7) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (selected) Color(0xFF38BDF8) else Color(0xFF334155)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSetUnitSystem(system) }
                                .testTag("global_option_unit_${system.name.lowercase()}")
                        ) {
                            Text(
                                text = system.displayNameAr,
                                color = if (selected) Color.White else Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OptionSwitchRow(
                    label = "إظهار المقاسات الحية على العناصر",
                    checked = state.showLiveDimensions,
                    onToggle = onToggleLiveDimensions,
                    testTag = "global_option_live_dimensions"
                )
                OptionSwitchRow(
                    label = "الالتصاق الذكي 1 مم (Snap)",
                    checked = state.isSmartSnappingEnabled,
                    onToggle = onToggleSmartSnapping,
                    testTag = "global_option_smart_snapping"
                )
                OptionSwitchRow(
                    label = "دوران تلقائي للكاميرا (عرض تقديمي)",
                    checked = state.isCameraAutoRotate,
                    onToggle = onToggleAutoRotate,
                    testTag = "global_option_autorotate"
                )

                // Walkthrough + VR (first-person entry, like Homestyler walk mode)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF7C3AED).copy(alpha = 0.30f),
                        border = BorderStroke(1.dp, Color(0xFFA855F7)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSetCameraMode(CameraMode.FIRST_PERSON_WALK)
                                onDismiss()
                            }
                            .testTag("global_option_walkthrough")
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(15.dp))
                            Text(" 🚶 جولة مشي داخل المطبخ", color = Color(0xFFE9D5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Realistic view + shortcuts
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onRealisticView()
                                onDismiss()
                            }
                            .testTag("global_option_realistic_view")
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(15.dp))
                            Text(" منظر واقعي", color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenDialog(ActiveDialog.GlobalStudio) }
                            .testTag("global_option_open_global_studio")
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(15.dp))
                            Text(" اللغات والمناطق 🌍", color = Color(0xFFE2E8F0), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "تلميح: زر الرجوع يغلق أي نافذة أولاً، ولا يخرج من التطبيق إلا بعد تحذير.",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        },
        confirmButton = {},
        dismissButton = {},
        containerColor = Color(0xFF0F172A)
    )
}

@Composable
private fun OptionSwitchRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}
