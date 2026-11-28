package com.example.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.export.CsvExportHelper
import com.example.core.geometry.MepEngine
import com.example.domain.model.KitchenElement
import com.example.domain.model.KitchenElementType
import com.example.domain.model.RoomStructure
import com.example.domain.model.UtilityPointType
import kotlinx.coroutines.launch

/**
 * v1.18 — water & electric (Chinese feature 9, 水暖电, simplified).
 *
 * The manual point editor already lives in RoomSettingsDialog and the PDF
 * blueprint already has an MEP layer — this dialog adds what was missing:
 * automatic socket placement, pipe routing (supply + drain) with a schematic
 * plan, and a CSV list for the workshop. Points are applied through the
 * existing undoable [onApplyPoints] path.
 */
@Composable
fun WaterElectricDialog(
    room: RoomStructure,
    elements: List<KitchenElement>,
    onApplyPoints: (List<MepEngine.SuggestedPoint>) -> Unit,
    onRemovePoint: (wallId: String, utilityId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val suggestions = remember(room, elements) { MepEngine.suggestPoints(room, elements) }
    val pipePlan = remember(room, elements) { MepEngine.planPipes(room, elements) }
    var appliedCount by remember { mutableStateOf<Int?>(null) }
    var csvStatus by remember { mutableStateOf<String?>(null) }

    val existingPoints = room.walls.flatMap { wall ->
        wall.utilityPoints.map { wall to it }
    }

    fun buildCsv(): String {
        val rows = mutableListOf<List<String>>()
        rows.add(listOf("النوع", "الجدار", "الإزاحة (سم)", "الارتفاع (سم)", "التسمية"))
        for ((wall, pt) in existingPoints) {
            rows.add(
                listOf(
                    pt.type.titleAr, wall.side.title,
                    "%.1f".format(pt.offsetCm), "%.1f".format(pt.elevationMm / 10f), pt.label
                )
            )
        }
        rows.add(emptyList())
        rows.add(listOf("الأنبوب", "النقاط", "المسار"))
        for (run in pipePlan.runs) {
            rows.add(
                listOf(
                    run.labelAr, run.points.size.toString(),
                    run.points.joinToString(" → ") { "(%.0f, %.0f)".format(it.xMm, it.zMm) }
                )
            )
        }
        return CsvExportHelper.buildCsv(rows)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .testTag("water_electric_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF090D16)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("الماء والكهرباء — نقاط وأنابيب", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Automatic socket placement.
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "توزيع المقابس تلقائياً",
                                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "مقبس لكل جهاز + مقبس كل 1200 مم على سطح العمل، كلها في منطقة الـ 60 سم (ارتفاع 1100 مم). النقاط الموجودة لا تتكرر.",
                                    color = Color(0xFF94A3B8), fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (suggestions.isEmpty()) {
                                    Text("لا اقتراحات جديدة — كل المقابس موجودة أصلاً.", color = Color(0xFF34D399), fontSize = 12.sp)
                                } else {
                                    for (s in suggestions) {
                                        Text(
                                            "• ${s.labelAr} — ${s.wallSide.title}، إزاحة ${(s.offsetMm / 10f).toInt()} سم — ${s.reasonAr}",
                                            color = Color(0xFFCBD5E1), fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Button(
                                    onClick = {
                                        onApplyPoints(suggestions)
                                        appliedCount = suggestions.size
                                    },
                                    enabled = suggestions.isNotEmpty() && appliedCount == null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("apply_auto_sockets_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                                ) {
                                    Text(
                                        if (appliedCount != null) "تمت الإضافة ✓ (${appliedCount} نقطة)" else "تطبيق الكل (${suggestions.size})",
                                        fontWeight = FontWeight.Bold, fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    // 2. Schematic plan with pipe routes.
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "مخطط الأنابيب (مسقط أفقي)",
                                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                MepSchematicCanvas(
                                    room = room,
                                    elements = elements,
                                    pipePlan = pipePlan,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .testTag("mep_schematic_canvas")
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "أزرق = ماء بارد • أحمر متقطع = ماء ساخن • رمادي = صرف • النقاط الملونة = نقاط الخدمة",
                                    color = Color(0xFF94A3B8), fontSize = 11.sp
                                )
                                pipePlan.noteAr?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(it, color = Color(0xFFFBBF24), fontSize = 12.sp)
                                }
                                if (pipePlan.runs.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    for (run in pipePlan.runs) {
                                        Text("• ${run.labelAr} (${run.points.size} نقطة مسار)", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 3. Current points with delete.
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "النقاط الحالية (${existingPoints.size})",
                                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                if (existingPoints.isEmpty()) {
                                    Text("لا نقاط بعد.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                } else {
                                    for ((wall, pt) in existingPoints) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${pt.type.codeSymbol} — ${wall.side.title}، ${(pt.offsetCm).toInt()} سم، ارتفاع ${(pt.elevationMm / 10f).toInt()} سم",
                                                color = Color(0xFFCBD5E1), fontSize = 12.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(onClick = { onRemovePoint(wall.id, pt.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFF87171), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. CSV export.
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val fileName = "Kitchen3D_MEP_${System.currentTimeMillis()}.csv"
                                        csvStatus = CsvExportHelper.saveCsvToDownloads(context, fileName, buildCsv())
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_mep_csv_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حفظ CSV", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val fileName = "Kitchen3D_MEP_${System.currentTimeMillis()}.csv"
                                        CsvExportHelper.shareCsv(context, fileName, buildCsv())
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_mep_csv_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مشاركة CSV", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        csvStatus?.let {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(it, color = Color(0xFF34D399), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/** Mini 2D plan: room outline, fixtures, utility points, pipe polylines. */
@Composable
private fun MepSchematicCanvas(
    room: RoomStructure,
    elements: List<KitchenElement>,
    pipePlan: MepEngine.PipePlan,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val pad = 16f
        val scale = minOf(
            (size.width - pad * 2) / room.widthMm,
            (size.height - pad * 2) / room.depthMm
        ).coerceAtLeast(0.001f)
        val offX = (size.width - room.widthMm * scale) / 2f
        val offY = (size.height - room.depthMm * scale) / 2f
        fun pt(xMm: Float, zMm: Float) = Offset(offX + xMm * scale, offY + zMm * scale)

        // Room outline.
        drawRect(
            color = Color(0xFF334155),
            topLeft = pt(0f, 0f),
            size = androidx.compose.ui.geometry.Size(room.widthMm * scale, room.depthMm * scale),
            style = Stroke(width = 2f)
        )
        // Fixtures as small rects.
        for (el in elements) {
            val col = when (el.type) {
                KitchenElementType.BASE_SINK_UNIT -> Color(0xFF38BDF8)
                KitchenElementType.DISHWASHER_INTEGRATED -> Color(0xFF22D3EE)
                KitchenElementType.COOKTOP_INDUCTION -> Color(0xFFF87171)
                KitchenElementType.TALL_FRIDGE_HOUSING -> Color(0xFF94A3B8)
                else -> continue
            }
            val w = el.dimensions.x * scale
            val d = el.dimensions.z * scale
            drawRect(
                color = col,
                topLeft = pt(el.position.x - el.dimensions.x / 2f, el.position.z - el.dimensions.z / 2f),
                size = androidx.compose.ui.geometry.Size(w, d)
            )
        }
        // Pipe runs.
        for (run in pipePlan.runs) {
            val col = Color(run.kind.colorHex)
            val pts = run.points.map { pt(it.xMm, it.zMm) }
            for (i in 0 until pts.size - 1) {
                if (run.kind == MepEngine.PipeKind.SUPPLY_HOT) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(pts[i].x, pts[i].y)
                        lineTo(pts[i + 1].x, pts[i + 1].y)
                    }
                    drawPath(
                        path = path, color = col,
                        style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f))
                    )
                } else {
                    drawLine(col, pts[i], pts[i + 1], strokeWidth = 3f)
                }
            }
        }
        // Utility points as dots.
        for (wall in room.walls) {
            for (pt in wall.utilityPoints) {
                val p = MepEngine.wallPlanPoint(room, wall.side, pt.offsetFromWallStartMm, 0f)
                drawCircle(
                    color = Color(pt.type.colorHex),
                    radius = 6f,
                    center = pt(p.xMm, p.zMm)
                )
            }
        }
    }
}
