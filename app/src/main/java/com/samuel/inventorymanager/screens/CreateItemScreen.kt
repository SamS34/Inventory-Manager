package com.samuel.inventorymanager.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.samuel.inventorymanager.data.AppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

// Import AppSettings from data package
//import com.samuel.inventorymanager.data.SettingsData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateItemScreen(
    garages: List<Garage>,
    onSaveItem: (Item) -> Unit,
    viewModel: CreateItemViewModel = viewModel(),
    appSettings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showUnsavedWarning by remember { mutableStateOf(false) }
    var showCameraPreferenceBanner by remember { mutableStateOf(false) }
    var autoSaveEnabled by remember { mutableStateOf(true) }
    var lastAutoSaveTime by remember { mutableLongStateOf(0L) }

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

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempCameraUri?.let {
                viewModel.imageUris.add(it)
                viewModel.checkForChanges()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val uri = createImageUri(context)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        viewModel.imageUris.addAll(uris)
        viewModel.checkForChanges()
    }

    fun launchCameraWithPermissionCheck() {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun saveItem(showNotification: Boolean = true) {
        if (viewModel.itemName.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("⚠️ Item name is required") }
            return
        }
        onSaveItem(viewModel.getItemToSave(garages))
        viewModel.markAsSaved()
        if (showNotification) {
            scope.launch { snackbarHostState.showSnackbar("✓ Item saved successfully!") }
        }
    }

    fun handleNewItemAndCamera() {
        if (viewModel.hasUnsavedChanges) {
            showUnsavedWarning = true
        } else {
            viewModel.clearFormForNewItem(garages)
            if (appSettings.openCameraOnNewItem) {
                if (!appSettings.hasShownCameraPreference) {
                    showCameraPreferenceBanner = true
                    onSettingsChange(appSettings.copy(hasShownCameraPreference = true))
                }
                launchCameraWithPermissionCheck()
            }
        }
    }

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
            if (currentTime - lastAutoSaveTime > 3000) {
                delay(3000)
                saveItem(false)
                lastAutoSaveTime = currentTime
                scope.launch { snackbarHostState.showSnackbar("💾 Auto-saved", withDismissAction = true) }
            }
        }
    }

    if (showUnsavedWarning) {
        AlertDialog(
            onDismissRequest = { showUnsavedWarning = false },
            icon = { Icon(Icons.Default.Warning, "Warning", tint = MaterialTheme.colorScheme.error) },
            title = { Text("Unsaved Changes", fontWeight = FontWeight.Bold) },
            text = { Text("You have unsaved changes. Do you want to save before creating a new item?") },
            confirmButton = {
                Button(onClick = {
                    saveItem()
                    viewModel.clearFormForNewItem(garages)
                    showUnsavedWarning = false
                    if (appSettings.openCameraOnNewItem) {
                        launchCameraWithPermissionCheck()
                    }
                }) {
                    Text("Save & Continue")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton({ showUnsavedWarning = false }) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            viewModel.clearFormForNewItem(garages)
                            showUnsavedWarning = false
                            if (appSettings.openCameraOnNewItem) {
                                launchCameraWithPermissionCheck()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Discard")
                    }
                }
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Create / Edit Item",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val statusIcon = if (viewModel.hasUnsavedChanges) Icons.Default.Edit else Icons.Default.Check
                            val statusText = if (viewModel.hasUnsavedChanges) "Unsaved changes" else "All changes saved"
                            val statusColor = if (viewModel.hasUnsavedChanges)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.primary
                            Icon(statusIcon, statusText, tint = statusColor, modifier = Modifier.size(16.dp))
                            Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
                        }
                    }
                    OutlinedButton(
                        onClick = { autoSaveEnabled = !autoSaveEnabled },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (autoSaveEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            if (autoSaveEnabled) Icons.Default.Check else Icons.Default.Close,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Auto-save: ${if (autoSaveEnabled) "ON" else "OFF"}", modifier = Modifier.padding(start = 4.dp))
                    }
                }

                AnimatedVisibility(
                    visible = showCameraPreferenceBanner,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📸 Auto-Camera Enabled", fontWeight = FontWeight.Bold)
                            Text(
                                "The camera opens automatically on 'New Item'. You can change this in Settings.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton({
                                    onSettingsChange(
                                        appSettings.copy(
                                            openCameraOnNewItem = false,
                                            hasShownCameraPreference = true
                                        )
                                    )
                                    showCameraPreferenceBanner = false
                                }) {
                                    Text("Turn Off")
                                }
                                TextButton({
                                    onSettingsChange(appSettings.copy(hasShownCameraPreference = true))
                                    showCameraPreferenceBanner = false
                                }) {
                                    Text("Got It")
                                }
                            }
                        }
                    }
                }

                ModernCard {
                    SectionHeader("📝 Core Details")
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
                                listOf("New", "Used", "Good", "For Parts"),
                                viewModel.condition
                            ) { viewModel.condition = it }
                        }
                        Box(Modifier.weight(1f)) {
                            DropdownField(
                                "Functionality",
                                listOf("Fully Functional", "Partially Functional", "Not Functional", "Needs Testing"),
                                viewModel.functionality
                            ) { viewModel.functionality = it }
                        }
                    }
                    ModernTextField(
                        value = viewModel.description,
                        onValueChange = { viewModel.description = it },
                        label = "Description",
                        singleLine = false,
                        modifier = Modifier.height(120.dp),
                        leadingIcon = Icons.AutoMirrored.Filled.Notes
                    )
                }

                ModernCard {
                    SectionHeader("📍 Location")
                    DropdownField(
                        "Garage *",
                        garageOptions,
                        viewModel.selectedGarageName
                    ) {
                        viewModel.selectedGarageName = it
                        viewModel.selectedCabinetName = ""
                        viewModel.selectedShelfName = ""
                        viewModel.selectedBoxName = null
                    }
                    DropdownField(
                        "Cabinet",
                        cabinetOptions,
                        viewModel.selectedCabinetName
                    ) {
                        viewModel.selectedCabinetName = it
                        viewModel.selectedShelfName = ""
                        viewModel.selectedBoxName = null
                    }
                    DropdownField(
                        "Shelf",
                        shelfOptions,
                        viewModel.selectedShelfName
                    ) {
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

                ModernCard {
                    SectionHeader("📊 Attributes")
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
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) {
                            DropdownField(
                                "Size",
                                listOf("Small", "Medium", "Large"),
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

                ModernCard {
                    ModernTextField(
                        value = viewModel.webLink,
                        onValueChange = { viewModel.webLink = it },
                        label = "Web Link / URL"
                    )
                }

                ModernCard {
                    SectionHeader("📸 Images & Recognition")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            { launchCameraWithPermissionCheck() },
                            Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("📷 Camera") }
                        Button(
                            { galleryLauncher.launch("image/*") },
                            Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("📁 Upload") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            {},
                            Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("📄 OCR") }
                        OutlinedButton(
                            {},
                            Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("🤖 AI ID") }
                    }
                    if (viewModel.imageUris.isNotEmpty()) {
                        Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp)) {
                            viewModel.imageUris.forEach { uri ->
                                Box(modifier = Modifier.padding(4.dp)) {
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
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
                                            viewModel.imageUris.removeAt(viewModel.imageUris.indexOf(uri))
                                            viewModel.checkForChanges()
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .background(Color.Black.copy(0.6f), RoundedCornerShape(8.dp))
                                            .size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, "Delete image", tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = ::handleNewItemAndCamera,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("➕ New Item")
                    }
                    Button(
                        onClick = { saveItem() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = viewModel.itemName.isNotBlank()
                    ) {
                        val scale by animateFloatAsState(
                            if (viewModel.hasUnsavedChanges) 1.1f else 1f,
                            label = "s"
                        )
                        Icon(Icons.Default.Check, "Save", modifier = Modifier.scale(scale))
                        Text("Save Item", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(12.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun ModernCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
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
        label = { Text(label) },
        leadingIcon = leadingIcon?.let { { Icon(it, null) } },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
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
        onExpandedChange = { isExpanded = it }
    ) {
        OutlinedTextField(
            value = selectedValue.ifEmpty { "Select..." },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

fun createImageUri(context: Context): Uri {
    val imageFile = File(context.cacheDir, "${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
}