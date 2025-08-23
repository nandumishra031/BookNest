package com.booknest.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
fun PrivacySettingsScreen(
    onBackClick: () -> Unit
) {
    var profileVisibility by remember { mutableStateOf("Public") }
    var showLocation by remember { mutableStateOf(true) }
    var showPurchaseHistory by remember { mutableStateOf(false) }
    var allowDataCollection by remember { mutableStateOf(true) }
    var shareDataWithPartners by remember { mutableStateOf(false) }
    var showOnlineStatus by remember { mutableStateOf(true) }
    var allowContactFromSellers by remember { mutableStateOf(true) }

    val profileVisibilityOptions = listOf("Public", "Friends Only", "Private")
    var showVisibilityDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    "Privacy Settings",
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
                            text = "Profile Privacy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Profile Visibility Dropdown
                        ExposedDropdownMenuBox(
                            expanded = showVisibilityDropdown,
                            onExpandedChange = { showVisibilityDropdown = !showVisibilityDropdown }
                        ) {
                            OutlinedTextField(
                                value = profileVisibility,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Profile Visibility") },
                                leadingIcon = {
                                    Icon(Icons.Default.Visibility, contentDescription = null)
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = showVisibilityDropdown
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = showVisibilityDropdown,
                                onDismissRequest = { showVisibilityDropdown = false }
                            ) {
                                profileVisibilityOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            profileVisibility = option
                                            showVisibilityDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        PrivacyToggleItem(
                            title = "Show Location",
                            subtitle = "Allow others to see your general location",
                            checked = showLocation,
                            onCheckedChange = { showLocation = it },
                            icon = Icons.Default.LocationOn
                        )

                        PrivacyToggleItem(
                            title = "Show Purchase History",
                            subtitle = "Display your reading preferences publicly",
                            checked = showPurchaseHistory,
                            onCheckedChange = { showPurchaseHistory = it },
                            icon = Icons.Default.History
                        )

                        PrivacyToggleItem(
                            title = "Show Online Status",
                            subtitle = "Let others know when you're active",
                            checked = showOnlineStatus,
                            onCheckedChange = { showOnlineStatus = it },
                            icon = Icons.Default.Circle
                        )

                        PrivacyToggleItem(
                            title = "Allow Contact from Sellers",
                            subtitle = "Sellers can message you about books",
                            checked = allowContactFromSellers,
                            onCheckedChange = { allowContactFromSellers = it },
                            icon = Icons.Default.Message
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
                            text = "Data & Analytics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        PrivacyToggleItem(
                            title = "Data Collection",
                            subtitle = "Help improve our service with usage analytics",
                            checked = allowDataCollection,
                            onCheckedChange = { allowDataCollection = it },
                            icon = Icons.Default.Analytics
                        )

                        PrivacyToggleItem(
                            title = "Share with Partners",
                            subtitle = "Share anonymized data with trusted partners",
                            checked = shareDataWithPartners,
                            onCheckedChange = { shareDataWithPartners = it },
                            icon = Icons.Default.Share
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Data Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        PrivacyActionItem(
                            title = "Download My Data",
                            subtitle = "Get a copy of your personal data",
                            icon = Icons.Default.Download,
                            onClick = { /* Handle download data */ }
                        )

                        PrivacyActionItem(
                            title = "Delete Search History",
                            subtitle = "Clear all your search and browsing history",
                            icon = Icons.Default.DeleteSweep,
                            onClick = { /* Handle delete search history */ }
                        )

                        PrivacyActionItem(
                            title = "Manage Blocked Users",
                            subtitle = "View and manage blocked users list",
                            icon = Icons.Default.Block,
                            onClick = { /* Handle manage blocked users */ }
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
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Your Privacy Matters",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "We're committed to protecting your privacy. You have full control over what information you share.",
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
fun PrivacyToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun PrivacyActionItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
