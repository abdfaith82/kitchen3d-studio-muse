package com.example.core.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Handles sharing exported files (.room, .pdf, .dxf, .json, .jpg) via Android Intent Chooser,
 * including direct blueprint sharing with workshops & friends via Social Media / Messaging apps
 * and Email with pre-populated engineering specifications.
 */
object ShareExportHelper {

    const val MIME_PDF = "application/pdf"
    const val MIME_DXF = "application/dxf"
    const val MIME_JSON = "application/json"
    const val MIME_JPEG = "image/jpeg"
    const val MIME_ROOM = "application/octet-stream"
    const val MIME_APK = "application/vnd.android.package-archive"

    /**
     * v1.12 (P1-9) — exports used to live only in cacheDir and only leave via
     * the share sheet, so dismissing the sheet lost the file. This also writes
     * a copy to Downloads/Kitchen3DStudio (MediaStore, no permission needed
     * on API 29+; legacy public dir below that), so the file survives even if
     * the share sheet is dismissed. Returns the display path, or null.
     */
    fun copyToDownloads(context: Context, file: File, mimeType: String): String? {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/Kitchen3DStudio")
                }
                val uri = context.contentResolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                ) ?: return null
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { input -> input.copyTo(out) }
                }
                "التنزيلات/Kitchen3DStudio/${file.name}"
            } else {
                val dir = File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    ),
                    "Kitchen3DStudio"
                ).apply { mkdirs() }
                val target = File(dir, file.name)
                file.copyTo(target, overwrite = true)
                "التنزيلات/Kitchen3DStudio/${file.name}"
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Saves a Downloads copy (P1-9) and tells the user where it went. */
    private fun saveDownloadsCopy(context: Context, file: File, mimeType: String) {
        copyToDownloads(context, file, mimeType)?.let { path ->
            Toast.makeText(context, "تم حفظ نسخة في $path", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Copies the currently running compiled APK (`context.applicationInfo.sourceDir`) into
     * `cacheDir/exports/Kitchen3DStudio_Release.apk` and opens the system Share sheet.
     */
    fun exportAndShareInstalledApk(context: Context): File {
        val sourceApk = File(context.applicationInfo.sourceDir)
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val targetApk = File(exportDir, "Kitchen3DStudio_Release.apk")
        sourceApk.copyTo(targetApk, overwrite = true)
        shareFile(
            context = context,
            file = targetApk,
            mimeType = MIME_APK,
            dialogTitle = "تصدير ومشاركة ملف التطبيق (Kitchen3DStudio_Release.apk)"
        )
        return targetApk
    }

    fun getUriForFile(context: Context, file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun shareFile(
        context: Context,
        file: File,
        mimeType: String,
        dialogTitle: String = "مشاركة ملف المشروع"
    ) {
        try {
            saveDownloadsCopy(context, file, mimeType)
            val uri = getUriForFile(context, file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, dialogTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.util.Log.e("ShareExport", "share sheet failed", e)
            Toast.makeText(
                context,
                "تعذّر فتح نافذة المشاركة — الملف محفوظ في التنزيلات/Kitchen3DStudio.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Shares a generated kitchen blueprint (PDF, High-Res JPG, DXF, or JSON) directly via
     * Social Media & Messaging apps (WhatsApp, Telegram, Messenger, etc.) with a complete
     * formatted engineering summary for workshops or friends.
     */
    fun shareBlueprintViaSocialOrMessaging(
        context: Context,
        file: File,
        mimeType: String,
        projectName: String,
        workshopName: String,
        elementsCount: Int,
        roomDimensionsText: String
    ) {
        try {
            saveDownloadsCopy(context, file, mimeType)
            val uri = getUriForFile(context, file)
            val summaryBody = buildString {
                appendLine("📐 مخطط مطبخ تنفيذي وهندسي — $projectName")
                appendLine("🏢 الورشة / المعرض: $workshopName")
                appendLine("📏 أبعاد الغرفة: $roomDimensionsText")
                appendLine("🗄️ إجمالي الوحدات والخزائن: $elementsCount وحدة (كاملة بظهر وجنب 360°)")
                appendLine("📎 مرفق ملف المخطط التنفيذي وقائمة المواد والقص (BOM & Cut-List).")
            }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "مخطط مطبخ تنفيذي: $projectName")
                putExtra(Intent.EXTRA_TEXT, summaryBody)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(
                shareIntent,
                "مشاركة المخطط مع الورش والأصدقاء عبر التواصل الاجتماعي"
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.util.Log.e("ShareExport", "social share failed", e)
            Toast.makeText(
                context,
                "تعذّرت المشاركة عبر التواصل الاجتماعي — الملف محفوظ في التنزيلات/Kitchen3DStudio.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Shares a generated kitchen blueprint (PDF / JPG / DXF) with a workshop or friend via Email
     * (`Intent.EXTRA_EMAIL`, `EXTRA_SUBJECT`, `EXTRA_TEXT`, and `EXTRA_STREAM` attachment).
     */
    fun shareBlueprintViaEmail(
        context: Context,
        file: File,
        mimeType: String,
        recipientEmail: String,
        projectName: String,
        workshopName: String,
        elementsCount: Int,
        roomDimensionsText: String
    ) {
        try {
            saveDownloadsCopy(context, file, mimeType)
            val uri = getUriForFile(context, file)
            val subject = "طلب تسعير وتنفيذ مخطط مطبخ هندسي: $projectName ($workshopName)"
            val emailBody = buildString {
                appendLine("تحية طيبة،")
                appendLine()
                appendLine("مرفق لكم المخطط الهندسي والتنفيذي الكامل لمشروع المطبخ:")
                appendLine("• اسم المشروع: $projectName")
                appendLine("• اسم الورشة / الجهة: $workshopName")
                appendLine("• أبعاد الغرفة: $roomDimensionsText")
                appendLine("• عدد الخزائن والوحدات: $elementsCount وحدة (شاملة قوائم القص BOM والأجناب والظهر الكامل)")
                appendLine()
                appendLine("يرجى مراجعة المخطط المرفق للتنفيذ أو إرسال عرض السعر.")
            }
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                if (recipientEmail.isNotBlank()) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail.trim()))
                }
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, emailBody)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(
                emailIntent,
                "إرسال المخطط بالبريد الإلكتروني للورشة أو الأصدقاء"
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.util.Log.e("ShareExport", "email share failed", e)
            Toast.makeText(
                context,
                "تعذّر فتح تطبيق البريد الإلكتروني — الملف محفوظ في التنزيلات/Kitchen3DStudio.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
