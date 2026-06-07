package com.snaptric.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.snaptric.feature.capture.ui.CaptureScreen
import com.snaptric.feature.home.ui.HomeScreen
import com.snaptric.feature.home.viewmodel.HomeViewModel
import com.snaptric.feature.properties.ui.PropertiesScreen
import com.snaptric.feature.properties.ui.PropertyDetailScreen
import com.snaptric.feature.properties.ui.UtilityScreen
import com.snaptric.feature.properties.viewmodel.PropertyViewModel

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
            val viewModel : PropertyViewModel = hiltViewModel()
            PropertiesScreen(viewModel, onPropertyClick = { propertyId ->
                navController.navigate("property_detail/$propertyId")
            })
        }
        composable(
            route = "property_detail/{propertyId}",
            arguments = listOf(navArgument("propertyId") { type = NavType.LongType })
        ) {
            PropertyDetailScreen(
                onBack = { navController.popBackStack() },
                onUtilityClick = { utilityId ->
                    navController.navigate("utility_detail/$utilityId")
                }
            )
        }
        composable(
            route = "utility_detail/{utilityId}",
            arguments = listOf(navArgument("utilityId") { type = NavType.LongType })
        ) {
            UtilityScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(TopLevelDestination.Settings.route){
            /* TODO : Add settings screen */
        }
        composable("capture") {
            CaptureScreen(
                onClose = { navController.popBackStack() }
            ) }
    }
}