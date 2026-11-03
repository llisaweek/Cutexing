package com.example.data

import com.example.model.ChecklistItem
import com.example.model.NoteColor
import com.example.model.NoteItem

object SampleData {
  val defaultLabels = listOf("Inspiration", "Personal", "Work", "Projects", "Shopping")

  val initialNotes: List<NoteItem> = listOf(
    NoteItem(
      id = "note-1",
      title = "Welcome to Cutestar ★",
      content = "Your cute, lightweight note-taking sanctuary inspired by Google Keep. Organize your ideas, color-code notes, pin your top priorities, and switch smoothly between grid and list views!",
      color = NoteColor.SAND,
      isPinned = true,
      labels = listOf("Inspiration", "Projects"),
    ),
    NoteItem(
      id = "note-2",
      title = "Trip to Kyoto ⛩️",
      content = "",
      color = NoteColor.MINT,
      isPinned = true,
      labels = listOf("Personal"),
      checklist = listOf(
        ChecklistItem("c1", "Pack universal power adapter", true),
        ChecklistItem("c2", "Collect JR rail pass voucher", true),
        ChecklistItem("c3", "Reserve tea ceremony at Gion", false),
        ChecklistItem("c4", "Visit Fushimi Inari at sunrise", false),
      ),
      reminder = "Tomorrow, 8:00 AM",
    ),
    NoteItem(
      id = "note-3",
      title = "Dessert Recipe Ideas 🧁",
      content = "• Yuzu lemon macarons with white chocolate ganache\n• Fluffy Japanese matcha roll cake\n• Strawberry shortcake with fresh mint drizzle",
      color = NoteColor.PEACH,
      isPinned = false,
      labels = listOf("Personal", "Inspiration"),
    ),
    NoteItem(
      id = "note-4",
      title = "Sprint Planning Highlights",
      content = "1. Polish Material 3 layout structure\n2. Integrate pastel theme palettes\n3. Ensure smooth gesture drawer navigation\n4. Benchmark staggered grid performance",
      color = NoteColor.FOG,
      isPinned = false,
      labels = listOf("Work"),
    ),
    NoteItem(
      id = "note-5",
      title = "Weekend Reading List 📚",
      content = "Design Systems for Android, The Psychology of Everyday Things, and atomic habit tracking notes.",
      color = NoteColor.DUSK,
      isPinned = false,
      labels = listOf("Inspiration"),
    ),
    NoteItem(
      id = "note-6",
      title = "Grocery Run",
      content = "",
      color = NoteColor.SAGE,
      isPinned = false,
      labels = listOf("Shopping"),
      checklist = listOf(
        ChecklistItem("g1", "Almond milk (unsweetened)", false),
        ChecklistItem("g2", "Organic avocados & lime", true),
        ChecklistItem("g3", "Whole bean dark roast coffee", false),
        ChecklistItem("g4", "Sourdough bread", false),
      ),
    ),
    NoteItem(
      id = "note-7",
      title = "Color Harmony Notes",
      content = "Pastel tones reduce cognitive overload during long note-taking sessions while preserving quick category scanning.",
      color = NoteColor.CORAL,
      isPinned = false,
      labels = listOf("Projects"),
    ),
    NoteItem(
      id = "note-8",
      title = "Archived Memo",
      content = "An old draft stored safely away for reference.",
      color = NoteColor.CLAY,
      isPinned = false,
      isArchived = true,
      labels = listOf("Personal"),
    ),
  )
}
