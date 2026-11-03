package com.example.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.example.ui.components.NavigationDrawerContent
import com.example.ui.components.NoteCard
import com.example.ui.components.NoteEditorDialog
import com.example.ui.components.ProfileDialog
import com.example.ui.components.TopSearchBar
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
  val layoutMode by viewModel.layoutMode.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val isUploadingAvatar by viewModel.isUploadingAvatar.collectAsStateWithLifecycle()
  val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
  val isSavingNote by viewModel.isSavingNote.collectAsStateWithLifecycle()

  var showCreateLabelDialog by remember { mutableStateOf(false) }
  var showProfileDialog by remember { mutableStateOf(false) }
  var showNoteEditor by remember { mutableStateOf(false) }
  var editingNote by remember { mutableStateOf<NoteItem?>(null) }
  var startWithDrawing by remember { mutableStateOf(false) }

  // Handle Android back button
  BackHandler(enabled = drawerState.isOpen || currentDestination !is NavigationDestination.Notes) {
    if (drawerState.isOpen) {
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
        BottomQuickBar(
          onTakeNote = {
            editingNote = null
            startWithDrawing = false
            showNoteEditor = true
          },
          onNewList = {
            editingNote = null
            startWithDrawing = false
            showNoteEditor = true
          },
          onNewDrawing = {
            editingNote = null
            startWithDrawing = true
            showNoteEditor = true
          },
          onNewAudio = {
            scope.launch { snackbarHostState.showSnackbar("Voice notes coming soon ★") }
          },
          onNewPhoto = {
            scope.launch { snackbarHostState.showSnackbar("Photo attachments coming soon ★") }
          },
        )
      },
    ) { innerPadding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(bottom = innerPadding.calculateBottomPadding()),
      ) {
        // Safe Area Top + Search Bar with User Avatar
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
            userProfile = userProfile,
            onAvatarClick = { showProfileDialog = true },
          )
        }

        // Active destination title header (if not default Notes)
        if (currentDestination !is NavigationDestination.Notes) {
          DestinationHeader(destination = currentDestination)
        }

        // Main note content list
        val pinnedNotes = notes.filter { it.isPinned }
        val otherNotes = notes.filter { !it.isPinned }

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
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
            verticalItemSpacing = 10.dp,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            // PINNED section
            if (pinnedNotes.isNotEmpty()) {
              item(span = StaggeredGridItemSpan.FullLine) {
                SectionHeader(title = stringResource(R.string.section_pinned))
              }
              items(pinnedNotes, key = { it.id }) { note ->
                NoteCard(
                  note = note,
                  onClick = {
                    editingNote = note
                    startWithDrawing = false
                    showNoteEditor = true
                  },
                  onTogglePin = { viewModel.togglePin(note.id) },
                )
              }
            }

            // OTHERS section
            if (otherNotes.isNotEmpty()) {
              if (pinnedNotes.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                  SectionHeader(title = stringResource(R.string.section_others))
                }
              }
              items(otherNotes, key = { it.id }) { note ->
                NoteCard(
                  note = note,
                  onClick = {
                    editingNote = note
                    startWithDrawing = false
                    showNoteEditor = true
                  },
                  onTogglePin = { viewModel.togglePin(note.id) },
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

  // Note Editor Dialog with simultaneous Text & Canvas Drawing support
  if (showNoteEditor) {
    NoteEditorDialog(
      initialNote = editingNote,
      startWithDrawing = startWithDrawing,
      isSaving = isSavingNote,
      onSave = { noteId, title, content, color, strokes, existingDrawingUrl ->
        viewModel.saveNoteWithDrawing(
          noteId = noteId,
          title = title,
          content = content,
          color = color,
          strokes = strokes,
          existingDrawingUrl = existingDrawingUrl,
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
      onDismiss = {
        showNoteEditor = false
        editingNote = null
      },
    )
  }

  // Create Label Dialog
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
