package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.LocalNotesStorage

class CutestarBootReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
      intent.action == "android.intent.action.QUICKBOOT_POWERON"
    ) {
      Log.d("CutestarBootReceiver", "Device reboot detected, restoring active alarms...")
      try {
        val storage = LocalNotesStorage(context)
        val notes = storage.loadNotes()
        val now = System.currentTimeMillis()
        notes.forEach { note ->
          val alarmTime = note.alarmTime
          if (alarmTime != null && alarmTime > now && !note.isDeleted && !note.isArchived) {
            CutestarAlarmScheduler.scheduleAlarm(
              context = context,
              noteId = note.id,
              title = note.title,
              content = note.content,
              triggerAtMillis = alarmTime,
            )
          }
        }
      } catch (e: Exception) {
        Log.e("CutestarBootReceiver", "Error restoring alarms on boot: ${e.message}", e)
      }
    }
  }
}
