package com.snaptric.feature.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.snaptric.core.designsystem.theme.SnaptricTheme
import com.snaptric.feature.home.viewmodel.HomeViewModel

// UI for the Home screen
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
){

    val latest by viewModel.latestRead.collectAsState()
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.align(alignment = Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Welcome to Snaptric!")
            Spacer(modifier = Modifier.height(8.dp))
            Card{
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Latest Reading")
                    Text(
                        text = latest?.value ?: "--",
                        style = MaterialTheme.typography.displaySmall
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (latest == null) "No readings yet" else "Updated just now",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview(){
    SnaptricTheme() {
        HomeScreen()
    }
}