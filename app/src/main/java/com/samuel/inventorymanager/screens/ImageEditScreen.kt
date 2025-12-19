package com.samuel.inventorymanager.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

enum class ProcessingChoice { MANUAL, AI_OCR }
enum class EditTool { CROP, ROTATE, ADJUST, PHOTOS }

@Composable
fun ImageEditScreen(
    imageUri: Uri,
    onNext: (List<Bitmap>, ProcessingChoice) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentImageUri by remember { mutableStateOf(imageUri) }
    var allImageUris by remember { mutableStateOf(listOf(imageUri)) }
    var currentImageIndex by remember { mutableIntStateOf(0) }

    var rotation by remember { mutableFloatStateOf(0f) }
    var brightness by remember { mutableFloatStateOf(1f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }

    var currentTool by remember { mutableStateOf(EditTool.CROP) }
    var showChoiceDialog by remember { mutableStateOf(false) }
    var isProcessingAI by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Crop launcher using Android Image Cropper
    val cropImageLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { croppedUri ->
                currentImageUri = croppedUri
                // Update the current image in the list
                allImageUris = allImageUris.toMutableList().apply {
                    this[currentImageIndex] = croppedUri
                }
                // Reload bitmap
                scope.launch {
                    withContext(Dispatchers.IO) {
                        originalBitmap = loadBitmap(context, croppedUri)
                    }
                }
            }
        }
    }

    // Load bitmap fast
    LaunchedEffect(currentImageIndex, allImageUris) {
        withContext(Dispatchers.IO) {
            val uri = allImageUris[currentImageIndex]
            originalBitmap = loadBitmap(context, uri)
            currentImageUri = uri
        }

        // Reset adjustments when switching images
        rotation = 0f
        brightness = 1f
        contrast = 1f
        saturation = 1f
    }

    // Camera launcher with BACK camera preference
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            allImageUris = allImageUris + tempCameraUri!!
            currentImageIndex = allImageUris.size - 1
            // Auto-open crop tool for new photo
            cropImageLauncher.launch(
                CropImageContractOptions(
                    uri = tempCameraUri!!,
                    cropImageOptions = CropImageOptions(
                        guidelines = CropImageView.Guidelines.ON,
                        fixAspectRatio = false,
                        allowRotation = false,
                        allowFlipping = false,
                        cropMenuCropButtonTitle = "Done",
                        activityBackgroundColor = android.graphics.Color.parseColor("#0F172A")
                    )
                )
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createImageUri(context)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    // Debounced preview
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var lastUpdateTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(originalBitmap, rotation, brightness, contrast, saturation) {
        val currentTime = System.currentTimeMillis()
        lastUpdateTime = currentTime

        delay(30)

        if (lastUpdateTime == currentTime) {
            originalBitmap?.let { bmp ->
                withContext(Dispatchers.Default) {
                    var result = bmp

                    if (rotation != 0f) {
                        result = rotateBitmap(result, rotation)
                    }

                    if (brightness != 1f || contrast != 1f || saturation != 1f) {
                        result = applyColorAdjustmentsFast(result, brightness, contrast, saturation)
                    }

                    previewBitmap = result
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (originalBitmap != null && previewBitmap != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B))))
            ) {
                TopBar(
                    onCancel = onCancel,
                    onNext = { showChoiceDialog = true },
                    imageCount = allImageUris.size
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Image navigation buttons
                    if (allImageUris.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (currentImageIndex > 0) {
                                        currentImageIndex -= 1
                                    }
                                },
                                enabled = currentImageIndex > 0,
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFF1E293B).copy(0.9f), CircleShape)
                            ) {
                                Icon(Icons.Default.ChevronLeft, null, tint = Color.White, modifier = Modifier.size(28.dp))
                            }

                            Text(
                                "${currentImageIndex + 1} / ${allImageUris.size}",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Color(0xFF1E293B).copy(0.9f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            )

                            IconButton(
                                onClick = {
                                    if (currentImageIndex < allImageUris.size - 1) {
                                        currentImageIndex += 1
                                    }
                                },
                                enabled = currentImageIndex < allImageUris.size - 1,
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFF1E293B).copy(0.9f), CircleShape)
                            ) {
                                Icon(Icons.Default.ChevronRight, null, tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }

                ToolsPanel(
                    currentTool = currentTool,
                    onToolChange = { currentTool = it },
                    rotation = rotation,
                    onRotationChange = { rotation = it },
                    brightness = brightness,
                    onBrightnessChange = { brightness = it },
                    contrast = contrast,
                    onContrastChange = { contrast = it },
                    saturation = saturation,
                    onSaturationChange = { saturation = it },
                    onCropClick = {
                        // Launch Android Image Cropper - automatically opens
                        cropImageLauncher.launch(
                            CropImageContractOptions(
                                uri = currentImageUri,
                                cropImageOptions = CropImageOptions(
                                    guidelines = CropImageView.Guidelines.ON,
                                    fixAspectRatio = false,
                                    allowRotation = false,
                                    allowFlipping = false,
                                    cropMenuCropButtonTitle = "Done",
                                    activityBackgroundColor = android.graphics.Color.parseColor("#0F172A")
                                )
                            )
                        )
                    },
                    onTakePhoto = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    bitmap = originalBitmap!!
                )
            }
        } else {
            LoadingScreen()
        }

        if (isProcessingAI) {
            ProcessingOverlay()
        }

        if (showChoiceDialog) {
            ChoiceDialog(
                onDismiss = { showChoiceDialog = false },
                onChoice = { choice ->
                    scope.launch {
                        // Collect all bitmaps from all images
                        val allBitmaps = withContext(Dispatchers.IO) {
                            allImageUris.mapNotNull { uri ->
                                loadBitmap(context, uri)?.let { bmp ->
                                    // Apply transformations to each bitmap
                                    var result = bmp
                                    if (rotation != 0f) {
                                        result = rotateBitmap(result, rotation)
                                    }
                                    if (brightness != 1f || contrast != 1f || saturation != 1f) {
                                        result = applyColorAdjustmentsFast(result, brightness, contrast, saturation)
                                    }
                                    result
                                }
                            }
                        }

                        if (allBitmaps.isNotEmpty()) {
                            showChoiceDialog = false
                            if (choice == ProcessingChoice.AI_OCR) isProcessingAI = true

                            withContext(Dispatchers.Main) {
                                onNext(allBitmaps, choice)
                            }
                        }
                    }
                }
            )
        }
    }
}



