package com.samuel.inventorymanager.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import java.util.Locale

@Composable
fun LocationsScreen(
    garages: List<Garage>,
    items: List<Item>,
    onAddGarage: () -> Unit,
    onAddCabinet: (garageId: String) -> Unit,
    onAddShelf: (cabinetId: String) -> Unit,
    onAddBox: (shelfId: String) -> Unit,
    onRenameLocation: (id: String, oldName: String, type: String) -> Unit,
    onDeleteLocation: (id: String, name: String, type: String) -> Unit
) {
    var isPreviewMode by remember { mutableStateOf(false) }

    if (isPreviewMode) {
        PreviewMode(
            garages = garages,
            items = items,
            onBackToEdit = { isPreviewMode = false }
        )
    } else {
        EditMode(
            garages = garages,
            items = items,
            onAddGarage = onAddGarage,
            onAddCabinet = onAddCabinet,
            onAddShelf = onAddShelf,
            onAddBox = onAddBox,
            onRenameLocation = onRenameLocation,
            onDeleteLocation = onDeleteLocation,
            onSwitchToPreview = { isPreviewMode = true }
        )
    }
}

// ========================================================================================
//      EDIT MODE
// ========================================================================================

@Composable
private fun EditMode(
    garages: List<Garage>,
    items: List<Item>,
    onAddGarage: () -> Unit,
    onAddCabinet: (garageId: String) -> Unit,
    onAddShelf: (cabinetId: String) -> Unit,
    onAddBox: (shelfId: String) -> Unit,
    onRenameLocation: (id: String, oldName: String, type: String) -> Unit,
    onDeleteLocation: (id: String, name: String, type: String) -> Unit,
    onSwitchToPreview: () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 3.dp,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Edit Locations",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${garages.size} garages • ${items.size} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onSwitchToPreview,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Preview,
                            "Preview Mode",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddGarage,
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp))
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Garage",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        if (garages.isEmpty()) {
            EmptyState(onAddGarage, modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(garages, key = { it.id }) { garage ->
                    val itemsInGarage = items.filter { it.garageId == garage.id }
                    GarageSection(
                        garage = garage,
                        itemsInGarage = itemsInGarage,
                        onAddCabinet = { onAddCabinet(garage.id) },
                        onAddShelf = onAddShelf,
                        onAddBox = onAddBox,
                        onRenameLocation = onRenameLocation,
                        onDeleteLocation = onDeleteLocation
                    )
                }
            }
        }
    }
}

// ========================================================================================
//      MULTI-ADD DIALOG
// ========================================================================================

