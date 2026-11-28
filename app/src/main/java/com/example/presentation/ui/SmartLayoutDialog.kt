package com.example.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.geometry.SmartLayoutEngine
import com.example.core.geometry.SmartLayoutEngine.SmartLayoutConfig
import com.example.core.geometry.SmartLayoutEngine.SmartLayoutShape
import com.example.domain.model.KitchenElement
import com.example.domain.model.KitchenElementType
import com.example.domain.model.RoomStructure
import com.example.domain.model.WallSide

/**
 * Smart layout generator dialog — v1.16 (AI智能布局 / التخطيط الذكي).
 * Picks a shape (I / L / U), the main wall and the options, shows a live
 * preview (unit counts + work-triangle numbers + warnings), and hands the
 * generated elements up for the confirmation step — applying replaces the
 * whole kitchen and is undoable, like templates (v1.14).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartLayoutDialog(
    room: RoomStructure,
    onApply: (SmartLayoutConfig, List<KitchenElement>) -> Unit,
    onDismiss: () -> Unit
) {
    var shape by remember { mutableStateOf(SmartLayoutShape.L_SHAPE) }
    var mainWall by remember { mutableStateOf(WallSide.NORTH) }
    var includeDishwasher by remember { mutableStateOf(true) }
    var includeTallPantry by remember { mutableStateOf(false) }

    val config = remember(shape, mainWall, includeDishwasher, includeTallPantry) {
        SmartLayoutConfig(
            shape = shape,
            mainWall = mainWall,
            includeDishwasher = includeDishwasher,
            includeTallPantry = includeTallPantry
        )
    }
    val result = remember(room, config) { SmartLayoutEngine.generate(room, config) }

    val baseCount = result.elements.count { it.type.category == com.example.domain.model.ElementCategory.BASE_CABINET }
    val wallCount = result.elements.count { it.type.category == com.example.domain.model.ElementCategory.WALL_CABINET }
    val tallCount = result.elements.count { it.type.category == com.example.domain.model.ElementCategory.TALL_UNIT }
    val applianceCount = result.elements.count { it.type.category == com.example.domain.model.ElementCategory.APPLIANCE }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("smart_layout_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0B1120),
            border = BorderStroke(1.5.dp, Color(0xFF334155)),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF8B5CF6))
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFC4B5FD),
                                modifier = Modifier.padding(8.dp).size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "التخطيط الذكي (AI智能布局)",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "يولّد مطبخاً كاملاً من أبعاد غرفتك: حوض وموقد وثلاجة بمثلث عمل صحيح",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_smart_layout_button")) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Shape picker
                Text("شكل التخطيط", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (s in SmartLayoutShape.entries) {
                        val selected = shape == s
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { shape = s }
                                .testTag("smart_layout_shape_${s.name.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) Color(0xFF8B5CF6) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (selected) Color(0xFFC4B5FD) else Color(0xFF334155))
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(s.titleAr, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(s.titleEn, color = Color(0xFF94A3B8), fontSize = 10.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Main wall picker
                Text("الجدار الرئيسي", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val walls = listOf(
                        WallSide.NORTH to "شمالي",
                        WallSide.EAST to "شرقي",
                        WallSide.SOUTH to "جنوبي",
                        WallSide.WEST to "غربي"
                    )
                    for ((w, label) in walls) {
                        val selected = mainWall == w
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { mainWall = w }
                                .testTag("smart_layout_wall_${w.name.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) Color(0xFF8B5CF6) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (selected) Color(0xFFC4B5FD) else Color(0xFF334155))
                        ) {
                            Text(
                                label,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 9.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("غسالة صحون بجانب الحوض", color = Color.White, fontSize = 12.sp)
                    Switch(
                        checked = includeDishwasher,
                        onCheckedChange = { includeDishwasher = it },
                        modifier = Modifier.testTag("smart_layout_dishwasher_switch")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("دولاب مؤونة عمودي", color = Color.White, fontSize = 12.sp)
                    Switch(
                        checked = includeTallPantry,
                        onCheckedChange = { includeTallPantry = it },
                        modifier = Modifier.testTag("smart_layout_pantry_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live preview
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("معاينة التخطيط — ${result.shapeUsed.titleAr}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (result.elements.isEmpty()) {
                            Text(
                                "لا يمكن توليد تخطيط لهذه الغرفة.",
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp
                            )
                        } else {
                            Text(
                                "${result.elements.size} وحدة: $baseCount سفلية • $wallCount علوية • $tallCount عمودية • $applianceCount أجهزة",
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "حوض–موقد: ${result.sinkToStoveMm?.toInt() ?: "—"} مم (المثالي 1200–2700)",
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp
                            )
                            Text(
                                "محيط مثلث العمل: ${result.trianglePerimeterMm?.toInt() ?: "—"} مم (المثالي 3400–7900)",
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp
                            )
                        }
                        for (warning in result.warnings) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(warning, color = Color(0xFFFCD34D), fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { onApply(config, result.elements) },
                    enabled = result.elements.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("apply_smart_layout_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تطبيق التخطيط", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "التطبيق يستبدل مطبخك الحالي بالكامل — وستظهر نافذة تأكيد، ويمكنك التراجع فوراً.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
        }
    }
}
