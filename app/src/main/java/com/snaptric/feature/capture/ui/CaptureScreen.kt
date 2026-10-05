package com.snaptric.feature.capture.ui

import android.Manifest
import android.util.Size
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import kotlinx.coroutines.withTimeoutOrNull
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import com.snaptric.ai.mlkit.LiveCounterReader
import com.snaptric.ai.mlkit.LiveFrame
import com.snaptric.ai.mlkit.visiblePart
import com.snaptric.ai.mlkit.AutoCaptureGate
import java.util.concurrent.Executors
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AssistChip
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.domain.insights.MeterMatch
import com.snaptric.core.domain.insights.ReadingCheck
import com.snaptric.core.domain.insights.checkReading
import com.snaptric.core.domain.insights.formatAmount
import java.math.BigDecimal
import kotlin.math.roundToInt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import com.snaptric.core.designsystem.components.accentColor
import com.snaptric.core.designsystem.components.formatMeterValue
import com.snaptric.core.designsystem.components.icon
import com.snaptric.core.designsystem.components.label
import com.snaptric.core.designsystem.theme.EcoGreen
import com.snaptric.core.designsystem.theme.tabularNumbers
import kotlinx.coroutines.delay
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

/**
 * CaptureScreen provides the camera interface for scanning meter readings.
 * It handles camera initialization, permissions check, and real-time UI overlays.
 */
