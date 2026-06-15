package com.snaptric.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Defines the main navigation entry points for the application.
 * Each destination has a route, a display label, and an associated icon.
 */
sealed class TopLevelDestination(
    val route : String,
    val label : String,
    val icon : ImageVector
){
    // The main dashboard where users see their latest activity.
    data object Home : TopLevelDestination(
        route = "home",
        label = "Home",
        icon = Icons.Default.Home
    )
    
    // Management of different physical locations (properties).
    data object Properties : TopLevelDestination(
        route = "Properties",
        label = "Properties",
        icon = Icons.Default.Apartment
    )
    
    // User preferences and app settings.
    data object Settings : TopLevelDestination(
        route = "Settings",
        label = "Settings",
        icon = Icons.Default.Settings
    )
}
