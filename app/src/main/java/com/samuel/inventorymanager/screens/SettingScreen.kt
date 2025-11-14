@file:Suppress("DEPRECATION")

package com.samuel.inventorymanager.screens

import android.Manifest
import android.app.Activity
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.samuel.inventorymanager.auth.GoogleAuthManager
import com.samuel.inventorymanager.data.AISettings
import com.samuel.inventorymanager.data.AppSettings
import com.samuel.inventorymanager.data.AppTheme
import com.samuel.inventorymanager.data.AutoFeatures
import com.samuel.inventorymanager.data.CustomTheme
import com.samuel.inventorymanager.data.FontSize
import com.samuel.inventorymanager.data.GoogleSettings
import com.samuel.inventorymanager.data.OCRSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentSettings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authManager = remember { GoogleAuthManager(context) }

    var settings by remember { mutableStateOf(currentSettings) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentSettings) {
        settings = currentSettings
    }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            kotlinx.coroutines.delay(2000)
            feedbackMessage = null
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        feedbackMessage = if (granted) "✅ Storage permission granted" else "❌ Storage permission denied"
    }

    val manageStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else true
        feedbackMessage = if (granted) "✅ Storage access granted" else "❌ Storage access denied"
    }

    val requestStoragePermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                manageStorageLauncher.launch(intent)
            }
        } else {
            storagePermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    // Firebase Google Sign-In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken

                if (idToken != null) {
                    authManager.handleSignInResult(
                        idToken,
                        onSuccess = { message ->
                            settings = settings.copy(
                                googleSettings = settings.googleSettings.copy(
                                    signedIn = true,
                                    userEmail = authManager.getCurrentUserEmail()
                                )
                            )
                            onSettingsChange(settings)
                            feedbackMessage = message
                        },
                        onError = { error ->
                            feedbackMessage = error
                        }
                    )
                }
            } catch (e: ApiException) {
                feedbackMessage = "❌ Sign in failed: ${e.message}"
            }
        } else {
            feedbackMessage = "❌ Sign in cancelled"
        }
    }

    val onSignInClick: () -> Unit = {
        val signInIntent = authManager.getSignInIntent()
        googleSignInLauncher.launch(signInIntent)
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isProcessing = true
                val result = importSettings(context, it)
                isProcessing = false
                result?.let { importedSettings ->
                    settings = importedSettings
                    onSettingsChange(importedSettings)
                    feedbackMessage = "✅ Settings imported successfully"
                } ?: run {
                    feedbackMessage = "❌ Failed to import settings"
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isProcessing = true
                val success = exportSettings(context, settings, it)
                isProcessing = false
                feedbackMessage = if (success) "✅ Settings exported successfully"
                else "❌ Failed to export settings"
            }
        }
    }

    var themeExpanded by remember { mutableStateOf(true) }
    var ocrExpanded by remember { mutableStateOf(false) }
    var aiExpanded by remember { mutableStateOf(false) }
    var googleExpanded by remember { mutableStateOf(false) }
    var dataExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                feedbackMessage?.let { message ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (message.startsWith("✅"))
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            message,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                ExpandableCard(
                    title = "Theme Settings",
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
                        },
                        onCustomThemeClick = { showColorPicker = true }
                    )
                }
            }

            item {
                ExpandableCard(
                    title = "OCR Settings & Fallback Priority",
                    icon = Icons.Default.DocumentScanner,
                    expanded = ocrExpanded,
                    onToggle = { ocrExpanded = !ocrExpanded }
                ) {
                    OCRSettingsContent(
                        ocrSettings = settings.ocrSettings,
                        onUpdate = { newOcrSettings ->
                            settings = settings.copy(ocrSettings = newOcrSettings)
                            onSettingsChange(settings)
                        }
                    )
                }
            }

            item {
                ExpandableCard(
                    title = "AI Settings & Fallback Priority",
                    icon = Icons.Default.Psychology,
                    expanded = aiExpanded,
                    onToggle = { aiExpanded = !aiExpanded }
                ) {
                    AISettingsContent(
                        aiSettings = settings.aiSettings,
                        onUpdate = { newAiSettings ->
                            settings = settings.copy(aiSettings = newAiSettings)
                            onSettingsChange(settings)
                        }
                    )
                }
            }

            item {
                ExpandableCard(
                    title = "Google Settings",
                    icon = Icons.Default.Cloud,
                    expanded = googleExpanded,
                    onToggle = { googleExpanded = !googleExpanded }
                ) {
                    GoogleSettingsContent(
                        authManager = authManager,
                        googleSettings = settings.googleSettings,
                        onSignInClick = onSignInClick,
                        onUpdate = { newGoogleSettings ->
                            settings = settings.copy(googleSettings = newGoogleSettings)
                            onSettingsChange(settings)
                        },
                        onBackupNow = {
                            scope.launch {
                                isProcessing = true
                                authManager.uploadToDrive(
                                    "inventory_backup_${System.currentTimeMillis()}.json",
                                    onSuccess = { message ->
                                        isProcessing = false
                                        val updated = settings.copy(
                                            googleSettings = settings.googleSettings.copy(
                                                lastBackupTime = System.currentTimeMillis()
                                            )
                                        )
                                        settings = updated
                                        onSettingsChange(updated)
                                        feedbackMessage = message
                                    }
                                )
                            }
                        }
                    )
                }
            }

            item {
                ExpandableCard(
                    title = "Data Management & Android Features",
                    icon = Icons.Default.Storage,
                    expanded = dataExpanded,
                    onToggle = { dataExpanded = !dataExpanded }
                ) {
                    DataManagementContent(
                        autoFeatures = settings.autoFeatures,
                        onRequestPermissions = requestStoragePermission,
                        onUpdate = { newAutoFeatures ->
                            settings = settings.copy(autoFeatures = newAutoFeatures)
                            onSettingsChange(settings)
                        },
                        onExport = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                .format(Date())
                            exportLauncher.launch("inventory_settings_$timestamp.json")
                        },
                        onImport = {
                            importLauncher.launch("application/json")
                        },
                        onLocalSaveNow = {
                            scope.launch {
                                isProcessing = true
                                val success = performLocalSave(context, settings)
                                isProcessing = false
                                if (success) {
                                    val updated = settings.copy(
                                        autoFeatures = settings.autoFeatures.copy(
                                            lastLocalSaveTime = System.currentTimeMillis()
                                        )
                                    )
                                    settings = updated
                                    onSettingsChange(updated)
                                    feedbackMessage = "✅ Local save completed"
                                } else {
                                    feedbackMessage = "❌ Local save failed"
                                }
                            }
                        }
                    )
                }
            }

            item {
                Button(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.RestartAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset All Settings")
                }
            }
        }

        if (isProcessing) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
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

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Settings") },
            text = { Text("Are you sure you want to reset all settings to default?") },
            confirmButton = {
                Button(
                    onClick = {
                        settings = AppSettings()
                        onSettingsChange(settings)
                        showResetDialog = false
                        feedbackMessage = "✅ Settings reset to default"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton({ showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ========================================================================================
// HELPER FUNCTIONS
// ========================================================================================

suspend fun exportSettings(context: Context, settings: AppSettings, uri: Uri): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(settings.toJson().toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

suspend fun importSettings(context: Context, uri: Uri): AppSettings? {
    return withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val json = inputStream.bufferedReader().readText()
                AppSettings.fromJson(json)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

suspend fun performLocalSave(context: Context, settings: AppSettings): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val file = java.io.File(context.filesDir, "app_settings.json")
            file.writeText(settings.toJson())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

// ========================================================================================
// COMPOSABLE COMPONENTS
// ========================================================================================

@Composable
fun ExpandableCard(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
fun ThemeSettingsContent(
    settings: AppSettings,
    onThemeChange: (AppTheme) -> Unit,
    onFontSizeChange: (FontSize) -> Unit,
    onCustomThemeClick: () -> Unit
) {
    val themeOptions = listOf(
        "☀️ Light" to Color(0xFFFFFBFE),
        "🌙 Dark" to Color(0xFF1A1A1A),
        "🧛 Dracula" to Color(0xFFBD93F9),
        "🧟 Vampire" to Color(0xFFFF1493),
        "🌊 Ocean" to Color(0xFF00B4D8),
        "🌲 Forest" to Color(0xFF2D6A4F),
        "🌅 Sunset" to Color(0xFFFF6B35),
        "⚙️ Cyberpunk" to Color(0xFFFF006E),
        "⚡ Neon" to Color(0xFF39FF14)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Select Theme", style = MaterialTheme.typography.labelLarge)

        // --- FIX START ---
        // Replace LazyVerticalGrid with a standard Column and Rows to build the grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Group the theme options into rows of 3
            themeOptions.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Create a ThemeOptionCard for each item in the row, giving it equal weight
                    rowItems.forEach { (name, color) ->
                        Box(modifier = Modifier.weight(1f)) {
                            ThemeOptionCard(name, color, settings.theme == AppTheme.CUSTOM) {
                                onThemeChange(AppTheme.CUSTOM)
                                onCustomThemeClick()
                            }
                        }
                    }
                    // Add invisible spacers to fill the row if it has fewer than 3 items.
                    // This ensures items in the last row align correctly with the rows above.
                    if (rowItems.size < 3) {
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        // --- FIX END ---

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Text("Font & Icon Size", style = MaterialTheme.typography.labelLarge)
        Text(
            "Select default text size",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FontSize.entries.forEach { size ->
                FontSizeButton(
                    size.name.lowercase().replaceFirstChar { it.uppercase() },
                    settings.fontSize == size
                ) { onFontSizeChange(size) }
            }
        }
    }
}

@Composable
fun ThemeOptionCard(
    themeName: String,
    themeColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = themeColor.copy(alpha = 0.2f)
        ),
        border = if (isSelected) BorderStroke(3.dp, themeColor) else null,
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            themeColor.copy(alpha = 0.4f),
                            themeColor.copy(alpha = 0.1f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    themeName,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        null,
                        tint = themeColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.FontSizeButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick,
        Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(text, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
fun OCRSettingsContent(ocrSettings: OCRSettings, onUpdate: (OCRSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "OCR Priority (Use Fallback)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Higher priority providers are tried first",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ocrSettings.providerPriority.forEachIndexed { index, provider ->
            ProviderPriorityItem(
                name = provider.name.replace("_", " ").lowercase()
                    .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                priority = index + 1,
                onMoveUp = if (index > 0) {
                    {
                        val newList = ocrSettings.providerPriority.toMutableList()
                        val temp = newList[index]
                        newList[index] = newList[index - 1]
                        newList[index - 1] = temp
                        onUpdate(ocrSettings.copy(providerPriority = newList))
                    }
                } else null,
                onMoveDown = if (index < ocrSettings.providerPriority.size - 1) {
                    {
                        val newList = ocrSettings.providerPriority.toMutableList()
                        val temp = newList[index]
                        newList[index] = newList[index + 1]
                        newList[index + 1] = temp
                        onUpdate(ocrSettings.copy(providerPriority = newList))
                    }
                } else null
            )
        }

        Button(
            onClick = { onUpdate(ocrSettings.copy(providerPriority = OCRSettings().providerPriority)) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            Icon(Icons.Default.RestartAlt, null)
            Spacer(Modifier.width(8.dp))
            Text("Reset to Default Priority")
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        APIKeyField("Roboflow API Key", ocrSettings.roboflowApiKey) {
            onUpdate(ocrSettings.copy(roboflowApiKey = it))
        }
        APIKeyField("OCR Space API Key", ocrSettings.ocrSpaceApiKey) {
            onUpdate(ocrSettings.copy(ocrSpaceApiKey = it))
        }
        APIKeyField("Google Vision API Key", ocrSettings.googleVisionApiKey) {
            onUpdate(ocrSettings.copy(googleVisionApiKey = it))
        }
    }
}

@Composable
fun AISettingsContent(aiSettings: AISettings, onUpdate: (AISettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "AI Priority (Use Fallback)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Higher priority providers are tried first",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        aiSettings.providerPriority.forEachIndexed { index, provider ->
            ProviderPriorityItem(
                name = provider.name.replace("_", " ").lowercase()
                    .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                priority = index + 1,
                onMoveUp = if (index > 0) {
                    {
                        val newList = aiSettings.providerPriority.toMutableList()
                        val temp = newList[index]
                        newList[index] = newList[index - 1]
                        newList[index - 1] = temp
                        onUpdate(aiSettings.copy(providerPriority = newList))
                    }
                } else null,
                onMoveDown = if (index < aiSettings.providerPriority.size - 1) {
                    {
                        val newList = aiSettings.providerPriority.toMutableList()
                        val temp = newList[index]
                        newList[index] = newList[index + 1]
                        newList[index + 1] = temp
                        onUpdate(aiSettings.copy(providerPriority = newList))
                    }
                } else null
            )
        }

        Icon(Icons.Default.RestartAlt, null)
        Spacer(Modifier.width(8.dp))
        Text("Reset to Default Priority")
    }

    HorizontalDivider(Modifier.padding(vertical = 8.dp))

    APIKeyField("Google Gemini API Key", aiSettings.googleGeminiApiKey) {
        onUpdate(aiSettings.copy(googleGeminiApiKey = it))
    }
    APIKeyField("ChatGPT API Key", aiSettings.openAIApiKey) {
        onUpdate(aiSettings.copy(openAIApiKey = it))
    }
}


@Composable
fun GoogleSettingsContent(
    authManager: GoogleAuthManager,
    googleSettings: GoogleSettings,
    onSignInClick: () -> Unit,
    onUpdate: (GoogleSettings) -> Unit,
    onBackupNow: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (authManager.isSignedIn()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        "Signed In",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "✅ Signed In",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            authManager.getCurrentUserEmail(),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            SwitchRow(
                "🔄 Auto Backup to Drive",
                googleSettings.autoBackupToDrive
            ) {
                onUpdate(googleSettings.copy(autoBackupToDrive = it))
            }

            if (googleSettings.lastBackupTime > 0) {
                Text(
                    "Last backup: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(googleSettings.lastBackupTime))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onBackupNow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Backup, null)
                Spacer(Modifier.width(8.dp))
                Text("Backup Now")
            }

            Button(
                onClick = {
                    authManager.signOut()
                    onUpdate(GoogleSettings())
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out")
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "ℹ️ Why Sign In?",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "• Backup your inventory to Google Drive\n" +
                                "• Access data across devices\n" +
                                "• Never lose your data",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Button(
                onClick = onSignInClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4285F4)
                )
            ) {
                Icon(Icons.Default.Login, null)
                Spacer(Modifier.width(8.dp))
                Text("Sign in with Google")
            }
        }
    }
}

@Composable
fun DataManagementContent(
    autoFeatures: AutoFeatures,
    onRequestPermissions: () -> Unit,
    onUpdate: (AutoFeatures) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onLocalSaveNow: () -> Unit
) {
    val context = LocalContext.current
    val hasStoragePermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!hasStoragePermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            "Warning",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Storage Permission Required",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Text(
                        "To save data locally, grant storage access",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
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
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        "Granted",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "✅ Storage Permission Granted",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        HorizontalDivider()

        SwitchRow("📦 Auto Local Save", autoFeatures.autoLocalSave) {
            onUpdate(autoFeatures.copy(autoLocalSave = it))
        }
        SwitchRow("☁️ Auto Google Backup", autoFeatures.autoGoogleBackup) {
            onUpdate(autoFeatures.copy(autoGoogleBackup = it))
        }

        if (autoFeatures.lastLocalSaveTime > 0) {
            Text(
                "Last local save: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(autoFeatures.lastLocalSaveTime))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = onLocalSaveNow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(Modifier.width(8.dp))
            Text("Save Locally Now")
        }

        HorizontalDivider()

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onExport,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Upload, null)
                Spacer(Modifier.width(4.dp))
                Text("Export")
            }
            OutlinedButton(
                onClick = onImport,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Download, null)
                Spacer(Modifier.width(4.dp))
                Text("Import")
            }
        }
    }
}

@Composable
fun APIKeyField(label: String, value: String, onValueChange: (String) -> Unit) {
    var showKey by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (showKey)
            VisualTransformation.None
        else
            PasswordVisualTransformation(),
        trailingIcon = {
            IconButton({ showKey = !showKey }) {
                Icon(
                    if (showKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    if (showKey) "Hide" else "Show"
                )
            }
        },
        singleLine = true
    )
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomThemeDialog(
    currentTheme: CustomTheme,
    onDismiss: () -> Unit,
    onConfirm: (CustomTheme) -> Unit
) {
    var theme by remember { mutableStateOf(currentTheme) }
    var fontSizeScale by remember { mutableFloatStateOf(currentTheme.fontSizeScale) }

    val colorOptions = listOf(
        "Red" to Color(0xFFE53E3E),
        "Orange" to Color(0xFFDD6B20),
        "Yellow" to Color(0xFFD69E2E),
        "Green" to Color(0xFF38A169),
        "Teal" to Color(0xFF319795),
        "Blue" to Color(0xFF3182CE),
        "Purple" to Color(0xFF805AD5),
        "Pink" to Color(0xFFD53F8C)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎨 Custom Theme") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Select Primary Color", fontWeight = FontWeight.Bold)
                }
                item {
                    colorOptions.chunked(4).forEach { rowColors ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowColors.forEach { (name, color) ->
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(color)
                                        .clickable {
                                            theme = theme.copy(
                                                primaryColor = color.toArgb().toLong()
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (Color(theme.primaryColor.toULong()) == color) {
                                        Icon(
                                            Icons.Default.Check,
                                            null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

                item {
                    Text("Select Background", fontWeight = FontWeight.Bold)
                }
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf(
                            "Light" to Color(0xFFFFFFFF),
                            "Dark" to Color(0xFF1A202C)
                        ).forEach { (name, color) ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(color)
                                    .clickable {
                                        theme = theme.copy(
                                            backgroundColor = color.toArgb().toLong()
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    name,
                                    color = if (color == Color.White) Color.Black else Color.White,
                                    fontWeight = if (Color(theme.backgroundColor.toULong()) == color)
                                        FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

                item {
                    Text("Font & Icon Size Scale", fontWeight = FontWeight.Bold)
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Slider(
                            value = fontSizeScale,
                            onValueChange = { fontSizeScale = it },
                            valueRange = 0.8f..1.5f,
                            steps = 13,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Scale: %.2f (0.8x to 1.5x)".format(fontSizeScale),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button({
                onConfirm(theme.copy(fontSizeScale = fontSizeScale))
            }) { Text("Apply Theme") }
        },
        dismissButton = {
            TextButton(onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ProviderPriorityItem(
    name: String,
    priority: Int,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        priority.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(name, fontWeight = FontWeight.Medium)
            }
            Row {
                IconButton(
                    onClick = { onMoveUp?.invoke() },
                    enabled = onMoveUp != null
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        "Move Up",
                        tint = if (onMoveUp != null)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Gray
                    )
                }
                IconButton(
                    onClick = { onMoveDown?.invoke() },
                    enabled = onMoveDown != null
                ) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        "Move Down",
                        tint = if (onMoveDown != null)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Gray
                    )
                }
            }
        }
    }
}