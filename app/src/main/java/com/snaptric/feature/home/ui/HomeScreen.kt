package com.snaptric.feature.home.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import com.snaptric.core.designsystem.components.SnaptricCard
import com.snaptric.core.designsystem.components.SnaptricSectionHeader
import com.snaptric.core.designsystem.components.UtilityIconBadge
import com.snaptric.core.designsystem.components.accentColor
import com.snaptric.core.designsystem.components.formatMeterValue
import com.snaptric.core.designsystem.components.label
import com.snaptric.core.designsystem.theme.SnaptricSpacing
import com.snaptric.core.designsystem.theme.SnaptricTheme
import com.snaptric.core.designsystem.theme.tabularNumbers
import com.snaptric.core.domain.insights.Insight
import com.snaptric.feature.home.viewmodel.HomeReadingItem
import com.snaptric.feature.home.viewmodel.HomeViewModel
import com.snaptric.feature.home.viewmodel.buildHomeReadingItems
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The entry point for the Home screen.
 * Connects the [HomeViewModel] to the [HomeContent] composable.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onScanClick: () -> Unit = {}
) {
    val properties by viewModel.properties.collectAsState()
    val readingItems by viewModel.readingItems.collectAsState()
    val insight by viewModel.insight.collectAsState()

    HomeContent(
        properties = properties,
        readingItems = readingItems,
        insight = insight,
        onScanClick = onScanClick
    )
}

/**
 * The main UI content of the Home screen.
 * Leads with the latest reading as a hero card, followed by quick stats and recent activity.
 */
@Composable
fun HomeContent(
    properties: List<PropertyEntity>,
    readingItems: List<HomeReadingItem>,
    modifier: Modifier = Modifier,
    insight: Insight? = null,
    onScanClick: () -> Unit = {}
) {
    // Determine the appropriate greeting based on the current time of day.
    val greeting = remember { getGreeting() }
    val latest = readingItems.firstOrNull()
    // The hero card already shows the newest reading, so the list starts from the one after it.
    val recentItems = readingItems.drop(1).take(5)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(SnaptricSpacing.md),
        verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.md)
    ) {
        item {
            EntranceAnimation(delayMillis = 0) {
                Column(modifier = Modifier.padding(top = SnaptricSpacing.sm)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Snaptric",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        item {
            EntranceAnimation(delayMillis = 80) {
                if (latest != null) {
                    LatestReadingHero(item = latest, onScanClick = onScanClick)
                } else {
                    EmptyHero(onScanClick = onScanClick)
                }
            }
        }

        if (insight != null) {
            item {
                EntranceAnimation(delayMillis = 120) {
                    InsightCard(insight = insight)
                }
            }
        }

        item {
            EntranceAnimation(delayMillis = 160) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SnaptricSpacing.md)
                ) {
                    QuickStat(
                        value = properties.size.toString(),
                        label = "Properties",
                        modifier = Modifier.weight(1f)
                    )
                    QuickStat(
                        value = readingItems.size.toString(),
                        label = "Readings",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (recentItems.isNotEmpty()) {
            item {
                SnaptricSectionHeader(title = "Recent Activity")
            }
            items(recentItems, key = { it.reading.id }) { item ->
                RecentReadingRow(item = item)
            }
        }
    }
}

/**
 * Fades and slides content in once when it first appears.
 */
@Composable
private fun EntranceAnimation(delayMillis: Int, content: @Composable () -> Unit) {
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(400, delayMillis = delayMillis)) +
            slideInVertically(tween(400, delayMillis = delayMillis)) { it / 6 }
    ) {
        content()
    }
}

/**
 * The hero card: the newest reading, large and easy to scan, with its meter and the change since last time.
 */
@Composable
private fun LatestReadingHero(item: HomeReadingItem, onScanClick: () -> Unit) {
    val type = item.utility?.type
    val accent = type?.accentColor ?: MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.14f), accent.copy(alpha = 0.02f))
                    )
                )
                .padding(SnaptricSpacing.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (type != null) {
                    UtilityIconBadge(type = type, size = 36.dp)
                    Spacer(modifier = Modifier.width(SnaptricSpacing.sm + SnaptricSpacing.xs))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Latest reading",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.meterTitle(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(SnaptricSpacing.lg))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatMeterValue(item.reading.value),
                    style = MaterialTheme.typography.displaySmall.tabularNumbers.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 44.sp,
                        lineHeight = 48.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                item.utility?.unit?.let { unit ->
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = SnaptricSpacing.sm, bottom = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(SnaptricSpacing.sm))

            Text(
                text = heroSubtitle(item),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(SnaptricSpacing.lg))

            Button(
                onClick = onScanClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.padding(end = SnaptricSpacing.sm))
                Text("Scan a meter")
            }
        }
    }
}