@Composable
private fun MultiAddDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (count: Int) -> Unit
) {
    var count by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "How many would you like to add?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = count,
                    onValueChange = {
                        if (it.isEmpty() || it.toIntOrNull() != null) {
                            count = it
                        }
                    },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick select buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 3, 5, 10).forEach { num ->
                        OutlinedButton(
                            onClick = { count = num.toString() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (count == num.toString())
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    Color.Transparent
                            )
                        ) {
                            Text(num.toString())
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val num = count.toIntOrNull()
                    if (num != null && num > 0) {
                        onConfirm(num)
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

// ========================================================================================
//      PREVIEW MODE
// ========================================================================================

private enum class PreviewStyle {
    DRILL_DOWN, COLLAPSIBLE
}

private enum class ItemViewMode {
    GRID, LIST
}

@Composable
private fun PreviewMode(
    garages: List<Garage>,
    items: List<Item>,
    onBackToEdit: () -> Unit
) {
    var previewStyle by remember { mutableStateOf(PreviewStyle.DRILL_DOWN) }

    when (previewStyle) {
        PreviewStyle.DRILL_DOWN -> {
            DrillDownNavigationHost(
                garages = garages,
                items = items,
                onBackToEdit = onBackToEdit,
                onSwitchStyle = { previewStyle = PreviewStyle.COLLAPSIBLE }
            )
        }
        PreviewStyle.COLLAPSIBLE -> {
            CollapsibleListPreview(
                garages = garages,
                items = items,
                onBackToEdit = onBackToEdit,
                onSwitchStyle = { previewStyle = PreviewStyle.DRILL_DOWN }
            )
        }
    }
}

// ========================================================================================
//      DRILL-DOWN PREVIEW
// ========================================================================================

@Composable
private fun DrillDownNavigationHost(
    garages: List<Garage>,
    items: List<Item>,
    onBackToEdit: () -> Unit,
    onSwitchStyle: () -> Unit
) {
    var selectedGarageId by remember { mutableStateOf<String?>(null) }
    var selectedCabinetId by remember { mutableStateOf<String?>(null) }
    var selectedShelfId by remember { mutableStateOf<String?>(null) }
    var selectedBoxId by remember { mutableStateOf<String?>(null) }
    var boxLayout by remember { mutableStateOf(BoxLayout.SCATTERED) }
    var showAllItemsInGarage by remember { mutableStateOf(false) }
    var showAllItemsInCabinet by remember { mutableStateOf(false) }
    var showAllItemsInShelf by remember { mutableStateOf(false) }

    when {
        selectedBoxId != null -> {
            val garage = garages.find { it.id == selectedGarageId }
            val cabinet = garage?.cabinets?.find { it.id == selectedCabinetId }
            val shelf = cabinet?.shelves?.find { it.id == selectedShelfId }
            val box = shelf?.boxes?.find { it.id == selectedBoxId }
            val itemsInBox = items.filter { it.boxId == selectedBoxId }

            if (box != null) {
                BoxPreviewScreen(
                    box = box,
                    itemsInBox = itemsInBox,
                    boxLayout = boxLayout,
                    onLayoutChange = { boxLayout = it },
                    onBack = {
                        selectedBoxId = null
                        showAllItemsInShelf = false
                    }
                )
            }
        }
        showAllItemsInShelf && selectedShelfId != null -> {
            val garage = garages.find { it.id == selectedGarageId }
            val cabinet = garage?.cabinets?.find { it.id == selectedCabinetId }
            val shelf = cabinet?.shelves?.find { it.id == selectedShelfId }
            val itemsInShelf = items.filter { it.shelfId == selectedShelfId }

            if (shelf != null) {
                AllItemsInLocationScreen(
                    locationName = shelf.name,
                    locationType = "Shelf",
                    items = itemsInShelf,
                    onBack = { showAllItemsInShelf = false }
                )
            }
        }
        selectedShelfId != null -> {
            val garage = garages.find { it.id == selectedGarageId }
            val cabinet = garage?.cabinets?.find { it.id == selectedCabinetId }
            val shelf = cabinet?.shelves?.find { it.id == selectedShelfId }
            val itemsInShelf = items.filter { it.shelfId == selectedShelfId }
            val boxesInShelf = shelf?.boxes ?: emptyList()

            if (shelf != null) {
                ShelfPreviewScreen(
                    shelf = shelf,
                    itemsInShelf = itemsInShelf,
                    boxesInShelf = boxesInShelf,
                    onBoxClick = { boxId -> selectedBoxId = boxId },
                    onShowAllItems = { showAllItemsInShelf = true },
                    onBack = {
                        selectedShelfId = null
                        showAllItemsInCabinet = false
                    }
                )
            }
        }
        showAllItemsInCabinet && selectedCabinetId != null -> {
            val garage = garages.find { it.id == selectedGarageId }
            val cabinet = garage?.cabinets?.find { it.id == selectedCabinetId }
            val itemsInCabinet = items.filter { it.cabinetId == selectedCabinetId }

            if (cabinet != null) {
                AllItemsInLocationScreen(
                    locationName = cabinet.name,
                    locationType = "Cabinet",
                    items = itemsInCabinet,
                    onBack = { showAllItemsInCabinet = false }
                )
            }
        }
        selectedCabinetId != null -> {
            val garage = garages.find { it.id == selectedGarageId }
            val cabinet = garage?.cabinets?.find { it.id == selectedCabinetId }
            val itemsInCabinet = items.filter { it.cabinetId == selectedCabinetId }

            if (cabinet != null) {
                CabinetPreviewScreen(
                    cabinet = cabinet,
                    itemsInCabinet = itemsInCabinet,
                    onShelfClick = { shelfId -> selectedShelfId = shelfId },
                    onShowAllItems = { showAllItemsInCabinet = true },
                    onBack = {
                        selectedCabinetId = null
                        showAllItemsInGarage = false
                    }
                )
            }
        }
        showAllItemsInGarage && selectedGarageId != null -> {
            val garage = garages.find { it.id == selectedGarageId }
            val itemsInGarage = items.filter { it.garageId == selectedGarageId }

            if (garage != null) {
                AllItemsInLocationScreen(
                    locationName = garage.name,
                    locationType = "Garage",
                    items = itemsInGarage,
                    onBack = { showAllItemsInGarage = false }
                )
            }
        }
        selectedGarageId != null -> {
            val garage = garages.find { it.id == selectedGarageId }
            val itemsInGarage = items.filter { it.garageId == selectedGarageId }

            if (garage != null) {
                GarageCabinetsScreen(
                    garage = garage,
                    itemsInGarage = itemsInGarage,
                    onCabinetClick = { cabinetId -> selectedCabinetId = cabinetId },
                    onShowAllItems = { showAllItemsInGarage = true },
                    onBack = { selectedGarageId = null }
                )
            }
        }
        else -> {
            GaragePreviewScreen(
                garages = garages,
                items = items,
                onGarageClick = { garageId -> selectedGarageId = garageId },
                onBackToEdit = onBackToEdit,
                onSwitchStyle = onSwitchStyle
            )
        }
    }
}

// ========================================================================================
//      ALL ITEMS IN LOCATION SCREEN
// ========================================================================================

@Composable
private fun AllItemsInLocationScreen(
    locationName: String,
    locationType: String,
    items: List<Item>,
    onBack: () -> Unit
) {
    var viewMode by remember { mutableStateOf(ItemViewMode.GRID) }

    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 3.dp,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "All Items",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "in $locationName",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Inventory2,
                                    null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Column {
                                    Text(
                                        "${items.size}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        "Total Items",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                viewMode = if (viewMode == ItemViewMode.GRID)
                                    ItemViewMode.LIST
                                else
                                    ItemViewMode.GRID
                            },
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    RoundedCornerShape(12.dp)
                                )
                                .size(56.dp)
                        ) {
                            Icon(
                                if (viewMode == ItemViewMode.GRID)
                                    Icons.Default.ViewAgenda
                                else
                                    Icons.Default.ViewModule,
                                "Toggle View",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Inventory,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        "No items in this $locationType",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            when (viewMode) {
                ItemViewMode.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(items) { item ->
                            EnhancedItemCard(item = item)
                        }
                    }
                }
                ItemViewMode.LIST -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items) { item ->
                            EnhancedItemListRow(item)
                        }
                    }
                }
            }
        }
    }
}

