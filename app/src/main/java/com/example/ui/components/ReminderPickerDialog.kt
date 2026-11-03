package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CutestarPrimary

@Composable
fun ReminderPickerDialog(
  currentReminder: String?,
  onSetReminder: (String) -> Unit,
  onClearReminder: () -> Unit,
  onDismiss: () -> Unit,
) {
  var isCustomDateMode by remember { mutableStateOf(false) }
  var customDateText by remember { mutableStateOf(currentReminder ?: "") }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.testTag("reminder_picker_dialog"),
    title = {
      Text(
        text = if (isCustomDateMode) "Pick date & time" else "Add reminder",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        if (!isCustomDateMode) {
          // Google Keep signature reminder presets
          ReminderPresetRow(
            icon = Icons.Outlined.AccessTime,
            title = "Later today",
            time = "6:00 PM",
            onClick = { onSetReminder("Today, 6:00 PM") },
          )
          ReminderPresetRow(
            icon = Icons.Outlined.WbSunny,
            title = "Tomorrow morning",
            time = "8:00 AM",
            onClick = { onSetReminder("Tomorrow, 8:00 AM") },
          )
          ReminderPresetRow(
            icon = Icons.Outlined.CalendarToday,
            title = "Next week",
            time = "Mon, 8:00 AM",
            onClick = { onSetReminder("Next Monday, 8:00 AM") },
          )

          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

          // Custom picker option
          ReminderPresetRow(
            icon = Icons.Outlined.CalendarToday,
            title = "Pick a date & time…",
            time = "",
            onClick = { isCustomDateMode = true },
          )

          if (currentReminder != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClearReminder)
                .padding(vertical = 10.dp, horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = "Delete reminder",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
              )
            }
          }
        } else {
          // Custom Date Input
          Text(
            text = "Enter a date and time description (e.g. 'Friday, 2:30 PM' or 'Aug 15, 10:00 AM'):",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = customDateText,
            onValueChange = { customDateText = it },
            placeholder = { Text("e.g., Friday, 3:00 PM") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("custom_reminder_input"),
          )
        }
      }
    },
    confirmButton = {
      if (isCustomDateMode) {
        Button(
          onClick = {
            if (customDateText.isNotBlank()) {
              onSetReminder(customDateText.trim())
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = CutestarPrimary),
          modifier = Modifier.testTag("save_custom_reminder_btn"),
        ) {
          Text("Save")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = {
        if (isCustomDateMode) isCustomDateMode = false else onDismiss()
      }) {
        Text(if (isCustomDateMode) "Back" else "Cancel")
      }
    },
  )
}

@Composable
private fun ReminderPresetRow(
  icon: ImageVector,
  title: String,
  time: String,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 12.dp, horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.width(14.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
    if (time.isNotEmpty()) {
      Text(
        text = time,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
