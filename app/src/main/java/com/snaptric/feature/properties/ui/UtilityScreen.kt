package com.snaptric.feature.properties.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.database.entity.TariffEntity
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import com.snaptric.core.designsystem.components.SnaptricCard
import com.snaptric.core.designsystem.components.SnaptricEmptyState
import com.snaptric.core.designsystem.components.UtilityIconBadge
import com.snaptric.core.designsystem.components.accentColor
import com.snaptric.core.designsystem.components.formatMeterValue
import com.snaptric.core.designsystem.components.label
import com.snaptric.core.designsystem.theme.SnaptricSpacing
import com.snaptric.core.designsystem.theme.SnaptricTheme
import com.snaptric.core.designsystem.theme.tabularNumbers
import com.snaptric.core.designsystem.components.SnaptricBadge
import com.snaptric.core.domain.insights.MeterRecap
import com.snaptric.core.domain.insights.formatAmount
import com.snaptric.core.domain.insights.monthlyUsage
import com.snaptric.core.domain.tariff.activeAt
import com.snaptric.core.domain.tariff.formatMoney
import com.snaptric.core.domain.tariff.formatRate
import com.snaptric.core.domain.tariff.gasFactors
import com.snaptric.core.domain.units.GasFactors
import com.snaptric.core.domain.units.MeterUnit
import com.snaptric.core.domain.units.dualUsage
import com.snaptric.core.domain.units.formatQuantity
import com.snaptric.feature.properties.viewmodel.UtilityViewModel
import java.text.SimpleDateFormat
import java.time.format.TextStyle
import java.util.Date

/**
 * Entry point for the Utility detail screen.
 * Connects the [UtilityViewModel] to the [UtilityDetailContent] composable.
 */
@Composable
fun UtilityScreen(
    viewModel: UtilityViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onScan: () -> Unit = {},
    onTariffs: (Long) -> Unit = {}
) {
    val readings by viewModel.readings.collectAsState()
    val utility by viewModel.utility.collectAsState()
    val tariffs by viewModel.tariffs.collectAsState()
    val thisMonth by viewModel.thisMonth.collectAsState()
    UtilityDetailContent(
        readings = readings,
        onBack = onBack,
        utility = utility,
        currentTariff = tariffs.activeAt(System.currentTimeMillis()),
        thisMonth = thisMonth,
        onTariffs = { utility?.let { onTariffs(it.id) } },
        onAddReading = viewModel::addManualReading,
        onEditReading = viewModel::updateReading,
        onDeleteReading = viewModel::deleteReading,
        onEditUtility = viewModel::updateUtility,
        onDeleteUtility = {
            viewModel.deleteUtility()
            onBack()
        },
        onScan = onScan
    )
}

/** Which dialog, if any, the meter screen is showing. */
private sealed interface UtilityDialog {
    data object AddReading : UtilityDialog
    data class EditReading(val reading: ReadingEntity) : UtilityDialog
    data object EditMeter : UtilityDialog
    data object DeleteMeter : UtilityDialog
}

