package com.samuel.inventorymanager.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ProcessingChoice { MANUAL, AI_OCR }
enum class EditTool { CROP, ROTATE, ADJUST }

@Composable
fun ImageEditScreen(
    imageUri: Uri,
    onNext: (Bitmap, ProcessingChoice) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var currentTool by remember { mutableStateOf(EditTool.CROP) }
    var showChoiceDialog by remember { mutableStateOf(false) }

    // Load bitmap once and keep it
    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            bitmap = try {
                context.contentResolver.openInputStream(imageUri)?.use {
                    BitmapFactory.decodeStream(it)
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        bitmap?.let { bmp ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F172A),
                                Color(0xFF1E293B)
                            )
                        )
                    )
            ) {
                // Top Bar
                ModernTopBar(
                    onCancel = onCancel,
                    onNext = { showChoiceDialog = true }
                )

                // Image Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    EditableImagePreview(
                        bitmap = bmp,
                        rotation = rotation,
                        brightness = brightness,
                        contrast = contrast,
                        currentTool = currentTool
                    )
                }

                // Tools Panel
                ModernToolsPanel(
                    currentTool = currentTool,
                    onToolChange = { currentTool = it },
                    rotation = rotation,
                    onRotationChange = { rotation = it },
                    brightness = brightness,
                    onBrightnessChange = { brightness = it },
                    contrast = contrast,
                    onContrastChange = { contrast = it }
                )
            }
        } ?: LoadingScreen()

        // Choice Dialog
        if (showChoiceDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                ProcessingChoiceDialog(
                    onDismiss = { showChoiceDialog = false },
                    onChoice = { choice ->
                        bitmap?.let { bmp ->
                            val processedBitmap = applyAllEdits(bmp, rotation, brightness, contrast)
                            onNext(processedBitmap, choice)
                        }
                        showChoiceDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun ModernTopBar(onCancel: () -> Unit, onNext: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(10f),
        color = Color(0xFF1E293B).copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Close,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                "✨ Edit Photo",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5CF6)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Next", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun EditableImagePreview(
    bitmap: Bitmap,
    rotation: Float,
    brightness: Float,
    contrast: Float,
    currentTool: EditTool
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val filteredBitmap by remember {
        derivedStateOf {
            applyImageFilters(bitmap, brightness, contrast).asImageBitmap()
        }
    }

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.9f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Image(
                    bitmap = filteredBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            rotationZ = rotation,
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 3f)
                                val maxOffset = 500f
                                offset = Offset(
                                    (offset.x + pan.x).coerceIn(-maxOffset, maxOffset),
                                    (offset.y + pan.y).coerceIn(-maxOffset, maxOffset)
                                )
                            }
                        },
                    contentScale = ContentScale.Fit
                )
            }

            if (currentTool == EditTool.CROP) {
                Box(
                    modifier = Modifier.fillMaxSize(0.85f),
                    contentAlignment = Alignment.Center
                ) {
                    CropGridOverlay()
                }
            }
        }
    }
}

@Composable
fun CropGridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridColor = Color.White.copy(alpha = 0.8f)
        val strokeWidth = 3f

        drawRect(
            color = gridColor,
            size = size,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )

        for (i in 1..2) {
            val x = size.width * i / 3
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = strokeWidth
            )
        }

        for (i in 1..2) {
            val y = size.height * i / 3
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidth
            )
        }

        val handleSize = 40f
        val cornerColor = Color(0xFF8B5CF6)

        drawLine(cornerColor, Offset(0f, 0f), Offset(handleSize, 0f), strokeWidth * 2)
        drawLine(cornerColor, Offset(0f, 0f), Offset(0f, handleSize), strokeWidth * 2)

        drawLine(cornerColor, Offset(size.width - handleSize, 0f), Offset(size.width, 0f), strokeWidth * 2)
        drawLine(cornerColor, Offset(size.width, 0f), Offset(size.width, handleSize), strokeWidth * 2)

        drawLine(cornerColor, Offset(0f, size.height - handleSize), Offset(0f, size.height), strokeWidth * 2)
        drawLine(cornerColor, Offset(0f, size.height), Offset(handleSize, size.height), strokeWidth * 2)

        drawLine(cornerColor, Offset(size.width, size.height - handleSize), Offset(size.width, size.height), strokeWidth * 2)
        drawLine(cornerColor, Offset(size.width - handleSize, size.height), Offset(size.width, size.height), strokeWidth * 2)
    }
}

