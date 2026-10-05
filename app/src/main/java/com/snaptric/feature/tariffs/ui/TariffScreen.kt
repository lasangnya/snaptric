package com.snaptric.feature.tariffs.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.snaptric.core.database.entity.TariffEntity
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import com.snaptric.core.designsystem.components.SnaptricBadge
import com.snaptric.core.designsystem.components.SnaptricCard
import com.snaptric.core.designsystem.components.label
import com.snaptric.core.designsystem.theme.SnaptricSpacing
import com.snaptric.core.designsystem.theme.SnaptricTheme
import com.snaptric.core.domain.tariff.activeAt
import com.snaptric.core.domain.tariff.billingUnitsFor
import com.snaptric.core.domain.tariff.formatRate
import com.snaptric.core.domain.units.UnitConversions
import com.snaptric.feature.tariffs.viewmodel.BillScanState
import com.snaptric.feature.tariffs.viewmodel.TariffDraft
import com.snaptric.feature.tariffs.viewmodel.TariffField
import com.snaptric.feature.tariffs.viewmodel.TariffViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Tariffs for one meter: the list of rates over time, and entering a new one by hand or by
 * scanning a bill. A scanned bill always opens the form for review; nothing is saved until the
 * user taps Save.
 */
@Composable
fun TariffScreen(
    viewModel: TariffViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val utility by viewModel.utility.collectAsState()
    val tariffs by viewModel.tariffs.collectAsState()
    val draft by viewModel.draft.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    var showCamera by remember { mutableStateOf(false) }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::readBill)
    }
    val choosePhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    if (showCamera) {
        BackHandler { showCamera = false }
        BillCamera(
            onCaptured = { bitmap ->
                showCamera = false
                viewModel.readBill(bitmap)
            },
            onUseGallery = {
                showCamera = false
                choosePhoto()
            },
            onClose = { showCamera = false }
        )
        return
    }

    BackHandler(enabled = draft != null) { viewModel.cancelDraft() }

    TariffContent(
        utility = utility,
        tariffs = tariffs,
        draft = draft,
        scanState = scanState,
        onBack = { if (draft != null) viewModel.cancelDraft() else onBack() },
        onScanBill = { showCamera = true },
        onChoosePhoto = choosePhoto,
        onEnterManually = viewModel::enterManually,
        onEditTariff = viewModel::edit,
        onDraftChange = viewModel::updateDraft,
        onSave = viewModel::save,
        onCancelDraft = viewModel::cancelDraft,
        onDeleteTariff = viewModel::delete,
        onDismissScanError = viewModel::dismissScanError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TariffContent(
    utility: UtilityEntity?,
    tariffs: List<TariffEntity>,
    draft: TariffDraft?,
    scanState: BillScanState,
    onBack: () -> Unit,
    onScanBill: () -> Unit = {},
    onChoosePhoto: () -> Unit = {},
    onEnterManually: () -> Unit = {},
    onEditTariff: (TariffEntity) -> Unit = {},
    onDraftChange: ((TariffDraft) -> TariffDraft) -> Unit = {},
    onSave: () -> Unit = {},
    onCancelDraft: () -> Unit = {},
    onDeleteTariff: (TariffEntity) -> Unit = {},
    onDismissScanError: () -> Unit = {}
) {
    val meterName = utility?.let { it.name ?: it.type.label }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            draft == null -> listOfNotNull("Tariff", meterName).joinToString(" · ")
                            draft.scanned && draft.id == 0L -> "Check scanned tariff"
                            draft.id == 0L -> "New tariff"
                            else -> "Edit tariff"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (draft != null && utility != null) {
                TariffForm(
                    draft = draft,
                    type = utility.type,
                    onChange = onDraftChange,
                    onSave = onSave,
                    onCancel = onCancelDraft,
                    onDelete = tariffs.firstOrNull { it.id == draft.id && draft.id != 0L }?.let { { onDeleteTariff(it) } }
                )
            } else {
                TariffList(
                    tariffs = tariffs,
                    type = utility?.type,
                    onScanBill = onScanBill,
                    onChoosePhoto = onChoosePhoto,
                    onEnterManually = onEnterManually,
                    onEditTariff = onEditTariff
                )
            }

            if (scanState == BillScanState.Reading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    SnaptricCard {
                        Row(
                            modifier = Modifier.padding(SnaptricSpacing.lg),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(SnaptricSpacing.md)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text("Reading your bill…")
                        }
                    }
                }
            }
        }
    }

    if (scanState is BillScanState.Failed) {
        AlertDialog(
            onDismissRequest = onDismissScanError,
            title = { Text("Couldn't find the tariff") },
            text = { Text(scanState.message) },
            confirmButton = {
                Button(onClick = {
                    onDismissScanError()
                    onEnterManually()
                }) { Text("Enter by hand") }
            },
            dismissButton = { TextButton(onClick = onDismissScanError) { Text("OK") } }
        )
    }
}

