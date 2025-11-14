package com.samuel.inventorymanager.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
//                         NAVIGATION STATE DATA CLASS
// ======================================================================

sealed class DashboardScreen {
    object Main : DashboardScreen()
    data class GarageItems(val garage: Garage) : DashboardScreen()
    data class ItemDetail(val item: Item, val garage: Garage) : DashboardScreen()
    data class AnalyticsDetail(val type: AnalyticsType) : DashboardScreen()
}

enum class AnalyticsType {
    CATEGORY, CONDITION, LOCATION, SIZE
}

// ======================================================================
//                         MAIN DASHBOARD COMPOSABLE
// ======================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun DashboardScreen(
    items: List<Item>,
    garages: List<Garage>
) {
    var currentScreen by remember { mutableStateOf<DashboardScreen>(DashboardScreen.Main) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState is DashboardScreen.Main) {
                slideInHorizontally { fullWidth -> -fullWidth } with slideOutHorizontally { fullWidth -> fullWidth }
            } else {
                slideInHorizontally { fullWidth -> fullWidth } with slideOutHorizontally { fullWidth -> -fullWidth }
            }
        },
        label = "dashboard_animation"
    ) { screen ->
        when (screen) {
            is DashboardScreen.Main -> {
                MainDashboard(
                    items = items,
                    garages = garages,
                    onGarageClick = { clickedGarage ->
                        currentScreen = DashboardScreen.GarageItems(clickedGarage)
                    },
                    onAnalyticsClick = { analyticsType ->
                        currentScreen = DashboardScreen.AnalyticsDetail(analyticsType)
                    }
                )
            }
            is DashboardScreen.GarageItems -> {
                val itemsInGarage = items.filter { it.garageId == screen.garage.id }
                GarageItemsView(
                    garage = screen.garage,
                    items = itemsInGarage,
                    onBackClick = { currentScreen = DashboardScreen.Main },
                    onItemClick = { clickedItem ->
                        currentScreen = DashboardScreen.ItemDetail(clickedItem, screen.garage)
                    }
                )
            }
            is DashboardScreen.ItemDetail -> {
                ItemDetailScreen(
                    item = screen.item,
                    garage = screen.garage,
                    onBackClick = {
                        currentScreen = DashboardScreen.GarageItems(screen.garage)
                    }
                )
            }
            is DashboardScreen.AnalyticsDetail -> {
                AnalyticsDetailScreen(
                    analyticsType = screen.type,
                    items = items,
                    garages = garages,
                    onBackClick = { currentScreen = DashboardScreen.Main }
                )
            }
        }
    }
}

// ======================================================================
//                         MAIN DASHBOARD UI (CLEANED)
// ======================================================================

