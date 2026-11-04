package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AuthRepository
import com.example.data.LocalNotesStorage
import com.example.data.SampleData
import com.example.model.ChecklistItem
import com.example.model.LayoutMode
import com.example.model.NavigationDestination
import com.example.model.NoteColor
import com.example.model.NoteItem
import com.example.model.NoteSortOrder
import com.example.model.UserProfile
import com.example.ui.components.CanvasBounds
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID

enum class NoteTypeFilter(val displayName: String) {
  ALL("All"),
  NOTES("Text"),
  LISTS("Lists"),
  DRAWINGS("Drawings"),
  IMAGES("Images"),
  REMINDERS("Reminders"),
}

class CutestarViewModel(application: Application) : AndroidViewModel(application) {

  val authRepository = AuthRepository(application)
  private val localStorage = LocalNotesStorage(application)
  private val auth: FirebaseAuth = Firebase.auth
  private val db: FirebaseFirestore by lazy {
    try {
      FirebaseFirestore.getInstance(application.getString(R.string.firestore_database_id))
    } catch (_: Exception) {
      FirebaseFirestore.getInstance()
    }
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

  private val _selectedTypeFilter = MutableStateFlow(NoteTypeFilter.ALL)
  val selectedTypeFilter: StateFlow<NoteTypeFilter> = _selectedTypeFilter

  private val _selectedColorFilter = MutableStateFlow<NoteColor?>(null)
  val selectedColorFilter: StateFlow<NoteColor?> = _selectedColorFilter

  private val _sortOrder = MutableStateFlow(NoteSortOrder.MODIFIED_DESC)
  val sortOrder: StateFlow<NoteSortOrder> = _sortOrder.asStateFlow()

  private val _layoutMode = MutableStateFlow(LayoutMode.STAGGERED_GRID)
  val layoutMode: StateFlow<LayoutMode> = _layoutMode

  // Selection mode for batch operations (Keep signature feature)
  private val _selectedNoteIds = MutableStateFlow<Set<String>>(emptySet())
  val selectedNoteIds: StateFlow<Set<String>> = _selectedNoteIds.asStateFlow()

  // Keep Settings preferences
  private val _moveCheckedToBottom = MutableStateFlow(true)
  val moveCheckedToBottom: StateFlow<Boolean> = _moveCheckedToBottom.asStateFlow()

  // App Language preference
  private val _currentLanguage = MutableStateFlow(localStorage.getLanguage())
  val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

  fun setLanguage(code: String) {
    _currentLanguage.value = code
    localStorage.saveLanguage(code)
  }

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
                  val imageUrl = doc.getString("imageUrl")
                  val imageStoragePath = doc.getString("imageStoragePath")
                  val audioUrl = doc.getString("audioUrl")

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
                    imageUrl = imageUrl,
                    imageStoragePath = imageStoragePath,
                    audioUrl = audioUrl,
                  )
                } catch (parseError: Exception) {
                  Log.w("CutestarViewModel", "Error parsing note doc: ${parseError.message}")
                  null
                }
              }

              if (cloudNotes.isNotEmpty()) {
                _notes.value = cloudNotes
                localStorage.saveNotes(cloudNotes)
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
          "drawingUrl" to (note.drawingUrl ?: ""),
          "imageUrl" to (note.imageUrl ?: ""),
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

  // Unified save supporting text, drawing, photo attachment, checklist, reminders, and labels
  fun saveFullNote(
    noteId: String,
    title: String,
    content: String,
    color: NoteColor,
    isPinned: Boolean? = null,
    isArchived: Boolean? = null,
    strokes: List<DrawingStroke> = emptyList(),
    existingDrawingUrl: String? = null,
    photoUri: Uri? = null,
    existingImageUrl: String? = null,
    checklist: List<ChecklistItem> = emptyList(),
    reminder: String? = null,
    labels: List<String>? = null,
    audioUrl: String? = null,
    onComplete: (Boolean, String?) -> Unit = { _, _ -> },
  ) {
    val userId = auth.currentUser?.uid
    val existingNote = _notes.value.firstOrNull { it.id == noteId }
    val pinned = isPinned ?: existingNote?.isPinned ?: false
    val archived = isArchived ?: existingNote?.isArchived ?: false
    val finalLabels = labels ?: existingNote?.labels ?: emptyList()
    val finalReminder = reminder ?: existingNote?.reminder
    val finalAudioUrl = audioUrl ?: existingNote?.audioUrl

    val optimisticNote = NoteItem(
      id = noteId,
      title = title,
      content = content,
      color = color,
      isPinned = pinned,
      isArchived = archived,
      isDeleted = false,
      labels = finalLabels,
      checklist = checklist,
      reminder = finalReminder,
      drawingUrl = existingDrawingUrl,
      imageUrl = existingImageUrl,
      audioUrl = finalAudioUrl,
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

    viewModelScope.launch(Dispatchers.IO) {
      _isSavingNote.value = true
      try {
        var localDrawingUrl = existingDrawingUrl
        var localImageUrl = existingImageUrl

        // Export strokes if new canvas drawing is created
        if (strokes.isNotEmpty()) {
          val bitmap = exportStrokesToBitmap(strokes, width = 800, height = 600)
          localDrawingUrl = localStorage.saveDrawingBitmap(noteId, bitmap)
        }

        // Handle photo image attachment
        if (photoUri != null) {
          try {
            val context = getApplication<Application>()
            val input: InputStream? = context.contentResolver.openInputStream(photoUri)
            val bitmap = BitmapFactory.decodeStream(input)
            input?.close()
            if (bitmap != null) {
              localImageUrl = localStorage.saveImageBitmap(noteId, bitmap)
            }
          } catch (imgErr: Exception) {
            Log.e("CutestarViewModel", "Error saving photo locally: ${imgErr.message}")
          }
        }

        val updatedLocalNote = optimisticNote.copy(
          drawingUrl = localDrawingUrl,
          imageUrl = localImageUrl,
        )

        _notes.update { list ->
          list.map { if (it.id == noteId) updatedLocalNote else it }
        }
        localStorage.saveNotes(_notes.value)

        // Cloud sync if signed in
        if (userId != null) {
          val noteRef = db.collection("users").document(userId).collection("notes").document(noteId)
          val snapshot = noteRef.get().await()

          val payload = mutableMapOf<String, Any>(
            "id" to noteId,
            "userId" to userId,
            "title" to title,
            "content" to content,
            "color" to color.name,
            "isPinned" to pinned,
            "isArchived" to archived,
            "isDeleted" to false,
            "labels" to finalLabels,
            "checklist" to checklist.map {
              mapOf("id" to it.id, "text" to it.text, "isChecked" to it.isChecked)
            },
            "updatedAt" to FieldValue.serverTimestamp(),
          )

          if (!snapshot.exists()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
          }
          if (finalReminder != null) payload["reminder"] = finalReminder
          if (localDrawingUrl != null) payload["drawingUrl"] = localDrawingUrl
          if (localImageUrl != null) payload["imageUrl"] = localImageUrl
          if (finalAudioUrl != null) payload["audioUrl"] = finalAudioUrl

          noteRef.set(payload, SetOptions.merge()).await()

          // Upload drawing strokes to Firebase Storage if new
          if (strokes.isNotEmpty()) {
            try {
              val bitmap = exportStrokesToBitmap(strokes, width = 800, height = 600)
              val outStream = ByteArrayOutputStream()
              bitmap.compress(Bitmap.CompressFormat.PNG, 95, outStream)
              val bytes = outStream.toByteArray()
              val storagePath = "users/$userId/notes/$noteId/drawing_${System.currentTimeMillis()}.png"
              val storageRef = storage.reference.child(storagePath)
              val downloadUrl = try {
                storageRef.putBytes(bytes).await()
                storageRef.downloadUrl.await().toString()
              } catch (_: Exception) {
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                "data:image/png;base64,$base64"
              }
              noteRef.update(
                "drawingUrl", downloadUrl,
                "drawingStoragePath", storagePath,
                "updatedAt", FieldValue.serverTimestamp(),
              ).await()
              _notes.update { list ->
                list.map { if (it.id == noteId) it.copy(drawingUrl = downloadUrl) else it }
              }
            } catch (drawErr: Exception) {
              Log.w("CutestarViewModel", "Drawing cloud upload fallback: ${drawErr.message}")
            }
          }

          // Upload photo to Firebase Storage if newly selected
          if (photoUri != null) {
            try {
              val context = getApplication<Application>()
              val input: InputStream? = context.contentResolver.openInputStream(photoUri)
              val bitmap = BitmapFactory.decodeStream(input)
              input?.close()
              if (bitmap != null) {
                val outStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outStream)
                val bytes = outStream.toByteArray()
                val storagePath = "users/$userId/notes/$noteId/image_${System.currentTimeMillis()}.jpg"
                val storageRef = storage.reference.child(storagePath)
                val downloadUrl = try {
                  storageRef.putBytes(bytes).await()
                  storageRef.downloadUrl.await().toString()
                } catch (_: Exception) {
                  val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                  "data:image/jpeg;base64,$base64"
                }
                noteRef.update(
                  "imageUrl", downloadUrl,
                  "imageStoragePath", storagePath,
                  "updatedAt", FieldValue.serverTimestamp(),
                ).await()
                _notes.update { list ->
                  list.map { if (it.id == noteId) it.copy(imageUrl = downloadUrl) else it }
                }
              }
            } catch (photoErr: Exception) {
              Log.w("CutestarViewModel", "Photo cloud upload fallback: ${photoErr.message}")
            }
          }
        }

        withContext(Dispatchers.Main) {
          _isSavingNote.value = false
          onComplete(true, "Saved & synchronized ★")
        }
      } catch (e: Exception) {
        Log.e("CutestarViewModel", "Error saving note: ${e.message}", e)
        withContext(Dispatchers.Main) {
          _isSavingNote.value = false
          onComplete(false, e.localizedMessage ?: "Sync error")
        }
      }
    }
  }

  fun saveDrawingNote(
    noteId: String,
    title: String,
    strokes: List<DrawingStroke>,
    color: NoteColor = NoteColor.DEFAULT,
    canvasBounds: CanvasBounds,
    onComplete: (Boolean, String?) -> Unit = { _, _ -> },
  ) {
    val userId = auth.currentUser?.uid
    val existingNote = _notes.value.firstOrNull { it.id == noteId }
    val pinned = existingNote?.isPinned ?: false
    val archived = existingNote?.isArchived ?: false
    val finalLabels = existingNote?.labels ?: emptyList()
    val finalTitle = title.ifBlank { "Drawing" }

    val optimisticNote = NoteItem(
      id = noteId,
      title = finalTitle,
      content = "",
      color = color,
      isPinned = pinned,
      isArchived = archived,
      isDeleted = false,
      labels = finalLabels,
      drawingUrl = existingNote?.drawingUrl,
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

    viewModelScope.launch(Dispatchers.IO) {
      _isSavingNote.value = true
      try {
        var localDrawingUrl = existingNote?.drawingUrl
        if (strokes.isNotEmpty()) {
          val bitmap = exportStrokesToBitmap(
            strokes = strokes,
            canvasLeft = canvasBounds.left,
            canvasTop = canvasBounds.top,
            canvasRight = canvasBounds.right,
            canvasBottom = canvasBounds.bottom,
          )
          localDrawingUrl = localStorage.saveDrawingBitmap(noteId, bitmap)
        }

        val updatedLocalNote = optimisticNote.copy(drawingUrl = localDrawingUrl)
        _notes.update { list ->
          list.map { if (it.id == noteId) updatedLocalNote else it }
        }
        localStorage.saveNotes(_notes.value)

        // Cloud sync if signed in
        if (userId != null && localDrawingUrl != null && strokes.isNotEmpty()) {
          try {
            val bitmap = exportStrokesToBitmap(
              strokes = strokes,
              canvasLeft = canvasBounds.left,
              canvasTop = canvasBounds.top,
              canvasRight = canvasBounds.right,
              canvasBottom = canvasBounds.bottom,
            )
            val outStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, outStream)
            val bytes = outStream.toByteArray()
            val storagePath = "users/$userId/notes/$noteId/drawing_${System.currentTimeMillis()}.png"
            val storageRef = storage.reference.child(storagePath)
            val downloadUrl = try {
              storageRef.putBytes(bytes).await()
              storageRef.downloadUrl.await().toString()
            } catch (_: Exception) {
              val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
              "data:image/png;base64,$base64"
            }

            val noteRef = db.collection("users").document(userId).collection("notes").document(noteId)
            val payload = mapOf(
              "id" to noteId,
              "userId" to userId,
              "title" to finalTitle,
              "content" to "",
              "color" to color.name,
              "isPinned" to pinned,
              "isArchived" to archived,
              "isDeleted" to false,
              "labels" to finalLabels,
              "drawingUrl" to downloadUrl,
              "drawingStoragePath" to storagePath,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
            noteRef.set(payload, SetOptions.merge()).await()
            _notes.update { list ->
              list.map { if (it.id == noteId) it.copy(drawingUrl = downloadUrl) else it }
            }
          } catch (cloudErr: Exception) {
            Log.w("CutestarViewModel", "Drawing cloud sync fallback: ${cloudErr.message}")
          }
        }

        withContext(Dispatchers.Main) {
          _isSavingNote.value = false
          onComplete(true, "Drawing saved ★")
        }
      } catch (e: Exception) {
        Log.e("CutestarViewModel", "Error saving drawing note: ${e.message}", e)
        withContext(Dispatchers.Main) {
          _isSavingNote.value = false
          onComplete(false, "Failed to save drawing")
        }
      }
    }
  }

  // Preserved backwards compatibility
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
    saveFullNote(
      noteId = noteId,
      title = title,
      content = content,
      color = color,
      strokes = strokes,
      existingDrawingUrl = existingDrawingUrl,
      checklist = checklist,
      onComplete = onComplete,
    )
  }

  // Filtered and sorted notes based on destination, search query, type filter, color filter, and sort order
  val filteredNotes: StateFlow<List<NoteItem>> = combine(
    _notes,
    _currentDestination,
    _searchQuery,
    _selectedTypeFilter,
    _selectedColorFilter,
    _sortOrder,
  ) { args: Array<Any?> ->
    @Suppress("UNCHECKED_CAST")
    val allNotes = args[0] as List<NoteItem>
    val destination = args[1] as NavigationDestination
    val query = args[2] as String
    val typeFilter = args[3] as NoteTypeFilter
    val colorFilter = args[4] as NoteColor?
    val sort = args[5] as NoteSortOrder

    var baseList = when (destination) {
      is NavigationDestination.Notes -> allNotes.filter { !it.isArchived && !it.isDeleted }
      is NavigationDestination.Reminders -> allNotes.filter { it.reminder != null && !it.isDeleted }
      is NavigationDestination.Calendar -> allNotes.filter { !it.isDeleted }
      is NavigationDestination.Label -> allNotes.filter { it.labels.contains(destination.name) && !it.isDeleted }
      is NavigationDestination.Archive -> allNotes.filter { it.isArchived && !it.isDeleted }
      is NavigationDestination.Trash -> allNotes.filter { it.isDeleted }
      is NavigationDestination.Settings -> emptyList()
    }

    // Apply color filter
    if (colorFilter != null) {
      baseList = baseList.filter { it.color == colorFilter }
    }

    // Apply type filter
    baseList = when (typeFilter) {
      NoteTypeFilter.ALL -> baseList
      NoteTypeFilter.NOTES -> baseList.filter { it.checklist.isEmpty() && it.drawingUrl.isNullOrBlank() && it.imageUrl.isNullOrBlank() }
      NoteTypeFilter.LISTS -> baseList.filter { it.checklist.isNotEmpty() }
      NoteTypeFilter.DRAWINGS -> baseList.filter { !it.drawingUrl.isNullOrBlank() }
      NoteTypeFilter.IMAGES -> baseList.filter { !it.imageUrl.isNullOrBlank() }
      NoteTypeFilter.REMINDERS -> baseList.filter { it.reminder != null }
    }

    // Apply search query
    if (query.isNotBlank()) {
      val trimmed = query.trim().lowercase()
      baseList = baseList.filter { note ->
        note.title.lowercase().contains(trimmed) ||
          note.content.lowercase().contains(trimmed) ||
          note.labels.any { it.lowercase().contains(trimmed) } ||
          note.checklist.any { it.text.lowercase().contains(trimmed) }
      }
    }

    // Apply sort order
    when (sort) {
      NoteSortOrder.MODIFIED_DESC -> baseList.sortedByDescending { it.updatedAt }
      NoteSortOrder.MODIFIED_ASC -> baseList.sortedBy { it.updatedAt }
      NoteSortOrder.TITLE_ASC -> baseList.sortedBy { it.title.ifBlank { it.content }.lowercase() }
      NoteSortOrder.COLOR -> baseList.sortedBy { it.color.name }
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

  fun setTypeFilter(filter: NoteTypeFilter) {
    _selectedTypeFilter.value = filter
  }

  fun setColorFilter(color: NoteColor?) {
    _selectedColorFilter.value = color
  }

  fun setSortOrder(order: NoteSortOrder) {
    _sortOrder.value = order
  }

  fun setDestination(destination: NavigationDestination) {
    _currentDestination.value = destination
    clearSelection()
  }

  fun togglePin(noteId: String) {
    _notes.update { list ->
      list.map { if (it.id == noteId) it.copy(isPinned = !it.isPinned) else it }
    }
    syncNoteUpdateToCloud(noteId) { note ->
      mapOf("isPinned" to note.isPinned, "updatedAt" to FieldValue.serverTimestamp())
    }
  }

  fun toggleArchive(noteId: String) {
    _notes.update { list ->
      list.map { if (it.id == noteId) it.copy(isArchived = !it.isArchived) else it }
    }
    syncNoteUpdateToCloud(noteId) { note ->
      mapOf("isArchived" to note.isArchived, "updatedAt" to FieldValue.serverTimestamp())
    }
  }

  fun trashNote(noteId: String) {
    _notes.update { list ->
      list.map { if (it.id == noteId) it.copy(isDeleted = true, isPinned = false) else it }
    }
    syncNoteUpdateToCloud(noteId) { note ->
      mapOf("isDeleted" to true, "isPinned" to false, "updatedAt" to FieldValue.serverTimestamp())
    }
  }

  fun restoreNote(noteId: String) {
    _notes.update { list ->
      list.map { if (it.id == noteId) it.copy(isDeleted = false) else it }
    }
    syncNoteUpdateToCloud(noteId) { note ->
      mapOf("isDeleted" to false, "updatedAt" to FieldValue.serverTimestamp())
    }
  }

  fun deleteNotePermanently(noteId: String) {
    _notes.update { list -> list.filterNot { it.id == noteId } }
    localStorage.saveNotes(_notes.value)
    val userId = auth.currentUser?.uid ?: return
    viewModelScope.launch(Dispatchers.IO) {
      try {
        db.collection("users").document(userId).collection("notes").document(noteId).delete().await()
      } catch (e: Exception) {
        Log.e("CutestarViewModel", "Error permanently deleting note: ${e.message}")
      }
    }
  }

  fun emptyTrash() {
    val deletedIds = _notes.value.filter { it.isDeleted }.map { it.id }
    _notes.update { list -> list.filterNot { it.isDeleted } }
    localStorage.saveNotes(_notes.value)
    val userId = auth.currentUser?.uid ?: return
    viewModelScope.launch(Dispatchers.IO) {
      deletedIds.forEach { id ->
        try {
          db.collection("users").document(userId).collection("notes").document(id).delete().await()
        } catch (_: Exception) {}
      }
    }
  }

  fun toggleChecklistItem(noteId: String, itemId: String) {
    _notes.update { list ->
      list.map { note ->
        if (note.id == noteId) {
          val updatedChecklist = note.checklist.map { item ->
            if (item.id == itemId) item.copy(isChecked = !item.isChecked) else item
          }
          note.copy(checklist = updatedChecklist)
        } else {
          note
        }
      }
    }
    syncNoteUpdateToCloud(noteId) { note ->
      mapOf(
        "checklist" to note.checklist.map {
          mapOf("id" to it.id, "text" to it.text, "isChecked" to it.isChecked)
        },
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    }
  }

  fun duplicateNote(noteId: String) {
    val original = _notes.value.firstOrNull { it.id == noteId } ?: return
    val newId = UUID.randomUUID().toString()
    val copy = original.copy(
      id = newId,
      title = if (original.title.isNotBlank()) "Copy of ${original.title}" else "Copy",
      isPinned = false,
      updatedAt = System.currentTimeMillis(),
    )
    _notes.update { listOf(copy) + it }
    localStorage.saveNotes(_notes.value)
    val userId = auth.currentUser?.uid ?: return
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val payload = mapOf(
          "id" to newId,
          "userId" to userId,
          "title" to copy.title,
          "content" to copy.content,
          "color" to copy.color.name,
          "isPinned" to false,
          "isArchived" to copy.isArchived,
          "isDeleted" to false,
          "labels" to copy.labels,
          "checklist" to copy.checklist.map {
            mapOf("id" to it.id, "text" to it.text, "isChecked" to it.isChecked)
          },
          "reminder" to (copy.reminder ?: ""),
          "drawingUrl" to (copy.drawingUrl ?: ""),
          "imageUrl" to (copy.imageUrl ?: ""),
          "audioUrl" to (copy.audioUrl ?: ""),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        db.collection("users").document(userId).collection("notes").document(newId).set(payload).await()
      } catch (e: Exception) {
        Log.e("CutestarViewModel", "Error duplicating note in cloud: ${e.message}")
      }
    }
  }

  fun updateNoteReminder(noteId: String, reminder: String?) {
    _notes.update { list ->
      list.map { if (it.id == noteId) it.copy(reminder = reminder) else it }
    }
    syncNoteUpdateToCloud(noteId) {
      val map = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
      if (reminder != null) map["reminder"] = reminder
      else map["reminder"] = FieldValue.delete()
      map
    }
  }

  fun updateNoteColor(noteId: String, color: NoteColor) {
    _notes.update { list ->
      list.map { if (it.id == noteId) it.copy(color = color) else it }
    }
    syncNoteUpdateToCloud(noteId) {
      mapOf("color" to color.name, "updatedAt" to FieldValue.serverTimestamp())
    }
  }

  fun updateNoteLabels(noteId: String, labels: List<String>) {
    _notes.update { list ->
      list.map { if (it.id == noteId) it.copy(labels = labels) else it }
    }
    syncNoteUpdateToCloud(noteId) {
      mapOf("labels" to labels, "updatedAt" to FieldValue.serverTimestamp())
    }
  }

  fun createLabel(name: String) {
    val trimmed = name.trim()
    if (trimmed.isNotEmpty() && !_labels.value.contains(trimmed)) {
      _labels.update { it + trimmed }
      localStorage.saveLabels(_labels.value)
    }
  }

  fun renameLabel(oldName: String, newName: String) {
    val trimmed = newName.trim()
    if (trimmed.isEmpty() || trimmed == oldName) return

    _labels.update { list -> list.map { if (it == oldName) trimmed else it } }
    localStorage.saveLabels(_labels.value)

    // Update all notes with oldName to newName
    _notes.update { list ->
      list.map { note ->
        if (note.labels.contains(oldName)) {
          val updatedLabels = note.labels.map { if (it == oldName) trimmed else it }
          note.copy(labels = updatedLabels)
        } else {
          note
        }
      }
    }
    localStorage.saveNotes(_notes.value)

    val userId = auth.currentUser?.uid ?: return
    viewModelScope.launch(Dispatchers.IO) {
      try {
        _notes.value.filter { it.labels.contains(trimmed) }.forEach { note ->
          db.collection("users").document(userId).collection("notes").document(note.id)
            .update("labels", note.labels, "updatedAt", FieldValue.serverTimestamp()).await()
        }
      } catch (e: Exception) {
        Log.e("CutestarViewModel", "Error renaming label in cloud: ${e.message}")
      }
    }
  }

  fun deleteLabel(name: String) {
    _labels.update { list -> list.filterNot { it == name } }
    localStorage.saveLabels(_labels.value)

    // Remove from all notes
    _notes.update { list ->
      list.map { note ->
        if (note.labels.contains(name)) {
          note.copy(labels = note.labels.filterNot { it == name })
        } else {
          note
        }
      }
    }
    localStorage.saveNotes(_notes.value)

    val userId = auth.currentUser?.uid ?: return
    viewModelScope.launch(Dispatchers.IO) {
      try {
        _notes.value.forEach { note ->
          db.collection("users").document(userId).collection("notes").document(note.id)
            .update("labels", note.labels, "updatedAt", FieldValue.serverTimestamp()).await()
        }
      } catch (e: Exception) {
        Log.e("CutestarViewModel", "Error deleting label in cloud: ${e.message}")
      }
    }
  }

  // Multi-Selection Operations
  fun toggleNoteSelection(noteId: String) {
    _selectedNoteIds.update { set ->
      if (set.contains(noteId)) set - noteId else set + noteId
    }
  }

  fun selectAllVisibleNotes() {
    _selectedNoteIds.value = filteredNotes.value.map { it.id }.toSet()
  }

  fun clearSelection() {
    _selectedNoteIds.value = emptySet()
  }

  fun pinSelectedNotes() {
    val selected = _selectedNoteIds.value
    if (selected.isEmpty()) return
    val allPinned = _notes.value.filter { selected.contains(it.id) }.all { it.isPinned }
    val newPinState = !allPinned
    _notes.update { list ->
      list.map { if (selected.contains(it.id)) it.copy(isPinned = newPinState) else it }
    }
    selected.forEach { noteId ->
      syncNoteUpdateToCloud(noteId) {
        mapOf("isPinned" to newPinState, "updatedAt" to FieldValue.serverTimestamp())
      }
    }
    clearSelection()
  }

  fun archiveSelectedNotes() {
    val selected = _selectedNoteIds.value
    if (selected.isEmpty()) return
    val isArchiveDestination = _currentDestination.value is NavigationDestination.Archive
    val newArchiveState = !isArchiveDestination
    _notes.update { list ->
      list.map { if (selected.contains(it.id)) it.copy(isArchived = newArchiveState) else it }
    }
    selected.forEach { noteId ->
      syncNoteUpdateToCloud(noteId) {
        mapOf("isArchived" to newArchiveState, "updatedAt" to FieldValue.serverTimestamp())
      }
    }
    clearSelection()
  }

  fun trashSelectedNotes() {
    val selected = _selectedNoteIds.value
    if (selected.isEmpty()) return
    _notes.update { list ->
      list.map { if (selected.contains(it.id)) it.copy(isDeleted = true, isPinned = false) else it }
    }
    selected.forEach { noteId ->
      syncNoteUpdateToCloud(noteId) {
        mapOf("isDeleted" to true, "isPinned" to false, "updatedAt" to FieldValue.serverTimestamp())
      }
    }
    clearSelection()
  }

  fun setColorForSelectedNotes(color: NoteColor) {
    val selected = _selectedNoteIds.value
    if (selected.isEmpty()) return
    _notes.update { list ->
      list.map { if (selected.contains(it.id)) it.copy(color = color) else it }
    }
    selected.forEach { noteId ->
      syncNoteUpdateToCloud(noteId) {
        mapOf("color" to color.name, "updatedAt" to FieldValue.serverTimestamp())
      }
    }
    clearSelection()
  }

  fun toggleMoveCheckedToBottom(value: Boolean) {
    _moveCheckedToBottom.value = value
  }

  private fun syncNoteUpdateToCloud(noteId: String, payloadBuilder: (NoteItem) -> Map<String, Any>) {
    localStorage.saveNotes(_notes.value)
    val userId = auth.currentUser?.uid ?: return
    val current = _notes.value.firstOrNull { it.id == noteId } ?: return
    viewModelScope.launch(Dispatchers.IO) {
      try {
        db.collection("users").document(userId).collection("notes").document(noteId)
          .set(payloadBuilder(current), SetOptions.merge()).await()
      } catch (e: Exception) {
        Log.w("CutestarViewModel", "Error syncing update to cloud: ${e.message}")
      }
    }
  }

  fun addQuickNote(
    title: String,
    content: String,
    color: NoteColor = NoteColor.DEFAULT,
    checklist: List<ChecklistItem> = emptyList(),
  ) {
    saveFullNote(
      noteId = UUID.randomUUID().toString(),
      title = title,
      content = content,
      color = color,
      checklist = checklist,
      onComplete = { _, _ -> },
    )
  }

  fun importNotes(importedList: List<NoteItem>) {
    val currentNotes = _notes.value.toMutableList()
    importedList.forEach { imported ->
      val index = currentNotes.indexOfFirst { it.id == imported.id }
      if (index >= 0) {
        currentNotes[index] = imported
      } else {
        currentNotes.add(0, imported)
      }
    }
    _notes.value = currentNotes
    localStorage.saveNotes(currentNotes)

    val userId = auth.currentUser?.uid
    if (userId != null) {
      viewModelScope.launch(Dispatchers.IO) {
        importedList.forEach { note ->
          try {
            db.collection("users").document(userId).collection("notes").document(note.id)
              .set(note, SetOptions.merge()).await()
          } catch (e: Exception) {
            Log.w("CutestarViewModel", "Error syncing imported note to cloud: ${e.message}")
          }
        }
      }
    }
  }
}
