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
import kotlin.math.abs

class AIService(private val context: Context) {

    companion object {
        private const val TAG = "AIService"
        private const val LABEL_CONFIDENCE_THRESHOLD = 0.65f

        // Non-English words to aggressively filter
        private val NON_ENGLISH_INDICATORS = setOf(
            "capacidad", "almacenamiento", "memoria", "unidad", "disco", "tarjeta",
            "portátil", "externo", "rápido", "alta", "velocidad", "garantía",
            "capacité", "stockage", "mémoire", "unité", "disque", "carte",
            "portable", "externe", "rapide", "haute", "vitesse", "garantie",
            "clé", "lecteur", "disponible", "producto", "produit"
        )

        private val ENGLISH_TECH_WORDS = setOf(
            "storage", "memory", "drive", "flash", "card", "usb", "external",
            "portable", "capacity", "speed", "ultra", "pro", "extreme", "plus",
            "stick", "disk", "solid", "state", "high", "fast", "warranty",
            "cruzer", "glide", "blade", "ultra", "fit", "edge", "force"
        )

        // Noise words that shouldn't be in product names
        private val NOISE_WORDS = setOf(
            "the", "with", "for", "and", "from", "this", "that", "these",
            "capacity", "storage", "available", "made", "china", "usa",
            "high", "speed", "fast", "quality", "compatible", "warranty"
        )
    }

    private val labeler by lazy {
        ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    }

    private val ocrService = OCRService(context)
    private val brandDatabase = BrandDatabase()
    private val productTypeClassifier = ProductTypeClassifier()
    private val contextualExtractor = ContextualExtractor()
    private val languageDetector = LanguageDetector()
    private val logoDetector = LogoDetector()
    private val nameBuilder = IntelligentNameBuilder()

    suspend fun analyzeItemFromBitmap(bitmap: Bitmap): AIAnalysisResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== Starting Advanced AI Analysis ===")

            val tempUri = saveBitmapToTempUri(bitmap)

            // Step 1: OCR
            val ocrResult = try {
                ocrService.performOCR(tempUri)
            } catch (e: Exception) {
                Log.w(TAG, "OCR failed", e)
                OCRResult("", 0.0, "ML Kit")
            }

            Log.d(TAG, "Raw OCR Text:\n${ocrResult.text}")

            // Step 2: Image Labeling
            val image = InputImage.fromBitmap(bitmap, 0)
            val rawLabels = try {
                labeler.process(image).await()
            } catch (e: Exception) {
                Log.w(TAG, "Image labeling failed", e)
                emptyList()
            }

            val labels = rawLabels
                .filter { it.confidence >= LABEL_CONFIDENCE_THRESHOLD }
                .map { LabelInfo(it.text, it.confidence) }

            Log.d(TAG, "Image Labels: ${labels.joinToString { "${it.label}(${it.confidence})" }}")

            // Step 3: Logo Detection
            val detectedLogos = logoDetector.detectLogos(ocrResult.text, labels)
            Log.d(TAG, "Detected Logos: $detectedLogos")

            // Step 4: Filter English text
            val filteredText = filterEnglishText(ocrResult.text)
            Log.d(TAG, "Filtered English Text:\n$filteredText")

            // Step 5: Preprocess
            val analysisContext = AnalysisContext(
                rawText = filteredText,
                imageLabels = labels,
                lines = preprocessText(filteredText),
                detectedLogos = detectedLogos
            )

            Log.d(TAG, "Preprocessed ${analysisContext.lines.size} semantic units")

            // Step 6: Extract product information
            val productInfo = extractProductInformation(analysisContext)
            Log.d(TAG, "=== Extracted Product Info ===")
            Log.d(TAG, "Brand: ${productInfo.brand}")
            Log.d(TAG, "Product Line: ${productInfo.productLine}")
            Log.d(TAG, "Model: ${productInfo.modelNumber}")
            Log.d(TAG, "Capacity: ${productInfo.capacity}")
            Log.d(TAG, "Category: ${productInfo.category}")

