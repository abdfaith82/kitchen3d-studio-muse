package com.example.presentation.ui

/** v1.12 (P1-10) — Magic Hour returns English statuses ("processing",
 * "queued", "error"...); the user sees Arabic. Unknown values pass through. */
private fun arabicVideoStatus(status: String): String = when (status) {
    "complete" -> "مكتمل"
    "processing" -> "قيد المعالجة"
    "queued" -> "في الانتظار"
    "error" -> "فشل"
    "canceled" -> "أُلغي"
    else -> status
}

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.ai.KitchenAiStylePreference
import com.example.core.media.KitchenVisualPrompt
import com.example.core.media.MagicHourClient
import com.example.core.media.MagicHourKeyStore
import com.example.core.media.PollinationsImageClient
import com.example.domain.model.RoomStructure
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class FreeImageSite(
    val id: String,
    val titleAr: String,
    val url: String,
    val noteAr: String
)

/**
 * The four free image models from the user's screenshot (verified
 * 2026-11-24 in the sibling app — the screenshot's own claims were partly
 * wrong: Hailuo has no daily credit and Pika's free plan is 0 credits, so
 * they are NOT listed here; only the four verified sites are).
 */
private val FREE_IMAGE_SITES = listOf(
    FreeImageSite(
        id = "raphael",
        titleAr = "Raphael AI",
        url = "https://raphael.app",
        noteAr = "بلا حدود على النموذج الأساسي وبلا تسجيل — لكن الصور المجانية بعلامة مائية وللاستخدام غير التجاري فقط."
    ),
    FreeImageSite(
        id = "leonardo",
        titleAr = "Leonardo.ai",
        url = "https://leonardo.ai",
        noteAr = "150 نقطة مجانية كل يوم تتجدد (نحو 18 صورة يومياً)، بلا علامة مائية — لكن الصور الناتجة عامة."
    ),
    FreeImageSite(
        id = "bingimage",
        titleAr = "Bing Image Creator",
        url = "https://www.bing.com/images/create",
        noteAr = "من مايكروسوفت — 15 إنشاءً سريعاً كل يوم بحساب شخصي، ثم سرعة عادية حتى 200 موجّه يومياً، بلا علامة مائية."
    ),
    FreeImageSite(
        id = "metaai",
        titleAr = "Meta AI",
        url = "https://meta.ai",
        noteAr = "من ميتا — صور بلا حدود تقريباً مجاناً وبدون علامة مائية، لكن تحتاج تسجيل دخول بحساب فيسبوك أو إنستغرام."
    )
)

/**
 * v1.10 — video source rule: the user's own camera/gallery photo wins
 * over the generated image — the video is animated FROM the photo.
 * Pure function, unit-tested.
 */
fun chooseVideoSourceFile(referenceFile: File?, generatedFile: File?): File? =
    referenceFile ?: generatedFile

/** Saves a camera/gallery bitmap as a JPEG in the app cache (for upload). */
fun saveBitmapToCacheFile(context: Context, bitmap: Bitmap, name: String): File? {
    return try {
        val file = File(context.cacheDir, name)
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        if (file.exists() && file.length() > 1024) file else null
    } catch (_: Exception) {
        null
    }
}

