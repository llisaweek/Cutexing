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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
fun LabelsPickerDialog(
  allLabels: List<String>,
  assignedLabels: List<String>,
  onCreateNewLabel: (String) -> Unit,
  onApplyLabels: (List<String>) -> Unit,
  onDismiss: () -> Unit,
) {
  val selected = remember { mutableStateListOf(*assignedLabels.toTypedArray()) }
  var newLabelInput by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.testTag("labels_picker_dialog"),
    title = {
      Text(
        text = "Note labels",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Quick create new label
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = newLabelInput,
            onValueChange = { newLabelInput = it },
            placeholder = { Text("Enter label name") },
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("new_label_picker_input"),
          )
          Spacer(modifier = Modifier.width(6.dp))
          IconButton(
            onClick = {
              val trimmed = newLabelInput.trim()
              if (trimmed.isNotEmpty()) {
                onCreateNewLabel(trimmed)
                if (!selected.contains(trimmed)) {
                  selected.add(trimmed)
                }
                newLabelInput = ""
              }
            },
            modifier = Modifier.testTag("add_label_picker_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Add label",
              tint = CutestarPrimary,
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        if (allLabels.isEmpty() && selected.isEmpty()) {
          Text(
            text = "No labels created yet. Type above to create one!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp),
          )
        } else {
          val distinctLabels = (allLabels + selected).distinct()
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(200.dp),
          ) {
            items(distinctLabels) { label ->
              val isChecked = selected.contains(label)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    if (isChecked) selected.remove(label) else selected.add(label)
                  }
                  .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Checkbox(
                  checked = isChecked,
                  onCheckedChange = { checked ->
                    if (checked) selected.add(label) else selected.remove(label)
                  },
                  colors = CheckboxDefaults.colors(checkedColor = CutestarPrimary),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                  imageVector = Icons.Outlined.Label,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = label,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onApplyLabels(selected.toList())
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = CutestarPrimary),
        modifier = Modifier.testTag("apply_labels_button"),
      ) {
        Text("Done")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}
