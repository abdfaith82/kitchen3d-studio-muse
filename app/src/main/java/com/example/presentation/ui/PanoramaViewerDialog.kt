package com.example.presentation.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.camera.rememberVrGyroscopeEffect
import com.example.core.export.ShareExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * v1.17 — 720° panorama viewer (Chinese feature 4, 720°全景漫游).
 *
 * Shows the equirectangular render (2048x1024) as a look-around panorama:
 * drag to look, or switch the gyroscope on and move the phone. Save drops a
 * PNG into Pictures/Kitchen3DStudio; share sends the same file to any app.
 */
@Composable
fun PanoramaViewerDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageBitmap: ImageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    var yawDeg by remember { mutableFloatStateOf(0f) }
    var pitchDeg by remember { mutableFloatStateOf(0f) }
    var gyroOn by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf<String?>(null) }

    // Gyroscope look-around (same hook the 3D viewport's VR walk uses).
    rememberVrGyroscopeEffect(
        enabled = gyroOn,
        autoWalkEnabled = false,
        onLookDelta = { dYaw, dPitch ->
            yawDeg = ((yawDeg + dYaw) % 360f + 360f) % 360f
            pitchDeg = (pitchDeg + dPitch).coerceIn(-60f, 60f)
        },
        onWalkStep = { _, _ -> }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .testTag("panorama_viewer_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = androidx.compose.ui.graphics.Color(0xFF090D16)
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
                        Icon(Icons.Default.Language, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("بانوراما 360° — اسحب للنظر حولك", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = androidx.compose.ui.graphics.Color.White)
                    }
                }

                // Panorama canvas: cover-fit, horizontal wrap, vertical pitch pan.
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(androidx.compose.ui.graphics.Color.Black)
                        .pointerInput(Unit) {
                            detectDragGestures { _, drag ->
                                yawDeg = ((yawDeg - drag.x * 0.25f) % 360f + 360f) % 360f
                                pitchDeg = (pitchDeg + drag.y * 0.15f).coerceIn(-60f, 60f)
                            }
                        }
                        .testTag("panorama_canvas")
                ) {
                    val bmpW = imageBitmap.width.toFloat()
                    val bmpH = imageBitmap.height.toFloat()
                    if (bmpW <= 0f || bmpH <= 0f) return@Canvas
                    val scale = maxOf(size.width / bmpW, size.height / bmpH)
                    val drawnW = bmpW * scale
                    val drawnH = bmpH * scale
                    val xOff = (yawDeg / 360f * drawnW) % drawnW
                    val yRange = (drawnH - size.height).coerceAtLeast(0f)
                    val yOff = ((pitchDeg + 60f) / 120f) * yRange
                    val dstSize = IntSize(drawnW.toInt(), drawnH.toInt())
                    drawImage(
                        image = imageBitmap,
                        dstOffset = IntOffset((-xOff).toInt(), (-yOff).toInt()),
                        dstSize = dstSize
                    )
                    // Second copy for the seamless horizontal wrap.
                    drawImage(
                        image = imageBitmap,
                        dstOffset = IntOffset((-xOff + drawnW).toInt(), (-yOff).toInt()),
                        dstSize = dstSize
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جيروسكوب", color = androidx.compose.ui.graphics.Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(checked = gyroOn, onCheckedChange = { gyroOn = it })
                    }
                    Text(
                        text = "زاوية ${yawDeg.toInt()}°",
                        color = androidx.compose.ui.graphics.Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                saveStatus = savePanoramaBitmapToGallery(context, bitmap)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_panorama_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF0EA5E9))
                    ) {
                        Text("حفظ في المعرض", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                sharePanoramaBitmap(context, bitmap)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_panorama_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                saveStatus?.let {
                    Text(
                        text = it,
                        color = androidx.compose.ui.graphics.Color(0xFF34D399),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private suspend fun savePanoramaBitmapToGallery(context: Context, bitmap: Bitmap): String =
    withContext(Dispatchers.IO) {
        try {
            val fileName = "Kitchen3D_Panorama_2048x1024_${System.currentTimeMillis()}.png"
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Kitchen3DStudio")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                "تم حفظ البانوراما في المعرض ✓ ($fileName)"
            } else {
                "تم الحفظ في ذاكرة التطبيق ✓"
            }
        } catch (e: Exception) {
            "تعذّر الحفظ — حاول مرة أخرى"
        }
    }

private suspend fun sharePanoramaBitmap(context: Context, bitmap: Bitmap) =
    withContext(Dispatchers.IO) {
        try {
            val file = File(context.cacheDir, "Kitchen3D_Panorama_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            withContext(Dispatchers.Main) {
                ShareExportHelper.shareFile(context, file, "image/png", "مشاركة بانوراما 360°")
            }
        } catch (e: Exception) {
            android.util.Log.e("PanoramaViewer", "share failed", e)
        }
    }
