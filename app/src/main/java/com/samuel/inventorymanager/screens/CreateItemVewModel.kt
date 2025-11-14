package com.samuel.inventorymanager.screens

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.util.UUID

class CreateItemViewModel : ViewModel() {

    // Form fields - all mutable state
    var itemName by mutableStateOf("")
    var modelNumber by mutableStateOf("")
    var description by mutableStateOf("")
    var webLink by mutableStateOf("")
    var condition by mutableStateOf("")
    var functionality by mutableStateOf("")
    var quantity by mutableStateOf("")
    var minPrice by mutableStateOf("")
    var maxPrice by mutableStateOf("")
    var weight by mutableStateOf("")
    var sizeCategory by mutableStateOf("")
    var dimensions by mutableStateOf("")

    // Location fields
    var selectedGarageName by mutableStateOf("")
    var selectedCabinetName by mutableStateOf("")
    var selectedShelfName by mutableStateOf("")
    var selectedBoxName by mutableStateOf<String?>(null)

    // Images
    val imageUris = mutableStateListOf<Uri>()

    // Change tracking
    var hasUnsavedChanges by mutableStateOf(false)
        private set
    private var lastSavedState: Int = 0

    init {
        lastSavedState = getCurrentStateHash()
    }

    fun checkForChanges() {
        hasUnsavedChanges = getCurrentStateHash() != lastSavedState
    }

    private fun getCurrentStateHash(): Int {
        return listOf(
            itemName, modelNumber, description, webLink, condition, functionality,
            quantity, minPrice, maxPrice, weight, sizeCategory, dimensions,
            selectedGarageName, selectedCabinetName, selectedShelfName, selectedBoxName,
            imageUris.size
        ).hashCode()
    }

    fun getItemToSave(garages: List<Garage>): Item {
        val garage = garages.find { it.name == selectedGarageName }
        val cabinet = garage?.cabinets?.find { it.name == selectedCabinetName }
        val shelf = cabinet?.shelves?.find { it.name == selectedShelfName }
        val box = shelf?.boxes?.find { it.name == selectedBoxName }

        return Item(
            id = UUID.randomUUID().toString(),
            name = itemName,
            modelNumber = modelNumber.ifBlank { null },
            description = description.ifBlank { null },
            webLink = webLink.ifBlank { null },
            condition = condition,
            functionality = functionality,
            garageId = garage?.id ?: "",
            cabinetId = cabinet?.id ?: "",
            shelfId = shelf?.id ?: "",
            boxId = box?.id,
            quantity = quantity.toIntOrNull() ?: 1,
            minPrice = minPrice.toDoubleOrNull(),
            maxPrice = maxPrice.toDoubleOrNull(),
            weight = weight.toDoubleOrNull(),
            sizeCategory = sizeCategory,
            dimensions = dimensions.ifBlank { null },
            images = imageUris.map { it.toString() }
        )
    }

    fun markAsSaved() {
        lastSavedState = getCurrentStateHash()
        hasUnsavedChanges = false
    }

    fun clearFormForNewItem(garages: List<Garage>) {
        itemName = ""
        modelNumber = ""
        description = ""
        webLink = ""
        condition = ""
        functionality = ""
        quantity = ""
        minPrice = ""
        maxPrice = ""
        weight = ""
        sizeCategory = ""
        dimensions = ""
        imageUris.clear()

        // Keep location if valid, otherwise reset
        if (garages.none { it.name == selectedGarageName }) {
            selectedGarageName = ""
            selectedCabinetName = ""
            selectedShelfName = ""
            selectedBoxName = null
        }

        hasUnsavedChanges = false
        lastSavedState = getCurrentStateHash()
    }

    fun loadItemForEditing(item: Item, garages: List<Garage>) {
        val garage = garages.find { it.id == item.garageId }
        val cabinet = garage?.cabinets?.find { it.id == item.cabinetId }
        val shelf = cabinet?.shelves?.find { it.id == item.shelfId }
        val box = shelf?.boxes?.find { it.id == item.boxId }

        itemName = item.name
        modelNumber = item.modelNumber ?: ""
        description = item.description ?: ""
        webLink = item.webLink ?: ""
        condition = item.condition
        functionality = item.functionality
        quantity = item.quantity.toString()
        minPrice = item.minPrice?.toString() ?: ""
        maxPrice = item.maxPrice?.toString() ?: ""
        weight = item.weight?.toString() ?: ""
        sizeCategory = item.sizeCategory
        dimensions = item.dimensions ?: ""

        selectedGarageName = garage?.name ?: ""
        selectedCabinetName = cabinet?.name ?: ""
        selectedShelfName = shelf?.name ?: ""
        selectedBoxName = box?.name

        imageUris.clear()
        imageUris.addAll(item.images.map { Uri.parse(it) })

        hasUnsavedChanges = false
        lastSavedState = getCurrentStateHash()
    }
}