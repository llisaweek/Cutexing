package com.example.ui

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.model.LayoutMode
import com.example.model.NavigationDestination
import com.example.model.NoteColor
import com.example.model.NoteItem
import com.example.ui.components.BottomQuickBar
import com.example.ui.components.EditLabelsDialog
import com.example.ui.components.NavigationDrawerContent
import com.example.ui.components.NoteCard
import com.example.ui.components.NoteEditorDialog
import com.example.ui.components.ProfileDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TopSearchBar
import com.example.ui.components.VoiceNoteDialog
import com.example.ui.theme.CutestarPrimary
import kotlinx.coroutines.launch

@Composable
fun CutestarScreen(
  viewModel: CutestarViewModel = viewModel(),
  onSignOut: () -> Unit = {},
  onSignInRequest: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val drawerState = rememberDrawerState(DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val credentialManager = remember { CredentialManager.create(context) }

  val notes by viewModel.filteredNotes.collectAsStateWithLifecycle()
  val labels by viewModel.labels.collectAsStateWithLifecycle()
  val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
  val selectedColorFilter by viewModel.selectedColorFilter.collectAsStateWithLifecycle()
  val layoutMode by viewModel.layoutMode.collectAsStateWithLifecycle()
  val selectedNoteIds by viewModel.selectedNoteIds.collectAsStateWithLifecycle()
  val moveCheckedToBottom by viewModel.moveCheckedToBottom.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val isUploadingAvatar by viewModel.isUploadingAvatar.collectAsStateWithLifecycle()
  val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
  val isSavingNote by viewModel.isSavingNote.collectAsStateWithLifecycle()

  // Dialog & Sheet States
  var showCreateLabelDialog by remember { mutableStateOf(false) }
  var showEditLabelsDialog by remember { mutableStateOf(false) }
  var showProfileDialog by remember { mutableStateOf(false) }
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showVoiceNoteDialog by remember { mutableStateOf(false) }
  var showNoteEditor by remember { mutableStateOf(false) }
  var showEmptyTrashConfirm by remember { mutableStateOf(false) }
  var trashActionTargetNote by remember { mutableStateOf<NoteItem?>(null) }

  var editingNote by remember { mutableStateOf<NoteItem?>(null) }
  var startWithDrawing by remember { mutableStateOf(false) }
  var startWithChecklist by remember { mutableStateOf(false) }

  // Zero-permission Photo Picker for bottom bar
  val bottomBarPhotoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      editingNote = null
      startWithDrawing = false
      startWithChecklist = false
      showNoteEditor = true
    }
  }

  // Handle Android back button
  val isSelectionActive = selectedNoteIds.isNotEmpty()
  BackHandler(enabled = isSelectionActive || drawerState.isOpen || currentDestination !is NavigationDestination.Notes) {
    if (isSelectionActive) {
      viewModel.clearSelection()
    } else if (drawerState.isOpen) {
      scope.launch { drawerState.close() }
    } else {
      viewModel.setDestination(NavigationDestination.Notes)
    }
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      NavigationDrawerContent(
        currentDestination = currentDestination,
        labels = labels,
        onSelectDestination = { dest ->
          viewModel.setDestination(dest)
          scope.launch { drawerState.close() }
        },
        onCreateLabel = {
          showCreateLabelDialog = true
          scope.launch { drawerState.close() }
        },
        onEditLabels = {
          showEditLabelsDialog = true
          scope.launch { drawerState.close() }
        },
        onSettingsClick = {
          showSettingsDialog = true
          scope.launch { drawerState.close() }
        },
        userProfile = userProfile,
        onProfileClick = {
          showProfileDialog = true
          scope.launch { drawerState.close() }
        },
      )
    },
    modifier = modifier.fillMaxSize(),
  ) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = MaterialTheme.colorScheme.background,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        // Hide bottom quick bar in Trash or when multi-selecting
        if (currentDestination !is NavigationDestination.Trash && selectedNoteIds.isEmpty()) {
          BottomQuickBar(
            onTakeNote = {
              editingNote = null
              startWithDrawing = false
              startWithChecklist = false
              showNoteEditor = true
            },
            onNewList = {
              editingNote = null
              startWithDrawing = false
              startWithChecklist = true
              showNoteEditor = true
            },
            onNewDrawing = {
              editingNote = null
              startWithDrawing = true
              startWithChecklist = false
              showNoteEditor = true
            },
            onNewAudio = {
              showVoiceNoteDialog = true
            },
            onNewPhoto = {
              bottomBarPhotoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
          )
        }
      },
    ) { innerPadding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(bottom = innerPadding.calculateBottomPadding()),
      ) {
        // Top Search Bar / Contextual Action Bar
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        ) {
          TopSearchBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { viewModel.setSearchQuery(it) },
            layoutMode = layoutMode,
            onToggleLayoutMode = { viewModel.toggleLayoutMode() },
            onOpenDrawer = { scope.launch { drawerState.open() } },
            selectedCount = selectedNoteIds.size,
            onClearSelection = { viewModel.clearSelection() },
            onPinSelected = { viewModel.pinSelectedNotes() },
            onArchiveSelected = {
              viewModel.archiveSelectedNotes()
              scope.launch { snackbarHostState.showSnackbar("Updated archive status") }
            },
            onTrashSelected = {
              viewModel.trashSelectedNotes()
              scope.launch { snackbarHostState.showSnackbar("Moved to trash") }
            },
            onSelectAll = { viewModel.selectAllVisibleNotes() },
            onSetColorSelected = { color -> viewModel.setColorForSelectedNotes(color) },
            userProfile = userProfile,
            onAvatarClick = { showProfileDialog = true },
          )
        }

        // Search Filter Chips (Google Keep signature quick filters)
        AnimatedVisibility(visible = selectedNoteIds.isEmpty()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            NoteTypeFilter.entries.forEach { filter ->
              val isSelected = selectedTypeFilter == filter
              FilterChip(
                selected = isSelected,
                onClick = { viewModel.setTypeFilter(filter) },
                label = { Text(filter.displayName, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = CutestarPrimary.copy(alpha = 0.15f),
                  selectedLabelColor = CutestarPrimary,
                ),
                shape = CircleShape,
                modifier = Modifier.height(32.dp),
              )
            }

            // Quick Pastel Color Filter Dots
            NoteColor.entries.filter { it != NoteColor.DEFAULT }.forEach { color ->
              val isSelected = selectedColorFilter == color
              Box(
                modifier = Modifier
                  .size(24.dp)
                  .clip(CircleShape)
                  .background(color.lightColor)
                  .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) CutestarPrimary else color.lightBorder,
                    shape = CircleShape,
                  )
                  .clickable {
                    viewModel.setColorFilter(if (isSelected) null else color)
                  },
              )
            }
          }
        }

        // Active Destination Title Header
        if (currentDestination !is NavigationDestination.Notes) {
          DestinationHeader(destination = currentDestination)
        }

        // Google Keep Trash Notice Banner
        if (currentDestination is NavigationDestination.Trash) {
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "Notes in Trash are deleted after 7 days.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
              )
              if (notes.isNotEmpty()) {
                TextButton(
                  onClick = { showEmptyTrashConfirm = true },
                  modifier = Modifier.testTag("empty_trash_button"),
                ) {
                  Text("Empty Trash", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }

        // Main Notes Staggered Grid
        val isTrashOrArchive = currentDestination is NavigationDestination.Trash || currentDestination is NavigationDestination.Archive
        val pinnedNotes = if (!isTrashOrArchive) notes.filter { it.isPinned } else emptyList()
        val otherNotes = if (!isTrashOrArchive) notes.filter { !it.isPinned } else notes

        if (notes.isEmpty()) {
          EmptyNotesState(
            destination = currentDestination,
            hasSearchQuery = searchQuery.isNotEmpty(),
          )
        } else {
          val columns = if (layoutMode == LayoutMode.STAGGERED_GRID) 2 else 1

          LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columns),
            modifier = Modifier
              .fillMaxSize()
              .animateContentSize()
              .testTag("notes_staggered_grid"),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 28.dp),
            verticalItemSpacing = 10.dp,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            // PINNED Section
            if (pinnedNotes.isNotEmpty()) {
              item(span = StaggeredGridItemSpan.FullLine) {
                SectionHeader(title = stringResource(R.string.section_pinned))
              }
              items(pinnedNotes, key = { it.id }) { note ->
                NoteCard(
                  note = note,
                  isSelected = selectedNoteIds.contains(note.id),
                  isSelectionModeActive = selectedNoteIds.isNotEmpty(),
                  onClick = {
                    if (selectedNoteIds.isNotEmpty()) {
                      viewModel.toggleNoteSelection(note.id)
                    } else {
                      editingNote = note
                      startWithDrawing = false
                      startWithChecklist = false
                      showNoteEditor = true
                    }
                  },
                  onLongClick = { viewModel.toggleNoteSelection(note.id) },
                  onTogglePin = { viewModel.togglePin(note.id) },
                  onToggleChecklistItem = { noteId, itemId ->
                    viewModel.toggleChecklistItem(noteId, itemId)
                  },
                )
              }
            }

            // OTHERS Section
            if (otherNotes.isNotEmpty()) {
              if (pinnedNotes.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                  SectionHeader(title = stringResource(R.string.section_others))
                }
              }
              items(otherNotes, key = { it.id }) { note ->
                NoteCard(
                  note = note,
                  isSelected = selectedNoteIds.contains(note.id),
                  isSelectionModeActive = selectedNoteIds.isNotEmpty(),
                  onClick = {
                    if (selectedNoteIds.isNotEmpty()) {
                      viewModel.toggleNoteSelection(note.id)
                    } else if (currentDestination is NavigationDestination.Trash) {
                      trashActionTargetNote = note
                    } else {
                      editingNote = note
                      startWithDrawing = false
                      startWithChecklist = false
                      showNoteEditor = true
                    }
                  },
                  onLongClick = { viewModel.toggleNoteSelection(note.id) },
                  onTogglePin = { viewModel.togglePin(note.id) },
                  onToggleChecklistItem = { noteId, itemId ->
                    viewModel.toggleChecklistItem(noteId, itemId)
                  },
                )
              }
            }
          }
        }
      }
    }
  }

  // Profile Dialog with Avatar Selection and Upload
  if (showProfileDialog) {
    ProfileDialog(
      userProfile = userProfile,
      isUploadingAvatar = isUploadingAvatar,
      uploadProgress = uploadProgress,
      onPickNewAvatar = { uri ->
        viewModel.uploadAvatar(uri) { success, message ->
          scope.launch {
            snackbarHostState.showSnackbar(message ?: if (success) "Avatar updated" else "Upload failed")
          }
        }
      },
      onSignOut = {
        viewModel.signOut(context, credentialManager) {
          showProfileDialog = false
          onSignOut()
        }
      },
      onSignInWithGoogle = {
        showProfileDialog = false
        onSignInRequest()
      },
      onDismiss = { showProfileDialog = false },
    )
  }

  // Note Editor Dialog with full Google Keep capabilities
  if (showNoteEditor) {
    NoteEditorDialog(
      initialNote = editingNote,
      startWithDrawing = startWithDrawing,
      startWithChecklist = startWithChecklist,
      allLabels = labels,
      isSaving = isSavingNote,
      onCreateLabel = { viewModel.createLabel(it) },
      onSave = { noteId, title, content, color, isPinned, isArchived, strokes, existingDrawingUrl, photoUri, existingImageUrl, checklist, reminder, assignedLabels ->
        viewModel.saveFullNote(
          noteId = noteId,
          title = title,
          content = content,
          color = color,
          isPinned = isPinned,
          isArchived = isArchived,
          strokes = strokes,
          existingDrawingUrl = existingDrawingUrl,
          photoUri = photoUri,
          existingImageUrl = existingImageUrl,
          checklist = checklist,
          reminder = reminder,
          labels = assignedLabels,
          onComplete = { success, message ->
            if (success) {
              showNoteEditor = false
              editingNote = null
            }
            scope.launch {
              snackbarHostState.showSnackbar(
                message ?: if (success) "Saved to Cutestar ★" else "Failed to save note"
              )
            }
          },
        )
      },
      onDeleteNote = { noteId ->
        viewModel.trashNote(noteId)
        showNoteEditor = false
        editingNote = null
        scope.launch { snackbarHostState.showSnackbar("Note moved to trash") }
      },
      onDuplicateNote = { noteId ->
        viewModel.duplicateNote(noteId)
        showNoteEditor = false
        editingNote = null
        scope.launch { snackbarHostState.showSnackbar("Note duplicated ★") }
      },
      onDismiss = {
        showNoteEditor = false
        editingNote = null
      },
    )
  }

  // Voice Note Dialog
  if (showVoiceNoteDialog) {
    VoiceNoteDialog(
      onSaveVoiceNote = { title, transcript ->
        viewModel.addQuickNote(
          title = title,
          content = transcript,
          color = NoteColor.DUSK,
        )
        showVoiceNoteDialog = false
        scope.launch { snackbarHostState.showSnackbar("Voice note saved ★") }
      },
      onDismiss = { showVoiceNoteDialog = false },
    )
  }

  // Edit Labels Dialog (from drawer)
  if (showEditLabelsDialog) {
    EditLabelsDialog(
      labels = labels,
      onCreateLabel = { viewModel.createLabel(it) },
      onRenameLabel = { oldName, newName -> viewModel.renameLabel(oldName, newName) },
      onDeleteLabel = { viewModel.deleteLabel(it) },
      onDismiss = { showEditLabelsDialog = false },
    )
  }

  // Create Label Dialog (single)
  if (showCreateLabelDialog) {
    CreateLabelDialog(
      onDismiss = { showCreateLabelDialog = false },
      onConfirm = { labelName ->
        viewModel.createLabel(labelName)
        showCreateLabelDialog = false
        scope.launch { snackbarHostState.showSnackbar("Created label: $labelName") }
      },
    )
  }

  // Settings Dialog
  if (showSettingsDialog) {
    SettingsDialog(
      moveCheckedToBottom = moveCheckedToBottom,
      onToggleMoveCheckedToBottom = { viewModel.toggleMoveCheckedToBottom(it) },
      userProfile = userProfile,
      onDismiss = { showSettingsDialog = false },
    )
  }

  // Trash Item Options Dialog (Restore or Delete permanently)
  if (trashActionTargetNote != null) {
    val target = trashActionTargetNote!!
    AlertDialog(
      onDismissRequest = { trashActionTargetNote = null },
      title = { Text(target.title.ifBlank { "Note in Trash" }) },
      text = { Text("Can’t edit in Trash. Restore this note to make changes or delete forever.") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.restoreNote(target.id)
            trashActionTargetNote = null
            scope.launch { snackbarHostState.showSnackbar("Note restored ★") }
          },
          colors = ButtonDefaults.buttonColors(containerColor = CutestarPrimary),
        ) {
          Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Restore")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            viewModel.deleteNotePermanently(target.id)
            trashActionTargetNote = null
            scope.launch { snackbarHostState.showSnackbar("Note deleted permanently") }
          },
        ) {
          Icon(Icons.Outlined.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
          Spacer(modifier = Modifier.width(4.dp))
          Text("Delete forever", color = MaterialTheme.colorScheme.error)
        }
      },
    )
  }

  // Empty Trash Confirmation Dialog
  if (showEmptyTrashConfirm) {
    AlertDialog(
      onDismissRequest = { showEmptyTrashConfirm = false },
      title = { Text("Empty Trash?") },
      text = { Text("All notes in Trash will be permanently deleted.") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.emptyTrash()
            showEmptyTrashConfirm = false
            scope.launch { snackbarHostState.showSnackbar("Trash emptied") }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("Empty Trash")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEmptyTrashConfirm = false }) {
          Text("Cancel")
        }
      },
    )
  }
}

