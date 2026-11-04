package com.example.data

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.model.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream

class AuthRepository(private val context: Context) {

  private val auth: FirebaseAuth = Firebase.auth
  private val db: FirebaseFirestore by lazy {
    try {
      FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
    } catch (_: Exception) {
      FirebaseFirestore.getInstance()
    }
  }
  private val storage: FirebaseStorage by lazy {
    FirebaseStorage.getInstance()
  }

  val currentUser: FirebaseUser?
    get() = auth.currentUser

  fun observeUserProfile(userId: String): Flow<UserProfile?> {
    return db.collection("users").document(userId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(UserProfile::class.java)
        } else {
          null
        }
      }
      .catch { e ->
        Log.e("AuthRepository", "Error observing user profile: ${e.message}", e)
        emit(null)
      }
      .flowOn(Dispatchers.IO)
  }

  fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val activeNetwork = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }

  suspend fun syncGoogleUserToFirestore(user: FirebaseUser) = withContext(Dispatchers.IO) {
    if (!isOnline(context)) return@withContext
    try {
      withTimeoutOrNull(3000L) {
        val userRef = db.collection("users").document(user.uid)
        val snapshot = userRef.get().await()

        val profileData = mutableMapOf<String, Any>(
          "userId" to user.uid,
          "displayName" to (user.displayName ?: "Cutestar User"),
          "email" to (user.email ?: ""),
          "updatedAt" to FieldValue.serverTimestamp(),
        )

        if (user.photoUrl != null) {
          profileData["googlePhotoUrl"] = user.photoUrl.toString()
        }

        if (!snapshot.exists()) {
          profileData["createdAt"] = FieldValue.serverTimestamp()
          profileData["customAvatarUrl"] = ""
          profileData["avatarStoragePath"] = ""
          userRef.set(profileData, SetOptions.merge()).await()
        } else {
          userRef.set(profileData, SetOptions.merge()).await()
        }
      }
    } catch (e: Exception) {
      Log.w("AuthRepository", "syncGoogleUserToFirestore note: ${e.message}")
    }
  }

  fun onGoogleSignInClicked(
    activity: Activity,
    credentialManager: CredentialManager,
    onAuthSuccess: (FirebaseUser) -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {},
  ) {
    val clientId = try {
      activity.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
      return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
      try {
        val result = credentialManager.getCredential(activity, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          val authResult = auth.signInWithCredential(authCredential).await()
          val user = authResult.user
          if (user != null) {
            syncGoogleUserToFirestore(user)
            onAuthSuccess(user)
          } else {
            onAuthError("Failed to obtain signed-in user")
          }
        } else {
          onAuthError("Unexpected credential type: ${credential::class.java.name}")
        }
      } catch (e: GetCredentialCancellationException) {
        Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
        onAuthCancelled()
      } catch (e: Exception) {
        Log.e("Auth", "Google Sign-In failed", e)
        onAuthError(e.localizedMessage ?: "Sign in failed")
      }
    }
  }

  fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: (FirebaseUser) -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope,
  ) {
    val current = auth.currentUser
    if (current != null) {
      scope.launch {
        if (isOnline(context)) {
          try {
            syncGoogleUserToFirestore(current)
          } catch (_: Exception) {}
        }
        onAuthSuccess(current)
      }
      return
    }

    // Critical offline protection:
    // If the device is offline, DO NOT call CredentialManager.
    // In offline mode, invoking credentialManager.getCredential() launches a transparent
    // system overlay / activity from Google Play Services that hangs waiting for network,
    // intercepting all user touch inputs and rendering the app unresponsive until Back is pressed.
    if (!isOnline(context)) {
      onUnauthenticated()
      return
    }

    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      onUnauthenticated()
      return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
      try {
        // Enforce a strict timeout to guarantee that the UI thread never freezes
        // and no credential prompt or transparent overlay hangs in a pending state.
        val result = withTimeoutOrNull(2500L) {
          credentialManager.getCredential(context, request)
        }
        if (result == null) {
          onUnauthenticated()
          return@launch
        }
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          val authResult = auth.signInWithCredential(authCredential).await()
          val user = authResult.user
          if (user != null) {
            if (isOnline(context)) {
              try {
                syncGoogleUserToFirestore(user)
              } catch (_: Exception) {}
            }
            onAuthSuccess(user)
          } else {
            onUnauthenticated()
          }
        } else {
          onUnauthenticated()
        }
      } catch (_: Exception) {
        onUnauthenticated()
      }
    }
  }

  fun signOut(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope,
  ) {
    auth.signOut()
    scope.launch {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.e("Auth", "Failed to clear credential state", e)
      } finally {
        onSignOutComplete()
      }
    }
  }

  suspend fun uploadCustomAvatar(
    imageUri: Uri,
    onProgress: (Float) -> Unit = {},
  ): Result<String> = withContext(Dispatchers.IO) {
    val user = auth.currentUser
      ?: return@withContext Result.failure(IllegalStateException("User is not signed in"))

    try {
      // Read bytes and compress to optimal profile avatar size (512x512)
      val bytes = context.contentResolver.openInputStream(imageUri)?.use { input ->
        val originalBitmap = BitmapFactory.decodeStream(input)
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 512, 512, true)
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        outputStream.toByteArray()
      } ?: return@withContext Result.failure(IllegalArgumentException("Could not read image from URI"))

      val timestamp = System.currentTimeMillis()
      val storagePath = "users/${user.uid}/avatar_$timestamp.jpg"
      val storageRef = storage.reference.child(storagePath)

      val downloadUrl: String = try {
        val uploadTask = storageRef.putBytes(bytes)
        uploadTask.addOnProgressListener { taskSnapshot ->
          if (taskSnapshot.totalByteCount > 0) {
            val progress = taskSnapshot.bytesTransferred.toFloat() / taskSnapshot.totalByteCount
            onProgress(progress)
          }
        }.await()
        storageRef.downloadUrl.await().toString()
      } catch (storageError: Exception) {
        Log.w("AuthRepository", "Firebase Storage direct upload note: ${storageError.message}. Saving optimized data URL to Firestore.")
        val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
        "data:image/jpeg;base64,$base64String"
      }

      // Synchronize and update download URL in Firestore
      val userRef = db.collection("users").document(user.uid)
      userRef.set(
        mapOf(
          "customAvatarUrl" to downloadUrl,
          "avatarStoragePath" to storagePath,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        SetOptions.merge(),
      ).await()

      Result.success(downloadUrl)
    } catch (e: Exception) {
      Log.e("AuthRepository", "Failed to upload avatar", e)
      Result.failure(e)
    }
  }
}
