package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.NoteItem
import com.example.ui.theme.CutestarPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarView(
  notes: List<NoteItem>,
  onNoteClick: (NoteItem) -> Unit,
  onCreateNoteForDate: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val todayCal = remember { Calendar.getInstance() }
  var currentYear by remember { mutableIntStateOf(todayCal.get(Calendar.YEAR)) }
  var currentMonth by remember { mutableIntStateOf(todayCal.get(Calendar.MONTH)) }

  // Normalize selected date to midnight ms
  var selectedDateMillis by remember {
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    mutableLongStateOf(cal.timeInMillis)
  }

  // Month navigation helpers
  fun goToPreviousMonth() {
    if (currentMonth == 0) {
      currentMonth = 11
      currentYear -= 1
    } else {
      currentMonth -= 1
    }
  }

  fun goToNextMonth() {
    if (currentMonth == 11) {
      currentMonth = 0
      currentYear += 1
    } else {
      currentMonth += 1
    }
  }

  fun goToToday() {
    val now = Calendar.getInstance()
    currentYear = now.get(Calendar.YEAR)
    currentMonth = now.get(Calendar.MONTH)
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    selectedDateMillis = cal.timeInMillis
  }

  // Calculate days in current month
  val monthCal = remember(currentYear, currentMonth) {
    Calendar.getInstance().apply {
      set(Calendar.YEAR, currentYear)
      set(Calendar.MONTH, currentMonth)
      set(Calendar.DAY_OF_MONTH, 1)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
  }

  val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
  val firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK) - 1 // 0-based from Sunday

  val monthTitle = remember(currentYear, currentMonth) {
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthCal.time)
  }

  // Group notes by normalized day
  val notesByDay = remember(notes) {
    val map = mutableMapOf<String, MutableList<NoteItem>>()
    val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    notes.filter { !it.isDeleted }.forEach { note ->
      val targetTime = note.calendarDate ?: note.updatedAt
      val key = dayFormat.format(Date(targetTime))
      map.getOrPut(key) { mutableListOf() }.add(note)
    }
    map
  }

  val selectedDateStr = remember(selectedDateMillis) {
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selectedDateMillis))
  }

  val selectedDateDisplay = remember(selectedDateMillis) {
    SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
  }

  val notesForSelectedDate = notesByDay[selectedDateStr] ?: emptyList()

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
    ) {
      // Calendar Month Header & Navigation
      item {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.CalendarToday,
              contentDescription = null,
              tint = CutestarPrimary,
              modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = monthTitle,
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
              onClick = { goToToday() },
              modifier = Modifier.testTag("calendar_today_btn"),
            ) {
              Text("Today", color = CutestarPrimary, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = { goToPreviousMonth() }) {
              Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
            }
            IconButton(onClick = { goToNextMonth() }) {
              Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
            }
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      // Day of Week Headers (Sun, Mon, Tue, Wed, Thu, Fri, Sat)
      item {
        val weekDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        Row(modifier = Modifier.fillMaxWidth()) {
          weekDays.forEach { day ->
            Text(
              text = day,
              modifier = Modifier.weight(1f),
              textAlign = TextAlign.Center,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
      }

      // Month Grid
      item {
        val totalCells = (firstDayOfWeek + daysInMonth + 6) / 7 * 7
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dayFormat.format(Date())

        Column(modifier = Modifier.fillMaxWidth()) {
          for (row in 0 until (totalCells / 7)) {
            Row(modifier = Modifier.fillMaxWidth()) {
              for (col in 0 until 7) {
                val index = row * 7 + col
                val dayNumber = index - firstDayOfWeek + 1

                if (dayNumber in 1..daysInMonth) {
                  val cellCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, currentMonth)
                    set(Calendar.DAY_OF_MONTH, dayNumber)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                  }
                  val cellDateMillis = cellCal.timeInMillis
                  val cellDateStr = dayFormat.format(Date(cellDateMillis))
                  val isSelected = cellDateStr == selectedDateStr
                  val isToday = cellDateStr == todayStr
                  val notesOnThisDay = notesByDay[cellDateStr] ?: emptyList()

                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .aspectRatio(1f)
                      .padding(2.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .background(
                        when {
                          isSelected -> CutestarPrimary
                          isToday -> CutestarPrimary.copy(alpha = 0.15f)
                          else -> Color.Transparent
                        }
                      )
                      .clickable { selectedDateMillis = cellDateMillis },
                    contentAlignment = Alignment.Center,
                  ) {
                    Column(
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.Center,
                    ) {
                      Text(
                        text = "$dayNumber",
                        style = MaterialTheme.typography.bodyMedium.copy(
                          fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                          fontSize = 14.sp,
                        ),
                        color = when {
                          isSelected -> Color.White
                          isToday -> CutestarPrimary
                          else -> MaterialTheme.colorScheme.onSurface
                        },
                      )

                      // Indicator dot if notes exist on this day
                      if (notesOnThisDay.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                          modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                              if (isSelected) Color.White else CutestarPrimary
                            ),
                        )
                      }
                    }
                  }
                } else {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))
      }

      // Selected Date Notes Section Header
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column {
            Text(
              text = selectedDateDisplay,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "${notesForSelectedDate.size} note${if (notesForSelectedDate.size == 1) "" else "s"}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Button(
            onClick = { onCreateNoteForDate(selectedDateMillis) },
            colors = ButtonDefaults.buttonColors(containerColor = CutestarPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("add_note_for_date_btn"),
          ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.add_note_for_date), fontSize = 12.sp)
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      // Notes List for selected date
      if (notesForSelectedDate.isEmpty()) {
        item {
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 24.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Icon(
                imageVector = Icons.Outlined.EventNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp),
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = stringResource(R.string.no_notes_for_date),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(12.dp))
              Button(
                onClick = { onCreateNoteForDate(selectedDateMillis) },
                colors = ButtonDefaults.buttonColors(containerColor = CutestarPrimary.copy(alpha = 0.9f)),
              ) {
                Text(stringResource(R.string.add_note_for_date))
              }
            }
          }
        }
      } else {
        items(notesForSelectedDate, key = { it.id }) { note ->
          Box(modifier = Modifier.padding(vertical = 4.dp)) {
            NoteCard(
              note = note,
              isSelected = false,
              isSelectionModeActive = false,
              onClick = { onNoteClick(note) },
              onLongClick = {},
              onTogglePin = {},
              onDrawingClick = { onNoteClick(note) },
              onToggleChecklistItem = { _, _ -> },
            )
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(80.dp))
      }
    }
  }
}