@Composable
private fun TariffList(
    tariffs: List<TariffEntity>,
    type: UtilityType?,
    onScanBill: () -> Unit,
    onChoosePhoto: () -> Unit,
    onEnterManually: () -> Unit,
    onEditTariff: (TariffEntity) -> Unit
) {
    val current = remember(tariffs) { tariffs.activeAt(System.currentTimeMillis()) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(SnaptricSpacing.md),
        verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.md)
    ) {
        item {
            SnaptricCard {
                Column(
                    modifier = Modifier.padding(SnaptricSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.sm)
                ) {
                    Text(
                        text = if (tariffs.isEmpty()) "Add your tariff to see what your usage costs" else "Rates changed? Add the new tariff",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Scan the part of your bill that shows the unit rate and standing charge. " +
                            "It's read on this phone and you check every value before saving.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(SnaptricSpacing.sm)) {
                        Button(onClick = onScanBill) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Scan bill", modifier = Modifier.padding(start = SnaptricSpacing.xs))
                        }
                        OutlinedButton(onClick = onChoosePhoto) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Photo", modifier = Modifier.padding(start = SnaptricSpacing.xs))
                        }
                    }
                    TextButton(onClick = onEnterManually) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Enter by hand", modifier = Modifier.padding(start = SnaptricSpacing.xs))
                    }
                }
            }
        }
        items(tariffs, key = { it.id }) { tariff ->
            TariffCard(tariff = tariff, type = type, isCurrent = tariff == current, onClick = { onEditTariff(tariff) })
        }
    }
}

