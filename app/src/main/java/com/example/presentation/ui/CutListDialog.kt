package com.example.presentation.ui

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
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.cam.ParametricCabinet
import com.example.core.export.CsvExportHelper
import kotlinx.coroutines.launch
import kotlin.math.ceil

/**
 * v1.18 — simplified cut list (Chinese feature 10, 拆单, simplified).
 *
 * The CAM engine ([ParametricCabinet.generateCarcassCutParts]) and the
 * DXF/PDF exports already compute panels, edge banding and hardware — what
 * was missing was a readable per-cabinet list on the phone, plus a CSV the
 * workshop can open. This dialog is that list: every cabinet → its panels
 * with dimensions, edge banding and hardware, totals, and a CSV export.
 */
@Composable
fun CutListDialog(
    cabinets: List<ParametricCabinet>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var csvStatus by remember { mutableStateOf<String?>(null) }

    val partsByCabinet = remember(cabinets) {
        cabinets.associateWith { it.generateCarcassCutParts() }
    }
    val hardwareByCabinet = remember(cabinets) {
        cabinets.associateWith { it.calculateHardwareRequirements() }
    }
    val totalPanels = partsByCabinet.values.sumOf { parts -> parts.sumOf { it.quantity } }
    val totalAreaSqM = partsByCabinet.values.sumOf { parts -> parts.sumOf { it.areaSqM.toDouble() } }.toFloat()
    val totalEdgeM = partsByCabinet.values.sumOf { parts -> parts.sumOf { it.edgeBandingMeters.toDouble() } }.toFloat()
    // One 2440x1220 mm melamine sheet = 2.976 m².
    val estimatedSheets = ceil(totalAreaSqM / 2.976f).toInt().coerceAtLeast(if (totalPanels == 0) 0 else 1)

    fun buildCsv(): String {
        val rows = mutableListOf<List<String>>()
        rows.add(listOf("الخزانة", "القطعة", "الفئة", "العرض (مم)", "الطول (مم)", "السماكة (مم)", "الكمية", "شريط الحافة (م)"))
        for ((cabinet, parts) in partsByCabinet) {
            for (part in parts) {
                rows.add(
                    listOf(
                        cabinet.name, part.partName, part.category.titleAr,
                        "%.0f".format(part.widthMm), "%.0f".format(part.lengthMm),
                        "%.0f".format(part.thicknessMm), part.quantity.toString(),
                        "%.2f".format(part.edgeBandingMeters)
                    )
                )
            }
        }
        rows.add(emptyList())
        rows.add(listOf("الخزانة", "الإكسسوار", "الكمية"))
        for ((cabinet, hardware) in hardwareByCabinet) {
            for ((name, qty) in hardware) {
                rows.add(listOf(cabinet.name, name, qty.toString()))
            }
        }
        rows.add(emptyList())
        rows.add(listOf("الإجمالي", "عدد الألواح", totalPanels.toString()))
        rows.add(listOf("الإجمالي", "المساحة (م²)", "%.2f".format(totalAreaSqM)))
        rows.add(listOf("الإجمالي", "شريط الحافة (م)", "%.2f".format(totalEdgeM)))
        rows.add(listOf("الإجمالي", "ألواح 2440×1220 تقديرية", estimatedSheets.toString()))
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
                .testTag("cut_list_dialog"),
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
                        Icon(Icons.Default.ContentCut, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("قائمة القطع — تفكيك الخزائن", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("الإجمالي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("• ${cabinets.size} خزانة — $totalPanels لوحاً", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                                Text("• المساحة الإجمالية: ${"%.2f".format(totalAreaSqM)} م² (≈ $estimatedSheets لوح 2440×1220)", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                                Text("• شريط الحافة الإجمالي: ${"%.1f".format(totalEdgeM)} م", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                                Text(
                                    "الأبعاد محسوبة من محرك الخزائن البارامترية (خصم سمك اللوح والظهر)؛ عدد الألواح تقديري قبل التعشيق.",
                                    color = Color(0xFF94A3B8), fontSize = 11.sp
                                )
                            }
                        }
                    }

                    if (cabinets.isEmpty()) {
                        item {
                            Text("لا خزائن في المشهد — أضف خزائن أولاً.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        }
                    }

                    items(cabinets) { cabinet ->
                        val parts = partsByCabinet[cabinet].orEmpty()
                        val hardware = hardwareByCabinet[cabinet].orEmpty()
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "${cabinet.name} — ${cabinet.widthMm.toInt()}×${cabinet.heightMm.toInt()}×${cabinet.depthMm.toInt()} مم",
                                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                for (part in parts) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            part.partName,
                                            color = Color(0xFFCBD5E1), fontSize = 12.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            "${part.widthMm.toInt()}×${part.lengthMm.toInt()}×${part.thicknessMm.toInt()} مم  ×${part.quantity}  • حافة ${"%.1f".format(part.edgeBandingMeters)} م",
                                            color = Color(0xFF94A3B8), fontSize = 12.sp
                                        )
                                    }
                                }
                                if (hardware.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    for ((name, qty) in hardware) {
                                        Text("• $name ×$qty", color = Color(0xFFFBBF24), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val fileName = "Kitchen3D_CutList_${System.currentTimeMillis()}.csv"
                                        csvStatus = CsvExportHelper.saveCsvToDownloads(context, fileName, buildCsv())
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_cutlist_csv_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حفظ CSV", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val fileName = "Kitchen3D_CutList_${System.currentTimeMillis()}.csv"
                                        CsvExportHelper.shareCsv(context, fileName, buildCsv())
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_cutlist_csv_button")
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
