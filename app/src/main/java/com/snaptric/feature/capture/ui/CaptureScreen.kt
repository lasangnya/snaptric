package com.snaptric.feature.capture.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.designsystem.theme.Amber70
import com.snaptric.core.designsystem.theme.Charcoal10
import com.snaptric.core.designsystem.theme.Charcoal20
import com.snaptric.core.designsystem.theme.SnaptricSpacing
import com.snaptric.feature.capture.viewmodel.CaptureViewModel

@Composable
fun CaptureScreen(
    viewModel: CaptureViewModel = hiltViewModel(),
    onClose : () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }

    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val capturedValue by viewModel.capturedValue.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val utilities by viewModel.utilities.collectAsState()

    val capturedBitmap by viewModel.capturedBitmap.collectAsState()


    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    if (!hasCameraPermission) {
        Text(text = "Camera permission is required to capture photos.")
        return
    }

    // Auto-select first property if none selected
    LaunchedEffect(properties) {
        if (viewModel.selectedPropertyId.value == null && properties.isNotEmpty()) {
            viewModel.onPropertySelected(properties.first().id)
        }
    }

    // Camera Setup
    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView( // Camera preview
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        // rectangle cutout
        CaptureOverlay()
        // Close button
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(SnaptricSpacing.md),
            shape = CircleShape,
            color = Charcoal20.copy(alpha = 0.7f),
            contentColor = Color.White
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "camera close"
                )
            }
        }
        if (isAnalyzing) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
        } else {
            // Capture Button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = SnaptricSpacing.xl)
                    .size(80.dp)
                    .border(2.dp, Amber70, CircleShape)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        imageCapture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    // 1. Get rotation from the camera sensor
                                    val rotationDegrees = image.imageInfo.rotationDegrees

                                    // 2. Convert to Bitmap
                                    val rawBitmap = image.toBitmap()

                                    // 3. Rotate the bitmap so it is "Upright" (matches what you see)
                                    val rotatedBitmap = if (rotationDegrees != 0) {
                                        val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                                        Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                                    } else {
                                        rawBitmap
                                    }

                                    // 4. NOW crop the rotated bitmap using UI percentages
                                    val croppedBitmap = cropToCenterStrip(rotatedBitmap)

                                    // 5. Send to AI and clean up
                                    viewModel.analyzeAndShowDialog(croppedBitmap)
                                    image.close()
                                }
                                override fun onError(e: ImageCaptureException) {}
                            }
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "capture", tint = Charcoal10)
                }
            }
        }
        // CONFIRMATION DIALOG
        capturedValue?.let { value ->
            ConfirmReadingDialog(
                detectedValue = value,
                properties = properties,
                utilities = utilities,
                onPropertySelected = { viewModel.onPropertySelected(it) },
                onDismiss = { viewModel.clearCapturedValue() },
                onSave = {reading, utilId ->
                    viewModel.saveReading(reading, utilId)
                    onClose() // Go home after save
                },
                capturedBitmap = capturedBitmap
            )
        }
    }
}

