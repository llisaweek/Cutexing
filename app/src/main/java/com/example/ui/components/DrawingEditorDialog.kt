package com.example.ui.components

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.model.NoteColor
import com.example.model.NoteItem
import com.example.ui.theme.CutestarPrimary
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Dedicated Full-Screen Image Drawing Editor.
 * Includes multi-tool vector drawing, paper styles, color controls, and image export/sharing.
 */
@Composable
fun DrawingEditorDialog(
  initialNote: NoteItem?,
  isSaving: Boolean = false,
  onSave: (
    noteId: String,
    title: String,
    strokes: List<DrawingStroke>,
    color: NoteColor,
    canvasBounds: CanvasBounds,
  ) -> Unit,
  onDelete: ((String) -> Unit)? = null,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val noteId = remember { initialNote?.id ?: UUID.randomUUID().toString() }
  var title by remember { mutableStateOf(initialNote?.title ?: "") }
  var selectedColor by remember { mutableStateOf(initialNote?.color ?: NoteColor.DEFAULT) }
  var showColorPicker by remember { mutableStateOf(false) }
  var paperStyle by remember { mutableStateOf(PaperStyle.BLANK) }

  val strokes = remember { mutableStateListOf<DrawingStroke>() }
  var currentBounds by remember { mutableStateOf(CanvasBounds(0f, 0f, 900f, 1200f)) }

  val sheetBgColor by animateColorAsState(
    targetValue = selectedColor.lightColor,
    label = "drawing_editor_bg",
  )

  fun handleSaveAndClose() {
    if (strokes.isNotEmpty() || title.isNotBlank() || !initialNote?.drawingUrl.isNullOrBlank()) {
      onSave(
        noteId,
        title,
        strokes.toList(),
        selectedColor,
        currentBounds,
      )
    } else {
      onDismiss()
    }
  }

  fun handleShareDrawing() {
    try {
      val bitmap = exportStrokesToBitmap(
        strokes = strokes.toList(),
        canvasLeft = currentBounds.left,
        canvasTop = currentBounds.top,
        canvasRight = currentBounds.right,
        canvasBottom = currentBounds.bottom,
      )
      val cacheDir = context.cacheDir
      val cacheFile = File(cacheDir, "sketch_${noteId.take(8)}_${System.currentTimeMillis()}.png")
      val fos = FileOutputStream(cacheFile)
      bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
      fos.flush()
      fos.close()

      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        cacheFile,
      )
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title.ifBlank { "Drawing from Cutestar" })
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(shareIntent, "Share drawing via"))
    } catch (e: Exception) {
      val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Drawing: ${title.ifBlank { "Cutestar sketch" }}")
      }
      context.startActivity(Intent.createChooser(fallbackIntent, "Share drawing"))
    }
  }

  Dialog(
    onDismissRequest = { handleSaveAndClose() },
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      decorFitsSystemWindows = false,
    ),
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .testTag("drawing_editor_screen"),
      color = sheetBgColor,
    ) {
      Column(
        modifier = Modifier.fillMaxSize(),
      ) {
        // Top Action Bar
        Surface(
          color = sheetBgColor,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .statusBarsPadding()
              .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f),
            ) {
              IconButton(
                onClick = { handleSaveAndClose() },
                modifier = Modifier
                  .size(44.dp)
                  .testTag("drawing_back_btn"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Save and go back",
                  tint = MaterialTheme.colorScheme.onSurface,
                )
              }

              Spacer(modifier = Modifier.width(6.dp))

              // Title input
              BasicTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier
                  .weight(1f)
                  .testTag("drawing_title_input"),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 18.sp,
                  color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(CutestarPrimary),
                decorationBox = { innerTextField ->
                  if (title.isEmpty()) {
                    Text(
                      text = "Drawing",
                      style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                      ),
                    )
                  }
                  innerTextField()
                },
              )
            }

            // Right header actions
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              // Share Drawing
              IconButton(
                onClick = { handleShareDrawing() },
                modifier = Modifier.size(40.dp).testTag("drawing_share_btn"),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Share,
                  contentDescription = "Share drawing",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              // Color palette toggle
              IconButton(
                onClick = { showColorPicker = !showColorPicker },
                modifier = Modifier.size(40.dp),
              ) {
                Icon(
                  imageVector = Icons.Outlined.ColorLens,
                  contentDescription = "Background color",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              // Delete note if existing
              if (initialNote != null && onDelete != null) {
                IconButton(
                  onClick = {
                    onDelete(initialNote.id)
                    onDismiss()
                  },
                  modifier = Modifier.size(40.dp),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete drawing note",
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }

              // Save / Done
              if (isSaving) {
                CircularProgressIndicator(
                  modifier = Modifier
                    .size(36.dp)
                    .padding(8.dp),
                  strokeWidth = 2.dp,
                  color = CutestarPrimary,
                )
              } else {
                IconButton(
                  onClick = { handleSaveAndClose() },
                  modifier = Modifier
                    .size(40.dp)
                    .testTag("drawing_save_btn"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Save drawing",
                    tint = CutestarPrimary,
                  )
                }
              }
            }
          }
        }

        // Color selector strip (if toggled)
        if (showColorPicker) {
          Surface(
            color = sheetBgColor,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              NoteColor.entries.forEach { color ->
                val isSelected = selectedColor == color
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.lightColor)
                    .border(
                      width = if (isSelected) 2.5.dp else 1.dp,
                      color = if (isSelected) CutestarPrimary else color.lightBorder,
                      shape = CircleShape,
                    )
                    .clickable { selectedColor = color },
                )
              }
            }
          }
        }

        // Main Drawing Canvas with Paper Styles & Expanded Tools
        DrawingCanvas(
          strokes = strokes,
          onStrokesChange = { updated ->
            strokes.clear()
            strokes.addAll(updated)
          },
          canvasBackgroundColor = Color.White,
          initialBounds = currentBounds,
          onBoundsChange = { newBounds ->
            currentBounds = newBounds
          },
          paperStyle = paperStyle,
          onPaperStyleChange = { paperStyle = it },
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}
