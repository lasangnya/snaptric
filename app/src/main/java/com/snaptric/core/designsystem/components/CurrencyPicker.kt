package com.snaptric.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.snaptric.core.designsystem.theme.SnaptricSpacing
import com.snaptric.core.domain.tariff.currencyOptions
import java.util.Currency

/**
 * A button showing the chosen currency ("GBP · British Pound") that opens a searchable list.
 */
@Composable
fun CurrencyPicker(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Currency"
) {
    var open by remember { mutableStateOf(false) }
    val name = remember(selected) { runCatching { Currency.getInstance(selected).displayName }.getOrNull() }
    OutlinedButton(onClick = { open = true }, modifier = modifier.fillMaxWidth()) {
        Text(
            text = "$label: " + listOfNotNull(selected, name).joinToString(" · "),
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
    }
    if (open) {
        CurrencyPickerDialog(
            onDismiss = { open = false },
            onSelect = {
                onSelect(it)
                open = false
            }
        )
    }
}

@Composable
private fun CurrencyPickerDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val options = remember(query) { currencyOptions(query) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose currency") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search, e.g. EUR or euro") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp).padding(top = SnaptricSpacing.sm)) {
                    items(options, key = { it.code }) { option ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(option.code) }
                                .padding(vertical = SnaptricSpacing.sm)
                        ) {
                            Text(option.code, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(option.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
