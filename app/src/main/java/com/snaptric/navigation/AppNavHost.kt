package com.snaptric.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.snaptric.feature.capture.ui.CaptureScreen
import com.snaptric.feature.capture.viewmodel.CaptureViewModel
import com.snaptric.feature.home.ui.HomeScreen
import com.snaptric.feature.home.viewmodel.HomeViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier
){
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.Home.route,
        modifier = modifier
    ){
        composable(TopLevelDestination.Home.route){
            val viewModel : HomeViewModel = hiltViewModel()
            HomeScreen(viewModel)
        }
        composable(TopLevelDestination.Properties.route){
            /* TODO : Add properties screen */
        }
        composable(TopLevelDestination.Settings.route){
            /* TODO : Add settings screen */
        }
        composable("capture") {
            val captureViewModel : CaptureViewModel = hiltViewModel()
            val isAnalyzing by captureViewModel.isAnalyzing.collectAsState()

            CaptureScreen(
                isAnalyzing = isAnalyzing,
                onClose = { navController.popBackStack() },
                onCapture = { uri ->
                    // Trigger analysis here
                    // Navigation will happen from inside the ViewModel when finished.
                    captureViewModel.analyzeAndSave(uri){
                        navController.popBackStack()
                    }
                }
            ) }
    }
}