fun loadBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(stream, null, options)

            var sampleSize = 1
            val maxDimension = 2048
            while (options.outWidth / sampleSize > maxDimension ||
                options.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            context.contentResolver.openInputStream(uri)?.use { stream2 ->
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }.let { opts ->
                    BitmapFactory.decodeStream(stream2, null, opts)
                }
            }
        }
    } catch (_: Exception) {
        null
    }
}

@Composable
fun TopBar(onCancel: () -> Unit, onNext: () -> Unit, imageCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1E293B).copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(44.dp).background(Color.White.copy(0.1f), CircleShape)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✨ Edit Photo", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (imageCount > 1) {
                    Text("$imageCount photos", color = Color.White.copy(0.6f), fontSize = 12.sp)
                }
            }

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Next", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun ToolsPanel(
    currentTool: EditTool,
    onToolChange: (EditTool) -> Unit,
    rotation: Float,
    onRotationChange: (Float) -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    contrast: Float,
    onContrastChange: (Float) -> Unit,
    saturation: Float,
    onSaturationChange: (Float) -> Unit,
    onCropClick: () -> Unit,
    onTakePhoto: () -> Unit,
    bitmap: Bitmap
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1E293B).copy(0.98f),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EditTool.entries.forEach { tool ->
                    ToolButton(tool, currentTool == tool) { onToolChange(tool) }
                }
            }

            AnimatedContent(
                targetState = currentTool,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tools"
            ) { tool ->
                when (tool) {
                    EditTool.CROP -> CropControls(onCropClick)
                    EditTool.ROTATE -> RotateControls(rotation, onRotationChange, bitmap)
                    EditTool.ADJUST -> AdjustControls(brightness, onBrightnessChange, contrast, onContrastChange, saturation, onSaturationChange)
                    EditTool.PHOTOS -> PhotosControls(onTakePhoto)
                }
            }
        }
    }
}

