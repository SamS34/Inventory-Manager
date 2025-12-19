package com.samuel.inventorymanager.services

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.roundToInt

data class OCRResult(
    val text: String,
    val confidence: Double = 0.0,
    val provider: String = "ML Kit (Free)",
    val lines: List<String> = emptyList(),
    val blocks: List<TextBlock> = emptyList()
)

data class TextBlock(
    val text: String,
    val confidence: Double,
    val boundingBox: android.graphics.Rect?
)

class OCRService(context: Context) {

    private val applicationContext = context.applicationContext
    private val TAG = "OCRService"

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun performOCR(imageUri: Uri): OCRResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Starting OCR for: $imageUri")

            val image: InputImage
            try {
                image = InputImage.fromFilePath(applicationContext, imageUri)
            } catch (e: IOException) {
                Log.e(TAG, "❌ Failed to load image", e)
                return@withContext OCRResult("", 0.0, "Error: Image Not Found")
            }

            val result = recognizer.process(image).await()

            // Extract all text blocks with metadata
            val textBlocks = result.textBlocks.map { block ->
                val blockConfidence = block.lines
                    .flatMap { it.elements }
                    .mapNotNull { it.confidence }
                    .average()
                    .takeIf { !it.isNaN() } ?: 0.8

                TextBlock(
                    text = block.text.trim(),
                    confidence = blockConfidence,
                    boundingBox = block.boundingBox
                )
            }

            // Extract clean lines (sorted by position - top to bottom)
            val cleanLines = result.textBlocks
                .sortedBy { it.boundingBox?.top ?: 0 }
                .flatMap { block ->
                    block.lines.map { line ->
                        line.text.trim()
                    }
                }
                .filter { it.isNotBlank() }

            val rawText = cleanLines.joinToString("\n")

            // Calculate overall confidence
            val allConfidences = result.textBlocks
                .flatMap { it.lines }
                .flatMap { it.elements }
                .mapNotNull { it.confidence }

            val finalConfidence = if (allConfidences.isNotEmpty()) {
                (allConfidences.average() * 100.0).roundToInt() / 100.0
            } else if (rawText.isNotEmpty()) {
                0.80
            } else {
                0.0
            }

            Log.i(TAG, "✅ OCR Complete - ${cleanLines.size} lines found (confidence: $finalConfidence)")

            OCRResult(
                text = rawText,
                confidence = finalConfidence,
                provider = "ML Kit (On-Device)",
                lines = cleanLines,
                blocks = textBlocks
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ OCR Failed", e)
            throw Exception("OCR failed: ${e.localizedMessage}")
        }
    }

    suspend fun performOCR(imageUri: Uri, ocrSettings: Any?): OCRResult {
        return performOCR(imageUri)
    }
}