/**
 * Shown in place of the hero card before any reading exists, with a clear first step.
 */
@Composable
private fun EmptyHero(onScanClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(SnaptricSpacing.lg)) {
            Text(
                text = "No readings yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(SnaptricSpacing.xs))
            Text(
                text = "Take a photo of your meter to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(SnaptricSpacing.lg))
            Button(
                onClick = onScanClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.padding(end = SnaptricSpacing.sm))
                Text("Scan your first meter")
            }
        }
    }
}

/**
 * This month's usage summary. Labelled when an on-device model wrote it, so it's clear where the words came from.
 */
@Composable
private fun InsightCard(insight: Insight) {
    SnaptricCard {
        Column(
            modifier = Modifier.padding(SnaptricSpacing.md),
            verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.xs)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(SnaptricSpacing.xs + 2.dp))
                Text(
                    text = "This month",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = insight.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (insight.writtenOnDevice) {
                Text(
                    text = "Written on your phone by Gemma",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * A compact stat tile where the value, not the label, carries the emphasis.
 */
@Composable
private fun QuickStat(value: String, label: String, modifier: Modifier = Modifier) {
    SnaptricCard(modifier = modifier) {
        Column(modifier = Modifier.padding(SnaptricSpacing.md)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.tabularNumbers,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * One reading in Recent Activity: which meter it belongs to, when, its value and its change.
 */
@Composable
private fun RecentReadingRow(item: HomeReadingItem) {
    SnaptricCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SnaptricSpacing.md, vertical = SnaptricSpacing.sm + SnaptricSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val type = item.utility?.type
            if (type != null) {
                UtilityIconBadge(type = type)
            } else {
                Spacer(
                    modifier = Modifier
                        .width(40.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            Spacer(modifier = Modifier.width(SnaptricSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.meterTitle(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatTimestamp(item.reading.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatMeterValue(item.reading.value),
                    style = MaterialTheme.typography.titleMedium.tabularNumbers,
                    color = MaterialTheme.colorScheme.onSurface
                )
                item.delta?.let { delta ->
                    Text(
                        text = formatDelta(delta, item.utility?.unit),
                        style = MaterialTheme.typography.labelMedium.tabularNumbers,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * "Electricity · Home", or the meter's custom name when it has one.
 */
private fun HomeReadingItem.meterTitle(): String {
    val meterName = utility?.name?.takeIf { it.isNotBlank() } ?: utility?.type?.label ?: "Meter"
    return listOfNotNull(meterName, propertyName).joinToString(" · ")
}

private fun heroSubtitle(item: HomeReadingItem): String {
    val time = formatTimestamp(item.reading.timestamp)
    return item.delta?.let { "${formatDelta(it, item.utility?.unit)} since last reading · $time" }
        ?: "First reading for this meter · $time"
}

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDelta(delta: Double, unit: String?): String {
    val sign = if (delta >= 0) "+" else "−"
    return listOfNotNull(sign + formatMeterValue(kotlin.math.abs(delta)), unit).joinToString(" ")
}

private val previewProperties = listOf(
    PropertyEntity(id = 1, name = "Home", address = "123 Main St"),
    PropertyEntity(id = 2, name = "Office", address = "456 Work Ave")
)

private val previewUtilities = listOf(
    UtilityEntity(id = 1, propertyId = 1, type = UtilityType.ELECTRICITY, unit = "kWh", initialReading = 0.0),
    UtilityEntity(id = 2, propertyId = 2, type = UtilityType.WATER, unit = "m³", initialReading = 0.0)
)

private val previewReadings = listOf(
    ReadingEntity(id = 1, utilityId = 1, value = 1250.0, timestamp = System.currentTimeMillis(), source = "MLKit"),
    ReadingEntity(id = 2, utilityId = 1, value = 1234.5, timestamp = System.currentTimeMillis() - 86400000, source = "Manual"),
    ReadingEntity(id = 3, utilityId = 2, value = 987.2, timestamp = System.currentTimeMillis() - 172800000, source = "MLKit")
)

@Preview(showBackground = true)
@Composable
fun HomeContentPreview() {
    SnaptricTheme {
        HomeContent(
            properties = previewProperties,
            readingItems = buildHomeReadingItems(previewReadings, previewUtilities, previewProperties),
            insight = Insight(
                text = "You've used 180 kWh of electricity at Home so far this month, on track for about 290 kWh. That's 6% less than last month.",
                writtenOnDevice = true
            )
        )
    }
}

@Preview(showBackground = true, name = "Home Content Empty")
@Composable
fun HomeContentEmptyPreview() {
    SnaptricTheme {
        HomeContent(
            properties = emptyList(),
            readingItems = emptyList()
        )
    }
}
