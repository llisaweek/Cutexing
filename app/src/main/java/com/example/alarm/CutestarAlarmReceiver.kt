package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class CutestarAlarmReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action
    val noteId = intent.getStringExtra(EXTRA_NOTE_ID) ?: ""
    val title = intent.getStringExtra(EXTRA_TITLE) ?: context.getString(R.string.alarm_notification_title)
    val content = intent.getStringExtra(EXTRA_CONTENT) ?: ""

    if (action == ACTION_DISMISS) {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val notifId = noteId.hashCode()
      notificationManager.cancel(notifId)
      return
    }

    try {
      showAlarmNotification(context, noteId, title, content)
    } catch (e: Exception) {
      Log.e("CutestarAlarmReceiver", "Failed to display alarm notification: ${e.message}", e)
    }
  }

  private fun showAlarmNotification(context: Context, noteId: String, title: String, content: String) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = CHANNEL_ID

    // Create NotificationChannel on API 26+ (Android 8.0+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      val audioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
        .build()

      val channel = NotificationChannel(
        channelId,
        context.getString(R.string.alarm_notification_title),
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Cutestar scheduled note alarms and reminders"
        enableLights(true)
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 450, 200, 450)
        setSound(alarmSound, audioAttributes)
      }
      notificationManager.createNotificationChannel(channel)
    }

    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    // Launch MainActivity when tapped
    val openIntent = Intent(context, MainActivity::class.java).apply {
      setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
      putExtra(EXTRA_NOTE_ID, noteId)
    }
    val contentPendingIntent = PendingIntent.getActivity(
      context,
      noteId.hashCode(),
      openIntent,
      flags
    )

    // Dismiss Action
    val dismissIntent = Intent(context, CutestarAlarmReceiver::class.java).apply {
      action = ACTION_DISMISS
      putExtra(EXTRA_NOTE_ID, noteId)
    }
    val dismissPendingIntent = PendingIntent.getBroadcast(
      context,
      noteId.hashCode() + 1,
      dismissIntent,
      flags
    )

    val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    val notification = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.drawable.ic_cutestar_icon)
      .setContentTitle(title.ifBlank { context.getString(R.string.alarm_notification_title) })
      .setContentText(content.ifBlank { "You have a scheduled note reminder." })
      .setStyle(NotificationCompat.BigTextStyle().bigText(content))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setAutoCancel(true)
      .setSound(alarmSound)
      .setVibrate(longArrayOf(0, 450, 200, 450))
      .setContentIntent(contentPendingIntent)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
      .setColor(ContextCompat.getColor(context, R.color.ic_cutestar_icon_background))
      .build()

    notificationManager.notify(noteId.hashCode(), notification)
  }

  companion object {
    const val CHANNEL_ID = "cutestar_reminders_channel"
    const val ACTION_ALARM = "com.example.action.CUTESTAR_ALARM"
    const val ACTION_DISMISS = "com.example.action.DISMISS_ALARM"
    const val EXTRA_NOTE_ID = "extra_note_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_CONTENT = "extra_content"
  }
}
