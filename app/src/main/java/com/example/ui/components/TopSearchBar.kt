package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.LayoutMode
import com.example.ui.theme.CutestarPrimary

import com.example.model.UserProfile

@Composable
fun TopSearchBar(
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  layoutMode: LayoutMode,
  onToggleLayoutMode: () -> Unit,
  onOpenDrawer: () -> Unit,
  userProfile: UserProfile? = null,
  onAvatarClick: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val focusManager = LocalFocusManager.current

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .shadow(elevation = 3.dp, shape = CircleShape)
      .testTag("top_search_bar"),
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surfaceVariant,
    tonalElevation = 2.dp,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .padding(horizontal = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Navigation Drawer toggle
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

      // Search input field
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

      // Clear search button
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

      // Grid / Single-column toggle button
      IconButton(
        onClick = onToggleLayoutMode,
        modifier = Modifier
          .size(48.dp)
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

      // User Avatar badge
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
