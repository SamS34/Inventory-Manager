// *** REPLACE THE ENTIRE CONTENTS of SettingsScreen.kt with this final code ***

@file:Suppress("DEPRECATION")

package com.samuel.inventorymanager.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.gson.Gson
import com.samuel.inventorymanager.data.AppSettings
import com.samuel.inventorymanager.data.AppTheme
import com.samuel.inventorymanager.data.CustomTheme
import com.samuel.inventorymanager.data.FontSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Helper function to check permission state
private fun checkStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentSettings: AppSettings,
    currentData: AppData,
    onSettingsChange: (AppSettings) -> Unit,
    onDataChange: (AppData) -> Unit,
    onClearAllData: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var settings by remember { mutableStateOf(currentSettings) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var hasStoragePermission by remember { mutableStateOf(checkStoragePermission(context)) }

    // This observer re-checks the permission every time the user returns to this screen. THIS IS THE FIX.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasStoragePermission = checkStoragePermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    LaunchedEffect(currentSettings) {
        settings = currentSettings
    }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            kotlinx.coroutines.delay(2500)
            feedbackMessage = null
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasStoragePermission = checkStoragePermission(context)
        feedbackMessage = if (hasStoragePermission) "✅ Storage granted" else "❌ Storage denied"
    }

    val manageStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // This runs after the user returns from the system settings screen
        hasStoragePermission = checkStoragePermission(context)
        feedbackMessage = if (hasStoragePermission) "✅ Storage permission granted!" else "❌ Storage permission was not granted."
    }

    val requestStoragePermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = "package:${context.packageName}".toUri()
                }
                manageStorageLauncher.launch(intent)
            } else {
                feedbackMessage = "✅ Storage permission is already granted."
            }
        } else {
            storagePermissionLauncher.launch(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            )
        }
    }

    val jsonImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isProcessing = true
                val result = importAppDataFromJson(context, it)
                isProcessing = false
                result?.let { importedData ->
                    onDataChange(importedData)
                    feedbackMessage = "✅ Data imported successfully"
                } ?: run { feedbackMessage = "❌ Import failed. Invalid file." }
            }
        }
    }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isProcessing = true
                val success = exportAppDataToJson(context, currentData, it)
                isProcessing = false
                feedbackMessage = if (success) "✅ Backup exported successfully" else "❌ Export failed"
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isProcessing = true
                val success = exportAppDataToCsv(context, currentData, it)
                isProcessing = false
                feedbackMessage = if (success) "✅ CSV exported successfully" else "❌ Export failed"
            }
        }
    }

    var themeExpanded by remember { mutableStateOf(false) }
    var dataExpanded by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("⚙️ Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Manage themes, data, and backups.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            item {
                ExpandableCard(
                    title = "🎨 Theme & Appearance",
                    icon = Icons.Default.Palette,
                    expanded = themeExpanded,
                    onToggle = { themeExpanded = !themeExpanded }
                ) {
                    ThemeSettingsContent(
                        settings = settings,
                        onThemeChange = { newTheme ->
                            settings = settings.copy(theme = newTheme)
                            onSettingsChange(settings)
                        },
                        onFontSizeChange = { newFontSize ->
                            settings = settings.copy(fontSize = newFontSize)
                            onSettingsChange(settings)
                        }
                    )
                }
            }
            item {
                ExpandableCard(
                    title = "💾 Data Management",
                    icon = Icons.Default.Storage,
                    expanded = dataExpanded,
                    onToggle = { dataExpanded = !dataExpanded }
                ) {
                    DataManagementContent(
                        hasPermission = hasStoragePermission, // Pass the reactive state here
                        onRequestPermissions = requestStoragePermission,
                        onExportJson = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            jsonExportLauncher.launch("inventory_backup_$timestamp.json")
                        },
                        onImportJson = { jsonImportLauncher.launch("application/json") },
                        onExportCsv = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            csvExportLauncher.launch("inventory_export_$timestamp.csv")
                        }
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
            item {
                OutlinedButton(
                    onClick = { showClearDataDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteForever, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Clear All Data")
                }
            }
        }
        if (isProcessing) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showColorPicker) {
        CustomThemeDialog(
            currentTheme = settings.customTheme ?: CustomTheme(),
            onDismiss = { showColorPicker = false },
            onConfirm = { customTheme ->
                settings = settings.copy(theme = AppTheme.CUSTOM, customTheme = customTheme)
                onSettingsChange(settings)
                showColorPicker = false
            }
        )
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear All Data?") },
            text = { Text("This will permanently delete all your inventory data and settings. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearDataDialog = false
                        feedbackMessage = "✅ All application data has been cleared."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear Everything") }
            },
            dismissButton = { TextButton({ showClearDataDialog = false }) { Text("Cancel") } }
        )
    }
}

