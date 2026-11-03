package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.NoteColor
import com.example.model.NoteItem
import com.example.ui.theme.CutestarPrimary

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
  note: NoteItem,
  isSelected: Boolean = false,
  isSelectionModeActive: Boolean = false,
  onClick: () -> Unit,
  onLongClick: () -> Unit = {},
  onTogglePin: () -> Unit,
  onToggleChecklistItem: ((noteId: String, itemId: String) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val isDark = false
  val containerColor = note.color.surfaceColor(isDark)
  val borderColor = if (isSelected) CutestarPrimary else note.color.borderColor(isDark)
  val borderWidth = if (isSelected) 2.5.dp else 1.dp

  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
      )
      .testTag("note_card_${note.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = BorderStroke(borderWidth, borderColor),
    elevation = CardDefaults.cardElevation(
      defaultElevation = if (isSelected) 4.dp else if (note.color == NoteColor.DEFAULT) 1.dp else 0.dp
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
    ) {
      // Photo Image Preview Banner (if note has an image)
      if (!note.imageUrl.isNullOrBlank()) {
        SubcomposeAsyncImage(
          model = ImageRequest.Builder(LocalContext.current)
            .data(note.imageUrl)
            .crossfade(true)
            .build(),
          contentDescription = "Note photo attachment",
          modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(0.5.dp, note.color.lightBorder, RoundedCornerShape(12.dp)),
          contentScale = ContentScale.Crop,
          loading = {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = CutestarPrimary)
            }
          },
        )
        Spacer(modifier = Modifier.height(10.dp))
      }

      // Canvas Drawing Sketch Preview (if note has a drawing)
      if (!note.drawingUrl.isNullOrBlank()) {
        SubcomposeAsyncImage(
          model = ImageRequest.Builder(LocalContext.current)
            .data(note.drawingUrl)
            .crossfade(true)
            .build(),
          contentDescription = "Note sketch",
          modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(0.5.dp, note.color.lightBorder, RoundedCornerShape(12.dp)),
          contentScale = ContentScale.Crop,
          loading = {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = CutestarPrimary)
            }
          },
        )
        Spacer(modifier = Modifier.height(8.dp))
      }

      // Header: Selection Checkmark or Title + Pin button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        if (isSelected || isSelectionModeActive) {
          Icon(
            imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.CheckBoxOutlineBlank,
            contentDescription = if (isSelected) "Selected" else "Not selected",
            tint = if (isSelected) CutestarPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
              .padding(end = 8.dp)
              .size(22.dp),
          )
        }

        if (note.title.isNotEmpty()) {
          Text(
            text = note.title,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.SemiBold,
              lineHeight = 20.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
              .weight(1f)
              .padding(end = 4.dp),
          )
        } else {
          Spacer(modifier = Modifier.weight(1f))
        }

        // Pin button (only if not in selection mode and not deleted)
        if (!isSelectionModeActive && !note.isDeleted) {
          IconButton(
            onClick = onTogglePin,
            modifier = Modifier
              .size(32.dp)
              .testTag("pin_button_${note.id}"),
          ) {
            Icon(
              imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
              contentDescription = if (note.isPinned) "Unpin note" else "Pin note",
              tint = if (note.isPinned) CutestarPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

      // Content text snippet (if any)
      if (note.content.isNotEmpty()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = note.content,
          style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 19.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 7,
          overflow = TextOverflow.Ellipsis,
        )
      }

      // Interactive Checklist items (Google Keep signature: direct toggle from card!)
      if (note.checklist.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        note.checklist.take(5).forEach { item ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(
              onClick = { onToggleChecklistItem?.invoke(note.id, item.id) },
              modifier = Modifier.size(24.dp),
            ) {
              Icon(
                imageVector = if (item.isChecked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                contentDescription = if (item.isChecked) "Mark unchecked" else "Mark checked",
                modifier = Modifier.size(16.dp),
                tint = if (item.isChecked) CutestarPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = item.text,
              style = MaterialTheme.typography.bodySmall.copy(
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
              ),
              color = if (item.isChecked) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
              } else {
                MaterialTheme.colorScheme.onSurface
              },
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        if (note.checklist.size > 5) {
          Text(
            text = "+${note.checklist.size - 5} more items",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 28.dp, top = 2.dp),
          )
        }
      }

      // Audio note badge
      if (!note.audioUrl.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
          border = BorderStroke(0.5.dp, note.color.lightBorder),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.Mic,
              contentDescription = null,
              modifier = Modifier.size(13.dp),
              tint = CutestarPrimary,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Voice note",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Reminder Chip & Label Chips
      val hasChips = note.reminder != null || note.labels.isNotEmpty()
      if (hasChips) {
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          // Reminder Chip
          if (note.reminder != null) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
              border = BorderStroke(0.5.dp, note.color.lightBorder),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Outlined.AccessTime,
                  contentDescription = null,
                  modifier = Modifier.size(12.dp),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = note.reminder,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          // Label Chips
          note.labels.forEach { label ->
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
              border = BorderStroke(0.5.dp, note.color.lightBorder),
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
        }
      }
    }
  }
}
