package com.samuel.inventorymanager.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.samuel.inventorymanager.data.AppSettings
import com.samuel.inventorymanager.ui.theme.AppThemeType
import com.samuel.inventorymanager.ui.theme.InventoryManagerTheme
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStreamReader

// ========================================================================================
//      SCREEN NAVIGATION DEFINITIONS
// ========================================================================================

enum class Screen {
    Dashboard, Items, Locations, Search,
    Overview, Images, History, Settings, Sync, Help,
    CreateItem
}

data class NavGridItem(
    val title: String,
    val icon: ImageVector,
    val screen: Screen
)

val navigationItems = listOf(
    NavGridItem("Dashboard", Icons.Default.Dashboard, Screen.Dashboard),
    NavGridItem("Items", Icons.Default.Inventory2, Screen.CreateItem),
    NavGridItem("Locations", Icons.Default.LocationOn, Screen.Locations),
    NavGridItem("Search", Icons.Default.Search, Screen.Search),
    NavGridItem("Overview", Icons.AutoMirrored.Filled.List, Screen.Overview),
    NavGridItem("Images", Icons.Default.Image, Screen.Images),
    NavGridItem("History", Icons.Default.History, Screen.History),
    NavGridItem("Settings", Icons.Default.Settings, Screen.Settings),
    NavGridItem("Sync", Icons.Default.Sync, Screen.Sync),
    NavGridItem("Help", Icons.AutoMirrored.Filled.HelpOutline, Screen.Help)
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

val LocalAppSettings = compositionLocalOf { AppSettings() }
val LocalOnSettingsChange = compositionLocalOf<(AppSettings) -> Unit> { {} }

// ========================================================================================
//      MAIN APPLICATION SCREEN
// ========================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    var appSettings by remember { mutableStateOf(AppSettings()) }

    // Load settings on first launch
    LaunchedEffect(Unit) {
        appSettings = loadSettingsFromFile(context)
    }

    val onSettingsChange: (AppSettings) -> Unit = { newSettings ->
        appSettings = newSettings
        saveSettingsToFile(context, newSettings)
    }

    // Determine theme type based on appSettings
    val themeType = when {
        appSettings.customTheme != null -> AppThemeType.LIGHT // Default to light for custom
        else -> AppThemeType.LIGHT
    }

    val fontScale = appSettings.customTheme?.fontSizeScale ?: 1.0f

    InventoryManagerTheme(
        themeType = themeType,
        fontScale = fontScale
    ) {
        var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
        val garages = remember { mutableStateListOf<Garage>() }
        val items = remember { mutableStateListOf<Item>() }
        val history = remember { mutableStateListOf<HistoryEntry>() }
        var dialogState by remember { mutableStateOf<DialogState>(DialogState.Closed) }

        val createItemViewModel: CreateItemViewModel = viewModel()

        LaunchedEffect(Unit) {
            loadDataFromFile(context, garages, items, history)
        }

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        val onSaveItem: (Item) -> Unit = { newItem ->
            val index = items.indexOfFirst { it.id == newItem.id }
            val newHistoryEntry: HistoryEntry
            if (index != -1) {
                newHistoryEntry = HistoryEntry(
                    id = "hist_${System.currentTimeMillis()}",
                    itemId = newItem.id,
                    itemName = newItem.name,
                    action = HistoryAction.Updated,
                    description = "Item details were modified."
                )
                items[index] = newItem
            } else {
                newHistoryEntry = HistoryEntry(
                    id = "hist_${System.currentTimeMillis()}",
                    itemId = newItem.id,
                    itemName = newItem.name,
                    action = HistoryAction.Added,
                    description = "A new item was created."
                )
                items.add(newItem)
            }
            history.add(0, newHistoryEntry)
            saveDataToFile(context, AppData(garages.toList(), items.toList(), history.toList()))
        }

        val onClearHistory = { dialogState = DialogState.ClearHistory }

        HandleDialogs(
            dialogState = dialogState,
            garages = garages,
            onClearHistory = {
                history.clear()
                dialogState = DialogState.Closed
                saveDataToFile(context, AppData(garages.toList(), items.toList(), history.toList()))
            },
            onDismiss = {
                dialogState = DialogState.Closed
                saveDataToFile(context, AppData(garages.toList(), items.toList(), history.toList()))
            }
        )

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
                                GridNavigationButton(item, currentScreen == item.screen) {
                                    currentScreen = item.screen
                                    scope.launch { drawerState.close() }
                                }
                            }
                        }
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                currentScreen.name,
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
                },
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.LocationOn, null) },
                            label = { Text("Locations") },
                            selected = currentScreen == Screen.Locations,
                            onClick = { currentScreen = Screen.Locations }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Add, null) },
                            label = { Text("Add Item") },
                            selected = currentScreen == Screen.CreateItem,
                            onClick = {
                                createItemViewModel.clearFormForNewItem(garages)
                                currentScreen = Screen.CreateItem
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                            label = { Text("Overview") },
                            selected = currentScreen == Screen.Overview,
                            onClick = { currentScreen = Screen.Overview }
                        )
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                    when (currentScreen) {
                        Screen.Dashboard -> DashboardScreen(items, garages)
                        Screen.Locations -> LocationsScreen(
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
                        Screen.Items, Screen.CreateItem -> CreateItemScreen(
                            garages = garages,
                            onSaveItem = onSaveItem,
                            viewModel = createItemViewModel,
                            appSettings = appSettings,
                            onSettingsChange = onSettingsChange
                        )
                        Screen.Overview -> OverviewScreen(items, garages)
                        Screen.Search -> SearchScreen(items, garages)
                        Screen.Settings -> SettingsScreen(
                            currentSettings = appSettings,
                            onSettingsChange = onSettingsChange
                        )
                        Screen.Help -> HelpScreen()
                        Screen.Images -> ImagesScreen(items) { item ->
                            createItemViewModel.loadItemForEditing(item, garages)
                            currentScreen = Screen.CreateItem
                        }
                        Screen.History -> HistoryScreen(
                            history = history,
                            items = items,
                            onItemClick = { targetItem ->
                                createItemViewModel.loadItemForEditing(targetItem, garages)
                                currentScreen = Screen.CreateItem
                            },
                            onClearHistory = onClearHistory
                        )
                        else -> PlaceholderScreen(screenName = currentScreen.name)
                    }
                }
            }
        }
    }
}

