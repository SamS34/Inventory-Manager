package com.samuel.inventorymanager.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samuel.inventorymanager.services.AIService
import com.samuel.inventorymanager.services.OCRService
import kotlinx.coroutines.delay

@Composable
fun AIProcessingScreen(
    bitmap: Bitmap,
    onComplete: (AIAnalysisResult) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current

    var currentStep by remember { mutableStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }

    val steps = listOf(
        "🔍 Analyzing image...",
        "📝 Extracting text...",
        "🤖 Understanding content...",
        "✨ Generating details..."
    )

    // Animate progress
    LaunchedEffect(currentStep) {
        while (currentStep < steps.size) {
            delay(800)
            progress = (currentStep + 1) / steps.size.toFloat()
            if (currentStep < steps.size - 1) {
                currentStep++
            } else {
                break
            }
        }
    }

    // Perform actual AI processing
    LaunchedEffect(Unit) {
        try {
            // Step 1: OCR
            currentStep = 0
            delay(500)
            val ocrService = OCRService(context)
            val tempUri = saveBitmapToTempUri(context, bitmap)
            val ocrResult = ocrService.performOCR(tempUri)

            // Step 2: AI Analysis
            currentStep = 1
            delay(500)
            val aiService = AIService(context)

            currentStep = 2
            delay(500)
            val aiResult = aiService.analyzeItemFromBitmap(bitmap)

            // Step 3: Combine results
            currentStep = 3
            delay(500)

            val finalResult = AIAnalysisResult(
                itemName = aiResult.itemName ?: ocrResult.text.lines().firstOrNull(),
                modelNumber = aiResult.modelNumber,
                description = aiResult.description ?: ocrResult.text,
                estimatedPrice = aiResult.estimatedPrice,
                dimensions = aiResult.dimensions,
                rawText = ocrResult.text
            )

            delay(300)
            onComplete(finalResult)

        } catch (e: Exception) {
            onError(e.message ?: "AI processing failed")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF312E81)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated AI Icon
            AILoadingAnimation()

            // Progress Text
            Text(
                "AI Magic in Progress",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            // Current Step
            AnimatedContent(
                targetState = steps[currentStep],
                transitionSpec = {
                    fadeIn() + slideInVertically { it } togetherWith
                            fadeOut() + slideOutVertically { -it }
                },
                label = "step_animation"
            ) { step ->
                Text(
                    step,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            // Progress Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF8B5CF6),
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            // Step Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                steps.forEachIndexed { index, _ ->
                    StepIndicator(
                        isComplete = index <= currentStep,
                        isActive = index == currentStep
                    )
                }
            }
        }
    }
}

@Composable
fun AILoadingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_loading")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer rotating circle
        Surface(
            shape = CircleShape,
            color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
            modifier = Modifier
                .size(120.dp)
                .rotate(rotation)
        ) {}

        // Inner icon
        Surface(
            shape = CircleShape,
            color = Color(0xFF8B5CF6),
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
fun StepIndicator(isComplete: Boolean, isActive: Boolean) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isComplete -> Color(0xFF10B981)
            isActive -> Color(0xFF8B5CF6)
            else -> Color.White.copy(alpha = 0.2f)
        },
        label = "step_bg"
    )

    val size by animateDpAsState(
        targetValue = if (isActive) 12.dp else 8.dp,
        label = "step_size"
    )

    Surface(
        shape = CircleShape,
        color = backgroundColor,
        modifier = Modifier.size(size)
    ) {}
}

// Helper function
private fun saveBitmapToTempUri(context: android.content.Context, bitmap: Bitmap): android.net.Uri {
    val file = java.io.File(context.cacheDir, "temp_ai_${System.currentTimeMillis()}.jpg")
    file.outputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    return android.net.Uri.fromFile(file)
}