@Composable
private fun TariffCard(tariff: TariffEntity, type: UtilityType?, isCurrent: Boolean, onClick: () -> Unit) {
    SnaptricCard(onClick = onClick) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(SnaptricSpacing.md),
            verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.xs)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SnaptricSpacing.sm)) {
                Text(
                    text = "From ${formatDate(tariff.effectiveFrom)}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                if (tariff.source == TariffDraft.SOURCE_BILL_SCAN) {
                    SnaptricBadge(
                        text = "From bill",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                if (isCurrent) SnaptricBadge(text = "Current")
            }
            Text(
                text = "Unit rate ${formatRate(tariff.unitRate, tariff.currency, tariff.billingUnit)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Standing charge ${formatRate(tariff.standingChargePerDay, tariff.currency, "day")}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (type == UtilityType.GAS) {
                Text(
                    text = if (tariff.calorificValue != null && tariff.volumeCorrection != null) {
                        "Calorific value ${TariffDraft.formatNumber(tariff.calorificValue)} MJ/m³ · correction ${TariffDraft.formatNumber(tariff.volumeCorrection)}"
                    } else {
                        "Typical calorific value and correction used, so kWh are estimated"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * The tariff form, used both for typing a tariff in and for reviewing one read from a bill.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TariffForm(
    draft: TariffDraft,
    type: UtilityType,
    onChange: ((TariffDraft) -> TariffDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var pickingDate by remember { mutableStateOf(false) }
    val currency = draft.currency.trim().uppercase().ifBlank { "currency" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SnaptricSpacing.md),
        verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.md)
    ) {
        if (draft.scanned && draft.id == 0L) {
            SnaptricCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    text = "Check each value against your bill. Fields marked \"Read from bill\" were found automatically; " +
                        "fill in anything that's missing. Nothing is saved until you tap Save.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(SnaptricSpacing.md)
                )
            }
        }

        OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Applies from ${draft.effectiveFrom.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}")
        }

        Text("Charged per", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(SnaptricSpacing.sm)) {
            billingUnitsFor(type).forEach { unit ->
                FilterChip(
                    selected = draft.billingUnit == unit.symbol,
                    onClick = { onChange { it.copy(billingUnit = unit.symbol) } },
                    label = { Text(unit.symbol) }
                )
            }
        }

        NumberField(
            value = draft.unitRate,
            onValueChange = { v -> onChange { it.copy(unitRate = v) } },
            label = "Unit rate ($currency per ${draft.billingUnit})",
            hint = draft.unitRateValue?.let { "= ${formatRate(it, draft.currency, draft.billingUnit)}" }
                ?: "In ${currency}, e.g. 0.2450 for 24.50p",
            field = TariffField.UNIT_RATE,
            draft = draft,
            isError = draft.unitRate.isNotBlank() && draft.unitRateValue == null
        )
        NumberField(
            value = draft.standingCharge,
            onValueChange = { v -> onChange { it.copy(standingCharge = v) } },
            label = "Standing charge ($currency per day)",
            hint = draft.standingChargeValue?.let { "= ${formatRate(it, draft.currency, "day")}" } ?: "Enter 0 if there isn't one",
            field = TariffField.STANDING_CHARGE,
            draft = draft,
            isError = draft.standingCharge.isNotBlank() && draft.standingChargeValue == null
        )
        OutlinedTextField(
            value = draft.currency,
            onValueChange = { v -> onChange { it.copy(currency = v.take(3).uppercase()) } },
            label = { Text("Currency") },
            supportingText = { Text(reviewNote(draft, TariffField.CURRENCY) ?: "Three-letter code, e.g. GBP or EUR") },
            isError = !draft.currencyValid,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (type == UtilityType.GAS) {
            Text(
                text = "Gas bills convert m³ to kWh with a calorific value that changes every bill. " +
                    "Leave these blank to use typical values; costs will then be marked as estimated.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            NumberField(
                value = draft.calorificValue,
                onValueChange = { v -> onChange { it.copy(calorificValue = v) } },
                label = "Calorific value (MJ/m³)",
                hint = "Typical: ${UnitConversions.DEFAULT_CALORIFIC_VALUE}",
                field = TariffField.CALORIFIC_VALUE,
                draft = draft,
                isError = !draft.calorificValueValid,
                flagMissing = false
            )
            NumberField(
                value = draft.volumeCorrection,
                onValueChange = { v -> onChange { it.copy(volumeCorrection = v) } },
                label = "Volume correction factor",
                hint = "Typical: ${UnitConversions.DEFAULT_VOLUME_CORRECTION}",
                field = TariffField.VOLUME_CORRECTION,
                draft = draft,
                isError = !draft.volumeCorrectionValid,
                flagMissing = false
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SnaptricSpacing.sm, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onDelete != null) {
                TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
            TextButton(onClick = onCancel) { Text("Cancel") }
            Button(onClick = onSave, enabled = draft.isValid) { Text("Save tariff") }
        }
    }

    if (pickingDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = draft.effectiveFrom.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onChange { it.copy(effectiveFrom = date) }
                    }
                    pickingDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }
}

/** "Read from bill – check it" for scanned fields, "Not found on the bill" for missing ones. */
private fun reviewNote(draft: TariffDraft, field: TariffField, flagMissing: Boolean = true): String? = when {
    !draft.scanned || draft.id != 0L -> null
    field in draft.fromBill -> "Read from bill – check it"
    flagMissing -> "Not found on the bill – please enter it"
    else -> null
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    hint: String,
    field: TariffField,
    draft: TariffDraft,
    isError: Boolean,
    flagMissing: Boolean = true
) {
    val note = reviewNote(draft, field, flagMissing)
    OutlinedTextField(
        value = value,
        onValueChange = { input -> if (input.all { it.isDigit() || it == '.' || it == ',' }) onValueChange(input) },
        label = { Text(label) },
        supportingText = {
            Text(
                text = listOfNotNull(note, hint).joinToString(" · "),
                color = if (note != null && field in draft.fromBill) MaterialTheme.colorScheme.primary else Color.Unspecified
            )
        },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

/**
 * Full-screen CameraX view for photographing a bill. Falls back to the photo picker when the
 * camera can't be used.
 */
@Composable
private fun BillCamera(
    onCaptured: (Bitmap) -> Unit,
    onUseGallery: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var denied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        denied = !granted
    }
    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    if (!hasPermission) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(SnaptricSpacing.lg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.md)) {
                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = if (denied) "Camera access is off. You can pick a photo of your bill instead."
                    else "Allow camera access to photograph your bill.",
                    textAlign = TextAlign.Center
                )
                Button(onClick = onUseGallery) { Text("Choose a photo") }
                TextButton(onClick = onClose) { Text("Cancel") }
            }
        }
        return
    }

    val previewView = remember { PreviewView(context) }
    // Sharp enough for small print, without the memory cost of a full-sensor photo.
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(ResolutionStrategy(Size(2560, 1920), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER))
                    .build()
            )
            .build()
    }
    var capturing by remember { mutableStateOf(false) }

    // The camera is bound to this screen's lifecycle, which outlives the camera view, so release
    // it explicitly when the view closes.
    DisposableEffect(Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            if (providerFuture.isDone) providerFuture.get().unbindAll()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart).padding(SnaptricSpacing.md)) {
            Icon(Icons.Default.Close, contentDescription = "Close camera", tint = Color.White)
        }
        Text(
            text = "Fit the unit rate and standing charge in view",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp)
        )
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = SnaptricSpacing.xl),
            horizontalArrangement = Arrangement.spacedBy(SnaptricSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onUseGallery) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "Choose a photo instead", tint = Color.White)
            }
            FilledIconButton(
                onClick = {
                    if (capturing) return@FilledIconButton
                    capturing = true
                    imageCapture.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bitmap = image.toUprightBitmap()
                                image.close()
                                capturing = false
                                onCaptured(bitmap)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                capturing = false
                            }
                        }
                    )
                },
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                if (capturing) CircularProgressIndicator(modifier = Modifier.size(28.dp))
                else Icon(Icons.Default.CameraAlt, contentDescription = "Photograph bill")
            }
        }
    }
}

