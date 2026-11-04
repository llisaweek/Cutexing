package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object CutestarAlarmScheduler {

  private const val TAG = "CutestarAlarmScheduler"

  fun scheduleAlarm(
    context: Context,
    noteId: String,
    title: String,
    content: String,
    triggerAtMillis: Long,
  ) {
    if (triggerAtMillis <= System.currentTimeMillis()) {
      Log.w(TAG, "Cannot schedule alarm in the past: $triggerAtMillis")
      return
    }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, CutestarAlarmReceiver::class.java).apply {
      action = CutestarAlarmReceiver.ACTION_ALARM
      putExtra(CutestarAlarmReceiver.EXTRA_NOTE_ID, noteId)
      putExtra(CutestarAlarmReceiver.EXTRA_TITLE, title)
      putExtra(CutestarAlarmReceiver.EXTRA_CONTENT, content)
    }

    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      noteId.hashCode(),
      intent,
      flags
    )

    try {
      when {
        // Android 12+ (API 31+) exact alarm check
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
          if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
              AlarmManager.RTC_WAKEUP,
              triggerAtMillis,
              pendingIntent
            )
          } else {
            alarmManager.setAndAllowWhileIdle(
              AlarmManager.RTC_WAKEUP,
              triggerAtMillis,
              pendingIntent
            )
          }
        }
        // Android 6.0+ (API 23+)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
          alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
          )
        }
        // Android 5.0 (API 21+)
        else -> {
          alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
          )
        }
      }
      Log.d(TAG, "Alarm successfully scheduled for note $noteId at $triggerAtMillis")
    } catch (e: SecurityException) {
      Log.w(TAG, "SecurityException scheduling exact alarm, falling back to inexact: ${e.message}")
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
          )
        } else {
          alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
          )
        }
      } catch (fallbackError: Exception) {
        Log.e(TAG, "Failed fallback alarm scheduling: ${fallbackError.message}")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to schedule alarm: ${e.message}", e)
    }
  }

  fun cancelAlarm(context: Context, noteId: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, CutestarAlarmReceiver::class.java).apply {
      action = CutestarAlarmReceiver.ACTION_ALARM
      putExtra(CutestarAlarmReceiver.EXTRA_NOTE_ID, noteId)
    }

    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      noteId.hashCode(),
      intent,
      flags
    )

    try {
      alarmManager.cancel(pendingIntent)
      pendingIntent.cancel()
      Log.d(TAG, "Alarm cancelled for note $noteId")
    } catch (e: Exception) {
      Log.w(TAG, "Error cancelling alarm: ${e.message}")
    }
  }
}
