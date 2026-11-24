package com.example.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.camera.CameraNavigationPreset

/**
 * Camera Control Panel v1.4 — the perspective control users asked for.
 *
 * Before v1.4 the camera could only be orbited by dragging on the 3D view
 * itself (and only when the interaction mode allowed it), with no visible
 * buttons at all: no zoom buttons, no angle presets, no reset. This panel
 * sits at the top-start of the workspace in 3D / split modes and offers:
 *  - orbit left/right (±15°) and tilt up/down (±10°),
 *  - zoom in/out buttons (pinch still works too),
 *  - one-tap view presets (perspective / isometric / top / front / side /
 *    walkthrough / focus on selection) via the existing animated presets,
 *  - camera reset, turntable auto-rotate, and the one-tap realistic view.
 *
 * It is collapsible to a single 🎥 pill so it never covers the design.
 */
@Composable
fun CameraControlPanel(
    isAutoRotate: Boolean,
    onOrbit: (Float, Float) -> Unit,
    onZoom: (Float) -> Unit,
    onPreset: (CameraNavigationPreset) -> Unit,
    onResetCamera: () -> Unit,
    onToggleAutoRotate: () -> Unit,
    onRealisticView: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Collapsed: a single pill that never covers the design.
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.88f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded }
                .testTag("camera_control_toggle")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                Text(
                    text = "المنظور 🎥",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (!expanded) return@Column
        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.88f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.testTag("camera_control_panel")
        ) {
            // v1.5 — the panel was a tall opaque column covering half the
            // scene while the user adjusted the camera (v1.4 video): presets
            // are now a 2-column grid, the panel is slightly see-through,
            // scrolls on small screens, and collapses by itself after a
            // preset/realistic-view tap so the result is visible at once.
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Orbit + tilt pad
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = { onOrbit(-15f, 0f) }, modifier = Modifier.size(34.dp).testTag("camera_yaw_left")) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "تدوير المنظور يساراً", tint = Color(0xFF38BDF8))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { onOrbit(0f, 10f) }, modifier = Modifier.size(34.dp).testTag("camera_pitch_up")) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "إمالة المنظور للأعلى", tint = Color(0xFF38BDF8))
                        }
                        IconButton(onClick = { onOrbit(0f, -10f) }, modifier = Modifier.size(34.dp).testTag("camera_pitch_down")) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "إمالة المنظور للأسفل", tint = Color(0xFF38BDF8))
                        }
                    }
                    IconButton(onClick = { onOrbit(15f, 0f) }, modifier = Modifier.size(34.dp).testTag("camera_yaw_right")) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "تدوير المنظور يميناً", tint = Color(0xFF38BDF8))
                    }
                }

                // Zoom
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(onClick = { onZoom(1f / 1.15f) }, modifier = Modifier.size(30.dp).testTag("camera_zoom_out")) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "تصغير المنظور", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    }
                    Text("تقريب", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { onZoom(1.15f) }, modifier = Modifier.size(30.dp).testTag("camera_zoom_in")) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "تكبير المنظور", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    }
                }

                // View presets (animated, like global apps) — 2-column grid
                // so the panel is half as tall as the v1.4 single column.
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        CameraNavigationPreset.PERSPECTIVE to "منظور عام",
                        CameraNavigationPreset.ISOMETRIC to "آيزومتري 45°",
                        CameraNavigationPreset.TOP_VIEW to "مسقط علوي",
                        CameraNavigationPreset.FRONT_ELEVATION to "واجهة أمامية",
                        CameraNavigationPreset.SIDE_ELEVATION to "واجهة جانبية"
                    ).chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            pair.forEach { (preset, label) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onPreset(preset)
                                            expanded = false
                                        }
                                        .testTag("camera_preset_${preset.name.lowercase()}")
                                ) {
                                    Text(
                                        text = label,
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            // Keep the last row's single button from stretching
                            // full-width when the pair has only one preset.
                            if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Focus on selection (only meaningful when something is selected; the
                // ViewModel no-ops safely otherwise, so the button stays harmless).
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0284C7).copy(alpha = 0.30f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onPreset(CameraNavigationPreset.FOCUS_SELECTION)
                            expanded = false
                        }
                        .testTag("camera_preset_focus_selection")
                ) {
                    Text(
                        text = "🎯 تركيز على العنصر المحدد",
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                // Reset / auto-rotate / realistic view
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onResetCamera() }
                            .testTag("camera_reset_btn")
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("إعادة ضبط", color = Color(0xFFFBBF24), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAutoRotate) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAutoRotate) Color(0xFF10B981) else Color(0xFF334155)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleAutoRotate() }
                            .testTag("camera_autorotate_btn")
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Autorenew, contentDescription = null, tint = if (isAutoRotate) Color(0xFF34D399) else Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isAutoRotate) "دوران تلقائي ✓" else "دوران تلقائي",
                                color = if (isAutoRotate) Color(0xFF34D399) else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onRealisticView()
                            expanded = false
                        }
                        .testTag("camera_realistic_view_btn")
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🌞 منظر واقعي بضغطة واحدة", color = Color(0xFFFBBF24), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