@Composable
fun CaptureOverlay(){
    val infiniteTransition = rememberInfiniteTransition(label = "overlay")

    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )

    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 2.dp.toPx()
        val cornerRadius = 12.dp.toPx()
        val cornerBracketLength = 24.dp.toPx()
        val cornerBracketStroke = 3.dp.toPx()
        val cornerInset = 4.dp.toPx()

        // cutout
        val rectWidth = size.width * 0.8f
        val rectHeight = size.height * 0.10f
        val left = (size.width - rectWidth) / 2
        val top = (size.height - rectHeight) / 2
        val right = left + rectWidth
        val bottom = top + rectHeight

        val rectPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(left, top, right, bottom),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            )
        }
        // draw dimmed background except the cutout
        clipPath(rectPath, clipOp = ClipOp.Difference){
            drawRect(Color.Black.copy(alpha = 0.6f))
        }
        // draw pulsing amber border around the cutout
        drawPath(
            path = rectPath,
            color = Amber70.copy(alpha = borderAlpha),
            style = Stroke(width = strokeWidth)
        )

        // Corner brackets
        // Top-left
        drawLine(
            color = Amber70,
            start = Offset(left + cornerInset, top + cornerInset),
            end = Offset(left + cornerInset + cornerBracketLength, top + cornerInset),
            strokeWidth = cornerBracketStroke
        )
        drawLine(
            color = Amber70,
            start = Offset(left + cornerInset, top + cornerInset),
            end = Offset(left + cornerInset, top + cornerInset + cornerBracketLength),
            strokeWidth = cornerBracketStroke
        )

        // Top-right
        drawLine(
            color = Amber70,
            start = Offset(right - cornerInset, top + cornerInset),
            end = Offset(right - cornerInset - cornerBracketLength, top + cornerInset),
            strokeWidth = cornerBracketStroke
        )
        drawLine(
            color = Amber70,
            start = Offset(right - cornerInset, top + cornerInset),
            end = Offset(right - cornerInset, top + cornerInset + cornerBracketLength),
            strokeWidth = cornerBracketStroke
        )

        // Bottom-left
        drawLine(
            color = Amber70,
            start = Offset(left + cornerInset, bottom - cornerInset),
            end = Offset(left + cornerInset + cornerBracketLength, bottom - cornerInset),
            strokeWidth = cornerBracketStroke
        )
        drawLine(
            color = Amber70,
            start = Offset(left + cornerInset, bottom - cornerInset),
            end = Offset(left + cornerInset, bottom - cornerInset - cornerBracketLength),
            strokeWidth = cornerBracketStroke
        )

        // Bottom-right
        drawLine(
            color = Amber70,
            start = Offset(right - cornerInset, bottom - cornerInset),
            end = Offset(right - cornerInset - cornerBracketLength, bottom - cornerInset),
            strokeWidth = cornerBracketStroke
        )
        drawLine(
            color = Amber70,
            start = Offset(right - cornerInset, bottom - cornerInset),
            end = Offset(right - cornerInset, bottom - cornerInset - cornerBracketLength),
            strokeWidth = cornerBracketStroke
        )

        // Scanning line
        val scanY = top + rectHeight * scanLineProgress
        drawLine(
            color = Amber70.copy(alpha = 0.7f),
            start = Offset(left + 12.dp.toPx(), scanY),
            end = Offset(right - 12.dp.toPx(), scanY),
            strokeWidth = 2.dp.toPx()
        )
    }
}

private fun cropToCenterStrip(bitmap: Bitmap) : Bitmap{
    val width = bitmap.width
    val height = bitmap.height

    // Match UI: 80% width, 10% height
    val rectWidth = (width * 0.8f).toInt()
    val rectHeight = (height * 0.10f).toInt()

    val left = (width - rectWidth) / 2
    val top = (height - rectHeight) / 2

    return Bitmap.createBitmap(bitmap, left, top, rectWidth, rectHeight)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmReadingDialog(
    detectedValue : String,
    properties: List<PropertyEntity>,
    utilities: List<UtilityEntity>,
    onPropertySelected : (Long) -> Unit,
    onDismiss: () -> Unit,
    onSave:(Double, Long) -> Unit,
    capturedBitmap: Bitmap?
){
    var editedValue by remember { mutableStateOf(detectedValue) }
    var selectedUtilityId by remember { mutableStateOf<Long?>(null) }

    // auto select the first property when list loads
    LaunchedEffect(utilities) {
        if(selectedUtilityId == null || !utilities.any{ it.id == selectedUtilityId }) {
            selectedUtilityId = utilities.firstOrNull()?.id
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Reading") },
        confirmButton = {
            Button(
                onClick = {
                    val valDouble = editedValue.toDoubleOrNull() ?: 0.0
                    selectedUtilityId?.let { onSave(valDouble, it) }
                },
                enabled = editedValue.isNotBlank() && selectedUtilityId != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.sm),
                modifier = Modifier.padding(SnaptricSpacing.sm)
            ) {
                capturedBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Captured Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray)
                    )
                }
                OutlinedTextField(
                    value = editedValue,
                    onValueChange = { editedValue = it },
                    label = { Text("Meter Reading") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                Text("Select Property", style = MaterialTheme.typography.labelSmall)
                // Property selection (Horizontal chips for speed)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(SnaptricSpacing.sm)) {
                    items(properties) { prop ->
                        FilterChip(
                            selected = false, // Add logic to track selected property
                            onClick = { onPropertySelected(prop.id) },
                            label = { Text(prop.name) }
                        )
                    }
                }

                Text("Select Meter", style = MaterialTheme.typography.labelSmall)
                // Utility selection
                LazyRow(horizontalArrangement = Arrangement.spacedBy(SnaptricSpacing.sm)) {
                    items(utilities) { util ->
                        FilterChip(
                            selected = selectedUtilityId == util.id,
                            onClick = { selectedUtilityId = util.id },
                            label = { Text("${util.name ?: util.type} (${util.unit})") }
                        )
                    }
                }
            }
        }

    )
}
