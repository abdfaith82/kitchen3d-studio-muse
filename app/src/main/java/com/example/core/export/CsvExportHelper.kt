package com.example.core.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * v1.18 — CSV export (Chinese feature 10, 拆单, and the water/electric list).
 * The app had PDF/DXF exports but no CSV — the format a workshop or factory
 * actually opens in a spreadsheet. [buildCsv] is pure and unit-tested.
 */
object CsvExportHelper {

    /** Builds CSV text from rows; fields with , " or a newline are quoted. */
    fun buildCsv(rows: List<List<String>>): String {
        val sb = StringBuilder()
        // BOM so Excel/Sheets open Arabic correctly.
        sb.append('\uFEFF')
        for (row in rows) {
            sb.append(row.joinToString(",") { field ->
                val needsQuotes = field.contains(',') || field.contains('"') ||
                    field.contains('\n') || field.contains('\r')
                if (!needsQuotes) field
                else "\"" + field.replace("\"", "\"\"") + "\""
            })
            sb.append("\r\n")
        }
        return sb.toString()
    }

    /** Saves CSV text into Downloads/Kitchen3DStudio and returns a status line. */
    suspend fun saveCsvToDownloads(context: Context, fileName: String, csv: String): String =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Downloads.RELATIVE_PATH, "Download/Kitchen3DStudio")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        out.write(csv.toByteArray(Charsets.UTF_8))
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }
                    "تم حفظ ملف CSV في التنزيلات ✓ ($fileName)"
                } else {
                    "تعذّر الحفظ — تحقق من مساحة التخزين"
                }
            } catch (e: Exception) {
                "تعذّر الحفظ — ${e.message ?: "خطأ غير معروف"}"
            }
        }

    /** Writes the CSV to a cache file and opens the share sheet for it. */
    suspend fun shareCsv(context: Context, fileName: String, csv: String) =
        withContext(Dispatchers.IO) {
            try {
                val file = File(context.cacheDir, fileName)
                FileOutputStream(file).use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                }
                withContext(Dispatchers.Main) {
                    ShareExportHelper.shareFile(context, file, "text/csv", "مشاركة قائمة CSV")
                }
            } catch (e: Exception) {
                android.util.Log.e("CsvExport", "share failed", e)
            }
        }
}
