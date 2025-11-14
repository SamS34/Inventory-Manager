package com.samuel.inventorymanager.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddGarage) {
                Icon(Icons.Default.Add, contentDescription = "Add Garage")
            }
        }
    ) { paddingValues ->
        if (garages.isEmpty()) {
            EmptyState(onAddGarage, modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
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

// ======================================================================
//                              SECTION COMPOSABLES
// ======================================================================

@Composable
private fun GarageSection(garage: Garage, itemsInGarage: List<Item>, onAddCabinet: () -> Unit, onAddShelf: (cabinetId: String) -> Unit, onAddBox: (shelfId: String) -> Unit, onRenameLocation: (id: String, oldName: String, type: String) -> Unit, onDeleteLocation: (id: String, name: String, type: String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    val totalValue = itemsInGarage.sumOf { ((it.minPrice ?: 0.0) + (it.maxPrice ?: 0.0)) / 2 * it.quantity }

    Card(elevation = CardDefaults.cardElevation(4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))) {
        Column {
            SectionHeader(
                title = garage.name, icon = Icons.Default.HomeWork, isExpanded = isExpanded,
                onToggleExpand = { isExpanded = !isExpanded },
                onAddClick = onAddCabinet,
                onRenameClick = { onRenameLocation(garage.id, garage.name, "garage") },
                onDeleteClick = { onDeleteLocation(garage.id, garage.name, "garage") }
            ) {
                StatChip(icon = Icons.Default.Inventory2, text = "${itemsInGarage.size} items")
                StatChip(icon = Icons.Default.AttachMoney, text = "$${String.format(Locale.US, "%.2f", totalValue)}")
            }
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(start = 24.dp, end = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (garage.cabinets.isEmpty()) {
                        EmptyChildState("No cabinets here. Tap '+ Add' to create one.")
                    } else {
                        garage.cabinets.forEach { cabinet ->
                            val itemsInCabinet = itemsInGarage.filter { it.cabinetId == cabinet.id }
                            CabinetSection(cabinet, itemsInCabinet, { onAddShelf(cabinet.id) }, onAddBox, onRenameLocation, onDeleteLocation)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CabinetSection(cabinet: Cabinet, itemsInCabinet: List<Item>, onAddShelf: () -> Unit, onAddBox: (shelfId: String) -> Unit, onRenameLocation: (id: String, oldName: String, type: String) -> Unit, onDeleteLocation: (id: String, name: String, type: String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    Surface(shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))) {
        Column {
            SectionHeader(
                title = cabinet.name, icon = Icons.Default.Kitchen, isExpanded = isExpanded,
                onToggleExpand = { isExpanded = !isExpanded },
                onAddClick = onAddShelf,
                onRenameClick = { onRenameLocation(cabinet.id, cabinet.name, "cabinet") },
                onDeleteClick = { onDeleteLocation(cabinet.id, cabinet.name, "cabinet") }
            ) {
                StatChip(icon = Icons.Default.Inventory2, text = "${itemsInCabinet.size} items", small = true)
            }
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(start = 24.dp, end = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (cabinet.shelves.isEmpty()) {
                        EmptyChildState("No shelves in this cabinet.")
                    } else {
                        cabinet.shelves.forEach { shelf ->
                            val itemsInShelf = itemsInCabinet.filter { it.shelfId == shelf.id }
                            ShelfSection(shelf, itemsInShelf, { onAddBox(shelf.id) }, onRenameLocation, onDeleteLocation)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShelfSection(shelf: Shelf, itemsInShelf: List<Item>, onAddBox: () -> Unit, onRenameLocation: (id: String, oldName: String, type: String) -> Unit, onDeleteLocation: (id: String, name: String, type: String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    Surface(shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))) {
        Column {
            SectionHeader(
                title = shelf.name, icon = Icons.AutoMirrored.Filled.ViewList, isExpanded = isExpanded,
                onToggleExpand = { isExpanded = !isExpanded },
                onAddClick = onAddBox,
                onRenameClick = { onRenameLocation(shelf.id, shelf.name, "shelf") },
                onDeleteClick = { onDeleteLocation(shelf.id, shelf.name, "shelf") }
            ) {
                StatChip(icon = Icons.Default.Inventory2, text = "${itemsInShelf.size} items", small = true)
            }
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(start = 24.dp, end = 8.dp, bottom = 8.dp)) {
                    if (shelf.boxes.isEmpty()) {
                        EmptyChildState("No boxes on this shelf.")
                    } else {
                        shelf.boxes.forEach { box ->
                            val itemsInBox = itemsInShelf.filter { it.boxId == box.id }
                            BoxItem(box = box, itemCount = itemsInBox.size)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun SectionHeader(title: String, icon: ImageVector, isExpanded: Boolean, onToggleExpand: () -> Unit, onAddClick: () -> Unit, onRenameClick: () -> Unit, onDeleteClick: () -> Unit, statsContent: @Composable RowScope.() -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleExpand).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { statsContent() }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAddClick) { Icon(Icons.Default.Add, "+ Add") }
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "More Options") }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { showMenu = false; onRenameClick() })
                    DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; onDeleteClick() })
                }
            }
            Icon(imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "Toggle Section")
        }
    }
}

@Composable
private fun StatChip(icon: ImageVector, text: String, small: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(if (small) 12.dp else 14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(text, style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BoxItem(box: Box, itemCount: Int) {
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("└─", color = MaterialTheme.colorScheme.outline); Spacer(Modifier.width(4.dp))
        Icon(Icons.Default.Inventory, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(box.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text("$itemCount items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyState(onAddGarage: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.LocationCity, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        Text("No Locations Created Yet", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
        Text("Get started by adding your first garage.", textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAddGarage) { Icon(Icons.Default.Add, "Add Garage"); Spacer(Modifier.width(8.dp)); Text("Add Your First Garage") }
    }
}

@Composable
private fun EmptyChildState(message: String) {
    Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
}