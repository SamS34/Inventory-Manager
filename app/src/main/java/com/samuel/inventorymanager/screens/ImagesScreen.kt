package com.samuel.inventorymanager.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

// We create a simple data class to make the logic cleaner.
// It bundles an image with the item it belongs to.
data class ImageEntry(
    val item: Item,
    val imageUrl: String
)

@Composable
fun ImagesScreen(
    items: List<Item>,
    onItemClick: (Item) -> Unit // This function will trigger the navigation
) {
    // We first gather all images from all items into a single, flat list.
    val allImages = items.flatMap { item ->
        item.images.map { imageUrl ->
            ImageEntry(item = item, imageUrl = imageUrl)
        }
    }

    if (allImages.isEmpty()) {
        // Show a helpful message if no images have been added yet.
        EmptyState()
    } else {
        // Display the images in a responsive, vertical grid.
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp), // This makes the grid look good on any screen size!
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(allImages) { imageEntry ->
                ImageCard(
                    imageEntry = imageEntry,
                    onClick = {
                        // When an image is clicked, we call the navigation function
                        // with the item that the image belongs to.
                        onItemClick(imageEntry.item)
                    }
                )
            }
        }
    }
}


// ======================================================================
//                              HELPER COMPOSABLES
// These are the building blocks that make the screen look polished. ✨
// ======================================================================

@Composable
private fun ImageCard(
    imageEntry: ImageEntry,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f) // This makes the card a perfect square
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // The Image itself, which fills the entire card
            Image(
                painter = rememberAsyncImagePainter(model = Uri.parse(imageEntry.imageUrl)),
                contentDescription = imageEntry.item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop // This ensures the image fills the square without stretching
            )

            // A semi-transparent overlay at the bottom to make the text readable
            Box(
                modifier = Modifier.run {
                    fillMaxWidth()
                                .fillMaxHeight(0.4f)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                    )
                                )
                                .align(Alignment.BottomCenter)
                }
            )

            // The name of the item the image belongs to
            Text(
                text = imageEntry.item.name,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
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