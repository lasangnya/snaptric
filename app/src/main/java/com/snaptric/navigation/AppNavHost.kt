package com.snaptric.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.snaptric.feature.capture.ui.CaptureScreen

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
            /* TODO : Add home screen */
        }
        composable(TopLevelDestination.Properties.route){
            /* TODO : Add properties screen */
        }
        composable(TopLevelDestination.Settings.route){
            /* TODO : Add settings screen */
        }
        composable("capture") { CaptureScreen() }
    }
}