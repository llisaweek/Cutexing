package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AuthRepository
import com.example.data.SampleData
import com.example.model.ChecklistItem
import com.example.model.LayoutMode
import com.example.model.NavigationDestination
import com.example.model.NoteColor
import com.example.model.NoteItem
import com.example.model.UserProfile
import com.example.ui.components.DrawingStroke
import com.example.ui.components.exportStrokesToBitmap
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import com.example.data.LocalNotesStorage
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

class CutestarViewModel(application: Application) : AndroidViewModel(application) {

  val authRepository = AuthRepository(application)
  private val localStorage = LocalNotesStorage(application)
  private val auth: FirebaseAuth = Firebase.auth
  private val db: FirebaseFirestore by lazy {
    FirebaseFirestore.getInstance(application.getString(R.string.firestore_database_id))
  }
  private val storage: FirebaseStorage by lazy {
    FirebaseStorage.getInstance()
  }

  private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
  val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

  private val _userProfile = MutableStateFlow<UserProfile?>(
    if (auth.currentUser != null) null
    else UserProfile(
      userId = "guest",
      displayName = "Guest User",
      email = "Local Storage Mode",
      googlePhotoUrl = "",
      customAvatarUrl = "",
    )
  )
  val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

  private val _isUploadingAvatar = MutableStateFlow(false)
  val isUploadingAvatar: StateFlow<Boolean> = _isUploadingAvatar.asStateFlow()

  private val _uploadProgress = MutableStateFlow(0f)
  val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

  private val _isSavingNote = MutableStateFlow(false)
  val isSavingNote: StateFlow<Boolean> = _isSavingNote.asStateFlow()

  private val _notes = MutableStateFlow<List<NoteItem>>(localStorage.loadNotes())
  val notes: StateFlow<List<NoteItem>> = _notes

  private val _labels = MutableStateFlow<List<String>>(localStorage.loadLabels())
  val labels: StateFlow<List<String>> = _labels

  private val _currentDestination = MutableStateFlow<NavigationDestination>(NavigationDestination.Notes)
  val currentDestination: StateFlow<NavigationDestination> = _currentDestination

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery

  private val _layoutMode = MutableStateFlow(LayoutMode.STAGGERED_GRID)
  val layoutMode: StateFlow<LayoutMode> = _layoutMode

  private var profileJob: Job? = null
  private var notesJob: Job? = null

  init {
    auth.addAuthStateListener { firebaseAuth ->
      val user = firebaseAuth.currentUser
      _currentUser.value = user
      if (user != null) {
        startObservingUserData(user.uid)
      } else {
        stopObservingUserData()
      }
    }

    auth.currentUser?.let { user ->
      startObservingUserData(user.uid)
    } ?: run {
      loadLocalStorageData()
    }
  }

  private fun loadLocalStorageData() {
    _userProfile.value = UserProfile(
      userId = "guest",
      displayName = "Guest User",
      email = "Local Storage Mode",
      googlePhotoUrl = "",
      customAvatarUrl = "",
    )
    _notes.value = localStorage.loadNotes()
    _labels.value = localStorage.loadLabels()
  }

