// File: MainActivity.kt
package com.example.notio

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.notio.ui.theme.NotioTheme

// So this is our MainActivity, the main screen and entry point of the app.
class MainActivity : ComponentActivity() {

    // This companion object is basically a little public space for our app.
    // We're putting the application Context here so that other parts of our app,
    // like the ViewModel, can easily grab it without us having to pass it around everywhere.
    // It's a handy shortcut.
    companion object {
        lateinit var AppContext: android.content.Context
            private set
    }

    // This `requestPermissionLauncher` is the modern way to ask for permissions.
    // We're setting it up to ask for a single permission, and then the part in the curly braces `{}`
    // is the code that runs *after* the user taps "Allow" or "Deny".
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // This `isGranted` variable is a boolean, so it's either true or false.


    }

    // This is the classic `onCreate` function. It's the first thing that runs when this screen opens.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Here we're grabbing the application context and storing it in our public `AppContext` variable.
        // Now the rest of the app can use it.
        AppContext = applicationContext

        // This just makes the app use the whole screen, drawing behind the status and navigation bars.
        // It makes things look more modern.
        enableEdgeToEdge()

        // This whole block is our logic for asking for notification permission.
        // It's a bit weird, but this is the Google-recommended way.
        // First, we check if the phone is Android 13 or newer, since this permission only exists on new versions.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // This `shouldShowRequestPermissionRationale` is a mouthful. It basically checks if we've asked before and the user said no.
            // By putting a "!" in front, we're saying: "If this is the very first time we are asking, then let's go ahead and ask."
            // I hope this logic makes sense, it's a bit backward.
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                // Okay, it's our first time, let's launch the permission dialog.
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        // If the phone is older than Android 13, we don't need to do anything. We already have permission.

        // And finally, this is the Jetpack Compose part where we tell the app to draw our UI.
        // We're just telling it to show the HomeScreen.
        setContent {
            NotioTheme {
                HomeScreen()
            }
        }
    }
}