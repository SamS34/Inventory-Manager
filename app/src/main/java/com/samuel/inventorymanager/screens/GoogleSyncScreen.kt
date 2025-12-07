@file:Suppress("DEPRECATION")

package com.samuel.inventorymanager.screens

import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private const val BACKUP_FILE_NAME = "inventory_manager_backup.json"
private const val AUTO_SAVE_INTERVAL = 30000L // 30 seconds

@Composable
fun GoogleSyncScreen(
    garages: List<Garage>,
    items: List<Item>,
    history: List<HistoryEntry>,
    onDataRestored: (AppData) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var driveService by remember { mutableStateOf<Drive?>(null) }
    var userEmail by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var lastSyncTime by remember { mutableStateOf<Long?>(null) }
    var autoSaveEnabled by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    // Check if already signed in
    LaunchedEffect(Unit) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            userEmail = account.email
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE_APPDATA)
            ).setSelectedAccount(account.account)

            driveService = Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Inventory Manager").build()
        }
    }

    // Auto-save loop
    LaunchedEffect(autoSaveEnabled, driveService, garages.size, items.size, history.size) {
        if (autoSaveEnabled && driveService != null) {
            while (true) {
                delay(AUTO_SAVE_INTERVAL)
                try {
                    val data = AppData(garages, items, history)
                    uploadToGoogleDrive(context, driveService!!, data)
                    lastSyncTime = System.currentTimeMillis()
                    statusMessage = "✅ Auto-saved"
                } catch (e: Exception) {
                    Log.e("GoogleSync", "Auto-save failed", e)
                }
            }
        }
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.result
            userEmail = account.email

            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE_APPDATA)
            ).setSelectedAccount(account.account)

            driveService = Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Inventory Manager").build()

            statusMessage = "✅ Connected successfully"
        } catch (e: Exception) {
            Log.e("GoogleSync", "Sign-in failed", e)
            statusMessage = "❌ Sign-in failed"
        }
    }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    // Dialogs
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = Color(0xFFEF4444)) },
            title = { Text("Delete All Cloud Data?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will permanently delete ALL your inventory data from Google. Your local data will remain unchanged.\n\nThis cannot be undone!")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                deleteFromGoogleDrive(driveService!!)
                                statusMessage = "🗑️ Cloud data deleted"
                            } catch (e: Exception) {
                                statusMessage = "❌ Delete failed"
                            }
                            isLoading = false
                            showDeleteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            icon = { Icon(Icons.Default.Logout, null) },
            title = { Text("Sign Out?", fontWeight = FontWeight.Bold) },
            text = { Text("Your data will remain in the cloud and locally. You can sign in again anytime.") },
            confirmButton = {
                Button(
                    onClick = {
                        googleSignInClient.signOut()
                        driveService = null
                        userEmail = null
                        autoSaveEnabled = false
                        statusMessage = "👋 Signed out"
                        showSignOutDialog = false
                    }
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF4285F4), Color(0xFF34A853))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CloudSync,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Google Sync",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Automatic cloud backup",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(20.dp))

                // Connection Status Card
                AnimatedVisibility(
                    visible = driveService != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    ConnectedStatusCard(
                        userEmail = userEmail ?: "",
                        lastSyncTime = lastSyncTime,
                        autoSaveEnabled = autoSaveEnabled
                    )
                }

                AnimatedVisibility(
                    visible = driveService == null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    DisconnectedStatusCard()
                }

                // Status Message
                AnimatedVisibility(
                    visible = statusMessage.isNotEmpty(),
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                statusMessage,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Main Actions
                if (driveService == null) {
                    // Sign In Button
                    Button(
                        onClick = { signInLauncher.launch(googleSignInClient.signInIntent) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4285F4)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Login, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Connect Google Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Auto-save Toggle
                    SyncOptionCard(
                        icon = if (autoSaveEnabled) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        title = "Auto-Save",
                        description = if (autoSaveEnabled) "Saves every 30 seconds" else "Disabled",
                        enabled = autoSaveEnabled,
                        onToggle = { autoSaveEnabled = !autoSaveEnabled }
                    )

                    // Manual Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionButton(
                            icon = Icons.Default.CloudUpload,
                            text = "Backup Now",
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        ) {
                            scope.launch {
                                isLoading = true
                                try {
                                    val data = AppData(garages, items, history)
                                    uploadToGoogleDrive(context, driveService!!, data)
                                    lastSyncTime = System.currentTimeMillis()
                                    statusMessage = "✅ Backup complete"
                                } catch (e: Exception) {
                                    statusMessage = "❌ Backup failed"
                                }
                                isLoading = false
                            }
                        }

                        ActionButton(
                            icon = Icons.Default.CloudDownload,
                            text = "Restore",
                            color = Color(0xFF8B5CF6),
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        ) {
                            scope.launch {
                                isLoading = true
                                try {
                                    val data = downloadFromGoogleDrive(driveService!!)
                                    if (data != null) {
                                        onDataRestored(data)
                                        statusMessage = "✅ Data restored"
                                    } else {
                                        statusMessage = "⚠️ No backup found"
                                    }
                                } catch (e: Exception) {
                                    statusMessage = "❌ Restore failed"
                                }
                                isLoading = false
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Danger Zone
                    DangerZoneCard(
                        onSignOut = { showSignOutDialog = true },
                        onDeleteAll = { showDeleteDialog = true }
                    )
                }

                // Info Cards
                InfoSection()
            }
        }

        // Loading Overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF4285F4),
                    modifier = Modifier.size(64.dp)
                )
            }
        }
    }
}