@Composable
private fun DestinationHeader(destination: NavigationDestination) {
  val title = when (destination) {
    is NavigationDestination.Notes -> stringResource(R.string.nav_notes)
    is NavigationDestination.Reminders -> stringResource(R.string.nav_reminders)
    is NavigationDestination.Label -> destination.name
    is NavigationDestination.Archive -> stringResource(R.string.nav_archive)
    is NavigationDestination.Trash -> stringResource(R.string.nav_trash)
    is NavigationDestination.Settings -> stringResource(R.string.nav_settings)
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun SectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.labelSmall.copy(
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.2.sp,
    ),
    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
    modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 4.dp),
  )
}

@Composable
private fun EmptyNotesState(
  destination: NavigationDestination,
  hasSearchQuery: Boolean,
) {
  val title = if (hasSearchQuery) {
    "No matching notes found"
  } else {
    when (destination) {
      is NavigationDestination.Notes -> stringResource(R.string.empty_notes_title)
      is NavigationDestination.Reminders -> "Notes with upcoming reminders appear here"
      is NavigationDestination.Label -> "No notes with this label yet"
      is NavigationDestination.Archive -> "Your archived notes appear here"
      is NavigationDestination.Trash -> "No notes in Trash"
      is NavigationDestination.Settings -> "Cutestar Preferences"
    }
  }

  val subtitle = if (hasSearchQuery) {
    "Try searching for another keyword or tag"
  } else {
    stringResource(R.string.empty_notes_subtitle)
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Image(
        painter = painterResource(R.drawable.ic_cutestar_logo),
        contentDescription = "Cutestar",
        modifier = Modifier
          .size(88.dp)
          .clip(RoundedCornerShape(22.dp)),
        contentScale = ContentScale.Crop,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun CreateLabelDialog(
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var labelName by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Create new label") },
    text = {
      OutlinedTextField(
        value = labelName,
        onValueChange = { labelName = it },
        label = { Text("Label name") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("create_label_input"),
      )
    },
    confirmButton = {
      Button(
        onClick = { if (labelName.isNotBlank()) onConfirm(labelName) },
        colors = ButtonDefaults.buttonColors(containerColor = CutestarPrimary),
        modifier = Modifier.testTag("confirm_create_label_button"),
      ) {
        Text("Create")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}
