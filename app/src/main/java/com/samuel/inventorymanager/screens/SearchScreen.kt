package com.samuel.inventorymanager.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    items: List<Item>,
    garages: List<Garage>,
    onItemClick: ((Item) -> Unit)? = null,
    onEditItem: ((Item) -> Unit)? = null,
    onDeleteItem: ((Item) -> Unit)? = null,
    onDuplicateItem: ((Item) -> Unit)? = null,
    onShareItem: ((Item) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All Categories") }
    var selectedLocation by remember { mutableStateOf("All Locations") }
    var selectedCondition by remember { mutableStateOf("All Conditions") }
    var isFiltersExpanded by remember { mutableStateOf(false) }

    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var showItemActionsSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Create dropdown options
    val categories = remember(items) {
        listOf("All Categories") + items.map { it.sizeCategory }.distinct().sorted()
    }

    val locations = remember(garages) {
        listOf("All Locations") + garages.map { it.name }.sorted()
    }

    val conditions = listOf("All Conditions", "Excellent", "Good", "Fair", "Poor")

    // Filter items based on search criteria
    val filteredItems by remember(searchQuery, selectedCategory, selectedLocation, selectedCondition, items) {
        derivedStateOf {
            items.filter { item ->
                val matchesSearch = searchQuery.isBlank() ||
                        item.name.contains(searchQuery, ignoreCase = true) ||
                        item.modelNumber?.contains(searchQuery, ignoreCase = true) == true ||
                        item.description?.contains(searchQuery, ignoreCase = true) == true

                val matchesCategory = selectedCategory == "All Categories" ||
                        item.sizeCategory == selectedCategory

                val matchesLocation = selectedLocation == "All Locations" ||
                        garages.find { it.id == item.garageId }?.name == selectedLocation

                val matchesCondition = selectedCondition == "All Conditions" ||
                        item.condition == selectedCondition

                matchesSearch && matchesCategory && matchesLocation && matchesCondition
            }.sortedBy { it.name }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Compact Search Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(0.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Main Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by name, model, or description") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filters Toggle Button
                OutlinedButton(
                    onClick = { isFiltersExpanded = !isFiltersExpanded },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isFiltersExpanded)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Advanced Filters",
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(if (isFiltersExpanded) 180f else 0f)
                    )
                }

                // Expandable Filters Section
                AnimatedVisibility(
                    visible = isFiltersExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        // Category Dropdown
                        DropdownMenuField(
                            label = "Category",
                            selectedValue = selectedCategory,
                            options = categories,
                            onValueChange = { selectedCategory = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Location Dropdown
                        DropdownMenuField(
                            label = "Location",
                            selectedValue = selectedLocation,
                            options = locations,
                            onValueChange = { selectedLocation = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Condition Dropdown
                        DropdownMenuField(
                            label = "Condition",
                            selectedValue = selectedCondition,
                            options = conditions,
                            onValueChange = { selectedCondition = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Clear Filters Button
                        OutlinedButton(
                            onClick = {
                                selectedCategory = "All Categories"
                                selectedLocation = "All Locations"
                                selectedCondition = "All Conditions"
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Filters")
                        }
                    }
                }
            }
        }

        // Results Count Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredItems.size} item${if (filteredItems.size != 1) "s" else ""} found",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (searchQuery.isNotEmpty() || selectedCategory != "All Categories" ||
                selectedLocation != "All Locations" || selectedCondition != "All Conditions") {
                Text(
                    text = "Clear All",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        searchQuery = ""
                        selectedCategory = "All Categories"
                        selectedLocation = "All Locations"
                        selectedCondition = "All Conditions"
                    }
                )
            }
        }

        // Results List
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isEmpty() && selectedCategory == "All Categories" &&
                            selectedLocation == "All Locations" && selectedCondition == "All Conditions") {
                            "Enter search terms or use filters"
                        } else {
                            "No items found"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    ImprovedItemCard(
                        item = item,
                        garages = garages,
                        onClick = {
                            selectedItem = item
                            showItemActionsSheet = true
                        }
                    )
                }
            }
        }
    }

    // Item Actions Bottom Sheet
    if (showItemActionsSheet && selectedItem != null) {
        ItemActionsBottomSheet(
            item = selectedItem!!,
            garages = garages,
            onDismiss = { showItemActionsSheet = false },
            onEdit = {
                showItemActionsSheet = false
                onEditItem?.invoke(selectedItem!!)
            },
            onView = {
                showItemActionsSheet = false
                onItemClick?.invoke(selectedItem!!)
            },
            onDuplicate = {
                showItemActionsSheet = false
                onDuplicateItem?.invoke(selectedItem!!)
            },
            onShare = {
                showItemActionsSheet = false
                onShareItem?.invoke(selectedItem!!)
            },
            onDelete = {
                showItemActionsSheet = false
                showDeleteDialog = true
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && selectedItem != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Item?") },
            text = { Text("Are you sure you want to delete '${selectedItem!!.name}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteItem?.invoke(selectedItem!!)
                        showDeleteDialog = false
                        selectedItem = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemActionsBottomSheet(
    item: Item,
    garages: List<Garage>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onView: () -> Unit,
    onDuplicate: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header with item info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.modelNumber != null) {
                    Text(
                        text = "Model: ${item.modelNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                val locationPath = buildLocationPath(item, garages)
                Text(
                    text = locationPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Action Items
            ActionSheetItem(
                icon = Icons.Default.Edit,
                title = "Edit Item",
                description = "Modify item details and information",
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onEdit()
                    }
                }
            )

            ActionSheetItem(
                icon = Icons.Default.ContentCopy,
                title = "Duplicate Item",
                description = "Create a copy of this item",
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onDuplicate()
                    }
                }
            )

            ActionSheetItem(
                icon = Icons.Default.Share,
                title = "Share Item",
                description = "Share item details with others",
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onShare()
                    }
                }
            )

            ActionSheetItem(
                icon = Icons.Default.Delete,
                title = "Delete Item",
                description = "Permanently remove this item",
                isDestructive = true,
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onDelete()
                    }
                }
            )
        }
    }
}

@Composable
private fun ActionSheetItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownMenuField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ImprovedItemCard(
    item: Item,
    garages: List<Garage>,
    onClick: () -> Unit
) {
    val locationPath = remember(item, garages) {
        buildLocationPath(item, garages)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Item Name
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (item.modelNumber != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Model: ${item.modelNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Location
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = locationPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Additional Info Row
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (item.quantity > 0) {
                        InfoChip(
                            label = "Qty",
                            value = item.quantity.toString()
                        )
                    }
                    if (item.sizeCategory.isNotEmpty()) {
                        InfoChip(
                            label = "Size",
                            value = item.sizeCategory
                        )
                    }
                    if (item.condition.isNotEmpty()) {
                        InfoChip(
                            label = "Condition",
                            value = item.condition
                        )
                    }
                }
            }

            // Right Arrow
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More Options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun InfoChip(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.secondaryContainer,
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

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