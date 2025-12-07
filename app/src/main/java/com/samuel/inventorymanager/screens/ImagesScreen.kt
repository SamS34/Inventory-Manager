package com.samuel.inventorymanager.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import com.google.gson.Gson
import java.io.File

// ========================================================================================
//      DELETED IMAGE TRACKING
// ========================================================================================

data class DeletedImageEntry(
    val imageUrl: String,
    val itemId: String,
    val deletedAt: Long = System.currentTimeMillis()
)

data class DeletedImagesData(
    val deletedImages: List<DeletedImageEntry> = emptyList()
)

data class ImageEntry(
    val item: Item,
    val imageUrl: String,
    val isDeleted: Boolean = false
)

enum class ImageSortOption {
    NAME_ASC,
    NAME_DESC,
    DATE_NEWEST,
    DATE_OLDEST,
    CATEGORY_ASC,
    CATEGORY_DESC
}

private fun saveDeletedImages(context: android.content.Context, deletedImages: List<DeletedImageEntry>) {
    try {
        context.openFileOutput("deleted_images.json", android.content.Context.MODE_PRIVATE).use {
            it.write(Gson().toJson(DeletedImagesData(deletedImages)).toByteArray())
        }
    } catch (_: Exception) {
        // Handle silently
    }
}

private fun loadDeletedImages(context: android.content.Context): List<DeletedImageEntry> {
    val file = File(context.filesDir, "deleted_images.json")
    return if (file.exists()) {
        try {
            val data = Gson().fromJson(file.readText(), DeletedImagesData::class.java)
            // Remove images that were deleted more than 2 days ago
            val twoDaysMs = 2 * 24 * 60 * 60 * 1000L
            val now = System.currentTimeMillis()
            data.deletedImages.filter { now - it.deletedAt < twoDaysMs }
        } catch (_: Exception) {
            emptyList()
        }
    } else {
        emptyList()
    }
}

// ========================================================================================
//      MAIN IMAGES SCREEN
// ========================================================================================

