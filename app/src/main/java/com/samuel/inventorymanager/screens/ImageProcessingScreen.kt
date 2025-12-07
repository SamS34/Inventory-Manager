package com.samuel.inventorymanager.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.samuel.inventorymanager.services.AIService
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun ImageProcessingScreen(
    imageUri: Uri,
    onImageProcessed: (Uri, AIService.AIAnalysisResult) -> Unit,
    onCancel: () -> Unit,
    aiService: AIService
) {
    var currentStep by remember { mutableStateOf(ProcessingStep.EDITING) }
    var editedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var aiResult by remember { mutableStateOf<AIService.AIAnalysisResult?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentStep) {
            ProcessingStep.EDITING -> {
                ImageEditorScreen(
                    imageUri = imageUri,
                    onNext = { bitmap ->
                        editedBitmap = bitmap
                        currentStep = ProcessingStep.AI_ANALYZING
                    },
                    onCancel = onCancel
                )
            }
            ProcessingStep.AI_ANALYZING -> {
                LaunchedEffect(Unit) {
                    delay(500)
                    try {
                        val result = aiService.analyzeItemFromBitmap(editedBitmap!!)
                        aiResult = result
                        currentStep = ProcessingStep.AI_PREVIEW
                    } catch (_: Exception) {
                        onImageProcessed(
                            imageUri,
                            AIService.AIAnalysisResult(
                                itemName = null,
                                confidence = 0.0,
                                modelNumber = null,
                                description = null,
                                estimatedPrice = null,
                                condition = null,
                                sizeCategory = null,
                                dimensions = null,
                                rawText = null
                            )
                        )
                    }
                }
                AIAnalyzingScreen()
            }
            ProcessingStep.AI_PREVIEW -> {
                aiResult?.let { result ->
                    editedBitmap?.let { bitmap ->
                        AIPreviewScreen(
                            result = result,
                            bitmap = bitmap,
                            onAccept = { finalResult ->
                                onImageProcessed(imageUri, finalResult)
                            },
                            onEdit = { finalResult ->
                                onImageProcessed(imageUri, finalResult)
                            }
                        )
                    }
                }
            }
        }
    }
}

enum class ProcessingStep {
    EDITING, AI_ANALYZING, AI_PREVIEW
}

@Composable
fun ImageEditorScreen(
    imageUri: Uri,
    onNext: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var currentTool by remember { mutableStateOf(EditorTool.ROTATE) }

    var showRotationDialog by remember { mutableStateOf(false) }
    var showBrightnessDialog by remember { mutableStateOf(false) }
    var showContrastDialog by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            loadBitmapFromUri(context, imageUri)
        } else {
            loadBitmapFromUriLegacy(context, imageUri)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A0A0A)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                EditorTopBar(
                    onCancel = onCancel,
                    onNext = {
                        bitmap?.let { bmp ->
                            val processed = applyEdits(bmp, rotation, brightness, contrast)
                            onNext(processed)
                        }
                    },
                    modifier = Modifier.zIndex(10f)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    bitmap?.let { bmp ->
                        EditableImage(
                            bitmap = bmp,
                            rotation = rotation,
                            brightness = brightness,
                            contrast = contrast,
                            scale = scale,
                            onScaleChange = { scale = it },
                            onResetView = {
                                scale = 1f
                                rotation = 0f
                            }
                        )
                    }
                }

                EditorToolsPanel(
                    currentTool = currentTool,
                    onToolChange = { currentTool = it },
                    rotation = rotation,
                    onRotationChange = { rotation = it },
                    brightness = brightness,
                    onBrightnessChange = { brightness = it },
                    contrast = contrast,
                    onContrastChange = { contrast = it },
                    onRotationClick = { showRotationDialog = true },
                    onBrightnessClick = { showBrightnessDialog = true },
                    onContrastClick = { showContrastDialog = true },
                    modifier = Modifier.zIndex(10f)
                )
            }

            if (showRotationDialog) {
                NumberInputDialog(
                    title = "Rotation",
                    currentValue = rotation,
                    onDismiss = { showRotationDialog = false },
                    onConfirm = {
                        rotation = it.coerceIn(-180f, 180f)
                        showRotationDialog = false
                    },
                    suffix = "°"
                )
            }

            if (showBrightnessDialog) {
                NumberInputDialog(
                    title = "Brightness",
                    currentValue = brightness * 100,
                    onDismiss = { showBrightnessDialog = false },
                    onConfirm = {
                        brightness = (it / 100f).coerceIn(-1f, 1f)
                        showBrightnessDialog = false
                    },
                    suffix = ""
                )
            }

            if (showContrastDialog) {
                NumberInputDialog(
                    title = "Contrast",
                    currentValue = contrast * 100,
                    onDismiss = { showContrastDialog = false },
                    onConfirm = {
                        contrast = (it / 100f).coerceIn(0f, 2f)
                        showContrastDialog = false
                    },
                    suffix = "%"
                )
            }
        }
    }
}