@Composable
fun MainDashboard(
    items: List<Item>,
    garages: List<Garage>,
    onGarageClick: (Garage) -> Unit,
    onAnalyticsClick: (AnalyticsType) -> Unit
) {
    var totalItemsExpanded by remember { mutableStateOf(false) }
    var totalValueExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Statistics Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExpandableStatCard(
                    title = "Total Items",
                    value = items.size.toString(),
                    icon = Icons.Default.Inventory,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    isExpanded = totalItemsExpanded,
                    onToggle = { totalItemsExpanded = !totalItemsExpanded }
                ) {
                    ItemsBreakdown(items, garages)
                }

                ExpandableStatCard(
                    title = "Total Value",
                    value = "$${String.format("%.2f", items.sumOf { ((it.minPrice ?: 0.0) + (it.maxPrice ?: 0.0)) / 2 * it.quantity })}",
                    icon = Icons.Default.AttachMoney,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    isExpanded = totalValueExpanded,
                    onToggle = { totalValueExpanded = !totalValueExpanded }
                ) {
                    ValueBreakdown(items, garages)
                }
            }
        }

        // Analytics Section
        item {
            Text(
                text = "Analytics Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Analytics Grid - 2x2
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ClickableChartCard(
                        title = "By Category",
                        icon = Icons.Default.Category,
                        modifier = Modifier.weight(1f),
                        onClick = { onAnalyticsClick(AnalyticsType.CATEGORY) }
                    ) {
                        MiniCategoryChart(items)
                    }
                    ClickableChartCard(
                        title = "By Condition",
                        icon = Icons.Default.Diamond,
                        modifier = Modifier.weight(1f),
                        onClick = { onAnalyticsClick(AnalyticsType.CONDITION) }
                    ) {
                        MiniConditionChart(items)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ClickableChartCard(
                        title = "By Location",
                        icon = Icons.Default.LocationOn,
                        modifier = Modifier.weight(1f),
                        onClick = { onAnalyticsClick(AnalyticsType.LOCATION) }
                    ) {
                        MiniLocationChart(items, garages)
                    }
                    ClickableChartCard(
                        title = "By Size",
                        icon = Icons.Default.Scale,
                        modifier = Modifier.weight(1f),
                        onClick = { onAnalyticsClick(AnalyticsType.SIZE) }
                    ) {
                        MiniSizeChart(items)
                    }
                }
            }
        }

        // Your Garages
        item {
            Text(
                text = "Your Garages",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(garages) { garage ->
                    GarageStatCard(
                        garageName = garage.name,
                        itemCount = items.count { it.garageId == garage.id },
                        onClick = { onGarageClick(garage) }
                    )
                }
            }
        }
    }
}

// ======================================================================
//                    ANALYTICS DETAIL SCREEN
// ======================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDetailScreen(
    analyticsType: AnalyticsType,
    items: List<Item>,
    garages: List<Garage>,
    onBackClick: () -> Unit
) {
    val title = when (analyticsType) {
        AnalyticsType.CATEGORY -> "Category Analytics"
        AnalyticsType.CONDITION -> "Condition Analytics"
        AnalyticsType.LOCATION -> "Location Analytics"
        AnalyticsType.SIZE -> "Size Analytics"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when (analyticsType) {
                AnalyticsType.CATEGORY -> {
                    item { DetailedCategoryAnalytics(items) }
                }
                AnalyticsType.CONDITION -> {
                    item { DetailedConditionAnalytics(items) }
                }
                AnalyticsType.LOCATION -> {
                    item { DetailedLocationAnalytics(items, garages) }
                }
                AnalyticsType.SIZE -> {
                    item { DetailedSizeAnalytics(items) }
                }
            }
        }
    }
}

// ======================================================================
//                    DETAILED ANALYTICS COMPONENTS
// ======================================================================

@Composable
fun DetailedCategoryAnalytics(items: List<Item>) {
    val categories = items.groupBy { it.sizeCategory }        .mapValues { entry ->
            val itemList = entry.value
            Triple(
                itemList.size,
                itemList.sumOf { it.quantity },
                itemList.sumOf { ((it.minPrice ?: 0.0) + (it.maxPrice ?: 0.0)) / 2 * it.quantity }
            )
        }
        .toList()
        .sortedByDescending { it.second.first }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Large Pie Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Category Distribution",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                AdvancedPieChart(
                    data = categories.map { it.first to it.second.first },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            }
        }

        // Statistics Table
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Category Breakdown",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Category", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.3f))
                    Text("Items", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
                    Text("Qty", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
                    Text("Value", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.3f), textAlign = TextAlign.End)
                }
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))

                categories.forEach { (category, stats) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(category, modifier = Modifier.weight(0.3f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${stats.first}", modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
                        Text("${stats.second}", modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
                        Text("$${String.format("%.0f", stats.third)}", modifier = Modifier.weight(0.3f), textAlign = TextAlign.End)
                    }
                    Divider()
                }
            }
        }

        // Top Category Highlight
        if (categories.isNotEmpty()) {
            val top = categories.first()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Top Category", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = top.first,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("${top.second.first} unique items • ${top.second.second} total quantity • $${String.format("%.2f", top.second.third)} value")
                }
            }
        }
    }
}

