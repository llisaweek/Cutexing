package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FormatAlignLeft
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.NoteColor
import com.example.model.NoteItem
import com.example.ui.theme.CutestarPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorDialog(
  initialNote: NoteItem?,
  startWithDrawing: Boolean = false,
  isSaving: Boolean = false,
  onSave: (
    noteId: String,
    title: String,
    content: String,
    color: NoteColor,
    strokes: List<DrawingStroke>,
    existingDrawingUrl: String?,
  ) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val context = LocalContext.current

  val noteId = remember { initialNote?.id ?: java.util.UUID.randomUUID().toString() }
  var title by remember { mutableStateOf(initialNote?.title ?: "") }
  var content by remember { mutableStateOf(initialNote?.content ?: "") }
  var selectedColor by remember { mutableStateOf(initialNote?.color ?: NoteColor.DEFAULT) }
  var existingDrawingUrl by remember { mutableStateOf(initialNote?.drawingUrl) }
  val drawingStrokes = remember { mutableStateListOf<DrawingStroke>() }
  var isDrawingExpanded by remember { mutableStateOf(startWithDrawing || !initialNote?.drawingUrl.isNullOrBlank()) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = selectedColor.lightColor,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    modifier = Modifier.testTag("note_editor_sheet"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.92f)
        .padding(horizontal = 20.dp)
        .verticalScroll(rememberScrollState()),
    ) {
      // Top header actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(40.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "Close editor",
            tint = MaterialTheme.colorScheme.onSurface,
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Toggle Drawing mode
          IconButton(
            onClick = { isDrawingExpanded = !isDrawingExpanded },
            modifier = Modifier.size(40.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.Brush,
              contentDescription = "Toggle drawing canvas",
              tint = if (isDrawingExpanded) CutestarPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          // Save button
          Button(
            onClick = {
              if (title.isNotBlank() || content.isNotBlank() || drawingStrokes.isNotEmpty() || !existingDrawingUrl.isNullOrBlank()) {
                onSave(
                  noteId,
                  title,
                  content,
                  selectedColor,
                  drawingStrokes.toList(),
                  existingDrawingUrl,
                )
              } else {
                onDismiss()
              }
            },
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = CutestarPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("editor_save_button"),
          ) {
            if (isSaving) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White,
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Syncing…", fontSize = 13.sp)
            } else {
              Text("Save & Sync", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Note Title
      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        placeholder = { Text("Title", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("note_title_input"),
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          color = MaterialTheme.colorScheme.onSurface,
        ),
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Canvas Drawing Section (if expanded or has drawing)
      AnimatedVisibility(visible = isDrawingExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Canvas Drawing / Sketch 🎨",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface,
            )

            if (!existingDrawingUrl.isNullOrBlank()) {
              TextButton(
                onClick = { existingDrawingUrl = null },
              ) {
                Icon(
                  imageVector = Icons.Outlined.Delete,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp),
                  tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Remove previous sketch", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
              }
            }
          }

          // Historical drawing preview if existing
          if (!existingDrawingUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            SubcomposeAsyncImage(
              model = ImageRequest.Builder(context)
                .data(existingDrawingUrl)
                .crossfade(true)
                .build(),
              contentDescription = "Historical note sketch",
              modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, selectedColor.lightBorder, RoundedCornerShape(14.dp)),
              contentScale = ContentScale.Crop,
              loading = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  CircularProgressIndicator(modifier = Modifier.size(24.dp), color = CutestarPrimary)
                }
              },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Add new strokes below to update your drawing:",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Interactive Vector Drawing Canvas
          DrawingCanvas(
            strokes = drawingStrokes,
            onStrokesChange = { updated ->
              drawingStrokes.clear()
              drawingStrokes.addAll(updated)
            },
            canvasBackgroundColor = Color.White,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Note Text Content
      OutlinedTextField(
        value = content,
        onValueChange = { content = it },
        placeholder = { Text("Note content…", fontSize = 15.sp) },
        modifier = Modifier
          .fillMaxWidth()
          .height(160.dp)
          .testTag("note_content_input"),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
          fontSize = 15.sp,
          lineHeight = 22.sp,
          color = MaterialTheme.colorScheme.onSurface,
        ),
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Color Palette Selector
      Text(
        text = "Card Background Color",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        listOf(
          NoteColor.DEFAULT,
          NoteColor.SAND,
          NoteColor.PEACH,
          NoteColor.CORAL,
          NoteColor.MINT,
          NoteColor.SAGE,
          NoteColor.FOG,
          NoteColor.DUSK,
          NoteColor.BLOSSOM,
        ).forEach { color ->
          val isSelected = selectedColor == color
          Box(
            modifier = Modifier
              .size(32.dp)
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

      Spacer(modifier = Modifier.height(32.dp))
    }
  }
}
