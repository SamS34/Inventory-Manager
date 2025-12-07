@file:SuppressLint("UnusedBoxWithConstraintsScope")

package com.samuel.inventorymanager.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// --- DATA CLASSES FOR SHARING ---

data class ShareData(
    val code: String = "",
    val data: String = "",
    val createdAt: Long = 0,
    val expiresAt: Long = 0,
    val creatorName: String = "Anonymous"
)

enum class ShareSection {
    RECEIVED,
    SHARED,
}

data class SharedGarageData(
    val code: String,
    val name: String,
    val owner: String,
    val sharedAt: Long,
    val itemCount: Int,
    val data: AppData // The full inventory data
)

data class ActiveShare(
    val code: String,
    val name: String,
    val createdAt: Long,
    val expiresAt: Long,
    val itemCount: Int
)

data class UserProfile(
    var username: String = "User",
    var email: String = "",
    var sharesCount: Int = 0,
    var receivedCount: Int = 0
)

data class ShareHistoryItem(
    val code: String,
    val timestamp: Long,
    val type: String, // "GENERATED" or "IMPORTED"
    val itemCount: Int
)

enum class ShareExpiry(val label: String, val milliseconds: Long) {
    MINUTES_30("30 Minutes", 30 * 60 * 1000L),
    HOURS_24("24 Hours", 24 * 60 * 60 * 1000L),
    DAYS_7("7 Days", 7 * 24 * 60 * 60 * 1000L),
    DAYS_30("30 Days", 30 * 24 * 60 * 60 * 1000L)
}


// --- MAIN COMPOSABLE ---