@Composable
fun DetailedConditionAnalytics(items: List<Item>) {
    val conditions = items.groupBy { it.condition }
        .mapValues { entry ->
            val itemList = entry.value
            Triple(
                itemList.size,
                itemList.sumOf { it.quantity },
                itemList.sumOf { ((it.minPrice ?: 0.0) + (it.maxPrice ?: 0.0)) / 2 * it.quantity }
            )
        }
        .toList()
        .sortedByDescending { it.second.first }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Condition Distribution",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                AdvancedPieChart(
                    data = conditions.map { it.first to it.second.first },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Condition Breakdown",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Condition", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.3f))
                    Text("Items", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
                    Text("Qty", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
                    Text("Value", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.3f), textAlign = TextAlign.End)
                }
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))

                conditions.forEach { (condition, stats) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(condition, modifier = Modifier.weight(0.3f))
                        Text("${stats.first}", modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
                        Text("${stats.second}", modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
                        Text("$${String.format("%.0f", stats.third)}", modifier = Modifier.weight(0.3f), textAlign = TextAlign.End)
                    }
                    Divider()
                }
            }
        }
    }
}

@Composable
fun DetailedLocationAnalytics(items: List<Item>, garages: List<Garage>) {
    val locationData = garages.map { garage ->
        val garageItems = items.filter { it.garageId == garage.id }
        garage.name to Triple(
            garageItems.size,
            garageItems.sumOf { it.quantity },
            garageItems.sumOf { ((it.minPrice ?: 0.0) + (it.maxPrice ?: 0.0)) / 2 * it.quantity }
        )
    }.filter { it.second.first > 0 }
        .sortedByDescending { it.second.first }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Location Distribution",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                AdvancedBarChart(
                    data = locationData.map { it.first to it.second.first },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Location Breakdown",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Location", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.3f))
                    Text("Items", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
                    Text("Qty", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
                    Text("Value", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.3f), textAlign = TextAlign.End)
                }
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))

                locationData.forEach { (location, stats) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(location, modifier = Modifier.weight(0.3f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${stats.first}", modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
                        Text("${stats.second}", modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
                        Text("$${String.format("%.0f", stats.third)}", modifier = Modifier.weight(0.3f), textAlign = TextAlign.End)
                    }
                    Divider()
                }
            }
        }
    }
}

@Composable
fun DetailedSizeAnalytics(items: List<Item>) {
    val sizes = items.groupBy { it.sizeCategory }
        .mapValues { entry ->
            val itemList = entry.value
            Triple(
                itemList.size,
                itemList.sumOf { it.quantity },
                itemList.sumOf { ((it.minPrice ?: 0.0) + (it.maxPrice ?: 0.0)) / 2 * it.quantity }
            )
        }
        .toList()
        .sortedByDescending { it.second.first }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Size Distribution",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                AdvancedPieChart(
                    data = sizes.map { it.first to it.second.first },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Size Breakdown",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Size", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.3f))
                    Text("Items", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
                    Text("Qty", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
                    Text("Value", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.3f), textAlign = TextAlign.End)
                }
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))

                sizes.forEach { (size, stats) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(size, modifier = Modifier.weight(0.3f))
                        Text("${stats.first}", modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
                        Text("${stats.second}", modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
                        Text("$${String.format("%.0f", stats.third)}", modifier = Modifier.weight(0.3f), textAlign = TextAlign.End)
                    }
                    Divider()
                }
            }
        }
    }
}

// ======================================================================
//                    ADVANCED CHART COMPONENTS
// ======================================================================