@Composable
private fun ConnectedStatusCard(
    userEmail: String,
    lastSyncTime: Long?,
    autoSaveEnabled: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF10B981).copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981).copy(alpha = if (autoSaveEnabled) alpha else 1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Connected",
                    color = Color(0xFF10B981),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    userEmail,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                if (lastSyncTime != null) {
                    val timeAgo = getTimeAgo(lastSyncTime)
                    Text(
                        "Last sync: $timeAgo",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DisconnectedStatusCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFBBF24).copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFBBF24)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CloudOff,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Column {
                Text(
                    "Not Connected",
                    color = Color(0xFFFBBF24),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Sign in to enable cloud backup",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun SyncOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                tint = if (enabled) Color(0xFF10B981) else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF10B981)
                )
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.15f),
            disabledContainerColor = Color.Gray.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp),
        enabled = enabled
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                null,
                tint = if (enabled) color else Color.Gray,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text,
                color = if (enabled) Color.White else Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DangerZoneCard(
    onSignOut: () -> Unit,
    onDeleteAll: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "⚠️ Danger Zone",
                color = Color(0xFFEF4444),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out")
            }

            OutlinedButton(
                onClick = onDeleteAll,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFEF4444)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DeleteForever, null)
                Spacer(Modifier.width(8.dp))
                Text("Delete All Cloud Data")
            }
        }
    }
}

@Composable
private fun InfoSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "How it works",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        InfoCard(
            icon = Icons.Default.Security,
            title = "Secure & Private",
            description = "Your data is encrypted and stored in your Google account"
        )

        InfoCard(
            icon = Icons.Default.Sync,
            title = "Automatic Backup",
            description = "Auto-saves every 30 seconds when enabled"
        )

        InfoCard(
            icon = Icons.Default.Devices,
            title = "Access Anywhere",
            description = "Restore your data on any device with your Google account"
        )
    }
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, description: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                null,
                tint = Color(0xFF60A5FA),
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

// Helper functions
private fun getTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        else -> "${hours / 24} days ago"
    }
}

private suspend fun uploadToGoogleDrive(context: Context, driveService: Drive, data: AppData) {
    withContext(Dispatchers.IO) {
        try {
            val json = Gson().toJson(data)
            val content = ByteArrayContent("application/json", json.toByteArray())

            // Check if file exists
            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name='$BACKUP_FILE_NAME'")
                .execute()

            if (result.files.isNullOrEmpty()) {
                // Create new file
                val fileMetadata = com.google.api.services.drive.model.File().apply {
                    name = BACKUP_FILE_NAME
                    parents = listOf("appDataFolder")
                }
                driveService.files().create(fileMetadata, content).execute()
            } else {
                // Update existing file
                val fileId = result.files[0].id
                driveService.files().update(fileId, null, content).execute()
            }

            Log.d("GoogleSync", "Upload successful")
        } catch (e: Exception) {
            Log.e("GoogleSync", "Upload failed", e)
            throw e
        }
    }
}

private suspend fun downloadFromGoogleDrive(driveService: Drive): AppData? {
    return withContext(Dispatchers.IO) {
        try {
            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name='$BACKUP_FILE_NAME'")
                .execute()

            if (result.files.isNullOrEmpty()) {
                return@withContext null
            }

            val fileId = result.files[0].id
            val outputStream = ByteArrayOutputStream()
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)

            val json = String(outputStream.toByteArray())
            Gson().fromJson(json, AppData::class.java)
        } catch (e: Exception) {
            Log.e("GoogleSync", "Download failed", e)
            null
        }
    }
}

private suspend fun deleteFromGoogleDrive(driveService: Drive) {
    withContext(Dispatchers.IO) {
        try {
            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name='$BACKUP_FILE_NAME'")
                .execute()

            if (!result.files.isNullOrEmpty()) {
                val fileId = result.files[0].id
                driveService.files().delete(fileId).execute()
                Log.d("GoogleSync", "Delete successful")
            }
        } catch (e: Exception) {
            Log.e("GoogleSync", "Delete failed", e)
            throw e
        }
    }
}