package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.LayoutMode
import com.example.model.NoteColor
import com.example.model.UserProfile
import com.example.ui.theme.CutestarPrimary

@Composable
fun TopSearchBar(
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  layoutMode: LayoutMode,
  onToggleLayoutMode: () -> Unit,
  onOpenDrawer: () -> Unit,
  selectedCount: Int = 0,
  onClearSelection: () -> Unit = {},
  onPinSelected: () -> Unit = {},
  onArchiveSelected: () -> Unit = {},
  onTrashSelected: () -> Unit = {},
  onSelectAll: () -> Unit = {},
  onSetColorSelected: (NoteColor) -> Unit = {},
  userProfile: UserProfile? = null,
  onAvatarClick: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val focusManager = LocalFocusManager.current
  val isSelectionMode = selectedCount > 0
  var showColorMenu by remember { mutableStateOf(false) }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .shadow(elevation = if (isSelectionMode) 5.dp else 2.dp, shape = CircleShape)
      .testTag("top_search_bar"),
    shape = CircleShape,
    color = if (isSelectionMode) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant,
    tonalElevation = if (isSelectionMode) 4.dp else 2.dp,
  ) {
    if (isSelectionMode) {
      // Contextual Action Bar for Multi-Selection (Google Keep signature)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onClearSelection,
            modifier = Modifier.size(44.dp).testTag("clear_selection_btn"),
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Clear selection",
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "$selectedCount selected",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onPinSelected,
            modifier = Modifier.size(40.dp).testTag("pin_selected_btn"),
          ) {
            Icon(
              imageVector = Icons.Default.PushPin,
              contentDescription = "Pin selected",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Box {
            IconButton(
              onClick = { showColorMenu = true },
              modifier = Modifier.size(40.dp).testTag("color_selected_btn"),
            ) {
              Icon(
                imageVector = Icons.Outlined.ColorLens,
                contentDescription = "Color selected",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            DropdownMenu(
              expanded = showColorMenu,
              onDismissRequest = { showColorMenu = false },
            ) {
              NoteColor.entries.forEach { color ->
                DropdownMenuItem(
                  text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Box(
                        modifier = Modifier
                          .size(18.dp)
                          .background(color.lightColor, CircleShape),
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(color.displayName)
                    }
                  },
                  onClick = {
                    showColorMenu = false
                    onSetColorSelected(color)
                  },
                )
              }
            }
          }

          IconButton(
            onClick = onArchiveSelected,
            modifier = Modifier.size(40.dp).testTag("archive_selected_btn"),
          ) {
            Icon(
              imageVector = Icons.Default.Archive,
              contentDescription = "Archive selected",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          IconButton(
            onClick = onTrashSelected,
            modifier = Modifier.size(40.dp).testTag("trash_selected_btn"),
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Trash selected",
              tint = MaterialTheme.colorScheme.error,
            )
          }

          IconButton(
            onClick = onSelectAll,
            modifier = Modifier.size(40.dp).testTag("select_all_btn"),
          ) {
            Icon(
              imageVector = Icons.Default.SelectAll,
              contentDescription = "Select all",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      // Standard Google Keep Search Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = onOpenDrawer,
          modifier = Modifier
            .size(48.dp)
            .testTag("drawer_menu_button"),
        ) {
          Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = stringResource(R.string.drawer_open),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 8.dp),
          contentAlignment = Alignment.CenterStart,
        ) {
          if (searchQuery.isEmpty()) {
            Text(
              text = stringResource(R.string.search_notes),
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
          }
          BasicTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("search_text_input"),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
              color = MaterialTheme.colorScheme.onSurface,
            ),
            singleLine = true,
            cursorBrush = SolidColor(CutestarPrimary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
          )
        }

        AnimatedVisibility(
          visible = searchQuery.isNotEmpty(),
          enter = fadeIn(),
          exit = fadeOut(),
        ) {
          IconButton(
            onClick = { onSearchQueryChange("") },
            modifier = Modifier
              .size(40.dp)
              .testTag("clear_search_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Clear search",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        IconButton(
          onClick = onToggleLayoutMode,
          modifier = Modifier
            .size(44.dp)
            .testTag("toggle_layout_button"),
        ) {
          val isGrid = layoutMode == LayoutMode.STAGGERED_GRID
          Icon(
            imageVector = if (isGrid) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
            contentDescription = stringResource(
              if (isGrid) R.string.toggle_list else R.string.toggle_grid,
            ),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Box(
          modifier = Modifier
            .padding(end = 4.dp)
            .clickable(onClick = onAvatarClick)
            .testTag("user_avatar_button"),
          contentAlignment = Alignment.Center,
        ) {
          UserAvatar(
            userProfile = userProfile,
            size = 36.dp,
          )
        }
      }
    }
  }
}
