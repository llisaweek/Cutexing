package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CutestarPrimary

@Composable
fun EditLabelsDialog(
  labels: List<String>,
  onCreateLabel: (String) -> Unit,
  onRenameLabel: (oldName: String, newName: String) -> Unit,
  onDeleteLabel: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var newLabelText by remember { mutableStateOf("") }
  var editingLabelName by remember { mutableStateOf<String?>(null) }
  var editedText by remember { mutableStateOf("") }
  var labelToDelete by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.testTag("edit_labels_dialog"),
    title = {
      Text(
        text = "Edit labels",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Create new label row
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = newLabelText,
            onValueChange = { newLabelText = it },
            placeholder = { Text("Create new label") },
            singleLine = true,
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
            trailingIcon = {
              if (newLabelText.isNotBlank()) {
                IconButton(
                  onClick = {
                    onCreateLabel(newLabelText.trim())
                    newLabelText = ""
                  },
                ) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save label",
                    tint = CutestarPrimary,
                  )
                }
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("create_label_drawer_input"),
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        // Existing labels list
        if (labels.isEmpty()) {
          Text(
            text = "No labels created yet",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp),
          )
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(240.dp),
          ) {
            items(labels) { label ->
              val isEditing = editingLabelName == label

              if (isEditing) {
                // In-line edit row
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  IconButton(
                    onClick = {
                      editingLabelName = null
                    },
                    modifier = Modifier.size(36.dp),
                  ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel")
                  }
                  OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    singleLine = true,
                    modifier = Modifier
                      .weight(1f)
                      .padding(horizontal = 4.dp),
                  )
                  IconButton(
                    onClick = {
                      if (editedText.isNotBlank()) {
                        onRenameLabel(label, editedText.trim())
                        editingLabelName = null
                      }
                    },
                    modifier = Modifier.size(36.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Default.Check,
                      contentDescription = "Save rename",
                      tint = CutestarPrimary,
                    )
                  }
                }
              } else {
                // View row
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                  ) {
                    IconButton(
                      onClick = { labelToDelete = label },
                      modifier = Modifier.size(36.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete label",
                        tint = MaterialTheme.colorScheme.error,
                      )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = label,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                  }
                  IconButton(
                    onClick = {
                      editingLabelName = label
                      editedText = label
                    },
                    modifier = Modifier.size(36.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Edit,
                      contentDescription = "Rename label",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = CutestarPrimary),
        modifier = Modifier.testTag("done_edit_labels_btn"),
      ) {
        Text("Done")
      }
    },
  )

  // Confirmation dialog before deleting label
  if (labelToDelete != null) {
    AlertDialog(
      onDismissRequest = { labelToDelete = null },
      title = { Text("Delete label?") },
      text = {
        Text("We’ll delete this label and remove it from all your Cutestar notes. Your notes won't be deleted.")
      },
      confirmButton = {
        Button(
          onClick = {
            labelToDelete?.let { onDeleteLabel(it) }
            labelToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { labelToDelete = null }) {
          Text("Cancel")
        }
      },
    )
  }
}
