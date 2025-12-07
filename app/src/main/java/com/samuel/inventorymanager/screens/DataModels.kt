package com.samuel.inventorymanager.screens

import java.util.UUID

// --- Core Data Structures ---
data class Item(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val modelNumber: String? = null,
    val description: String? = null,
    val webLink: String? = null,
    val condition: String = "Good",
    val functionality: String = "Working",
    val garageId: String,
    val cabinetId: String,
    val shelfId: String,
    val boxId: String? = null,
    val quantity: Int = 1,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val weight: Double? = null,
    val sizeCategory: String = "Medium",
    val dimensions: String? = null,
    val images: List<String> = emptyList()
)

data class Box(
    val id: String = UUID.randomUUID().toString(),
    val name: String
)

data class Shelf(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val boxes: MutableList<Box> = mutableListOf()
)

data class Cabinet(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val shelves: MutableList<Shelf> = mutableListOf()
)

data class Garage(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val cabinets: MutableList<Cabinet> = mutableListOf()
)

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
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val itemName: String,
    val actionType: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

// --- App Data Bundle for Saving/Loading ---
data class AppData(
    val garages: List<Garage> = emptyList(),
    val items: List<Item> = emptyList(),
    val history: List<HistoryEntry> = emptyList()
)