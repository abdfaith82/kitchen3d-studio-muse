package com.example.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.geometry.GrowthTourEngine
import com.example.domain.model.KitchenElement
import kotlinx.coroutines.delay

/**
 * v1.17 — growth animation tour (Chinese feature 13, 生长动画).
 *
 * Plays the kitchen being built piece by piece in the 3D viewport: each step
 * reveals the next element in [GrowthTourEngine.buildOrder]. The reveal is
 * transient — dismissing the dialog (or finishing) restores the full kitchen.
 * The dialog drives the stage through [onStageChange]; null means "show all".
 */
@Composable
fun GrowthTourDialog(
    orderedElements: List<KitchenElement>,
    onStageChange: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val total = orderedElements.size
    var shown by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }

    LaunchedEffect(playing, shown) {
        if (playing && shown < total) {
            delay(450L)
            val next = shown + 1
            shown = next
            onStageChange(next)
            if (next >= total) playing = false
        }
    }

    val currentLabel = if (shown < total) {
        GrowthTourEngine.stageLabelAr(GrowthTourEngine.stageOf(orderedElements[shown].type))
    } else {
        "اكتمل النمو — المطبخ كامل"
    }

    Dialog(
        onDismissRequest = {
            onStageChange(null)
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("growth_tour_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF0B1120)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "جولة النمو — المطبخ يُبنى قطعة قطعة",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    IconButton(onClick = {
                        onStageChange(null)
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "$shown / $total — $currentLabel",
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else shown.toFloat() / total.toFloat() },
                    color = Color(0xFF8B5CF6),
                    trackColor = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Slider(
                    value = shown.toFloat(),
                    onValueChange = {
                        playing = false
                        shown = it.toInt()
                        onStageChange(shown)
                    },
                    valueRange = 0f..total.toFloat(),
                    steps = (total - 1).coerceAtLeast(0),
                    modifier = Modifier.testTag("growth_tour_slider")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (shown >= total) {
                                shown = 0
                                onStageChange(0)
                            }
                            playing = !playing
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("growth_tour_play_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                    ) {
                        Icon(
                            if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (playing) "إيقاف مؤقت" else "تشغيل", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            onStageChange(null)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("عرض الكل وإغلاق", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "شاهد في العرض ثلاثي الأبعاد خلف هذه النافذة: الخزائن العالية أولاً، ثم السفلية، ثم العلوية، ثم الأجهزة.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }
    }
}