@Composable
fun CaptureScreen(
    viewModel: CaptureViewModel = hiltViewModel(),
    onClose : () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // Photos are capped near 1920 px: plenty for the counter, and much faster to read than full
    // sensor resolution.
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(Size(1920, 1440), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)
                    )
                    .build()
            )
            .build()
    }
    val previewView = remember { PreviewView(context) }

    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val capturedValue by viewModel.capturedValue.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val utilities by viewModel.utilities.collectAsState()

    val capturedBitmap by viewModel.capturedBitmap.collectAsState()
    val selectedPropertyId by viewModel.selectedPropertyId.collectAsState()
    val selectedUtilityHistory by viewModel.selectedUtilityHistory.collectAsState()
    val uncertainDigits by viewModel.uncertainDigits.collectAsState()
    val meterMatch by viewModel.meterMatch.collectAsState()

    // Set once a reading is saved; shows the success animation, then closes the screen.
    var savedReading by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(savedReading) {
        if (savedReading != null) {
            delay(1200)
            onClose()
        }
    }


    // Camera permission is asked for here, when it's needed, and re-checked whenever the screen
    // resumes so granting it in system Settings takes effect without leaving the screen.
    fun cameraGranted() = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    var hasCameraPermission by remember { mutableStateOf(cameraGranted()) }
    var permissionAsked by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        permissionAsked = true
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { hasCameraPermission = cameraGranted() }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    if (!hasCameraPermission) {
        CameraPermissionNeeded(
            // After a denial Android may stop showing the prompt, so offer system Settings instead.
            showSettings = permissionAsked,
            onAllow = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                )
            },
            onClose = onClose
        )
        return
    }

    // Auto-select first property if none selected to simplify the flow.
    LaunchedEffect(properties) {
        properties.firstOrNull()?.let { viewModel.selectDefaultProperty(it.id) }
    }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var autoCapture by rememberSaveable { mutableStateOf(true) }
    var capturing by remember { mutableStateOf(false) }
    var liveFrame by remember { mutableStateOf<LiveFrame?>(null) }
    val autoCaptureGate = remember { AutoCaptureGate() }

    // Takes the photo, crops it to the target strip, and sends both to the analyzer.
    fun capture() {
        if (capturing) return
        capturing = true
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    // Rotate to match what the user sees, then keep only the area inside the overlay.
                    val rotationDegrees = image.imageInfo.rotationDegrees
                    val rawBitmap = image.toBitmap().visiblePart(image.cropRect)
                    val rotatedBitmap = if (rotationDegrees != 0) {
                        val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                        Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                    } else {
                        rawBitmap
                    }
                    val croppedBitmap = cropToCenterStrip(rotatedBitmap)
                    viewModel.analyzeAndShowDialog(croppedBitmap, fullFrame = rotatedBitmap)
                    image.close()
                    capturing = false
                }
                override fun onError(e: ImageCaptureException) {
                    capturing = false
                }
            }
        )
    }

    // Handles every analysed preview frame. Called directly rather than through a state change,
    // because identical consecutive frames are equal and wouldn't trigger recomposition.
    val onLiveFrame by rememberUpdatedState { frame: LiveFrame ->
        liveFrame = frame
        val ready = autoCapture && !isAnalyzing && !capturing && capturedValue == null && savedReading == null
        if (autoCaptureGate.shouldCapture(frame.value, ready)) capture()
    }

    // Reads the counter from preview frames so the photo can be taken automatically once it's steady.
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val liveReader = remember { LiveCounterReader(cropToTarget = ::cropToCenterStrip) { onLiveFrame(it) } }
    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            liveReader.close()
        }
    }

    // CameraX setup: binds preview, photo capture and live analysis to the lifecycle.
    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(analysisExecutor, liveReader) }
        // Share the preview's view port so photos are cropped to exactly what's on screen, and the
        // target strip in the photo lines up with the box drawn over the preview.
        val viewPort = withTimeoutOrNull(2_000) {
            while (previewView.viewPort == null) delay(50)
            previewView.viewPort
        }
        val useCases = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(imageCapture)
            .addUseCase(analysis)
            .apply { viewPort?.let(::setViewPort) }
            .build()
        cameraProvider.unbindAll()
        camera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, useCases)
    }

    LaunchedEffect(torchOn, camera) { camera?.cameraControl?.enableTorch(torchOn) }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // The live camera feed.
        AndroidView( 
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        
        // Draw the visual guides (rectangle and scanning line).
        CaptureOverlay()

        // Live feedback under the target strip.
        if (capturedValue == null && !isAnalyzing) {
            LiveHint(
                frame = liveFrame,
                autoCapture = autoCapture,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 140.dp)
            )
        }

        // Auto-capture and torch controls.
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(SnaptricSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(SnaptricSpacing.sm)
        ) {
            CameraToggle(
                checked = autoCapture,
                onCheckedChange = {
                    autoCapture = it
                    autoCaptureGate.reset()
                },
                icon = Icons.Default.AutoMode,
                label = if (autoCapture) "Auto on" else "Auto off"
            )
            if (camera?.cameraInfo?.hasFlashUnit() == true) {
                CameraToggle(
                    checked = torchOn,
                    onCheckedChange = { torchOn = it },
                    icon = if (torchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    label = "Torch",
                    highlight = liveFrame?.tooDark == true && !torchOn
                )
            }
        }
        
        // Button to exit the capture screen.
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
        
        // Show a loading indicator while the AI is processing the image.
        if (isAnalyzing) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
        } else {
            // Main Capture Button
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
                    onClick = { capture() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "capture", tint = Charcoal10)
                }
            }
        }
        
        // Display the confirmation sheet once the AI has detected a value.
        capturedValue?.let { value ->
            ConfirmReadingSheet(
                detectedValue = value,
                properties = properties,
                selectedPropertyId = selectedPropertyId,
                utilities = utilities,
                history = selectedUtilityHistory,
                uncertainDigits = uncertainDigits,
                meterMatch = meterMatch,
                preselectedUtilityId = viewModel.preselectedUtilityId,
                onPropertySelected = { viewModel.onPropertySelected(it) },
                onUtilitySelected = { viewModel.onUtilitySelected(it) },
                onDismiss = { viewModel.clearCapturedValue() },
                onSave = { reading, utilId ->
                    viewModel.saveReading(reading, utilId)
                    savedReading = utilities.firstOrNull { it.id == utilId }
                        ?.let { "${formatMeterValue(reading)} ${it.unit}" } ?: formatMeterValue(reading)
                },
                capturedBitmap = capturedBitmap
            )
        }

        savedReading?.let { SaveSuccessOverlay(savedValue = it) }
    }
}

/**
 * Tells the user what the viewfinder sees: the digits it's reading, whether to hold steady, or
 * that it's too dark.
 */
@Composable
private fun LiveHint(frame: LiveFrame?, autoCapture: Boolean, modifier: Modifier = Modifier) {
    val text = when {
        frame?.tooDark == true -> "Too dark to read. Turn on the torch."
        frame?.value != null && autoCapture -> "Reading ${frame.value} · hold steady"
        frame?.value != null -> "Reading ${frame.value}"
        else -> "Line up the meter's numbers in the box"
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Charcoal20.copy(alpha = 0.75f),
        contentColor = Color.White
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.tabularNumbers,
            modifier = Modifier.padding(horizontal = SnaptricSpacing.md, vertical = SnaptricSpacing.sm)
        )
    }
}

