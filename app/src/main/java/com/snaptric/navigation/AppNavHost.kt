package com.snaptric.navigation

import androidx.compose.runtime.Composable
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
        composable("capture") { CaptureScreen(
            onClose = { navController.popBackStack() },
            onCapture = { uri ->
            }
        ) }
    }
}