/**
 * v1.9 — Kitchen Media: the same chain the user asked for in the sibling
 * app, adapted to this kitchen: text → image → video.
 *  1) 📝 نص: an English visual prompt is built automatically from the
 *     current room (dims, layout, style, island) — editable.
 *  2) 🖼️ صورة: Pollinations generates a photorealistic render keylessly
 *     (flux, 1024×576), shown in-app and saved to the gallery.
 *  3) 🎬 فيديو: Magic Hour animates that image (free key, encrypted on
 *     device) into a 5s clip, played in-app and saved to the gallery.
 *  4) 🌐 مواقع: the four verified free image sites from the screenshot
 *     open in WebView tabs, with a one-tap "copy prompt" button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenMediaDialog(
    currentRoom: RoomStructure,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }

    // --- v1.10 camera: the user's own kitchen photo as the chain's input.
    // TakePicturePreview needs the CAMERA permission at runtime; the photo
    // picker needs none. The photo is saved as a cache JPEG so Magic Hour
    // can upload it as the video's first frame.
    var referenceFile by remember { mutableStateOf<File?>(null) }
    var referenceBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var referenceSource by remember { mutableStateOf<String?>(null) } // "camera" | "gallery"
    var cameraStatus by remember { mutableStateOf<String?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    fun usePhotoBitmap(bmp: Bitmap, source: String) {
        val file = saveBitmapToCacheFile(
            context, bmp, "kitchen-camera-ref-${System.currentTimeMillis()}.jpg"
        )
        if (file != null) {
            referenceFile = file
            referenceBitmap = bmp.asImageBitmap()
            referenceSource = source
            cameraStatus = if (source == "camera") {
                "تم التقاط صورة مطبخك ✓ — ستكون الإطار الأول للفيديو."
            } else {
                "تم اختيار الصورة من المعرض ✓ — ستكون الإطار الأول للفيديو."
            }
        } else {
            cameraStatus = "تعذر حفظ الصورة — جرّب مرة أخرى."
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? -> if (bmp != null) usePhotoBitmap(bmp, "camera") }
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.let { bmp ->
                        usePhotoBitmap(bmp, "gallery")
                    }
                }
            } catch (_: Exception) {
                cameraStatus = "تعذر قراءة الصورة المختارة."
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) cameraLauncher.launch(null)
        else cameraStatus = "الكاميرا تحتاج إذناً — اسمح به من إعدادات التطبيق."
    }
    // WebView file chooser (sites tab): lets the user upload the camera
    // photo to Raphael/Leonardo from inside the site.
    var webFileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val webFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        webFileChooserCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
        webFileChooserCallback = null
    }
    val tabs = listOf("🖼️ صورة واقعية", "🎬 فيديو متحرك", "🌐 مواقع مجانية")

    // --- Shared chain state: the prompt and the generated image ---
    var style by remember { mutableStateOf(KitchenAiStylePreference.ROYAL_OAK_AND_CHARCOAL) }
    var includeIsland by remember { mutableStateOf(false) }
    var promptText by remember {
        mutableStateOf(
            KitchenVisualPrompt.buildKitchenImagePrompt(currentRoom, style, includeIsland)
        )
    }
    fun rebuildPrompt() {
        promptText = KitchenVisualPrompt.buildKitchenImagePrompt(currentRoom, style, includeIsland)
    }

    var imageFile by remember { mutableStateOf<File?>(null) }
    var imageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var imageLoading by remember { mutableStateOf(false) }
    var imageStatus by remember { mutableStateOf<String?>(null) }
    var imageSaved by remember { mutableStateOf(false) }

    fun generateImage() {
        if (promptText.isBlank()) {
            imageStatus = "اكتب الوصف أولاً."
            return
        }
        imageLoading = true
        imageStatus = "جاري توليد الصورة الواقعية (بلا مفتاح)..."
        scope.launch {
            val result = PollinationsImageClient.generateImage(
                prompt = promptText,
                cacheDir = context.cacheDir
            )
            imageLoading = false
            result.onSuccess { file ->
                imageFile = file
                imageSaved = false
                imageBitmap = BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                imageStatus = "تم توليد الصورة ✓ — يمكنك حفظها أو تحريكها كفيديو من التبويب التالي."
            }.onFailure { err ->
                imageStatus = "تعذر توليد الصورة (${err.message ?: "خطأ شبكة"}) — الخدمة المجانية قد تكون بطيئة؛ جرّب مرة أخرى أو استخدم تبويب المواقع المجانية."
            }
        }
    }

    // --- Video state (Magic Hour) ---
    var hasMagicHourKey by remember { mutableStateOf(MagicHourKeyStore.hasApiKey(context)) }
    var magicHourKeyInput by remember { mutableStateOf("") }
    var showMagicHourKeyField by remember(hasMagicHourKey) { mutableStateOf(!hasMagicHourKey) }
    var videoModel by remember { mutableStateOf(MagicHourClient.FREE_MODELS.first()) }
    var videoBusy by remember { mutableStateOf(false) }
    var videoStatus by remember { mutableStateOf<String?>(null) }
    var videoProjectId by remember { mutableStateOf<String?>(null) }
    var videoFile by remember { mutableStateOf<File?>(null) }
    // v1.11: polling progress (attempts of 60) + a nonce to resume polling
    // after a timeout without re-uploading the image.
    var videoPollAttempts by remember { mutableIntStateOf(0) }
    var videoPollNonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(videoProjectId, videoPollNonce) {
        val id = videoProjectId ?: return@LaunchedEffect
        val key = MagicHourKeyStore.loadApiKey(context) ?: return@LaunchedEffect
        var attempts = 0
        videoPollAttempts = 0
        while (attempts < 60) {
            delay(5000)
            attempts++
            videoPollAttempts = attempts
            val res = MagicHourClient.projectStatus(id, key)
            res.onSuccess { (status, url) ->
                videoStatus = "حالة الفيديو: ${arabicVideoStatus(status)} (تحقق $attempts)"
                when (status) {
                    "complete" -> {
                        if (url != null) {
                            val target = File(context.cacheDir, "kitchen-video-${System.currentTimeMillis()}.mp4")
                            MagicHourClient.downloadUrl(url, target).onSuccess { file ->
                                videoFile = file
                                videoBusy = false
                                videoStatus = "اكتمل الفيديو ✓ — شغّله بالأسفل أو احفظه في المعرض."
                            }.onFailure {
                                videoBusy = false
                                videoStatus = "اكتمل الفيديو لكن تعذر تنزيله — جرّب مرة أخرى."
                            }
                        } else {
                            videoBusy = false
                            videoStatus = "اكتمل الفيديو لكن بلا رابط تنزيل."
                        }
                        return@LaunchedEffect
                    }
                    "error", "canceled" -> {
                        videoBusy = false
                        videoProjectId = null
                        videoStatus = "فشل توليد الفيديو (${arabicVideoStatus(status)}) — جرّب نموذجاً آخر أو أعد المحاولة."
                        return@LaunchedEffect
                    }
                }
            }.onFailure { err ->
                videoStatus = "تعذر التحقق من الحالة: ${err.message}"
            }
        }
        videoBusy = false
        videoStatus = "انتهت مهلة الانتظار (5 دقائق) — الفيديو قد يكتمل لاحقاً؛ أعد فتح النافذة والمحاولة."
    }

    fun startVideo() {
        val file = chooseVideoSourceFile(referenceFile, imageFile)
        if (file == null) {
            videoStatus = "صوّر مطبخك بالكاميرا أو ولّد صورة أولاً من تبويب «صورة واقعية»."
            return
        }
        val key = MagicHourKeyStore.loadApiKey(context)
        if (key.isNullOrBlank()) {
            videoStatus = "احفظ مفتاح Magic Hour المجاني أولاً (من magichour.ai)."
            return
        }
        videoBusy = true
        videoFile = null
        videoStatus = "جاري رفع الصورة وتحويلها إلى فيديو..."
        scope.launch {
            val uploaded = MagicHourClient.uploadImage(file, key)
            uploaded.onFailure { err ->
                videoBusy = false
                videoStatus = "تعذر رفع الصورة: ${err.message}"
                return@launch
            }
            val submitted = MagicHourClient.submitImageToVideo(
                prompt = promptText,
                model = videoModel,
                durationSec = 5,
                imageFilePath = uploaded.getOrNull() ?: "",
                key = key
            )
            submitted.onSuccess { id ->
                videoProjectId = id
                videoStatus = "بدأ توليد الفيديو — سأتحقق من حالته كل 5 ثوانٍ..."
            }.onFailure { err ->
                videoBusy = false
                videoStatus = "تعذر بدء الفيديو: ${err.message}"
            }
        }
    }

    // --- Free sites tab state ---
    var selectedSite by remember { mutableIntStateOf(0) }
    var promptCopied by remember { mutableStateOf(false) }
    // Plain holder (not State): which site URL the WebView already loaded.
    // Reading webView.url would reload the site home whenever the user
    // navigates inside it, so the loaded URL is tracked separately.
    val webViewLoadedUrl = remember { arrayOf<String?>(null) }
    // v1.11: holder for the WebView instance so the reload button can reach it.
    val webViewRef = remember { arrayOf<WebView?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.94f),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "صور وفيديو المطبخ 🎨🎬",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "السلسلة: وصف نصي من مشروعك ← صورة واقعية ← فيديو متحرك",
                            fontSize = 11.sp,
                            color = Color(0xFFA78BFA)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_kitchen_media_dialog")) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                TabRow(
                    selectedTabIndex = tab,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White
                ) {
                    tabs.forEachIndexed { i, title ->
                        Tab(
                            selected = tab == i,
                            onClick = { tab = i },
                            text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("kitchen_media_tab_$i")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // v1.11: the browser tab is NOT a LazyColumn item anymore —
                // the WebView fills the dialog and scrolls with its own
                // gestures (inside a LazyColumn the outer list stole the
                // swipes and the page sat in a fixed 480dp box). Tabs 0/1 stay lazy.
                if (tab == 2) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                    // ===== FREE SITES TAB =====
                        Text(
                            text = "مواقع توليد الصور المجانية من لقطتك — افتح أياً منها، وسجّل دخولك إن طلب، ثم الصق الوصف المنسوخ من زر النسخ بالأسفل.",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                        OutlinedButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("kitchen prompt", promptText))
                                promptCopied = true
                            },
                            modifier = Modifier.fillMaxWidth().testTag("copy_media_prompt_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (promptCopied) "تم نسخ الوصف ✓ — الصقه في الموقع" else "📋 نسخ وصف الصورة",
                                fontSize = 12.sp
                            )
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(FREE_IMAGE_SITES) { site ->
                                val sel = FREE_IMAGE_SITES[selectedSite] == site
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (sel) Color(0xFF4C1D95) else Color(0xFF1E293B),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (sel) Color(0xFFA78BFA) else Color(0xFF334155)
                                    ),
                                    modifier = Modifier
                                        .clickable { selectedSite = FREE_IMAGE_SITES.indexOf(site) }
                                        .testTag("free_site_${site.id}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Public,
                                            contentDescription = null,
                                            tint = Color(0xFFA78BFA),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = site.titleAr, color = Color.White, fontSize = 11.sp)
                                        if (sel) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(13.dp))
                                        }
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = FREE_IMAGE_SITES[selectedSite].noteAr,
                                color = Color(0xFFF59E0B),
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = { webViewRef[0]?.reload() },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("reload_free_site_button")
                            ) {
                                Text("تحديث ⟳", fontSize = 11.sp)
                            }
                        }
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                            settings.setSupportZoom(true)
                                            settings.builtInZoomControls = true
                                            settings.displayZoomControls = false
                                    webViewClient = WebViewClient()
                                    // Upload support: a site's "upload image" button
                                    // opens the gallery and hands the photo to the page.
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onShowFileChooser(
                                            view: WebView?,
                                            filePathCallback: ValueCallback<Array<Uri>>,
                                            fileChooserParams: FileChooserParams
                                        ): Boolean {
                                            webFileChooserCallback?.onReceiveValue(null)
                                            webFileChooserCallback = filePathCallback
                                            webFilePicker.launch("image/*")
                                            return true
                                        }
                                    }
                                    val startUrl = FREE_IMAGE_SITES[selectedSite].url
                                    loadUrl(startUrl)
                                    webViewLoadedUrl[0] = startUrl
                                            webViewRef[0] = this
                                }
                            },
                            update = { webView ->
                                val url = FREE_IMAGE_SITES[selectedSite].url
                                if (webViewLoadedUrl[0] != url) {
                                    webView.loadUrl(url)
                                    webViewLoadedUrl[0] = url
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .testTag("free_image_site_webview")
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (tab) {
                        0 -> {
                            // ===== IMAGE TAB =====
                            item {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF1E3A5F).copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                                    modifier = Modifier.fillMaxWidth().testTag("camera_reference_card")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "📷 صورة مرجعية من كاميرتك (اختياري)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "صوّر مطبخك الحقيقي — الصورة تصبح الإطار الأول للفيديو في التبويب التالي، ويمكنك رفعها لمواقع الصور من تبويب المواقع.",
                                            fontSize = 10.sp,
                                            color = Color(0xFFCBD5E1)
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    if (hasCameraPermission) cameraLauncher.launch(null)
                                                    else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                                modifier = Modifier.weight(1f).testTag("camera_capture_button")
                                            ) {
                                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("الكاميرا", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    galleryPicker.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                                modifier = Modifier.weight(1f).testTag("gallery_pick_button")
                                            ) {
                                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("من المعرض", fontSize = 11.sp)
                                            }
                                        }
                                        if (cameraStatus != null) {
                                            Text(text = cameraStatus!!, color = Color(0xFFCBD5E1), fontSize = 11.sp)
                                        }
                                        if (referenceBitmap != null) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                androidx.compose.foundation.Image(
                                                    bitmap = referenceBitmap!!,
                                                    contentDescription = "صورة مطبخك المرجعية",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(72.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .testTag("camera_reference_photo")
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "صورتك المرجعية جاهزة ✓",
                                                    color = Color(0xFF34D399),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        referenceFile = null
                                                        referenceBitmap = null
                                                        referenceSource = null
                                                        cameraStatus = null
                                                    },
                                                    modifier = Modifier.testTag("remove_camera_reference_button")
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "إزالة الصورة", tint = Color(0xFFFCA5A5))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                Text(
                                    text = "النمط (يُبنى منه الوصف تلقائياً):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFC4B5FD)
                                )
                            }
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(KitchenAiStylePreference.entries) { s ->
                                        val sel = style == s
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (sel) Color(0xFF4C1D95) else Color(0xFF1E293B),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (sel) Color(0xFFA78BFA) else Color(0xFF334155)
                                            ),
                                            modifier = Modifier
                                                .clickable {
                                                    style = s
                                                    rebuildPrompt()
                                                }
                                                .testTag("media_style_${s.id}")
                                        ) {
                                            Text(
                                                text = s.arabicTitle,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "جزيرة وسطية",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Switch(
                                        checked = includeIsland,
                                        onCheckedChange = {
                                            includeIsland = it
                                            rebuildPrompt()
                                        },
                                        modifier = Modifier.testTag("media_island_switch")
                                    )
                                    Text(
                                        text = "أبعاد الغرفة: ${(currentRoom.widthMm / 1000f)}م × ${(currentRoom.depthMm / 1000f)}م",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            item {
                                OutlinedTextField(
                                    value = promptText,
                                    onValueChange = { promptText = it },
                                    label = { Text("الوصف الإنجليزي للصورة (قابل للتعديل)", fontSize = 11.sp) },
                                    minLines = 3,
                                    maxLines = 6,
                                    modifier = Modifier.fillMaxWidth().testTag("media_prompt_input")
                                )
                            }
                            item {
                                Button(
                                    onClick = { generateImage() },
                                    enabled = !imageLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                    modifier = Modifier.fillMaxWidth().testTag("generate_kitchen_image_button")
                                ) {
                                    if (imageLoading) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text("🎨 توليد صورة واقعية (مجاني — بلا مفتاح)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            if (imageStatus != null) {
                                item {
                                    Text(text = imageStatus!!, color = Color(0xFFCBD5E1), fontSize = 11.sp)
                                }
                            }
                            if (imageBitmap != null) {
                                item {
                                    androidx.compose.foundation.Image(
                                        bitmap = imageBitmap!!,
                                        contentDescription = "صورة المطبخ المولدة",
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .testTag("generated_kitchen_image")
                                    )
                                }
                                item {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    val file = imageFile ?: return@launch
                                                    imageSaved = saveFileToGallery(
                                                        context = context,
                                                        file = file,
                                                        mimeType = "image/jpeg",
                                                        folder = "Pictures/Kitchen3DStudio",
                                                        isVideo = false
                                                    )
                                                    imageStatus = if (imageSaved) "تم الحفظ في المعرض ✓" else "تعذر الحفظ في المعرض."
                                                }
                                            },
                                            modifier = Modifier.weight(1f).testTag("save_kitchen_image_button")
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("حفظ في المعرض", fontSize = 11.sp)
                                        }
                                        OutlinedButton(
                                            onClick = { generateImage() },
                                            enabled = !imageLoading,
                                            modifier = Modifier.weight(1f).testTag("regenerate_kitchen_image_button")
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("صورة أخرى", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // ===== VIDEO TAB =====
                            item {
                                Text(
                                    text = "يُحرَّك الإطار المولّد من تبويب الصورة عبر Magic Hour — مفتاح مجاني بلا بطاقة، لكن الرصيد محدود (نحو 15 مقطعاً قصيراً إجمالاً).",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp
                                )
                            }
                            item {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (hasMagicHourKey) Color(0xFF064E3B).copy(alpha = 0.4f) else Color(0xFF451A03).copy(alpha = 0.45f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (hasMagicHourKey) Color(0xFF10B981) else Color(0xFFF59E0B)
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("magichour_key_card")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = if (hasMagicHourKey) "🔑 مفتاح Magic Hour محفوظ ✓" else "🔑 مفتاح Magic Hour المجاني (مطلوب للفيديو)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "من magichour.ai ← سجّل مجاناً ← API Keys ← انسخ المفتاح. يُحفظ مشفّراً على جهازك فقط.",
                                            fontSize = 10.sp,
                                            color = Color(0xFFCBD5E1)
                                        )
                                        if (hasMagicHourKey && !showMagicHourKeyField) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                    onClick = { showMagicHourKeyField = true },
                                                    modifier = Modifier.weight(1f).testTag("change_magichour_key_button")
                                                ) { Text("تغيير المفتاح", fontSize = 11.sp) }
                                                OutlinedButton(
                                                    onClick = {
                                                        MagicHourKeyStore.clearApiKey(context)
                                                        hasMagicHourKey = false
                                                        showMagicHourKeyField = true
                                                    },
                                                    modifier = Modifier.weight(1f).testTag("clear_magichour_key_button")
                                                ) { Text("مسح المفتاح", fontSize = 11.sp) }
                                            }
                                        } else {
                                            OutlinedTextField(
                                                value = magicHourKeyInput,
                                                onValueChange = { magicHourKeyInput = it.trim() },
                                                label = { Text("الصق مفتاح Magic Hour هنا", fontSize = 11.sp) },
                                                singleLine = true,
                                                visualTransformation = PasswordVisualTransformation(),
                                                modifier = Modifier.fillMaxWidth().testTag("magichour_key_input")
                                            )
                                            Button(
                                                onClick = {
                                                    if (magicHourKeyInput.isNotBlank()) {
                                                        // v1.12 (P2-1): saveApiKey returns Boolean now — never
                                                        // claim "saved ✓" when the KeyStore write failed.
                                                        if (MagicHourKeyStore.saveApiKey(context, magicHourKeyInput)) {
                                                            magicHourKeyInput = ""
                                                            hasMagicHourKey = true
                                                            showMagicHourKeyField = false
                                                        } else {
                                                            videoStatus = "تعذّر حفظ المفتاح على هذا الجهاز — حاول مجدداً."
                                                        }
                                                    }
                                                },
                                                enabled = magicHourKeyInput.isNotBlank(),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                                modifier = Modifier.fillMaxWidth().testTag("save_magichour_key_button")
                                            ) {
                                                Text("حفظ المفتاح", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                val videoSource = chooseVideoSourceFile(referenceFile, imageFile)
                                Text(
                                    text = when {
                                        referenceFile != null -> "🎬 مصدر الفيديو: صورتك من ${if (referenceSource == "camera") "الكاميرا" else "المعرض"} — ستتحرك كما هي."
                                        videoSource != null -> "🎬 مصدر الفيديو: الصورة المولّدة."
                                        else -> "🎬 لا يوجد مصدر بعد — صوّر مطبخك أو ولّد صورة من التبويب الأول."
                                    },
                                    color = Color(0xFFF59E0B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            item {
                                Text(
                                    text = "نموذج الفيديو (مجاني — 480p):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFC4B5FD)
                                )
                            }
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(MagicHourClient.FREE_MODELS) { m ->
                                        val sel = videoModel == m
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (sel) Color(0xFF4C1D95) else Color(0xFF1E293B),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (sel) Color(0xFFA78BFA) else Color(0xFF334155)
                                            ),
                                            modifier = Modifier
                                                .clickable { videoModel = m }
                                                .testTag("video_model_$m")
                                        ) {
                                            Text(
                                                text = m,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            item {
                                Button(
                                    onClick = { startVideo() },
                                    enabled = !videoBusy,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB2777)),
                                    modifier = Modifier.fillMaxWidth().testTag("generate_kitchen_video_button")
                                ) {
                                    if (videoBusy) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("🎬 تحويل الصورة إلى فيديو (5 ثوانٍ)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            if (videoStatus != null) {
                                item {
                                    Text(text = videoStatus!!, color = Color(0xFFCBD5E1), fontSize = 11.sp)
                                }
                            }
                            // v1.11: polling progress + resume/retry controls.
                            if (videoBusy && videoProjectId != null) {
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        LinearProgressIndicator(
                                            progress = { (videoPollAttempts / 60f).coerceIn(0f, 1f) },
                                            color = Color(0xFFEC4899),
                                            trackColor = Color(0xFF1E293B),
                                            modifier = Modifier.fillMaxWidth().height(6.dp)
                                        )
                                        Text(
                                            text = "أتحقق من حالة الفيديو… ($videoPollAttempts من 60) — عادة يجهز خلال دقيقة.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                            if (!videoBusy && videoFile == null && videoProjectId != null) {
                                item {
                                    OutlinedButton(
                                        onClick = { videoPollNonce++ },
                                        modifier = Modifier.fillMaxWidth().testTag("resume_kitchen_video_poll_button")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("متابعة حالة المشروع (دون إعادة الرفع)", fontSize = 11.sp)
                                    }
                                }
                            }
                            if (!videoBusy && videoFile == null && videoProjectId == null &&
                                (videoStatus?.contains("تعذر") == true || videoStatus?.contains("فشل") == true)
                            ) {
                                item {
                                    OutlinedButton(
                                        onClick = { startVideo() },
                                        modifier = Modifier.fillMaxWidth().testTag("retry_kitchen_video_button")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("إعادة المحاولة", fontSize = 11.sp)
                                    }
                                }
                            }
                            if (videoFile != null) {
                                item {
                                    AndroidView(
                                        factory = { ctx ->
                                            VideoView(ctx).apply {
                                                layoutParams = ViewGroup.LayoutParams(
                                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                                )
                                                setVideoPath(videoFile!!.absolutePath)
                                                setOnPreparedListener { mp ->
                                                    mp.isLooping = true
                                                    start()
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(220.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .testTag("generated_kitchen_video")
                                    )
                                }
                                item {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    val saved = saveFileToGallery(
                                                        context = context,
                                                        file = videoFile!!,
                                                        mimeType = "video/mp4",
                                                        folder = "Movies/Kitchen3DStudio",
                                                        isVideo = true
                                                    )
                                                    videoStatus = if (saved) "تم حفظ الفيديو في المعرض ✓" else "تعذر حفظ الفيديو."
                                                }
                                            },
                                            modifier = Modifier.weight(1f).testTag("save_kitchen_video_button")
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("حفظ في المعرض", fontSize = 11.sp)
                                        }
                                        OutlinedButton(
                                            onClick = { startVideo() },
                                            enabled = !videoBusy,
                                            modifier = Modifier.weight(1f).testTag("regenerate_kitchen_video_button")
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("فيديو آخر", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

/** Copies a generated file (image/video) into the public gallery. */
private suspend fun saveFileToGallery(
    context: Context,
    file: File,
    mimeType: String,
    folder: String,
    isVideo: Boolean
): Boolean = withContext(Dispatchers.IO) {
    try {
        val fileName = "Kitchen3D_${System.currentTimeMillis()}.${if (isVideo) "mp4" else "jpg"}"
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            if (isVideo) {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, mimeType)
            } else {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (isVideo) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, folder)
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                } else {
                    put(MediaStore.Images.Media.RELATIVE_PATH, folder)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
        }
        val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, contentValues) ?: return@withContext false
        resolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { input -> input.copyTo(out) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            if (isVideo) contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
            else contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
        true
    } catch (_: Exception) {
        false
    }
}