@Composable
fun ShareScreen(
    garages: List<Garage>,
    items: List<Item>,
    history: List<HistoryEntry>,
    onDataImported: (AppData) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database: DatabaseReference = remember { Firebase.database.reference }

    var selectedSection by remember { mutableStateOf(ShareSection.RECEIVED) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var generatedCode by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var importCode by remember { mutableStateOf("") }
    var shareExpiry by remember { mutableStateOf(ShareExpiry.DAYS_7) }
    var shareHistory by remember { mutableStateOf<List<ShareHistoryItem>>(emptyList()) }
    var showExpiryDialog by remember { mutableStateOf(false) }
    var sharedGarages by remember { mutableStateOf<List<SharedGarageData>>(emptyList()) }
    var myActiveShares by remember { mutableStateOf<List<ActiveShare>>(emptyList()) }
    var userProfile by remember { mutableStateOf(UserProfile()) }
    var showProfileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Load local data on start
        shareHistory = loadShareHistory(context)
        myActiveShares = loadMyActiveShares(context)
        sharedGarages = loadSharedGarages(context)
        userProfile = loadUserProfile(context)
    }

    LaunchedEffect(statusMessage) {
        if (statusMessage.isNotEmpty()) {
            delay(3000)
            statusMessage = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ShareHeader(
                onProfileClick = { showProfileDialog = true }
            )

            // Section Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionTab(
                    icon = Icons.Default.Download,
                    title = "Received",
                    count = sharedGarages.size,
                    color = androidx.compose.ui.graphics.Color(0xFF10B981),
                    isSelected = selectedSection == ShareSection.RECEIVED,
                    onClick = { selectedSection = ShareSection.RECEIVED },
                    modifier = Modifier.weight(1f)
                )
                SectionTab(
                    icon = Icons.Default.Upload,
                    title = "My Shares",
                    count = myActiveShares.size,
                    color = androidx.compose.ui.graphics.Color(0xFF8B5CF6),
                    isSelected = selectedSection == ShareSection.SHARED,
                    onClick = { selectedSection = ShareSection.SHARED },
                    modifier = Modifier.weight(1f)
                )
            }

            // Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedSection) {
                    ShareSection.RECEIVED -> ReceivedSection(
                        sharedGarages = sharedGarages,
                        importCode = importCode,
                        isLoading = isLoading,
                        onCodeChange = { importCode = it.uppercase().trim() },
                        onImport = {
                            if (importCode.isBlank()) {
                                statusMessage = "⚠️ Code cannot be empty"
                                return@ReceivedSection
                            }
                            scope.launch {
                                isLoading = true
                                try {
                                    val snapshot: DataSnapshot = database.child("shares").child(importCode).get().await()
                                    val shareData = snapshot.getValue(ShareData::class.java)
                                    if (shareData != null) {
                                        if (System.currentTimeMillis() > shareData.expiresAt) {
                                            statusMessage = "⚠️ Code has expired"
                                        } else {
                                            val appData = Gson().fromJson(shareData.data, AppData::class.java)

                                            val newSharedGarage = SharedGarageData(
                                                code = importCode,
                                                name = "${shareData.creatorName}'s Garage",
                                                owner = shareData.creatorName,
                                                sharedAt = System.currentTimeMillis(),
                                                itemCount = appData.items.size,
                                                data = appData
                                            )
                                            // Prevent duplicates
                                            sharedGarages = (listOf(newSharedGarage) + sharedGarages).distinctBy { it.code }
                                            saveSharedGarages(context, sharedGarages)

                                            val historyItem = ShareHistoryItem(code = importCode, timestamp = System.currentTimeMillis(), type = "IMPORTED", itemCount = appData.items.size)
                                            shareHistory = (listOf(historyItem) + shareHistory).distinctBy { it.code }
                                            saveShareHistory(context, shareHistory)

                                            userProfile.receivedCount = sharedGarages.size
                                            saveUserProfile(context, userProfile)

                                            statusMessage = "✅ Added to Received Garages!"
                                            importCode = ""
                                        }
                                    } else {
                                        statusMessage = "❌ Invalid code"
                                    }
                                } catch (e: Exception) {
                                    statusMessage = "❌ Import failed: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        onViewGarage = { sharedGarage ->
                            Toast.makeText(context, "Viewing ${sharedGarage.name}. \n(Functionality to be implemented)", Toast.LENGTH_SHORT).show()
                        },
                        onImportGarage = { sharedGarage ->
                            onDataImported(sharedGarage.data)
                            statusMessage = "✅ Imported to your main inventory!"
                        },
                        onDeleteGarage = { sharedGarage ->
                            sharedGarages = sharedGarages.filter { it.code != sharedGarage.code }
                            saveSharedGarages(context, sharedGarages)
                            userProfile.receivedCount = sharedGarages.size
                            saveUserProfile(context, userProfile)
                            statusMessage = "🗑️ Removed from received list"
                        }
                    )

                    ShareSection.SHARED -> MySharesSection(
                        garages = garages,
                        items = items,
                        history = history,
                        myActiveShares = myActiveShares,
                        generatedCode = generatedCode,
                        qrBitmap = qrBitmap,
                        shareExpiry = shareExpiry,
                        isLoading = isLoading,
                        onExpiryClick = { showExpiryDialog = true },
                        onGenerate = {
                            if (items.isEmpty()){
                                statusMessage = "⚠️ Cannot share an empty inventory"
                                return@MySharesSection
                            }
                            scope.launch {
                                isLoading = true
                                try {
                                    val code = generateShareCode()
                                    val appData = AppData(garages, items, history)
                                    val shareData = ShareData(
                                        code = code,
                                        data = Gson().toJson(appData),
                                        createdAt = System.currentTimeMillis(),
                                        expiresAt = System.currentTimeMillis() + shareExpiry.milliseconds,
                                        creatorName = userProfile.username
                                    )
                                    database.child("shares").child(code).setValue(shareData).await()
                                    generatedCode = code
                                    qrBitmap = generateQRCode(code)

                                    val activeShare = ActiveShare(code = code, name = "My Inventory Share", createdAt = System.currentTimeMillis(), expiresAt = shareData.expiresAt, itemCount = items.size)
                                    myActiveShares = (listOf(activeShare) + myActiveShares).distinctBy { it.code }
                                    saveMyActiveShares(context, myActiveShares)

                                    val historyItem = ShareHistoryItem(code = code, timestamp = System.currentTimeMillis(), type = "GENERATED", itemCount = items.size)
                                    shareHistory = (listOf(historyItem) + shareHistory).distinctBy { it.code }
                                    saveShareHistory(context, shareHistory)

                                    userProfile.sharesCount = myActiveShares.size
                                    saveUserProfile(context, userProfile)

                                    statusMessage = "✅ Share code created successfully!"
                                } catch (e: Exception) {
                                    statusMessage = "❌ Generation failed: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        onCopyCode = { code ->
                            copyToClipboard(context, code)
                            statusMessage = "📋 Copied code to clipboard!"
                        },
                        onCopyLink = { code ->
                            val link = "inventorymanager://share/$code"
                            copyToClipboard(context, link)
                            statusMessage = "🔗 Copied share link!"
                        },
                        onDeleteShare = { activeShare ->
                            myActiveShares = myActiveShares.filter { it.code != activeShare.code }
                            saveMyActiveShares(context, myActiveShares)
                            userProfile.sharesCount = myActiveShares.size
                            saveUserProfile(context, userProfile)

                            // Also delete from Firebase
                            database.child("shares").child(activeShare.code).removeValue()

                            statusMessage = "🗑️ Share code deleted"
                        }
                    )
                }

                // --- Status Message Display ---
                this@Column.AnimatedVisibility(
                    visible = statusMessage.isNotEmpty(),
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Text(
                            statusMessage,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                            color = androidx.compose.ui.graphics.Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = androidx.compose.ui.graphics.Color(0xFF8B5CF6),
                    modifier = Modifier.size(64.dp)
                )
            }
        }
    }

    // --- Dialogs ---

    if (showExpiryDialog) {
        AlertDialog(
            onDismissRequest = { showExpiryDialog = false },
            icon = { Icon(Icons.Default.Schedule, null) },
            title = { Text("Set Share Expiry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShareExpiry.entries.forEach { expiry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    shareExpiry = expiry
                                    showExpiryDialog = false
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(expiry.label)
                            if (shareExpiry == expiry) {
                                Icon(Icons.Default.Check, null, tint = androidx.compose.ui.graphics.Color(0xFF10B981))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showExpiryDialog = false }) { Text("Close") } }
        )
    }

    if (showProfileDialog) {
        ProfileDialog(
            userProfile = userProfile,
            onDismiss = { showProfileDialog = false },
            onSave = { newProfile ->
                userProfile = newProfile
                saveUserProfile(context, newProfile)
                showProfileDialog = false
                statusMessage = "✅ Profile updated!"
            }
        )
    }
}


// --- HELPER COMPOSABLES & FUNCTIONS ---

@Composable
private fun ShareHeader(onProfileClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(Brush.verticalGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFF6366F1), androidx.compose.ui.graphics.Color(0xFF8B5CF6))))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(shape = CircleShape, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f), modifier = Modifier.size(60.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(32.dp), tint = androidx.compose.ui.graphics.Color.White)
                    }
                }
                Column {
                    Text("Share Hub", color = androidx.compose.ui.graphics.Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Manage Your Network", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                }
            }
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.AccountCircle, "Profile", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(28.dp))
            }
        }
    }
}


