package com.samuel.inventorymanager.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ======================================================================
// MAIN OVERVIEW SCREEN - REDESIGNED
// ======================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    items: List<Item>,
    garages: List<Garage>
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("All Locations") }
    var selectedSize by remember { mutableStateOf("All Sizes") }
    var selectedCondition by remember { mutableStateOf("All Conditions") }
    var showLocationMenu by remember { mutableStateOf(false) }
    var showSizeMenu by remember { mutableStateOf(false) }
    var showConditionMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var expandedStats by remember { mutableStateOf(true) }
    var expandedAnalytics by remember { mutableStateOf(true) }

    val filteredItems = remember(searchQuery, selectedLocation, selectedSize, selectedCondition, items) {
        items.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.modelNumber?.contains(searchQuery, ignoreCase = true) == true
            val matchesLocation = selectedLocation == "All Locations" ||
                    (garages.find { it.id == item.garageId }?.name == selectedLocation)
            val matchesSize = selectedSize == "All Sizes" ||
                    item.sizeCategory.equals(selectedSize, ignoreCase = true)
            val matchesCondition = selectedCondition == "All Conditions" ||
                    item.condition.equals(selectedCondition, ignoreCase = true)
            matchesSearch && matchesLocation && matchesSize && matchesCondition
        }
    }

    val totalItemsCount = filteredItems.size
    val totalQuantity = filteredItems.sumOf { it.quantity }
    val totalValue = filteredItems.sumOf { ((it.minPrice ?: 0.0) + (it.maxPrice ?: 0.0)) / 2 * it.quantity }
    val totalWeight = filteredItems.sumOf { (it.weight ?: 0.0) * it.quantity }
    val locationsUsed = filteredItems.map { it.garageId }.distinct().count()
    val avgItemValue = if (totalItemsCount > 0) totalValue / totalItemsCount else 0.0

    val conditions = items.map { it.condition }.distinct().sorted()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
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
                IconButton(onClick = { /* TODO: Export */ }) {
                    Icon(Icons.Default.FileDownload, "Export", tint = MaterialTheme.colorScheme.primary)
                }
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
                placeholder = { Text("Search by name or model number...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
        }

        // Filter Row
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterDropdown(
                    label = selectedLocation,
                    icon = Icons.Default.LocationOn,
                    expanded = showLocationMenu,
                    onExpandChange = { showLocationMenu = it },
                    options = listOf("All Locations") + garages.map { it.name },
                    onSelect = { selectedLocation = it; showLocationMenu = false },
                    modifier = Modifier.weight(1f)
                )

                FilterDropdown(
                    label = selectedSize,
                    icon = Icons.Default.Straighten,
                    expanded = showSizeMenu,
                    onExpandChange = { showSizeMenu = it },
                    options = listOf("All Sizes", "Small", "Medium", "Large"),
                    onSelect = { selectedSize = it; showSizeMenu = false },
                    modifier = Modifier.weight(1f)
                )

                FilterDropdown(
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

        // Statistics Section
        item {
            ExpandableSection(
                title = "Key Statistics",
                subtitle = "${filteredItems.size} items",
                icon = Icons.Default.TrendingUp,
                isExpanded = expandedStats,
                onToggle = { expandedStats = !expandedStats }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ModernStatCard(
                            title = "Unique Items",
                            value = totalItemsCount.toString(),
                            icon = Icons.Default.Inventory,
                            color = Color(0xFF6200EE),
                            modifier = Modifier.weight(1f)
                        )
                        ModernStatCard(
                            title = "Total Quantity",
                            value = totalQuantity.toString(),
                            icon = Icons.Default.Numbers,
                            color = Color(0xFF03DAC5),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ModernStatCard(
                            title = "Total Value",
                            value = "$${String.format("%.0f", totalValue)}",
                            icon = Icons.Default.AttachMoney,
                            color = Color(0xFF047857),
                            modifier = Modifier.weight(1f)
                        )
                        ModernStatCard(
                            title = "Avg Value",
                            value = "$${String.format("%.0f", avgItemValue)}",
                            icon = Icons.Default.TrendingUp,
                            color = Color(0xFFB45309),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ModernStatCard(
                            title = "Total Weight",
                            value = "${String.format("%.1f", totalWeight)} lbs",
                            icon = Icons.Default.Scale,
                            color = Color(0xFF0369A1),
                            modifier = Modifier.weight(1f)
                        )
                        ModernStatCard(
                            title = "Locations Used",
                            value = "$locationsUsed/${garages.size}",
                            icon = Icons.Default.LocationCity,
                            color = Color(0xFFBE185D),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Advanced Analytics Section
        item {
            ExpandableSection(
                title = "Advanced Analytics",
                subtitle = "Visual breakdown",
                icon = Icons.Default.Category,
                isExpanded = expandedAnalytics,
                onToggle = { expandedAnalytics = !expandedAnalytics }
            ) {
                Column {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Category", fontSize = 13.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Condition", fontSize = 13.sp) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Location", fontSize = 13.sp) }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("Size", fontSize = 13.sp) }
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    when (selectedTab) {
                        0 -> AdvancedAnalyticsView(
                            groupedItems = filteredItems.groupBy { it.sizeCategory },
                            label = "Category"
                        )
                        1 -> AdvancedAnalyticsView(
                            groupedItems = filteredItems.groupBy { it.condition },
                            label = "Condition"
                        )
                        2 -> {
                            val locationData = garages.associate { garage ->
                                garage.name to filteredItems.filter { it.garageId == garage.id }
                            }.filter { it.value.isNotEmpty() }
                            AdvancedAnalyticsView(locationData, "Location")
                        }
                        3 -> AdvancedAnalyticsView(
                            groupedItems = filteredItems.groupBy { it.sizeCategory },
                            label = "Size"
                        )
                    }
                }
            }
        }

        // Item List
        item {
            Text(
                "Item List (${filteredItems.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(filteredItems) { item ->
            ModernItemCard(item = item, garages = garages)
        }
    }
}

// ======================================================================
// MODERN COMPONENTS
// ======================================================================

@Composable
fun FilterDropdown(
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
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp
            )
            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandChange(false) }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(option) }
                )
            }
        }
    }
}