            // Step 7: Build final result
            val result = buildStructuredResult(productInfo, analysisContext)
            Log.d(TAG, "=== Final Result ===")
            Log.d(TAG, "Name: ${result.itemName}")
            Log.d(TAG, "Confidence: ${result.confidence}")

            cleanupTempFiles()
            result

        } catch (e: Exception) {
            Log.e(TAG, "AI analysis failed", e)
            throw Exception("AI analysis failed: ${e.message}")
        }
    }

    private fun buildStructuredResult(
        productInfo: ProductInfo,
        context: AnalysisContext
    ): AIAnalysisResult {
        return AIAnalysisResult(
            itemName = productInfo.name,
            modelNumber = productInfo.modelNumber,
            description = productInfo.description,
            condition = productInfo.condition,
            sizeCategory = null,
            estimatedPrice = productInfo.price,
            dimensions = productInfo.dimensions,
            rawText = context.rawText.ifBlank { null },
            confidence = calculateOverallConfidence(productInfo, context)
        )
    }

    private fun filterEnglishText(rawText: String): String {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val scoredLines = mutableListOf<Pair<String, Double>>()

        for (line in lines) {
            val score = languageDetector.scoreEnglishLikelihood(line)
            if (score >= 0.3) {
                scoredLines.add(line to score)
                Log.d(TAG, "✓ Line: '$line' | Score: $score")
            } else {
                Log.d(TAG, "✗ Filtered: '$line' | Score: $score")
            }
        }

        val englishLines = scoredLines
            .sortedByDescending { it.second }
            .map { it.first }

        return if (englishLines.isNotEmpty()) {
            englishLines.joinToString("\n")
        } else {
            Log.w(TAG, "No English text detected, using original")
            rawText
        }
    }

    private fun preprocessText(rawText: String): List<SemanticUnit> {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val semanticUnits = mutableListOf<SemanticUnit>()
        var position = 0

        for (line in lines) {
            val cleaned = cleanLine(line)
            if (cleaned.isEmpty()) continue

            val quality = assessLineQuality(cleaned)
            if (quality.isUseful) {
                semanticUnits.add(
                    SemanticUnit(
                        text = cleaned,
                        originalText = line,
                        position = position++,
                        quality = quality,
                        tokens = tokenize(cleaned),
                        type = classifyLineType(cleaned)
                    )
                )
            }
        }

        return groupRelatedUnits(semanticUnits)
    }

    private fun cleanLine(line: String): String {
        var cleaned = line

        // Filter lines with too many accents
        val accentedCount = cleaned.count { it in "áéíóúñüàèìòùâêîôûëïöçÁÉÍÓÚÑÜÀÈÌÒÙÂÊÎÔÛËÏÖÇ" }
        if (accentedCount > 2) {
            return ""
        }

        cleaned = cleaned.replace(Regex("\\s+"), " ").trim()

        // Noise patterns
        val noisePatterns = listOf(
            Regex("^(MSIP|REM|FCC|CE|UL|ETL|ROHS|WEEE|CE\\d+|RECYCLABLE).*", RegexOption.IGNORE_CASE),
            Regex(".*\\b(COMPLIANCE|CERTIFIED|APPROVED|REGULATION)\\s+\\d+.*", RegexOption.IGNORE_CASE),
            Regex("^\\d{13,}$"),
            Regex("^[A-Z]{10,}$"),
            Regex("^[0-9A-F]{16,}$"),
            Regex("^www\\..*", RegexOption.IGNORE_CASE),
            Regex(".*@.*\\.com", RegexOption.IGNORE_CASE)
        )

        for (pattern in noisePatterns) {
            if (pattern.matches(cleaned)) return ""
        }

        // Filter obvious non-English
        val lowerCleaned = cleaned.lowercase()
        val hasNonEnglish = NON_ENGLISH_INDICATORS.any { lowerCleaned.contains(it) }
        val hasEnglish = ENGLISH_TECH_WORDS.any { lowerCleaned.contains(it) }

        if (hasNonEnglish && !hasEnglish) {
            return ""
        }

        return cleaned
    }

    private fun assessLineQuality(line: String): LineQuality {
        var score = 50.0

        // Length scoring
        score += when (line.length) {
            in 3..5 -> -20.0
            in 6..10 -> 5.0
            in 11..40 -> 25.0
            in 41..80 -> 15.0
            else -> -25.0
        }

        // Character composition
        val letters = line.count { it.isLetter() }
        val numbers = line.count { it.isDigit() }
        val total = letters + numbers

        if (total > 0) {
            val letterRatio = letters.toDouble() / total
            score += when {
                letterRatio in 0.4..0.8 -> 25.0
                letterRatio in 0.2..0.9 -> 10.0
                else -> -15.0
            }
        }

        // Word count
        val words = line.split(Regex("\\s+")).filter { it.length > 1 }
        score += when (words.size) {
            0, 1 -> -10.0
            2, 3 -> 15.0
            4, 5, 6 -> 25.0
            else -> 10.0
        }

        // Meaningful words
        if (words.any { it.length >= 4 }) score += 20.0

        // Special characters
        val specialChars = line.count { !it.isLetterOrDigit() && !it.isWhitespace() }
        val specialRatio = if (line.isNotEmpty()) specialChars.toDouble() / line.length else 0.0
        if (specialRatio > 0.4) score -= 40.0

        // Product keywords
        val productKeywords = listOf(
            "GB", "TB", "MB", "USB", "Drive", "Flash", "Storage", "Memory",
            "External", "Portable", "SSD", "HDD", "Card", "Stick", "Pro",
            "Ultra", "Elite", "Max", "Plus", "Extreme", "Cruzer", "Glide",
            "Blade", "Edge", "Fit", "Force", "Evo", "Premium"
        )

        val keywordMatches = productKeywords.count { line.contains(it, ignoreCase = true) }
        score += keywordMatches * 20.0

        // English scoring
        val englishScore = languageDetector.scoreEnglishLikelihood(line)
        score += (englishScore * 35.0)

        // Penalty for accents
        val accentedChars = line.count { it in "áéíóúñüàèìòùâêîôûëïöç" }
        score -= (accentedChars * 20.0)

        return LineQuality(score, score > 35.0, (score / 100.0).coerceIn(0.0, 1.0))
    }

    private fun tokenize(text: String): List<Token> {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        return words.mapIndexed { index, word ->
            Token(word, index, classifyToken(word))
        }
    }

    private fun classifyToken(word: String) = when {
        word.matches(Regex("\\d+(?:\\.\\d+)?\\s*(?:TB|GB|MB|PB)", RegexOption.IGNORE_CASE)) -> TokenType.CAPACITY
        word.matches(Regex("\\d+")) -> TokenType.NUMBER
        word.matches(Regex("[A-Z][a-z]+")) -> TokenType.PROPER_NOUN
        word.matches(Regex("[A-Z]{2,}")) -> TokenType.ACRONYM
        word.matches(Regex("[A-Z0-9\\-]+")) -> TokenType.MODEL_CODE
        word.lowercase() in listOf("usb", "ssd", "hdd", "drive", "flash", "storage", "card") -> TokenType.PRODUCT_TYPE
        else -> TokenType.WORD
    }

    private fun classifyLineType(line: String): LineType = when {
        line.contains(Regex("\\d+\\s*(?:GB|TB|MB)", RegexOption.IGNORE_CASE)) -> LineType.CAPACITY_INFO
        line.matches(Regex(".*(?:Model|SKU|Part|P/N|MPN).*", RegexOption.IGNORE_CASE)) -> LineType.MODEL_INFO
        line.split("\\s+".toRegex()).size <= 4 && line.any { it.isUpperCase() } -> LineType.BRAND_OR_SERIES
        line.contains("$") || line.contains(Regex("\\d+\\.\\d{2}")) -> LineType.PRICE_INFO
        line.contains(Regex("\\d+\\s*x\\s*\\d+", RegexOption.IGNORE_CASE)) -> LineType.DIMENSIONS
        else -> LineType.DESCRIPTIVE
    }

    private fun groupRelatedUnits(units: List<SemanticUnit>): List<SemanticUnit> {
        val grouped = mutableListOf<SemanticUnit>()
        var i = 0

        while (i < units.size) {
            val current = units[i]
            if (i + 1 < units.size) {
                val next = units[i + 1]
                if (shouldMerge(current, next)) {
                    grouped.add(
                        current.copy(
                            text = "${current.text} ${next.text}",
                            tokens = current.tokens + next.tokens
                        )
                    )
                    i += 2
                    continue
                }
            }
            grouped.add(current)
            i++
        }
        return grouped
    }

    private fun shouldMerge(a: SemanticUnit, b: SemanticUnit): Boolean {
        if (abs(a.position - b.position) > 1) return false
        if (a.type == LineType.BRAND_OR_SERIES && b.type == LineType.MODEL_INFO) return true
        if (a.type == LineType.BRAND_OR_SERIES && b.type == LineType.CAPACITY_INFO) return true
        if (a.text.length < 20 && b.text.length < 20 &&
            a.type == LineType.DESCRIPTIVE && b.type == LineType.DESCRIPTIVE
        ) return true
        return false
    }

    private fun extractProductInformation(context: AnalysisContext): ProductInfo {
        val info = ProductInfo()

        val goodLines = context.lines.filter { it.quality.score > 45 }

        if (context.rawText.length < 10 || goodLines.isEmpty()) {
            info.name = "Unknown Item"
            info.description = "Not enough readable text"
            return info
        }

        // Extract in priority order
        info.category = productTypeClassifier.classify(context)
        info.brand = brandDatabase.findBrand(context, info.category, context.detectedLogos)
        info.capacity = contextualExtractor.extractCapacity(context)
        info.productLine = contextualExtractor.extractProductLine(context, info.brand)
        info.modelNumber = contextualExtractor.extractModelNumber(context, info.brand, info.capacity, info.productLine)
        info.name = nameBuilder.buildName(info, context)
        info.description = generateSmartDescription(info, context)
        info.condition = contextualExtractor.extractCondition(context)
        info.price = contextualExtractor.extractPrice(context)
        info.dimensions = contextualExtractor.extractDimensions(context)

        return info
    }

    private fun generateSmartDescription(info: ProductInfo, context: AnalysisContext): String {
        val parts = mutableListOf<String>()

        info.category?.let { parts.add(it) }
        info.capacity?.let { parts.add("Capacity: $it") }

        context.lines
            .filter { it.quality.score > 55 }
            .filter { it.type == LineType.DESCRIPTIVE }
            .filter { line ->
                !line.text.contains(info.brand ?: "", ignoreCase = true) &&
                        !line.text.contains(info.productLine ?: "", ignoreCase = true)
            }
            .take(2)
            .forEach { parts.add(it.text) }

        return parts.joinToString(" • ").ifBlank { "No description available" }
    }

    private fun calculateOverallConfidence(info: ProductInfo, context: AnalysisContext): Double {
        var confidence = 0.0

        if (info.brand != null) confidence += 30.0
        if (info.productLine != null) confidence += 20.0
        if (info.modelNumber != null) confidence += 15.0
        if (info.capacity != null) confidence += 20.0
        if (info.category != null) confidence += 10.0

        val avgQuality = context.lines.mapNotNull { it.quality.score }.average()
        if (!avgQuality.isNaN()) {
            confidence += (avgQuality * 0.05).coerceAtMost(5.0)
        }

        return (confidence / 100.0).coerceIn(0.0, 1.0)
    }

    private fun cleanupTempFiles() {
        try {
            context.cacheDir.listFiles { it.name.startsWith("temp_") && it.name.endsWith(".jpg") }
                ?.forEach { it.delete() }
        } catch (_: Exception) {
            // Ignore
        }
    }

    private fun saveBitmapToTempUri(bitmap: Bitmap): Uri {
        val file = java.io.File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return Uri.fromFile(file)
    }

    // === INNER CLASSES ===

    inner class LanguageDetector {
        fun scoreEnglishLikelihood(text: String): Double {
            var score = 0.5
            val lowerText = text.lowercase()
            val words = text.split(Regex("\\s+")).filter { it.length > 2 }

            // English words
            val englishCount = words.count { word ->
                ENGLISH_TECH_WORDS.any { word.lowercase().contains(it) }
            }
            score += englishCount * 0.20

            // Non-English words
            val nonEnglishCount = words.count { word ->
                NON_ENGLISH_INDICATORS.any { word.lowercase().contains(it) }
            }
            score -= nonEnglishCount * 0.30

            // Accents
            val accentedChars = text.count { it in "áéíóúñüàèìòùâêîôûëïöç" }
            val accentRatio = if (text.length > 0) accentedChars.toDouble() / text.length else 0.0
            score -= accentRatio * 3.0

            // English patterns
            if (lowerText.matches(Regex(".*\\b(the|and|with|for|from|this|that)\\b.*"))) {
                score += 0.25
            }

            // Spanish/French patterns
            if (lowerText.matches(Regex(".*\\b(de|el|la|le|du|des|une?|los|las)\\b.*"))) {
                score -= 0.35
            }

            // Brands
            val allBrands = brandDatabase.getAllBrands()
            if (allBrands.any { lowerText.contains(it.lowercase()) }) {
                score += 0.25
            }

            return score.coerceIn(0.0, 1.0)
        }
    }

    inner class LogoDetector {
        private val logoPatterns = mapOf(
            "SanDisk" to listOf("sandisk", "san disk", "SANDISK"),
            "Samsung" to listOf("samsung", "SAMSUNG"),
            "Western Digital" to listOf("western digital", "wd", "WD", "westerndigital"),
            "Kingston" to listOf("kingston", "KINGSTON"),
            "Seagate" to listOf("seagate", "SEAGATE"),
            "Crucial" to listOf("crucial", "CRUCIAL"),
            "Corsair" to listOf("corsair", "CORSAIR"),
            "PNY" to listOf("pny", "PNY"),
            "Lexar" to listOf("lexar", "LEXAR"),
            "Transcend" to listOf("transcend", "TRANSCEND")
        )

        fun detectLogos(text: String, labels: List<LabelInfo>): List<String> {
            val detected = mutableSetOf<String>()
            val lowerText = text.lowercase()

            // Text-based detection
            for ((brand, patterns) in logoPatterns) {
                if (patterns.any { lowerText.contains(it.lowercase()) }) {
                    detected.add(brand)
                    Log.d(TAG, "Logo detected in text: $brand")
                }
            }

            // Image label detection
            for (label in labels) {
                for ((brand, patterns) in logoPatterns) {
                    if (patterns.any { label.label.contains(it, ignoreCase = true) }) {
                        detected.add(brand)
                        Log.d(TAG, "Logo detected in image: $brand")
                    }
                }
            }

            return detected.toList()
        }
    }

    inner class IntelligentNameBuilder {
        fun buildName(info: ProductInfo, context: AnalysisContext): String {
            val parts = mutableListOf<String>()

            // Priority 1: Brand (from logo or text)
            info.brand?.let { brand ->
                parts.add(brand)
                Log.d(TAG, "Name part 1: Brand = $brand")
            }

            // Priority 2: Product Line (Cruzer, Ultra, etc.)
            info.productLine?.let { line ->
                if (!parts.any { it.equals(line, ignoreCase = true) }) {
                    parts.add(line)
                    Log.d(TAG, "Name part 2: Product Line = $line")
                }
            }

            // Priority 3: Capacity
            info.capacity?.let { capacity ->
                parts.add(capacity)
                Log.d(TAG, "Name part 3: Capacity = $capacity")
            }

            // Priority 4: Category (if we don't have enough info)
            if (parts.size < 2) {
                info.category?.let { category ->
                    if (!parts.any { it.equals(category, ignoreCase = true) }) {
                        parts.add(category)
                        Log.d(TAG, "Name part 4: Category = $category")
                    }
                }
            }

            // Fallback: use highest quality line
            if (parts.isEmpty()) {
                context.lines
                    .filter { it.quality.score > 60 }
                    .maxByOrNull { it.quality.score }
                    ?.let {
                        val cleaned = cleanNameText(it.text)
                        if (cleaned.isNotBlank()) {
                            parts.add(cleaned)
                            Log.d(TAG, "Name fallback: $cleaned")
                        }
                    }
            }

            val finalName = parts.joinToString(" ").ifBlank { "Unknown Item" }
            Log.d(TAG, "=== Final Name: $finalName ===")
            return finalName
        }

        private fun cleanNameText(text: String): String {
            var cleaned = text

            // Remove noise words
            val words = cleaned.split(Regex("\\s+"))
            val filtered = words.filter { word ->
                !NOISE_WORDS.contains(word.lowercase()) &&
                        word.length > 1
            }

            cleaned = filtered.joinToString(" ").take(60)
            return cleaned.trim()
        }
    }

    inner class BrandDatabase {
        private val brands = mapOf(
            "storage" to listOf(
                "SanDisk", "Samsung", "Western Digital", "WD", "Seagate",
                "Kingston", "Crucial", "Intel", "Corsair", "PNY", "Transcend",
                "Lexar", "ADATA", "G.SKILL", "TeamGroup", "Patriot", "Mushkin",
                "Silicon Power", "Toshiba", "Verbatim", "Sony"
            ),
            "general" to listOf(
                "Apple", "Sony", "LG", "HP", "Dell", "Lenovo", "ASUS", "Acer",
                "Microsoft", "Google", "Amazon", "Logitech", "Razer", "Anker"
            )
        )

        fun getAllBrands(): List<String> = brands.values.flatten()

        fun findBrand(context: AnalysisContext, category: String?, detectedLogos: List<String>): String? {
            // Priority 1: Detected logos
            if (detectedLogos.isNotEmpty()) {
                Log.d(TAG, "Brand from logo: ${detectedLogos.first()}")
                return detectedLogos.first()
            }

            val allBrands = getAllBrands()
            val categoryBrands = category?.let { brands[it.lowercase()] } ?: emptyList()

            // Priority 2: Category-specific brands in high-quality lines
            for (unit in context.lines.sortedByDescending { it.quality.score }.take(7)) {
                for (brand in categoryBrands) {
                    if (unit.text.contains(brand, ignoreCase = true)) {
                        Log.d(TAG, "Brand from category search: $brand")
                        return brand
                    }
                }
            }

            // Priority 3: Any brand in high-quality lines
            for (unit in context.lines.sortedByDescending { it.quality.score }.take(7)) {
                for (brand in allBrands) {
                    if (unit.text.contains(brand, ignoreCase = true)) {
                        Log.d(TAG, "Brand from general search: $brand")
                        return brand
                    }
                }
            }

            // Priority 4: Image labels
            for (label in context.imageLabels.filter { it.confidence > 0.6f }) {
                for (brand in allBrands) {
                    if (label.label.contains(brand, ignoreCase = true)) {
                        Log.d(TAG, "Brand from image label: $brand")
                        return brand
                    }
                }
            }

            return null
        }
    }

    inner class ProductTypeClassifier {
        private val typeKeywords = mapOf(
            "USB Flash Drive" to listOf("usb", "flash", "drive", "stick", "pendrive", "thumb", "cruzer", "glide", "blade"),
            "SSD" to listOf("ssd", "solid state", "nvme", "sata"),
            "Hard Drive" to listOf("hdd", "hard drive", "disk drive", "mechanical"),
            "Memory Card" to listOf("sd card", "microsd", "memory card", "flash card", "tf card"),
            "External Storage" to listOf("external", "portable storage", "portable drive"),
            "Power Supply" to listOf("power supply", "psu", "switching", "adapter", "charger", "ac/dc")
        )

        fun classify(context: AnalysisContext): String? {
            val scores = mutableMapOf<String, Double>()

            for ((type, keywords) in typeKeywords) {
                var score = 0.0

                for (unit in context.lines) {
                    for (keyword in keywords) {
                        if (unit.text.contains(keyword, ignoreCase = true)) {
                            score += (unit.quality.score / 100.0) * 1.5
                        }
                    }
                }

                for (label in context.imageLabels) {
                    for (keyword in keywords) {
                        if (label.label.contains(keyword, ignoreCase = true)) {
                            score += label.confidence * 2.5
                        }
                    }
                }

                scores[type] = score
            }

            val best = scores.filter { it.value > 0.6 }.maxByOrNull { it.value }
            Log.d(TAG, "Category scores: $scores")
            Log.d(TAG, "Selected category: ${best?.key}")
            return best?.key
        }
    }

    inner class ContextualExtractor {

        fun extractCapacity(context: AnalysisContext): String? {
            val regex = Regex("(\\d+(?:\\.\\d+)?\\s*(?:TB|GB|MB|PB))\\b", RegexOption.IGNORE_CASE)
            val candidates = mutableListOf<Pair<String, Double>>()

            for (unit in context.lines) {
                regex.findAll(unit.text).forEach {
                    val capacity = it.value.trim().uppercase()
                    candidates.add(capacity to unit.quality.score)
                }
            }

            val best = candidates.maxByOrNull { it.second }?.first
            Log.d(TAG, "Extracted capacity: $best from ${candidates.size} candidates")
            return best
        }

        fun extractProductLine(context: AnalysisContext, brand: String?): String? {
            // SanDisk product lines
            val sandiskLines = listOf("Cruzer", "Ultra", "Extreme", "Ultra Fit", "Ultra Flair", "Glide",
                "Blade", "Switch", "Pop", "Edge", "Force", "Dual Drive", "iXpand")

            // Other brand lines
            val genericLines = listOf("Ultra", "Pro", "Extreme", "Evo", "Plus", "Max", "Elite",
                "Gaming", "Premium", "Performance", "Essential", "Value", "Blue", "Black", "Red")

            val searchLines = if (brand?.equals("SanDisk", ignoreCase = true) == true) {
                sandiskLines + genericLines
            } else {
                genericLines
            }

            // First: Look for exact product line matches in high-quality lines
            for (unit in context.lines.sortedByDescending { it.quality.score }.take(8)) {
                for (line in searchLines) {
                    // Check if the product line appears as a distinct word
                    val pattern = Regex("\\b${Regex.escape(line)}\\b", RegexOption.IGNORE_CASE)
                    if (pattern.containsMatchIn(unit.text)) {
                        Log.d(TAG, "Found product line: $line in '${unit.text}'")
                        return line
                    }
                }
            }

            // Second: Check for multi-word product lines (like "Ultra Fit")
            for (unit in context.lines.sortedByDescending { it.quality.score }.take(8)) {
                val multiWordLines = searchLines.filter { it.contains(" ") }
                for (line in multiWordLines) {
                    if (unit.text.contains(line, ignoreCase = true)) {
                        Log.d(TAG, "Found multi-word product line: $line")
                        return line
                    }
                }
            }

            return null
        }

        fun extractModelNumber(context: AnalysisContext, brand: String?, capacity: String?, productLine: String?): String? {
            val patterns = listOf(
                Regex("(?:Model|SKU|Part|P/N|MPN)[:\\s]+([A-Z0-9\\-]{4,20})", RegexOption.IGNORE_CASE),
                Regex("\\b([A-Z]{2,4}\\d{3,}[A-Z0-9\\-]*)\\b"),
                Regex("\\b(\\d{3,}[A-Z]{2,}[A-Z0-9\\-]*)\\b"),
                brand?.let { Regex("${Regex.escape(it)}[\\s-]?([A-Z0-9\\-]{4,15})\\b", RegexOption.IGNORE_CASE) }
            ).filterNotNull()

            for (unit in context.lines.sortedByDescending { it.quality.score }.take(10)) {
                // Skip very long descriptive lines
                if (unit.text.length > 35 && unit.type == LineType.DESCRIPTIVE) continue

                for (pattern in patterns) {
                    pattern.findAll(unit.text).forEach { match ->
                        val model = match.groupValues.getOrNull(1)?.trim() ?: match.value.trim()

                        // Validation checks
                        if (model.length !in 4..25) return@forEach

                        // Don't confuse capacity with model number
                        if (capacity != null && model.contains(capacity, ignoreCase = true)) return@forEach

                        // Don't confuse product line with model
                        if (productLine != null && model.equals(productLine, ignoreCase = true)) return@forEach

                        if (model.isBlank()) return@forEach
                        if (!model.any { it.isLetterOrDigit() }) return@forEach

                        // Must have mix of letters and numbers
                        val letters = model.count { it.isLetter() }
                        val digits = model.count { it.isDigit() }
                        if (letters < 2 || digits < 1) return@forEach

                        Log.d(TAG, "Found model number: $model")
                        return model
                    }
                }
            }
            return null
        }

        fun extractCondition(context: AnalysisContext): String? {
            val conditions = mapOf(
                "New" to listOf("brand new", "new in box", "sealed", "unopened", "nib"),
                "Like New" to listOf("like new", "mint", "excellent", "pristine", "barely used"),
                "Good" to listOf("good", "used", "working", "functional"),
                "Fair" to listOf("fair", "worn", "some wear"),
                "Poor" to listOf("poor", "damaged", "broken", "for parts", "not working")
            )

            val text = context.rawText.lowercase()
            for ((condition, keywords) in conditions) {
                if (keywords.any { text.contains(it) }) {
                    Log.d(TAG, "Found condition: $condition")
                    return condition
                }
            }
            return null
        }

        fun extractPrice(context: AnalysisContext): Double? {
            val priceRegex = Regex("\\$\\s*([0-9,]+(?:\\.\\d{2})?)")
            val match = priceRegex.find(context.rawText)
            val priceString = match?.groupValues?.get(1)?.replace(",", "")
            val price = priceString?.toDoubleOrNull()
            return if (price != null && price in 0.01..999999.99) {
                Log.d(TAG, "Found price: $price")
                price
            } else null
        }

        fun extractDimensions(context: AnalysisContext): String? {
            val dimensionsRegex = Regex(
                "(\\d+(?:\\.\\d+)?)\\s*[xX×]\\s*(\\d+(?:\\.\\d+)?)(?:\\s*[xX×]\\s*(\\d+(?:\\.\\d+)?))?\\s*(in|inch|cm|mm)?",
                RegexOption.IGNORE_CASE
            )
            val result = dimensionsRegex.find(context.rawText)?.value?.trim()
            if (result != null) {
                Log.d(TAG, "Found dimensions: $result")
            }
            return result
        }
    }

    // Data classes
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

    data class AnalysisContext(
        val rawText: String,
        val imageLabels: List<LabelInfo>,
        val lines: List<SemanticUnit>,
        val detectedLogos: List<String> = emptyList()
    )

    data class LabelInfo(val label: String, val confidence: Float)

    data class SemanticUnit(
        val text: String,
        val originalText: String,
        val position: Int,
        val quality: LineQuality,
        val tokens: List<Token>,
        val type: LineType
    )

    data class LineQuality(val score: Double, val isUseful: Boolean, val confidence: Double)

    data class Token(val text: String, val position: Int, val type: TokenType)

    enum class TokenType { CAPACITY, NUMBER, PROPER_NOUN, ACRONYM, MODEL_CODE, PRODUCT_TYPE, WORD }

    enum class LineType { BRAND_OR_SERIES, CAPACITY_INFO, MODEL_INFO, PRICE_INFO, DIMENSIONS, DESCRIPTIVE }

    data class ProductInfo(
        var name: String = "",
        var brand: String? = null,
        var modelNumber: String? = null,
        var productLine: String? = null,
        var capacity: String? = null,
        var category: String? = null,
        var description: String = "",
        var condition: String? = null,
        var price: Double? = null,
        var dimensions: String? = null
    )
}