private fun ImageProxy.toUprightBitmap(): Bitmap {
    val bitmap = toBitmap()
    val degrees = imageInfo.rotationDegrees
    if (degrees == 0) return bitmap
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private val previewMeter = UtilityEntity(id = 1, propertyId = 1, type = UtilityType.GAS, unit = "m³", initialReading = 0.0)

@ComposePreview(showBackground = true)
@Composable
private fun TariffListPreview() {
    SnaptricTheme {
        TariffContent(
            utility = previewMeter,
            tariffs = listOf(
                TariffEntity(id = 1, utilityId = 1, unitRate = 0.0624, billingUnit = "kWh", standingChargePerDay = 0.3143,
                    currency = "GBP", effectiveFrom = 0, calorificValue = 39.2, volumeCorrection = 1.02264, source = "BillScan")
            ),
            draft = null,
            scanState = BillScanState.Idle,
            onBack = {}
        )
    }
}

@ComposePreview(showBackground = true)
@Composable
private fun TariffReviewPreview() {
    SnaptricTheme {
        TariffContent(
            utility = previewMeter,
            tariffs = emptyList(),
            draft = TariffDraft(
                unitRate = "0.0624", billingUnit = "kWh", standingCharge = "", currency = "GBP",
                effectiveFrom = LocalDate.of(2026, 10, 1), calorificValue = "39.2",
                fromBill = setOf(TariffField.UNIT_RATE, TariffField.CALORIFIC_VALUE), scanned = true
            ),
            scanState = BillScanState.Idle,
            onBack = {}
        )
    }
}
