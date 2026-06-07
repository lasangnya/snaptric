package com.snaptric.feature.properties.ui

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.designsystem.components.SnaptricBadge
import com.snaptric.core.designsystem.components.SnaptricCard
import com.snaptric.core.designsystem.components.SnaptricEmptyState
import com.snaptric.core.designsystem.theme.SnaptricSpacing
import com.snaptric.core.designsystem.theme.SnaptricTheme
import com.snaptric.feature.properties.viewmodel.UtilityViewModel
import java.util.Date
import java.util.Locale

@Composable
fun UtilityScreen(
    viewModel: UtilityViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val readings by viewModel.readings.collectAsState()
    UtilityDetailContent(
        readings = readings,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilityDetailContent(
    readings: List<ReadingEntity>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
                    description = "Use the camera to capture your first meter reading."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(SnaptricSpacing.md),
                verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.sm)
            ) {
                item {
                    ReadingBarChart(
                        readings = readings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = SnaptricSpacing.sm)
                    )
                }
                items(readings, key = { it.id }) { reading ->
                    ReadingHistoryItem(
                        reading = reading,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

/**
 * Groups readings by month (YYYY-MM), calculates consumption per month
 * (max - min reading within the month), and renders a bar chart of the
 * last 6 months. Falls back to raw reading value when only 1 reading
 * exists in a month.
 */
@Composable
fun ReadingBarChart(
    readings: List<ReadingEntity>,
    modifier: Modifier = Modifier
) {
    val monthlyUsage = remember(readings) {
        val cal = java.util.Calendar.getInstance()

        readings
            .groupBy { reading ->
                cal.timeInMillis = reading.timestamp
                "${cal.get(java.util.Calendar.YEAR)}-${String.format("%02d", cal.get(java.util.Calendar.MONTH) + 1)}"
            }
            .mapValues { (_, monthReadings) ->
                val values = monthReadings.map { it.value }
                // Consumption = max - min if 2+ readings, else the reading itself
                if (values.size >= 2) values.max() - values.min() else values.first()
            }
            .toList()
            .sortedBy { it.first }
            .takeLast(6)
    }

    if (monthlyUsage.size < 2) {
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxValue = monthlyUsage.maxOf { it.second }
    val minValue = monthlyUsage.minOf { it.second }
    val valueRange = if (maxValue == minValue) 1.0 else maxValue - minValue

    val monthLabelFormat = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
    val cal = remember { java.util.Calendar.getInstance() }
    val textMeasurer = rememberTextMeasurer()

    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Box(
        modifier = modifier
            .height(180.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(SnaptricSpacing.md)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val chartPaddingLeft = 48.dp.toPx()
            val chartPaddingBottom = 28.dp.toPx()
            val chartPaddingTop = 8.dp.toPx()
            val chartWidth = size.width - chartPaddingLeft
            val chartHeight = size.height - chartPaddingBottom - chartPaddingTop

            val barCount = monthlyUsage.size
            val barSpacing = 10.dp.toPx()
            val totalBarSpacing = barSpacing * (barCount - 1)
            val barWidth = (chartWidth - totalBarSpacing) / barCount

            // Draw Y-axis labels and grid lines
            val ySteps = 3
            for (i in 0..ySteps) {
                val fraction = i / ySteps.toFloat()
                val value = minValue + (valueRange * (1 - fraction))
                val y = chartPaddingTop + (chartHeight * fraction)
                val label = if (value == value.toInt().toDouble()) {
                    value.toInt().toString()
                } else {
                    String.format(Locale.getDefault(), "%.1f", value)
                }
                val textLayoutResult = textMeasurer.measure(
                    text = label,
                    style = labelStyle
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x = chartPaddingLeft - textLayoutResult.size.width.toFloat() - 4.dp.toPx(),
                        y = y - textLayoutResult.size.height.toFloat() / 2
                    )
                )
                if (i > 0) {
                    drawLine(
                        color = outlineColor,
                        start = Offset(chartPaddingLeft, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // Draw bars and X-axis month labels
            monthlyUsage.forEachIndexed { index, (monthKey, usage) ->
                val barHeightFraction = if (maxValue == minValue) {
                    0.5
                } else {
                    (usage - minValue) / valueRange
                }
                val barHeight = (barHeightFraction * chartHeight).toFloat()
                val x = chartPaddingLeft + index * (barWidth + barSpacing)
                val y = chartPaddingTop + chartHeight - barHeight

                drawRect(
                    color = primaryColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight)
                )

                // Parse YYYY-MM to get a Date for formatting
                val parts = monthKey.split("-")
                cal.set(java.util.Calendar.YEAR, parts[0].toInt())
                cal.set(java.util.Calendar.MONTH, parts[1].toInt() - 1)
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                val monthLabel = monthLabelFormat.format(cal.time)
                val textLayoutResult = textMeasurer.measure(
                    text = monthLabel,
                    style = labelStyle
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x = x + barWidth / 2 - textLayoutResult.size.width.toFloat() / 2,
                        y = size.height - chartPaddingBottom + 4.dp.toPx()
                    )
                )
            }
        }
    }
}

@Composable
fun ReadingHistoryItem(
    reading: ReadingEntity,
    modifier: Modifier = Modifier
) {
    val date = remember(reading.timestamp) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(reading.timestamp))
    }

    val (badgeContainerColor, badgeContentColor) = when (reading.source) {
        "MLKit" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "Manual" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        "Gemma" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    SnaptricCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
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
                    text = "${reading.value}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            SnaptricBadge(
                text = reading.source,
                containerColor = badgeContainerColor,
                contentColor = badgeContentColor
            )
        }
    }
}

@Preview
@Composable
private fun UtilityDetailContentPreview() {
    SnaptricTheme {
        UtilityDetailContent(
            readings = listOf(
                ReadingEntity(
                    id = 1,
                    utilityId = 1,
                    value = 1234.0,
                    timestamp = System.currentTimeMillis(),
                    source = "MLKit"
                ),
                ReadingEntity(
                    id = 2,
                    utilityId = 1,
                    value = 1250.0,
                    timestamp = System.currentTimeMillis() - 86400000,
                    source = "Manual"
                ),
                ReadingEntity(
                    id = 3,
                    utilityId = 1,
                    value = 1265.0,
                    timestamp = System.currentTimeMillis() - 172800000,
                    source = "Gemma"
                ),
                ReadingEntity(
                    id = 4,
                    utilityId = 1,
                    value = 1280.0,
                    timestamp = System.currentTimeMillis() - 259200000,
                    source = "MLKit"
                ),
                ReadingEntity(
                    id = 5,
                    utilityId = 1,
                    value = 1295.0,
                    timestamp = System.currentTimeMillis() - 345600000,
                    source = "Manual"
                ),
                ReadingEntity(
                    id = 6,
                    utilityId = 1,
                    value = 1310.0,
                    timestamp = System.currentTimeMillis() - 432000000,
                    source = "MLKit"
                ),
                ReadingEntity(
                    id = 7,
                    utilityId = 1,
                    value = 1325.0,
                    timestamp = System.currentTimeMillis() - 518400000,
                    source = "Gemma"
                )
            ),
            onBack = {}
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

@Preview
@Composable
private fun UtilityDetailContentSingleReadingPreview() {
    SnaptricTheme {
        UtilityDetailContent(
            readings = listOf(
                ReadingEntity(
                    id = 1,
                    utilityId = 1,
                    value = 1234.0,
                    timestamp = System.currentTimeMillis(),
                    source = "MLKit"
                )
            ),
            onBack = {}
        )
    }
}