/**
 * A pill-shaped on/off control over the camera preview.
 */
@Composable
private fun CameraToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    label: String,
    highlight: Boolean = false
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(50),
        color = when {
            checked || highlight -> MaterialTheme.colorScheme.primary
            else -> Charcoal20.copy(alpha = 0.7f)
        },
        contentColor = if (checked || highlight) MaterialTheme.colorScheme.onPrimary else Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Explains why the camera is needed and how to allow it.
 */
@Composable
private fun CameraPermissionNeeded(
    showSettings: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(SnaptricSpacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.md)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text("Allow camera access", style = MaterialTheme.typography.titleLarge)
            Text(
                "Snaptric reads your meter from a photo. Photos are processed on this phone and never uploaded.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = if (showSettings) onOpenSettings else onAllow) {
                Text(if (showSettings) "Open settings" else "Allow camera")
            }
            TextButton(onClick = onClose) { Text("Not now") }
        }
    }
}

/**
 * CaptureOverlay draws a scanning rectangle and a moving scan line animation
 * to help the user align the meter reading within the target area.
 */
@Composable
fun CaptureOverlay(){
    val infiniteTransition = rememberInfiniteTransition(label = "overlay")

    // Pulsing effect for the border alpha.
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )

    // Vertical progress for the scanning line animation.
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

        // Calculate the scanning rectangle dimensions (80% width, 10% height).
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
        
        // 1. Dim the background area outside the scanning rectangle.
        clipPath(rectPath, clipOp = ClipOp.Difference){
            drawRect(Color.Black.copy(alpha = 0.6f))
        }
        
        // 2. Draw the pulsing main border.
        drawPath(
            path = rectPath,
            color = Amber70.copy(alpha = borderAlpha),
            style = Stroke(width = strokeWidth)
        )

        // 3. Draw high-visibility corner brackets.
        // Top-left
        drawLine(color = Amber70, start = Offset(left + cornerInset, top + cornerInset), end = Offset(left + cornerInset + cornerBracketLength, top + cornerInset), strokeWidth = cornerBracketStroke)
        drawLine(color = Amber70, start = Offset(left + cornerInset, top + cornerInset), end = Offset(left + cornerInset, top + cornerInset + cornerBracketLength), strokeWidth = cornerBracketStroke)

        // Top-right
        drawLine(color = Amber70, start = Offset(right - cornerInset, top + cornerInset), end = Offset(right - cornerInset - cornerBracketLength, top + cornerInset), strokeWidth = cornerBracketStroke)
        drawLine(color = Amber70, start = Offset(right - cornerInset, top + cornerInset), end = Offset(right - cornerInset, top + cornerInset + cornerBracketLength), strokeWidth = cornerBracketStroke)

        // Bottom-left
        drawLine(color = Amber70, start = Offset(left + cornerInset, bottom - cornerInset), end = Offset(left + cornerInset + cornerBracketLength, bottom - cornerInset), strokeWidth = cornerBracketStroke)
        drawLine(color = Amber70, start = Offset(left + cornerInset, bottom - cornerInset), end = Offset(left + cornerInset, bottom - cornerInset - cornerBracketLength), strokeWidth = cornerBracketStroke)

        // Bottom-right
        drawLine(color = Amber70, start = Offset(right - cornerInset, bottom - cornerInset), end = Offset(right - cornerInset - cornerBracketLength, bottom - cornerInset), strokeWidth = cornerBracketStroke)
        drawLine(color = Amber70, start = Offset(right - cornerInset, bottom - cornerInset), end = Offset(right - cornerInset, bottom - cornerInset - cornerBracketLength), strokeWidth = cornerBracketStroke)

        // 4. Draw the animated scanning line.
        val scanY = top + rectHeight * scanLineProgress
        drawLine(
            color = Amber70.copy(alpha = 0.7f),
            start = Offset(left + 12.dp.toPx(), scanY),
            end = Offset(right - 12.dp.toPx(), scanY),
            strokeWidth = 2.dp.toPx()
        )
    }
}

/**
 * Crops the provided bitmap to match the scanning rectangle's relative size in the UI.
 * This ensures the AI only processes the relevant part of the photo.
 */
