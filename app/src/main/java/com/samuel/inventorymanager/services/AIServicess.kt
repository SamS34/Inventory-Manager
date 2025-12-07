//package com.samuel.inventorymanager.services
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.util.Base64
//import android.util.Log
//import com.samuel.inventorymanager.data.AISettings
//import com.samuel.inventorymanager.screens.AIAnalysisResult
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import org.json.JSONObject
//import java.io.ByteArrayOutputStream
//import java.net.HttpURLConnection
//import java.net.URL
//
//class AIService(private val context: Context) {
//
//    suspend fun analyzeItemFromBitmap(
//        bitmap: Bitmap,
//        aiSettings: AISettings = AISettings()
//    ): AIAnalysisResult = withContext(Dispatchers.IO) {
//        try {
//            // Check if API key exists
//            if (aiSettings.anthropicApiKey.isNullOrBlank()) {
//                throw Exception("AI API Key is missing. Please configure it in Settings.")
//            }
//
//            // 1. Convert bitmap to base64
//            val base64Image = bitmapToBase64(bitmap)
//
//            // 2. Call Claude API
//            val response = callClaudeAPI(base64Image, aiSettings)
//
//            // 3. Parse response
//            parseAIResponse(response)
//        } catch (e: Exception) {
//            Log.e("AIService", "Analysis failed", e)
//            // Return an empty result with the error in description instead of crashing
//            AIAnalysisResult(
//                description = "AI Analysis failed: ${e.message}",
//                rawText = "Error during processing."
//            )
//        }
//    }
//
//    private suspend fun callClaudeAPI(
//        base64Image: String?,
//        aiSettings: AISettings,
//        customPrompt: String? = null
//    ): String = withContext(Dispatchers.IO) {
//        val url = URL("https://api.anthropic.com/v1/messages")
//        val connection = url.openConnection() as HttpURLConnection
//
//        try {
//            connection.requestMethod = "POST"
//            connection.setRequestProperty("Content-Type", "application/json")
//            connection.setRequestProperty("x-api-key", aiSettings.anthropicApiKey)
//            connection.setRequestProperty("anthropic-version", "2023-06-01")
//            connection.doOutput = true
//
//            val prompt = customPrompt ?: buildAnalysisPrompt()
//            val requestBody = buildJSONRequest(prompt, base64Image)
//
//            connection.outputStream.use { it.write(requestBody.toByteArray()) }
//
//            val responseCode = connection.responseCode
//            if (responseCode == 200) {
//                connection.inputStream.bufferedReader().use { it.readText() }
//            } else {
//                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
//                throw Exception("API Error ($responseCode): $error")
//            }
//        } finally {
//            connection.disconnect()
//        }
//    }
//
//    private fun buildAnalysisPrompt(): String = """
//        Analyze this item image and extract the following information in JSON format:
//        {
//            "itemName": "The main product name",
//            "modelNumber": "Model/SKU if visible",
//            "description": "Brief description (2-3 sentences)",
//            "condition": "New/Like New/Good/Fair/Poor based on appearance",
//            "sizeCategory": "Small/Medium/Large/Extra Large",
//            "estimatedPrice": numeric value in USD (just the number),
//            "dimensions": "Estimated dimensions (e.g. 10x10x5 inches) if visual cues exist",
//            "detectedText": "Any text visible in the image"
//        }
//        Be specific. If information is not clearly visible, use null.
//        Respond ONLY with valid JSON.
//    """.trimIndent()
//
//    private fun buildJSONRequest(
//        prompt: String,
//        base64Image: String?
//    ): String {
//        val contentParts = mutableListOf<String>()
//
//        if (base64Image != null) {
//            // Image block
//            contentParts.add("""
//                {
//                    "type": "image",
//                    "source": {
//                        "type": "base64",
//                        "media_type": "image/jpeg",
//                        "data": "$base64Image"
//                    }
//                }
//            """.trimIndent())
//        }
//
//        // Text block (Prompt)
//        contentParts.add("""
//            {
//                "type": "text",
//                "text": "$prompt"
//            }
//        """.trimIndent())
//
//        // Construct final JSON safely
//        return """
//            {
//                "model": "claude-3-5-sonnet-20241022",
//                "max_tokens": 1024,
//                "messages": [
//                    {
//                        "role": "user",
//                        "content": [${contentParts.joinToString(",")}]
//                    }
//                ]
//            }
//        """.trimIndent()
//    }
//
//    private fun parseAIResponse(jsonResponse: String): AIAnalysisResult {
//        try {
//            val rootJson = JSONObject(jsonResponse)
//            val contentArray = rootJson.getJSONArray("content")
//            val textContent = contentArray.getJSONObject(0).getString("text")
//
//            // Clean markdown code blocks if Claude adds them
//            val cleanJsonString = textContent
//                .replace("```json", "")
//                .replace("```", "")
//                .trim()
//
//            val resultJson = JSONObject(cleanJsonString)
//
//            return AIAnalysisResult(
//                itemName = resultJson.optString("itemName").takeIf { it.isNotEmpty() },
//                modelNumber = resultJson.optString("modelNumber").takeIf { it.isNotEmpty() },
//                description = resultJson.optString("description").takeIf { it.isNotEmpty() },
//                condition = resultJson.optString("condition").takeIf { it.isNotEmpty() },
//                sizeCategory = resultJson.optString("sizeCategory").takeIf { it.isNotEmpty() },
//                estimatedPrice = resultJson.optDouble("estimatedPrice").takeIf { !it.isNaN() },
//                dimensions = resultJson.optString("dimensions").takeIf { it.isNotEmpty() },
//                // Map AI "detectedText" to our Data Model "rawText"
//                rawText = resultJson.optString("detectedText").takeIf { it.isNotEmpty() }
//            )
//        } catch (e: Exception) {
//            Log.e("AIService", "JSON Parsing failed", e)
//            return AIAnalysisResult(
//                description = "Could not parse AI response. Raw response: $jsonResponse"
//            )
//        }
//    }
//
//    private fun bitmapToBase64(bitmap: Bitmap): String {
//        val outputStream = ByteArrayOutputStream()
//        // Compress to 70% quality to save bandwidth/tokens
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
//        val bytes = outputStream.toByteArray()
//        return Base64.encodeToString(bytes, Base64.NO_WRAP)
//    }
//}