// DATA I/O FUNCTIONS and UI HELPERS remain the same, just moved outside the main composable

private suspend fun exportAppDataToJson(context: Context, appData: AppData, uri: Uri): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val jsonString = Gson().toJson(appData)
            context.contentResolver.openOutputStream(uri)?.use { it.write(jsonString.toByteArray()) }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

private suspend fun importAppDataFromJson(context: Context, uri: Uri): AppData? {
    return withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use {
                val json = it.bufferedReader().readText()
                Gson().fromJson(json, AppData::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

private suspend fun exportAppDataToCsv(context: Context, appData: AppData, uri: Uri): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val garageMap = appData.garages.associateBy { it.id }
            val cabinetMap = appData.garages.flatMap { it.cabinets }.associateBy { it.id }
            val shelfMap = appData.garages.flatMap { it.cabinets }.flatMap { it.shelves }.associateBy { it.id }
            val boxMap = appData.garages.flatMap { it.cabinets }.flatMap { it.shelves }.flatMap { it.boxes }.associateBy { it.id }

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                OutputStreamWriter(stream).use { writer ->
                    writer.appendLine("\"itemID\",\"itemName\",\"quantity\",\"condition\",\"functionality\",\"garageName\",\"cabinetName\",\"shelfName\",\"boxName\",\"modelNumber\",\"description\",\"webLink\",\"minPrice\",\"maxPrice\",\"weight\",\"sizeCategory\",\"dimensions\"")
                    appData.items.forEach { item ->
                        val row = listOf(
                            item.id, item.name, item.quantity.toString(), item.condition,
                            item.functionality, garageMap[item.garageId]?.name ?: "N/A",
                            cabinetMap[item.cabinetId]?.name ?: "N/A", shelfMap[item.shelfId]?.name ?: "N/A",
                            item.boxId?.let { boxMap[it]?.name } ?: "", item.modelNumber ?: "",
                            item.description ?: "", item.webLink ?: "",
                            item.minPrice?.toString() ?: "", item.maxPrice?.toString() ?: "",
                            item.weight?.toString() ?: "", item.sizeCategory, item.dimensions ?: ""
                        ).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
                        writer.appendLine(row)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

@Composable
private fun ExpandableCard(
    title: String, icon: ImageVector, expanded: Boolean, onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(16.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun DataManagementContent(
    hasPermission: Boolean, // Takes the state as a parameter now
    onRequestPermissions: () -> Unit, onExportJson: () -> Unit, onImportJson: () -> Unit, onExportCsv: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!hasPermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, "Warning", tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("Storage Permission Required", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    Text("To import or export data files, the app needs storage access.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    Button(
                        onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Folder, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Grant Storage Permission")
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, "Granted", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("✅ Storage Permission Granted", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text("Backup & Restore (JSON)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Use a JSON file to create a complete backup of your app data or restore from a previous backup.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onImportJson, modifier = Modifier.weight(1f), enabled = hasPermission) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(8.dp))
                Text("Import")
            }
            OutlinedButton(onClick = onExportJson, modifier = Modifier.weight(1f), enabled = hasPermission) {
                Icon(Icons.Default.Upload, null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(8.dp))
                Text("Export")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text("Export for Spreadsheet (CSV)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Export your item list as a CSV file to open in Excel or Google Sheets.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Button(onClick = onExportCsv, modifier = Modifier.fillMaxWidth(), enabled = hasPermission) {
            Icon(Icons.Default.Upload, null)
            Spacer(Modifier.width(8.dp))
            Text("Export Items to CSV")
        }
    }
}

@Composable
private fun CustomThemeDialog(
    currentTheme: CustomTheme, onDismiss: () -> Unit, onConfirm: (CustomTheme) -> Unit
) {
    var theme by remember { mutableStateOf(currentTheme) }
    var fontSizeScale by remember { mutableFloatStateOf(currentTheme.fontSizeScale) }

    val colorOptions = listOf(
        "Red" to Color(0xFFE53E3E), "Orange" to Color(0xFFDD6B20),
        "Yellow" to Color(0xFFD69E2E), "Green" to Color(0xFF38A169),
        "Teal" to Color(0xFF319795), "Blue" to Color(0xFF3182CE),
        "Purple" to Color(0xFF805AD5), "Pink" to Color(0xFFD53F8C)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎨 Custom Theme") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("Select Primary Color", fontWeight = FontWeight.Bold) }
                item {
                    colorOptions.chunked(4).forEach { rowColors ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowColors.forEach { (_, color) ->
                                Box(
                                    Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                                        .background(color).clickable { theme = theme.copy(primaryColor = color.toArgb().toLong()) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (Color(theme.primaryColor.toULong()) == color) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                item { Text("Select Background", fontWeight = FontWeight.Bold) }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("Light" to Color(0xFFFFFFFF), "Dark" to Color(0xFF1A202C)).forEach { (name, color) ->
                            Box(
                                Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(12.dp))
                                    .background(color).clickable { theme = theme.copy(backgroundColor = color.toArgb().toLong()) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(name, color = if (color == Color.White) Color.Black else Color.White,
                                    fontWeight = if (Color(theme.backgroundColor.toULong()) == color) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
                item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                item { Text("Font & Icon Size Scale", fontWeight = FontWeight.Bold) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Slider(
                            value = fontSizeScale, onValueChange = { fontSizeScale = it }, valueRange = 0.8f..1.5f,
                            steps = 13, modifier = Modifier.fillMaxWidth()
                        )
                        Text(String.format(Locale.US, "Scale: %.2f (0.8x to 1.5x)", fontSizeScale), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = { Button({ onConfirm(theme.copy(fontSizeScale = fontSizeScale)) }) { Text("Apply Theme") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ThemeSettingsContent(
    settings: AppSettings, onThemeChange: (AppTheme) -> Unit, onFontSizeChange: (FontSize) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Select Theme", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        val themeItems = listOf(
            "☀️ Light" to AppTheme.LIGHT, "🌙 Dark" to AppTheme.DARK, "🧛 Dracula" to AppTheme.DRACULA,
            "🌊 Ocean" to AppTheme.OCEAN, "🌲 Forest" to AppTheme.FOREST, "🌅 Sunset" to AppTheme.SUNSET
        )
        themeItems.chunked(2).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { (name, theme) ->
                    ThemePresetCard(
                        name = name, color = getThemeColor(theme), isSelected = settings.theme == theme,
                        onClick = { onThemeChange(theme) }, modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Text("Font Size", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FontSize.entries.forEach { size ->
                FontSizeButton(size.name.lowercase().replaceFirstChar { it.uppercase() }, settings.fontSize == size) { onFontSizeChange(size) }
            }
        }
        Text("Current size: ${settings.fontSize.name} (${String.format(Locale.US, "%.2f", settings.fontSize.scale)}x)",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)
        )
    }
}

private fun getThemeColor(theme: AppTheme): Color {
    return when (theme) {
        AppTheme.LIGHT -> Color(0xFFE3F2FD)
        AppTheme.DARK -> Color(0xFF212121)
        AppTheme.DRACULA -> Color(0xFFBD93F9)
        AppTheme.OCEAN -> Color(0xFF00B4D8)
        AppTheme.FOREST -> Color(0xFF2D6A4F)
        AppTheme.SUNSET -> Color(0xFFFF6B35)
        AppTheme.VAMPIRE -> Color(0xFFFF1493)
        AppTheme.CYBERPUNK -> Color(0xFFFF006E)
        AppTheme.NEON -> Color(0xFF39FF14)
        AppTheme.SYSTEM -> Color(0xFF808080)
        AppTheme.CUSTOM -> Color(0xFF6200EE)
    }
}

@Composable
private fun ThemePresetCard(
    name: String, color: Color, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(60.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f)),
        border = if (isSelected) BorderStroke(3.dp, color) else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(color.copy(0.4f), color.copy(0.1f)))),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = if (themeIsDark(color)) Color.White else Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (isSelected) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Check, null, tint = color, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

private fun themeIsDark(color: Color): Boolean {
    val darkness = 1 - (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
    return darkness >= 0.5
}

@Composable
private fun RowScope.FontSizeButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick, Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(text, fontSize = 11.sp, maxLines = 1)
    }
}