package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.model.ChecklistItem
import com.example.model.NoteColor
import com.example.model.NoteItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class LocalNotesStorage(private val context: Context) {

  private val prefs = context.getSharedPreferences("cutestar_local_storage", Context.MODE_PRIVATE)

  fun loadNotes(): List<NoteItem> {
    val jsonString = prefs.getString(KEY_LOCAL_NOTES, null)
    if (jsonString.isNullOrBlank()) {
      return SampleData.initialNotes
    }
    return try {
      val jsonArray = JSONArray(jsonString)
      val result = mutableListOf<NoteItem>()
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        result.add(deserializeNote(obj))
      }
      if (result.isEmpty()) SampleData.initialNotes else result
    } catch (e: Exception) {
      Log.e("LocalNotesStorage", "Error loading local notes: ${e.message}", e)
      SampleData.initialNotes
    }
  }

  fun saveNotes(notes: List<NoteItem>) {
    try {
      val jsonArray = JSONArray()
      notes.forEach { note ->
        jsonArray.put(serializeNote(note))
      }
      prefs.edit().putString(KEY_LOCAL_NOTES, jsonArray.toString()).apply()
    } catch (e: Exception) {
      Log.e("LocalNotesStorage", "Error saving local notes: ${e.message}", e)
    }
  }

  fun loadLabels(): List<String> {
    val jsonString = prefs.getString(KEY_LOCAL_LABELS, null)
    if (jsonString.isNullOrBlank()) {
      return SampleData.defaultLabels
    }
    return try {
      val jsonArray = JSONArray(jsonString)
      val list = mutableListOf<String>()
      for (i in 0 until jsonArray.length()) {
        list.add(jsonArray.getString(i))
      }
      list.ifEmpty { SampleData.defaultLabels }
    } catch (e: Exception) {
      SampleData.defaultLabels
    }
  }

  fun saveLabels(labels: List<String>) {
    try {
      val jsonArray = JSONArray()
      labels.forEach { jsonArray.put(it) }
      prefs.edit().putString(KEY_LOCAL_LABELS, jsonArray.toString()).apply()
    } catch (e: Exception) {
      Log.e("LocalNotesStorage", "Error saving local labels: ${e.message}", e)
    }
  }

  fun saveDrawingBitmap(noteId: String, bitmap: Bitmap): String {
    val drawingsDir = File(context.filesDir, "drawings")
    if (!drawingsDir.exists()) {
      drawingsDir.mkdirs()
    }
    val drawingFile = File(drawingsDir, "drawing_${noteId}_${System.currentTimeMillis()}.png")
    FileOutputStream(drawingFile).use { out ->
      bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
      out.flush()
    }
    return Uri.fromFile(drawingFile).toString()
  }

  fun saveImageBitmap(noteId: String, bitmap: Bitmap): String {
    val imagesDir = File(context.filesDir, "images")
    if (!imagesDir.exists()) {
      imagesDir.mkdirs()
    }
    val imageFile = File(imagesDir, "image_${noteId}_${System.currentTimeMillis()}.jpg")
    FileOutputStream(imageFile).use { out ->
      bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
      out.flush()
    }
    return Uri.fromFile(imageFile).toString()
  }

  private fun serializeNote(note: NoteItem): JSONObject {
    val obj = JSONObject()
    obj.put("id", note.id)
    obj.put("title", note.title)
    obj.put("content", note.content)
    obj.put("color", note.color.name)
    obj.put("isPinned", note.isPinned)
    obj.put("isArchived", note.isArchived)
    obj.put("isDeleted", note.isDeleted)
    obj.put("reminder", note.reminder ?: "")
    obj.put("drawingUrl", note.drawingUrl ?: "")
    obj.put("drawingStoragePath", note.drawingStoragePath ?: "")
    obj.put("imageUrl", note.imageUrl ?: "")
    obj.put("imageStoragePath", note.imageStoragePath ?: "")
    obj.put("audioUrl", note.audioUrl ?: "")
    obj.put("updatedAt", note.updatedAt)
    if (note.calendarDate != null) obj.put("calendarDate", note.calendarDate)
    if (note.alarmTime != null) obj.put("alarmTime", note.alarmTime)

    val labelsArray = JSONArray()
    note.labels.forEach { labelsArray.put(it) }
    obj.put("labels", labelsArray)

    val checklistArray = JSONArray()
    note.checklist.forEach { item ->
      val itemObj = JSONObject()
      itemObj.put("id", item.id)
      itemObj.put("text", item.text)
      itemObj.put("isChecked", item.isChecked)
      checklistArray.put(itemObj)
    }
    obj.put("checklist", checklistArray)

    return obj
  }

  private fun deserializeNote(obj: JSONObject): NoteItem {
    val colorName = obj.optString("color", NoteColor.DEFAULT.name)
    val color = try {
      NoteColor.valueOf(colorName)
    } catch (_: Exception) {
      NoteColor.DEFAULT
    }

    val labelsList = mutableListOf<String>()
    val labelsArray = obj.optJSONArray("labels")
    if (labelsArray != null) {
      for (i in 0 until labelsArray.length()) {
        labelsList.add(labelsArray.getString(i))
      }
    }

    val checklistList = mutableListOf<ChecklistItem>()
    val checklistArray = obj.optJSONArray("checklist")
    if (checklistArray != null) {
      for (i in 0 until checklistArray.length()) {
        val itemObj = checklistArray.getJSONObject(i)
        checklistList.add(
          ChecklistItem(
            id = itemObj.optString("id", java.util.UUID.randomUUID().toString()),
            text = itemObj.optString("text", ""),
            isChecked = itemObj.optBoolean("isChecked", false),
          )
        )
      }
    }

    val reminder = obj.optString("reminder", "").ifBlank { null }
    val drawingUrl = obj.optString("drawingUrl", "").ifBlank { null }
    val drawingStoragePath = obj.optString("drawingStoragePath", "").ifBlank { null }
    val imageUrl = obj.optString("imageUrl", "").ifBlank { null }
    val imageStoragePath = obj.optString("imageStoragePath", "").ifBlank { null }
    val audioUrl = obj.optString("audioUrl", "").ifBlank { null }
    val calendarDate = if (obj.has("calendarDate")) obj.optLong("calendarDate") else null
    val alarmTime = if (obj.has("alarmTime")) obj.optLong("alarmTime") else null

    return NoteItem(
      id = obj.optString("id", java.util.UUID.randomUUID().toString()),
      title = obj.optString("title", ""),
      content = obj.optString("content", ""),
      color = color,
      isPinned = obj.optBoolean("isPinned", false),
      isArchived = obj.optBoolean("isArchived", false),
      isDeleted = obj.optBoolean("isDeleted", false),
      labels = labelsList,
      checklist = checklistList,
      reminder = reminder,
      drawingUrl = drawingUrl,
      drawingStoragePath = drawingStoragePath,
      imageUrl = imageUrl,
      imageStoragePath = imageStoragePath,
      audioUrl = audioUrl,
      updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
      calendarDate = calendarDate,
      alarmTime = alarmTime,
    )
  }

  fun getLanguage(): String {
    return prefs.getString(KEY_APP_LANGUAGE, "system") ?: "system"
  }

  fun saveLanguage(languageCode: String) {
    prefs.edit().putString(KEY_APP_LANGUAGE, languageCode).apply()
  }

  companion object {
    private const val KEY_LOCAL_NOTES = "key_cutestar_local_notes"
    private const val KEY_LOCAL_LABELS = "key_cutestar_local_labels"
    private const val KEY_APP_LANGUAGE = "key_cutestar_app_language"
  }
}
