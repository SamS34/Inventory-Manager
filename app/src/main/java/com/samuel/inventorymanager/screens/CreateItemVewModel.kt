package com.samuel.inventorymanager.screens

// Make sure you have your data model imports here
import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.samuel.inventorymanager.services.OCRService
import java.util.UUID

// --- Helper Data Class for AI Results ---
data class AIAnalysisResult(
    val itemName: String? = null,
    val confidence: Double = 0.0,
    val modelNumber: String? = null,
    val description: String? = null,
    val estimatedPrice: Double? = null,
    val condition: String? = null,
    val sizeCategory: String? = null,
    val dimensions: String? = null,
    val rawText: String? = null
)

class CreateItemViewModel(
    application: Application,
    private val ocrService: OCRService
) : AndroidViewModel(application) {

    // =========================================================
    // *** 1. ADD THIS PROPERTY ***
    // This tells the UI if we are editing an existing item.
    var isEditing by mutableStateOf(false)
        private set
    // =========================================================

    // --- Core Item Data ---
    var currentItem: Item? by mutableStateOf(null)

    // --- UI Form State ---
    var itemName by mutableStateOf("")
    var modelNumber by mutableStateOf("")
    var description by mutableStateOf("")
    var webLink by mutableStateOf("")
    var condition by mutableStateOf("Good")
    var functionality by mutableStateOf("Working")
    var quantity by mutableStateOf("1")
    var minPrice by mutableStateOf("")
    var maxPrice by mutableStateOf("")
    var weight by mutableStateOf("")
    var sizeCategory by mutableStateOf("Medium")
    var dimensions by mutableStateOf("")

    // --- Location State ---
    var selectedGarageName by mutableStateOf("")
    var selectedCabinetName by mutableStateOf("")
    var selectedShelfName by mutableStateOf("")
    var selectedBoxName by mutableStateOf<String?>(null)

    // --- Image & AI State ---
    val imageUris = mutableStateListOf<Uri>()
    var isProcessing by mutableStateOf(false)
    var aiAnalysisResult by mutableStateOf<AIAnalysisResult?>(null)
    var showAIPreview by mutableStateOf(false)

    // --- Change Tracking ---
    var hasUnsavedChanges by mutableStateOf(false)
        private set
    private var lastSavedHash: Int = 0

    init {
        markAsSaved()
    }

    // Your SMART AI/OCR FUNCTIONALITY section is unchanged...
    // (analyzeImage, smartParseOCRText, applyAIResultToForm)
    // They are perfect as they are.

    // ...

    // =================================================================================
    // 2. CRUD LOGIC (WITH THE FIXES)
    // =================================================================================

    private fun getCurrentStateHash(): Int {
        return listOf(
            itemName, modelNumber, description, webLink, condition, functionality,
            quantity, minPrice, maxPrice, weight, sizeCategory, dimensions,
            selectedGarageName, selectedCabinetName, selectedShelfName, selectedBoxName,
            imageUris.toList().map { it.toString() }
        ).hashCode()
    }

    fun checkForChanges() {
        hasUnsavedChanges = getCurrentStateHash() != lastSavedHash
    }

    fun markAsSaved() {
        lastSavedHash = getCurrentStateHash()
        hasUnsavedChanges = false
    }

    fun getItemToSave(garages: List<Garage>): Item {
        val garage = garages.find { it.name == selectedGarageName }
        val cabinet = garage?.cabinets?.find { it.name == selectedCabinetName }
        val shelf = cabinet?.shelves?.find { it.name == selectedShelfName }
        val box = shelf?.boxes?.find { it.name == selectedBoxName }

        return Item(
            id = currentItem?.id ?: UUID.randomUUID().toString(),
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

    fun clearFormForNewItem() {
        currentItem = null
        itemName = ""
        modelNumber = ""
        description = ""
        webLink = ""
        condition = "Good"
        functionality = "Working"
        quantity = "1"
        minPrice = ""
        maxPrice = ""
        weight = ""
        sizeCategory = "Medium"
        dimensions = ""
        imageUris.clear()

        selectedGarageName = ""
        selectedCabinetName = ""
        selectedShelfName = ""
        selectedBoxName = null

        aiAnalysisResult = null

        // =========================================================
        // *** 3. SET isEditing back to false for a new item ***
        isEditing = false
        // =========================================================

        markAsSaved()
    }

    fun loadItemForEditing(item: Item, garages: List<Garage>) {
        currentItem = item

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

        // =========================================================
        // *** 2. SET isEditing to true because we are editing an item ***
        isEditing = true
        // =========================================================

        markAsSaved()
    }

}