// ========================================================================================
//      ENHANCED ITEM CARDS
// ========================================================================================

@Composable
private fun EnhancedItemCard(item: Item) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (item.images.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(model = item.images[0].toUri()),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Inventory2,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
                    .align(Alignment.BottomCenter)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    item.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.modelNumber != null) {
                    Text(
                        "Model: ${item.modelNumber}",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            "Qty: ${item.quantity}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (item.minPrice != null && item.maxPrice != null) {
                        val avgPrice = (item.minPrice + item.maxPrice) / 2
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981)
                        ) {
                            Text(
                                "$${String.format(Locale.US, "%.0f", avgPrice)}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedItemListRow(item: Item) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(3.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (item.images.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(model = item.images[0].toUri()),
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Inventory2,
                        null,
                        modifier = Modifier.size(45.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    item.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                if (item.modelNumber != null) {
                    Text(
                        "Model: ${item.modelNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Inventory2,
                                null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "${item.quantity}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    if (item.minPrice != null && item.maxPrice != null) {
                        val avgPrice = (item.minPrice + item.maxPrice) / 2
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AttachMoney,
                                    null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFF10B981)
                                )
                                Text(
                                    String.format(Locale.US, "%.0f", avgPrice),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }

                if (item.description != null && item.description.isNotBlank()) {
                    Text(
                        item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ========================================================================================
//      GARAGE PREVIEW SCREENS
// ========================================================================================

@Composable
private fun GaragePreviewScreen(
    garages: List<Garage>,
    items: List<Item>,
    onGarageClick: (String) -> Unit,
    onBackToEdit: () -> Unit,
    onSwitchStyle: () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 3.dp,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onSwitchStyle,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ViewList,
                                "Switch View",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Column {
                            Text(
                                "Your Garages",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${garages.size} locations",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onBackToEdit,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            "Edit Mode",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(garages) { garage ->
                val itemsInGarage = items.filter { it.garageId == garage.id }
                val totalValue = itemsInGarage.sumOf {
                    ((it.minPrice ?: 0.0) + (it.maxPrice ?: 0.0)) / 2 * it.quantity
                }

                GaragePreviewCard(
                    garage = garage,
                    itemCount = itemsInGarage.size,
                    totalValue = totalValue,
                    onClick = { onGarageClick(garage.id) }
                )
            }
        }
    }
}

@Composable
private fun GaragePreviewCard(
    garage: Garage,
    itemCount: Int,
    totalValue: Double,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.HomeWork,
                    null,
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    garage.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoChip(
                        icon = Icons.Default.Inventory2,
                        text = itemCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    InfoChip(
                        icon = Icons.Default.AttachMoney,
                        text = String.format(Locale.US, "%.0f", totalValue),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun GarageCabinetsScreen(
    garage: Garage,
    itemsInGarage: List<Item>,
    onCabinetClick: (String) -> Unit,
    onShowAllItems: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 3.dp,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    garage.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${garage.cabinets.size} cabinets • ${itemsInGarage.size} items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (itemsInGarage.isNotEmpty()) {
                        Button(
                            onClick = onShowAllItems,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            Icon(
                                Icons.Default.ViewModule,
                                null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "View All ${itemsInGarage.size} Items in Garage",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(garage.cabinets) { cabinet ->
                val itemsInCabinet = itemsInGarage.filter { it.cabinetId == cabinet.id }
                CabinetGridCard(
                    cabinet = cabinet,
                    itemCount = itemsInCabinet.size,
                    shelfCount = cabinet.shelves.size,
                    onClick = { onCabinetClick(cabinet.id) }
                )
            }
        }
    }
}

@Composable
private fun CabinetGridCard(
    cabinet: Cabinet,
    itemCount: Int,
    shelfCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 6.dp
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Kitchen,
                    null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    cabinet.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "$shelfCount shelves • $itemCount items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CabinetPreviewScreen(
    cabinet: Cabinet,
    itemsInCabinet: List<Item>,
    onShelfClick: (String) -> Unit,
    onShowAllItems: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 3.dp,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    )
                            ) {
                                Icon(Icons.Default.ArrowBack, "Back")
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    cabinet.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${cabinet.shelves.size} shelves",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (itemsInCabinet.isNotEmpty()) {
                        Button(
                            onClick = onShowAllItems,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            Icon(
                                Icons.Default.ViewModule,
                                null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "View All ${itemsInCabinet.size} Items in Cabinet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(cabinet.shelves) { shelf ->
                val itemsInShelf = itemsInCabinet.filter { it.shelfId == shelf.id }
                ShelfPreviewCard(
                    shelf = shelf,
                    itemCount = itemsInShelf.size,
                    onClick = { onShelfClick(shelf.id) }
                )
            }
        }
    }
}

@Composable
private fun ShelfPreviewCard(
    shelf: Shelf,
    itemCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.tertiaryContainer,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ViewList,
                    null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Column {
                Text(
                    shelf.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "$itemCount items",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ShelfPreviewScreen(
    shelf: Shelf,
    itemsInShelf: List<Item>,
    boxesInShelf: List<Box>,
    onBoxClick: (String) -> Unit,
    onShowAllItems: () -> Unit,
    onBack: () -> Unit
) {
    val looseItems = itemsInShelf.filter { it.boxId == null }

    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 3.dp,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    shelf.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${boxesInShelf.size} boxes • ${looseItems.size} loose items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (itemsInShelf.isNotEmpty()) {
                        Button(
                            onClick = onShowAllItems,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            ),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            Icon(
                                Icons.Default.ViewModule,
                                null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "View All ${itemsInShelf.size} Items on Shelf",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(looseItems) { item ->
                SmallItemPreviewCard(item = item)
            }

            items(boxesInShelf) { box ->
                val itemsInBox = itemsInShelf.filter { it.boxId == box.id }
                BoxPreviewCard(
                    box = box,
                    itemCount = itemsInBox.size,
                    onClick = { onBoxClick(box.id) }
                )
            }
        }
    }
}

@Composable
private fun SmallItemPreviewCard(item: Item) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (item.images.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(model = item.images[0].toUri()),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Inventory2,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .align(Alignment.BottomCenter)
            )

            Text(
                item.name,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BoxPreviewCard(
    box: Box,
    itemCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 6.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF8B5CF6).copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        Color(0xFF8B5CF6).copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Inventory,
                    null,
                    modifier = Modifier.size(26.dp),
                    tint = Color(0xFF8B5CF6)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    box.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "$itemCount items",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

enum class BoxLayout {
    SCATTERED, NEAT
}

@Composable
private fun BoxPreviewScreen(
    box: Box,
    itemsInBox: List<Item>,
    boxLayout: BoxLayout,
    onLayoutChange: (BoxLayout) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 3.dp,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    )
                            ) {
                                Icon(Icons.Default.ArrowBack, "Back")
                            }
                            Column {
                                Text(
                                    box.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${itemsInBox.size} items inside",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LayoutToggleButton(
                            text = "Grid View",
                            isSelected = boxLayout == BoxLayout.SCATTERED,
                            onClick = { onLayoutChange(BoxLayout.SCATTERED) },
                            modifier = Modifier.weight(1f)
                        )
                        LayoutToggleButton(
                            text = "List View",
                            isSelected = boxLayout == BoxLayout.NEAT,
                            onClick = { onLayoutChange(BoxLayout.NEAT) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (itemsInBox.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Inventory,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        "No items in this box",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            when (boxLayout) {
                BoxLayout.SCATTERED -> ScatteredLayout(itemsInBox, paddingValues)
                BoxLayout.NEAT -> NeatLayout(itemsInBox, paddingValues)
            }
        }
    }
}

@Composable
private fun LayoutToggleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Text(
            text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScatteredLayout(items: List<Item>, paddingValues: PaddingValues) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items) { item ->
            SmallItemPreviewCard(item = item)
        }
    }
}

@Composable
private fun NeatLayout(items: List<Item>, paddingValues: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            NeatLayoutItemRow(item)
        }
    }
}

@Composable
private fun NeatLayoutItemRow(item: Item) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (item.images.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(model = item.images[0].toUri()),
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Inventory2,
                        null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    item.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                if (item.modelNumber != null) {
                    Text(
                        "Model: ${item.modelNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Qty: ${item.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ========================================================================================
//      COLLAPSIBLE LIST PREVIEW
// ========================================================================================

@Composable
private fun CollapsibleListPreview(
    garages: List<Garage>,
    items: List<Item>,
    onBackToEdit: () -> Unit,
    onSwitchStyle: () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 3.dp,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onSwitchStyle,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                Icons.Default.GridView,
                                "Switch View",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Column {
                            Text(
                                "All Locations",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${garages.size} garages • ${items.size} items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onBackToEdit,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            "Edit Mode",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (garages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.LocationCity,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        "No locations to preview",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(garages, key = { it.id }) { garage ->
                    val itemsInGarage = items.filter { it.garageId == garage.id }
                    CollapsibleGarageSection(garage, itemsInGarage)
                }
            }
        }
    }
}

@Composable
private fun CollapsibleGarageSection(garage: Garage, itemsInGarage: List<Item>) {
    var isExpanded by remember { mutableStateOf(false) }
    val totalValue = itemsInGarage.sumOf { ((it.minPrice ?: 0.0) + (it.maxPrice ?: 0.0)) / 2 * it.quantity }

    Card(
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    ) {
        Column {
            PreviewSectionHeader(
                title = garage.name,
                icon = Icons.Default.HomeWork,
                isExpanded = isExpanded,
                onToggleExpand = { isExpanded = !isExpanded }
            ) {
                StatChip(icon = Icons.Default.Inventory2, text = "${itemsInGarage.size}", small = false)
                StatChip(icon = Icons.Default.AttachMoney, text = String.format(Locale.US, "%.0f", totalValue), small = false)
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(start = 18.dp, end = 12.dp, bottom = 16.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (garage.cabinets.isEmpty()) {
                        EmptyChildState("No cabinets in this garage")
                    } else {
                        garage.cabinets.forEach { cabinet ->
                            val itemsInCabinet = itemsInGarage.filter { it.cabinetId == cabinet.id }
                            CollapsibleCabinetSection(cabinet, itemsInCabinet)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleCabinetSection(cabinet: Cabinet, itemsInCabinet: List<Item>) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        modifier = Modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    ) {
        Column {
            PreviewSectionHeader(
                title = cabinet.name,
                icon = Icons.Default.Kitchen,
                isExpanded = isExpanded,
                onToggleExpand = { isExpanded = !isExpanded }
            ) {
                StatChip(icon = Icons.Default.Inventory2, text = "${itemsInCabinet.size}", small = true)
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 18.dp, end = 12.dp, bottom = 14.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (cabinet.shelves.isEmpty()) {
                        EmptyChildState("No shelves in this cabinet")
                    } else {
                        cabinet.shelves.forEach { shelf ->
                            val itemsInShelf = itemsInCabinet.filter { it.shelfId == shelf.id }
                            CollapsibleShelfSection(shelf, itemsInShelf)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleShelfSection(shelf: Shelf, itemsInShelf: List<Item>) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    ) {
        Column {
            PreviewSectionHeader(
                title = shelf.name,
                icon = Icons.AutoMirrored.Filled.ViewList,
                isExpanded = isExpanded,
                onToggleExpand = { isExpanded = !isExpanded }
            ) {
                StatChip(icon = Icons.Default.Inventory2, text = "${itemsInShelf.size}", small = true)
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 18.dp, end = 12.dp, bottom = 14.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val looseItems = itemsInShelf.filter { it.boxId == null }
                    if (shelf.boxes.isEmpty() && looseItems.isEmpty()) {
                        EmptyChildState("No items or boxes on this shelf")
                    } else {
                        shelf.boxes.forEach { box ->
                            val itemsInBox = itemsInShelf.filter { it.boxId == box.id }
                            CollapsibleBoxSection(box, itemsInBox)
                        }
                        looseItems.forEach { item ->
                            NeatLayoutItemRow(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleBoxSection(box: Box, itemsInBox: List<Item>) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.2f)),
        color = Color(0xFF8B5CF6).copy(alpha = 0.08f),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Inventory,
                        null,
                        modifier = Modifier.size(22.dp),
                        tint = Color(0xFF8B5CF6)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        box.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${itemsInBox.size} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (itemsInBox.isEmpty()) {
                        EmptyChildState("No items in this box")
                    } else {
                        itemsInBox.forEach { item ->
                            NeatLayoutItemRow(item)
                        }
                    }
                }
            }
        }
    }
}

// ========================================================================================
//      EDIT MODE COMPONENTS
// ========================================================================================

@Composable
private fun GarageSection(
    garage: Garage,
    itemsInGarage: List<Item>,
    onAddCabinet: () -> Unit,
    onAddShelf: (cabinetId: String) -> Unit,
    onAddBox: (shelfId: String) -> Unit,
    onRenameLocation: (id: String, oldName: String, type: String) -> Unit,
    onDeleteLocation: (id: String, name: String, type: String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    val totalValue = itemsInGarage.sumOf { ((it.minPrice ?: 0.0) + (it.maxPrice ?: 0.0)) / 2 * it.quantity }

    Card(
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    ) {
        Column {
            SectionHeader(
                title = garage.name,
                icon = Icons.Default.HomeWork,
                isExpanded = isExpanded,
                onToggleExpand = { isExpanded = !isExpanded },
                onAddClick = { showAddDialog = true },
                onRenameClick = { onRenameLocation(garage.id, garage.name, "garage") },
                onDeleteClick = { onDeleteLocation(garage.id, garage.name, "garage") }
            ) {
                StatChip(icon = Icons.Default.Inventory2, text = "${itemsInGarage.size}", small = false)
                StatChip(icon = Icons.Default.AttachMoney, text = String.format(Locale.US, "%.0f", totalValue), small = false)
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(start = 18.dp, end = 12.dp, bottom = 16.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (garage.cabinets.isEmpty()) {
                        EmptyChildState("No cabinets yet. Tap '+' to add.")
                    } else {
                        garage.cabinets.forEach { cabinet ->
                            val itemsInCabinet = itemsInGarage.filter { it.cabinetId == cabinet.id }
                            CabinetSection(
                                cabinet,
                                itemsInCabinet,
                                { onAddShelf(cabinet.id) },
                                onAddBox,
                                onRenameLocation,
                                onDeleteLocation
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        MultiAddDialog(
            title = "Add Cabinets",
            onDismiss = { showAddDialog = false },
            onConfirm = { count ->
                repeat(count) { onAddCabinet() }
            }
        )
    }
}

@Composable
private fun CabinetSection(
    cabinet: Cabinet,
    itemsInCabinet: List<Item>,
    onAddShelf: () -> Unit,
    onAddBox: (shelfId: String) -> Unit,
    onRenameLocation: (id: String, oldName: String, type: String) -> Unit,
    onDeleteLocation: (id: String, name: String, type: String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        modifier = Modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    ) {
        Column {
            SectionHeader(
                title = cabinet.name,
                icon = Icons.Default.Kitchen,
                isExpanded = isExpanded,
                onToggleExpand = { isExpanded = !isExpanded },
                onAddClick = { showAddDialog = true },
                onRenameClick = { onRenameLocation(cabinet.id, cabinet.name, "cabinet") },
                onDeleteClick = { onDeleteLocation(cabinet.id, cabinet.name, "cabinet") }
            ) {
                StatChip(icon = Icons.Default.Inventory2, text = "${itemsInCabinet.size}", small = true)
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(start = 18.dp, end = 12.dp, bottom = 14.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (cabinet.shelves.isEmpty()) {
                        EmptyChildState("No shelves yet. Tap '+' to add.")
                    } else {
                        cabinet.shelves.forEach { shelf ->
                            val itemsInShelf = itemsInCabinet.filter { it.shelfId == shelf.id }
                            ShelfSection(
                                shelf,
                                itemsInShelf,
                                { onAddBox(shelf.id) },
                                onRenameLocation,
                                onDeleteLocation
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        MultiAddDialog(
            title = "Add Shelves",
            onDismiss = { showAddDialog = false },
            onConfirm = { count ->
                repeat(count) { onAddShelf() }
            }
        )
    }
}

@Composable
private fun ShelfSection(
    shelf: Shelf,
    itemsInShelf: List<Item>,
    onAddBox: () -> Unit,
    onRenameLocation: (id: String, oldName: String, type: String) -> Unit,
    onDeleteLocation: (id: String, name: String, type: String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    ) {
        Column {
            SectionHeader(
                title = shelf.name,
                icon = Icons.AutoMirrored.Filled.ViewList,
                isExpanded = isExpanded,
                onToggleExpand = { isExpanded = !isExpanded },
                onAddClick = { showAddDialog = true },
                onRenameClick = { onRenameLocation(shelf.id, shelf.name, "shelf") },
                onDeleteClick = { onDeleteLocation(shelf.id, shelf.name, "shelf") }
            ) {
                StatChip(icon = Icons.Default.Inventory2, text = "${itemsInShelf.size}", small = true)
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(start = 18.dp, end = 12.dp, bottom = 14.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (shelf.boxes.isEmpty()) {
                        EmptyChildState("No boxes yet. Tap '+' to add.")
                    } else {
                        shelf.boxes.forEach { box ->
                            val itemsInBox = itemsInShelf.filter { it.boxId == box.id }
                            BoxItem(
                                box = box,
                                itemCount = itemsInBox.size,
                                onRename = { onRenameLocation(box.id, box.name, "box") },
                                onDelete = { onDeleteLocation(box.id, box.name, "box") }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        MultiAddDialog(
            title = "Add Boxes",
            onDismiss = { showAddDialog = false },
            onConfirm = { count ->
                repeat(count) { onAddBox() }
            }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    statsContent: @Composable RowScope.() -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                statsContent()
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onAddClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Add,
                    "Add",
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        "More Options",
                        modifier = Modifier.size(22.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp)) },
                        onClick = { showMenu = false; onRenameClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = { showMenu = false; onDeleteClick() }
                    )
                }
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = "Toggle",
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PreviewSectionHeader(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    statsContent: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                statsContent()
            }
        }

        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = "Toggle",
            modifier = Modifier.size(26.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatChip(icon: ImageVector, text: String, small: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(if (small) 16.dp else 18.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text,
                style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun BoxItem(
    box: Box,
    itemCount: Int,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.2f)),
        color = Color(0xFF8B5CF6).copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Inventory,
                    null,
                    modifier = Modifier.size(22.dp),
                    tint = Color(0xFF8B5CF6)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    box.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$itemCount items",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        "More",
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp)) },
                        onClick = { showMenu = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onAddGarage: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.LocationCity,
                null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "No Storage Locations Yet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            "Create your first garage to start organizing your inventory efficiently",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onAddGarage,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
            elevation = ButtonDefaults.buttonElevation(4.dp)
        ) {
            Icon(Icons.Default.Add, "Add", modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Create First Garage", style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun EmptyChildState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}