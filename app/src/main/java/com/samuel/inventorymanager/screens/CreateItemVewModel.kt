package com.samuel.inventorymanager.screens

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samuel.inventorymanager.services.OCRService
import kotlinx.coroutines.launch
import java.util.UUID

// --- IMPORTANT: Import your Data Models here ---
// If your Item/Garage classes are in 'com.samuel.inventorymanager.data', keep these.
// If they are in 'models', change 'data' to 'models'.
//import com.samuel.inventorymanager.data.Garage
//import com.samuel.inventorymanager.data.Item

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

    // =================================================================================
    // 1. SMART AI/OCR FUNCTIONALITY
    // =================================================================================

    /**
     * UI should call this when an image is captured.
     */
    @Suppress("unused") // Called from UI
    fun analyzeImage(imageUri: Uri) {
        isProcessing = true

        viewModelScope.launch {
            try {
                // 1. Run OCR
                val result = ocrService.performOCR(imageUri)

                // 2. Smart Parse
                val analyzedData = smartParseOCRText(result.text)

                // 3. Update Preview
                aiAnalysisResult = analyzedData

                // Add to images if new
                if (!imageUris.contains(imageUri)) {
                    imageUris.add(imageUri)
                }

                showAIPreview = true
            } catch (e: Exception) {
                Log.e("CreateItemVM", "AI Analysis failed: ${e.message}")
            } finally {
                isProcessing = false
            }
        }
    }


    /**
     * Helper to parse raw text into structured data.
     */
    private fun smartParseOCRText(rawText: String): AIAnalysisResult {
        val lines = rawText.lines()

        // Guess Model Number (uppercase + numbers mixed, >3 chars)
        val modelRegex = Regex("\\b(?=.*[A-Z])(?=.*\\d)[A-Z\\d-]{4,}\\b")
        val possibleModel = lines.firstNotNullOfOrNull { line ->
            modelRegex.find(line)?.value
        }

        // Guess Dimensions (Num x Num or NumxNum)
        val dimRegex = Regex("\\d+(\\.\\d+)?\\s*[xX]\\s*\\d+(\\.\\d+)?(\\s*[xX]\\s*\\d+(\\.\\d+)?)?")
        val possibleDimensions = lines.firstNotNullOfOrNull { line ->
            dimRegex.find(line)?.value
        }

        // Guess Price
        val priceRegex = Regex("\\$\\s*([0-9,]+(\\.\\d{2})?)")
        val priceString = lines.firstNotNullOfOrNull { line ->
            priceRegex.find(line)?.groupValues?.get(1)
        }?.replace(",", "")
        val priceVal = priceString?.toDoubleOrNull()

        // Name (simple guess: first reasonably long line without a '$')
        val possibleName = lines.firstOrNull { it.length > 4 && !it.contains("$") }

        return AIAnalysisResult(
            itemName = possibleName ?: "",
            confidence = 0.5, // Add confidence estimate
            modelNumber = possibleModel,
            description = rawText.take(200),
            estimatedPrice = priceVal,
            dimensions = possibleDimensions,
            rawText = rawText,
            condition = null,
            sizeCategory = null
        )
    }

    /**
     * Called when user confirms AI preview.
     */
    @Suppress("unused") // Called from UI
    fun applyAIResultToForm(result: AIAnalysisResult) {
        if (itemName.isBlank()) result.itemName?.let { itemName = it }
        if (modelNumber.isBlank()) result.modelNumber?.let { modelNumber = it }

        // Combine description logic
        if (!result.rawText.isNullOrBlank()) {
            description = if (description.isBlank()) {
                result.rawText
            } else {
                "$description\n\n--- Scanned Data ---\n${result.rawText}"
            }
        }

        if (dimensions.isBlank()) result.dimensions?.let { dimensions = it }

        result.estimatedPrice?.let { p ->
            if (minPrice.isBlank()) minPrice = p.toString()
            if (maxPrice.isBlank()) maxPrice = (p * 1.2).toString()
        }

        checkForChanges()
        showAIPreview = false
    }


    // =================================================================================
    // 2. CRUD LOGIC
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

        // Reset Location state logic (Optional: keep location if rapid adding?)
        selectedGarageName = ""
        selectedCabinetName = ""
        selectedShelfName = ""
        selectedBoxName = null

        aiAnalysisResult = null
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
        // Use .toUri() from core-ktx or standard Uri.parse
        imageUris.addAll(item.images.map { Uri.parse(it) })

        markAsSaved()
    }

}