@Composable
fun ModernToolsPanel(
    currentTool: EditTool,
    onToolChange: (EditTool) -> Unit,
    rotation: Float,
    onRotationChange: (Float) -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    contrast: Float,
    onContrastChange: (Float) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(10f),
        color = Color(0xFF1E293B).copy(alpha = 0.95f),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EditTool.entries.forEach { tool ->
                    ModernToolButton(
                        tool = tool,
                        isSelected = currentTool == tool,
                        onClick = { onToolChange(tool) }
                    )
                }
            }

            AnimatedContent(
                targetState = currentTool,
                label = "tool_controls",
                transitionSpec = {
                    fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically()
                }
            ) { tool ->
                when (tool) {
                    EditTool.CROP -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF8B5CF6).copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    null,
                                    tint = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    "Pinch to zoom • Drag to reposition",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    EditTool.ROTATE -> {
                        ModernSliderControl(
                            label = "Rotation",
                            value = rotation,
                            onValueChange = onRotationChange,
                            valueRange = -180f..180f,
                            displayValue = "${rotation.toInt()}°",
                            color = Color(0xFF10B981)
                        )
                    }
                    EditTool.ADJUST -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ModernSliderControl(
                                label = "Brightness",
                                value = brightness,
                                onValueChange = onBrightnessChange,
                                valueRange = -1f..1f,
                                displayValue = "${(brightness * 100).toInt()}",
                                color = Color(0xFFFBBF24)
                            )
                            ModernSliderControl(
                                label = "Contrast",
                                value = contrast,
                                onValueChange = onContrastChange,
                                valueRange = 0f..2f,
                                displayValue = "${(contrast * 100).toInt()}%",
                                color = Color(0xFFEC4899)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernToolButton(tool: EditTool, isSelected: Boolean, onClick: () -> Unit) {
    val (icon, label) = when (tool) {
        EditTool.CROP -> Icons.Default.CropFree to "Crop"
        EditTool.ROTATE -> Icons.Default.RotateRight to "Rotate"
        EditTool.ADJUST -> Icons.Default.Tune to "Adjust"
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF8B5CF6) else Color(0xFF334155),
        label = "bg_color"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        label = "scale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        modifier = Modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Text(
                label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun ModernSliderControl(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    color: Color
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Surface(
                onClick = { showEditDialog = true },
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.2f)
            ) {
                Text(
                    displayValue,
                    color = color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.3f)
            ),
            modifier = Modifier.height(48.dp)
        )
    }

    if (showEditDialog) {
        ValueEditDialog(
            title = label,
            currentValue = value,
            valueRange = valueRange,
            onDismiss = { showEditDialog = false },
            onConfirm = { newValue ->
                onValueChange(newValue)
                showEditDialog = false
            },
            color = color
        )
    }
}

@Composable
fun ValueEditDialog(
    title: String,
    currentValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit,
    color: Color
) {
    var textValue by remember {
        mutableStateOf(
            when (title) {
                "Rotation" -> currentValue.toInt().toString()
                "Brightness" -> (currentValue * 100).toInt().toString()
                "Contrast" -> (currentValue * 100).toInt().toString()
                else -> currentValue.toString()
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            )
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    "Set $title",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text(title) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = color,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        cursorColor = color,
                        focusedIndicatorColor = color,
                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.White)
                    }

                    Button(
                        onClick = {
                            val parsedValue = textValue.toFloatOrNull()
                            if (parsedValue != null) {
                                val finalValue = when (title) {
                                    "Rotation" -> parsedValue.coerceIn(valueRange)
                                    "Brightness" -> (parsedValue / 100f).coerceIn(valueRange)
                                    "Contrast" -> (parsedValue / 100f).coerceIn(valueRange)
                                    else -> parsedValue.coerceIn(valueRange)
                                }
                                onConfirm(finalValue)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = color
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessingChoiceDialog(
    onDismiss: () -> Unit,
    onChoice: (ProcessingChoice) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            )
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        null,
                        tint = Color(0xFF8B5CF6),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "How would you like to proceed?",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                ChoiceOptionCard(
                    icon = Icons.Default.Psychology,
                    title = "✨ AI Auto-Fill",
                    description = "Let AI extract all item details automatically (FREE)",
                    color = Color(0xFF8B5CF6),
                    onClick = { onChoice(ProcessingChoice.AI_OCR) }
                )

                ChoiceOptionCard(
                    icon = Icons.Default.Edit,
                    title = "✍️ Manual Entry",
                    description = "Fill in item details yourself",
                    color = Color(0xFF10B981),
                    onClick = { onChoice(ProcessingChoice.MANUAL) }
                )
            }
        }
    }
}

@Composable
fun ChoiceOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        border = BorderStroke(2.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    description,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = Color(0xFF8B5CF6),
            modifier = Modifier.size(64.dp)
        )
    }
}

// Helper Functions
fun applyImageFilters(bitmap: Bitmap, brightness: Float, contrast: Float): Bitmap {
    val colorMatrix = ColorMatrix().apply {
        set(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness * 255,
            0f, contrast, 0f, 0f, brightness * 255,
            0f, 0f, contrast, 0f, brightness * 255,
            0f, 0f, 0f, 1f, 0f
        ))
    }
    val paint = android.graphics.Paint().apply {
        colorFilter = ColorMatrixColorFilter(colorMatrix)
    }
    val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(result)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return result
}

fun applyAllEdits(bitmap: Bitmap, rotation: Float, brightness: Float, contrast: Float): Bitmap {
    var result = applyImageFilters(bitmap, brightness, contrast)

    if (rotation != 0f) {
        val matrix = Matrix().apply { postRotate(rotation) }
        result = Bitmap.createBitmap(result, 0, 0, result.width, result.height, matrix, true)
    }

    return result
}