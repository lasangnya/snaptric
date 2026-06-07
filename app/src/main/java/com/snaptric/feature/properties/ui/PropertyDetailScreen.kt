package com.snaptric.feature.properties.ui

import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import com.snaptric.core.designsystem.components.SnaptricBadge
import com.snaptric.core.designsystem.components.SnaptricCard
import com.snaptric.core.designsystem.components.SnaptricEmptyState
import com.snaptric.core.designsystem.theme.ElectricityYellow
import com.snaptric.core.designsystem.theme.GasOrange
import com.snaptric.core.designsystem.theme.SnaptricSpacing
import com.snaptric.core.designsystem.theme.SnaptricTheme
import com.snaptric.core.designsystem.theme.WaterBlue
import com.snaptric.feature.properties.viewmodel.PropertyDetailViewModel

@Composable
fun PropertyDetailScreen(
    viewModel: PropertyDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onUtilityClick: (Long) -> Unit
) {
    val utilities by viewModel.utilities.collectAsState()

    PropertyDetailContent(
        utilities = utilities,
        onBack = onBack,
        onAddUtility = { type, unit, initialReading, name ->
            viewModel.addUtility(type, unit, initialReading, name)
        },
        onUtilityClick = onUtilityClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailContent(
    utilities: List<UtilityEntity>,
    onBack: () -> Unit,
    onAddUtility: (UtilityType, String, Double, String?) -> Unit,
    onUtilityClick: (Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meter Room") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Meter"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (utilities.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SnaptricEmptyState(
                    icon = Icons.Default.Bolt,
                    title = "No Meters Added",
                    description = "Add your first meter to start capturing readings.",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(SnaptricSpacing.md))
                TextButton(onClick = { showAddDialog = true }) {
                    Text("Add your first meter")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding),
                contentPadding = PaddingValues(SnaptricSpacing.md),
                verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.md)
            ) {
                items(
                    items = utilities,
                    key = { it.id }
                ) { utility ->
                    UtilityCard(
                        utility = utility,
                        onClick = { onUtilityClick(utility.id) },
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(300),
                            fadeOutSpec = tween(300),
                            placementSpec = tween(300)
                        )
                    )
                }
            }
        }

        if (showAddDialog) {
            AddUtilityDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { type, unit, initialReading, name ->
                    onAddUtility(type, unit, initialReading, name)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun UtilityCard(
    utility: UtilityEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = when (utility.type) {
        UtilityType.GAS -> GasOrange
        UtilityType.ELECTRICITY -> ElectricityYellow
        UtilityType.WATER -> WaterBlue
    }

    val typeLabel = utility.type.name.replaceFirstChar { it.uppercase() }

    SnaptricCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SnaptricSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SnaptricSpacing.md)
        ) {
            // Colored indicator with soft background
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }

            // Name and unit
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = utility.name ?: utility.type.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(SnaptricSpacing.xs))
                Text(
                    text = "Unit: ${utility.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Type badge
            SnaptricBadge(
                text = typeLabel,
                containerColor = color.copy(alpha = 0.15f),
                contentColor = color
            )

            // Chevron
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUtilityDialog(
    onDismiss: () -> Unit,
    onConfirm: (UtilityType, String, Double, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(UtilityType.ELECTRICITY) }
    var initialReading by remember { mutableStateOf("") }

    val unitOptions = when (selectedType) {
        UtilityType.GAS -> listOf("kWh", "Wh")
        UtilityType.ELECTRICITY -> listOf("m³", "ft³", "kWh", "therms")
        UtilityType.WATER -> listOf("m³", "Liters", "Gallons")
    }
    var selectedUnit by remember { mutableStateOf(unitOptions.first()) }

    // Reset unit when type changes
    val currentUnitOptions = unitOptions
    val currentFirst = currentUnitOptions.first()
    if (selectedUnit !in currentUnitOptions) {
        selectedUnit = currentFirst
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add New Meter",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.md)
            ) {
                // Type selector
                Text(
                    text = "Utility type",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    UtilityType.entries.forEachIndexed { index, type ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = UtilityType.entries.size
                            ),
                            onClick = { selectedType = type },
                            selected = selectedType == type
                        ) {
                            Text(type.name.replaceFirstChar { it.uppercase() })
                        }
                    }
                }

                // Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Meter Name (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Reading input
                OutlinedTextField(
                    value = initialReading,
                    onValueChange = { initialReading = it },
                    label = { Text("Initial reading") },
                    suffix = { Text(selectedUnit) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Unit selector
                Text(
                    text = "Measurement Unit",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SnaptricSpacing.sm)
                ) {
                    currentUnitOptions.forEach { unit ->
                        FilterChip(
                            selected = selectedUnit == unit,
                            onClick = { selectedUnit = unit },
                            label = { Text(unit) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val readingValue = initialReading.toDoubleOrNull() ?: 0.0
                    onConfirm(selectedType, selectedUnit, readingValue, name.ifBlank { null })
                },
                enabled = initialReading.isNotBlank()
            ) {
                Text("Save Meter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun PropertyDetailContentPreview() {
    SnaptricTheme {
        PropertyDetailContent(
            utilities = listOf(
                UtilityEntity(
                    id = 1,
                    propertyId = 1,
                    type = UtilityType.ELECTRICITY,
                    unit = "kWh",
                    initialReading = 1200.0,
                    name = "Main Meter"
                ),
                UtilityEntity(
                    id = 2,
                    propertyId = 1,
                    type = UtilityType.GAS,
                    unit = "m³",
                    initialReading = 45.0,
                    name = null
                ),
                UtilityEntity(
                    id = 3,
                    propertyId = 1,
                    type = UtilityType.WATER,
                    unit = "Liters",
                    initialReading = 8900.0,
                    name = "Garden Supply"
                )
            ),
            onBack = {},
            onAddUtility = { _, _, _, _ -> },
            onUtilityClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PropertyDetailContentEmptyPreview() {
    SnaptricTheme {
        PropertyDetailContent(
            utilities = emptyList(),
            onBack = {},
            onAddUtility = { _, _, _, _ -> },
            onUtilityClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddUtilityDialogPreview() {
    SnaptricTheme {
        AddUtilityDialog(
            onDismiss = {},
            onConfirm = { _, _, _, _ -> }
        )
    }
}
