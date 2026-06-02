package com.snaptric.feature.properties.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.designsystem.theme.SnaptricTheme
import com.snaptric.feature.properties.viewmodel.PropertyViewModel

@Composable
fun PropertiesScreen(
    viewModel: PropertyViewModel = hiltViewModel()
){
    val properties by viewModel.properties.collectAsState()

    PropertiesContent(
        properties
    ) { name, address -> viewModel.addProperty(name, address) }
}

@Composable
fun PropertiesContent(
    properties : List<PropertyEntity>,
    onAddProperty : (String, String?) -> Unit,
){
    var showDialog by remember { mutableStateOf(false) }

    Scaffold { padding ->
        if(properties.isEmpty()){
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(padding),
                contentAlignment = Alignment.Center) {
                Text("No properties found. Tap the + button to add one.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(properties){property ->
                    PropertyCard(property)
                }
            }
        }
        if(showDialog){
            AddPropertyDialog(
                onDismiss = { showDialog = false },
                onConfirm = { name, address ->
                    onAddProperty(name, address)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun PropertyCard(property : PropertyEntity){
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = property.name, style = MaterialTheme.typography.titleLarge)
            if (!property.address.isNullOrBlank()){
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location Icon",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = property.address, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun AddPropertyDialog(
    onDismiss : () -> Unit,
    onConfirm : (String, String?) -> Unit
){
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Property") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Property Name (e.g. Home") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (Optional") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {onConfirm(name, address.ifBlank { null })},
                enabled = name.isNotBlank()
            ){
                Text("Save")
            }
        },
        dismissButton ={
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Preview(showBackground = true)
@Composable
fun PropertiesPreview() {
    val mockProperties = listOf(
        PropertyEntity(id = 1, name = "My Home", address = "123 Main St"),
        PropertyEntity(id = 2, name = "Office", address = null)
    )
    SnaptricTheme {
        PropertiesContent(
            properties = mockProperties,
            onAddProperty = { _, _ -> } // Do nothing in preview
        )
    }
}