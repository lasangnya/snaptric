package com.snaptric.feature.properties.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import com.snaptric.feature.properties.viewmodel.PropertyDetailViewModel

@Composable
fun PropertyDetailScreen(
    viewModel: PropertyDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onUtilityClick : (Long) -> Unit
){
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
    utilities : List<UtilityEntity>,
    onBack: () -> Unit,
    onAddUtility : (UtilityType, String, Double, String?) -> Unit,
    onUtilityClick: (Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meter Room") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Meter")
                    }
                }
            )
        },
    ) { padding ->
        if (utilities.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No meters added yet", style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { showAddDialog = true }) {
                        Text("Add your first meter")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(utilities) { utility ->
                    UtilityCard(utility, onClick = { onUtilityClick(utility.id) })
                }
            }
        }
        if (showAddDialog){
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
fun UtilityCard(utility: UtilityEntity,
                onClick : () -> Unit) {
    val color = when(utility.type){
        UtilityType.GAS -> Color(0xFFFF9800)
        UtilityType.ELECTRICITY -> Color(0xFFFFEB3B)
        UtilityType.WATER -> Color(0xFF2196F3)
    }
    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable{onClick()}
    ) {
        Row(modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(12.dp))
            Column{
                Text(utility.name ?: utility.type.name, style = MaterialTheme.typography.titleMedium)
                Text("Unit : ${utility.unit}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUtilityDialog(
    onDismiss: () -> Unit,
    onConfirm: (UtilityType, String, Double, String?) -> Unit
){
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(UtilityType.ELECTRICITY) }
    var initialReading by remember { mutableStateOf("") }


    val unitOptions = when(selectedType){
        UtilityType.GAS -> listOf("kWh","Wh")
        UtilityType.ELECTRICITY -> listOf("m³", "ft³", "kWh", "therms")
        UtilityType.WATER -> listOf("m³", "Liters", "Gallons")
    }
    var selectedUnit by remember { mutableStateOf(unitOptions.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Meter") },
        text = {
            Column( verticalArrangement = Arrangement.spacedBy(8.dp) ){
                // type selector (segmented buttons)
                Text("Utility type", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    UtilityType.entries.forEachIndexed { index, type ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = UtilityType.entries.size),
                            onClick = { selectedType = type },
                            selected = selectedType == type
                        ) {
                            Text(type.name.lowercase().capitalize())
                        }
                    }
                }
                // name and reading inputs
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Meter Name (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = initialReading,
                    onValueChange = { initialReading = it },
                    label = { Text("Initial reading") },
                    suffix = { Text(selectedUnit) },
                    modifier = Modifier.fillMaxWidth()
                )
                // unit selector
                Text("Measurement Unit", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    unitOptions.forEach { unit ->
                        FilterChip(
                            selected = false,
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