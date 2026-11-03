package com.example.ui

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.AuthRepository
import com.example.ui.theme.CutestarPrimary
import com.google.firebase.auth.FirebaseUser

@Composable
fun AuthScreen(
  authRepository: AuthRepository,
  onAuthSuccess: (FirebaseUser) -> Unit,
  onContinueAsGuest: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val activity = context as? Activity
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  Box(
    modifier = modifier.fillMaxSize(),
  ) {
    // Background using the uploaded launch screen image
    Image(
      painter = painterResource(R.drawable.bg_launch_screen),
      contentDescription = null,
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Crop,
    )

    // Center card
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(elevation = 12.dp, shape = RoundedCornerShape(28.dp))
          .testTag("auth_card"),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = "Cutestar",
            style = MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "Capture your thoughts, ideas, and canvas drawings with local storage or Google Cloud sync.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
          )

          Spacer(modifier = Modifier.height(28.dp))

          // Google Sign-In Button
          Button(
            onClick = {
              if (activity != null) {
                isLoading = true
                errorMessage = null
                authRepository.onGoogleSignInClicked(
                  activity = activity,
                  credentialManager = credentialManager,
                  onAuthSuccess = { user ->
                    isLoading = false
                    onAuthSuccess(user)
                  },
                  onAuthError = { error ->
                    isLoading = false
                    errorMessage = error
                  },
                  scope = scope,
                  onAuthCancelled = {
                    isLoading = false
                  },
                )
              }
            },
            enabled = !isLoading,
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("google_sign_in_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.surface,
              contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
              brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE0E0E0)),
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = CutestarPrimary,
                strokeWidth = 2.5.dp,
              )
            } else {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
              ) {
                // Google "G" emblem
                Box(
                  modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(0.5.dp, Color(0xFFDDDDDD), CircleShape),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = "G",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF4285F4),
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = "Sign in with Google",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                  ),
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          androidx.compose.material3.OutlinedButton(
            onClick = onContinueAsGuest,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("continue_as_guest_button"),
            shape = RoundedCornerShape(16.dp),
          ) {
            Text(
              text = "Use Local Storage Mode",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
              ),
              color = MaterialTheme.colorScheme.onSurface,
            )
          }

          if (errorMessage != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = errorMessage ?: "",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    }
  }
}
