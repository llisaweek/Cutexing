package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class NoteColor(
  val displayName: String,
  val lightColor: Color,
  val darkColor: Color,
  val lightBorder: Color,
  val darkBorder: Color,
) {
  DEFAULT(
    displayName = "Default",
    lightColor = NoteColorDefaultLight,
    darkColor = NoteColorDefaultDark,
    lightBorder = Color(0xFFE0E0E0),
    darkBorder = Color(0xFF3C4043),
  ),
  SAND(
    displayName = "Sand",
    lightColor = NoteColorSandLight,
    darkColor = NoteColorSandDark,
    lightBorder = Color(0xFFFFF59D),
    darkBorder = Color(0xFF7A7320),
  ),
  PEACH(
    displayName = "Peach",
    lightColor = NoteColorPeachLight,
    darkColor = NoteColorPeachDark,
    lightBorder = Color(0xFFFFCC80),
    darkBorder = Color(0xFF7A5C20),
  ),
  CORAL(
    displayName = "Coral",
    lightColor = NoteColorCoralLight,
    darkColor = NoteColorCoralDark,
    lightBorder = Color(0xFFEF9A9A),
    darkBorder = Color(0xFF7A3936),
  ),
  BLOSSOM(
    displayName = "Blossom",
    lightColor = NoteColorBlossomLight,
    darkColor = NoteColorBlossomDark,
    lightBorder = Color(0xFFF48FB1),
    darkBorder = Color(0xFF732E54),
  ),
  DUSK(
    displayName = "Dusk",
    lightColor = NoteColorDuskLight,
    darkColor = NoteColorDuskDark,
    lightBorder = Color(0xFFB39DDB),
    darkBorder = Color(0xFF573D85),
  ),
  FOG(
    displayName = "Fog",
    lightColor = NoteColorFogLight,
    darkColor = NoteColorFogDark,
    lightBorder = Color(0xFF90CAF9),
    darkBorder = Color(0xFF2C4A78),
  ),
  SAGE(
    displayName = "Sage",
    lightColor = NoteColorSageLight,
    darkColor = NoteColorSageDark,
    lightBorder = Color(0xFF80CBC4),
    darkBorder = Color(0xFF275E58),
  ),
  MINT(
    displayName = "Mint",
    lightColor = NoteColorMintLight,
    darkColor = NoteColorMintDark,
    lightBorder = Color(0xFFA5D6A7),
    darkBorder = Color(0xFF2D5F37),
  ),
  CLAY(
    displayName = "Clay",
    lightColor = NoteColorClayLight,
    darkColor = NoteColorClayDark,
    lightBorder = Color(0xFFBCAAA4),
    darkBorder = Color(0xFF63433C),
  );

  fun surfaceColor(darkTheme: Boolean): Color = if (darkTheme) darkColor else lightColor
  fun borderColor(darkTheme: Boolean): Color = if (darkTheme) darkBorder else lightBorder
}

data class ChecklistItem(
  val id: String,
  val text: String,
  val isChecked: Boolean = false,
)

data class NoteItem(
  val id: String,
  val title: String,
  val content: String,
  val color: NoteColor = NoteColor.DEFAULT,
  val isPinned: Boolean = false,
  val isArchived: Boolean = false,
  val isDeleted: Boolean = false,
  val labels: List<String> = emptyList(),
  val checklist: List<ChecklistItem> = emptyList(),
  val reminder: String? = null,
  val drawingUrl: String? = null,
  val drawingStoragePath: String? = null,
  val imageUrl: String? = null,
  val imageStoragePath: String? = null,
  val audioUrl: String? = null,
  val updatedAt: Long = System.currentTimeMillis(),
)

sealed interface NavigationDestination {
  data object Notes : NavigationDestination
  data object Reminders : NavigationDestination
  data class Label(val name: String) : NavigationDestination
  data object Archive : NavigationDestination
  data object Trash : NavigationDestination
  data object Settings : NavigationDestination
}

enum class LayoutMode {
  STAGGERED_GRID,
  SINGLE_COLUMN,
}
