package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.model.ChecklistItem
import com.example.model.NoteColor
import com.example.model.NoteItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object NoteImportExportUtil {

  private const val TAG = "NoteImportExportUtil"

  /**
   * Export single note as a formatted .txt text file and share or save.
   */
  fun exportNoteAsText(context: Context, note: NoteItem): Uri? {
    return try {
      val sb = StringBuilder()
      if (note.title.isNotBlank()) {
        sb.append(note.title).append("\n")
        sb.append("=".repeat(note.title.length.coerceAtLeast(10))).append("\n\n")
      }
      if (note.content.isNotBlank()) {
        sb.append(note.content).append("\n\n")
      }
      if (note.checklist.isNotEmpty()) {
        sb.append("Checklist:\n")
        note.checklist.forEach { item ->
          val check = if (item.isChecked) "[x]" else "[ ]"
          sb.append("$check ${item.text}\n")
        }
        sb.append("\n")
      }
      if (note.reminder != null) {
        sb.append("Reminder: ${note.reminder}\n")
      }
      if (note.labels.isNotEmpty()) {
        sb.append("Labels: ${note.labels.joinToString(", ")}\n")
      }
      val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(note.updatedAt))
      sb.append("Updated: $dateFormatted\n")

      val cleanTitle = note.title.filter { it.isLetterOrDigit() }.take(15).ifBlank { "note" }
      val file = File(context.cacheDir, "Note_${cleanTitle}_${System.currentTimeMillis()}.txt")
      file.writeText(sb.toString())

      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
      Log.e(TAG, "Error exporting note as text: ${e.message}", e)
      null
    }
  }

  /**
   * Export multiple or all notes as a combined JSON or Text backup file.
   */
  fun exportAllNotesAsJson(context: Context, notes: List<NoteItem>): Uri? {
    return try {
      val jsonArray = JSONArray()
      notes.forEach { note ->
        val obj = JSONObject()
        obj.put("id", note.id)
        obj.put("title", note.title)
        obj.put("content", note.content)
        obj.put("color", note.color.name)
        obj.put("isPinned", note.isPinned)
        obj.put("isArchived", note.isArchived)
        obj.put("labels", JSONArray(note.labels))
        val checklistArr = JSONArray()
        note.checklist.forEach { item ->
          val itemObj = JSONObject()
          itemObj.put("text", item.text)
          itemObj.put("isChecked", item.isChecked)
          checklistArr.put(itemObj)
        }
        obj.put("checklist", checklistArr)
        obj.put("reminder", note.reminder ?: "")
        obj.put("calendarDate", note.calendarDate ?: -1L)
        obj.put("updatedAt", note.updatedAt)
        jsonArray.put(obj)
      }

      val file = File(context.cacheDir, "Cutestar_Notes_Backup_${System.currentTimeMillis()}.json")
      file.writeText(jsonArray.toString(2))
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
      Log.e(TAG, "Error exporting all notes: ${e.message}", e)
      null
    }
  }

  /**
   * Render note into a high-resolution PNG image card and return Uri.
   */
  fun exportNoteAsImage(context: Context, note: NoteItem): Uri? {
    return try {
      val width = 800
      val padding = 40f
      val lineHeight = 38f

      // Calculate required height
      var estimatedLines = 4 // Base padding & title
      if (note.title.isNotBlank()) estimatedLines += (note.title.length / 28) + 1
      if (note.content.isNotBlank()) estimatedLines += (note.content.length / 32) + 2
      estimatedLines += note.checklist.size
      if (note.reminder != null) estimatedLines += 1
      if (note.labels.isNotEmpty()) estimatedLines += 1

      val height = (estimatedLines * lineHeight + padding * 2).toInt().coerceIn(600, 2400)
      val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)

      // Background
      val bgPaint = Paint().apply {
        isAntiAlias = true
        color = note.color.lightColor.hashCode()
      }
      canvas.drawColor(android.graphics.Color.WHITE)
      val cardRect = RectF(16f, 16f, (width - 16).toFloat(), (height - 16).toFloat())
      canvas.drawRoundRect(cardRect, 24f, 24f, bgPaint)

      val borderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = note.color.lightBorder.hashCode()
      }
      canvas.drawRoundRect(cardRect, 24f, 24f, borderPaint)

      var currentY = padding + 40f

      // Title
      val titlePaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.BLACK
        textSize = 34f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
      if (note.title.isNotBlank()) {
        canvas.drawText(note.title.take(50), padding, currentY, titlePaint)
        currentY += lineHeight * 1.4f
      }

      // Content
      val bodyPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.DKGRAY
        textSize = 24f
      }
      if (note.content.isNotBlank()) {
        val lines = note.content.split("\n")
        lines.forEach { line ->
          canvas.drawText(line.take(60), padding, currentY, bodyPaint)
          currentY += lineHeight
        }
        currentY += 16f
      }

      // Checklist
      if (note.checklist.isNotEmpty()) {
        val checkPaint = Paint().apply {
          isAntiAlias = true
          color = android.graphics.Color.BLACK
          textSize = 22f
        }
        note.checklist.forEach { item ->
          val icon = if (item.isChecked) "☑" else "☐"
          canvas.drawText("$icon  ${item.text.take(55)}", padding, currentY, checkPaint)
          currentY += lineHeight
        }
        currentY += 16f
      }

      // Metadata footer (Date & App Name)
      val footerPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.GRAY
        textSize = 18f
      }
      val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.updatedAt))
      canvas.drawText("★ Cutestar • $dateStr", padding, (height - 30).toFloat(), footerPaint)

      val cleanTitle = note.title.filter { it.isLetterOrDigit() }.take(15).ifBlank { "note" }
      val file = File(context.cacheDir, "Card_${cleanTitle}_${System.currentTimeMillis()}.png")
      FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        out.flush()
      }

      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
      Log.e(TAG, "Error exporting note as image: ${e.message}", e)
      null
    }
  }

  /**
   * Import notes from text (.txt) or JSON (.json) URI.
   */
  fun importNotesFromTextUri(context: Context, uri: Uri): List<NoteItem> {
    val results = mutableListOf<NoteItem>()
    try {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BufferedReader(InputStreamReader(stream)).use { reader ->
          val content = reader.readText().trim()
          if (content.startsWith("[") && content.endsWith("]")) {
            // JSON format
            val jsonArray = JSONArray(content)
            for (i in 0 until jsonArray.length()) {
              val obj = jsonArray.getJSONObject(i)
              val title = obj.optString("title", "Imported Note")
              val body = obj.optString("content", "")
              val colorName = obj.optString("color", NoteColor.DEFAULT.name)
              val color = try { NoteColor.valueOf(colorName) } catch (_: Exception) { NoteColor.DEFAULT }
              val calDate = if (obj.has("calendarDate") && obj.getLong("calendarDate") > 0) obj.getLong("calendarDate") else null

              val checklist = mutableListOf<ChecklistItem>()
              val checkArr = obj.optJSONArray("checklist")
              if (checkArr != null) {
                for (j in 0 until checkArr.length()) {
                  val itemObj = checkArr.getJSONObject(j)
                  checklist.add(
                    ChecklistItem(
                      id = UUID.randomUUID().toString(),
                      text = itemObj.optString("text", ""),
                      isChecked = itemObj.optBoolean("isChecked", false),
                    )
                  )
                }
              }

              results.add(
                NoteItem(
                  id = UUID.randomUUID().toString(),
                  title = title,
                  content = body,
                  color = color,
                  checklist = checklist,
                  calendarDate = calDate,
                  updatedAt = System.currentTimeMillis(),
                )
              )
            }
          } else {
            // Plain text format
            val lines = content.lines()
            val title = lines.firstOrNull()?.trim() ?: "Imported Note"
            val body = if (lines.size > 1) lines.drop(1).joinToString("\n").trim() else ""
            results.add(
              NoteItem(
                id = UUID.randomUUID().toString(),
                title = title,
                content = body,
                updatedAt = System.currentTimeMillis(),
              )
            )
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error importing notes from text: ${e.message}", e)
    }
    return results
  }

  /**
   * Import note from an image URI (stores image locally and creates NoteItem).
   */
  fun importNoteFromImageUri(context: Context, imageUri: Uri): NoteItem? {
    return try {
      val imagesDir = File(context.filesDir, "images")
      if (!imagesDir.exists()) imagesDir.mkdirs()

      val newFile = File(imagesDir, "imported_${System.currentTimeMillis()}.jpg")
      context.contentResolver.openInputStream(imageUri)?.use { input ->
        FileOutputStream(newFile).use { output ->
          input.copyTo(output)
        }
      }

      NoteItem(
        id = UUID.randomUUID().toString(),
        title = "Imported Image",
        content = "",
        imageUrl = Uri.fromFile(newFile).toString(),
        updatedAt = System.currentTimeMillis(),
      )
    } catch (e: Exception) {
      Log.e(TAG, "Error importing note from image: ${e.message}", e)
      null
    }
  }

  fun shareExportUri(context: Context, uri: Uri, mimeType: String, subject: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = mimeType
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, subject)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export / Share via"))
  }
}
