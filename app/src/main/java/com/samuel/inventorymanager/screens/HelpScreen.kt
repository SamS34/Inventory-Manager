package com.samuel.inventorymanager.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri

@Composable
fun HelpScreen() {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // --- HEADER ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("❓", fontSize = 56.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Help & Support Center",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Your guide to mastering Android Inventory Pro",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- GETTING STARTED ---
        item {
            CollapsibleHelpSection(
                title = "Getting Started Guide",
                icon = Icons.Default.RocketLaunch,
                initiallyExpanded = true // Start with this one open
            ) {
                HelpContentBlock(
                    question = "Step 1: Create Your First Location",
                    answer = "Go to the 'Locations' tab and tap the 'Add Garage' button. A 'Garage' can be any main area, like a 'Workshop,' 'Storage Unit,' or 'Kitchen.'"
                )
                HelpContentBlock(
                    question = "Step 2: Build Your Hierarchy",
                    answer = "Inside your new Garage, you can add Cabinets. Inside Cabinets, add Shelves. Inside Shelves, add Boxes. This structure helps you know exactly where everything is."
                )
                HelpContentBlock(
                    question = "Step 3: Add Your First Item",
                    answer = "Navigate to the 'Items' tab and tap 'New'. Fill in the details. The most important fields are Item Name and its Location (Garage)."
                )
                HelpContentBlock(
                    question = "Step 4: Add Photos",
                    answer = "While editing an item, tap 'Camera' to take a photo or 'Upload' to select one from your device. A picture is worth a thousand words!"
                )
                HelpContentBlock(
                    question = "Step 5: Find Your Items",
                    answer = "Use the 'Search' tab to instantly find anything you've added. For a complete spreadsheet-like view of all your items, visit the 'Overview' tab."
                )
            }
        }

        // --- MANAGING ITEMS ---
        item {
            CollapsibleHelpSection(
                title = "Managing Your Items",
                icon = Icons.Default.ShoppingBag
            ) {
                HelpContentBlock(
                    question = "How does Auto-Save work?",
                    answer = "The app automatically saves your changes a few seconds after you stop typing in the 'Items' form. A '💾 Auto-saved' message will briefly appear. You can disable this in 'Settings' > 'Data Management' if you prefer to save manually."
                )
                HelpContentBlock(
                    question = "What are OCR and AI?",
                    answer = "These are powerful tools to help you add items faster. After taking a picture:\n• OCR scans the image for text (like a model number).\n• AI analyzes the image and tries to identify the item, suggesting a name and description for you."
                )
                HelpContentBlock(
                    question = "How do I add custom options for Condition or Functionality?",
                    answer = "In the 'Items' tab, next to the 'Item Condition' or 'Functionality' dropdowns, tap the '+ Add' button. This lets you create your own custom options (e.g., 'Slightly Scratched' or 'Needs Batteries') that will be saved for future use."
                )
            }
        }

        // --- DATA & BACKUP ---
        item {
            CollapsibleHelpSection(
                title = "Data, Backup &                     (Sync COMING SOON...)",
                icon = Icons.Default.SdStorage
            ) {
                HelpContentBlock(
                    question = "Where is my data stored?",
                    answer = "Your inventory data is stored locally in the app's private storage on your device. This means it works offline and is completely private. For safety, it is highly recommended you create regular backups."
                )
                HelpContentBlock(
                    question = "How do I back up and restore my data?",
                    answer = "Go to 'Settings' > 'Data Management'.\n• Export Data: This saves a complete copy of your inventory to a single '.json' file in your device's Downloads folder.\n• Import Data: This lets you load an inventory from a file. Warning: Importing will overwrite all of your current data.\n• Backup to Google Drive: Sign in to your Google account and save your backup file directly to a private folder in Google Drive."
                )
                HelpContentBlock(
                    question = "How do I sync with the computer app?",
                    answer = "The 'Sync' tab connects your device to our companion desktop app.\n1. Tap 'Generate' on your phone to get a Sync Key.\n2. Enter this key into the computer app.\n3. Enter the key from the computer app into your phone and tap 'Pair'."
                )
            }
        }

        // --- ADVANCED SETTINGS ---
        item {
            CollapsibleHelpSection(
                title = "Advanced Settings & APIs",
                icon = Icons.Default.Settings
            ) {
                HelpContentBlock(
                    question = "What are API Keys and do I need them?",
                    answer = "API Keys are special codes that let this app connect to powerful online services for OCR and AI. The app works perfectly fine without them using its free, built-in tools. However, adding keys can provide even more accurate results."
                )
                HelpContentBlock(
                    question = "How does the Priority Fallback system work?",
                    answer = "In the OCR and AI settings, you can reorder the list of services. When you use a feature, the app tries the first service. If it fails, it automatically 'falls back' to the next one, ensuring you always get a result from the best available service."
                )
            }
        }

        // --- APP INFO AND CONTACT CARD ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // App Info Section
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("App Information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    // FIX: Renamed InfoRow to AppInfoRow to solve overload ambiguity
                    AppInfoRow("Version:", "1.0 (FIRST)")
                    AppInfoRow("Platform:", "Android (Jetpack Compose)")
                    AppInfoRow("Data Storage:", "Local Device Storage")

                    // FIX: Replaced deprecated Divider with HorizontalDivider
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Contact Section
                    ContactRow(
                        icon = Icons.Default.Code,
                        title = "GitHub of Parminder",
                        subtitle = "github.com/JohnJackson12",
                        onClick = {
                            // FIX: Use .toUri() KTX extension
                            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/JohnJackson12".toUri())
                            context.startActivity(intent)
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                    ContactRow(
                        icon = Icons.Default.Code,
                        title = "GitHub of Samuel",
                        subtitle = "github.com/SamS34",
                        onClick = {
                            // FIX: Use .toUri() KTX extension
                            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/SamS34".toUri())
                            context.startActivity(intent)
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                    ContactRow(
                        icon = Icons.Default.Email,
                        title = "Email of Parminder",
                        subtitle = "parminder.nz@gmail.com",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                // FIX: Use .toUri() KTX extension
                                data = "mailto:parminder.nz@gmail.com".toUri()
                            }
                            context.startActivity(intent)
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                    ContactRow(
                        icon = Icons.Default.Email,
                        title = "Email of Samuel",
                        subtitle = "sam.of.s34@gmail.com",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                // FIX: Use .toUri() KTX extension
                                data = "mailto:sam.of.s34@gmail.com".toUri()
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}


// ======================================================================
//                              Helper Composables
// These are the reusable building blocks that make the screen work! ✨
// ======================================================================

/**
 * A reusable, collapsible Card for displaying FAQ-style help content.
 */
@Composable
private fun CollapsibleHelpSection(
    title: String,
    icon: ImageVector,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val rotationAngle by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "rotation")

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotationAngle)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // FIX: Replaced deprecated Divider with HorizontalDivider
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))
                    content()
                }
            }
        }
    }
}


/**
 * Formats a question and answer block within a help section.
 */
@Composable
private fun HelpContentBlock(question: String, answer: String) {
    Column {
        Text(
            text = question,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = answer,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 24.sp
        )
    }
}

/**
 * Displays a clickable row for contact information.
 */
@Composable
private fun ContactRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            )
        }
    }
}


/**
 * Displays a simple "Label: Value" row for app information.
 * FIX: Renamed from InfoRow to AppInfoRow to resolve compiler ambiguity.
 */
@Composable
private fun AppInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.6f),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}