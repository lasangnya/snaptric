package com.snaptric.feature.capture.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.camera.core.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.feature.capture.viewmodel.CaptureViewModel
import java.io.File
import java.nio.file.Files.size

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
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "camera close",
                tint = Color.White
            )
        }
        if (isAnalyzing) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
        } else {
            // Capture Button
            IconButton(
                onClick = {
                    val file = File(context.cacheDir, "temp.jpg")
                    val options = ImageCapture.OutputFileOptions.Builder(file).build()
                    imageCapture.takePicture(options, ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                viewModel.analyzeAndShowDialog(output.savedUri ?: file.toUri())
                            }
                            override fun onError(e: ImageCaptureException) {}
                        }
                    )
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp).size(72.dp).background(Color.White, CircleShape)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "capture", tint = Color.Black)
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
                onSave = { reading, utilId ->
                    viewModel.saveReading(reading, utilId)
                    onClose() // Go home after save
                }
            )
        }
    }
}

@Composable
fun CaptureOverlay(){
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 2.dp.toPx()
        val cornerRadius = 12.dp.toPx()

        // cutout
        val rectWidth = size.width *0.8f
        val rectHeight = size.width *0.2f
        val left = (size.width -rectWidth)/2
        val top = (size.height - rectHeight) / 2

        val rectPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(left, top, left + rectWidth, top + rectHeight),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            )
        }
        // draw dimmed background except the cutout
        clipPath(rectPath, clipOp = ClipOp.Difference){
            drawRect(Color.Black.copy(alpha = 0.5f))
        }
        // draw white border around the cutout
        drawPath(
            path = rectPath,
            color = Color.White,
            style = Stroke(width = strokeWidth)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmReadingDialog(
    detectedValue : String,
    properties: List<PropertyEntity>,
    utilities: List<UtilityEntity>,
    onPropertySelected : (Long) -> Unit,
    onDismiss: () -> Unit,
    onSave:(Double, Long) -> Unit
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
                enabled = editedValue.isNotBlank() && selectedUtilityId != null
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = editedValue,
                    onValueChange = { editedValue = it },
                    label = { Text("Meter Reading") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Select Property", style = MaterialTheme.typography.labelSmall)
                // Property selection (Horizontal chips for speed)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

