package com.example

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.model.NoteColor
import com.example.model.NoteItem
import com.example.ui.components.DrawingStroke
import com.example.ui.components.exportStrokesToBitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Cutestar", appName)
  }

  @Test
  fun `note item default drawingUrl is null`() {
    val note = NoteItem(
      id = "test-1",
      title = "Doodle Note",
      content = "Text and drawings",
      color = NoteColor.PEACH,
    )
    assertNull(note.drawingUrl)
    assertNull(note.drawingStoragePath)
  }

  @Test
  fun `note item with drawing preserves drawingUrl`() {
    val sampleUrl = "https://firebasestorage.googleapis.com/v0/b/app/o/drawing.png"
    val note = NoteItem(
      id = "test-2",
      title = "Drawing Note",
      content = "Canvas art",
      color = NoteColor.MINT,
      drawingUrl = sampleUrl,
    )
    assertEquals(sampleUrl, note.drawingUrl)
  }

  @Test
  fun `exportStrokesToBitmap produces valid bitmap with strokes`() {
    val stroke = DrawingStroke(
      points = listOf(Offset(10f, 10f), Offset(50f, 50f), Offset(100f, 100f)),
      color = Color.Black,
      strokeWidth = 4f,
    )
    val bitmap = exportStrokesToBitmap(listOf(stroke), width = 200, height = 200)
    assertNotNull(bitmap)
    assertEquals(200, bitmap.width)
    assertEquals(200, bitmap.height)
  }

  @Test
  fun `local storage saves and loads notes without authentication`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = com.example.data.LocalNotesStorage(context)
    val testNotes = listOf(
      NoteItem(
        id = "local-1",
        title = "Offline idea",
        content = "Saved locally",
        color = NoteColor.PEACH,
        drawingUrl = "file:///data/local.png",
      )
    )
    storage.saveNotes(testNotes)
    val loaded = storage.loadNotes()
    assertEquals(1, loaded.size)
    assertEquals("Offline idea", loaded[0].title)
    assertEquals("Saved locally", loaded[0].content)
    assertEquals("file:///data/local.png", loaded[0].drawingUrl)
  }
}