@Composable
fun EditorTopBar(onCancel: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF1A1A1A),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Icon(Icons.Default.Close, null, tint = Color.White)
                Spacer(Modifier.width(4.dp))
                Text("Cancel", color = Color.White, fontWeight = FontWeight.Medium)
            }
            Text("Edit Image", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Next", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
            }
        }
    }
}

@Composable
fun EditableImage(
    bitmap: Bitmap,
    rotation: Float,
    brightness: Float,
    contrast: Float,
    scale: Float,
    onScaleChange: (Float) -> Unit,
    onResetView: () -> Unit
) {
    val filteredBitmap by remember {
        derivedStateOf {
            applyFilters(bitmap, brightness, contrast).asImageBitmap()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(16.dp))) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.95f)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Image(
                bitmap = filteredBitmap,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(rotationZ = rotation, scaleX = scale, scaleY = scale)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            onScaleChange((scale * zoom).coerceIn(0.5f, 3f))
                        }
                    },
                contentScale = ContentScale.Fit
            )
        }

        IconButton(
            onClick = onResetView,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
        ) {
            Icon(Icons.Default.CropFree, contentDescription = "Reset View", tint = Color.White)
        }
    }
}

enum class EditorTool { ROTATE, ADJUST }

@Composable
fun EditorToolsPanel(
    currentTool: EditorTool,
    onToolChange: (EditorTool) -> Unit,
    rotation: Float,
    onRotationChange: (Float) -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    contrast: Float,
    onContrastChange: (Float) -> Unit,
    onRotationClick: () -> Unit,
    onBrightnessClick: () -> Unit,
    onContrastClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF1A1A1A),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EditorTool.entries.forEach { tool ->
                    ToolButton(tool = tool, isSelected = currentTool == tool, onClick = { onToolChange(tool) })
                }
            }

            AnimatedContent(targetState = currentTool, label = "tool_controls") { tool ->
                when (tool) {
                    EditorTool.ROTATE -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2A2A2A))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Rotation:", color = Color.White, fontSize = 14.sp)
                                TextButton(onClick = onRotationClick) {
                                    Text(
                                        "${rotation.toInt()}°",
                                        color = Color(0xFF6366F1),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Slider(
                                value = rotation,
                                onValueChange = onRotationChange,
                                valueRange = -180f..180f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF6366F1),
                                    activeTrackColor = Color(0xFF6366F1)
                                )
                            )
                        }
                    }
                    EditorTool.ADJUST -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF2A2A2A))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Brightness:", color = Color.White, fontSize = 14.sp)
                                    TextButton(onClick = onBrightnessClick) {
                                        Text(
                                            "${(brightness * 100).toInt()}",
                                            color = Color(0xFFFBBF24),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Slider(
                                    value = brightness,
                                    onValueChange = onBrightnessChange,
                                    valueRange = -1f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFBBF24),
                                        activeTrackColor = Color(0xFFFBBF24)
                                    )
                                )
                            }
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF2A2A2A))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Contrast:", color = Color.White, fontSize = 14.sp)
                                    TextButton(onClick = onContrastClick) {
                                        Text(
                                            "${(contrast * 100).toInt()}%",
                                            color = Color(0xFF8B5CF6),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Slider(
                                    value = contrast,
                                    onValueChange = onContrastChange,
                                    valueRange = 0f..2f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF8B5CF6),
                                        activeTrackColor = Color(0xFF8B5CF6)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolButton(tool: EditorTool, isSelected: Boolean, onClick: () -> Unit) {
    val (icon, label) = when (tool) {
        EditorTool.ROTATE -> Icons.AutoMirrored.Filled.RotateRight to "Rotate"
        EditorTool.ADJUST -> Icons.Default.Tune to "Adjust"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFF6366F1) else Color(0xFF2A2A2A),
        modifier = Modifier.padding(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
            Text(
                label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun NumberInputDialog(
    title: String,
    currentValue: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit,
    suffix: String
) {
    var textValue by remember { mutableStateOf(currentValue.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set $title", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter value:", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFF3A3A3A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF6366F1)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text(suffix, color = Color.White.copy(alpha = 0.6f)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = textValue.toFloatOrNull() ?: currentValue
                    onConfirm(value)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Apply", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        },
        containerColor = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun AIAnalyzingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "ai_pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale),
                tint = Color(0xFF6366F1)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("AI Analyzing Image...", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Extracting item details automatically",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }

            CircularProgressIndicator(color = Color(0xFF6366F1), modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
fun AIPreviewScreen(
    result: AIService.AIAnalysisResult,
    bitmap: Bitmap,
    onAccept: (AIService.AIAnalysisResult) -> Unit,
    onEdit: (AIService.AIAnalysisResult) -> Unit
) {
    var editedResult by remember { mutableStateOf(result) }
    var isEditing by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0F0F0F)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.4f
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(colors = listOf(Color.Transparent, Color(0xFF0F0F0F)))
                        )
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("✨ AI Detected (AI is not perfect)", color = Color(0xFF6366F1), fontSize = 22.sp, fontWeight = FontWeight.Bold)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Detected Information", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { isEditing = !isEditing }) {
                                Icon(
                                    if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                    null,
                                    tint = Color(0xFF6366F1)
                                )
                            }
                        }

                        AIField("Item Name", editedResult.itemName, isEditing) {
                            editedResult = editedResult.copy(itemName = it)
                        }
                        AIField("Model", editedResult.modelNumber, isEditing) {
                            editedResult = editedResult.copy(modelNumber = it)
                        }
                        AIField("Description", editedResult.description, isEditing, maxLines = 3) {
                            editedResult = editedResult.copy(description = it)
                        }
                        AIField("Condition", editedResult.condition, isEditing) {
                            editedResult = editedResult.copy(condition = it)
                        }
                        AIField("Size Category", editedResult.sizeCategory, isEditing) {
                            editedResult = editedResult.copy(sizeCategory = it)
                        }
                        editedResult.estimatedPrice?.let { price ->
                            AIField("Est. Price", "$${price}", isEditing) {
                                editedResult = editedResult.copy(estimatedPrice = it.replace("$", "").toDoubleOrNull())
                            }
                        }
                    }
                }
            }

            Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF1A1A1A), shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onEdit(editedResult) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(18.dp)
                    ) {
                        Icon(Icons.Default.Edit, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Edit More", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onAccept(editedResult) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(18.dp)
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Accept", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AIField(
    label: String,
    value: String?,
    isEditing: Boolean,
    maxLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        if (isEditing) {
            OutlinedTextField(
                value = value ?: "",
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color(0xFF2A2A2A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = maxLines
            )
        } else {
            Text(
                value ?: "Not detected",
                color = if (value != null) Color.White else Color.White.copy(alpha = 0.4f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
suspend fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
            android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        }
    } catch (_: Exception) {  // Changed from (e: Exception) to (_: Exception)
        null
    }
}
@Suppress("DEPRECATION")
suspend fun loadBitmapFromUriLegacy(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (_: Exception) {
        null
    }
}
fun applyFilters(bitmap: Bitmap, brightness: Float, contrast: Float): Bitmap {
    val colorMatrix = android.graphics.ColorMatrix().apply {
        set(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness * 255,
            0f, contrast, 0f, 0f, brightness * 255,
            0f, 0f, contrast, 0f, brightness * 255,
            0f, 0f, 0f, 1f, 0f
        ))
    }
    val paint = android.graphics.Paint().apply {
        colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
    }
    val safeConfig = bitmap.config ?: Bitmap.Config.ARGB_8888
    // Using androidx.core.graphics.createBitmap extension function
    val result = androidx.core.graphics.createBitmap(bitmap.width, bitmap.height, safeConfig)
    val canvas = android.graphics.Canvas(result)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return result
}

fun applyEdits(bitmap: Bitmap, rotation: Float, brightness: Float, contrast: Float): Bitmap {
    var result = applyFilters(bitmap, brightness, contrast)

    if (rotation != 0f) {
        val matrix = Matrix().apply { postRotate(rotation) }
        result = Bitmap.createBitmap(result, 0, 0, result.width, result.height, matrix, true)
    }

    return result
}