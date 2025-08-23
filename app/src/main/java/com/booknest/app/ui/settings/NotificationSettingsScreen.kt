package com.booknest.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBackClick: () -> Unit
) {
    var pushNotifications by remember { mutableStateOf(true) }
    var emailNotifications by remember { mutableStateOf(true) }
    var bookReminders by remember { mutableStateOf(true) }
    var priceAlerts by remember { mutableStateOf(false) }
    var newBooksAlerts by remember { mutableStateOf(true) }
    var marketingEmails by remember { mutableStateOf(false) }
    var weeklyDigest by remember { mutableStateOf(true) }
    var rentalsReminder by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    "Notification Settings",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Push Notifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        NotificationToggleItem(
                            title = "Enable Push Notifications",
                            subtitle = "Receive notifications on your device",
                            checked = pushNotifications,
                            onCheckedChange = { pushNotifications = it },
                            icon = Icons.Default.Notifications
                        )

                        NotificationToggleItem(
                            title = "Book Reminders",
                            subtitle = "Reminders about due dates and renewals",
                            checked = bookReminders,
                            onCheckedChange = { bookReminders = it },
                            icon = Icons.Default.Schedule,
                            enabled = pushNotifications
                        )

                        NotificationToggleItem(
                            title = "Price Alerts",
                            subtitle = "Get notified when book prices drop",
                            checked = priceAlerts,
                            onCheckedChange = { priceAlerts = it },
                            icon = Icons.Default.TrendingDown,
                            enabled = pushNotifications
                        )

                        NotificationToggleItem(
                            title = "New Books",
                            subtitle = "Notifications about new book arrivals",
                            checked = newBooksAlerts,
                            onCheckedChange = { newBooksAlerts = it },
                            icon = Icons.Default.NewReleases,
                            enabled = pushNotifications
                        )

                        NotificationToggleItem(
                            title = "Rental Reminders",
                            subtitle = "Reminders about rental due dates",
                            checked = rentalsReminder,
                            onCheckedChange = { rentalsReminder = it },
                            icon = Icons.Default.AccessTime,
                            enabled = pushNotifications
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Email Notifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        NotificationToggleItem(
                            title = "Email Notifications",
                            subtitle = "Receive important updates via email",
                            checked = emailNotifications,
                            onCheckedChange = { emailNotifications = it },
                            icon = Icons.Default.Email
                        )

                        NotificationToggleItem(
                            title = "Weekly Digest",
                            subtitle = "Weekly summary of your activity",
                            checked = weeklyDigest,
                            onCheckedChange = { weeklyDigest = it },
                            icon = Icons.Default.CalendarToday,
                            enabled = emailNotifications
                        )

                        NotificationToggleItem(
                            title = "Marketing Emails",
                            subtitle = "Promotional offers and deals",
                            checked = marketingEmails,
                            onCheckedChange = { marketingEmails = it },
                            icon = Icons.Default.LocalOffer,
                            enabled = emailNotifications
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Notification Tip",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "You can always change these settings later. We recommend keeping rental reminders enabled to avoid late fees.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Bottom navigation space
            }
        }
    }
}

@Composable
fun NotificationToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                  else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
