package com.snaptric.feature.home.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.snaptric.core.designsystem.components.SnaptricCard
import com.snaptric.core.designsystem.components.SnaptricEmptyState
import com.snaptric.core.designsystem.components.SnaptricSectionHeader
import com.snaptric.core.designsystem.components.SnaptricStatCard
import com.snaptric.core.designsystem.theme.SnaptricSpacing
import com.snaptric.core.designsystem.theme.SnaptricTheme
import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.domain.Reading
import com.snaptric.feature.home.viewmodel.HomeViewModel
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
    viewModel: HomeViewModel = hiltViewModel()
) {
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val latestRead by viewModel.latestRead.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val readings by viewModel.readings.collectAsState()

    HomeContent(
        isAnalyzing = isAnalyzing,
        latestRead = latestRead,
        properties = properties,
        readings = readings
    )
}

/**
 * The main UI content of the Home screen.
 * Displays a welcome message, high-level stats, and a list of recent activities.
 */
@Composable
fun HomeContent(
    isAnalyzing: Boolean,
    latestRead: Reading?,
    properties: List<PropertyEntity>,
    readings: List<ReadingEntity>,
    modifier: Modifier = Modifier
) {
    // Determine the appropriate greeting based on the current time of day.
    val greeting = remember { getGreeting() }
    
    // Show only the 5 most recent readings in the summary list.
    val recentReadings = remember(readings) {
        readings.sortedByDescending { it.timestamp }.take(5)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(SnaptricSpacing.md),
        verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.lg)
    ) {
        // Welcome section with a slide-in animation.
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 4 }
            ) {
                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(SnaptricSpacing.xs))
                    Text(
                        text = "Snaptric",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Stats summary row (Properties, total readings, and the very latest value).
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(500, delayMillis = 100)) +
                    slideInVertically(tween(500, delayMillis = 100)) { it / 4 }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SnaptricSpacing.md)
                ) {
                    SnaptricStatCard(
                        title = "Properties",
                        value = properties.size.toString(),
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Home
                    )
                    SnaptricStatCard(
                        title = "Readings",
                        value = readings.size.toString(),
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.BarChart
                    )
                    SnaptricStatCard(
                        title = "Latest",
                        value = latestRead?.value ?: "--",
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Bolt
                    )
                }
            }
        }

        // Recent Activity list section.
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(500, delayMillis = 200)) +
                    slideInVertically(tween(500, delayMillis = 200)) { it / 4 }
            ) {
                Column {
                    SnaptricSectionHeader(title = "Recent Activity")

                    if (recentReadings.isEmpty()) {
                        SnaptricEmptyState(
                            title = "No readings yet",
                            description = "Take a photo of your meter to get started."
                        )
                    } else {
                        recentReadings.forEachIndexed { index, reading ->
                            // Calculate the difference between this reading and the previous one for a simple consumption hint.
                            val gap = recentReadings.getOrNull(index + 1)?.let { reading.value - it.value }
                            SnaptricCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(SnaptricSpacing.md),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = reading.value.toString(),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = formatTimestamp(reading.timestamp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (gap != null) {
                                        Text(
                                            text = formatGap(gap),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            if (index < recentReadings.size - 1) {
                                Spacer(modifier = Modifier.height(SnaptricSpacing.sm))
                            }
                        }
                    }
                }
            }
        }

        // Status indicator shown when AI is busy in the background.
        if (isAnalyzing) {
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 }
                ) {
                    Text(
                        text = "AI is analyzing your photo...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = SnaptricSpacing.sm)
                    )
                }
            }
        }
    }
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

private fun formatGap(gap: Double): String {
    val formatted = String.format(Locale.getDefault(), "%.2f", gap)
        .trimEnd('0')
        .trimEnd('.')
    return "+$formatted"
}

@Preview(showBackground = true)
@Composable
fun HomeContentPreview() {
    val sampleProperties = listOf(
        PropertyEntity(id = 1, name = "Home", address = "123 Main St"),
        PropertyEntity(id = 2, name = "Office", address = "456 Work Ave")
    )
    val sampleReadings = listOf(
        ReadingEntity(
            id = 1,
            utilityId = 1,
            value = 1234.5,
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
            utilityId = 2,
            value = 987.2,
            timestamp = System.currentTimeMillis() - 172800000,
            source = "Gemma"
        )
    )
    SnaptricTheme {
        HomeContent(
            isAnalyzing = false,
            latestRead = Reading(
                value = "1250.0",
                timestamp = System.currentTimeMillis(),
                source = "MLKit"
            ),
            properties = sampleProperties,
            readings = sampleReadings
        )
    }
}

@Preview(showBackground = true, name = "Home Content Empty")
@Composable
fun HomeContentEmptyPreview() {
    SnaptricTheme {
        HomeContent(
            isAnalyzing = false,
            latestRead = null,
            properties = emptyList(),
            readings = emptyList()
        )
    }
}
