package com.samuel.inventorymanager.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SortType {
    NAME_ASC, NAME_DESC, PRICE_ASC, PRICE_DESC, DATE_NEW, DATE_OLD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    items: List<Item>,
    garages: List<Garage>
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGarageId by remember { mutableStateOf<String?>(null) }
    var selectedSize by remember { mutableStateOf("All Sizes") }
    var selectedCondition by remember { mutableStateOf("All Conditions") }
    var showSizeMenu by remember { mutableStateOf(false) }
    var showConditionMenu by remember { mutableStateOf(false) }
    var sortType by remember { mutableStateOf(SortType.NAME_ASC) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedItemForDetail by remember { mutableStateOf<Item?>(null) }

    // Filter by garage if selected
    val garageFilteredItems = if (selectedGarageId != null) {
        items.filter { it.garageId == selectedGarageId }
    } else {
        items
    }

    // Apply other filters
    val filteredItems = remember(searchQuery, garageFilteredItems, selectedSize, selectedCondition, sortType) {
        var result = garageFilteredItems.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.modelNumber?.contains(searchQuery, ignoreCase = true) == true
            val matchesSize = selectedSize == "All Sizes" ||
                    item.sizeCategory.equals(selectedSize, ignoreCase = true)
            val matchesCondition = selectedCondition == "All Conditions" ||
                    item.condition.equals(selectedCondition, ignoreCase = true)
            matchesSearch && matchesSize && matchesCondition
        }

        // Apply sorting
        when (sortType) {
            SortType.NAME_ASC -> result.sortedBy { it.name.lowercase() }
            SortType.NAME_DESC -> result.sortedByDescending { it.name.lowercase() }
            SortType.PRICE_ASC -> result.sortedBy { ((it.minPrice ?: 0.0) + (it.maxPrice ?: 0.0)) / 2 }
            SortType.PRICE_DESC -> result.sortedByDescending { ((it.minPrice ?: 0.0) + (it.maxPrice ?: 0.0)) / 2 }
            SortType.DATE_NEW -> result.reversed() // Newest first (assuming items list is chronological)
            SortType.DATE_OLD -> result // Oldest first
        }
    }

    val totalItemsCount = filteredItems.size
    val totalQuantity = filteredItems.sumOf { it.quantity }
    val totalValue = filteredItems.sumOf { ((it.minPrice ?: 0.0) + (it.maxPrice ?: 0.0)) / 2 * it.quantity }
    val totalWeight = filteredItems.sumOf { (it.weight ?: 0.0) * it.quantity }
    val locationsUsed = filteredItems.map { it.garageId }.distinct().count()
    val avgItemValue = if (totalItemsCount > 0) totalValue / totalItemsCount else 0.0

    val conditions = items.map { it.condition }.distinct().sorted()

    // Show item detail PDF if selected
    if (selectedItemForDetail != null) {
        ItemDetailPDFScreen(
            item = selectedItemForDetail!!,
            garage = garages.find { it.id == selectedItemForDetail!!.garageId },
            onBackClick = { selectedItemForDetail = null }
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Inventory Overview",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { /* TODO: Refresh */ }) {
                    Icon(Icons.Default.Refresh, "Refresh", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search items...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }

        // Filter Row
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Filters",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterDropdownCompact(
                        label = selectedSize,
                        icon = Icons.Default.Straighten,
                        expanded = showSizeMenu,
                        onExpandChange = { showSizeMenu = it },
                        options = listOf("All Sizes", "Small", "Medium", "Large"),
                        onSelect = { selectedSize = it; showSizeMenu = false },
                        modifier = Modifier.weight(1f)
                    )

                    FilterDropdownCompact(
                        label = selectedCondition,
                        icon = Icons.Default.CheckCircle,
                        expanded = showConditionMenu,
                        onExpandChange = { showConditionMenu = it },
                        options = listOf("All Conditions") + conditions,
                        onSelect = { selectedCondition = it; showConditionMenu = false },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Sort Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sort by:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Box {
                    OutlinedButton(
                        onClick = { showSortMenu = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Sort, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            when (sortType) {
                                SortType.NAME_ASC -> "Name (A-Z)"
                                SortType.NAME_DESC -> "Name (Z-A)"
                                SortType.PRICE_ASC -> "Price (Low-High)"
                                SortType.PRICE_DESC -> "Price (High-Low)"
                                SortType.DATE_NEW -> "Newest First"
                                SortType.DATE_OLD -> "Oldest First"
                            },
                            fontSize = 12.sp
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Name (A-Z)") },
                            onClick = { sortType = SortType.NAME_ASC; showSortMenu = false },
                            leadingIcon = { Icon(Icons.Default.ArrowUpward, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Name (Z-A)") },
                            onClick = { sortType = SortType.NAME_DESC; showSortMenu = false },
                            leadingIcon = { Icon(Icons.Default.ArrowDownward, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Price (Low-High)") },
                            onClick = { sortType = SortType.PRICE_ASC; showSortMenu = false },
                            leadingIcon = { Icon(Icons.Default.ArrowUpward, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Price (High-Low)") },
                            onClick = { sortType = SortType.PRICE_DESC; showSortMenu = false },
                            leadingIcon = { Icon(Icons.Default.ArrowDownward, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Newest First") },
                            onClick = { sortType = SortType.DATE_NEW; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Oldest First") },
                            onClick = { sortType = SortType.DATE_OLD; showSortMenu = false }
                        )
                    }
                }
            }
        }
        // Key Statistics Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Key Statistics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CompactStatCard(
                        title = "Unique Items",
                        value = totalItemsCount.toString(),
                        icon = Icons.Default.Inventory,
                        color = Color(0xFF6200EE),
                        modifier = Modifier.weight(1f)
                    )
                    CompactStatCard(
                        title = "Total Qty",
                        value = totalQuantity.toString(),
                        icon = Icons.Default.Numbers,
                        color = Color(0xFF03DAC5),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CompactStatCard(
                        title = "Total Value",
                        value = "$${String.format("%.0f", totalValue)}",
                        icon = Icons.Default.AttachMoney,
                        color = Color(0xFF047857),
                        modifier = Modifier.weight(1f)
                    )
                    CompactStatCard(
                        title = "Avg Value",
                        value = "$${String.format("%.0f", avgItemValue)}",
                        icon = Icons.Default.TrendingUp,
                        color = Color(0xFFB45309),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CompactStatCard(
                        title = "Total Weight",
                        value = "${String.format("%.1f", totalWeight)} lbs",
                        icon = Icons.Default.Scale,
                        color = Color(0xFF0369A1),
                        modifier = Modifier.weight(1f)
                    )
                    CompactStatCard(
                        title = "Locations",
                        value = "$locationsUsed/${garages.size}",
                        icon = Icons.Default.LocationOn,
                        color = Color(0xFFBE185D),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Your Garages Section
        item {
            Text(
                "Your Garages",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(garages) { garage ->
                    GarageOverviewCard(
                        garageName = garage.name,
                        itemCount = items.count { it.garageId == garage.id },
                        isSelected = selectedGarageId == garage.id,
                        onClick = {
                            selectedGarageId = if (selectedGarageId == garage.id) null else garage.id
                        }
                    )
                }
            }
        }

        // Item List Header
        item {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(
                    "Items (${filteredItems.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Item List
        if (filteredItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No items found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredItems) { item ->
                SimpleItemCard(
                    item = item,
                    garages = garages,
                    onClick = { selectedItemForDetail = item }
                )
            }
        }
    }
}

@Composable
fun FilterDropdownCompact(
    label: String,
    icon: ImageVector,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { onExpandChange(true) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandChange(false) }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 13.sp) },
                    onClick = { onSelect(option) }
                )
            }
        }
    }
}

@Composable
fun CompactStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun GarageOverviewCard(
    garageName: String,
    itemCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(width = 140.dp, height = 100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 8.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.HomeWork,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onTertiaryContainer
            )
            Column {
                Text(
                    text = garageName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onTertiaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$itemCount items",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun SimpleItemCard(
    item: Item,
    garages: List<Garage>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    getReadableLocation(item, garages),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Row {
                        Text(
                            "Qty: ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${item.quantity}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    val avgPrice = ((item.minPrice ?: 0.0) + (item.maxPrice ?: 0.0)) / 2
                    if (avgPrice > 0) {
                        Text(
                            "$${String.format("%.0f", avgPrice)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF047857),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            when (item.condition.lowercase()) {
                                "new" -> Color(0xFF047857)
                                "like new" -> Color(0xFF1E40AF)
                                "good" -> Color(0xFFB45309)
                                "fair" -> Color(0xFFC2410C)
                                "poor" -> Color(0xFF9F1239)
                                else -> Color(0xFF4B5563)
                            },
                            CircleShape
                        )
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailPDFScreen(
    item: Item,
    garage: Garage?,
    onBackClick: () -> Unit
) {
    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Text(
                        text = item.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Item Name Header
            item {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Model Number (if exists)
            item.modelNumber?.let { model ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Model Number",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                model,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Basic Information Section
            item {
                DetailSection(title = "📋 Basic Information") {
                    DetailRow(label = "Quantity", value = item.quantity.toString())
                    DetailRow(label = "Condition", value = item.condition)
                    DetailRow(label = "Functionality", value = item.functionality)
                    DetailRow(label = "Size Category", value = item.sizeCategory)
                }
            }

            // Location Information
            item {
                DetailSection(title = "📍 Location") {
                    garage?.let { g ->
                        DetailRow(label = "Garage", value = g.name)

                        val cabinet = g.cabinets.find { it.id == item.cabinetId }
                        cabinet?.let { c ->
                            DetailRow(label = "Cabinet", value = c.name)

                            val shelf = c.shelves.find { it.id == item.shelfId }
                            shelf?.let { s ->
                                DetailRow(label = "Shelf", value = s.name)

                                val box = s.boxes.find { it.id == item.boxId }
                                box?.let { b ->
                                    DetailRow(label = "Box", value = b.name)
                                }
                            }
                        }
                    } ?: run {
                        DetailRow(label = "Location", value = "Unknown")
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Full Path: ${getReadableLocation(item, listOfNotNull(garage))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Physical Specifications
            if (item.dimensions != null || item.weight != null) {
                item {
                    DetailSection(title = "📏 Physical Specifications") {
                        item.dimensions?.let {
                            DetailRow(label = "Dimensions", value = it)
                        }
                        item.weight?.let {
                            DetailRow(label = "Weight", value = "$it lbs")
                        }
                    }
                }
            }

            // Pricing Information
            val avgPrice = ((item.minPrice ?: 0.0) + (item.maxPrice ?: 0.0)) / 2
            if (avgPrice > 0) {
                item {
                    DetailSection(title = "💰 Pricing") {
                        item.minPrice?.let {
                            DetailRow(
                                label = "Minimum Price",
                                value = "$${String.format("%.2f", it)}"
                            )
                        }
                        item.maxPrice?.let {
                            DetailRow(
                                label = "Maximum Price",
                                value = "$${String.format("%.2f", it)}"
                            )
                        }
                        DetailRow(
                            label = "Estimated Value (Avg)",
                            value = "$${String.format("%.2f", avgPrice)}",
                            highlighted = true
                        )
                        DetailRow(
                            label = "Total Value (× ${item.quantity})",
                            value = "$${String.format("%.2f", avgPrice * item.quantity)}",
                            highlighted = true
                        )
                    }
                }
            }

            // Description
            item.description?.let { desc ->
                if (desc.isNotBlank()) {
                    item {
                        DetailSection(title = "📝 Description") {
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
            }

            // Web Link
            item.webLink?.let { link ->
                if (link.isNotBlank()) {
                    item {
                        DetailSection(title = "🔗 Web Link") {
                            Text(
                                text = link,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Images Section
            if (item.images.isNotEmpty()) {
                item {
                    DetailSection(title = "📷 Images") {
                        Text(
                            "${item.images.size} image(s) attached",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Summary Statistics Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "📊 Quick Stats",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatBadge(
                                label = "Quantity",
                                value = item.quantity.toString(),
                                icon = Icons.Default.Numbers
                            )
                            if (avgPrice > 0) {
                                StatBadge(
                                    label = "Value",
                                    value = "$${String.format("%.0f", avgPrice * item.quantity)}",
                                    icon = Icons.Default.AttachMoney
                                )
                            }
                            item.weight?.let { w ->
                                StatBadge(
                                    label = "Weight",
                                    value = "${String.format("%.1f", w * item.quantity)} lbs",
                                    icon = Icons.Default.Scale
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    highlighted: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.5f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
            color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.5f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun StatBadge(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

fun getReadableLocation(item: Item, garages: List<Garage>): String {
    val garage = garages.find { it.id == item.garageId } ?: return "Unknown"
    val cabinet = garage.cabinets.find { it.id == item.cabinetId } ?: return garage.name
    val shelf = cabinet.shelves.find { it.id == item.shelfId } ?: return "${garage.name} > ${cabinet.name}"
    val box = shelf.boxes.find { it.id == item.boxId }

    val shelfAndBox = if (box != null) "${shelf.name} > ${box.name}" else shelf.name

    return if (garage.name.equals(cabinet.name, ignoreCase = true)) {
        shelfAndBox
    } else {
        "${cabinet.name} > $shelfAndBox"
    }
}