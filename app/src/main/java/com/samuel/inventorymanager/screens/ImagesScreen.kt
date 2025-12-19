package com.samuel.inventorymanager.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter

// Make sure your Item class is imported/accessible
// import com.samuel.inventorymanager.data.Item

// We define the available sort options here
private enum class SortOption {
    NAME_AZ,
    NAME_ZA,
    MOST_IMAGES,
    FEWEST_IMAGES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagesScreen(
    items: List<Item>,
    onNavigateToDetail: (Item) -> Unit
) {
    // State for search query, sort order, and menu visibility
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SortOption.NAME_AZ) }
    var showSortMenu by remember { mutableStateOf(false) }

    // This is the core logic: a derived list that re-calculates whenever the source data or filters change
    val filteredAndSortedItems = remember(items, searchQuery, sortOption) {
        val itemsWithImages = items.filter { it.images.isNotEmpty() }

        // 1. Filter by search query
        val searchedItems = if (searchQuery.isBlank()) {
            itemsWithImages
        } else {
            itemsWithImages.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }

        // 2. Sort the filtered results
        when (sortOption) {
            SortOption.NAME_AZ -> searchedItems.sortedBy { it.name.lowercase() }
            SortOption.NAME_ZA -> searchedItems.sortedByDescending { it.name.lowercase() }
            SortOption.MOST_IMAGES -> searchedItems.sortedByDescending { it.images.size }
            SortOption.FEWEST_IMAGES -> searchedItems.sortedBy { it.images.size }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image Folders") },
                actions = {
                    // Sort Menu Button
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort Items")
                        }
                        // The dropdown menu for sorting
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Name (A-Z)") },
                                onClick = { sortOption = SortOption.NAME_AZ; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (Z-A)") },
                                onClick = { sortOption = SortOption.NAME_ZA; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Most Images First") },
                                onClick = { sortOption = SortOption.MOST_IMAGES; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Fewest Images First") },
                                onClick = { sortOption = SortOption.FEWEST_IMAGES; showSortMenu = false }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text("Search by name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                singleLine = true
            )

            if (filteredAndSortedItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (items.any { it.images.isNotEmpty() }) "No matching items found." else "No items with images found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredAndSortedItems, key = { it.id }) { item ->
                        ImageFolderCard(item = item, onClick = { onNavigateToDetail(item) })
                    }
                }
            }
        }
    }
}

// Your existing "stacked card" composable - no changes needed here.
@Composable
fun ImageFolderCard(item: Item, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        if (item.images.size > 1) {
            Card(
                modifier = Modifier
                    .fillMaxSize(0.9f)
                    .rotate(-5f)
                    .align(Alignment.Center),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray))
            }
        }
        Card(
            modifier = Modifier
                .fillMaxSize(0.9f)
                .align(Alignment.Center),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = rememberAsyncImagePainter(model = item.images.first().toUri()),
                    contentDescription = "Image of ${item.name}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                startY = 200f
                            )
                        )
                )
                Text(
                    text = item.name,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}