@Composable
fun AdvancedPieChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.second }.toFloat()
    val colors = listOf(
        Color(0xFF6200EE),
        Color(0xFF03DAC5),
        Color(0xFFFF6B6B),
        Color(0xFF4ECDC4),
        Color(0xFFFFBE0B),
        Color(0xFFFFA500),
        Color(0xFF9C27B0),
        Color(0xFF00BCD4)
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pie Chart
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .weight(1f)
        ) {
            val canvasSize = size.minDimension
            val radius = canvasSize / 2.5f
            val center = Offset(size.width / 2, size.height / 2)

            var startAngle = -90f
            data.forEachIndexed { index, (_, value) ->
                val sweepAngle = (value / total) * 360f
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

            // Inner white circle for donut effect
            drawCircle(
                color = Color.White,
                radius = radius * 0.5f,
                center = center
            )
        }

        // Legend
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.forEachIndexed { index, (label, value) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(colors[index % colors.size], CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$value items (${String.format("%.1f", (value / total) * 100)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdvancedBarChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val maxValue = data.maxOfOrNull { it.second }?.toFloat() ?: 1f
    val barColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        data.forEach { (label, value) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(value / maxValue)
                            .height(32.dp)
                            .background(
                                barColor,
                                RoundedCornerShape(8.dp)
                            )
                    )
                }
            }
        }
    }
}

// ======================================================================
//                    MINI CHART COMPONENTS FOR DASHBOARD
// ======================================================================

@Composable
fun MiniCategoryChart(items: List<Item>) {
    val categories = items.groupBy { it.sizeCategory }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
        .take(3)

    if (categories.isEmpty()) {
        Text("No data", style = MaterialTheme.typography.bodySmall)
        return
    }

    MiniPieChart(categories)
}

@Composable
fun MiniConditionChart(items: List<Item>) {
    val conditions = items.groupBy { it.condition }
        .mapValues { it.value.size }
        .toList()
        .take(3)

    if (conditions.isEmpty()) {
        Text("No data", style = MaterialTheme.typography.bodySmall)
        return
    }

    MiniPieChart(conditions)
}

@Composable
fun MiniLocationChart(items: List<Item>, garages: List<Garage>) {
    val locationData = garages.map { garage ->
        garage.name to items.count { it.garageId == garage.id }
    }.filter { it.second > 0 }
        .take(3)

    if (locationData.isEmpty()) {
        Text("No data", style = MaterialTheme.typography.bodySmall)
        return
    }

    MiniPieChart(locationData)
}

@Composable
fun MiniSizeChart(items: List<Item>) {
    val sizes = items.groupBy { it.sizeCategory }
        .mapValues { it.value.size }
        .toList()
        .take(3)

    if (sizes.isEmpty()) {
        Text("No data", style = MaterialTheme.typography.bodySmall)
        return
    }

    MiniPieChart(sizes)
}