@Composable
fun ImagesScreen(
    items: List<Item>,
    onItemClick: (Item) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var deletedImages by remember { mutableStateOf(loadDeletedImages(context)) }
    var selectedImageEntry by remember { mutableStateOf<ImageEntry?>(null) }
    var showZoomDialog by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(ImageSortOption.NAME_ASC) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Filter items to only include those with images
    val itemsWithImages = remember(items) {
        items.filter { it.images.isNotEmpty() }
    }

    // Gather all images and mark deleted ones
    val allImages = remember(itemsWithImages, deletedImages) {
        itemsWithImages.flatMap { item ->
            item.images.map { imageUrl ->
                val isDeleted = deletedImages.any { it.imageUrl == imageUrl && it.itemId == item.id }
                ImageEntry(item = item, imageUrl = imageUrl, isDeleted = isDeleted)
            }
        }
    }

    // Filter by search query
    val filteredImages = remember(allImages, searchQuery) {
        allImages.filter { entry ->
            searchQuery.isBlank() ||
                    entry.item.name.contains(searchQuery, ignoreCase = true) ||
                    entry.item.modelNumber?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    // Sort images
    val sortedImages = remember(filteredImages, sortOption) {
        when (sortOption) {
            ImageSortOption.NAME_ASC -> filteredImages.sortedBy { it.item.name.lowercase() }
            ImageSortOption.NAME_DESC -> filteredImages.sortedByDescending { it.item.name.lowercase() }
            ImageSortOption.DATE_NEWEST -> {
                // Sort by item index in reverse (newest items first)
                val itemIndices = items.mapIndexed { index, item -> item.id to index }.toMap()
                filteredImages.sortedByDescending { itemIndices[it.item.id] ?: 0 }
            }
            ImageSortOption.DATE_OLDEST -> {
                // Sort by item index (oldest items first)
                val itemIndices = items.mapIndexed { index, item -> item.id to index }.toMap()
                filteredImages.sortedBy { itemIndices[it.item.id] ?: 0 }
            }
            ImageSortOption.CATEGORY_ASC -> filteredImages.sortedBy { it.item.sizeCategory.lowercase() }
            ImageSortOption.CATEGORY_DESC -> filteredImages.sortedByDescending { it.item.sizeCategory.lowercase() }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with Search and Sort
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(0.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        placeholder = { Text("Search items...") },
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

                    // Sort button
                    Box {
                        Button(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Name")
                                        Spacer(Modifier.width(8.dp))
                                        Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                onClick = {
                                    sortOption = ImageSortOption.NAME_ASC
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Name")
                                        Spacer(Modifier.width(8.dp))
                                        Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                onClick = {
                                    sortOption = ImageSortOption.NAME_DESC
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Newest First") },
                                onClick = {
                                    sortOption = ImageSortOption.DATE_NEWEST
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest First") },
                                onClick = {
                                    sortOption = ImageSortOption.DATE_OLDEST
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Size Category")
                                        Spacer(Modifier.width(8.dp))
                                        Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                onClick = {
                                    sortOption = ImageSortOption.CATEGORY_ASC
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Size Category")
                                        Spacer(Modifier.width(8.dp))
                                        Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                onClick = {
                                    sortOption = ImageSortOption.CATEGORY_DESC
                                    showSortMenu = false
                                }
                            )
                        }
                    }

                    // Clean deleted images button
                    Button(
                        onClick = {
                            deletedImages = emptyList()
                            saveDeletedImages(context, emptyList())
                        },
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        enabled = deletedImages.isNotEmpty()
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Results
        if (sortedImages.isEmpty()) {
            EmptyState()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(sortedImages) { imageEntry ->
                    ImageCard(
                        imageEntry = imageEntry,
                        onImageClick = {
                            selectedImageEntry = imageEntry
                            showZoomDialog = true
                        },
                        onEditClick = {
                            onItemClick(imageEntry.item)
                        },
                        onDeleteClick = {
                            // Mark image as deleted
                            val newDeletedEntry = DeletedImageEntry(
                                imageUrl = imageEntry.imageUrl,
                                itemId = imageEntry.item.id,
                                deletedAt = System.currentTimeMillis()
                            )
                            deletedImages = deletedImages + newDeletedEntry
                            saveDeletedImages(context, deletedImages)
                        }
                    )
                }
            }
        }
    }

    // Zoom Dialog
    if (showZoomDialog && selectedImageEntry != null) {
        ZoomImageDialog(
            imageEntry = selectedImageEntry!!,
            onDismiss = { showZoomDialog = false },
            onEditClick = {
                onItemClick(selectedImageEntry!!.item)
                showZoomDialog = false
            }
        )
    }
}

// ========================================================================================
//      HELPER COMPOSABLES
// ========================================================================================

@Composable
private fun ImageCard(
    imageEntry: ImageEntry,
    onImageClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Box(modifier = Modifier.aspectRatio(1f)) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onImageClick),
            elevation = CardDefaults.cardElevation(4.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Image
                Image(
                    painter = rememberAsyncImagePainter(model = imageEntry.imageUrl.toUri()),
                    contentDescription = imageEntry.item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Deleted overlay
                if (imageEntry.isDeleted) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Deleted",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // Gradient overlay for text
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.4f)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                        .align(Alignment.BottomCenter)
                )

                // Item name
                Text(
                    text = imageEntry.item.name,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Action buttons (top right)
        if (!imageEntry.isDeleted) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Zoom button
                IconButton(
                    onClick = onImageClick,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(6.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.ZoomIn,
                        contentDescription = "Zoom",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Edit button
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(6.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            // Show delete button for deleted images (to allow permanent removal if needed)
            @Suppress("UNUSED_EXPRESSION")
            onDeleteClick // Keep parameter to avoid warning
        }
    }
}

@Composable
private fun ZoomImageDialog(
    imageEntry: ImageEntry,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            // Close button (top left)
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Edit button (top right)
            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Item",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Main image
            Image(
                painter = rememberAsyncImagePainter(model = imageEntry.imageUrl.toUri()),
                contentDescription = imageEntry.item.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onDismiss),
                contentScale = ContentScale.Fit
            )

            // Item info at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = imageEntry.item.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                if (imageEntry.item.modelNumber != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Model: ${imageEntry.item.modelNumber}",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (imageEntry.item.description != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = imageEntry.item.description,
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ImageSearch,
                contentDescription = "No Images",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Text(
                text = "No Images Found",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Add items with pictures using the camera or upload button on the 'Create Item' screen.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}