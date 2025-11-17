// File: NotificationViewModel.kt
package com.example.notio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.notio.MainActivity.Companion.AppContext
import kotlinx.coroutines.launch

// This ViewModel is now much simpler and cleaner. I think you'll like this new version.
// Its only job now is to talk to the database and tell our "worker" (the NotificationReceiver)
// when to start or stop the alarm process.
class NotificationViewModel : ViewModel() {

    // This part is all the same. We still need our database connection.
    private val db by lazy {
        Room.databaseBuilder(
            AppContext,
            NotificationDatabase::class.java,
            "notio.db"
        ).build()
    }
    private val dao = db.dao()

    // And we still need the live stream of notifications for our UI.
    val notifications = dao.getAll()

    // This is the function that gets called when the user hits "Add".
    fun add(task: String, time: String) = viewModelScope.launch {
        // First, we still save the new notification to the database to get its unique ID.
        val newId = dao.insert(Notification(task = task, time = time))
        val newNotification = Notification(id = newId, task = task, time = time)

        // NOW, instead of having its own scheduling logic, the ViewModel just calls the public
        // "START" function in our NotificationReceiver. It's like telling your worker, "Go do your job."
        NotificationReceiver.scheduleNotification(AppContext, newNotification)
    }

    // This function runs when the user taps the delete icon. This is our "STOP" button.
    fun delete(item: Notification) = viewModelScope.launch {
        // 1. We call the public "STOP" function in our NotificationReceiver.
        // I remember we made this function smart enough to both cancel the future alarm AND
        // remove the pinned notification from the user's screen.
        NotificationReceiver.cancelNotification(AppContext, item)

        // 2. After the alarm and notification are gone, we delete the item from the database.
        // This makes it disappear from the main list in the app.
        dao.delete(item)
    }

    // And the best part: the old, long, private `scheduleNotification` and `cancelNotification` functions
    // are completely gone from this file. It's so much cleaner now. The ViewModel doesn't need to know
    // the details anymore, it just gives orders.
}