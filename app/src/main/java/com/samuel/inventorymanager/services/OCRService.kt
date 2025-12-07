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

// Simplified data model
data class OCRResult(
    val text: String,
    val confidence: Double = 0.0,
    val provider: String = "ML Kit (Free)"
)

class OCRService(context: Context) {

    // Use Application Context to prevent Activity leaks
    private val applicationContext = context.applicationContext
    private val TAG = "OCRService"

    // Lazy initialization: Client is only created once when needed, saving memory.
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun performOCR(imageUri: Uri): OCRResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting OCR for Image: $imageUri")

            // 1. Pre-check: Ensure image loadable
            val image: InputImage
            try {
                image = InputImage.fromFilePath(applicationContext, imageUri)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to load image from path", e)
                return@withContext OCRResult("", 0.0, "Error: Image Not Found")
            }

            // 2. Process the image
            val result = recognizer.process(image).await()
            val rawText = result.text

            // 3. Smart Confidence Calculation
            // TextBlock and Line do not have confidence in ML Kit V2, only 'Elements' (words) do.
            val allConfidences = result.textBlocks
                .flatMap { it.lines }
                .flatMap { it.elements }
                .mapNotNull { it.confidence }

            // If we found words, calculate average. If not, 0.0
            val finalConfidence = if (allConfidences.isNotEmpty()) {
                allConfidences.average()
            } else {
                // If text was found but no confidence data exists (rare), default to generic high or low
                if (rawText.isNotEmpty()) 0.80 else 0.0
            }

            // 4. Round confidence for cleaner UI (e.g. 0.92 instead of 0.9234512)
            val roundedConfidence = (finalConfidence * 100.0).roundToInt() / 100.0

            if (rawText.isBlank()) {
                Log.w(TAG, "OCR finished but found no text.")
            } else {
                Log.i(TAG, "OCR Success. Confidence: $roundedConfidence")
            }

            OCRResult(
                text = rawText.trim(), // Smart clean: Remove extra whitespace
                confidence = roundedConfidence,
                provider = "ML Kit (On-Device)"
            )

        } catch (e: Exception) {
            Log.e(TAG, "OCR Critical Failure", e)
            // Return an empty result or rethrow depending on your app's needs.
            // Rethrowing with a clear message allows the ViewModel to handle the UI state "Error"
            throw Exception("Failed to scan text: ${e.localizedMessage}")
        }
    }

    // Backward compatibility: Keeps existing code working while ignoring unused settings
    suspend fun performOCR(imageUri: Uri, ocrSettings: Any?): OCRResult {
        return performOCR(imageUri)
    }
}