@Composable
private fun ProfileDialog(
    userProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var username by remember { mutableStateOf(userProfile.username) }
    var email by remember { mutableStateOf(userProfile.email) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(48.dp)) },
        title = { Text("Your Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                HorizontalDivider()

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(userProfile.sharesCount.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF8B5CF6))
                        Text("Shares Created", fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(userProfile.receivedCount.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF10B981))
                        Text("Shares Received", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(UserProfile(username, email, userProfile.sharesCount, userProfile.receivedCount)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SectionTab(
    icon: ImageVector,
    title: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) color.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color(0xFF1E293B)),
        border = if (isSelected) BorderStroke(2.dp, color) else null,
        elevation = CardDefaults.cardElevation(if (isSelected) 8.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = if (isSelected) color else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f), modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, color = androidx.compose.ui.graphics.Color.White, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            Text("$count total", color = if (isSelected) color else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ReceivedSection(
    sharedGarages: List<SharedGarageData>,
    importCode: String,
    isLoading: Boolean,
    onCodeChange: (String) -> Unit,
    onImport: () -> Unit,
    onViewGarage: (SharedGarageData) -> Unit,
    onImportGarage: (SharedGarageData) -> Unit,
    onDeleteGarage: (SharedGarageData) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Import Card
        Card(
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B)),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = CircleShape, color = androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.2f), modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, null, tint = androidx.compose.ui.graphics.Color(0xFF10B981), modifier = Modifier.size(24.dp))
                        }
                    }
                    Column {
                        Text("Import Share Code", color = androidx.compose.ui.graphics.Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Add someone's garage to your list", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                }

                OutlinedTextField(
                    value = importCode,
                    onValueChange = onCodeChange,
                    label = { Text("Enter Code") },
                    placeholder = { Text("ABC-123") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.ui.graphics.Color(0xFF10B981),
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f),
                        cursorColor = androidx.compose.ui.graphics.Color(0xFF10B981),
                        focusedLabelColor = androidx.compose.ui.graphics.Color(0xFF10B981),
                        unfocusedLabelColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                        focusedTextColor = androidx.compose.ui.graphics.Color.White,
                        unfocusedTextColor = androidx.compose.ui.graphics.Color.White
                    )
                )

                Button(
                    onClick = onImport,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading && importCode.isNotBlank()
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Add to Received", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Received Garages List
        if (sharedGarages.isNotEmpty()){
            Text(
                "Received Garages (${sharedGarages.size})",
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (sharedGarages.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B)), shape = RoundedCornerShape(20.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(56.dp), tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f))
                    Text("No Garages Yet", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text("Import a code above to get started", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f), fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            sharedGarages.forEach { sharedGarage ->
                ReceivedGarageCard(
                    sharedGarage = sharedGarage,
                    onView = { onViewGarage(sharedGarage) },
                    onImport = { onImportGarage(sharedGarage) },
                    onDelete = { onDeleteGarage(sharedGarage) }
                )
            }
        }
    }
}