private fun cropToCenterStrip(bitmap: Bitmap) : Bitmap{
    val width = bitmap.width
    val height = bitmap.height

    // Match UI proportions: 80% width, 10% height.
    val rectWidth = (width * 0.8f).toInt()
    val rectHeight = (height * 0.10f).toInt()

    val left = (width - rectWidth) / 2
    val top = (height - rectHeight) / 2

    return Bitmap.createBitmap(bitmap, left, top, rectWidth, rectHeight)
}

/**
 * A bottom sheet for the user to review the AI-detected meter reading,
 * edit it if necessary, and select which property/meter it belongs to.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmReadingSheet(
    detectedValue: String,
    properties: List<PropertyEntity>,
    selectedPropertyId: Long?,
    utilities: List<UtilityEntity>,
    history: List<ReadingEntity>,
    onPropertySelected: (Long) -> Unit,
    onUtilitySelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Double, Long) -> Unit,
    capturedBitmap: Bitmap?,
    uncertainDigits: Set<Int> = emptySet(),
    meterMatch: MeterMatch? = null,
    preselectedUtilityId: Long? = null
) {
    var editedValue by remember { mutableStateOf(detectedValue) }
    var selectedUtilityId by remember { mutableStateOf<Long?>(null) }
    val selectedUtility = utilities.firstOrNull { it.id == selectedUtilityId }
    val parsedValue = editedValue.toDoubleOrNull()
    // Checked against the selected meter's history as the user edits the value.
    val check = remember(parsedValue, history) {
        parsedValue?.let { checkReading(it, System.currentTimeMillis(), history) } ?: ReadingCheck.Plausible
    }

    LaunchedEffect(selectedUtilityId) { onUtilitySelected(selectedUtilityId) }

    // Auto-select the first available utility (meter) for the selected property.
    // The meter the user started from wins, then an automatic match, then the property's first meter.
    LaunchedEffect(utilities, meterMatch) {
        val preferred = (preselectedUtilityId ?: meterMatch?.utility?.id)?.takeIf { id -> utilities.any { it.id == id } }
        if (preferred != null && selectedUtilityId == null) {
            selectedUtilityId = preferred
        } else if (selectedUtilityId == null || !utilities.any { it.id == selectedUtilityId }) {
            selectedUtilityId = preferred ?: utilities.firstOrNull()?.id
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SnaptricSpacing.lg)
                .padding(bottom = SnaptricSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.md)
        ) {
            Column {
                Text("Confirm reading", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Check the number, then choose the meter it belongs to.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Show the cropped image that was used for detection.
            capturedBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Captured meter",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Charcoal10)
                )
            }

            // Allow manual correction of the reading, with a numeric keypad.
            OutlinedTextField(
                value = editedValue,
                onValueChange = { input ->
                    val normalized = input.replace(',', '.')
                    if (normalized.all { it.isDigit() || it == '.' } && normalized.count { it == '.' } <= 1) {
                        editedValue = normalized
                    }
                },
                label = { Text("Meter reading") },
                suffix = selectedUtility?.let { { Text(it.unit) } },
                textStyle = MaterialTheme.typography.headlineSmall.tabularNumbers,
                singleLine = true,
                isError = editedValue.isNotEmpty() && parsedValue == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Point out digits OCR wasn't sure about, until the user edits the value.
            if (uncertainDigits.isNotEmpty() && editedValue == detectedValue) {
                UncertainDigits(value = detectedValue, uncertain = uncertainDigits)
            }

            SheetLabel("Property")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(SnaptricSpacing.sm)) {
                items(properties, key = { it.id }) { prop ->
                    FilterChip(
                        selected = prop.id == selectedPropertyId,
                        onClick = { onPropertySelected(prop.id) },
                        label = { Text(prop.name) },
                        colors = selectedChipColors()
                    )
                }
            }

            SheetLabel("Meter")
            meterMatch?.takeIf { it.utility.id == selectedUtilityId }?.let { match ->
                Text(
                    text = when (match.reason) {
                        MeterMatch.Reason.SERIAL_NUMBER ->
                            "Matched by serial number …${match.utility.serialNumber.orEmpty().takeLast(4)}"
                        MeterMatch.Reason.CLOSEST_READING ->
                            "Chosen because its last reading is just below this one"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (utilities.isEmpty()) {
                Text(
                    "This property has no meters yet. Add one under Properties first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(SnaptricSpacing.sm)) {
                    items(utilities, key = { it.id }) { util ->
                        FilterChip(
                            selected = selectedUtilityId == util.id,
                            onClick = { selectedUtilityId = util.id },
                            leadingIcon = {
                                Icon(
                                    imageVector = util.type.icon,
                                    contentDescription = null,
                                    tint = util.type.accentColor,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            },
                            label = { Text("${util.name ?: util.type.label} (${util.unit})") },
                            colors = selectedChipColors()
                        )
                    }
                }
            }

            if (check != ReadingCheck.Plausible) {
                ReadingWarning(
                    check = check,
                    unit = selectedUtility?.unit.orEmpty(),
                    onUseSuggestion = { editedValue = it }
                )
            }

            Spacer(modifier = Modifier.height(SnaptricSpacing.xs))

            // Actions sit at the bottom, in easy thumb reach.
            Row(horizontalArrangement = Arrangement.spacedBy(SnaptricSpacing.sm)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) { Text("Retake") }
                Button(
                    onClick = {
                        val utilityId = selectedUtilityId
                        if (parsedValue != null && utilityId != null) onSave(parsedValue, utilityId)
                    },
                    enabled = parsedValue != null && selectedUtilityId != null,
                    modifier = Modifier
                        .weight(2f)
                        .height(56.dp)
                ) { Text(if (check == ReadingCheck.Plausible) "Save reading" else "Save anyway") }
            }
        }
    }
}

/**
 * Explains why a reading looks wrong, in the meter's own units, and offers the likely fix.
 */
