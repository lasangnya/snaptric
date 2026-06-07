package com.snaptric.feature.properties.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Cottage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Villa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.designsystem.components.SnaptricCard
import com.snaptric.core.designsystem.components.SnaptricEmptyState
import com.snaptric.core.designsystem.theme.SnaptricSpacing
import com.snaptric.core.designsystem.theme.SnaptricTheme
import com.snaptric.feature.properties.viewmodel.PropertyViewModel

@Composable
fun PropertiesScreen(
    viewModel: PropertyViewModel = hiltViewModel(),
    onPropertyClick: (Long) -> Unit
) {
    val properties by viewModel.properties.collectAsState()
    PropertiesContent(
        properties = properties,
        onPropertyClick = onPropertyClick
    ) { name, address, icon -> viewModel.addProperty(name, address, icon) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesContent(
    properties: List<PropertyEntity>,
    onPropertyClick: (Long) -> Unit,
    onAddProperty: (String, String?, String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Property",
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (properties.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                SnaptricEmptyState(
                    icon = Icons.Default.Home,
                    title = "No Properties Yet",
                    description = "Add your first property to start tracking utility readings."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(SnaptricSpacing.md),
                verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.md)
            ) {
                items(
                    items = properties,
                    key = { it.id }
                ) { property ->
                    PropertyCard(
                        property = property,
                        onClick = { onPropertyClick(property.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
        if (showDialog) {
            AddPropertyDialog(
                onDismiss = { showDialog = false },
                onConfirm = { name, address, icon ->
                    onAddProperty(name, address, icon)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun PropertyCard(
    property: PropertyEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SnaptricCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column {
            // Subtle amber accent line at the top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SnaptricSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    // Colored circular background behind the property icon
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PropertyIconProvider.getIcon(property.iconIdentifier),
                            contentDescription = "Property icon",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(SnaptricSpacing.md))
                    Column {
                        Text(
                            text = property.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!property.address.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(SnaptricSpacing.xs))
                            Text(
                                text = property.address,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "View property",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun AddPropertyDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("home") } // default icon key

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add New Property",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.md)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Property Name (e.g. Home)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Select Icon",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SnaptricSpacing.sm)
                ) {
                    for ((id, icon) in PropertyIconProvider.IconsList) {
                        val isSelected = selectedIcon == id
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = 2.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedIcon = id },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = "Icon for $id",
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, address.ifBlank { null }, selectedIcon) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

object PropertyIconProvider {
    val IconsList = mapOf(
        "home" to Icons.Default.Home,
        "apartment" to Icons.Default.Apartment,
        "business" to Icons.Default.Business,
        "cottage" to Icons.Default.Cottage,
        "villa" to Icons.Default.Villa
    )

    fun getIcon(identifier: String): ImageVector {
        return IconsList[identifier] ?: Icons.Default.Home
    }
}

@Preview(showBackground = true)
@Composable
fun PropertiesPreview() {
    val mockProperties = listOf(
        PropertyEntity(id = 1, name = "My Home", address = "123 Main St", iconIdentifier = "home"),
        PropertyEntity(id = 2, name = "Office", address = null, iconIdentifier = "business")
    )
    SnaptricTheme {
        PropertiesContent(
            properties = mockProperties,
            onPropertyClick = { }, // Do nothing in preview
            onAddProperty = { _, _, _ -> } // Do nothing in preview
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddPropertyDialogPreview() {
    SnaptricTheme {
        // We wrap it in a Box so the dialog has a container to anchor to in the preview
        Box(modifier = Modifier.fillMaxSize()) {
            AddPropertyDialog(
                onDismiss = { },
                onConfirm = { _, _, _ -> }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PropertiesEmptyPreview() {
    SnaptricTheme {
        PropertiesContent(
            properties = emptyList(),
            onPropertyClick = { },
            onAddProperty = { _, _, _ -> }
        )
    }
}