/**
 * UI content for the Utility detail screen.
 * Shows monthly usage, the reading history, and lets the user add, correct or delete readings
 * and edit or delete the meter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilityDetailContent(
    readings: List<ReadingEntity>,
    onBack: () -> Unit,
    utility: UtilityEntity? = null,
    onAddReading: (Double) -> Unit = {},
    onEditReading: (ReadingEntity, Double) -> Unit = { _, _ -> },
    onDeleteReading: (ReadingEntity) -> Unit = {},
    onEditUtility: (UtilityEntity) -> Unit = {},
    onDeleteUtility: () -> Unit = {},
    onScan: () -> Unit = {},
    currentTariff: TariffEntity? = null,
    thisMonth: MeterRecap? = null,
    onTariffs: () -> Unit = {}
) {
    var dialog by remember { mutableStateOf<UtilityDialog?>(null) }
    val unit = utility?.unit.orEmpty()
    val gasFactors = currentTariff?.gasFactors ?: GasFactors()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(utility?.let { it.name ?: it.type.label } ?: "Reading History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onScan) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Scan this meter")
                    }
                    if (utility != null) {
                        IconButton(onClick = onTariffs) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Tariff")
                        }
                        IconButton(onClick = { dialog = UtilityDialog.EditMeter }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit meter")
                        }
                        IconButton(onClick = { dialog = UtilityDialog.DeleteMeter }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete meter")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (readings.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { dialog = UtilityDialog.AddReading },
                    icon = { Icon(Icons.Default.Keyboard, contentDescription = null) },
                    text = { Text("Type a reading") }
                )
            }
        }
    ) { padding ->
        if (readings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                SnaptricEmptyState(
                    icon = Icons.Default.History,
                    title = "No Readings Yet",
                    description = "Use the camera to capture your first meter reading.",
                    actionLabel = "Type a reading instead",
                    onAction = { dialog = UtilityDialog.AddReading }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = SnaptricSpacing.md,
                    end = SnaptricSpacing.md,
                    top = SnaptricSpacing.md,
                    bottom = 88.dp // clear the floating button
                ),
                verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.sm)
            ) {
                if (utility != null) {
                    item {
                        ThisMonthCard(
                            utility = utility,
                            thisMonth = thisMonth,
                            tariff = currentTariff,
                            gasFactors = gasFactors,
                            onTariffs = onTariffs
                        )
                    }
                }

                item {
                    ReadingBarChart(
                        readings = readings,
                        type = utility?.type,
                        unit = unit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = SnaptricSpacing.sm)
                    )
                }

                itemsIndexed(readings, key = { _, it -> it.id }) { index, reading ->
                    val gap = readings.getOrNull(index + 1)?.let { reading.value - it.value }
                    ReadingHistoryItem(
                        reading = reading,
                        gap = gap,
                        unit = unit,
                        type = utility?.type,
                        gasFactors = gasFactors,
                        onClick = { dialog = UtilityDialog.EditReading(reading) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    when (val current = dialog) {
        UtilityDialog.AddReading -> ReadingValueDialog(
            title = "Type a reading",
            initialValue = "",
            unit = unit,
            onDismiss = { dialog = null },
            onSave = { value ->
                onAddReading(value)
                dialog = null
            }
        )
        is UtilityDialog.EditReading -> ReadingValueDialog(
            title = "Correct reading",
            initialValue = formatMeterValue(current.reading.value),
            unit = unit,
            onDismiss = { dialog = null },
            onSave = { value ->
                onEditReading(current.reading, value)
                dialog = null
            },
            onDelete = {
                onDeleteReading(current.reading)
                dialog = null
            }
        )
        UtilityDialog.EditMeter -> if (utility != null) {
            EditMeterDialog(
                utility = utility,
                onDismiss = { dialog = null },
                onSave = { updated ->
                    onEditUtility(updated)
                    dialog = null
                }
            )
        }
        UtilityDialog.DeleteMeter -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Delete this meter?") },
            text = { Text("This removes the meter and all ${readings.size} of its readings. It can't be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        dialog = null
                        onDeleteUtility()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Delete meter") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } }
        )
        null -> Unit
    }
}

/**
 * Enter or correct a reading's value. Shows a Delete action when editing an existing reading.
 */
@Composable
private fun ReadingValueDialog(
    title: String,
    initialValue: String,
    unit: String,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var text by remember { mutableStateOf(initialValue) }
    val value = text.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    val normalized = input.replace(',', '.')
                    if (normalized.all { it.isDigit() || it == '.' } && normalized.count { it == '.' } <= 1) {
                        text = normalized
                    }
                },
                label = { Text("Meter reading") },
                suffix = if (unit.isNotBlank()) ({ Text(unit) }) else null,
                textStyle = MaterialTheme.typography.headlineSmall.tabularNumbers,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { value?.let(onSave) }, enabled = value != null) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