@Composable
fun CropControls(onCropClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Crop Image", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)

        Button(
            onClick = onCropClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.CropFree, null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text("Crop Photo", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF334155).copy(0.5f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
            Text(
                "Professional crop tool with grid lines and aspect ratios",
                color = Color.White.copy(0.8f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun RotateControls(rotation: Float, onRotationChange: (Float) -> Unit, bitmap: Bitmap) {
    var editingAngle by remember { mutableStateOf(false) }
    var angleText by remember(rotation) { mutableStateOf(rotation.roundToInt().toString()) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Rotate", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0f, 90f, 180f, 270f).forEach { angle ->
                Button(
                    onClick = { onRotationChange(angle) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (rotation == angle) Color(0xFF10B981) else Color(0xFF334155)
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("${angle.toInt()}°", fontWeight = FontWeight.Bold)
                }
            }
        }

        Button(
            onClick = { onRotationChange(detectAutoRotation(bitmap)) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Auto Detect", fontWeight = FontWeight.Bold)
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fine Tune", color = Color.White, fontSize = 13.sp)

                if (editingAngle) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF334155),
                        modifier = Modifier.width(80.dp)
                    ) {
                        BasicTextField(
                            value = angleText,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                    angleText = newValue
                                    newValue.toFloatOrNull()?.let { angle ->
                                        if (angle <= 360f) onRotationChange(angle)
                                    }
                                }
                            },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    Text(
                        "${rotation.roundToInt()}°",
                        color = Color(0xFF10B981),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF334155))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    editingAngle = true
                                    angleText = rotation.roundToInt().toString()
                                }
                            }
                    )
                }
            }

            Slider(
                value = rotation,
                onValueChange = {
                    onRotationChange(it)
                    editingAngle = false
                },
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF10B981),
                    activeTrackColor = Color(0xFF10B981),
                    inactiveTrackColor = Color(0xFF10B981).copy(0.3f)
                )
            )
        }
    }
}

@Composable
fun AdjustControls(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    contrast: Float,
    onContrastChange: (Float) -> Unit,
    saturation: Float,
    onSaturationChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 350.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Adjustments", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)

        SliderControl("☀️ Brightness", brightness, onBrightnessChange, 0.5f..1.5f, Color(0xFFFBBF24), 1f)
        SliderControl("◐ Contrast", contrast, onContrastChange, 0.5f..1.5f, Color(0xFFEC4899), 1f)
        SliderControl("🎨 Saturation", saturation, onSaturationChange, 0f..2f, Color(0xFF10B981), 1f)

        Text("💡 Changes apply in real-time", color = Color.White.copy(0.6f), fontSize = 11.sp)
    }
}

@Composable
fun SliderControl(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    color: Color,
    defaultValue: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 13.sp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    String.format("%.2f", value),
                    color = color,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color(0xFF334155), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                IconButton(
                    onClick = { onValueChange(defaultValue) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White.copy(0.6f), modifier = Modifier.size(18.dp))
                }
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(0.3f)
            )
        )
    }
}

@Composable
fun PhotosControls(onTakePhoto: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Add Photos", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Button(
            onClick = onTakePhoto,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Take Another Photo", fontWeight = FontWeight.Bold)
        }
        Text("📸 Add multiple receipts", color = Color.White.copy(0.6f), fontSize = 12.sp)
    }
}

