package com.samuel.inventorymanager.services

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class AIService(private val context: Context) {

    companion object {
        private const val TAG = "AIService"
    }

    private val labeler by lazy {
        ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    }

    private val ocrService = OCRService(context)

    /**
     * Main analysis function - analyzes image and returns structured data
     */
    suspend fun analyzeItemFromBitmap(bitmap: Bitmap): AIAnalysisResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🤖 Starting AI Analysis")

            // Step 1: Save bitmap temporarily
            val tempUri = saveBitmapToTempUri(bitmap)

            // Step 2: Perform OCR
            val ocrResult = try {
                ocrService.performOCR(tempUri)
            } catch (e: Exception) {
                Log.w(TAG, "OCR failed, continuing without it", e)
                OCRResult("", 0.0, "Failed")
            }

            Log.d(TAG, "📝 OCR Text:\n${ocrResult.text}")

            // Step 3: Image labeling (for category detection)
            val image = InputImage.fromBitmap(bitmap, 0)
            val labels = try {
                labeler.process(image).await()
                    .filter { it.confidence >= 0.6f }
                    .map { it.text }
            } catch (e: Exception) {
                Log.w(TAG, "Image labeling failed", e)
                emptyList()
            }

            Log.d(TAG, "🏷️ Image Labels: $labels")

            // Step 4: Extract structured information
            val extractor = SmartExtractor(ocrResult.lines, labels)

            val brand = extractor.extractBrand()
            val productLine = extractor.extractProductLine(brand)
            val capacity = extractor.extractCapacity()
            val modelNumber = extractor.extractModelNumber(brand, productLine, capacity)
            val category = extractor.extractCategory()
            val condition = extractor.extractCondition()
            val price = extractor.extractPrice()
            val dimensions = extractor.extractDimensions()

            // Step 5: Build intelligent item name
            val itemName = buildItemName(brand, productLine, capacity, category, modelNumber)

            // Step 6: Build description from remaining relevant lines
            val description = buildDescription(ocrResult.lines, itemName, modelNumber, category)

            // Step 7: Calculate confidence
            val confidence = calculateConfidence(itemName, modelNumber, description, brand, capacity)

            Log.d(TAG, """
                ✅ AI Analysis Complete:
                   Name: $itemName
                   Brand: $brand
                   Product Line: $productLine
                   Model: $modelNumber
                   Capacity: $capacity
                   Category: $category
                   Confidence: $confidence
            """.trimIndent())

            cleanupTempFiles()

            AIAnalysisResult(
                itemName = itemName,
                modelNumber = modelNumber,
                description = description,
                condition = condition,
                sizeCategory = null,
                estimatedPrice = price,
                dimensions = dimensions,
                rawText = ocrResult.text.takeIf { it.isNotBlank() },
                confidence = confidence
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ AI Analysis Failed", e)
            throw Exception("AI analysis failed: ${e.message}")
        }
    }

    /**
     * Smart Extractor - Uses pattern matching and heuristics
     */
    private inner class SmartExtractor(
        private val lines: List<String>,
        private val imageLabels: List<String>
    ) {

        private val brandPatterns = mapOf(
            "SanDisk" to listOf("sandisk", "san disk"),
            "Samsung" to listOf("samsung"),
            "Western Digital" to listOf("western digital", "wd "),
            "Kingston" to listOf("kingston"),
            "Seagate" to listOf("seagate"),
            "Crucial" to listOf("crucial"),
            "Corsair" to listOf("corsair"),
            "PNY" to listOf("pny"),
            "Lexar" to listOf("lexar"),
            "Transcend" to listOf("transcend"),
            "ADATA" to listOf("adata"),
            "Intel" to listOf("intel"),
            "Sony" to listOf("sony"),
            "Toshiba" to listOf("toshiba"),
            "HP" to listOf("hp ", "hewlett"),
            "Dell" to listOf("dell"),
            "Apple" to listOf("apple"),
            "Microsoft" to listOf("microsoft"),
            "Logitech" to listOf("logitech")
        )

        private val productLineKeywords = listOf(
            "Ultra", "Extreme", "Pro", "Plus", "Max", "Elite", "Premium",
            "Cruzer", "Glide", "Blade", "Fit", "Edge", "Force", "Switch",
            "Evo", "Gaming", "Performance", "Essential", "Portable"
        )

        fun extractBrand(): String? {
            for (line in lines) {
                val lowerLine = line.lowercase()
                for ((brand, patterns) in brandPatterns) {
                    if (patterns.any { lowerLine.contains(it) }) {
                        Log.d(TAG, "✓ Brand found: $brand in '$line'")
                        return brand
                    }
                }
            }
            for (label in imageLabels) {
                val lowerLabel = label.lowercase()
                for ((brand, patterns) in brandPatterns) {
                    if (patterns.any { lowerLabel.contains(it) }) {
                        Log.d(TAG, "✓ Brand found in label: $brand")
                        return brand
                    }
                }
            }
            return null
        }

        fun extractProductLine(brand: String?): String? {
            for (line in lines) {
                for (keyword in productLineKeywords) {
                    val pattern = Regex("\\b$keyword\\b", RegexOption.IGNORE_CASE)
                    if (pattern.containsMatchIn(line)) {
                        Log.d(TAG, "✓ Product Line: $keyword in '$line'")
                        return keyword
                    }
                }
            }
            return null
        }

        fun extractCapacity(): String? {
            val capacityPattern = Regex("(\\d+(?:\\.\\d+)?\\s*(?:TB|GB|MB|PB))\\b", RegexOption.IGNORE_CASE)
            for (line in lines) {
                capacityPattern.find(line)?.let { match ->
                    val capacity = match.value.uppercase().replace(" ", "")
                    Log.d(TAG, "✓ Capacity: $capacity in '$line'")
                    return capacity
                }
            }
            return null
        }

        fun extractModelNumber(brand: String?, productLine: String?, capacity: String?): String? {
            val patterns = listOf(
                Regex("(?:Model|SKU|Part|P/N|MPN)[:\\s]+([A-Z0-9\\-]{6,25})", RegexOption.IGNORE_CASE),
                Regex("\\b([A-Z]{3,6}\\d{2,4}[A-Z0-9\\-]{0,15})\\b"),
                Regex("\\b([A-Z]{2}\\d{2,4}[A-Z0-9\\-]{4,15})\\b"),
                Regex("\\b(\\d{3,4}[A-Z]?[\\-][A-Z0-9]{3,10})\\b")
            )
            for (line in lines) {
                for (pattern in patterns) {
                    pattern.findAll(line).forEach { match ->
                        val potential = match.groupValues.getOrNull(1)?.trim() ?: match.value.trim()
                        if (potential.length < 6 || potential.length > 25) return@forEach
                        if (capacity != null && potential.contains(capacity, ignoreCase = true)) return@forEach
                        if (productLine != null && potential.equals(productLine, ignoreCase = true)) return@forEach
                        if (!potential.any { it.isLetter() } || !potential.any { it.isDigit() }) return@forEach
                        if (potential.all { it.isDigit() || it == '-' }) return@forEach
                        Log.d(TAG, "✓ Model Number: $potential in '$line'")
                        return potential
                    }
                }
            }
            return null
        }

        fun extractCategory(): String? {
            val categoryKeywords = mapOf(
                "USB Flash Drive" to listOf("usb", "flash drive", "thumb drive", "pendrive", "cruzer", "glide"),
                "SSD" to listOf("ssd", "solid state", "nvme", "m.2"),
                "Hard Drive" to listOf("hard drive", "hdd", "disk drive"),
                "Memory Card" to listOf("sd card", "microsd", "memory card", "sdhc", "sdxc"),
                "External Storage" to listOf("external", "portable storage", "portable drive"),
                "RAM" to listOf("memory", "ddr", "dimm", "sodimm"),
                "Power Supply" to listOf("power supply", "psu", "ac adapter", "charger")
            )
            val allText = (lines + imageLabels).joinToString(" ").lowercase()
            for ((category, keywords) in categoryKeywords) {
                val matchCount = keywords.count { allText.contains(it) }
                if (matchCount >= 1) {
                    Log.d(TAG, "✓ Category: $category")
                    return category
                }
            }
            return null
        }

        fun extractCondition(): String? {
            val conditions = mapOf(
                "New" to listOf("new", "brand new", "sealed", "unopened"),
                "Like New" to listOf("like new", "mint", "excellent"),
                "Good" to listOf("good", "used", "working"),
                "Fair" to listOf("fair", "wear"),
                "Poor" to listOf("poor", "damaged", "broken")
            )
            val allText = lines.joinToString(" ").lowercase()
            for ((condition, keywords) in conditions) {
                if (keywords.any { allText.contains(it) }) {
                    return condition
                }
            }
            return null
        }

        fun extractPrice(): Double? {
            val pricePattern = Regex("\\$\\s*([0-9,]+(?:\\.\\d{2})?)")
            for (line in lines) {
                pricePattern.find(line)?.let { match ->
                    val priceStr = match.groupValues[1].replace(",", "")
                    val price = priceStr.toDoubleOrNull()
                    if (price != null && price in 0.01..99999.99) {
                        return price
                    }
                }
            }
            return null
        }

        fun extractDimensions(): String? {
            val dimPattern = Regex("(\\d+(?:\\.\\d+)?)\\s*[xX×]\\s*(\\d+(?:\\.\\d+)?)(?:\\s*[xX×]\\s*(\\d+(?:\\.\\d+)?))?\\s*(in|inch|cm|mm)?", RegexOption.IGNORE_CASE)
            for (line in lines) {
                dimPattern.find(line)?.let { return it.value.trim() }
            }
            return null
        }
    }

    private fun buildItemName(brand: String?, productLine: String?, capacity: String?, category: String?, modelNumber: String?): String {
        val parts = mutableListOf<String>()
        brand?.let { parts.add(it) }
        productLine?.let { if (!parts.contains(it)) parts.add(it) }
        capacity?.let { parts.add(it) }
        if (parts.size < 2) {
            category?.let {
                if (!parts.any { p -> p.equals(it, ignoreCase = true) }) parts.add(it)
            }
        }
        return if (parts.isNotEmpty()) parts.joinToString(" ") else if (modelNumber != null) modelNumber else "Unknown Item"
    }

    /**
     * **[NEW & IMPROVED]**
     * Build a smarter, more readable description by categorizing information.
     */
    private fun buildDescription(
        lines: List<String>,
        itemName: String,
        modelNumber: String?,
        category: String?
    ): String {
        val usedWords = (itemName.lowercase().split(" ") + (modelNumber?.lowercase() ?: "")).toSet()

        val features = mutableListOf<String>()
        val specs = mutableMapOf<String, String>()

        val speedPattern = Regex("(\\bup to\\b |read speed of |\\b)(\\d{2,5}\\s*mb/s)", RegexOption.IGNORE_CASE)
        val usbPattern = Regex("\\b(usb[- ]?(?:3\\.\\d|2\\.0|type-c|c))\\b", RegexOption.IGNORE_CASE)
        val featureKeywords = listOf(
            "durable", "metal casing", "compact", "lightweight", "design", "waterproof",
            "shockproof", "temperature proof", "x-ray proof", "password protection",
            "encryption", "secureaccess", "compatible with", "ideal for", "high-performance"
        )
        val junkPhrases = listOf(
            "www.", ".com", "photo", "video", "music", "file", "all rights reserved",
            "made in", "serial no", "warranty", "satisfaction guaranteed"
        )

        val remainingLines = lines
            .map { it.replace("•", "").trim() }
            .filter { line ->
                line.length > 4 && line.any { it.isLetter() } &&
                        !junkPhrases.any { junk -> line.lowercase().contains(junk) }
            }
            .toMutableList()

        val iterator = remainingLines.iterator()
        while (iterator.hasNext()) {
            val line = iterator.next()
            val lowerLine = line.lowercase()

            val lineWords = lowerLine.split(" ").toSet()
            if (lineWords.all { it in usedWords }) {
                iterator.remove()
                continue
            }

            speedPattern.find(lowerLine)?.let {
                if (!specs.containsKey("Speed")) {
                    val speed = it.groupValues[2].uppercase()
                    specs["Speed"] = "Up to $speed"
                    iterator.remove()
                    continue
                }
            }

            usbPattern.find(lowerLine)?.let {
                if (!specs.containsKey("Interface")) {
                    val interfaceType = it.groupValues[1].uppercase().replace(" ", "")
                    specs["Interface"] = interfaceType
                    iterator.remove()
                    continue
                }
            }

            if (featureKeywords.any { keyword -> lowerLine.contains(keyword) }) {
                val featureSentence = line.trimEnd('.').capitalizeFirstLetter()
                features.add(featureSentence)
                iterator.remove()
            }
        }

        val descriptionBuilder = StringBuilder()

        if (features.isNotEmpty()) {
            descriptionBuilder.append(features.joinToString(". ") + ".")
        }

        if (specs.isNotEmpty()) {
            if (descriptionBuilder.isNotEmpty()) descriptionBuilder.append("\n\n")
            descriptionBuilder.append("Key Specs:\n")
            specs.forEach { (key, value) ->
                descriptionBuilder.append("• $key: $value\n")
            }
        }

        if (remainingLines.isNotEmpty()) {
            if (descriptionBuilder.isNotEmpty()) descriptionBuilder.append("\n")
            descriptionBuilder.append("Additional Details:\n")
            remainingLines.take(5).forEach {
                descriptionBuilder.append("• ${it.capitalizeFirstLetter()}\n")
            }
        }

        val finalDescription = descriptionBuilder.toString().trim()
        return finalDescription.ifBlank { "No additional details captured from the image." }
    }


    private fun calculateConfidence(itemName: String?, modelNumber: String?, description: String?, brand: String?, capacity: String?): Double {
        var score = 0.0
        if (brand != null) score += 0.30
        if (capacity != null) score += 0.25
        if (modelNumber != null) score += 0.20
        if (itemName != null && itemName != "Unknown Item") score += 0.15
        if (description != null && !description.contains("No additional")) score += 0.10
        return score.coerceIn(0.0, 1.0)
    }

    private fun cleanupTempFiles() {
        try {
            context.cacheDir.listFiles { it.name.startsWith("temp_") && it.name.endsWith(".jpg") }
                ?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

    private fun saveBitmapToTempUri(bitmap: Bitmap): Uri {
        val file = File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg")
        file.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
        return Uri.fromFile(file)
    }

    data class AIAnalysisResult(
        val itemName: String?,
        val modelNumber: String?,
        val description: String?,
        val condition: String?,
        val sizeCategory: String?,
        val estimatedPrice: Double?,
        val dimensions: String?,
        val rawText: String?,
        val confidence: Double
    )
}

/**
 * Helper extension function to capitalize the first letter of a string.
 */
private fun String.capitalizeFirstLetter(): String {
    return this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}