// File: NotificationReceiver.kt
package com.example.notio

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationCompat
import java.util.Calendar

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        when (action) {
            ACTION_SHOW_NOTIFICATION -> {
                // This is the initial alarm or a repeat alarm
                val task = intent.getStringExtra("task") ?: "Reminder"
                val id = intent.getIntExtra("id", 0)
                if (id == 0) return

                // Show the notification
                showNotification(context, task, id)

                // Schedule the next repeat (1 minute later)
                scheduleRepeatNotification(context, task, id)
            }

            ACTION_DISMISS_NOTIFICATION -> {
                // User clicked "Dismiss" button
                val id = intent.getIntExtra("id", 0)
                if (id == 0) return

                // Cancel all future repeats
                cancelAllNotifications(context, id)

                // Dismiss the notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(id)
            }
        }
    }

    private fun showNotification(context: Context, task: String, id: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(context, notificationManager)

        // Intent to open the app when tapped
        val activityIntent = Intent(context, MainActivity::class.java)
        val activityPendingIntent = PendingIntent.getActivity(
            context,
            id,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to dismiss (stop repeating)
        val dismissIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_DISMISS_NOTIFICATION
            putExtra("id", id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            id + 10000, // Different request code
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification with a DISMISS action button
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Notio Reminder")
            .setContentText(task)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(activityPendingIntent)
            .setAutoCancel(false) // Don't dismiss when tapped
            .setOngoing(true) // Make it persistent
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                dismissPendingIntent
            )
            .setSound(null) // No sound for repeats (can change if needed)
            .setVibrate(longArrayOf(500, 500)) // Vibrate pattern
            .build()

        notificationManager.notify(id, notification)
    }

    private fun scheduleRepeatNotification(context: Context, task: String, id: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_SHOW_NOTIFICATION
            putExtra("task", task)
            putExtra("id", id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id + 20000, // Different request code for repeats
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule for 1 minute from now
        val triggerTime = System.currentTimeMillis() + (60 * 1000) // 1 minute

        // Use exact alarm for better timing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            AlarmManagerCompat.setExactAndAllowWhileIdle(
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    companion object {
        const val CHANNEL_ID = "notio_channel_id"
        const val ACTION_SHOW_NOTIFICATION = "com.example.notio.SHOW_NOTIFICATION"
        const val ACTION_DISMISS_NOTIFICATION = "com.example.notio.DISMISS_NOTIFICATION"

        // Schedule the INITIAL notification
        fun scheduleNotification(context: Context, notification: Notification) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_SHOW_NOTIFICATION
                putExtra("task", notification.task)
                putExtra("id", notification.id.toInt())
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notification.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Calculate the initial trigger time
            val calendar = Calendar.getInstance().apply {
                val parts = notification.time.split(":", " ")
                val hour = parts[0].toInt()
                val minute = parts[1].toInt()
                val ampm = parts[2]
                var hourOfDay = if (ampm.equals("PM", ignoreCase = true) && hour < 12) hour + 12
                else if (ampm.equals("AM", ignoreCase = true) && hour == 12) 0
                else hour
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DATE, 1)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                AlarmManagerCompat.setExactAndAllowWhileIdle(
                    alarmManager,
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }

        // Cancel ALL notifications (initial + all repeats)
        fun cancelAllNotifications(context: Context, id: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Cancel the initial alarm
            val initialIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_SHOW_NOTIFICATION
            }
            val initialPendingIntent = PendingIntent.getBroadcast(
                context,
                id,
                initialIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(initialPendingIntent)

            // Cancel the repeat alarm
            val repeatIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_SHOW_NOTIFICATION
            }
            val repeatPendingIntent = PendingIntent.getBroadcast(
                context,
                id + 20000,
                repeatIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(repeatPendingIntent)

            // Remove the notification from the status bar
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(id)
        }

        // Cancel notification (called from ViewModel when user deletes from app)
        fun cancelNotification(context: Context, notification: Notification) {
            cancelAllNotifications(context, notification.id.toInt())
        }

        private fun createNotificationChannel(context: Context, notificationManager: NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "Notio Notifications",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Repeating reminder notifications"
                        enableVibration(true)
                        vibrationPattern = longArrayOf(500, 500)
                    }
                    notificationManager.createNotificationChannel(channel)
                }
            }
        }
    }
}