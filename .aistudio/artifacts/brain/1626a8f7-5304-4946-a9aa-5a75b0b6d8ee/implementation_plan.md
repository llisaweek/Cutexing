# Cutestar - Google Keep Layout Framework

Establish the foundational Material 3 layout architecture for **Cutestar**, modeled after the latest Google Keep design, featuring a pill-shaped top search bar, navigation drawer, dual-column staggered card grid with pastel palette styling, and an interactive bottom quick-note bar.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following layout and design choices were confirmed during Phase 1:

- **Visual Style & Color Palette**: Clean Google Keep aesthetic with soft pastel note cards (pastel amber, coral, blush, lavender, sky, mint, sage, and sand) on a clean neutral background.
- **Note Layout Format**: Staggered dual-column card grid with seamless toggle to single-column list view and a persistent bottom quick-note bar.
- **Navigation Structure**: Material 3 Navigation Drawer (`ModalNavigationDrawer`) housing Notes, Reminders, dynamic Labels, Archive, Trash, and Settings.
- **Future-Proof Architecture**: Clean separation between layout scaffold, state management (`NoteViewModel`), and note data models so full note editing, label management, and persistence can be connected seamlessly in subsequent phases.

---

## 1. Overview & Core Concept

- **What It Does**: Delivers the complete visual skeleton and interactive layout shell of Cutestar. Users can open/close the navigation drawer to filter views (Notes, Reminders, Labels, Archive, Trash), switch between dual-column staggered grid and single-column views, tap quick-action buttons on the bottom bar, and view sample pastel-themed notes with tags and pinned states.
- **Target Audience**: Users seeking a fast, friction-free note-taking and idea-capture experience with the familiarity and ergonomics of Google Keep and the charming identity of Cutestar.
- **Key Value**: Provides an immediately functional, fluid, and responsive Android interface conforming strictly to modern Material 3 and Google Keep ergonomics before adding deep editor features.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Browsing Notes**:
   - The user opens Cutestar and sees the edge-to-edge layout with a top search bar ("Search Cutestar notes").
   - Notes are arranged in a dynamic dual-column staggered grid with distinct note heights, pastel color backgrounds, titles, snippets, tag chips, and pin icons.
   - Tapping the view toggle icon in the top bar animates the layout between staggered dual-column and single-column stream.
2. **Opening Navigation Drawer**:
   - Tapping the drawer menu icon (or swiping from the left edge) reveals the M3 `ModalNavigationDrawer`.
   - The drawer displays the Cutestar header, navigation destinations ("Notes", "Reminders", "Archive", "Trash"), and a dedicated "Labels" category section.
   - Selecting a destination updates the active screen header and filters displayed cards.
3. **Bottom Quick-Note Interaction**:
   - The bottom bar remains anchored above the Android navigation bar safe insets.
   - Features quick-action icons: New List (checkbox), Drawing (brush), Audio Note (mic), and Image Note (photo).
   - Right side features the prominent "+" / "Take a note..." action button.

### Visual Identity & Theme

- **Palette**:
  - Light theme: Clean off-white surface (`#FAF9F6`), primary text `#1F1F1F`, muted text `#5F6368`.
  - Dark theme: Slate/Charcoal background (`#1F1F1F` / `#121212`), high legibility muted tones.
  - Pastel Note Palette: Soft yellow (`#FFF9C4`), peach (`#FFE0B2`), coral (`#FFCDD2`), lavender (`#E1BEE7`), sky blue (`#BBDEFB`), mint (`#C8E6C9`), teal (`#B2DFDB`), and clay/sand (`#D7CCC8`).
- **Typography & Hierarchy**:
  - Title/Header: Google Sans style clean medium weight (`titleLarge`, `titleMedium`).
  - Note Snippets: Clean readable sans-serif (`bodyMedium`, `bodySmall`).
  - Label Chips: Compact rounded pill typography (`labelSmall`).
