package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.UserProfile
import com.example.ui.theme.CutestarPrimary

@Composable
fun UserAvatar(
  userProfile: UserProfile?,
  modifier: Modifier = Modifier,
  size: Dp = 34.dp,
  isUploading: Boolean = false,
  uploadProgress: Float = 0f,
) {
  val avatarUrl = userProfile?.effectiveAvatarUrl()
  val context = LocalContext.current

  Box(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .border(1.5.dp, CutestarPrimary.copy(alpha = 0.5f), CircleShape)
      .testTag("user_avatar"),
    contentAlignment = Alignment.Center,
  ) {
    if (!avatarUrl.isNullOrBlank()) {
      SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
          .data(avatarUrl)
          .crossfade(true)
          .build(),
        contentDescription = "User Avatar",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        loading = {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(size * 0.5f),
              strokeWidth = 2.dp,
              color = CutestarPrimary,
            )
          }
        },
        error = {
          // Elegant default local asset placeholder avatar
          Image(
            painter = painterResource(R.drawable.ic_cutestar_logo),
            contentDescription = "Default Avatar Placeholder",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
        },
      )
    } else {
      // Default local asset placeholder avatar
      Image(
        painter = painterResource(R.drawable.ic_cutestar_logo),
        contentDescription = "Default Avatar Placeholder",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
      )
    }

    // Upload progress indicator overlay
    if (isUploading) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clip(CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(
          progress = { if (uploadProgress > 0f) uploadProgress else 0.5f },
          modifier = Modifier.size(size * 0.8f),
          strokeWidth = 3.dp,
          color = Color.White,
          trackColor = CutestarPrimary.copy(alpha = 0.4f),
        )
      }
    }
  }
}
