package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import com.example.data.AuthRepository
import com.example.ui.AuthScreen
import com.example.ui.CutestarScreen
import com.example.ui.SplashScreen
import com.example.ui.theme.CutestarTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CutestarViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
      window.attributes.layoutInDisplayCutoutMode =
        android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
    enableEdgeToEdge()
    setContent {
      CutestarTheme(darkTheme = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
          AppContent()
        }
      }
    }
  }
}

@Composable
fun AppContent() {
  val viewModel: CutestarViewModel = viewModel()
  val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
  val context = LocalContext.current

  val localizedConfiguration = remember(currentLanguage) {
    val loc = when (currentLanguage) {
      "zh" -> Locale.SIMPLIFIED_CHINESE
      "ja" -> Locale.JAPANESE
      "en" -> Locale.ENGLISH
      else -> Locale.getDefault()
    }
    Configuration(context.resources.configuration).apply {
      setLocale(loc)
    }
  }

  val localizedContext = remember(currentLanguage) {
    val loc = when (currentLanguage) {
      "zh" -> Locale.SIMPLIFIED_CHINESE
      "ja" -> Locale.JAPANESE
      "en" -> Locale.ENGLISH
      else -> Locale.getDefault()
    }
    val config = Configuration(context.resources.configuration)
    config.setLocale(loc)
    context.createConfigurationContext(config)
  }

  CompositionLocalProvider(
    LocalConfiguration provides localizedConfiguration,
    LocalContext provides localizedContext,
  ) {
    AppContentBody(viewModel = viewModel)
  }
}

@Composable
fun AppContentBody(viewModel: CutestarViewModel) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val authRepository = remember { AuthRepository(context) }
  val credentialManager = remember { CredentialManager.create(context) }
  var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

  var isSplashActive by remember { mutableStateOf(true) }
  var showAuthScreen by remember { mutableStateOf(false) }

  // Listen to Auth State
  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth ->
      currentUser = auth.currentUser
    }
    Firebase.auth.addAuthStateListener(listener)
    onDispose {
      Firebase.auth.removeAuthStateListener(listener)
    }
  }

  // Attempt silent auto-sign-in on app startup if a previous Google session exists
  LaunchedEffect(Unit) {
    if (currentUser == null) {
      authRepository.attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = { user -> currentUser = user },
        onUnauthenticated = { /* Continues directly in Local Storage Mode */ },
        scope = scope,
      )
    }
  }

  if (isSplashActive) {
    // Splash screen: ONLY renders the background container with no children inside
    SplashScreen(
      onSplashFinished = { isSplashActive = false },
    )
  } else if (showAuthScreen) {
    AuthScreen(
      authRepository = authRepository,
      onAuthSuccess = { user ->
        currentUser = user
        showAuthScreen = false
      },
      onContinueAsGuest = {
        showAuthScreen = false
      },
    )
  } else {
    // Main interface accessible directly without requiring an account (Local Storage Mode)
    CutestarScreen(
      viewModel = viewModel,
      onSignOut = {
        currentUser = null
      },
      onSignInRequest = {
        showAuthScreen = true
      },
    )
  }
}