@Composable
fun MiniPieChart(data: List<Pair<String, Int>>) {
    val total = data.sumOf { it.second }.toFloat()
    val colors = listOf(
        Color(0xFF6200EE),
        Color(0xFF03DAC5),
        Color(0xFFFF6B6B)
    )

    Canvas(
        modifier = Modifier.size(80.dp)
    ) {
        val canvasSize = size.minDimension
        val radius = canvasSize / 2
        val center = Offset(size.width / 2, size.height / 2)

        var startAngle = -90f
        data.forEachIndexed { index, (_, value) ->
            val sweepAngle = (value / total) * 360f
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
    }
}

// ======================================================================
//                         EXPANDABLE STAT CARD
// ======================================================================

@Composable
fun ExpandableStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    modifier = Modifier.size(28.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Divider()
                    Spacer(Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}

// ======================================================================
//                         CLICKABLE CHART CARD
// ======================================================================

@Composable
fun ClickableChartCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .height(180.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

// ======================================================================
//                         BREAKDOWN COMPONENTS
// ======================================================================

@Composable
fun ItemsBreakdown(items: List<Item>, garages: List<Garage>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        garages.forEach { garage ->
            val garageItems = items.filter { it.garageId == garage.id }
            if (garageItems.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = garage.name,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${garageItems.size} items",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Divider(modifier = Modifier.padding(vertical = 4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Unique Items", fontWeight = FontWeight.Bold)
            Text(items.size.toString(), fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Total Quantity", fontWeight = FontWeight.Bold)
            Text(items.sumOf { it.quantity }.toString(), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ValueBreakdown(items: List<Item>, garages: List<Garage>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        garages.forEach { garage ->
            val garageItems = items.filter { it.garageId == garage.id }
            if (garageItems.isNotEmpty()) {
                val garageValue = garageItems.sumOf {
                    ((it.minPrice ?: 0.0) + (it.maxPrice ?: 0.0)) / 2 * it.quantity
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = garage.name,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$${String.format("%.2f", garageValue)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Divider(modifier = Modifier.padding(vertical = 4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Average Item Value", fontWeight = FontWeight.Bold)
            val avgValue = if (items.isNotEmpty())
                items.sumOf { ((it.minPrice ?: 0.0) + (it.maxPrice ?: 0.0)) / 2 } / items.size
            else 0.0
            Text("$${String.format("%.2f", avgValue)}", fontWeight = FontWeight.Bold)
        }
    }
}

// ======================================================================
//                         GARAGE ITEMS LIST VIEW
// ======================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageItemsView(
    garage: Garage,
    items: List<Item>,
    onBackClick: () -> Unit,
    onItemClick: (Item) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(garage.name) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No items found in this garage.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                items(items) { item ->
                    CompactItemCard(
                        item = item,
                        onClick = { onItemClick(item) }
                    )
                }
            }
        }
    }
}

// ======================================================================
//                         ITEM DETAIL SCREEN
// ======================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    item: Item,
    garage: Garage,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = item.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            item {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                InfoSection(title = "Basic Information") {
                    DetailRow("Quantity", item.quantity.toString())
                    DetailRow("Condition", item.condition)
                    DetailRow("Functionality", item.functionality)
                    DetailRow("Size Category", item.sizeCategory)
                    item.modelNumber?.let { DetailRow("Model Number", it) }
                }
            }

            item {
                InfoSection(title = "Location") {
                    DetailRow("Garage", garage.name)
                }
            }

            if (item.dimensions != null || item.weight != null) {
                item {
                    InfoSection(title = "Physical Specifications") {
                        item.dimensions?.let { DetailRow("Dimensions", it) }
                        item.weight?.let { DetailRow("Weight", "${it} lbs") }
                    }
                }
            }

            val avgPrice = ((item.minPrice ?: 0.0) + (item.maxPrice ?: 0.0)) / 2
            if (avgPrice > 0) {
                item {
                    InfoSection(title = "Pricing") {
                        item.minPrice?.let {
                            DetailRow("Min Price", "$${String.format("%.2f", it)}")
                        }
                        item.maxPrice?.let {
                            DetailRow("Max Price", "$${String.format("%.2f", it)}")
                        }
                        DetailRow("Est. Value", "$${String.format("%.2f", avgPrice)}")
                        DetailRow(
                            "Total Value",
                            "$${String.format("%.2f", avgPrice * item.quantity)}"
                        )
                    }
                }
            }

            item.description?.let { desc ->
                if (desc.isNotBlank()) {
                    item {
                        InfoSection(title = "Description") {
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            item.webLink?.let { link ->
                if (link.isNotBlank()) {
                    item {
                        InfoSection(title = "Web Link") {
                            Text(
                                text = link,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ======================================================================
//                         HELPER COMPOSABLES
// ======================================================================

@Composable
fun GarageStatCard(
    garageName: String,
    itemCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(width = 140.dp, height = 100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(2.dp)
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
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Column {
                Text(
                    text = garageName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$itemCount items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun CompactItemCard(
    item: Item,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Qty: ${item.quantity} • ${item.condition}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = "View details",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InfoSection(
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
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.6f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}