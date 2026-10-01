package com.snaptric.feature.settings.ui

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.snaptric.ai.litertlm.GemmaModelDownloader
import com.snaptric.ai.litertlm.GemmaModelDownloader.State
import com.snaptric.core.designsystem.components.SnaptricCard
import com.snaptric.core.designsystem.theme.SnaptricSpacing
import com.snaptric.core.designsystem.theme.SnaptricTheme
import com.snaptric.feature.settings.viewmodel.DataSummary
import com.snaptric.feature.settings.viewmodel.SettingsViewModel

/**
 * Entry point for the Settings screen.
 */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val modelState by viewModel.modelState.collectAsState()
    val dataSummary by viewModel.dataSummary.collectAsState()
    val context = LocalContext.current
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }

    SettingsContent(
        modelState = modelState,
        dataSummary = dataSummary,
        versionName = versionName,
        onDownloadModel = viewModel::downloadModel,
        onCancelDownload = viewModel::cancelDownload,
        onDeleteModel = viewModel::deleteModel
    )
}

@Composable
fun SettingsContent(
    modelState: State,
    dataSummary: DataSummary,
    versionName: String?,
    onDownloadModel: (allowMobileData: Boolean) -> Unit = {},
    onCancelDownload: () -> Unit = {},
    onDeleteModel: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Extra bottom space so the camera button never covers the last item.
        contentPadding = PaddingValues(
            start = SnaptricSpacing.md,
            end = SnaptricSpacing.md,
            top = SnaptricSpacing.md,
            bottom = 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.md)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = SnaptricSpacing.sm, bottom = SnaptricSpacing.xs)
            )
        }
        item { ModelSection(modelState, onDownloadModel, onCancelDownload, onDeleteModel) }
        item {
            SettingsSection(icon = Icons.Default.Lock, title = "Your data") {
                Text(
                    "${count(dataSummary.properties, "property", "properties")}, " +
                        "${count(dataSummary.meters, "meter", "meters")} and " +
                        "${count(dataSummary.readings, "reading", "readings")}, " +
                        "stored only on this phone. Photos are read on the device and never uploaded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            SettingsSection(icon = Icons.Default.Info, title = "About") {
                Text(
                    "Snaptric${versionName?.let { " $it" }.orEmpty()} · open source",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * The optional Gemma model: what it does, and download, progress, or remove.
 */
@Composable
private fun ModelSection(
    state: State,
    onDownload: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    fun size(bytes: Long) = Formatter.formatShortFileSize(context, bytes)
    var allowMobileData by rememberSaveable { mutableStateOf(false) }

    SettingsSection(icon = Icons.Default.AutoAwesome, title = "On-device AI") {
        Text(
            "Gemma writes the monthly recap on Home in plain language. It runs on this phone, so nothing is sent anywhere.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        when (state) {
            State.NotInstalled, State.Failed -> {
                if (state == State.Failed) {
                    Text(
                        "The download didn't finish. Check your connection and free space, then try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = allowMobileData, onCheckedChange = { allowMobileData = it })
                    Text("Allow mobile data", style = MaterialTheme.typography.bodyMedium)
                }
                Button(onClick = { onDownload(allowMobileData) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Download Gemma (${size(GemmaModelDownloader.MODEL_SIZE_BYTES)})")
                }
            }
            is State.Downloading -> {
                val progress = state.progress
                if (progress != null) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Text(
                    if (state.totalBytes > 0) "Downloading ${size(state.downloadedBytes)} of ${size(state.totalBytes)}"
                    else "Waiting to download…",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(onClick = onCancel) { Text("Cancel download") }
            }
            is State.Installed -> {
                Text(
                    "Installed · ${size(state.file.length())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedButton(onClick = onDelete) { Text("Remove model") }
            }
        }
    }
}

private fun count(n: Int, one: String, many: String) = "$n ${if (n == 1) one else many}"

@Composable
private fun SettingsSection(icon: ImageVector, title: String, content: @Composable () -> Unit) {
    SnaptricCard {
        Column(
            modifier = Modifier.padding(SnaptricSpacing.md),
            verticalArrangement = Arrangement.spacedBy(SnaptricSpacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(SnaptricSpacing.sm))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    SnaptricTheme {
        SettingsContent(
            modelState = State.Downloading(downloadedBytes = 900_000_000, totalBytes = GemmaModelDownloader.MODEL_SIZE_BYTES),
            dataSummary = DataSummary(properties = 2, meters = 3, readings = 41),
            versionName = "1.0"
        )
    }
}
