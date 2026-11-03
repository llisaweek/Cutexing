package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CutestarPrimary

@Composable
fun BottomQuickBar(
  onTakeNote: () -> Unit,
  onNewList: () -> Unit,
  onNewDrawing: () -> Unit,
  onNewAudio: () -> Unit,
  onNewPhoto: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .shadow(elevation = 8.dp)
      .background(MaterialTheme.colorScheme.surface)
      .testTag("bottom_quick_bar"),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding(),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(60.dp)
          .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        // Quick Action icons on left
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          IconButton(
            onClick = onNewList,
            modifier = Modifier
              .size(48.dp)
              .testTag("new_list_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.CheckBox,
              contentDescription = stringResource(R.string.new_list),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          IconButton(
            onClick = onNewDrawing,
            modifier = Modifier
              .size(48.dp)
              .testTag("new_drawing_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Brush,
              contentDescription = stringResource(R.string.new_drawing),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          IconButton(
            onClick = onNewAudio,
            modifier = Modifier
              .size(48.dp)
              .testTag("new_audio_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Mic,
              contentDescription = stringResource(R.string.new_audio),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          IconButton(
            onClick = onNewPhoto,
            modifier = Modifier
              .size(48.dp)
              .testTag("new_photo_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Image,
              contentDescription = stringResource(R.string.new_photo),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // Right side: "Take a note…" action button / FAB
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(end = 4.dp),
        ) {
          // Keep-style prominent FAB
          FloatingActionButton(
            onClick = onTakeNote,
            modifier = Modifier
              .size(48.dp)
              .testTag("take_note_fab"),
            shape = CircleShape,
            containerColor = CutestarPrimary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = stringResource(R.string.take_a_note),
              modifier = Modifier.size(26.dp),
            )
          }
        }
      }
    }
  }
}
