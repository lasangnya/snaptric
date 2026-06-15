package com.snaptric.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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

/**
 * The main navigation graph for the Snaptric application.
 * Defines all routes and their corresponding UI screens, along with custom transitions.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier
){
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.Home.route,
        modifier = modifier,
        // Slide and fade transitions for a modern feel.
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        }
    ){
        // Home Screen: Dashboard summary.
        composable(TopLevelDestination.Home.route){
            val viewModel : HomeViewModel = hiltViewModel()
            HomeScreen(viewModel)
        }
        
        // Properties Screen: List of properties.
        composable(TopLevelDestination.Properties.route){
            val viewModel : PropertyViewModel = hiltViewModel()
            PropertiesScreen(viewModel, onPropertyClick = { propertyId ->
                navController.navigate("property_detail/$propertyId")
            })
        }
        
        // Property Detail: Specific meters for a property.
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
        
        // Utility Detail: Reading history and chart for a meter.
        composable(
            route = "utility_detail/{utilityId}",
            arguments = listOf(navArgument("utilityId") { type = NavType.LongType })
        ) {
            UtilityScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Settings Screen: App preferences.
        composable(TopLevelDestination.Settings.route){
            /* TODO : Add settings screen */
        }
        
        // Capture Screen: The camera-based meter scanning interface.
        composable("capture") {
            CaptureScreen(
                onClose = { navController.popBackStack() }
            ) }
    }
}