/**
 * Edit a meter's name, unit and starting value.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditMeterDialog(
    utility: UtilityEntity,
    onDismiss: () -> Unit,
    onSave: (UtilityEntity) -> Unit
) {
    var name by remember { mutableStateOf(utility.name.orEmpty()) }
    var unit by remember { mutableStateOf(utility.unit) }
    var initial by remember { mutableStateOf(formatMeterValue(utility.initialReading)) }
    val initialValue = initial.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit meter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.sm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Meter name (optional)") },
                    placeholder = { Text(utility.type.label) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Unit on the meter's dial",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Readings are kept as read; changing the unit changes how they're labelled and converted.
                val options = MeterUnit.optionsFor(utility.type).map { it.symbol }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(SnaptricSpacing.sm)) {
                    (options + listOf(utility.unit).filter { it.isNotBlank() && it !in options }).forEach { option ->
                        FilterChip(selected = unit == option, onClick = { unit = option }, label = { Text(option) })
                    }
                }
                OutlinedTextField(
                    value = initial,
                    onValueChange = { initial = it.replace(',', '.') },
                    label = { Text("Initial reading") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        utility.copy(
                            name = name.trim().ifBlank { null },
                            unit = unit.trim(),
                            initialReading = initialValue ?: utility.initialReading
                        )
                    )
                },
                enabled = unit.isNotBlank() && initialValue != null
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * This month's usage in the meter's unit and in kWh where they differ, what it costs under the
 * current tariff, and a way to add or change the tariff.
 */
@Composable
private fun ThisMonthCard(
    utility: UtilityEntity,
    thisMonth: MeterRecap?,
    tariff: TariffEntity?,
    gasFactors: GasFactors,
    onTariffs: () -> Unit
) {
    SnaptricCard {
        Column(
            modifier = Modifier.padding(SnaptricSpacing.md),
            verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.xs)
        ) {
            Text("This month", style = MaterialTheme.typography.titleSmall)
            if (thisMonth == null) {
                Text(
                    text = "No usage recorded this month yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val usage = dualUsage(thisMonth.usedSoFar, utility.unit, utility.type, gasFactors)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SnaptricSpacing.sm)) {
                    Text(usage.text(), style = MaterialTheme.typography.headlineSmall.tabularNumbers)
                    if (usage.estimated) EstimatedBadge()
                }
                thisMonth.cost?.let { cost ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SnaptricSpacing.sm)) {
                        Text(
                            text = "≈ ${formatMoney(cost.total, cost.currency)} so far, incl. standing charge",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (cost.estimated && !usage.estimated) EstimatedBadge()
                    }
                }
                thisMonth.forecastCost?.let { forecast ->
                    Text(
                        text = "On track for about ${formatMoney(forecast.total, forecast.currency)} this month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (tariff != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${formatRate(tariff.unitRate, tariff.currency, tariff.billingUnit)} + " +
                            formatRate(tariff.standingChargePerDay, tariff.currency, "day"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onTariffs) { Text("Tariff") }
                }
            } else {
                TextButton(onClick = onTariffs, contentPadding = PaddingValues(0.dp)) {
                    Text("Add your tariff to see costs")
                }
            }
        }
    }
}

/** Marks a figure worked out with a typical calorific value rather than the one on the bill. */
@Composable
private fun EstimatedBadge() {
    SnaptricBadge(
        text = "estimated",
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    )
}

/**
 * Usage per month for the last six months, drawn to scale from zero in the meter's colour.
 * Usage between two readings counts toward the month of the later reading.
 */
