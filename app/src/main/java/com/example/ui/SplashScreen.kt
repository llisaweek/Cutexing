package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import kotlinx.coroutines.delay

/**
 * Splash screen component.
 * Renders ONLY the background container with no children inside, per specification.
 */
@Composable
fun SplashScreen(
  onSplashFinished: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LaunchedEffect(Unit) {
    delay(850) // Smooth initial splash duration
    onSplashFinished()
  }

  // Background container with NO children inside
  Box(
    modifier = modifier
      .fillMaxSize()
      .testTag("splash_screen_container"),
  ) {
    Image(
      painter = painterResource(R.drawable.bg_launch_screen),
      contentDescription = null,
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Crop,
    )
  }
}
