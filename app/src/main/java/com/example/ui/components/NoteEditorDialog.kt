package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Visibility
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.ChecklistItem
import com.example.model.NoteColor
import com.example.model.NoteItem
import com.example.ui.theme.CutestarPrimary
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorDialog(
  initialNote: NoteItem?,
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
  var contentState by remember { mutableStateOf(TextFieldValue(initialNote?.content ?: "")) }
  var selectedColor by remember { mutableStateOf(initialNote?.color ?: NoteColor.DEFAULT) }
  var isPinned by remember { mutableStateOf(initialNote?.isPinned ?: false) }
  var isArchived by remember { mutableStateOf(initialNote?.isArchived ?: false) }
  var reminder by remember { mutableStateOf(initialNote?.reminder) }

  // Media attachments (photos)
  var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
  var existingImageUrl by remember { mutableStateOf(initialNote?.imageUrl) }

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

  // Advanced practical text writing states
  var isReadingMode by remember { mutableStateOf(false) }
  var showFindBar by remember { mutableStateOf(false) }
  var findQuery by remember { mutableStateOf("") }
  var showFormattingBar by remember { mutableStateOf(true) }

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

  // Cursor-aware text insertion / formatting helper
  fun insertTextAtCursor(prefix: String, suffix: String = "") {
    val currentText = contentState.text
    val selection = contentState.selection
    val selectedSegment = if (selection.collapsed) "" else currentText.substring(selection.min, selection.max)
    val newText = currentText.substring(0, selection.min) + prefix + selectedSegment + suffix + currentText.substring(selection.max)
    val newCursorPos = if (selection.collapsed) {
      selection.min + prefix.length
    } else {
      selection.min + prefix.length + selectedSegment.length + suffix.length
    }
    contentState = TextFieldValue(
      text = newText,
      selection = TextRange(newCursorPos),
    )
  }

  // Live text stats
  val rawText = contentState.text
  val wordCount = remember(rawText) {
    if (rawText.isBlank()) 0 else rawText.trim().split("\\s+".toRegex()).size
  }
  val charCount = rawText.length
  val matchCount = remember(rawText, findQuery) {
    if (findQuery.isBlank()) 0 else rawText.split(findQuery, ignoreCase = true).size - 1
  }

  fun handleSaveAndClose() {
    val finalChecklist = if (isChecklistMode) {
      checklistItems.filter { it.text.isNotBlank() }
    } else {
      emptyList()
    }

    val contentStr = contentState.text
    val hasContent = title.isNotBlank() ||
      contentStr.isNotBlank() ||
      finalChecklist.isNotEmpty() ||
      selectedPhotoUri != null ||
      !existingImageUrl.isNullOrBlank()

    if (hasContent) {
      onSave(
        noteId,
        title,
        contentStr,
        selectedColor,
        isPinned,
        isArchived,
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
        .fillMaxHeight(0.95f)
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

        // Live stats pill
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
          border = androidx.compose.foundation.BorderStroke(0.5.dp, selectedColor.lightBorder),
        ) {
          Text(
            text = if (isChecklistMode) "${checklistItems.size} items" else "$wordCount words • $charCount chars",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Reading Mode toggle
          IconButton(
            onClick = { isReadingMode = !isReadingMode },
            modifier = Modifier.size(40.dp).testTag("editor_reading_mode_btn"),
          ) {
            Icon(
              imageVector = if (isReadingMode) Icons.Outlined.Edit else Icons.Outlined.Visibility,
              contentDescription = if (isReadingMode) "Edit mode" else "Reading mode",
              tint = if (isReadingMode) CutestarPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

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
                text = { Text("Find in note") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                onClick = {
                  showMoreMenu = false
                  showFindBar = !showFindBar
                },
              )
              DropdownMenuItem(
                text = { Text("Copy text") },
                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                onClick = {
                  showMoreMenu = false
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  val textToCopy = buildString {
                    if (title.isNotBlank()) append(title).append("\n\n")
                    if (isChecklistMode) {
                      checklistItems.forEach { append(if (it.isChecked) "[✓] " else "[ ] ").append(it.text).append("\n") }
                    } else {
                      append(contentState.text)
                    }
                  }
                  clipboard.setPrimaryClip(ClipData.newPlainText("Note Content", textToCopy))
                  Toast.makeText(context, "Note copied to clipboard", Toast.LENGTH_SHORT).show()
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
                      checklistItems.forEach { append(if (it.isChecked) "[✓] " else "[ ] ").append(it.text).append("\n") }
                    } else {
                      append(contentState.text)
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
              DropdownMenuItem(
                text = { Text("Labels") },
                leadingIcon = { Icon(Icons.Outlined.Label, contentDescription = null) },
                onClick = {
                  showMoreMenu = false
                  showLabelsDialog = true
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
            }
          }
        }
      }

      // In-note Find bar (if toggled)
      AnimatedVisibility(visible = showFindBar) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 3.dp,
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.Search,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
              tint = CutestarPrimary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
              value = findQuery,
              onValueChange = { findQuery = it },
              modifier = Modifier.weight(1f),
              textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
              cursorBrush = SolidColor(CutestarPrimary),
              decorationBox = { inner ->
                if (findQuery.isEmpty()) {
                  Text("Find in note…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
                inner()
              },
            )
            if (findQuery.isNotEmpty()) {
              Text(
                text = "$matchCount found",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = if (matchCount > 0) CutestarPrimary else MaterialTheme.colorScheme.error,
              )
              Spacer(modifier = Modifier.width(6.dp))
              IconButton(onClick = { findQuery = "" }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Clear find", modifier = Modifier.size(16.dp))
              }
            }
            IconButton(onClick = { showFindBar = false }, modifier = Modifier.size(24.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Close find bar", modifier = Modifier.size(16.dp))
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
        if (isReadingMode) {
          Text(
            text = title.ifBlank { "Untitled Note" },
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 22.sp,
              color = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          )
        } else {
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
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Note Content: Checklists vs Plain Text
        if (isChecklistMode) {
          val uncompletedItems = checklistItems.filter { !it.isChecked }
          val completedItems = checklistItems.filter { it.isChecked }

          // Checklist Action Strip (Check all, uncheck all, delete checked)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
              border = androidx.compose.foundation.BorderStroke(0.5.dp, selectedColor.lightBorder),
              modifier = Modifier.clickable {
                checklistItems.forEachIndexed { i, it -> checklistItems[i] = it.copy(isChecked = true) }
              },
            ) {
              Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.DoneAll, contentDescription = null, modifier = Modifier.size(13.dp), tint = CutestarPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Check all", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
              }
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
              border = androidx.compose.foundation.BorderStroke(0.5.dp, selectedColor.lightBorder),
              modifier = Modifier.clickable {
                checklistItems.forEachIndexed { i, it -> checklistItems[i] = it.copy(isChecked = false) }
              },
            ) {
              Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.RemoveDone, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Uncheck all", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
              }
            }

            if (completedItems.isNotEmpty()) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, selectedColor.lightBorder),
                modifier = Modifier.clickable {
                  checklistItems.removeAll { it.isChecked }
                },
              ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.error)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Clear done", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.error))
                }
              }
            }
          }

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
              if (isReadingMode) {
                Text(
                  text = item.text,
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.weight(1f),
                )
              } else {
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
              }

              if (!isReadingMode) {
                // Move item up / down buttons
                if (index > 0) {
                  IconButton(
                    onClick = { Collections.swap(checklistItems, index, index - 1) },
                    modifier = Modifier.size(26.dp),
                  ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up", modifier = Modifier.size(16.dp))
                  }
                }
                if (index < checklistItems.size - 1) {
                  IconButton(
                    onClick = { Collections.swap(checklistItems, index, index + 1) },
                    modifier = Modifier.size(26.dp),
                  ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down", modifier = Modifier.size(16.dp))
                  }
                }

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
          }

          // "+ List item" Row to seamlessly add items
          if (!isReadingMode) {
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
                    if (!isReadingMode) {
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
          }
        } else {
          // Plain Text Mode
          if (isReadingMode) {
            Text(
              text = if (contentState.text.isBlank()) "(Empty note)" else contentState.text,
              style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 26.sp,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface,
              ),
              modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
          } else {
            BasicTextField(
              value = contentState,
              onValueChange = { contentState = it },
              modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .testTag("note_content_input"),
              textStyle = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurface,
              ),
              cursorBrush = SolidColor(CutestarPrimary),
              decorationBox = { innerTextField ->
                if (contentState.text.isEmpty()) {
                  Text(
                    text = "Note… Write markdown, bold, bullets, or thoughts",
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
        }

        Spacer(modifier = Modifier.height(24.dp))
      }

      // Rich Formatting & Text Writing Helpers Strip (When in text mode and not reading mode)
      if (!isChecklistMode && !isReadingMode) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
          border = androidx.compose.foundation.BorderStroke(0.5.dp, selectedColor.lightBorder),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            // Bold
            IconButton(onClick = { insertTextAtCursor("**", "**") }, modifier = Modifier.size(34.dp)) {
              Icon(Icons.Outlined.FormatBold, contentDescription = "Bold", modifier = Modifier.size(18.dp))
            }
            // Italic
            IconButton(onClick = { insertTextAtCursor("*", "*") }, modifier = Modifier.size(34.dp)) {
              Icon(Icons.Outlined.FormatItalic, contentDescription = "Italic", modifier = Modifier.size(18.dp))
            }
            // Strikethrough
            IconButton(onClick = { insertTextAtCursor("~~", "~~") }, modifier = Modifier.size(34.dp)) {
              Icon(Icons.Outlined.FormatStrikethrough, contentDescription = "Strikethrough", modifier = Modifier.size(18.dp))
            }
            // Heading 1
            IconButton(onClick = { insertTextAtCursor("# ") }, modifier = Modifier.size(34.dp)) {
              Text("H1", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CutestarPrimary)
            }
            // Heading 2
            IconButton(onClick = { insertTextAtCursor("## ") }, modifier = Modifier.size(34.dp)) {
              Text("H2", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = CutestarPrimary)
            }
            // Bullet List
            IconButton(onClick = { insertTextAtCursor("• ") }, modifier = Modifier.size(34.dp)) {
              Icon(Icons.Outlined.FormatListBulleted, contentDescription = "Bullet", modifier = Modifier.size(18.dp))
            }
            // Number List
            IconButton(onClick = { insertTextAtCursor("1. ") }, modifier = Modifier.size(34.dp)) {
              Icon(Icons.Outlined.FormatListNumbered, contentDescription = "Numbered", modifier = Modifier.size(18.dp))
            }
            // Quote
            IconButton(onClick = { insertTextAtCursor("> ") }, modifier = Modifier.size(34.dp)) {
              Icon(Icons.Outlined.FormatQuote, contentDescription = "Quote", modifier = Modifier.size(18.dp))
            }
            // Inline Code
            IconButton(onClick = { insertTextAtCursor("`", "`") }, modifier = Modifier.size(34.dp)) {
              Text("<>", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            // Timestamp insert
            IconButton(
              onClick = {
                val timestamp = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())
                insertTextAtCursor("[$timestamp] ")
              },
              modifier = Modifier.size(34.dp),
            ) {
              Icon(Icons.Outlined.Schedule, contentDescription = "Insert Date & Time", modifier = Modifier.size(18.dp), tint = CutestarPrimary)
            }
          }
        }
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

          // Checkboxes Toggle
          IconButton(
            onClick = {
              if (!isChecklistMode) {
                isChecklistMode = true
                if (checklistItems.isEmpty()) {
                  val lines = contentState.text.split("\n").filter { it.isNotBlank() }
                  if (lines.isNotEmpty()) {
                    lines.forEach { line ->
                      checklistItems.add(ChecklistItem(UUID.randomUUID().toString(), line.trim(), false))
                    }
                  } else {
                    checklistItems.add(ChecklistItem(UUID.randomUUID().toString(), "", false))
                  }
                }
              } else {
                isChecklistMode = false
                if (checklistItems.isNotEmpty()) {
                  val combined = checklistItems.joinToString("\n") { it.text }
                  contentState = TextFieldValue(combined)
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

          // Add Photo
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

        // Syncing indicator or Status
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
