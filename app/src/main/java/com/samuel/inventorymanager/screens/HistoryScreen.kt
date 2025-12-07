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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



sealed class ActionType(
    val key: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
) {
    object Added : ActionType("Added", "Added", Icons.Filled.AddCircle, Color(0xFF10B981))
    object Updated : ActionType("Updated", "Updated", Icons.Filled.Edit, Color(0xFFF59E0B))
    object Removed : ActionType("Removed", "Removed", Icons.Filled.Delete, Color(0xFFEF4444))
    object QuantityChanged : ActionType("QuantityChanged", "Qty Changed", Icons.Filled.SwapVert, Color(0xFF8B5CF6))
    object CheckedOut : ActionType("CheckedOut", "Checked Out", Icons.AutoMirrored.Filled.Logout, Color(0xFFEF4444))
    object CheckedIn : ActionType("CheckedIn", "Checked In", Icons.AutoMirrored.Filled.Login, Color(0xFF10B981))
    data class Unknown(val raw: String) : ActionType(raw, if (raw.isBlank()) "Unknown" else raw, Icons.Filled.History, Color(0xFF6B7280))

    companion object {
        fun fromRaw(raw: String): ActionType = when (raw) {
            Added.key -> Added
            Updated.key -> Updated
            Removed.key -> Removed
            QuantityChanged.key -> QuantityChanged
            CheckedOut.key -> CheckedOut
            CheckedIn.key -> CheckedIn
            else -> Unknown(raw)
        }

        val filterableTypes = listOf(Added, Updated, Removed, QuantityChanged, CheckedOut, CheckedIn)
    }
}

enum class HistorySortField { DATE, NAME }
enum class HistorySortDirection { ASCENDING, DESCENDING }
data class HistorySort(val field: HistorySortField, val direction: HistorySortDirection)

data class HistoryUiState(
    val searchQuery: String = "",
    val selectedActionKey: String? = null,
    val sortField: HistorySortField = HistorySortField.DATE,
    val sortDirection: HistorySortDirection = HistorySortDirection.DESCENDING,
    val pendingDeleteId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    history: List<HistoryEntry>,
    items: List<Item>,
    onItemClick: (Item) -> Unit,
    onClearHistory: () -> Unit,
    onDeleteHistoryEntry: (HistoryEntry) -> Unit
) {
    val localHistory = remember(history) { mutableStateListOf<HistoryEntry>().apply { addAll(history) } }
    var uiState by remember { mutableStateOf(HistoryUiState()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val selectedActionType = uiState.selectedActionKey?.let { ActionType.fromRaw(it) }
    val filteredSorted = remember(localHistory, uiState) {
        localHistory.filter { e ->
            (selectedActionType == null || ActionType.fromRaw(e.actionType).key == selectedActionType.key) &&
                    (uiState.searchQuery.isBlank() || e.itemName.contains(uiState.searchQuery, ignoreCase = true) || e.description.contains(uiState.searchQuery, ignoreCase = true))
        }.sortedWith { a, b ->
            val cmp = when (uiState.sortField) {
                HistorySortField.DATE -> a.timestamp.compareTo(b.timestamp)
                HistorySortField.NAME -> a.itemName.lowercase().compareTo(b.itemName.lowercase())
            }
            if (uiState.sortDirection == HistorySortDirection.ASCENDING) cmp else -cmp
        }
    }

    fun clearAllWithUndo() {
        val backup = localHistory.toList()
        localHistory.clear()
        scope.launch {
            val result = snackbarHostState.showSnackbar("History cleared", actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) localHistory.addAll(backup) else onClearHistory()
        }
    }

    fun deleteWithUndo(entry: HistoryEntry) {
        localHistory.remove(entry)
        onDeleteHistoryEntry(entry)
        scope.launch {
            val result = snackbarHostState.showSnackbar("Entry deleted", actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) localHistory.add(entry)
        }
    }

    Scaffold(topBar = { HistoryTopBar() }, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize()
        ) {
            HistoryHeader(
                searchQuery = uiState.searchQuery,
                selectedActionType = selectedActionType,
                sort = HistorySort(uiState.sortField, uiState.sortDirection),
                historyCount = filteredSorted.size,
                totalCount = localHistory.size,
                onSearchQueryChange = { uiState = uiState.copy(searchQuery = it) },
                onActionSelect = { type -> uiState = uiState.copy(selectedActionKey = if (uiState.selectedActionKey == type.key) null else type.key) },
                onSortFieldChange = { uiState = uiState.copy(sortField = it) },
                onSortDirectionToggle = {
                    val newDir = if (uiState.sortDirection == HistorySortDirection.DESCENDING) HistorySortDirection.ASCENDING else HistorySortDirection.DESCENDING
                    uiState = uiState.copy(sortDirection = newDir)
                },
                onClearHistoryClick = { if (localHistory.isNotEmpty()) clearAllWithUndo() }
            )
            Spacer(Modifier.height(12.dp))
            if (filteredSorted.isEmpty()) EmptyHistoryState(Modifier.fillMaxSize().padding(24.dp)) else LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                items(filteredSorted, key = { it.id }) { entry ->
                    HistoryItemCard(
                        entry = entry,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { items.find { it.id == entry.itemId }?.let(onItemClick) },
                        onDeleteClick = { deleteWithUndo(entry) }
                    )
                }
            }
        }
    }

    uiState.pendingDeleteId?.let { id ->
        localHistory.find { it.id == id }?.let { entry ->
            ConfirmDeleteDialog(
                entry = entry,
                onDismiss = { uiState = uiState.copy(pendingDeleteId = null) },
                onConfirm = {
                    uiState = uiState.copy(pendingDeleteId = null)
                    deleteWithUndo(entry)
                }
            )
        }
    }
}

