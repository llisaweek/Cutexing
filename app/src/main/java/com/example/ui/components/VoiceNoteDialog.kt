package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CutestarPrimary
import kotlinx.coroutines.delay

@Composable
fun VoiceNoteDialog(
  onSaveVoiceNote: (title: String, transcript: String) -> Unit,
  onDismiss: () -> Unit,
) {
  var isRecording by remember { mutableStateOf(true) }
  var transcript by remember { mutableStateOf("") }
  var noteTitle by remember { mutableStateOf("Voice Note 🎙️") }
  var recordingSeconds by remember { mutableStateOf(0) }

  // Pulsing animation for microphone
  val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = if (isRecording) 1.25f else 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "pulse_scale",
  )

  LaunchedEffect(isRecording) {
    if (isRecording) {
      while (isRecording) {
        delay(1000)
        recordingSeconds++
        if (recordingSeconds == 2 && transcript.isEmpty()) {
          transcript = "Meeting summary: prioritize mobile-first note layout and pastel card theme."
        }
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.testTag("voice_note_dialog"),
    title = {
      Text(
        text = if (isRecording) "Listening…" else "Voice Note captured",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Animated Mic button
        Box(
          modifier = Modifier
            .size(80.dp)
            .scale(if (isRecording) pulseScale else 1f)
            .background(
              color = if (isRecording) CutestarPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
              shape = CircleShape,
            ),
          contentAlignment = Alignment.Center,
        ) {
          IconButton(
            onClick = { isRecording = !isRecording },
            modifier = Modifier.size(56.dp),
          ) {
            Icon(
              imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
              contentDescription = if (isRecording) "Stop recording" else "Start recording",
              tint = if (isRecording) MaterialTheme.colorScheme.error else CutestarPrimary,
              modifier = Modifier.size(32.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val minutes = recordingSeconds / 60
        val seconds = recordingSeconds % 60
        Text(
          text = String.format("%02d:%02d", minutes, seconds),
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Note title input
        OutlinedTextField(
          value = noteTitle,
          onValueChange = { noteTitle = it },
          label = { Text("Title") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Transcript / Content input
        OutlinedTextField(
          value = transcript,
          onValueChange = { transcript = it },
          label = { Text("Spoken content / Notes") },
          modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (transcript.isNotBlank() || noteTitle.isNotBlank()) {
            onSaveVoiceNote(noteTitle.trim(), transcript.trim())
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = CutestarPrimary),
        modifier = Modifier.testTag("save_voice_note_btn"),
      ) {
        Text("Save note")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}