@Composable
fun ModernStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = color.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ExpandableSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        icon,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    "Toggle",
                    modifier = Modifier.size(28.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(20.dp))
                    content()
                }
            }
        }
    }
}

@Composable
fun AdvancedAnalyticsView(
    groupedItems: Map<String, List<Item>>,
    label: String
) {
    val sortedData = groupedItems.map { (key, items) ->
        val totalValue = items.sumOf { ((it.minPrice ?: 0.0) + (it.maxPrice ?: 0.0)) / 2 * it.quantity }
        val totalQty = items.sumOf { it.quantity }
        AnalyticsData(key, items.size, totalQty, totalValue)
    }.sortedByDescending { it.totalValue }

    val maxValue = sortedData.maxOfOrNull { it.totalValue } ?: 1.0
    val total = sortedData.sumOf { it.itemCount }

    val colors = listOf(
        Color(0xFF6200EE),
        Color(0xFF03DAC5),
        Color(0xFFFF6B6B),
        Color(0xFF4ECDC4),
        Color(0xFFFFBE0B),
        Color(0xFFFFA500)
    )

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Pie Chart with Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier.size(180.dp)
            ) {
                val canvasSize = size.minDimension
                val radius = canvasSize / 2
                val center = Offset(size.width / 2, size.height / 2)

                var startAngle = -90f
                sortedData.forEachIndexed { index, data ->
                    val sweepAngle = (data.itemCount.toFloat() / total) * 360f
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2)
                    )
                    startAngle += sweepAngle
                }

                drawCircle(
                    color = Color.White,
                    radius = radius * 0.5f,
                    center = center
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sortedData.take(6).forEachIndexed { index, data ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(colors[index % colors.size], CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${data.name} (${data.itemCount})",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Divider()

        // Data Table
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    label,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.3f),
                    fontSize = 13.sp
                )
                Text(
                    "Items",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.2f),
                    fontSize = 13.sp
                )
                Text(
                    "Qty",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.2f),
                    fontSize = 13.sp
                )
                Text(
                    "Value",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.3f),
                    fontSize = 13.sp
                )
            }

            sortedData.forEachIndexed { index, data ->
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(0.3f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(colors[index % colors.size], CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                data.name,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            "${data.itemCount}",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.2f),
                            fontSize = 13.sp
                        )
                        Text(
                            "${data.totalQuantity}",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.2f),
                            fontSize = 13.sp
                        )
                        Text(
                            "$${String.format("%.0f", data.totalValue)}",
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.3f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (data.totalValue / maxValue).toFloat())
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors[index % colors.size])
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernItemCard(
    item: Item,
    garages: List<Garage>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    getReadableLocation(item, garages),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Qty: ${item.quantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val avgPrice = ((item.minPrice ?: 0.0) + (item.maxPrice ?: 0.0)) / 2
                    if (avgPrice > 0) {
                        Text(
                            "$${String.format("%.0f", avgPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF047857),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                ConditionIndicator(item.condition)
            }
        }
    }
}

@Composable
fun ConditionIndicator(condition: String) {
    val color = when (condition.lowercase()) {
        "new" -> Color(0xFF047857)
        "like new" -> Color(0xFF1E40AF)
        "good" -> Color(0xFFB45309)
        "fair" -> Color(0xFFC2410C)
        "poor" -> Color(0xFF9F1239)
        else -> Color(0xFF4B5563)
    }

    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color, CircleShape)
    )
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

data class AnalyticsData(
    val name: String,
    val itemCount: Int,
    val totalQuantity: Int,
    val totalValue: Double
)