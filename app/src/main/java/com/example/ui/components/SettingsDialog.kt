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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.model.UserProfile
import com.example.ui.theme.CutestarPrimary

@Composable
fun SettingsDialog(
  moveCheckedToBottom: Boolean,
  onToggleMoveCheckedToBottom: (Boolean) -> Unit,
  currentLanguage: String,
  onSelectLanguage: (String) -> Unit,
  onExportAllNotes: () -> Unit,
  onExportNotesToLocalStorage: () -> Unit,
  onImportNotesFromLocalStorage: () -> Unit,
  onImportTextNotes: () -> Unit,
  onImportImageNote: () -> Unit,
  userProfile: UserProfile?,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.testTag("settings_dialog"),
    title = {
      Text(
        text = stringResource(R.string.nav_settings),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
      ) {
        // LANGUAGE SECTION
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.Language,
            contentDescription = null,
            tint = CutestarPrimary,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.language_setting),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // QUICK TOGGLE BETWEEN JAPANESE AND CHINESE
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Switch: 日本語 ⇄ 中文",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                  text = if (currentLanguage == "ja") "Selected: 日本語 (Japanese)" else if (currentLanguage == "zh") "Selected: 中文 (Chinese)" else "Toggle between JA and ZH",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = currentLanguage == "ja",
                onCheckedChange = { isJa ->
                  onSelectLanguage(if (isJa) "ja" else "zh")
                },
                colors = SwitchDefaults.colors(checkedThumbColor = CutestarPrimary),
                modifier = Modifier.testTag("language_toggle_switch"),
              )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Button(
                onClick = { onSelectLanguage("ja") },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (currentLanguage == "ja") CutestarPrimary else MaterialTheme.colorScheme.surface,
                  contentColor = if (currentLanguage == "ja") Color.White else MaterialTheme.colorScheme.onSurface,
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("toggle_lang_ja_btn"),
              ) {
                Text("日本語 (JA)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }
              Button(
                onClick = { onSelectLanguage("zh") },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (currentLanguage == "zh") CutestarPrimary else MaterialTheme.colorScheme.surface,
                  contentColor = if (currentLanguage == "zh") Color.White else MaterialTheme.colorScheme.onSurface,
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("toggle_lang_zh_btn"),
              ) {
                Text("中文 (ZH)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val languages = listOf(
          "system" to "System default (系统默认 / システムデフォルト)",
          "en" to "English (US)",
          "zh" to "中文 (简体中文)",
          "ja" to "日本語 (Japanese)",
        )

        languages.forEach { (code, label) ->
          val isSelected = currentLanguage == code
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelectLanguage(code) }
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = isSelected,
              onClick = { onSelectLanguage(code) },
              colors = RadioButtonDefaults.colors(selectedColor = CutestarPrimary),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = label,
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              ),
              color = if (isSelected) CutestarPrimary else MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(10.dp))

        // IMPORT & EXPORT SECTION
        Text(
          text = "DATA & IMPORT / EXPORT",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
          color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Export to Local Storage
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExportNotesToLocalStorage)
            .padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Outlined.FileUpload,
            contentDescription = null,
            tint = CutestarPrimary,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Export Backup to Local Storage",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            )
            Text(
              text = "Save backup file to device storage",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // Import from Local Storage
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onImportNotesFromLocalStorage)
            .padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Outlined.FileDownload,
            contentDescription = null,
            tint = CutestarPrimary,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Import Backup from Local Storage",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            )
            Text(
              text = "Restore notes from local storage backup file",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // Export All Notes
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExportAllNotes)
            .padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Outlined.FileUpload,
            contentDescription = null,
            tint = CutestarPrimary,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Export All Notes (Backup)",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            )
            Text(
              text = "Save as JSON / text backup file",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // Import Text Notes
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onImportTextNotes)
            .padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Outlined.FileDownload,
            contentDescription = null,
            tint = CutestarPrimary,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = stringResource(R.string.import_text),
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            )
            Text(
              text = "Import notes from .txt or .json files",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // Import Image Note
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onImportImageNote)
            .padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Outlined.Image,
            contentDescription = null,
            tint = CutestarPrimary,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = stringResource(R.string.import_image),
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            )
            Text(
              text = "Attach photo/image into a new note",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(10.dp))

        // CHECKLISTS SECTION
        Text(
          text = "CHECKLISTS",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
          color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Move checked items to bottom",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            )
            Text(
              text = "Completed tasks automatically sort to the bottom section",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = moveCheckedToBottom,
            onCheckedChange = onToggleMoveCheckedToBottom,
            colors = SwitchDefaults.colors(checkedThumbColor = CutestarPrimary),
            modifier = Modifier.testTag("move_checked_bottom_switch"),
          )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "CLOUD & SYNC",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
          color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = if (userProfile?.userId == "guest" || userProfile == null) {
            "Status: Local Storage Mode (Offline-first)"
          } else {
            "Status: Synchronized with Google Cloud (${userProfile.email})"
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "ABOUT",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
          color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Cutestar v2.5 • Compatible with Android 5 to 17\nMultilingual (EN, 中文, 日本語) • Calendar & Alarm Integration",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = CutestarPrimary),
        modifier = Modifier.testTag("close_settings_btn"),
      ) {
        Text("Done")
      }
    },
  )
}
