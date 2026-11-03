package com.example.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.ChecklistItem
import com.example.model.NoteColor
import com.example.model.NoteItem
import com.example.ui.theme.CutestarPrimary
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorDialog(
  initialNote: NoteItem?,
  startWithDrawing: Boolean = false,
  startWithChecklist: Boolean = false,
  allLabels: List<String> = emptyList(),
  isSaving: Boolean = false,
  onCreateLabel: (String) -> Unit = {},
  onSave: (
    noteId: String,
    title: String,
    content: String,
    color: NoteColor,
    isPinned: Boolean,
    isArchived: Boolean,
    strokes: List<DrawingStroke>,
    existingDrawingUrl: String?,
    photoUri: Uri?,
    existingImageUrl: String?,
    checklist: List<ChecklistItem>,
    reminder: String?,
    labels: List<String>,
  ) -> Unit,
  onDeleteNote: (String) -> Unit = {},
  onDuplicateNote: (String) -> Unit = {},
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val context = LocalContext.current

  val noteId = remember { initialNote?.id ?: UUID.randomUUID().toString() }
  var title by remember { mutableStateOf(initialNote?.title ?: "") }
  var content by remember { mutableStateOf(initialNote?.content ?: "") }
  var selectedColor by remember { mutableStateOf(initialNote?.color ?: NoteColor.DEFAULT) }
  var isPinned by remember { mutableStateOf(initialNote?.isPinned ?: false) }
  var isArchived by remember { mutableStateOf(initialNote?.isArchived ?: false) }
  var reminder by remember { mutableStateOf(initialNote?.reminder) }

  // Media attachments
  var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
  var existingImageUrl by remember { mutableStateOf(initialNote?.imageUrl) }
  var existingDrawingUrl by remember { mutableStateOf(initialNote?.drawingUrl) }
  val drawingStrokes = remember { mutableStateListOf<DrawingStroke>() }
  var isDrawingExpanded by remember { mutableStateOf(startWithDrawing || !initialNote?.drawingUrl.isNullOrBlank()) }

  // Checklists
  val checklistItems = remember {
    mutableStateListOf<ChecklistItem>().apply {
      if (initialNote != null && initialNote.checklist.isNotEmpty()) {
        addAll(initialNote.checklist)
      } else if (startWithChecklist) {
        add(ChecklistItem(UUID.randomUUID().toString(), "", false))
      }
    }
  }
  var isChecklistMode by remember {
    mutableStateOf(startWithChecklist || (initialNote?.checklist?.isNotEmpty() == true))
  }
  var newListItemText by remember { mutableStateOf("") }
  var showCheckedItems by remember { mutableStateOf(true) }

  // Labels
  val assignedLabels = remember {
    mutableStateListOf<String>().apply {
      if (initialNote != null) addAll(initialNote.labels)
    }
  }

  // Dialog & Menu visibility
  var showMoreMenu by remember { mutableStateOf(false) }
  var showColorPickerRow by remember { mutableStateOf(false) }
  var showReminderDialog by remember { mutableStateOf(false) }
  var showLabelsDialog by remember { mutableStateOf(false) }

  // Zero-permission Android Photo Picker
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      selectedPhotoUri = uri
    }
  }

  val backgroundColor by animateColorAsState(
    targetValue = selectedColor.lightColor,
    label = "sheet_bg_color",
  )

  fun handleSaveAndClose() {
    val finalChecklist = if (isChecklistMode) {
      checklistItems.filter { it.text.isNotBlank() }
    } else {
      emptyList()
    }

    val hasContent = title.isNotBlank() ||
      content.isNotBlank() ||
      finalChecklist.isNotEmpty() ||
      drawingStrokes.isNotEmpty() ||
      !existingDrawingUrl.isNullOrBlank() ||
      selectedPhotoUri != null ||
      !existingImageUrl.isNullOrBlank()

    if (hasContent) {
      onSave(
        noteId,
        title,
        content,
        selectedColor,
        isPinned,
        isArchived,
        drawingStrokes.toList(),
        existingDrawingUrl,
        selectedPhotoUri,
        existingImageUrl,
        finalChecklist,
        reminder,
        assignedLabels.toList(),
      )
    } else {
      onDismiss()
    }
  }

  ModalBottomSheet(
    onDismissRequest = { handleSaveAndClose() },
    sheetState = sheetState,
    containerColor = backgroundColor,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    modifier = Modifier
      .testTag("note_editor_sheet")
      .imePadding(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.94f)
        .padding(horizontal = 16.dp),
    ) {
      // Top Keep-style Header Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = { handleSaveAndClose() },
          modifier = Modifier.size(44.dp).testTag("editor_back_btn"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back and save",
            tint = MaterialTheme.colorScheme.onSurface,
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Pin / Unpin
          IconButton(
            onClick = { isPinned = !isPinned },
            modifier = Modifier.size(40.dp).testTag("editor_pin_btn"),
          ) {
            Icon(
              imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
              contentDescription = if (isPinned) "Unpin" else "Pin",
              tint = if (isPinned) CutestarPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          // Reminder button
          IconButton(
            onClick = { showReminderDialog = true },
            modifier = Modifier.size(40.dp).testTag("editor_reminder_btn"),
          ) {
            Icon(
              imageVector = Icons.Outlined.AccessTime,
              contentDescription = "Set reminder",
              tint = if (reminder != null) CutestarPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          // Archive / Unarchive
          IconButton(
            onClick = { isArchived = !isArchived },
            modifier = Modifier.size(40.dp).testTag("editor_archive_btn"),
          ) {
            Icon(
              imageVector = if (isArchived) Icons.Filled.Archive else Icons.Outlined.Archive,
              contentDescription = if (isArchived) "Unarchive" else "Archive",
              tint = if (isArchived) CutestarPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          // More Options Menu
          Box {
            IconButton(
              onClick = { showMoreMenu = true },
              modifier = Modifier.size(40.dp).testTag("editor_more_btn"),
            ) {
              Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            DropdownMenu(
              expanded = showMoreMenu,
              onDismissRequest = { showMoreMenu = false },
            ) {
              DropdownMenuItem(
                text = { Text("Delete note") },
                leadingIcon = {
                  Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                onClick = {
                  showMoreMenu = false
                  onDeleteNote(noteId)
                  onDismiss()
                },
              )
              DropdownMenuItem(
                text = { Text("Make a copy") },
                onClick = {
                  showMoreMenu = false
                  onDuplicateNote(noteId)
                  onDismiss()
                },
              )
              DropdownMenuItem(
                text = { Text("Labels") },
                leadingIcon = { Icon(Icons.Outlined.Label, contentDescription = null) },
                onClick = {
                  showMoreMenu = false
                  showLabelsDialog = true
                },
              )
              DropdownMenuItem(
                text = { Text("Send / Share") },
                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                onClick = {
                  showMoreMenu = false
                  val shareText = buildString {
                    if (title.isNotBlank()) append(title).append("\n\n")
                    if (isChecklistMode) {
                      checklistItems.forEach {
                        append(if (it.isChecked) "[✓] " else "[ ] ").append(it.text).append("\n")
                      }
                    } else {
                      append(content)
                    }
                  }
                  val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                  }
                  context.startActivity(Intent.createChooser(shareIntent, "Share note via"))
                },
              )
            }
          }
        }
      }

      // Main Editor Scrollable Content
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
      ) {
        // Photo Image Banner (if attached)
        val currentPhotoModel = selectedPhotoUri ?: existingImageUrl
        if (currentPhotoModel != null) {
          Spacer(modifier = Modifier.height(6.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(200.dp)
              .clip(RoundedCornerShape(16.dp))
              .border(1.dp, selectedColor.lightBorder, RoundedCornerShape(16.dp)),
          ) {
            SubcomposeAsyncImage(
              model = ImageRequest.Builder(context)
                .data(currentPhotoModel)
                .crossfade(true)
                .build(),
              contentDescription = "Note photo attachment",
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop,
              loading = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  CircularProgressIndicator(modifier = Modifier.size(28.dp), color = CutestarPrimary)
                }
              },
            )
            // Remove Photo Button
            IconButton(
              onClick = {
                selectedPhotoUri = null
                existingImageUrl = null
              },
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(32.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove photo",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
        }

        // Canvas Drawing Banner (if active or previously drawn)
        AnimatedVisibility(visible = isDrawingExpanded) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 6.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Vector Sketch Canvas 🎨",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
              )
              IconButton(
                onClick = {
                  isDrawingExpanded = false
                  existingDrawingUrl = null
                  drawingStrokes.clear()
                },
                modifier = Modifier.size(32.dp),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Delete,
                  contentDescription = "Delete drawing",
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(18.dp),
                )
              }
            }

            if (!existingDrawingUrl.isNullOrBlank()) {
              SubcomposeAsyncImage(
                model = ImageRequest.Builder(context).data(existingDrawingUrl).crossfade(true).build(),
                contentDescription = "Existing sketch",
                modifier = Modifier
                  .fillMaxWidth()
                  .height(160.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .border(1.dp, selectedColor.lightBorder, RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
              )
              Spacer(modifier = Modifier.height(6.dp))
            }

            DrawingCanvas(
              strokes = drawingStrokes,
              onStrokesChange = { updated ->
                drawingStrokes.clear()
                drawingStrokes.addAll(updated)
              },
              canvasBackgroundColor = Color.White,
            )
            Spacer(modifier = Modifier.height(10.dp))
          }
        }

        // Active Reminder Chip in Editor
        if (reminder != null) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(1.dp, selectedColor.lightBorder),
            modifier = Modifier.padding(bottom = 8.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = CutestarPrimary,
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = reminder ?: "",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear reminder",
                modifier = Modifier
                  .size(14.dp)
                  .clickable { reminder = null },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        // Active Label Chips in Editor
        if (assignedLabels.isNotEmpty()) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 8.dp),
          ) {
            assignedLabels.forEach { label ->
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, selectedColor.lightBorder),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove label $label",
                    modifier = Modifier
                      .size(12.dp)
                      .clickable { assignedLabels.remove(label) },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }

        // Note Title
        BasicTextField(
          value = title,
          onValueChange = { title = it },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("note_title_input"),
          textStyle = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp,
            color = MaterialTheme.colorScheme.onSurface,
          ),
          cursorBrush = SolidColor(CutestarPrimary),
          decorationBox = { innerTextField ->
            if (title.isEmpty()) {
              Text(
                text = "Title",
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 21.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                ),
              )
            }
            innerTextField()
          },
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Note Content: Checklists vs Plain Text (Google Keep signature toggle)
        if (isChecklistMode) {
          val uncompletedItems = checklistItems.filter { !it.isChecked }
          val completedItems = checklistItems.filter { it.isChecked }

          // Uncompleted Checklist Items
          uncompletedItems.forEach { item ->
            val index = checklistItems.indexOf(item)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              IconButton(
                onClick = {
                  if (index >= 0) {
                    checklistItems[index] = item.copy(isChecked = true)
                  }
                },
                modifier = Modifier.size(32.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.CheckBoxOutlineBlank,
                  contentDescription = "Mark done",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(20.dp),
                )
              }
              Spacer(modifier = Modifier.width(4.dp))
              BasicTextField(
                value = item.text,
                onValueChange = { newText ->
                  if (index >= 0) checklistItems[index] = item.copy(text = newText)
                },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                  color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(CutestarPrimary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                  onNext = {
                    checklistItems.add(ChecklistItem(UUID.randomUUID().toString(), "", false))
                  },
                ),
              )
              IconButton(
                onClick = { checklistItems.remove(item) },
                modifier = Modifier.size(32.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Delete item",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }

          // "+ List item" Row to seamlessly add items
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(
              onClick = {
                val trimmed = newListItemText.trim()
                if (trimmed.isNotEmpty()) {
                  checklistItems.add(ChecklistItem(UUID.randomUUID().toString(), trimmed, false))
                  newListItemText = ""
                }
              },
              modifier = Modifier.size(32.dp),
            ) {
              Icon(
                imageVector = Icons.Default.CheckBoxOutlineBlank,
                contentDescription = "Add item",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp),
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
            BasicTextField(
              value = newListItemText,
              onValueChange = { newListItemText = it },
              modifier = Modifier.weight(1f).testTag("new_checklist_item_input"),
              textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
              ),
              cursorBrush = SolidColor(CutestarPrimary),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
              keyboardActions = KeyboardActions(
                onDone = {
                  val trimmed = newListItemText.trim()
                  if (trimmed.isNotEmpty()) {
                    checklistItems.add(ChecklistItem(UUID.randomUUID().toString(), trimmed, false))
                    newListItemText = ""
                  }
                },
              ),
              decorationBox = { innerTextField ->
                if (newListItemText.isEmpty()) {
                  Text(
                    text = "List item",
                    style = MaterialTheme.typography.bodyLarge.copy(
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    ),
                  )
                }
                innerTextField()
              },
            )
          }

          // Completed (Checked) items collapsible section
          if (completedItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { showCheckedItems = !showCheckedItems }
                .padding(vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = if (showCheckedItems) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "${completedItems.size} checked items",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            AnimatedVisibility(visible = showCheckedItems) {
              Column(modifier = Modifier.fillMaxWidth()) {
                completedItems.forEach { item ->
                  val index = checklistItems.indexOf(item)
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    IconButton(
                      onClick = {
                        if (index >= 0) {
                          checklistItems[index] = item.copy(isChecked = false)
                        }
                      },
                      modifier = Modifier.size(32.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Default.CheckBox,
                        contentDescription = "Uncheck",
                        tint = CutestarPrimary,
                        modifier = Modifier.size(20.dp),
                      )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = item.text,
                      style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                      ),
                      modifier = Modifier.weight(1f),
                    )
                    IconButton(
                      onClick = { checklistItems.remove(item) },
                      modifier = Modifier.size(32.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete completed item",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp),
                      )
                    }
                  }
                }
              }
            }
          }
        } else {
          // Plain Text Mode
          BasicTextField(
            value = content,
            onValueChange = { content = it },
            modifier = Modifier
              .fillMaxWidth()
              .height(240.dp)
              .testTag("note_content_input"),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
              lineHeight = 24.sp,
              color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(CutestarPrimary),
            decorationBox = { innerTextField ->
              if (content.isEmpty()) {
                Text(
                  text = "Note",
                  style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                  ),
                )
              }
              innerTextField()
            },
          )
        }

        Spacer(modifier = Modifier.height(24.dp))
      }

      // Pastel Palette Selector Row (if toggled)
      AnimatedVisibility(visible = showColorPickerRow) {
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
            NoteColor.entries.forEach { color ->
              val isSelected = selectedColor == color
              Box(
                modifier = Modifier
                  .size(30.dp)
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
          Spacer(modifier = Modifier.height(4.dp))
        }
      }

      HorizontalDivider(color = selectedColor.lightBorder.copy(alpha = 0.6f))

      // Bottom Keep-Style Action Toolbar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Color Palette Toggle
          IconButton(
            onClick = { showColorPickerRow = !showColorPickerRow },
            modifier = Modifier.size(40.dp).testTag("editor_color_palette_btn"),
          ) {
            Icon(
              imageVector = Icons.Outlined.ColorLens,
              contentDescription = "Color palette",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          // Checkboxes Toggle (Switch between checkboxes and text mode)
          IconButton(
            onClick = {
              if (!isChecklistMode) {
                // Convert plain text lines to checklist
                isChecklistMode = true
                if (checklistItems.isEmpty()) {
                  val lines = content.split("\n").filter { it.isNotBlank() }
                  if (lines.isNotEmpty()) {
                    lines.forEach { line ->
                      checklistItems.add(ChecklistItem(UUID.randomUUID().toString(), line.trim(), false))
                    }
                  } else {
                    checklistItems.add(ChecklistItem(UUID.randomUUID().toString(), "", false))
                  }
                }
              } else {
                // Convert checklist back to plain text
                isChecklistMode = false
                if (checklistItems.isNotEmpty()) {
                  content = checklistItems.joinToString("\n") { it.text }
                }
              }
            },
            modifier = Modifier.size(40.dp).testTag("editor_checklist_toggle_btn"),
          ) {
            Icon(
              imageVector = Icons.Outlined.CheckBox,
              contentDescription = if (isChecklistMode) "Hide checkboxes" else "Show checkboxes",
              tint = if (isChecklistMode) CutestarPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          // Add Photo (Android Photo Picker)
          IconButton(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier.size(40.dp).testTag("editor_add_photo_btn"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Image,
              contentDescription = "Add image",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          // Add Drawing Canvas
          IconButton(
            onClick = { isDrawingExpanded = !isDrawingExpanded },
            modifier = Modifier.size(40.dp).testTag("editor_drawing_btn"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Brush,
              contentDescription = "Drawing canvas",
              tint = if (isDrawingExpanded) CutestarPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          // Add Label
          IconButton(
            onClick = { showLabelsDialog = true },
            modifier = Modifier.size(40.dp).testTag("editor_label_btn"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Label,
              contentDescription = "Add labels",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // Syncing indicator or Done button
        if (isSaving) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CutestarPrimary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Syncing…", style = MaterialTheme.typography.labelSmall)
          }
        } else {
          Text(
            text = "Edited recently",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
          )
        }
      }
    }
  }

  // Reminder Picker Dialog
  if (showReminderDialog) {
    ReminderPickerDialog(
      currentReminder = reminder,
      onSetReminder = { newReminder ->
        reminder = newReminder
        showReminderDialog = false
      },
      onClearReminder = {
        reminder = null
        showReminderDialog = false
      },
      onDismiss = { showReminderDialog = false },
    )
  }

  // Labels Picker Dialog
  if (showLabelsDialog) {
    LabelsPickerDialog(
      allLabels = allLabels,
      assignedLabels = assignedLabels.toList(),
      onCreateNewLabel = { newLabel ->
        onCreateLabel(newLabel)
        if (!assignedLabels.contains(newLabel)) assignedLabels.add(newLabel)
      },
      onApplyLabels = { updated ->
        assignedLabels.clear()
        assignedLabels.addAll(updated)
      },
      onDismiss = { showLabelsDialog = false },
    )
  }
}