  private fun startObservingUserData(userId: String) {
    profileJob?.cancel()
    profileJob = viewModelScope.launch {
      authRepository.observeUserProfile(userId).collect { profile ->
        _userProfile.value = profile
      }
    }

    notesJob?.cancel()
    notesJob = viewModelScope.launch(Dispatchers.IO) {
      try {
        db.collection("users").document(userId).collection("notes")
          .snapshots()
          .catch { e ->
            Log.e("CutestarViewModel", "Error observing Firestore notes: ${e.message}", e)
          }
          .collect { snapshot ->
            if (snapshot.isEmpty) {
              seedInitialNotesInFirestore(userId)
            } else {
              val cloudNotes = snapshot.documents.mapNotNull { doc ->
                try {
                  val id = doc.getString("id") ?: doc.id
                  val title = doc.getString("title") ?: ""
                  val content = doc.getString("content") ?: ""
                  val colorName = doc.getString("color") ?: NoteColor.DEFAULT.name
                  val color = try {
                    NoteColor.valueOf(colorName)
                  } catch (_: Exception) {
                    NoteColor.DEFAULT
                  }
                  val isPinned = doc.getBoolean("isPinned") ?: false
                  val isArchived = doc.getBoolean("isArchived") ?: false
                  val isDeleted = doc.getBoolean("isDeleted") ?: false
                  @Suppress("UNCHECKED_CAST")
                  val labelsList = (doc.get("labels") as? List<String>) ?: emptyList()
                  @Suppress("UNCHECKED_CAST")
                  val checklistRaw = (doc.get("checklist") as? List<Map<String, Any>>) ?: emptyList()
                  val checklistItems = checklistRaw.map { itemMap ->
                    ChecklistItem(
                      id = itemMap["id"] as? String ?: UUID.randomUUID().toString(),
                      text = itemMap["text"] as? String ?: "",
                      isChecked = itemMap["isChecked"] as? Boolean ?: false,
                    )
                  }
                  val reminder = doc.getString("reminder")
                  val drawingUrl = doc.getString("drawingUrl")
                  val drawingStoragePath = doc.getString("drawingStoragePath")

                  NoteItem(
                    id = id,
                    title = title,
                    content = content,
                    color = color,
                    isPinned = isPinned,
                    isArchived = isArchived,
                    isDeleted = isDeleted,
                    labels = labelsList,
                    checklist = checklistItems,
                    reminder = reminder,
                    drawingUrl = drawingUrl,
                    drawingStoragePath = drawingStoragePath,
                  )
                } catch (parseError: Exception) {
                  Log.w("CutestarViewModel", "Error parsing note doc: ${parseError.message}")
                  null
                }
              }

              if (cloudNotes.isNotEmpty()) {
                _notes.value = cloudNotes
              }
            }
          }
      } catch (e: Exception) {
        Log.e("CutestarViewModel", "Notes observation failure: ${e.message}", e)
      }
    }
  }