- **Component Styling & Layout**:
  - Search bar: Pill shape (`CircleShape`) with subtle elevation or border, embedded in the top safe area.
  - Note cards: Rounded corners (`16.dp` radius), gentle border outline (`1.dp` solid or tinted border), variable card heights.
  - Bottom bar: Rounded dock style with elevated surface and generous 48dp touch targets.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Staggered Grid vs. Standard Grid**
  - *Chosen Approach*: `LazyVerticalStaggeredGrid` with `StaggeredGridCells.Fixed(columns)`.
  - *Why*: Google Keep’s signature look relies on masonry-style card packing where notes of different lengths fit naturally without empty vertical gaps.
  - *Alternatives Considered*: Standard uniform grid (causes awkward whitespace) or single-column only (wastes horizontal space on modern high-res screens).
- **Decision 2: Layout Framework Scope in Step 1**
  - *Chosen Approach*: Implement a complete reactive layout shell with sample data, drawer navigation, view toggling, search input state, and click handlers for bottom actions.
  - *Why*: Gives an immediate, highly polished visual foundation matching the latest Google Keep design that can be tested in the emulator right away, ready for subsequent steps.

---

## 4. Technical Architecture & Data Strategy

### Layout & Component Hierarchy

```
┌─────────────────────────────────────────────────────────────┐
│                    Cutestar Scaffold                        │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Top App Bar / Search Box (Pill Container)             │  │
│  │ [☰ Drawer]   "Search Cutestar notes"   [▦ Toggle] [★] │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Main Content: LazyVerticalStaggeredGrid               │  │
│  │                                                       │  │
│  │  ┌──────────────┐  ┌──────────────┐                   │  │
│  │  │ Pinned Note  │  │ Note Card 2  │                   │  │
│  │  │ (Pastel Mint)│  │ (Pastel Peach)│                  │  │
│  │  └──────────────┘  │              │                   │  │
│  │  ┌──────────────┐  │  • Checklist │                   │  │
│  │  │ Note Card 3  │  └──────────────┘                   │  │
│  │  │ (Lavender)   │  ┌──────────────┐                   │  │
│  │  └──────────────┘  │ Note Card 4  │                   │  │
│  │                    └──────────────┘                   │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Bottom Quick-Note Bar                                 │  │
│  │ [☑ List]  [✎ Draw]  [🎙 Audio]  [🖼 Photo]    [+ Note] │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Navigation Drawer Structure

```
┌──────────────────────────────┐
│ Cutestar ★                   │
├──────────────────────────────┤
│ 📝  Notes          (Active)  │
│ 🔔  Reminders                │
├──────────────────────────────┤
│ LABELS                       │
│ 🏷️  Inspiration              │
│ 🏷️  Personal                 │
│ 🏷️  Work                     │
│ ➕  Create new label         │
├──────────────────────────────┤
│ 📥  Archive                  │
│ 🗑️  Trash                    │
├──────────────────────────────┤
│ ⚙️  Settings                 │
│ ❓  Help & feedback          │
└──────────────────────────────┘
```

### Core Data Models & State

- **`NoteItem`**:
  - `id: String`
  - `title: String`
  - `content: String`
  - `color: NoteColor` (Pastel enum with surface & onSurface tokens)
  - `isPinned: Boolean`
  - `isArchived: Boolean`
  - `labels: List<String>`
  - `checklist: List<ChecklistItem>` (optional preview checklist)
  - `updatedAt: Long`
- **`NavigationDestination`**:
  - `Notes`, `Reminders`, `Label(name)`, `Archive`, `Trash`
- **`LayoutMode`**:
  - `Grid` (2 columns staggered) vs `List` (1 column)

### Interactive Handlers & State Transitions

- **Drawer State**: Controlled by `DrawerState` with coroutine scope for smooth opening and closing.
- **Search Query**: Real-time filtering across titles, body snippets, and label tags.
- **View Toggle**: Animated transition between dual-column and single-column modes.
- **Edge-to-Edge Compliance**: Uses `WindowInsets.statusBars` for the top search area and `WindowInsets.navigationBars` for the bottom bar, ensuring full compatibility with gesture navigation and status bars.