// ========================================================================================
//      HELPER COMPOSABLES
// ========================================================================================

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HandleDialogs(
    dialogState: DialogState,
    garages: MutableList<Garage>,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    when (val state = dialogState) {
        is DialogState.Closed -> {}
        DialogState.AddGarage -> AddLocationDialog("Create New Garage", onDismiss) { name ->
            garages.add(Garage("g_${System.currentTimeMillis()}", name, emptyList()))
        }
        is DialogState.AddCabinet -> AddLocationDialog("Add Cabinet", onDismiss) { name ->
            val i = garages.indexOfFirst { it.id == state.garageId }
            if (i != -1) {
                val g = garages[i]
                garages[i] = g.copy(
                    cabinets = g.cabinets + Cabinet("c_${System.currentTimeMillis()}", name, emptyList())
                )
            }
        }
        is DialogState.AddShelf -> AddLocationDialog("Add Shelf", onDismiss) { name ->
            for ((gi, g) in garages.withIndex()) {
                val ci = g.cabinets.indexOfFirst { it.id == state.cabinetId }
                if (ci != -1) {
                    val c = g.cabinets[ci]
                    val newShelf = Shelf("s_${System.currentTimeMillis()}", name, emptyList())
                    garages[gi] = g.copy(
                        cabinets = g.cabinets.toMutableList().also { cabinetList ->
                            cabinetList[ci] = c.copy(shelves = c.shelves + newShelf)
                        }
                    )
                    break
                }
            }
        }
        is DialogState.AddBox -> AddLocationDialog("Add Box/Bin", onDismiss) { name ->
            run loop@{
                for ((gi, g) in garages.withIndex()) {
                    for ((ci, c) in g.cabinets.withIndex()) {
                        val si = c.shelves.indexOfFirst { it.id == state.shelfId }
                        if (si != -1) {
                            val s = c.shelves[si]
                            val newBox = Box("b_${System.currentTimeMillis()}", name)
                            garages[gi] = g.copy(
                                cabinets = g.cabinets.toMutableList().also { cabinetList ->
                                    cabinetList[ci] = c.copy(
                                        shelves = c.shelves.toMutableList().also { shelfList ->
                                            shelfList[si] = s.copy(boxes = s.boxes + newBox)
                                        }
                                    )
                                }
                            )
                            return@loop
                        }
                    }
                }
            }
        }
        is DialogState.RenameLocation -> {
            RenameDialog(state, onDismiss) { /* Rename logic */ }
        }
        is DialogState.DeleteLocation -> {
            DeleteConfirmDialog(state, onDismiss) { /* Delete logic */ }
        }
        is DialogState.ClearHistory -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Clear History?") },
                text = { Text("Are you sure you want to clear all history? This cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = onClearHistory,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Clear All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLocationDialog(title: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { newText -> text = newText },
                label = { Text("Location Name") }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) onConfirm(text)
                    onDismiss()
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenameDialog(
    state: DialogState.RenameLocation,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(state.oldName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename ${state.type.replaceFirstChar { it.uppercase() }}") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { newText -> text = newText },
                label = { Text("New Name") }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) onConfirm(text)
                    onDismiss()
                }
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeleteConfirmDialog(
    state: DialogState.DeleteLocation,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete ${state.type.replaceFirstChar { it.uppercase() }}?") },
        text = {
            Text("Are you sure you want to delete '${state.name}'? This cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ========================================================================================
//      DATA PERSISTENCE FUNCTIONS
// ========================================================================================

private fun saveDataToFile(context: Context, appData: AppData) {
    try {
        context.openFileOutput("inventory.json", Context.MODE_PRIVATE).use {
            it.write(Gson().toJson(appData).toByteArray())
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun loadDataFromFile(
    context: Context,
    garages: MutableList<Garage>,
    items: MutableList<Item>,
    history: MutableList<HistoryEntry>
) {
    val file = File(context.filesDir, "inventory.json")
    if (!file.exists()) return
    try {
        context.openFileInput("inventory.json").use { stream ->
            InputStreamReader(stream).use { reader ->
                val data = Gson().fromJson(reader, AppData::class.java)
                garages.clear()
                items.clear()
                history.clear()
                garages.addAll(data.garages)
                items.addAll(data.items)
                history.addAll(data.history)
            }
        }
    } catch (_: Exception) {
        // Failed to load data, starting fresh
    }
}

private fun saveSettingsToFile(context: Context, settings: AppSettings) {
    try {
        context.openFileOutput("app_settings.json", Context.MODE_PRIVATE).use {
            it.write(Gson().toJson(settings).toByteArray())
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun loadSettingsFromFile(context: Context): AppSettings {
    val file = File(context.filesDir, "app_settings.json")
    return if (file.exists()) {
        try {
            Gson().fromJson(file.readText(), AppSettings::class.java)
        } catch (e: Exception) {
            AppSettings()
        }
    } else {
        AppSettings()
    }
}