  private suspend fun seedInitialNotesInFirestore(userId: String) {
    try {
      val notesCol = db.collection("users").document(userId).collection("notes")
      SampleData.initialNotes.forEach { note ->
        val payload = mapOf(
          "id" to note.id,
          "userId" to userId,
          "title" to note.title,
          "content" to note.content,
          "color" to note.color.name,
          "isPinned" to note.isPinned,
          "isArchived" to note.isArchived,
          "isDeleted" to note.isDeleted,
          "labels" to note.labels,
          "checklist" to note.checklist.map {
            mapOf("id" to it.id, "text" to it.text, "isChecked" to it.isChecked)
          },
          "reminder" to (note.reminder ?: ""),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        notesCol.document(note.id).set(payload, SetOptions.merge()).await()
      }
    } catch (e: Exception) {
      Log.w("CutestarViewModel", "Seeding initial notes note: ${e.message}")
    }
  }

  private fun stopObservingUserData() {
    profileJob?.cancel()
    notesJob?.cancel()
    loadLocalStorageData()
  }

  fun uploadAvatar(imageUri: Uri, onResult: (Boolean, String?) -> Unit) {
    if (auth.currentUser == null) {
      // Local profile photo
      _userProfile.update { it?.copy(customAvatarUrl = imageUri.toString()) }
      onResult(true, "Local avatar updated ★")
      return
    }

    viewModelScope.launch {
      _isUploadingAvatar.value = true
      _uploadProgress.value = 0.1f
      val result = authRepository.uploadCustomAvatar(
        imageUri = imageUri,
        onProgress = { progress ->
          _uploadProgress.value = progress
        },
      )
      _isUploadingAvatar.value = false
      if (result.isSuccess) {
        onResult(true, "Profile picture updated ★")
      } else {
        onResult(false, result.exceptionOrNull()?.localizedMessage ?: "Failed to update profile picture")
      }
    }
  }

  fun signOut(context: Context, credentialManager: CredentialManager, onComplete: () -> Unit) {
    authRepository.signOut(context, credentialManager, {
      loadLocalStorageData()
      onComplete()
    }, viewModelScope)
  }

  // Core Synchronization supporting text input and canvas drawing (Local Storage + Cloud)
  fun saveNoteWithDrawing(
    noteId: String,
    title: String,
    content: String,
    color: NoteColor,
    strokes: List<DrawingStroke>,
    existingDrawingUrl: String?,
    checklist: List<ChecklistItem> = emptyList(),
    onComplete: (Boolean, String?) -> Unit,
  ) {
    val userId = auth.currentUser?.uid
    val existingNote = _notes.value.firstOrNull { it.id == noteId }
    val isPinned = existingNote?.isPinned ?: false
    val isArchived = existingNote?.isArchived ?: false

    // Optimistic UI state update
    val optimisticNote = NoteItem(
      id = noteId,
      title = title,
      content = content,
      color = color,
      isPinned = isPinned,
      isArchived = isArchived,
      checklist = checklist,
      drawingUrl = existingDrawingUrl,
      updatedAt = System.currentTimeMillis(),
    )
    _notes.update { list ->
      val index = list.indexOfFirst { it.id == noteId }
      if (index >= 0) {
        list.toMutableList().apply { set(index, optimisticNote) }
      } else {
        listOf(optimisticNote) + list
      }
    }

    if (userId == null) {
      // Local Storage Mode: save directly to local disk/SharedPreferences without requiring account
      viewModelScope.launch(Dispatchers.IO) {
        _isSavingNote.value = true
        try {
          var localDrawingUrl = existingDrawingUrl
          if (strokes.isNotEmpty()) {
            val bitmap = exportStrokesToBitmap(strokes, width = 800, height = 600)
            localDrawingUrl = localStorage.saveDrawingBitmap(noteId, bitmap)
          }

          val finalNote = optimisticNote.copy(drawingUrl = localDrawingUrl)
          _notes.update { list ->
            list.map { if (it.id == noteId) finalNote else it }
          }
          localStorage.saveNotes(_notes.value)

          withContext(Dispatchers.Main) {
            _isSavingNote.value = false
            onComplete(true, "Note & drawing saved to device ★")
          }
        } catch (e: Exception) {
          Log.e("CutestarViewModel", "Error saving note locally: ${e.message}", e)
          withContext(Dispatchers.Main) {
            _isSavingNote.value = false
            onComplete(false, "Error saving note: ${e.message}")
          }
        }
      }
      return
    }

    viewModelScope.launch(Dispatchers.IO) {
      _isSavingNote.value = true
      try {
        val noteRef = db.collection("users").document(userId).collection("notes").document(noteId)
        val snapshot = noteRef.get().await()

        val payload = mutableMapOf<String, Any>(
          "id" to noteId,
          "userId" to userId,
          "title" to title,
          "content" to content,
          "color" to color.name,
          "isPinned" to isPinned,
          "isArchived" to isArchived,
          "isDeleted" to false,
          "labels" to (existingNote?.labels ?: emptyList<String>()),
          "checklist" to (checklist.ifEmpty { existingNote?.checklist ?: emptyList() }).map {
            mapOf("id" to it.id, "text" to it.text, "isChecked" to it.isChecked)
          },
          "updatedAt" to FieldValue.serverTimestamp(),
        )

        if (!snapshot.exists()) {
          payload["createdAt"] = FieldValue.serverTimestamp()
        }

        if (existingDrawingUrl != null) {
          payload["drawingUrl"] = existingDrawingUrl
        }

        // 1. Synchronize text content directly to Firestore
        noteRef.set(payload, SetOptions.merge()).await()

        // 2. Export canvas drawings on client side and asynchronously upload to Firebase Storage
        if (strokes.isNotEmpty()) {
          val bitmap = exportStrokesToBitmap(strokes, width = 800, height = 600)
          val outputStream = ByteArrayOutputStream()
          bitmap.compress(Bitmap.CompressFormat.PNG, 95, outputStream)
          val bytes = outputStream.toByteArray()

          val timestamp = System.currentTimeMillis()
          val storagePath = "users/$userId/notes/$noteId/drawing_$timestamp.png"
          val storageRef = storage.reference.child(storagePath)

          val downloadUrl: String = try {
            storageRef.putBytes(bytes).await()
            storageRef.downloadUrl.await().toString()
          } catch (storageErr: Exception) {
            Log.w("CutestarViewModel", "Storage upload fallback: ${storageErr.message}")
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/png;base64,$base64"
          }

          // 3. Append storage download URL to current note document in Firestore
          noteRef.update(
            "drawingUrl", downloadUrl,
            "drawingStoragePath", storagePath,
            "updatedAt", FieldValue.serverTimestamp(),
          ).await()

          // Update local state with the uploaded drawing URL
          _notes.update { list ->
            list.map { if (it.id == noteId) it.copy(drawingUrl = downloadUrl) else it }
          }
        }

        // Cache cloud state locally for offline readiness
        localStorage.saveNotes(_notes.value)

        withContext(Dispatchers.Main) {
          _isSavingNote.value = false
          onComplete(true, "Note & drawing synchronized ★")
        }
      } catch (e: Exception) {
        Log.e("CutestarViewModel", "Error syncing note: ${e.message}", e)
        withContext(Dispatchers.Main) {
          _isSavingNote.value = false
          onComplete(false, e.localizedMessage ?: "Sync error")
        }
      }
    }
  }

  // Filtered notes based on current destination and search query
  val filteredNotes: StateFlow<List<NoteItem>> = combine(
    _notes,
    _currentDestination,
    _searchQuery,
  ) { allNotes, destination, query ->
    val baseList = when (destination) {
      is NavigationDestination.Notes -> allNotes.filter { !it.isArchived && !it.isDeleted }
      is NavigationDestination.Reminders -> allNotes.filter { it.reminder != null && !it.isDeleted }
      is NavigationDestination.Label -> allNotes.filter { it.labels.contains(destination.name) && !it.isDeleted }
      is NavigationDestination.Archive -> allNotes.filter { it.isArchived && !it.isDeleted }
      is NavigationDestination.Trash -> allNotes.filter { it.isDeleted }
      is NavigationDestination.Settings -> emptyList()
    }

    if (query.isBlank()) {
      baseList
    } else {
      val trimmed = query.trim().lowercase()
      baseList.filter { note ->
        note.title.lowercase().contains(trimmed) ||
          note.content.lowercase().contains(trimmed) ||
          note.labels.any { it.lowercase().contains(trimmed) } ||
          note.checklist.any { it.text.lowercase().contains(trimmed) }
      }
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = SampleData.initialNotes.filter { !it.isArchived && !it.isDeleted },
  )

  fun toggleLayoutMode() {
    _layoutMode.update { current ->
      if (current == LayoutMode.STAGGERED_GRID) LayoutMode.SINGLE_COLUMN else LayoutMode.STAGGERED_GRID
    }
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setDestination(destination: NavigationDestination) {
    _currentDestination.value = destination
  }

  fun togglePin(noteId: String) {
    _notes.update { list ->
      list.map { if (it.id == noteId) it.copy(isPinned = !it.isPinned) else it }
    }

    val userId = auth.currentUser?.uid
    if (userId == null) {
      localStorage.saveNotes(_notes.value)
      return
    }

    viewModelScope.launch(Dispatchers.IO) {
      try {
        val current = _notes.value.firstOrNull { it.id == noteId }
        if (current != null) {
          db.collection("users").document(userId).collection("notes").document(noteId)
            .update("isPinned", current.isPinned, "updatedAt", FieldValue.serverTimestamp())
        }
      } catch (e: Exception) {
        Log.w("CutestarViewModel", "Error updating pin in Firestore: ${e.message}")
      }
    }
  }

  fun addQuickNote(
    title: String,
    content: String,
    color: NoteColor = NoteColor.DEFAULT,
    checklist: List<ChecklistItem> = emptyList(),
  ) {
    saveNoteWithDrawing(
      noteId = UUID.randomUUID().toString(),
      title = title,
      content = content,
      color = color,
      strokes = emptyList(),
      existingDrawingUrl = null,
      checklist = checklist,
      onComplete = { _, _ -> },
    )
  }

  fun createLabel(name: String) {
    val trimmed = name.trim()
    if (trimmed.isNotEmpty() && !_labels.value.contains(trimmed)) {
      _labels.update { it + trimmed }
      if (auth.currentUser == null) {
        localStorage.saveLabels(_labels.value)
      }
    }
  }
}