@Composable
fun ReadingBarChart(
    readings: List<ReadingEntity>,
    modifier: Modifier = Modifier,
    type: UtilityType? = null,
    unit: String = ""
) {
    val months = remember(readings) { monthlyUsage(readings).toList().takeLast(6) }

    if (months.size < 2) {
        Box(
            modifier = modifier
                .height(180.dp)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Capture readings across multiple months to see trends",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(SnaptricSpacing.md)
            )
        }
        return
    }

    val maxValue = months.maxOf { it.second }.takeIf { it > 0 } ?: 1.0
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val barColor = type?.accentColor ?: MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val locale = LocalConfiguration.current.locales[0]

    SnaptricCard(modifier = modifier) {
        Column(modifier = Modifier.padding(SnaptricSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                type?.let {
                    UtilityIconBadge(type = it, size = 28.dp)
                    Spacer(modifier = Modifier.width(SnaptricSpacing.sm))
                }
                Text(
                    text = if (unit.isBlank()) "Monthly usage" else "Monthly usage ($unit)",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(modifier = Modifier.height(SnaptricSpacing.sm))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val bottomLabel = 20.dp.toPx()
                val topLabel = 16.dp.toPx()
                val chartHeight = size.height - bottomLabel - topLabel
                val slot = size.width / months.size
                val barWidth = slot * 0.56f

                // Baseline at zero, so bar heights compare honestly.
                drawLine(
                    color = gridColor,
                    start = Offset(0f, topLabel + chartHeight),
                    end = Offset(size.width, topLabel + chartHeight),
                    strokeWidth = 1.dp.toPx()
                )

                months.forEachIndexed { index, (month, usage) ->
                    val barHeight = (usage / maxValue * chartHeight).toFloat()
                    val x = index * slot + (slot - barWidth) / 2
                    val y = topLabel + chartHeight - barHeight
                    val isCurrent = index == months.lastIndex
                    drawRoundRect(
                        color = if (isCurrent) barColor else barColor.copy(alpha = 0.55f),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )

                    val valueLayout = textMeasurer.measure(formatAmount(usage), labelStyle)
                    drawText(
                        textLayoutResult = valueLayout,
                        topLeft = Offset(
                            x + barWidth / 2 - valueLayout.size.width / 2f,
                            (y - valueLayout.size.height - 2.dp.toPx()).coerceAtLeast(0f)
                        )
                    )

                    val monthLayout = textMeasurer.measure(month.month.getDisplayName(TextStyle.SHORT, locale), labelStyle)
                    drawText(
                        textLayoutResult = monthLayout,
                        topLeft = Offset(
                            x + barWidth / 2 - monthLayout.size.width / 2f,
                            size.height - bottomLabel + 4.dp.toPx()
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ReadingHistoryItem(
    reading: ReadingEntity,
    gap: Double?,
    modifier: Modifier = Modifier,
    unit: String = "",
    type: UtilityType? = null,
    gasFactors: GasFactors = GasFactors(),
    onClick: (() -> Unit)? = null
) {
    val locale = LocalConfiguration.current.locales[0]
    val date = remember(reading.timestamp, locale) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", locale).format(Date(reading.timestamp))
    }

    SnaptricCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SnaptricSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(SnaptricSpacing.xs))
                Text(
                    text = formatMeterValue(reading.value),
                    style = MaterialTheme.typography.headlineSmall.tabularNumbers,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (reading.source == "Manual") {
                    Text(
                        text = "Typed in",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (gap != null) {
                val energy = type?.let { dualUsage(kotlin.math.abs(gap), unit, it, gasFactors).energyKwh }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = (if (gap >= 0) "+" else "−") + formatMeterValue(kotlin.math.abs(gap)) +
                            if (unit.isBlank()) "" else " $unit",
                        style = MaterialTheme.typography.labelMedium.tabularNumbers,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (energy != null) {
                        Text(
                            text = "≈ ${formatQuantity(energy)} kWh",
                            style = MaterialTheme.typography.labelSmall.tabularNumbers,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private val previewReadings = (0 until 7).map { i ->
    ReadingEntity(
        id = i.toLong() + 1,
        utilityId = 1,
        value = 1325.0 - i * 40,
        timestamp = System.currentTimeMillis() - i * 30L * 86_400_000,
        source = if (i % 3 == 0) "Manual" else "MLKit"
    )
}

@Preview
@Composable
private fun UtilityDetailContentPreview() {
    SnaptricTheme {
        UtilityDetailContent(
            readings = previewReadings,
            onBack = {},
            utility = UtilityEntity(id = 1, propertyId = 1, type = UtilityType.ELECTRICITY, unit = "kWh", initialReading = 0.0)
        )
    }
}

@Preview
@Composable
private fun UtilityDetailContentEmptyPreview() {
    SnaptricTheme {
        UtilityDetailContent(
            readings = emptyList(),
            onBack = {}
        )
    }
}
