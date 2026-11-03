package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserProfile
import com.example.ui.theme.CutestarPrimary

@Composable
fun ProfileDialog(
  userProfile: UserProfile?,
  isUploadingAvatar: Boolean,
  uploadProgress: Float,
  onPickNewAvatar: (Uri) -> Unit,
  onSignOut: () -> Unit,
  onSignInWithGoogle: (() -> Unit)? = null,
  onDismiss: () -> Unit,
) {
  val isGuest = userProfile?.userId == null || userProfile.userId == "guest"
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri ->
      if (uri != null) {
        onPickNewAvatar(uri)
      }
    },
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(24.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Text(
        text = if (isGuest) "Local Profile" else "Account Profile",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Tappable Avatar with Camera icon badge
        Box(
          modifier = Modifier
            .size(100.dp)
            .clickable(enabled = !isUploadingAvatar) {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
              )
            }
            .testTag("avatar_picker_trigger"),
          contentAlignment = Alignment.Center,
        ) {
          UserAvatar(
            userProfile = userProfile,
            size = 96.dp,
            isUploading = isUploadingAvatar,
            uploadProgress = uploadProgress,
          )

          // Camera badge in bottom right corner
          Box(
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .size(30.dp)
              .clip(CircleShape)
              .background(CutestarPrimary)
              .shadow(2.dp, CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Outlined.CameraAlt,
              contentDescription = "Upload new photo",
              tint = Color.White,
              modifier = Modifier.size(16.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Display Name & Email
        Text(
          text = userProfile?.displayName?.ifBlank { "Cutestar User" } ?: "Cutestar User",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = userProfile?.email?.ifBlank { "Signed in with Google" } ?: "Signed in with Google",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Status badge
        val avatarSource = when {
          isGuest -> "Local Storage Mode (On-Device)"
          !userProfile?.customAvatarUrl.isNullOrBlank() -> "Custom profile photo (Cloud synced)"
          !userProfile?.googlePhotoUrl.isNullOrBlank() -> "Google account photo"
          else -> "Default Cutestar avatar"
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = CutestarPrimary.copy(alpha = 0.12f),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.CheckCircle,
              contentDescription = null,
              tint = CutestarPrimary,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = avatarSource,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
              color = CutestarPrimary,
            )
          }
        }

        // Upload progress indicator
        if (isUploadingAvatar) {
          Spacer(modifier = Modifier.height(14.dp))
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            LinearProgressIndicator(
              progress = { if (uploadProgress > 0f) uploadProgress else 0.5f },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = CutestarPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Uploading photo…",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // "Choose New Photo" Button
        Button(
          onClick = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
          },
          enabled = !isUploadingAvatar,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("choose_photo_button"),
          colors = ButtonDefaults.buttonColors(containerColor = CutestarPrimary),
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Select Custom Photo")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isGuest) {
          if (onSignInWithGoogle != null) {
            Button(
              onClick = onSignInWithGoogle,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("sign_in_google_dialog_button"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
              ),
            ) {
              Text("Sign in with Google to Sync Cloud", fontSize = 13.sp)
            }
          }
        } else {
          // Sign Out Button
          OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("sign_out_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.Logout,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
              tint = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Sign out to Local Mode",
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    },
  )
}
