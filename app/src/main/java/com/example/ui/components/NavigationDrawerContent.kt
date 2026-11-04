package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.NavigationDestination
import com.example.ui.theme.CutestarPrimary

import com.example.model.UserProfile
import androidx.compose.foundation.clickable

@Composable
fun NavigationDrawerContent(
  currentDestination: NavigationDestination,
  labels: List<String>,
  onSelectDestination: (NavigationDestination) -> Unit,
  onCreateLabel: () -> Unit,
  onEditLabels: () -> Unit = {},
  onSettingsClick: () -> Unit = {},
  userProfile: UserProfile? = null,
  onProfileClick: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  ModalDrawerSheet(
    modifier = modifier
      .width(300.dp)
      .fillMaxHeight()
      .testTag("modal_drawer_sheet"),
    drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
    drawerContainerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(vertical = 12.dp),
    ) {
      // Cutestar Branding Header with User Profile / Logo
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = onProfileClick)
          .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (userProfile != null) {
          UserAvatar(
            userProfile = userProfile,
            size = 44.dp,
          )
        } else {
          Image(
            painter = painterResource(R.drawable.ic_cutestar_logo),
            contentDescription = "Cutestar Logo",
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = userProfile?.displayName?.ifBlank { "Cutestar" } ?: "Cutestar",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
          )
          Text(
            text = userProfile?.email?.ifBlank { "Notes & Ideas" } ?: "Notes & Ideas",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Primary navigation items
      NavigationDrawerItem(
        label = { Text(stringResource(R.string.nav_notes)) },
        icon = {
          Icon(
            imageVector = if (currentDestination is NavigationDestination.Notes) {
              Icons.Filled.Lightbulb
            } else {
              Icons.Outlined.Lightbulb
            },
            contentDescription = stringResource(R.string.nav_notes),
          )
        },
        selected = currentDestination is NavigationDestination.Notes,
        onClick = { onSelectDestination(NavigationDestination.Notes) },
        modifier = Modifier
          .padding(NavigationDrawerItemDefaults.ItemPadding)
          .testTag("nav_item_notes"),
        colors = NavigationDrawerItemDefaults.colors(
          selectedContainerColor = CutestarPrimary.copy(alpha = 0.15f),
          selectedIconColor = CutestarPrimary,
          selectedTextColor = CutestarPrimary,
        ),
      )

      NavigationDrawerItem(
        label = { Text(stringResource(R.string.nav_reminders)) },
        icon = {
          Icon(
            imageVector = if (currentDestination is NavigationDestination.Reminders) {
              Icons.Filled.Notifications
            } else {
              Icons.Outlined.Notifications
            },
            contentDescription = stringResource(R.string.nav_reminders),
          )
        },
        selected = currentDestination is NavigationDestination.Reminders,
        onClick = { onSelectDestination(NavigationDestination.Reminders) },
        modifier = Modifier
          .padding(NavigationDrawerItemDefaults.ItemPadding)
          .testTag("nav_item_reminders"),
        colors = NavigationDrawerItemDefaults.colors(
          selectedContainerColor = CutestarPrimary.copy(alpha = 0.15f),
          selectedIconColor = CutestarPrimary,
          selectedTextColor = CutestarPrimary,
        ),
      )

      NavigationDrawerItem(
        label = { Text(stringResource(R.string.nav_calendar)) },
        icon = {
          Icon(
            imageVector = if (currentDestination is NavigationDestination.Calendar) {
              Icons.Filled.CalendarMonth
            } else {
              Icons.Outlined.CalendarMonth
            },
            contentDescription = stringResource(R.string.nav_calendar),
          )
        },
        selected = currentDestination is NavigationDestination.Calendar,
        onClick = { onSelectDestination(NavigationDestination.Calendar) },
        modifier = Modifier
          .padding(NavigationDrawerItemDefaults.ItemPadding)
          .testTag("nav_item_calendar"),
        colors = NavigationDrawerItemDefaults.colors(
          selectedContainerColor = CutestarPrimary.copy(alpha = 0.15f),
          selectedIconColor = CutestarPrimary,
          selectedTextColor = CutestarPrimary,
        ),
      )

      HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
      )

      // Labels Section Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "LABELS",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f),
        )
        TextButton(
          onClick = onEditLabels,
          modifier = Modifier.testTag("edit_labels_button"),
        ) {
          Text(
            text = "Edit",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      // Dynamic Labels
      labels.forEach { labelName ->
        val isSelected = (currentDestination as? NavigationDestination.Label)?.name == labelName
        NavigationDrawerItem(
          label = { Text(labelName) },
          icon = {
            Icon(
              imageVector = if (isSelected) Icons.Filled.Label else Icons.Outlined.Label,
              contentDescription = labelName,
            )
          },
          selected = isSelected,
          onClick = { onSelectDestination(NavigationDestination.Label(labelName)) },
          modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .testTag("nav_item_label_$labelName"),
          colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = CutestarPrimary.copy(alpha = 0.15f),
            selectedIconColor = CutestarPrimary,
            selectedTextColor = CutestarPrimary,
          ),
        )
      }

      // Create new label item
      NavigationDrawerItem(
        label = { Text(stringResource(R.string.nav_create_label)) },
        icon = {
          Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = stringResource(R.string.nav_create_label),
          )
        },
        selected = false,
        onClick = onCreateLabel,
        modifier = Modifier
          .padding(NavigationDrawerItemDefaults.ItemPadding)
          .testTag("nav_item_create_label"),
      )

      HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
      )

      // Archive & Trash
      NavigationDrawerItem(
        label = { Text(stringResource(R.string.nav_archive)) },
        icon = {
          Icon(
            imageVector = if (currentDestination is NavigationDestination.Archive) {
              Icons.Filled.Archive
            } else {
              Icons.Outlined.Archive
            },
            contentDescription = stringResource(R.string.nav_archive),
          )
        },
        selected = currentDestination is NavigationDestination.Archive,
        onClick = { onSelectDestination(NavigationDestination.Archive) },
        modifier = Modifier
          .padding(NavigationDrawerItemDefaults.ItemPadding)
          .testTag("nav_item_archive"),
        colors = NavigationDrawerItemDefaults.colors(
          selectedContainerColor = CutestarPrimary.copy(alpha = 0.15f),
          selectedIconColor = CutestarPrimary,
          selectedTextColor = CutestarPrimary,
        ),
      )

      NavigationDrawerItem(
        label = { Text(stringResource(R.string.nav_trash)) },
        icon = {
          Icon(
            imageVector = if (currentDestination is NavigationDestination.Trash) {
              Icons.Filled.Delete
            } else {
              Icons.Outlined.Delete
            },
            contentDescription = stringResource(R.string.nav_trash),
          )
        },
        selected = currentDestination is NavigationDestination.Trash,
        onClick = { onSelectDestination(NavigationDestination.Trash) },
        modifier = Modifier
          .padding(NavigationDrawerItemDefaults.ItemPadding)
          .testTag("nav_item_trash"),
        colors = NavigationDrawerItemDefaults.colors(
          selectedContainerColor = CutestarPrimary.copy(alpha = 0.15f),
          selectedIconColor = CutestarPrimary,
          selectedTextColor = CutestarPrimary,
        ),
      )

      HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
      )

      // Settings & Feedback
      NavigationDrawerItem(
        label = { Text(stringResource(R.string.nav_settings)) },
        icon = {
          Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = stringResource(R.string.nav_settings),
          )
        },
        selected = currentDestination is NavigationDestination.Settings,
        onClick = onSettingsClick,
        modifier = Modifier
          .padding(NavigationDrawerItemDefaults.ItemPadding)
          .testTag("nav_item_settings"),
      )

      NavigationDrawerItem(
        label = { Text(stringResource(R.string.nav_help)) },
        icon = {
          Icon(
            imageVector = Icons.Outlined.HelpOutline,
            contentDescription = stringResource(R.string.nav_help),
          )
        },
        selected = false,
        onClick = { /* Help & feedback */ },
        modifier = Modifier
          .padding(NavigationDrawerItemDefaults.ItemPadding)
          .testTag("nav_item_help"),
      )
    }
  }
}
