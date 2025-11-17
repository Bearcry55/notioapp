// File: BootCompletedReceiver.kt
package com.example.notio

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

// So, this whole file is a super important one. Its only job is to fix that annoying bug
// where all our scheduled notifications would disappear if the user restarted their phone.
class BootCompletedReceiver : BroadcastReceiver() {

    // This `onReceive` function is the entry point. The Android system calls this function
    // automatically when it has a message for us that we're listening for.
    override fun onReceive(context: Context, intent: Intent) {

        // We're only interested in one specific system message: `ACTION_BOOT_COMPLETED`.
        // So, the first thing we do is check if that's the message we just received.
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {

            // Okay, the phone just finished booting up. Now we need to read our database and
            // re-schedule all the alarms. That's a "slow" operation, so we can't do it on the
            // main thread or the system might shut us down. So, we launch a coroutine
            // on a background thread to do all the heavy lifting.
            CoroutineScope(Dispatchers.IO).launch {
                // Since this receiver is kind of its own little program running in the background,
                // it doesn't have access to the ViewModel. We have to create our own temporary
                // connection to the database right here.
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    NotificationDatabase::class.java,
                    "notio.db"
                ).build()
                val dao = db.dao()

                // Now we use that database connection to get a list of all our saved notifications.
                // We're using `getAllOnce()` because we just need a snapshot of the data, not a live stream.
                val notifications = dao.getAllOnce()

                // And finally, we just loop through every notification we found...
                notifications.forEach { notification ->
                    // ...and call our helper function to re-schedule the alarm for each one.
                    // This is what brings all our reminders back to life.
                    scheduleNotification(context, notification)
                }
            }
        }
    }

    // This helper function is a copy of the scheduling logic we had in the ViewModel.
    // I think keeping it here makes this file totally self-contained, which is pretty neat.
    // It has everything it needs to re-schedule an alarm all by itself.
    private fun scheduleNotification(context: Context, notification: Notification) {
        // Let's get the Android system's alarm clock service.
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // We pack up the notification's details into an 'Intent' message.
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("task", notification.task)
            putExtra("id", notification.id.toInt())
        }

        // We create the special 'key' (PendingIntent) that the AlarmManager will use to
        // wake up our `NotificationReceiver` when the time is right.
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notification.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Just like before, we have to calculate the exact trigger time from our saved time string.
        val calendar = Calendar.getInstance().apply {
            val parts = notification.time.split(":", " ")
            val hour = parts[0].toInt(); val minute = parts[1].toInt(); val ampm = parts[2]
            var hourOfDay = if (ampm.equals("PM", ignoreCase = true) && hour < 12) hour + 12 else if (ampm.equals("AM", ignoreCase = true) && hour == 12) 0 else hour
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // This check is still important, even after a reboot. If an alarm was for a time that has
            // already passed today, it should be set for tomorrow.
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        // This `try-catch` block is maybe even more important here than in the ViewModel.
        // After a reboot, the app is in the background, and the system is VERY strict about exact alarms.
        // If our app doesn't have the special permission, this would normally crash. This safety net
        // just catches the error and moves on, which is better than nothing.
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // If we get here, it means the alarm wasn't set. We're just printing the error for debugging.
            e.printStackTrace()
        }
    }
}