@Composable
private fun ReceivedGarageCard(
    sharedGarage: SharedGarageData,
    onView: () -> Unit,
    onImport: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(sharedGarage.name, color = androidx.compose.ui.graphics.Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, tint = androidx.compose.ui.graphics.Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        Text("By ${sharedGarage.owner}", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatTimestamp(sharedGarage.sharedAt), color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, tint = androidx.compose.ui.graphics.Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill("Items", sharedGarage.itemCount.toString(), androidx.compose.ui.graphics.Color(0xFF8B5CF6))
                StatPill("Garages", sharedGarage.data.garages.size.toString(), androidx.compose.ui.graphics.Color(0xFF3B82F6))
                StatPill("Logs", sharedGarage.data.history.size.toString(), androidx.compose.ui.graphics.Color(0xFFF59E0B))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onView,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = androidx.compose.ui.graphics.Color(0xFF3B82F6)),
                    border = BorderStroke(2.dp, androidx.compose.ui.graphics.Color(0xFF3B82F6))
                ) {
                    Icon(Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("View", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onImport,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import All", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.15f)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(label, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun MySharesSection(
    garages: List<Garage>,
    items: List<Item>,
    history: List<HistoryEntry>,
    myActiveShares: List<ActiveShare>,
    generatedCode: String?,
    qrBitmap: Bitmap?,
    shareExpiry: ShareExpiry,
    isLoading: Boolean,
    onExpiryClick: () -> Unit,
    onGenerate: () -> Unit,
    onCopyCode: (String) -> Unit,
    onCopyLink: (String) -> Unit,
    onDeleteShare: (ActiveShare) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current Inventory Stats
        Card(
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B)),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = CircleShape, color = androidx.compose.ui.graphics.Color(0xFF8B5CF6).copy(alpha = 0.2f), modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Upload, null, tint = androidx.compose.ui.graphics.Color(0xFF8B5CF6), modifier = Modifier.size(24.dp))
                        }
                    }
                    Column {
                        Text("Your Inventory", color = androidx.compose.ui.graphics.Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Ready to share with others", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatPill("Garages", garages.size.toString(), androidx.compose.ui.graphics.Color(0xFF3B82F6))
                    StatPill("Items", items.size.toString(), androidx.compose.ui.graphics.Color(0xFF8B5CF6))
                    StatPill("Logs", history.size.toString(), androidx.compose.ui.graphics.Color(0xFFF59E0B))
                }
            }
        }

        // Expiry Selection
        Card(
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.clickable { onExpiryClick() },
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = androidx.compose.ui.graphics.Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                    Column {
                        Text("Expires In", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                        Text(shareExpiry.label, color = androidx.compose.ui.graphics.Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Icon(Icons.Default.ChevronRight, null, tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f), modifier = Modifier.size(28.dp))
            }
        }

        // Generate Button
        Button(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF8B5CF6)),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading && items.isNotEmpty()
        ) {
            Icon(Icons.Default.QrCode2, null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text("Generate Share Code", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        // Generated Code Display
        AnimatedVisibility(visible = generatedCode != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (qrBitmap != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Image(bitmap = qrBitmap.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.size(220.dp))
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B)),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Your Share Code", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                        Text(generatedCode ?: "", color = androidx.compose.ui.graphics.Color(0xFF8B5CF6), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 5.sp)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { generatedCode?.let { onCopyCode(it) } },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = androidx.compose.ui.graphics.Color.White),
                        border = BorderStroke(2.dp, androidx.compose.ui.graphics.Color(0xFF8B5CF6))
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Copy Code", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { generatedCode?.let { onCopyLink(it) } },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = androidx.compose.ui.graphics.Color.White),
                        border = BorderStroke(2.dp, androidx.compose.ui.graphics.Color(0xFF3B82F6))
                    ) {
                        Icon(Icons.Default.Link, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Copy Link", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active Shares List
        if (myActiveShares.isNotEmpty()) {
            Text(
                "Active Shares (${myActiveShares.size})",
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp)
            )
            myActiveShares.forEach { share ->
                ActiveShareCard(
                    activeShare = share,
                    onCopy = { onCopyCode(share.code) },
                    onDelete = { onDeleteShare(share) }
                )
            }
        }
    }
}

@Composable
private fun ActiveShareCard(activeShare: ActiveShare, onCopy: () -> Unit, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(activeShare.name, color = androidx.compose.ui.graphics.Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(activeShare.code, color = androidx.compose.ui.graphics.Color(0xFF8B5CF6), fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${activeShare.itemCount} items", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    Text("•", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
                    Text("Expires ${formatTimestamp(activeShare.expiresAt, "MMM dd, hh:mm a")}", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onCopy, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.ContentCopy, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Delete, null, tint = androidx.compose.ui.graphics.Color(0xFFEF4444), modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}


// --- UTILITY AND DATA PERSISTENCE ---

private fun generateShareCode(): String {
    val chars = ('A'..'Z').toList()
    val nums = ('0'..'9').toList()
    val part1 = (1..3).map { chars.random() }.joinToString("")
    val part2 = (1..3).map { nums.random() }.joinToString("")
    return "$part1-$part2"
}

private fun generateQRCode(text: String): Bitmap? {
    return try {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bmp
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun formatTimestamp(timestamp: Long, pattern: String = "MMM dd, yyyy"): String {
    val sdf = SimpleDateFormat(pattern, Locale.US)
    return try {
        "on ${sdf.format(Date(timestamp))}"
    } catch (e: Exception) {
        "Invalid Date"
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("share_code", text)
    clipboard.setPrimaryClip(clip)
}


// Generic file helpers for data persistence
private inline fun <reified T> saveDataToFile(context: Context, data: T, filename: String) {
    val file = File(context.filesDir, filename)
    file.writeText(Gson().toJson(data))
}

private inline fun <reified T> loadDataFromFile(context: Context, filename: String): T? {
    val file = File(context.filesDir, filename)
    return if (file.exists()) {
        val json = file.readText()
        Gson().fromJson(json, object : TypeToken<T>() {}.type)
    } else {
        null
    }
}

// Specific implementations for each data type
private fun saveUserProfile(context: Context, profile: UserProfile) = saveDataToFile(context, profile, "user_profile.json")
private fun loadUserProfile(context: Context): UserProfile = loadDataFromFile<UserProfile>(context, "user_profile.json") ?: UserProfile()

private fun saveShareHistory(context: Context, history: List<ShareHistoryItem>) = saveDataToFile(context, history, "share_history.json")
private fun loadShareHistory(context: Context): List<ShareHistoryItem> = loadDataFromFile<List<ShareHistoryItem>>(context, "share_history.json") ?: emptyList()

private fun saveSharedGarages(context: Context, garages: List<SharedGarageData>) = saveDataToFile(context, garages, "shared_garages.json")
private fun loadSharedGarages(context: Context): List<SharedGarageData> = loadDataFromFile<List<SharedGarageData>>(context, "shared_garages.json") ?: emptyList()

private fun saveMyActiveShares(context: Context, shares: List<ActiveShare>) = saveDataToFile(context, shares, "my_active_shares.json")
private fun loadMyActiveShares(context: Context): List<ActiveShare> = loadDataFromFile<List<ActiveShare>>(context, "my_active_shares.json") ?: emptyList()