@Composable
private fun HistoryTopBar() {
    Surface(tonalElevation = 2.dp, shadowElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.History, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Track changes to your items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // Remove or add meaningful menu here if desired.
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryHeader(
    searchQuery: String,
    selectedActionType: ActionType?,
    sort: HistorySort,
    historyCount: Int,
    totalCount: Int,
    onSearchQueryChange: (String) -> Unit,
    onActionSelect: (ActionType) -> Unit,
    onSortFieldChange: (HistorySortField) -> Unit,
    onSortDirectionToggle: () -> Unit,
    onClearHistoryClick: () -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$historyCount item${if (historyCount == 1) "" else "s"} shown" + if (historyCount != totalCount) ", total $totalCount" else "",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                TextButton(
                    onClick = onClearHistoryClick,
                    enabled = totalCount > 0,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                ) {
                    Icon(Icons.Filled.DeleteForever, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Clear All")
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search item or description...") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text("Filter by action", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                ActionType.filterableTypes.forEach { type ->
                    FilterChip(
                        selected = selectedActionType?.key == type.key,
                        onClick = { onActionSelect(type) },
                        label = { Text(type.label) },
                        leadingIcon = { Icon(type.icon, null) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = type.color.copy(alpha = 0.2f),
                            selectedLabelColor = type.color,
                            selectedLeadingIconColor = type.color,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sort by:", style = MaterialTheme.typography.labelMedium)
                var expanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(
                        onClick = { expanded = true },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            when (sort.field) {
                                HistorySortField.DATE -> "Date"
                                HistorySortField.NAME -> "Name"
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort icon", modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Date") }, onClick = { expanded = false; onSortFieldChange(HistorySortField.DATE) })
                        DropdownMenuItem(text = { Text("Name") }, onClick = { expanded = false; onSortFieldChange(HistorySortField.NAME) })
                    }
                }
                IconButton(
                    onClick = onSortDirectionToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (sort.direction == HistorySortDirection.DESCENDING) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                        contentDescription = "Toggle sort direction"
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    entry: HistoryEntry,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    elevation: Dp = 2.dp,
) {
    val details = remember(entry.actionType) { detailsFor(entry) }
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(elevation),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).background(details.color.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(details.icon, details.label, tint = details.color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(entry.itemName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatTimestamp(entry.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(details.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = details.color)
                    Text("•", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.alpha(0.7f))
                    Text("ID: ${entry.id.take(8)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (entry.description.isNotBlank()) {
                    Text(entry.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, "More actions")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("View item") }, onClick = { menuExpanded = false; onClick() })
                    DropdownMenuItem(
                        text = { Text("Delete entry") },
                        onClick = { menuExpanded = false; onDeleteClick() },
                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(
                Modifier.size(88.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.HistoryToggleOff, "No history", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp).alpha(0.9f))
            }
            Text("No history yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Changes you make to items, quantities, and check-in/out will appear here.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    entry: HistoryEntry,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Delete this entry?") },
        text = { Text("Remove the history record for “${entry.itemName}”? The item itself stays in inventory.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun detailsFor(entry: HistoryEntry): ActionDetails {
    val type = ActionType.fromRaw(entry.actionType)
    return ActionDetails(type.icon, type.label, type.color)
}

private data class ActionDetails(val icon: ImageVector, val label: String, val color: Color)

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000L}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000L}h ago"
        diff < 7L * 86_400_000L -> "${diff / 86_400_000L}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
