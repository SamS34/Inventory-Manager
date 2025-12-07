package com.samuel.inventorymanager.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.samuel.inventorymanager.data.AppSettings
import com.samuel.inventorymanager.services.AIService
import com.samuel.inventorymanager.services.OCRService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateItemScreen(
    items: List<Item>,
    garages: List<Garage>,
    onSaveItem: (Item) -> Unit,
    onUpdateItem: (Item) -> Unit,
    onDeleteItem: (Item) -> Unit,
    viewModel: CreateItemViewModel = viewModel(),
    appSettings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Image Processing States
    var showImageProcessing by remember { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Dialog States
    var showUnsavedWarning by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var duplicateItem by remember { mutableStateOf<Item?>(null) }
    var showCameraPreferenceBanner by remember { mutableStateOf(false) }

    // Processing States
    var isProcessingOCR by remember { mutableStateOf(false) }
    var isProcessingAI by remember { mutableStateOf(false) }

    // Auto-save
    var autoSaveEnabled by remember { mutableStateOf(true) }
    var lastAutoSaveTime by remember { mutableLongStateOf(0L) }

    // Dynamic options based on selection
    val garageOptions = remember(garages) { garages.map { it.name } }
    val cabinetOptions = remember(viewModel.selectedGarageName, garages) {
        garages.find { it.name == viewModel.selectedGarageName }?.cabinets?.map { it.name } ?: emptyList()
    }
    val shelfOptions = remember(viewModel.selectedGarageName, viewModel.selectedCabinetName, garages) {
        garages.find { it.name == viewModel.selectedGarageName }
            ?.cabinets?.find { it.name == viewModel.selectedCabinetName }
            ?.shelves?.map { it.name } ?: emptyList()
    }
    val boxOptions = remember(viewModel.selectedGarageName, viewModel.selectedCabinetName, viewModel.selectedShelfName, garages) {
        garages.find { it.name == viewModel.selectedGarageName }
            ?.cabinets?.find { it.name == viewModel.selectedCabinetName }
            ?.shelves?.find { it.name == viewModel.selectedShelfName }
            ?.boxes?.map { it.name } ?: emptyList()
    }

    // ==================== FUNCTIONS: Helpers ====================
    fun createImageUri(context: Context): Uri {
        val directory = File(context.cacheDir, "images")
        if (!directory.exists()) directory.mkdirs()
        val file = File(directory, "IMG_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    }

    // ==================== LAUNCHERS ====================

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            capturedImageUri = tempCameraUri
            showImageProcessing = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createImageUri(context)
            tempCameraUri = uri
            capturedImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            scope.launch { snackbarHostState.showSnackbar("📷 Camera permission required") }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.imageUris.addAll(uris)
            viewModel.checkForChanges()
            scope.launch { snackbarHostState.showSnackbar("✅ ${uris.size} image(s) added") }
        }
    }

    // ==================== FUNCTIONS ====================

    fun launchCameraWithPermissionCheck() {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun findDuplicateItem(): Item? {
        val itemName = viewModel.itemName.trim()
        val modelNumber = viewModel.modelNumber.trim()

        return items.find { existingItem ->
            val nameMatch = existingItem.name.equals(itemName, ignoreCase = true)
            val modelMatch = if (modelNumber.isNotBlank() && existingItem.modelNumber != null) {
                existingItem.modelNumber.equals(modelNumber, ignoreCase = true)
            } else false

            nameMatch || modelMatch
        }
    }

    fun saveItem(showNotification: Boolean = true) {
        if (viewModel.itemName.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("⚠️ Item name is required") }
            return
        }

        if (viewModel.selectedGarageName.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("⚠️ You must select a Garage!") }
            return
        }

        val duplicate = findDuplicateItem()
        if (duplicate != null) {
            duplicateItem = duplicate
            showDuplicateDialog = true
            return
        }

        onSaveItem(viewModel.getItemToSave(garages))
        viewModel.markAsSaved()
        if (showNotification) {
            scope.launch { snackbarHostState.showSnackbar("✅ Item created successfully!") }
        }
    }

    fun updateExistingItem(item: Item) {
        val updatedItem = viewModel.getItemToSave(garages).copy(id = item.id)
        onUpdateItem(updatedItem)
        viewModel.markAsSaved()
        scope.launch { snackbarHostState.showSnackbar("✅ Item updated successfully!") }
    }

    fun createNewItemAnyway() {
        onSaveItem(viewModel.getItemToSave(garages))
        viewModel.markAsSaved()
        scope.launch { snackbarHostState.showSnackbar("✅ New item created!") }
    }

    fun handleNewItemClick() {
        if (viewModel.hasUnsavedChanges) {
            showUnsavedWarning = true
        } else {
            viewModel.clearFormForNewItem()
            if (appSettings.openCameraOnNewItem) {
                if (!appSettings.hasShownCameraPreference) {
                    showCameraPreferenceBanner = true
                    onSettingsChange(appSettings.copy(hasShownCameraPreference = true))
                }
                launchCameraWithPermissionCheck()
            }
        }
    }

    fun performOCR() {
        if (viewModel.imageUris.isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar("⚠️ Please add an image first!") }
            return
        }

        scope.launch {
            isProcessingOCR = true
            try {
                val ocrService = OCRService(context)
                val result = ocrService.performOCR(viewModel.imageUris.first(), appSettings.ocrSettings)

                val lines = result.text.lines().filter { it.isNotBlank() }
                if (lines.isNotEmpty()) {
                    viewModel.itemName = lines.firstOrNull() ?: ""
                    if (lines.size > 1) viewModel.modelNumber = lines[1]
                    if (lines.size > 2) viewModel.description = lines.drop(2).joinToString("\n")
                }

                snackbarHostState.showSnackbar("✅ OCR Complete! (${result.provider})")
                viewModel.checkForChanges()
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("❌ OCR Failed: ${e.message}")
            } finally {
                isProcessingOCR = false
            }
        }
    }

    fun performAI() {
        if (viewModel.imageUris.isEmpty() && viewModel.itemName.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("⚠️ Please add an image or item name first!") }
            return
        }
        scope.launch {
            isProcessingAI = true
            try {
                val aiService = AIService(context)
                val result: AIService.AIAnalysisResult =
                    if (viewModel.imageUris.isNotEmpty()) {
                        aiService.analyzeItemFromBitmap(
                            android.graphics.BitmapFactory.decodeStream(
                                context.contentResolver.openInputStream(viewModel.imageUris.first())
                            )!!
                        )
                    } else {
                        // Fixed: Provide all required parameters
                        AIService.AIAnalysisResult(
                            itemName = null,
                            confidence = 0.0,
                            modelNumber = null,
                            description = "Please add an image for AI analysis",
                            estimatedPrice = null,
                            condition = null,
                            sizeCategory = null,
                            dimensions = null,
                            rawText = null
                        )
                    }
                result.itemName?.let { viewModel.itemName = it }
                result.modelNumber?.let { viewModel.modelNumber = it }
                result.description?.let { viewModel.description = it }
                result.condition?.let { viewModel.condition = it }
                result.sizeCategory?.let { viewModel.sizeCategory = it }
                result.estimatedPrice?.let { price ->
                    viewModel.minPrice = price.toString()
                    viewModel.maxPrice = (price * 1.2).toString()
                }
                result.dimensions?.let { viewModel.dimensions = it }

                snackbarHostState.showSnackbar("✅ AI Analysis Complete!")
                viewModel.checkForChanges()
            } catch (e: Exception) {
                Log.e("performAI", "AI processing failed", e)
                snackbarHostState.showSnackbar("AI processing failed: ${e.localizedMessage}")
            } finally {
                isProcessingAI = false
            }
        }
    }


    // ==================== AUTO-SAVE ====================

    LaunchedEffect(
        viewModel.itemName, viewModel.modelNumber, viewModel.description, viewModel.webLink,
        viewModel.condition, viewModel.functionality, viewModel.quantity, viewModel.minPrice,
        viewModel.maxPrice, viewModel.weight, viewModel.sizeCategory, viewModel.dimensions,
        viewModel.imageUris.size, viewModel.selectedGarageName, viewModel.selectedCabinetName,
        viewModel.selectedShelfName, viewModel.selectedBoxName
    ) {
        viewModel.checkForChanges()
        if (autoSaveEnabled && viewModel.hasUnsavedChanges && viewModel.itemName.isNotBlank()) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastAutoSaveTime > 8000) {
                delay(2000)
                saveItem(false)
                lastAutoSaveTime = currentTime
                scope.launch {
                    snackbarHostState.showSnackbar("💾 Auto-saved", withDismissAction = true)
                }
            }
        }
    }

    // ==================== IMAGE PROCESSING DIALOG ====================

    if (showImageProcessing && capturedImageUri != null) {
        Dialog(
            onDismissRequest = { showImageProcessing = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            ImageProcessingScreen(
                imageUri = capturedImageUri!!,
                onImageProcessed = { uri, aiResult ->
                    viewModel.imageUris.add(uri)
                    aiResult.itemName?.let { viewModel.itemName = it }
                    aiResult.modelNumber?.let { viewModel.modelNumber = it }
                    aiResult.description?.let {
                        viewModel.description = if (viewModel.description.isBlank()) it else "${viewModel.description}\n\n$it"
                    }
                    aiResult.condition?.let { viewModel.condition = it }
                    aiResult.sizeCategory?.let { viewModel.sizeCategory = it }
                    aiResult.estimatedPrice?.let {
                        viewModel.minPrice = it.toString()
                        viewModel.maxPrice = (it * 1.2).toString()
                    }
                    viewModel.checkForChanges()
                    showImageProcessing = false
                    scope.launch { snackbarHostState.showSnackbar("✨ AI auto-filled item details!") }
                },
                onCancel = { showImageProcessing = false },
                aiService = AIService(context)
            )
        }
    }

    // ==================== DUPLICATE DIALOG ====================

    if (showDuplicateDialog && duplicateItem != null) {
        ModernAlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            icon = Icons.Default.Warning,
            iconTint = Color(0xFFFBBF24),
            title = "Duplicate Item Found",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "An item with this name already exists:",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            InfoRow("Name:", duplicateItem!!.name)
                            InfoRow("Qty:", "${duplicateItem!!.quantity}")
                            InfoRow("Condition:", duplicateItem!!.condition)
                        }
                    }
                    Text(
                        "Would you like to update this item or create a new one?",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        updateExistingItem(duplicateItem!!)
                        showDuplicateDialog = false
                        duplicateItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Update, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Update Existing", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showDuplicateDialog = false; duplicateItem = null }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                    Button(
                        onClick = {
                            createNewItemAnyway()
                            showDuplicateDialog = false
                            duplicateItem = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Create New", fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }

    // ==================== UNSAVED WARNING DIALOG ====================

    if (showUnsavedWarning) {
        ModernAlertDialog(
            onDismissRequest = { showUnsavedWarning = false },
            icon = Icons.Default.Warning,
            iconTint = Color(0xFFFBBF24),
            title = "Unsaved Changes",
            content = {
                Text(
                    "You have unsaved changes. Do you want to save before creating a new item?",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        saveItem()
                        viewModel.clearFormForNewItem()
                        showUnsavedWarning = false
                        if (appSettings.openCameraOnNewItem) launchCameraWithPermissionCheck()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save & Continue", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton({ showUnsavedWarning = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                    TextButton(
                        onClick = {
                            viewModel.clearFormForNewItem()
                            showUnsavedWarning = false
                            if (appSettings.openCameraOnNewItem) launchCameraWithPermissionCheck()
                        }
                    ) {
                        Text("Discard", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }

    // ==================== DELETE CONFIRMATION DIALOG ====================

    if (showDeleteConfirmDialog) {
        ModernAlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = Icons.Default.Delete,
            iconTint = Color(0xFFEF4444),
            title = "Delete This Item?",
            content = {
                Text(
                    "This will permanently delete this item from your inventory. This action cannot be undone.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val itemToDelete = viewModel.getItemToSave(garages)
                        onDeleteItem(itemToDelete)
                        viewModel.clearFormForNewItem()
                        showDeleteConfirmDialog = false
                        scope.launch { snackbarHostState.showSnackbar("🗑️ Item deleted successfully") }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete Permanently", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }

    // ==================== MAIN UI ====================

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A0A0A)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 200.dp)
            ) {
                // Header with gradient
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                                )
                            )
                    )
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Create Item",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Add new items to your inventory",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )

                        Spacer(Modifier.height(20.dp))

                        // Status card
                        StatusCard(viewModel = viewModel)
                    }
                }

                // Camera Preference Banner
                AnimatedVisibility(
                    visible = showCameraPreferenceBanner,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    CameraPreferenceBanner(
                        onDismiss = { showCameraPreferenceBanner = false }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Main Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Core Details
                    ModernCard(title = "📝 Item Details") {
                        ModernTextField(
                            value = viewModel.itemName,
                            onValueChange = { viewModel.itemName = it },
                            label = "Item Name *",
                            leadingIcon = Icons.Default.Edit
                        )
                        ModernTextField(
                            value = viewModel.modelNumber,
                            onValueChange = { viewModel.modelNumber = it },
                            label = "Model Number",
                            leadingIcon = Icons.Default.ConfirmationNumber
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.weight(1f)) {
                                DropdownField(
                                    "Condition",
                                    listOf("New", "Like New", "Good", "Fair", "Poor"),
                                    viewModel.condition
                                ) { viewModel.condition = it }
                            }
                            Box(Modifier.weight(1f)) {
                                DropdownField(
                                    "Status",
                                    listOf("Fully Functional", "Partially Functional", "Not Functional", "Needs Testing"),
                                    viewModel.functionality
                                ) { viewModel.functionality = it }
                            }
                        }
                    }

                    // Description
                    ModernCard(title = "📄 Description") {
                        ModernTextField(
                            value = viewModel.description,
                            onValueChange = { viewModel.description = it },
                            label = "Description / Notes",
                            singleLine = false,
                            modifier = Modifier.height(120.dp),
                            leadingIcon = Icons.AutoMirrored.Filled.Notes
                        )
                    }

                    // Location
                    ModernCard(title = "📍 Storage Location") {
                        DropdownField("Garage *", garageOptions, viewModel.selectedGarageName) {
                            viewModel.selectedGarageName = it
                            viewModel.selectedCabinetName = ""
                            viewModel.selectedShelfName = ""
                            viewModel.selectedBoxName = null
                        }
                        DropdownField("Cabinet", cabinetOptions, viewModel.selectedCabinetName) {
                            viewModel.selectedCabinetName = it
                            viewModel.selectedShelfName = ""
                            viewModel.selectedBoxName = null
                        }
                        DropdownField("Shelf", shelfOptions, viewModel.selectedShelfName) {
                            viewModel.selectedShelfName = it
                            viewModel.selectedBoxName = null
                        }
                        DropdownField(
                            "Box/Bin (Optional)",
                            listOf("None") + boxOptions,
                            viewModel.selectedBoxName ?: "None"
                        ) {
                            viewModel.selectedBoxName = if (it == "None") null else it
                        }
                    }

                    // Quantity & Physical Attributes
                    ModernCard(title = "📦 Quantity & Attributes") {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ModernTextField(
                                value = viewModel.quantity,
                                onValueChange = { viewModel.quantity = it },
                                label = "Quantity",
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f)
                            )
                            ModernTextField(
                                value = viewModel.weight,
                                onValueChange = { viewModel.weight = it },
                                label = "Weight (lbs)",
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.weight(1f)) {
                                DropdownField(
                                    "Size",
                                    listOf("Small", "Medium", "Large", "Extra Large"),
                                    viewModel.sizeCategory
                                ) { viewModel.sizeCategory = it }
                            }
                            ModernTextField(
                                value = viewModel.dimensions,
                                onValueChange = { viewModel.dimensions = it },
                                label = "Dimensions (LxWxH)",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Pricing
                    ModernCard(title = "💰 Pricing") {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ModernTextField(
                                value = viewModel.minPrice,
                                onValueChange = { viewModel.minPrice = it },
                                label = "Min Price ($)",
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f)
                            )
                            ModernTextField(
                                value = viewModel.maxPrice,
                                onValueChange = { viewModel.maxPrice = it },
                                label = "Max Price ($)",
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Web Link
                    ModernCard(title = "🔗 Product Link") {
                        ModernTextField(
                            value = viewModel.webLink,
                            onValueChange = { viewModel.webLink = it },
                            label = "Web Link / Product URL"
                        )
                    }

                    // Images
                    ModernCard(title = "📸 Images") {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                { launchCameraWithPermissionCheck() },
                                Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Camera", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                { galleryLauncher.launch("image/*") },
                                Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                Text("📁 Upload", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (viewModel.imageUris.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "${viewModel.imageUris.size} image(s) attached",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                            Row(
                                Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                viewModel.imageUris.forEach { uri ->
                                    Box {
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            elevation = CardDefaults.cardElevation(4.dp)
                                        ) {
                                            Image(
                                                rememberAsyncImagePainter(uri),
                                                "Selected",
                                                Modifier.size(120.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.imageUris.remove(uri)
                                                viewModel.checkForChanges()
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .background(Color(0xFFEF4444), CircleShape)
                                                .size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                "Delete",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                }
            }

            // Loading Overlay
            if (isProcessingOCR || isProcessingAI) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "processing_pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = EaseInOut),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )

                        Icon(
                            if (isProcessingOCR) Icons.Default.DocumentScanner else Icons.Default.Psychology,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .graphicsLayer(scaleX = scale, scaleY = scale),
                            tint = if (isProcessingOCR) Color(0xFF6366F1) else Color(0xFF8B5CF6)
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                if (isProcessingOCR) "📸 Processing OCR..." else "🤖 Processing AI...",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isProcessingOCR) "Extracting text from image" else "Analyzing item details",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }

                        CircularProgressIndicator(
                            color = if (isProcessingOCR) Color(0xFF6366F1) else Color(0xFF8B5CF6),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            // Bottom Action Bar
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                shadowElevation = 12.dp,
                color = Color(0xFF1A1A1A)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // OCR and AI buttons row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { performOCR() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Icon(Icons.Default.DocumentScanner, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("OCR Scan", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { performAI() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Icon(Icons.Default.Psychology, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("AI Fill", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Main action buttons row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { handleNewItemClick() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("New", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { saveItem() },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(16.dp),
                            enabled = viewModel.itemName.isNotBlank() && viewModel.selectedGarageName.isNotBlank()
                        ) {
                            val scale by animateFloatAsState(
                                if (viewModel.hasUnsavedChanges) 1.15f else 1f,
                                label = "save_scale"
                            )
                            Icon(
                                Icons.Default.SaveAs,
                                "Save",
                                modifier = Modifier
                                    .scale(scale)
                                    .size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Save", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Snackbar at BOTTOM
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 180.dp, start = 16.dp, end = 16.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(14.dp),
                    containerColor = Color(0xFF2A2A2A),
                    contentColor = Color.White
                )
            }
        }
    }
}

// ==================== COMPOSABLE COMPONENTS ====================

@Composable
private fun StatusCard(viewModel: CreateItemViewModel) {
    val isNameEmpty = viewModel.itemName.isBlank()
    val isLocationEmpty = viewModel.selectedGarageName.isBlank()

    when {
        isNameEmpty || isLocationEmpty -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        "Warning",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            "⚠️ Required Fields Missing",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            if (isNameEmpty && isLocationEmpty) "Item name and location required"
                            else if (isNameEmpty) "Item name required"
                            else "Storage location required",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        viewModel.hasUnsavedChanges -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFBBF24).copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        "Unsaved",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            "📝 Unsaved Changes",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "Don't forget to save your work",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        else -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        "Saved",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            "✅ All Saved",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "Your item is up to date",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreferenceBanner(
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF6366F1).copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                null,
                tint = Color(0xFF6366F1),
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "📸 Auto-Camera Active",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    "Camera opens on 'New Item'",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ModernCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                title,
                color = Color(0xFF6366F1),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            content()
        }
    }
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        leadingIcon = leadingIcon?.let { { Icon(it, null, modifier = Modifier.size(20.dp), tint = Color(0xFF6366F1)) } },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF6366F1),
            unfocusedBorderColor = Color(0xFF3A3A3A),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color(0xFF6366F1),
            focusedLabelColor = Color(0xFF6366F1),
            unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    selectedValue: String,
    onValueChange: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = !isExpanded }
    ) {
        OutlinedTextField(
            value = selectedValue.ifEmpty { "Select..." },
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 13.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6366F1),
                unfocusedBorderColor = Color(0xFF3A3A3A),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color(0xFF6366F1),
                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.background(Color(0xFF2A2A2A))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 13.sp, color = Color.White) },
                    onClick = {
                        onValueChange(option)
                        isExpanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun ModernAlertDialog(
    onDismissRequest: () -> Unit,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    content: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = content,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        containerColor = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
        Text(
            value,
            color = Color.White,
            fontSize = 13.sp
        )
    }
}