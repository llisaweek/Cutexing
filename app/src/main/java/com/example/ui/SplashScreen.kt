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
 * Startup splash screen.
 * Seamlessly transitions to the main page within normal startup time.
 */
@Composable
fun SplashScreen(
  onSplashFinished: () -> Unit,
  modifier: Modifier = Modifier,
) {

  // Edge-to-edge drawing is already enabled in MainActivity
  // Optimize startup duration so total elapsed time to main page is kept well within 2 seconds
  LaunchedEffect(Unit) {
    delay(600)
    onSplashFinished()
  }

  // Full-screen background covering the entire display area
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