@Composable
fun ToolButton(tool: EditTool, isSelected: Boolean, onClick: () -> Unit) {
    val (icon, label) = when (tool) {
        EditTool.CROP -> Icons.Default.CropFree to "Crop"
        EditTool.ROTATE -> Icons.AutoMirrored.Filled.RotateRight to "Rotate"
        EditTool.ADJUST -> Icons.Default.Tune to "Adjust"
        EditTool.PHOTOS -> Icons.Default.CameraAlt to "Photos"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF8B5CF6) else Color(0xFF334155)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
fun ChoiceDialog(onDismiss: () -> Unit, onChoice: (ProcessingChoice) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("How to proceed?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                ChoiceCard(Icons.Default.Psychology, "✨ AI Auto-Fill", "Extract details", Color(0xFF8B5CF6)) {
                    onChoice(ProcessingChoice.AI_OCR)
                }

                ChoiceCard(Icons.Default.Edit, "✍️ Manual", "Fill manually", Color(0xFF10B981)) {
                    onChoice(ProcessingChoice.MANUAL)
                }
            }
        }
    }
}

@Composable
fun ChoiceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.15f)),
        border = androidx.compose.foundation.BorderStroke(2.dp, color.copy(0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = color, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
            Column {
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(desc, color = Color.White.copy(0.7f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ProcessingOverlay() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.85f)).zIndex(200f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = Color(0xFF8B5CF6), modifier = Modifier.size(60.dp))
            Text("Loading...", color = Color.White, fontSize = 16.sp)
        }
    }
}

// HELPER FUNCTIONS

fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    if (degrees == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun applyColorAdjustmentsFast(
    bitmap: Bitmap,
    brightness: Float,
    contrast: Float,
    saturation: Float
): Bitmap {
    val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint()

    val colorMatrix = ColorMatrix()
    colorMatrix.setSaturation(saturation)

    val scale = contrast
    val translate = (1f - contrast) * 127.5f + (brightness - 1f) * 127.5f

    val adjustMatrix = ColorMatrix(floatArrayOf(
        scale, 0f, 0f, 0f, translate,
        0f, scale, 0f, 0f, translate,
        0f, 0f, scale, 0f, translate,
        0f, 0f, 0f, 1f, 0f
    ))

    colorMatrix.postConcat(adjustMatrix)
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)

    return result
}

// --- 1. The Logic Function (Keep this outside of UI functions) ---
fun detectAutoRotation(bitmap: android.graphics.Bitmap): Float {
    val width = bitmap.width
    val height = bitmap.height
    if (width == 0 || height == 0) return 0f

    var horizontalEdges = 0
    var verticalEdges = 0
    val samples = 50

    for (i in 0 until samples) {
        val x = (width * i / samples).coerceIn(0, width - 2)
        val y = (height * i / samples).coerceIn(0, height - 2)
        try {
            val p1 = bitmap.getPixel(x, y)
            val p2 = bitmap.getPixel(x + 1, y)
            val p3 = bitmap.getPixel(x, y + 1)

            val hDiff = abs(android.graphics.Color.red(p1) - android.graphics.Color.red(p2)) +
                    abs(android.graphics.Color.green(p1) - android.graphics.Color.green(p2)) +
                    abs(android.graphics.Color.blue(p1) - android.graphics.Color.blue(p2))

            val vDiff = abs(android.graphics.Color.red(p1) - android.graphics.Color.red(p3)) +
                    abs(android.graphics.Color.green(p1) - android.graphics.Color.green(p3)) +
                    abs(android.graphics.Color.blue(p1) - android.graphics.Color.blue(p3))

            if (hDiff > 50) horizontalEdges++
            if (vDiff > 50) verticalEdges++
        } catch (_: Exception) {}
    }

    val aspectRatio = width.toFloat() / height.toFloat()
    return when {
        aspectRatio > 1.2f && verticalEdges > horizontalEdges * 1.4f -> 90f
        aspectRatio < 0.8f && horizontalEdges > verticalEdges * 1.4f -> 270f
        verticalEdges > horizontalEdges * 2f -> 180f
        else -> 0f
    }
}

// --- 2. The UI Component (Put your UI code inside a Composable) ---



@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = Color(0xFF8B5CF6), modifier = Modifier.size(60.dp))
            Text("Loading...", color = Color.White, fontSize = 16.sp)
        }
    }
}