package com.example.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.geometry.SmartTilingEngine
import com.example.core.geometry.SmartTilingEngine.TilingPattern
import com.example.core.geometry.SmartTilingEngine.TilingRequest
import com.example.core.geometry.SmartTilingEngine.TilingSurface
import com.example.domain.model.KitchenElement
import com.example.domain.model.MaterialFinish
import com.example.domain.model.RoomStructure

/**
 * Smart tiling calculator dialog — v1.16 (智能铺砖 / التبليط الذكي).
 * Floor (with under-cabinet savings) or backsplash, tile size, pattern,
 * grout and an optional price per tile; live counts + waste % + cost.
 * "Apply" (floor only) sets the room's floor finish to the chosen tile
 * material — undoable, like any floor-material change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartTilingDialog(
    room: RoomStructure,
    elements: List<KitchenElement>,
    onApplyFloorMaterial: (MaterialFinish) -> Unit,
    onDismiss: () -> Unit
) {
    var surface by remember { mutableStateOf(TilingSurface.FLOOR) }
    var tileSize by remember { mutableStateOf(600f to 600f) }
    var pattern by remember { mutableStateOf(TilingPattern.STRAIGHT) }
    var groutMm by remember { mutableStateOf(3f) }
    var priceText by remember { mutableStateOf("") }
    var floorMaterial by remember { mutableStateOf(MaterialFinish.MARBLE_WHITE_FLOOR) }

    val request = remember(surface, tileSize, pattern, groutMm, priceText) {
        TilingRequest(
            surface = surface,
            tileWidthMm = tileSize.first,
            tileHeightMm = tileSize.second,
            groutMm = groutMm,
            pattern = pattern,
            sparePercent = 10f,
            pricePerTile = priceText.toFloatOrNull() ?: 0f
        )
    }
    val result = remember(room, elements, request) {
        if (surface == TilingSurface.FLOOR) {
            SmartTilingEngine.calculateFloor(room, elements, request)
        } else {
            SmartTilingEngine.calculateBacksplash(room, elements, request)
        }
    }

    val tileSizes = listOf(
        (300f to 300f) to "30×30",
        (300f to 600f) to "30×60",
        (600f to 600f) to "60×60",
        (800f to 800f) to "80×80",
        (600f to 1200f) to "60×120"
    )
    val tileMaterials = listOf(
        MaterialFinish.MARBLE_WHITE_FLOOR,
        MaterialFinish.BEIGE_TRAVERTINE_FLOOR,
        MaterialFinish.GREY_SLATE_FLOOR,
        MaterialFinish.CONCRETE_LOFT_FLOOR,
        MaterialFinish.DARK_EMPERADOR_FLOOR
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("smart_tiling_dialog"),
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
                            color = Color(0xFF0EA5E9).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF0EA5E9))
                        ) {
                            Icon(
                                Icons.Default.GridOn,
                                contentDescription = null,
                                tint = Color(0xFF7DD3FC),
                                modifier = Modifier.padding(8.dp).size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "التبليط الذكي (智能铺砖)",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "عدد البلاط والقص والهدر — للأرضية أو للوزرة فوق سطح العمل",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_smart_tiling_button")) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Surface picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (s in TilingSurface.entries) {
                        val selected = surface == s
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { surface = s }
                                .testTag("tiling_surface_${s.name.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) Color(0xFF0EA5E9) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (selected) Color(0xFF7DD3FC) else Color(0xFF334155))
                        ) {
                            Text(
                                s.titleAr,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 10.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tile size
                Text("مقاس البلاطة (سم)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for ((size, label) in tileSizes) {
                        val selected = tileSize == size
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { tileSize = size }
                                .testTag("tiling_size_${label.replace("×", "x")}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) Color(0xFF0EA5E9) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (selected) Color(0xFF7DD3FC) else Color(0xFF334155))
                        ) {
                            Text(
                                label,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pattern
                Text("نمط التركيب", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (p in TilingPattern.entries) {
                        val selected = pattern == p
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pattern = p }
                                .testTag("tiling_pattern_${p.name.lowercase()}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) Color(0xFF0EA5E9) else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (selected) Color(0xFF7DD3FC) else Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(p.titleAr, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(p.descriptionAr, color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Grout + price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("عرض الفاصل (مم)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (g in listOf(2f, 3f, 5f)) {
                                val selected = groutMm == g
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { groutMm = g }
                                        .testTag("tiling_grout_${g.toInt()}"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) Color(0xFF0EA5E9) else Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, if (selected) Color(0xFF7DD3FC) else Color(0xFF334155))
                                ) {
                                    Text(
                                        "${g.toInt()}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("سعر البلاطة (اختياري)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it.filter { c -> c.isDigit() || c == '.' } },
                            placeholder = { Text("مثال: 2.5", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("tiling_price_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF0EA5E9),
                                unfocusedBorderColor = Color(0xFF334155)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Results
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("النتيجة", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (result.noteAr != null) {
                            Text(result.noteAr, color = Color(0xFFFCA5A5), fontSize = 12.sp)
                        } else {
                            Text(
                                "المساحة: ${"%.2f".format(result.areaM2)} م² (${result.areaWidthMm.toInt()}×${result.areaDepthMm.toInt()} مم)",
                                color = Color(0xFFCBD5E1), fontSize = 12.sp
                            )
                            Text("بلاط كامل: ${result.fullTiles} • بلاط مقصوص: ${result.cutTiles}", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                            if (surface == TilingSurface.FLOOR) {
                                Text("مُوفَّر تحت الخزائن: ${result.hiddenTiles} بلاطة", color = Color(0xFF86EFAC), fontSize = 12.sp)
                            }
                            Text(
                                "الإجمالي للشراء (مع 10% احتياطي): ${result.totalToBuy} بلاطة",
                                color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                            Text(
                                "نسبة الهدر: ${"%.1f".format(result.wastePercent)}%",
                                color = Color(0xFFCBD5E1), fontSize = 12.sp
                            )
                            if (result.cost > 0f) {
                                Text(
                                    "التكلفة التقديرية: ${"%.2f".format(result.cost)}",
                                    color = Color(0xFFCBD5E1), fontSize = 12.sp
                                )
                            }
                            if (pattern == TilingPattern.DIAGONAL_45) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "نمط المائل تقديري: أُضيف 15% هدر للقص المائل (معيار الصناعة).",
                                    color = Color(0xFFFCD34D), fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Floor material apply (floor only)
                if (surface == TilingSurface.FLOOR && result.totalToBuy > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("خامة البلاط على الأرضية", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (m in tileMaterials) {
                            val selected = floorMaterial == m
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { floorMaterial = m }
                                    .testTag("tiling_material_${m.id}"),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(m.colorHex),
                                border = BorderStroke(
                                    2.dp,
                                    if (selected) Color.White else Color(0xFF334155)
                                )
                            ) {
                                Box(modifier = Modifier.height(36.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(floorMaterial.name, color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onApplyFloorMaterial(floorMaterial) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("apply_tiling_floor_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تطبيق خامة البلاط على الأرضية", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
