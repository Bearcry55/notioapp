// File: Notification.kt
package com.example.notio

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Alright, so this `Notification` data class is basically a blueprint.
// It tells our database, "Hey, each notification item we save is going to have these pieces of info."
// It's like defining the columns in a spreadsheet.
@Entity(tableName = "notifications")
data class Notification(
    // The `@PrimaryKey(autoGenerate = true)` part is super useful. It means we don't have to worry about the ID.
    // The database will automatically assign a unique number to every new notification we create.
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val task: String,
    val time: String // I'm just adding a little note here to remind myself what the time format looks like, e.g. "02:00 PM".
)

// This `NotificationDao` is like the control panel for our database.
// The 'Dao' stands for Data Access Object. Instead of writing complicated database code ourselves,
// we just define simple functions here, and the Room library figures out how to do the actual work. It's awesome.
@Dao
interface NotificationDao {

    // This function gets all the notifications from our database. The cool part is that it returns a `Flow`.
    // I think of it as a live 'stream' of our data. Whenever a notification is added or deleted,
    // this stream automatically sends the new, updated list to our UI. It's the magic that makes our screen refresh itself.
    @Query("SELECT * FROM notifications")
    fun getAll(): Flow<List<Notification>>

    // This one's pretty straightforward, it just adds a new notification to the database.
    // We made it return a `Long`, which is the ID of the new item we just saved.
    // We need that ID to schedule the unique alarm, so this is super important.
    @Insert
    suspend fun insert(notification: Notification): Long

    // And just as you'd expect, this one deletes a notification from the database.
    // We just pass it the item the user wants to get rid of, and poof, it's gone from the database.
    @Delete
    suspend fun delete(notification: Notification)

    // Okay, so this one looks similar to `getAll`, but there's a key difference.
    // It returns a simple `List` instead of a `Flow`. This is for when we don't need a live stream.
    // We just need to grab all the data one single time. I remember we added this specifically for our
    // `BootCompletedReceiver`, which needs to read all the saved alarms just once after the phone restarts.
    @Query("SELECT * FROM notifications")
    fun getAllOnce(): List<Notification>
}

// And this is the main database file itself. It's a bit abstract, I know.
// But it's where we tell the Room library everything it needs to know:
// 1. Which tables (or `entities`) are in our database (just the `Notification` one for now).
// 2. Which control panel (`dao`) to use for that database.
// Room handles all the complicated setup from here.
@Database(entities = [Notification::class], version = 1, exportSchema = false)
abstract class NotificationDatabase : RoomDatabase() {
    abstract fun dao(): NotificationDao
}