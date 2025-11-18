// File: HomeScreen.kt
package com.example.notio

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notio.ui.theme.NotioTheme

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: NotificationViewModel = viewModel()) {

    val items by vm.notifications.collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var newTask by remember { mutableStateOf("") }

    // ✅ CHANGED: Now using 12-hour format with AM/PM toggle
    val timePickerState = rememberTimePickerState(
        initialHour = 12,
        initialMinute = 0,
        is24Hour = false  // This enables 12-hour format with AM/PM toggle
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add notification")
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Your Notifications",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notifications yet. Add one!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.task,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "At: ${item.time}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { vm.delete(item) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add New Notification") },
                text = {
                    Column {
                        TextField(
                            value = newTask,
                            onValueChange = { newTask = it },
                            label = { Text("Notification Task") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Select Time", style = MaterialTheme.typography.labelLarge)

                        // The TimePicker now shows 1-12 with AM/PM toggle!
                        TimePicker(state = timePickerState)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newTask.isNotBlank()) {
                                val hour = timePickerState.hour
                                val minute = timePickerState.minute

                                // ✅ SIMPLIFIED: The formatting still works the same way
                                val formattedTime = String.format(
                                    "%02d:%02d %s",
                                    if (hour % 12 == 0) 12 else hour % 12,
                                    minute,
                                    if (hour < 12) "AM" else "PM"
                                )

                                vm.add(newTask, formattedTime)

                                newTask = ""
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
//this is to update
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    NotioTheme {
        HomeScreen()
    }
}