@Composable
private fun ReadingWarning(check: ReadingCheck, unit: String, onUseSuggestion: (String) -> Unit) {
    val warning = MaterialTheme.colorScheme.error
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, warning.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(SnaptricSpacing.md),
            verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.xs)
        ) {
            val unitSuffix = if (unit.isBlank()) "" else " $unit"
            when (check) {
                is ReadingCheck.LowerThanPrevious -> {
                    Text("Lower than last time", style = MaterialTheme.typography.titleSmall, color = warning)
                    Text(
                        "The last reading was ${formatMeterValue(check.previous)}$unitSuffix. Meters only count up, so check each digit.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is ReadingCheck.UnusuallyHigh -> {
                    val days = check.days.roundToInt().coerceAtLeast(1)
                    val times = (check.usedSinceLast / check.days / check.typicalPerDay).roundToInt()
                    Text("This looks off", style = MaterialTheme.typography.titleSmall, color = warning)
                    Text(
                        "That's ${formatAmount(check.usedSinceLast)}$unitSuffix in ${if (days == 1) "1 day" else "$days days"}, " +
                            "about $times× your usual ${formatAmount(check.typicalPerDay)}$unitSuffix a day." +
                            if (check.suggestion != null) " Was the decimal point missed?" else "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    check.suggestion?.let { suggestion ->
                        val text = BigDecimal.valueOf(suggestion).stripTrailingZeros().toPlainString()
                        AssistChip(
                            onClick = { onUseSuggestion(text) },
                            label = { Text("Use $text") }
                        )
                    }
                }
                ReadingCheck.Plausible -> Unit
            }
        }
    }
}

/**
 * Shows the detected value digit by digit, with the ones OCR was unsure about highlighted.
 */
@Composable
private fun UncertainDigits(value: String, uncertain: Set<Int>) {
    Column(verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.xs)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            value.forEachIndexed { index, char ->
                val flagged = index in uncertain
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (flagged) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = if (flagged) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Text(
                        text = char.toString(),
                        style = MaterialTheme.typography.titleMedium.tabularNumbers,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
        Text(
            text = if (uncertain.size == 1) "Check the highlighted digit." else "Check the highlighted digits.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun selectedChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
)

@Composable
private fun SheetLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * A short celebratory confirmation shown after a reading is saved.
 */
@Composable
private fun SaveSuccessOverlay(savedValue: String) {
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Charcoal10.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(tween(200)) + scaleIn(
                initialScale = 0.6f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = SnaptricSpacing.xl, vertical = SnaptricSpacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(EcoGreen.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = EcoGreen,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(SnaptricSpacing.md))
                    Text("Reading saved", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = savedValue,
                        style = MaterialTheme.typography.bodyLarge.tabularNumbers,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
