// file: com/samuel/inventorymanager/screens/ImageProcessingScreen.kt
package com.samuel.inventorymanager.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream


@Composable
fun ImageProcessingScreen(
    imageUri: Uri,
    onImageProcessed: (Uri) -> Unit,
    onCancel: () -> Unit,
    autoCropOnLaunch: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State for the single image being edited
    var currentImageUri by remember { mutableStateOf(imageUri) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Image adjustment states
    var rotation by remember { mutableFloatStateOf(0f) }
    var brightness by remember { mutableFloatStateOf(1f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }

    var currentTool by remember { mutableStateOf(EditTool.CROP) }

    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { croppedUri ->
                currentImageUri = croppedUri
                // When crop is done, reload the bitmap to reflect changes
                scope.launch {
                    isLoading = true
                    withContext(Dispatchers.IO) {
                        originalBitmap = loadBitmap(context, croppedUri)
                    }
                    isLoading = false
                }
            }
        }
    }

    // Function to save the final bitmap and return a Uri
    fun saveAndProceed() {
        scope.launch {
            isLoading = true
            previewBitmap?.let { finalBitmap ->
                val finalUri = withContext(Dispatchers.IO) {
                    saveBitmapToCache(context, finalBitmap)
                }
                if (finalUri != null) {
                    onImageProcessed(finalUri)
                } else {
                    onCancel() // Fallback if save fails
                }
            }
            isLoading = false
        }
    }

    // Load initial bitmap
    LaunchedEffect(currentImageUri) {
        isLoading = true
        withContext(Dispatchers.IO) {
            originalBitmap = loadBitmap(context, currentImageUri)
        }
        isLoading = false

        // Reset adjustments when bitmap changes
        rotation = 0f
        brightness = 1f
        contrast = 1f
        saturation = 1f
    }

    // Auto-launch crop if requested
    LaunchedEffect(Unit) {
        if (autoCropOnLaunch) {
            cropImageLauncher.launch(
                CropImageContractOptions(
                    uri = currentImageUri,
                    cropImageOptions = CropImageOptions(
                        guidelines = CropImageView.Guidelines.ON,
                        cropMenuCropButtonTitle = "Done"
                    )
                )
            )
        }
    }

    // Debounced preview generation
    var lastUpdateTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(originalBitmap, rotation, brightness, contrast, saturation) {
        val currentTime = System.currentTimeMillis()
        lastUpdateTime = currentTime
        delay(30) // Debounce

        if (lastUpdateTime == currentTime && originalBitmap != null) {
            withContext(Dispatchers.Default) {
                previewBitmap = applyAllTransformations(originalBitmap!!, rotation, brightness, contrast, saturation)
            }
        }
    }

    // Main UI
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0F172A)) {
        if (isLoading || originalBitmap == null || previewBitmap == null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(color = Color(0xFF8B5CF6))
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar with Cancel and Save buttons
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B)).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Cancel", tint = Color.White) }
                    Text("Edit Image", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { saveAndProceed() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Done")
                    }
                }

                // Image Preview
                Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp), contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Image Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // Tools Panel
                Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B)).padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ToolButton(EditTool.CROP, currentTool == EditTool.CROP) { currentTool = EditTool.CROP }
                        ToolButton(EditTool.ROTATE, currentTool == EditTool.ROTATE) { currentTool = EditTool.ROTATE }
                        //ToolButton(EditTool.ADJUST, currentTool == EditTool.ADJUST) { currentTool = EditTool.ADJUST }
                    }
                    Spacer(Modifier.height(16.dp))
                    AnimatedContent(targetState = currentTool, label = "tool_animation") { tool ->
                        when (tool) {
                            EditTool.CROP -> Button(
                                onClick = {
                                    cropImageLauncher.launch(CropImageContractOptions(uri = currentImageUri, cropImageOptions = CropImageOptions(guidelines = CropImageView.Guidelines.ON)))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CropFree, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Open Cropper")
                            }
                            EditTool.ROTATE -> Slider(value = rotation, onValueChange = { rotation = it }, valueRange = -180f..180f)
                            //EditTool.ADJUST -> Column {
                            //    Text("Brightness", color = Color.White, fontSize = 12.sp)
                            //    Slider(value = brightness, onValueChange = { brightness = it }, valueRange = 0f..2f)
                            //    Text("Contrast", color = Color.White, fontSize = 12.sp)
                            //    Slider(value = contrast, onValueChange = { contrast = it }, valueRange = 0.5f..1.5f)
                            //}
//
                            EditTool.PHOTOS -> TODO()
                            EditTool.ADJUST -> TODO()
                        }
                    }
                }
            }
        }
    }
}


// --- Helper Functions and Composables for ImageProcessingScreen ---

private fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
    return try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "processed_${System.currentTimeMillis()}.jpg")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        stream.close()
        androidx.core.content.FileProvider.getUriForFile(
            context,
            context.packageName + ".provider", // IMPORTANT: Must match AndroidManifest
            file
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


private fun applyAllTransformations(
    bitmap: Bitmap,
    degrees: Float,
    brightness: Float,
    contrast: Float,
    saturation: Float
): Bitmap {
    if (degrees == 0f && brightness == 1f && contrast == 1f && saturation == 1f) return bitmap

    val matrix = Matrix().apply { postRotate(degrees) }
    var resultBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

    val canvas = Canvas(resultBitmap)
    val paint = Paint()
    val colorMatrix = ColorMatrix()

    // Apply saturation first
    colorMatrix.setSaturation(saturation)

    // Then apply brightness and contrast
    val scale = contrast
    val translate = (1f - contrast) * 127.5f + (brightness - 1f) * 255f
    colorMatrix.postConcat(ColorMatrix(floatArrayOf(
        scale, 0f, 0f, 0f, translate,
        0f, scale, 0f, 0f, translate,
        0f, 0f, scale, 0f, translate,
        0f, 0f, 0f, 1f, 0f
    )))

    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(resultBitmap, 0f, 0f, paint)

    return resultBitmap
}



