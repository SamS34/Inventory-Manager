package com.samuel.inventorymanager.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import com.samuel.inventorymanager.data.AppSettings
import com.samuel.inventorymanager.data.AppTheme
import com.samuel.inventorymanager.services.OCRService
import com.samuel.inventorymanager.ui.theme.AppThemeType
import com.samuel.inventorymanager.ui.theme.InventoryManagerTheme
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStreamReader
import java.util.UUID

// ViewModel Factory
class CreateItemViewModelFactory(
    private val application: android.app.Application,
    private val ocrService: OCRService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateItemViewModel::class.java)) {
            return CreateItemViewModel(application, ocrService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Navigation Routes
sealed class AppScreen(val route: String) {
    object Dashboard : AppScreen("dashboard")
    object Locations : AppScreen("locations")
    object Search : AppScreen("search")
    object Overview : AppScreen("overview")
    object Images : AppScreen("images")
    object History : AppScreen("history")
    object Settings : AppScreen("settings")
    object Sync : AppScreen("sync")
    object Help : AppScreen("help")
    object CreateItem : AppScreen("create_item")
    object ShareScreen : AppScreen("share")

    object Drive : AppScreen("google sync") // ADD THIS
    // Image flow screens
    object ImageEdit : AppScreen("image_edit")
    object AIProcessing : AppScreen("ai_processing")
    object AIResults : AppScreen("ai_results")
    object ImageDetail : AppScreen("image_detail/{itemId}") {
        fun createRoute(itemId: String) = "image_detail/$itemId"
    }
}

data class NavGridItem(
    val title: String,
    val icon: ImageVector,
    val screen: AppScreen
)

val navigationItems = listOf(
    NavGridItem("Dashboard", Icons.Default.Dashboard, AppScreen.Dashboard),
    NavGridItem("Create Item", Icons.Default.Inventory2, AppScreen.CreateItem),
    NavGridItem("Locations", Icons.Default.LocationOn, AppScreen.Locations),
    NavGridItem("Search", Icons.Default.Search, AppScreen.Search),
    NavGridItem("Overview", Icons.AutoMirrored.Filled.List, AppScreen.Overview),
    NavGridItem("Images", Icons.Default.Image, AppScreen.Images),
    NavGridItem("Share Screen", Icons.Default.Share, AppScreen.ShareScreen),
    NavGridItem("History", Icons.Default.History, AppScreen.History),
    NavGridItem("Google Sync" , Icons.Default.CloudUpload, AppScreen.Drive),
    NavGridItem("Settings", Icons.Default.Settings, AppScreen.Settings),
    NavGridItem("Sync (COMING SOON)", Icons.Default.Sync, AppScreen.Sync),
    NavGridItem("Help", Icons.AutoMirrored.Filled.HelpOutline, AppScreen.Help)
)

sealed class DialogState {
    object Closed : DialogState()
    object AddGarage : DialogState()
    data class AddCabinet(val garageId: String) : DialogState()
    data class AddShelf(val cabinetId: String) : DialogState()
    data class AddBox(val shelfId: String) : DialogState()
    data class RenameLocation(val id: String, val oldName: String, val type: String) : DialogState()
    data class DeleteLocation(val id: String, val name: String, val type: String) : DialogState()
    object ClearHistory : DialogState()
}

@Composable
fun mapAppThemeToAppThemeType(appTheme: AppTheme): AppThemeType {
    return when (appTheme) {
        AppTheme.LIGHT -> AppThemeType.LIGHT
        AppTheme.DARK -> AppThemeType.DARK
        AppTheme.SYSTEM -> if (isSystemInDarkTheme()) AppThemeType.DARK else AppThemeType.LIGHT
        AppTheme.DRACULA -> AppThemeType.DRACULA
        AppTheme.VAMPIRE -> AppThemeType.VAMPIRE
        AppTheme.OCEAN -> AppThemeType.OCEAN
        AppTheme.FOREST -> AppThemeType.FOREST
        AppTheme.SUNSET -> AppThemeType.SUNSET
        AppTheme.CYBERPUNK -> AppThemeType.CYBERPUNK
        AppTheme.NEON -> AppThemeType.NEON
        AppTheme.CUSTOM -> AppThemeType.LIGHT
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // Factory for ViewModel injection
    val ocrService = remember { OCRService(context) }
    val createItemViewModel: CreateItemViewModel = viewModel(
        factory = CreateItemViewModelFactory(context.applicationContext as android.app.Application, ocrService)
    )

    // Settings
    var appSettings by remember { mutableStateOf(AppSettings()) }

    LaunchedEffect(Unit) {
        appSettings = loadSettingsFromFile(context)
    }

    val onSettingsChange: (AppSettings) -> Unit = { newSettings ->
        appSettings = newSettings
        saveSettingsToFile(context, newSettings)
    }

    val themeType = when {
        appSettings.customTheme != null -> AppThemeType.LIGHT
        else -> mapAppThemeToAppThemeType(appSettings.theme)
    }

    val fontScale = appSettings.customTheme?.fontSizeScale ?: appSettings.fontSize.scale

    InventoryManagerTheme(themeType = themeType, fontScale = fontScale) {
        // Data State
        val garages = remember { mutableStateListOf<Garage>() }
        val items = remember { mutableStateListOf<Item>() }
        val history = remember { mutableStateListOf<HistoryEntry>() }
        var dialogState by remember { mutableStateOf<DialogState>(DialogState.Closed) }
        var saveCounter by remember { mutableIntStateOf(0) }


        // Image flow state
        var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
        var editedBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
        var aiAnalysisResult by remember { mutableStateOf<AIAnalysisResult?>(null) }
        var showAddItemSheet by remember { mutableStateOf(false) }

        // Load data
        LaunchedEffect(Unit) {
            loadDataFromFile(context, garages, items, history)
        }

        // Auto-save
        LaunchedEffect(saveCounter) {
            if (saveCounter > 0) {
                saveDataToFile(context, AppData(garages.toList(), items.toList(), history.toList()))
            }
        }

        val triggerSave = { saveCounter++ }

        // Item operations
        val onSaveItem: (Item) -> Unit = { item ->
            val index = items.indexOfFirst { it.id == item.id }
            if (index != -1) {
                items[index] = item
                history.add(0, HistoryEntry(
                    id = "hist_${System.currentTimeMillis()}",
                    itemId = item.id,
                    itemName = item.name,
                    actionType = "Updated",
                    description = "Item details were modified."
                ))
            } else {
                items.add(item)
                history.add(0, HistoryEntry(
                    id = "hist_${System.currentTimeMillis()}",
                    itemId = item.id,
                    itemName = item.name,
                    actionType = "Added",
                    description = "A new item was created."
                ))
            }
            triggerSave()
        }

        val onUpdateItem: (Item) -> Unit = { updatedItem ->
            val index = items.indexOfFirst { it.id == updatedItem.id }
            if (index != -1) {
                items[index] = updatedItem
                history.add(0, HistoryEntry(
                    id = "hist_${System.currentTimeMillis()}",
                    itemId = updatedItem.id,
                    itemName = updatedItem.name,
                    actionType = "Updated",
                    description = "Item was updated."
                ))
                triggerSave()
            }
        }

        val onDeleteItem: (Item) -> Unit = { itemToDelete ->
            items.remove(itemToDelete)
            history.add(0, HistoryEntry(
                id = "hist_${System.currentTimeMillis()}",
                itemId = itemToDelete.id,
                itemName = itemToDelete.name,
                actionType = "Deleted",
                description = "Item was permanently deleted."
            ))
            triggerSave()
        }
        val onDeleteHistoryEntry: (HistoryEntry) -> Unit = { entry ->
            history.remove(entry)
            triggerSave()
        }


        // Camera & Gallery Launchers
        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && capturedImageUri != null) {
                navController.navigate(AppScreen.ImageEdit.route)
            }
        }

        val galleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                capturedImageUri = it
                navController.navigate(AppScreen.ImageEdit.route)
            }
        }

        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                val uri = createImageUri(context)
                capturedImageUri = uri
                cameraLauncher.launch(uri)
            }
        }

        // Drawer state
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        val currentAppData = AppData(garages = garages, items = items, history = history)
        // --- CALLBACK FUNCTIONS FOR SETTINGS SCREEN ---
        val onDataChange: (AppData) -> Unit = { importedData ->
            // This runs when you import a file from settings
            garages.clear()
            items.clear()
            history.clear()

            garages.addAll(importedData.garages)
            items.addAll(importedData.items)
            history.addAll(importedData.history)

            triggerSave() // Save the new imported data
        }

        val onClearAllData: () -> Unit = {
            // This runs when you click "Clear All Data" in settings
            garages.clear()
            items.clear()
            history.clear()

            // Also delete the physical file
            val file = File(context.filesDir, "app_data.json")
            if (file.exists()) {
                file.delete()
            }

            triggerSave() // Save the empty state
        }

        // Hide bottom bar on image flow screens
        val hideBottomBar = currentRoute in listOf(
            AppScreen.ImageEdit.route,
            AppScreen.AIProcessing.route,
            AppScreen.AIResults.route
        )

        // Dialogs
        HandleDialogs(
            dialogState = dialogState,
            garages = garages,
            onDismiss = {
                dialogState = DialogState.Closed
                triggerSave()
            },
            onClearHistory = {
                history.clear()
                dialogState = DialogState.Closed
                triggerSave()
            }
        )

        // Main UI
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Inventory Manager",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(navigationItems) { item ->
                                GridNavigationButton(
                                    item = item,
                                    isSelected = currentRoute == item.screen.route
                                ) {
                                    // Auto-open camera when clicking "Items"
                                    if (item.screen == AppScreen.CreateItem) {
                                        scope.launch { drawerState.close() }
                                        showAddItemSheet = true  // This opens camera/gallery sheet
                                    } else {
                                        navController.navigate(item.screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                        scope.launch { drawerState.close() }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    if (!hideBottomBar) {
                        TopAppBar(
                            title = {
                                Text(
                                    getScreenTitle(currentRoute),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            navigationIcon = {
                                IconButton({ scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, "Menu")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                },
                bottomBar = {
                    if (!hideBottomBar) {
                        ModernBottomNavBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onAddItem = { showAddItemSheet = true }
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    NavHost(navController = navController, startDestination = AppScreen.Dashboard.route) {
                        composable(AppScreen.Dashboard.route) {
                            DashboardScreen(items, garages)
                        }
                        composable(AppScreen.ShareScreen.route) {
                            ShareScreen(
                                garages = garages,
                                items = items,
                                history = history,
                                onDataImported = { importedData ->
                                    garages.clear()
                                    items.clear()
                                    history.clear()

                                    garages.addAll(importedData.garages)
                                    items.addAll(importedData.items)
                                    history.addAll(importedData.history)

                                    triggerSave()

                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(AppScreen.Drive.route) {
                            GoogleSyncScreen(
                                garages = garages,
                                items = items,
                                history = history,
                                onDataRestored = { restoredData ->
                                    garages.clear()
                                    garages.addAll(restoredData.garages)
                                    items.clear()
                                    items.addAll(restoredData.items)
                                    history.clear()
                                    history.addAll(restoredData.history)
                                    triggerSave()
                                }
                            )
                        }

                        composable(AppScreen.Locations.route) {
                            LocationsScreen(
                                garages = garages,
                                items = items,
                                onAddGarage = { dialogState = DialogState.AddGarage },
                                onAddCabinet = { gid -> dialogState = DialogState.AddCabinet(gid) },
                                onAddShelf = { cid -> dialogState = DialogState.AddShelf(cid) },
                                onAddBox = { sid -> dialogState = DialogState.AddBox(sid) },
                                onRenameLocation = { id, old, type ->
                                    dialogState = DialogState.RenameLocation(id, old, type)
                                },
                                onDeleteLocation = { id, name, type ->
                                    dialogState = DialogState.DeleteLocation(id, name, type)
                                }
                            )
                        }

                        composable(AppScreen.CreateItem.route) {
                            CreateItemScreen(
                                items = items,
                                garages = garages,
                                onSaveItem = onSaveItem,
                                onUpdateItem = onUpdateItem,
                                viewModel = createItemViewModel,
                                appSettings = appSettings,
                                onDeleteItem = onDeleteItem,
                                onSettingsChange = onSettingsChange,
                                onBack = { // <-- THIS IS THE LOGIC FOR THE NEW PARAMETER
                                    // This clears the viewmodel state AND navigates back.
                                    // This is the key to fixing the navigation lock.
                                    createItemViewModel.clearFormForNewItem()
                                    navController.popBackStack()
                                }
                            )
                        }


                        composable(AppScreen.Overview.route) {
                            OverviewScreen(items, garages)
                        }

                        composable(AppScreen.Search.route) {
                            SearchScreen(
                                items = items,
                                garages = garages,
                                onEditItem = { item ->
                                    createItemViewModel.loadItemForEditing(item, garages)
                                    navController.navigate(AppScreen.CreateItem.route)
                                },
                                onDeleteItem = { item ->
                                    onDeleteItem(item)
                                },
                                onDuplicateItem = { item ->
                                    val duplicatedItem = item.copy(
                                        id = UUID.randomUUID().toString(),
                                        name = "${item.name} (Copy)"
                                    )
                                    onSaveItem(duplicatedItem)
                                },
                                onShareItem = { item ->
                                    shareItem(context, item, garages)
                                }
                            )
                        }

                        composable(AppScreen.Settings.route) {
                            SettingsScreen(
                                currentSettings = appSettings,
                                onSettingsChange = { newSettings -> appSettings = newSettings },

                                // Add these three required parameters
                                currentData = currentAppData,
                                onDataChange = onDataChange,
                                onClearAllData = onClearAllData
                            )
                        }

                        composable(AppScreen.Help.route) {
                            HelpScreen()
                        }

                        composable(AppScreen.Images.route) {
                            // This is the "folders" screen from my first reply.
                            // It will call the ImageDetailScreen when a folder is clicked.
                            ImagesScreen(items) { item ->
                                navController.navigate(AppScreen.ImageDetail.createRoute(item.id))
                            }
                        }

                        composable(AppScreen.History.route) {
                            HistoryScreen(
                                history = history,
                                items = items,
                                onItemClick = { targetItem ->
                                    createItemViewModel.loadItemForEditing(targetItem, garages)
                                    navController.navigate(AppScreen.CreateItem.route)
                                },
                                onClearHistory = {
                                    dialogState = DialogState.ClearHistory
                                },
                                onDeleteHistoryEntry = onDeleteHistoryEntry
                            )
                        }


                        composable(AppScreen.Sync.route) {
                            PlaceholderScreen("Sync")
                        }

                        composable(AppScreen.ImageDetail.route) { backStackEntry ->
                            val itemId = backStackEntry.arguments?.getString("itemId")
                            val item = items.find { it.id == itemId }

                            if (item != null) {
                                // This is where we call YOUR screen
                                ImageDetailScreen(
                                    item = item,
                                    onBack = { navController.popBackStack() },
                                    onNavigateToEditor = { itemToEdit ->
                                        createItemViewModel.loadItemForEditing(itemToEdit, garages)
                                        navController.navigate(AppScreen.CreateItem.route)
                                    }
                                )
                            }
                        }
                        // Image Edit Screen
                        composable(AppScreen.ImageEdit.route) {
                            capturedImageUri?.let { uri ->
                                ImageEditScreen(
                                    imageUri = uri,
                                    onNext = { bitmaps, choice ->
                                        // Store ALL bitmaps
                                        editedBitmaps = bitmaps

                                        when (choice) {
                                            ProcessingChoice.MANUAL -> {
                                                createItemViewModel.clearFormForNewItem()

                                                // Save ALL bitmaps
                                                bitmaps.forEach { bitmap ->
                                                    val savedUri = saveBitmapToUri(context, bitmap)
                                                    if(!createItemViewModel.imageUris.contains(savedUri)) {
                                                        createItemViewModel.imageUris.add(savedUri)
                                                    }
                                                }

                                                navController.navigate(AppScreen.CreateItem.route) {
                                                    popUpTo(AppScreen.Dashboard.route)
                                                }
                                            }
                                            ProcessingChoice.AI_OCR -> {
                                                navController.navigate(AppScreen.AIProcessing.route)
                                            }
                                        }
                                    },
                                    onCancel = {
                                        capturedImageUri = null
                                        editedBitmaps = emptyList()
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }

// AI Processing Screen - Process FIRST bitmap for AI
                        composable(AppScreen.AIProcessing.route) {
                            val firstBitmap = editedBitmaps.firstOrNull()
                            firstBitmap?.let { bitmap ->
                                AIProcessingScreen(
                                    bitmap = bitmap,
                                    onComplete = { result ->
                                        aiAnalysisResult = result
                                        navController.navigate(AppScreen.AIResults.route)
                                    },
                                    onError = { _ ->
                                        scope.launch {
                                            navController.popBackStack()
                                        }
                                    }
                                )
                            }
                        }

                        composable(AppScreen.AIResults.route) {
                            val firstBitmap = editedBitmaps.firstOrNull()
                            firstBitmap?.let { bitmap ->
                                aiAnalysisResult?.let { result ->
                                    AIResultsScreen(
                                        bitmap = bitmap,
                                        result = result,
                                        onBackToEdit = {
                                            navController.navigate(AppScreen.ImageEdit.route) {
                                                popUpTo(AppScreen.ImageEdit.route) { inclusive = true }
                                            }
                                        },
                                        onSaveAndContinue = { finalResult ->
                                            createItemViewModel.clearFormForNewItem()

                                            // Prefill from AI result
                                            finalResult.itemName?.let { createItemViewModel.itemName = it }
                                            finalResult.description?.let { createItemViewModel.description = it }
                                            finalResult.modelNumber?.let { createItemViewModel.modelNumber = it }
                                            finalResult.dimensions?.let { createItemViewModel.dimensions = it }
                                            finalResult.estimatedPrice?.let { price ->
                                                createItemViewModel.minPrice = price.toString()
                                                createItemViewModel.maxPrice = (price * 1.2).toString()
                                            }

                                            // Save ALL edited bitmaps
                                            editedBitmaps.forEach { bmp ->
                                                val savedUri = saveBitmapToUri(context, bmp)
                                                if(!createItemViewModel.imageUris.contains(savedUri)) {
                                                    createItemViewModel.imageUris.add(savedUri)
                                                }
                                            }

                                            navController.navigate(AppScreen.CreateItem.route) {
                                                popUpTo(AppScreen.Dashboard.route)
                                            }

                                            capturedImageUri = null
                                            editedBitmaps = emptyList()
                                            aiAnalysisResult = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Item Bottom Sheet
        if (showAddItemSheet) {
            AddItemBottomSheet(
                onDismiss = { showAddItemSheet = false },
                onCameraClick = {
                    showAddItemSheet = false
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val uri = createImageUri(context)
                        capturedImageUri = uri
                        cameraLauncher.launch(uri)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onGalleryClick = {
                    showAddItemSheet = false
                    galleryLauncher.launch("image/*")
                }
            )
        }
    }
}

// Modern Bottom Navigation Bar
@Composable
fun ModernBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onAddItem: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(MaterialTheme.colorScheme.surface),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.LocationOn,
                label = "Locations",
                selected = currentRoute == AppScreen.Locations.route,
                onClick = { onNavigate(AppScreen.Locations.route) }
            )

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .offset(y = (-8).dp)
            ) {
                FloatingActionButton(
                    onClick = onAddItem,
                    containerColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Item",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            BottomNavItem(
                icon = Icons.AutoMirrored.Filled.List,
                label = "Overview",
                selected = currentRoute == AppScreen.Overview.route,
                onClick = { onNavigate(AppScreen.Overview.route) }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "nav_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}

// Add Item Bottom Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemBottomSheet(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                ) {}

                Spacer(Modifier.height(16.dp))

                Text(
                    "📸 Add New Item",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Choose how to add your item",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            AddItemOptionCard(
                icon = Icons.Default.CameraAlt,
                title = "Take Photo",
                description = "Use your camera to capture the item",
                color = Color(0xFF8B5CF6),
                onClick = onCameraClick
            )

            AddItemOptionCard(
                icon = Icons.Default.PhotoLibrary,
                title = "Choose from Gallery",
                description = "Select an existing photo",
                color = Color(0xFF10B981),
                onClick = onGalleryClick
            )

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.3f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun AddItemOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        border = BorderStroke(2.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    description,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun GridNavigationButton(item: NavGridItem, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PlaceholderScreen(screenName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$screenName Screen",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Coming Soon",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Helper function to share an item
fun shareItem(context: Context, item: Item, garages: List<Garage>) {
    val locationPath = buildLocationPath(item, garages)

    val shareText = buildString {
        appendLine("📦 ${item.name}")
        item.modelNumber?.let { appendLine("Model: $it") }
        appendLine()
        item.description?.let {
            appendLine("Description:")
            appendLine(it)
            appendLine()
        }
        appendLine("📍 Location: $locationPath")
        appendLine("Quantity: ${item.quantity}")
        appendLine("Condition: ${item.condition}")
        item.dimensions?.let { appendLine("Dimensions: $it") }

        if (item.minPrice != null || item.maxPrice != null) {
            appendLine()
            append("💰 Price: ")
            when {
                item.minPrice != null && item.maxPrice != null ->
                    appendLine("$${item.minPrice} - $${item.maxPrice}")
                item.minPrice != null -> appendLine("$${item.minPrice}")
                else -> appendLine("Up to $${item.maxPrice}")
            }
        }
    }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_TITLE, "Item: ${item.name}")
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, "Share ${item.name}")
    context.startActivity(shareIntent)
}

// Helper function for location path
private fun buildLocationPath(item: Item, garages: List<Garage>): String {
    val garage = garages.find { it.id == item.garageId }
    val cabinet = garage?.cabinets?.find { it.id == item.cabinetId }
    val shelf = cabinet?.shelves?.find { it.id == item.shelfId }
    val box = shelf?.boxes?.find { it.id == item.boxId }

    return buildString {
        garage?.let { append(it.name) }
        cabinet?.let { append(" → ${it.name}") }
        shelf?.let { append(" → ${it.name}") }
        box?.let { append(" → ${it.name}") }
    }.ifEmpty { "Unknown Location" }
}

private fun getScreenTitle(route: String?): String {
    return when (route) {
        AppScreen.Dashboard.route -> "Dashboard"
        AppScreen.Locations.route -> "Locations"
        AppScreen.Search.route -> "Search"
        AppScreen.Overview.route -> "Overview"
        AppScreen.Images.route -> "Images"
        AppScreen.History.route -> "History"
        AppScreen.Settings.route -> "Settings"
        AppScreen.Sync.route -> "Sync"
        AppScreen.Help.route -> "Help"
        AppScreen.CreateItem.route -> "Create Item"
        AppScreen.ImageEdit.route -> "Edit Image"
        AppScreen.AIProcessing.route -> "AI Processing"
        AppScreen.AIResults.route -> "AI Results"
        else -> "Inventory Manager"
    }
}

fun createImageUri(context: Context): Uri {
    val imageFile = File(
        context.cacheDir,
        "captured_image_${System.currentTimeMillis()}.jpg"
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        imageFile
    )
}

fun saveBitmapToUri(context: Context, bitmap: Bitmap): Uri {
    val file = File(
        context.cacheDir,
        "edited_image_${System.currentTimeMillis()}.jpg"
    )
    file.outputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
}

@Composable
private fun HandleDialogs(
    dialogState: DialogState,
    garages: MutableList<Garage>,
    onDismiss: () -> Unit,
    onClearHistory: () -> Unit
) {
    when (dialogState) {
        is DialogState.AddGarage -> {
            BasicInputDialog(title = "Add Garage", onConfirm = { name ->
                garages.add(
                    Garage(
                        id = "garage_${System.currentTimeMillis()}",
                        name = name,
                        cabinets = mutableListOf()
                    )
                )
                onDismiss()
            }, onDismiss = onDismiss)
        }
        is DialogState.AddCabinet -> {
            BasicInputDialog(title = "Add Cabinet", onConfirm = { name ->
                val garageIndex = garages.indexOfFirst { it.id == dialogState.garageId }
                if (garageIndex != -1) {
                    val garage = garages[garageIndex]
                    val updatedCabinets = garage.cabinets.toMutableList()
                    updatedCabinets.add(
                        Cabinet(
                            id = "cabinet_${System.currentTimeMillis()}",
                            name = name,
                            shelves = mutableListOf()
                        )
                    )
                    garages[garageIndex] = garage.copy(cabinets = updatedCabinets)
                }
                onDismiss()
            }, onDismiss = onDismiss)
        }
        is DialogState.AddShelf -> {
            BasicInputDialog(title = "Add Shelf", onConfirm = { name ->
                garages.forEachIndexed { garageIndex, garage ->
                    val cabinetIndex = garage.cabinets.indexOfFirst { it.id == dialogState.cabinetId }
                    if (cabinetIndex != -1) {
                        val cabinet = garage.cabinets[cabinetIndex]
                        val updatedShelves = cabinet.shelves.toMutableList()
                        updatedShelves.add(
                            Shelf(
                                id = "shelf_${System.currentTimeMillis()}",
                                name = name,
                                boxes = mutableListOf()
                            )
                        )
                        val updatedCabinets = garage.cabinets.toMutableList()
                        updatedCabinets[cabinetIndex] = cabinet.copy(shelves = updatedShelves)
                        garages[garageIndex] = garage.copy(cabinets = updatedCabinets)
                    }
                }
                onDismiss()
            }, onDismiss = onDismiss)
        }
        is DialogState.AddBox -> {
            BasicInputDialog(title = "Add Box", onConfirm = { name ->
                garages.forEachIndexed { garageIndex, garage ->
                    garage.cabinets.forEachIndexed { cabinetIndex, cabinet ->
                        val shelfIndex = cabinet.shelves.indexOfFirst { it.id == dialogState.shelfId }
                        if (shelfIndex != -1) {
                            val shelf = cabinet.shelves[shelfIndex]
                            val updatedBoxes = shelf.boxes.toMutableList()
                            updatedBoxes.add(
                                Box(
                                    id = "box_${System.currentTimeMillis()}",
                                    name = name
                                )
                            )
                            val updatedShelves = cabinet.shelves.toMutableList()
                            updatedShelves[shelfIndex] = shelf.copy(boxes = updatedBoxes)
                            val updatedCabinets = garage.cabinets.toMutableList()
                            updatedCabinets[cabinetIndex] = cabinet.copy(shelves = updatedShelves)
                            garages[garageIndex] = garage.copy(cabinets = updatedCabinets)
                        }
                    }
                }
                onDismiss()
            }, onDismiss = onDismiss)
        }
        is DialogState.RenameLocation -> {
            BasicInputDialog(
                title = "Rename ${dialogState.type}",
                initialValue = dialogState.oldName,
                onConfirm = { newName ->
                    when (dialogState.type) {
                        "Garage" -> {
                            val index = garages.indexOfFirst { it.id == dialogState.id }
                            if (index != -1) {
                                garages[index] = garages[index].copy(name = newName)
                            }
                        }
                        "Cabinet" -> {
                            garages.forEachIndexed { garageIndex, garage ->
                                val cabinetIndex = garage.cabinets.indexOfFirst { it.id == dialogState.id }
                                if (cabinetIndex != -1) {
                                    val updatedCabinets = garage.cabinets.toMutableList()
                                    updatedCabinets[cabinetIndex] = updatedCabinets[cabinetIndex].copy(name = newName)
                                    garages[garageIndex] = garage.copy(cabinets = updatedCabinets)
                                }
                            }
                        }
                        "Shelf" -> {
                            garages.forEachIndexed { garageIndex, garage ->
                                garage.cabinets.forEachIndexed { cabinetIndex, cabinet ->
                                    val shelfIndex = cabinet.shelves.indexOfFirst { it.id == dialogState.id }
                                    if (shelfIndex != -1) {
                                        val updatedShelves = cabinet.shelves.toMutableList()
                                        updatedShelves[shelfIndex] = updatedShelves[shelfIndex].copy(name = newName)
                                        val updatedCabinets = garage.cabinets.toMutableList()
                                        updatedCabinets[cabinetIndex] = cabinet.copy(shelves = updatedShelves)
                                        garages[garageIndex] = garage.copy(cabinets = updatedCabinets)
                                    }
                                }
                            }
                        }
                        "Box" -> {
                            garages.forEachIndexed { garageIndex, garage ->
                                garage.cabinets.forEachIndexed { cabinetIndex, cabinet ->
                                    cabinet.shelves.forEachIndexed { shelfIndex, shelf ->
                                        val boxIndex = shelf.boxes.indexOfFirst { it.id == dialogState.id }
                                        if (boxIndex != -1) {
                                            val updatedBoxes = shelf.boxes.toMutableList()
                                            updatedBoxes[boxIndex] = updatedBoxes[boxIndex].copy(name = newName)
                                            val updatedShelves = cabinet.shelves.toMutableList()
                                            updatedShelves[shelfIndex] = shelf.copy(boxes = updatedBoxes)
                                            val updatedCabinets = garage.cabinets.toMutableList()
                                            updatedCabinets[cabinetIndex] = cabinet.copy(shelves = updatedShelves)
                                            garages[garageIndex] = garage.copy(cabinets = updatedCabinets)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    onDismiss()
                },
                onDismiss = onDismiss
            )
        }
        is DialogState.DeleteLocation -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Delete ${dialogState.type}?") },
                text = { Text("Are you sure you want to delete '${dialogState.name}'? This cannot be undone.") },
                confirmButton = {
                    Button(onClick = {
                        when (dialogState.type) {
                            "Garage" -> {
                                garages.removeIf { it.id == dialogState.id }
                            }
                            "Cabinet" -> {
                                garages.forEachIndexed { garageIndex, garage ->
                                    val updatedCabinets = garage.cabinets.filter { it.id != dialogState.id }
                                    if (updatedCabinets.size != garage.cabinets.size) {
                                        garages[garageIndex] = garage.copy(cabinets = updatedCabinets as MutableList<Cabinet>)
                                    }
                                }
                            }
                            "Shelf" -> {
                                garages.forEachIndexed { garageIndex, garage ->
                                    garage.cabinets.forEachIndexed { cabinetIndex, cabinet ->
                                        val updatedShelves = cabinet.shelves.filter { it.id != dialogState.id }
                                        if (updatedShelves.size != cabinet.shelves.size) {
                                            val updatedCabinets = garage.cabinets.toMutableList()
                                            updatedCabinets[cabinetIndex] = cabinet.copy(shelves = updatedShelves as MutableList<Shelf>)
                                            garages[garageIndex] = garage.copy(cabinets = updatedCabinets)
                                        }
                                    }
                                }
                            }
                            "Box" -> {
                                garages.forEachIndexed { garageIndex, garage ->
                                    garage.cabinets.forEachIndexed { cabinetIndex, cabinet ->
                                        cabinet.shelves.forEachIndexed { shelfIndex, shelf ->
                                            val updatedBoxes = shelf.boxes.filter { it.id != dialogState.id }
                                            if (updatedBoxes.size != shelf.boxes.size) {
                                                val updatedShelves = cabinet.shelves.toMutableList()
                                                updatedShelves[shelfIndex] = shelf.copy(boxes = updatedBoxes as MutableList<Box>)
                                                val updatedCabinets = garage.cabinets.toMutableList()
                                                updatedCabinets[cabinetIndex] = cabinet.copy(shelves = updatedShelves)
                                                garages[garageIndex] = garage.copy(cabinets = updatedCabinets)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        onDismiss()
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }
        is DialogState.ClearHistory -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Clear History") },
                text = { Text("Clear all history logs?") },
                confirmButton = { Button(onClick = onClearHistory) { Text("Clear") } },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
            )
        }
        DialogState.Closed -> { /* No dialog */ }
    }
}

@Composable
fun BasicInputDialog(
    title: String,
    initialValue: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }) },
        confirmButton = { Button(onClick = { onConfirm(text) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun loadDataFromFile(
    context: Context,
    garages: MutableList<Garage>,
    items: MutableList<Item>,
    history: MutableList<HistoryEntry>
) {
    try {
        val file = File(context.filesDir, "app_data.json")
        if (file.exists()) {
            val json = file.readText()
            val data = Gson().fromJson(json, AppData::class.java)
            garages.clear()
            garages.addAll(data.garages)
            items.clear()
            items.addAll(data.items)
            history.clear()
            history.addAll(data.history)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun saveDataToFile(context: Context, data: AppData) {
    try {
        val file = File(context.filesDir, "app_data.json")
        val json = Gson().toJson(data)
        file.writeText(json)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun loadSettingsFromFile(context: Context): AppSettings {
    return try {
        val file = File(context.filesDir, "settings.json")
        if (file.exists()) {
            val reader = InputStreamReader(file.inputStream())
            Gson().fromJson(reader, AppSettings::class.java)
        } else {
            AppSettings()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        AppSettings()
    }
}

private fun saveSettingsToFile(context: Context, settings: AppSettings) {
    try {
        val file = File(context.filesDir, "settings.json")
        val json = Gson().toJson(settings)
        file.writeText(json)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}