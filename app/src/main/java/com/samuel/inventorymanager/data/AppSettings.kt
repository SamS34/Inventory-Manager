package com.samuel.inventorymanager.data

// Theme and appearance settings
enum class AppTheme {
    LIGHT, DARK, SYSTEM, CUSTOM
}

enum class FontSize(val scale: Float) {
    SMALL(0.85f),
    MEDIUM(1.0f),
    LARGE(1.15f),
    EXTRA_LARGE(1.3f)
}

data class CustomTheme(
    val primaryColor: Long = 0xFF6200EE,
    val backgroundColor: Long = 0xFFFFFFFF,
    val surfaceColor: Long = 0xFFFFFFFF,
    val onPrimaryColor: Long = 0xFFFFFFFF,
    val fontSizeScale: Float = 1.0f  // NEW: Font scaling for custom theme
)

// OCR Provider enum
enum class OCRProvider {
    ROBOFLOW, OCR_SPACE, GOOGLE_VISION
}

// AI Provider enum
enum class AIProvider {
    GOOGLE_GEMINI, OPENAI
}

// OCR settings with priority
data class OCRSettings(
    val roboflowApiKey: String = "",
    val ocrSpaceApiKey: String = "",
    val googleVisionApiKey: String = "",
    val providerPriority: List<OCRProvider> = listOf(
        OCRProvider.ROBOFLOW,
        OCRProvider.OCR_SPACE,
        OCRProvider.GOOGLE_VISION
    )
)

// AI settings with priority
data class AISettings(
    val googleGeminiApiKey: String = "",
    val openAIApiKey: String = "",
    val providerPriority: List<AIProvider> = listOf(
        AIProvider.GOOGLE_GEMINI,
        AIProvider.OPENAI
    )
)

// Google integration settings
data class GoogleSettings(
    val signedIn: Boolean = false,
    val userEmail: String = "",
    val autoBackupToDrive: Boolean = false,
    val lastBackupTime: Long = 0  // NEW: Track last backup timestamp
)

// Auto features settings
data class AutoFeatures(
    val autoGoogleBackup: Boolean = false,
    val autoLocalSave: Boolean = true,
    val lastLocalSaveTime: Long = 0  // NEW: Track last local save timestamp
)

// COMPLETE AppSettings with ALL properties
data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val fontSize: FontSize = FontSize.MEDIUM,
    val customTheme: CustomTheme? = null,
    val ocrSettings: OCRSettings = OCRSettings(),
    val aiSettings: AISettings = AISettings(),
    val googleSettings: GoogleSettings = GoogleSettings(),
    val autoFeatures: AutoFeatures = AutoFeatures(),
    val hasShownCameraPreference: Boolean = false,
    val openCameraOnNewItem: Boolean = true
) {
    // JSON serialization for export/import
    fun toJson(): String {
        return com.google.gson.Gson().toJson(this)
    }

    companion object {
        fun fromJson(json: String): AppSettings? {
            return try {
                com.google.gson.Gson().fromJson(json, AppSettings::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}