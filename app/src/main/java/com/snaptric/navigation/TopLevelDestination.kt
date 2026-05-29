package com.snaptric.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class TopLevelDestination(
    val route : String,
    val label : String,
    val icon : ImageVector
){
    data object Home : TopLevelDestination(
        route = "home",
        label = "Home",
        icon = Icons.Default.Home
    )
    data object Properties : TopLevelDestination(
        route = "Properties",
        label = "Properties",
        icon = Icons.Default.Apartment
    )
    data object Settings : TopLevelDestination(
        route = "Settings",
        label = "Settings",
        icon = Icons.Default.Settings
    )
}