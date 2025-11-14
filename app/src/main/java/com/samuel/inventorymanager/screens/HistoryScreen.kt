package com.samuel.inventorymanager.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    history: List<HistoryEntry>,
    items: List<Item>,
    onItemClick: (Item) -> Unit,
    onClearHistory: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedActionType by remember { mutableStateOf<ActionType?>(null) }

    // Filter history based on search and selected action type
    val filteredHistory = remember(searchQuery, selectedActionType, history) {
        history.filter { entry ->
            val matchesAction = selectedActionType == null ||
                    when (entry.action) {
                        is HistoryAction.Added -> selectedActionType == ActionType.ADDED
                        is HistoryAction.Updated -> selectedActionType == ActionType.UPDATED
                        is HistoryAction.Removed -> selectedActionType == ActionType.REMOVED
                        is HistoryAction.QuantityChanged -> selectedActionType == ActionType.QUANTITY_CHANGED
                        is HistoryAction.CheckedOut -> selectedActionType == ActionType.CHECKED_OUT
                        is HistoryAction.CheckedIn -> selectedActionType == ActionType.CHECKED_IN
                    }

            val matchesSearch = searchQuery.isBlank() ||
                    entry.itemName.contains(searchQuery, ignoreCase = true) ||
                    entry.description.contains(searchQuery, ignoreCase = true)

            matchesAction && matchesSearch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HistoryHeader(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            selectedActionType = selectedActionType,
            onActionSelect = { actionType ->
                selectedActionType = if (selectedActionType == actionType) null else actionType
            },
            onClearHistory = onClearHistory
        )

        if (filteredHistory.isEmpty()) {
            EmptyHistoryState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredHistory, key = { it.id }) { entry ->
                    HistoryItemCard(
                        entry = entry,
                        onClick = {
                            items.find { it.id == entry.itemId }?.let { item ->
                                onItemClick(item)
                            }
                        }
                    )
                }
            }
        }
    }
}

// Helper enum to simplify filtering
enum class ActionType {
    ADDED, UPDATED, REMOVED, QUANTITY_CHANGED, CHECKED_OUT, CHECKED_IN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedActionType: ActionType?,
    onActionSelect: (ActionType) -> Unit,
    onClearHistory: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by item name or change...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton({ onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    FilterChip(
                        selected = selectedActionType == ActionType.ADDED,
                        onClick = { onActionSelect(ActionType.ADDED) },
                        label = { Text("Added") },
                        leadingIcon = {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        }
                    )
                    FilterChip(
                        selected = selectedActionType == ActionType.UPDATED,
                        onClick = { onActionSelect(ActionType.UPDATED) },
                        label = { Text("Updated") },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                        }
                    )
                    FilterChip(
                        selected = selectedActionType == ActionType.REMOVED,
                        onClick = { onActionSelect(ActionType.REMOVED) },
                        label = { Text("Removed") },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
                IconButton(onClick = onClearHistory) {
                    Icon(
                        Icons.Default.DeleteForever,
                        "Clear History",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(entry: HistoryEntry, onClick: () -> Unit) {
    val actionDetails = when (entry.action) {
        is HistoryAction.Added -> ActionDetails(
            Icons.Default.AddCircle,
            "Added",
            MaterialTheme.colorScheme.primary
        )
        is HistoryAction.Updated -> ActionDetails(
            Icons.Default.Edit,
            "Updated",
            Color(0xFFF59E0B)
        )
        is HistoryAction.Removed -> ActionDetails(
            Icons.Default.Delete,
            "Removed",
            MaterialTheme.colorScheme.error
        )
        is HistoryAction.QuantityChanged -> {
            val qtyChange = entry.action as HistoryAction.QuantityChanged
            ActionDetails(
                Icons.Default.SwapVert,
                "Qty: ${qtyChange.oldQuantity} → ${qtyChange.newQuantity}",
                Color(0xFF8B5CF6)
            )
        }
        is HistoryAction.CheckedOut -> {
            val checkout = entry.action as HistoryAction.CheckedOut
            ActionDetails(
                Icons.Default.Logout,
                "Checked Out (${checkout.userId})",
                Color(0xFFEF4444)
            )
        }
        is HistoryAction.CheckedIn -> {
            val checkin = entry.action as HistoryAction.CheckedIn
            ActionDetails(
                Icons.Default.Login,
                "Checked In (${checkin.userId})",
                Color(0xFF10B981)
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = actionDetails.icon,
                contentDescription = actionDetails.label,
                modifier = Modifier.size(32.dp),
                tint = actionDetails.color
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = actionDetails.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = actionDetails.color,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = formatTimestamp(entry.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

private data class ActionDetails(
    val icon: ImageVector,
    val label: String,
    val color: Color
)

@Composable
private fun EmptyHistoryState() {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.HistoryToggleOff,
                "No History",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Text(
                "No History Yet",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Changes you make to items and locations will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            val format = SimpleDateFormat("MMM d", Locale.getDefault())
            format.format(Date(timestamp))
        }
    }
}