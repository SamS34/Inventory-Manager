package com.samuel.inventorymanager.screens

// --- Core Data Structures ---
data class Item(
    val id: String,
    val name: String,
    val modelNumber: String?,
    val description: String?,
    val webLink: String?,
    val condition: String,
    val functionality: String,
    val garageId: String,
    val cabinetId: String,
    val shelfId: String,
    val boxId: String?,
    val quantity: Int,
    val minPrice: Double?,
    val maxPrice: Double?,
    val weight: Double?,
    val sizeCategory: String,
    val dimensions: String?,
    val images: List<String>
)

data class Box(val id: String, val name: String)
data class Shelf(val id: String, val name: String, val boxes: List<Box>)
data class Cabinet(val id: String, val name: String, val shelves: List<Shelf>)
data class Garage(val id: String, val name: String, val cabinets: List<Cabinet>)

// --- History Tracking ---
sealed class HistoryAction {
    object Added : HistoryAction()
    object Updated : HistoryAction()
    object Removed : HistoryAction()
    data class QuantityChanged(val oldQuantity: Int, val newQuantity: Int) : HistoryAction()
    data class CheckedOut(val userId: String) : HistoryAction()
    data class CheckedIn(val userId: String) : HistoryAction()
}

data class HistoryEntry(
    val id: String,
    val itemId: String,
    val itemName: String,
    val action: HistoryAction,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

// --- App Data Bundle for Saving/Loading ---
data class AppData(
    val garages: List<Garage>,
    val items: List<Item>,
    val history: List<HistoryEntry>
)