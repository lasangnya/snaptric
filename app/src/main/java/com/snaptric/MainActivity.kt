package com.snaptric

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.snaptric.core.designsystem.theme.SnaptricTheme
import com.snaptric.navigation.AppNavHost
import com.snaptric.navigation.TopLevelDestination
import dagger.hilt.android.AndroidEntryPoint

/**
 * The main entry point of the Snaptric application.
 * This activity sets up the Compose theme and initializes the root of the UI.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SnaptricTheme {
                AppRoot()
            }
        }
    }
}

/**
 * AppRoot manages the high-level state of the application, including:
 * - Navigation controller setup.
 * - Initial runtime permission requests.
 * - Routing logic for the main content areas.
 */
@Composable
fun AppRoot() {
    val navController = rememberNavController()

    // Launcher for requesting multiple permissions (e.g., Camera) at once.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    // Request necessary permissions as soon as the app starts.
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA
            )
        )
    }

    // Determine the current route to update UI elements like the bottom bar or FAB visibility.
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    AppRootContent(
        currentRoute = currentRoute,
        onNavigate = { route -> navController.navigate(route) }
    ) { innerPadding ->
        // The AppNavHost defines all the screens and their transitions.
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/**
 * AppRootContent defines the structural layout (Scaffold) of the app.
 * It includes the Bottom Bar for main navigation and a Floating Action Button for the Capture feature.
 */
@Composable
fun AppRootContent(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
){
    // Destinations shown in the bottom navigation bar.
    val bottomBarDestinations = listOf(
        TopLevelDestination.Home,
        TopLevelDestination.Properties,
        TopLevelDestination.Settings
    )
    Scaffold(
        bottomBar = {
            // Hide the bottom bar when in the 'capture' screen to maximize camera view.
            if (currentRoute != "capture"){
                BottomAppBar {
                    bottomBarDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = { onNavigate(destination.route) },
                            icon = {
                                Icon(
                                    destination.icon,
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // Only show the camera FAB if we are not already in the capture screen.
            AnimatedVisibility(
                visible = currentRoute != "capture",
                enter = scaleIn(
                    animationSpec = tween(200)
                ) + fadeIn(
                    animationSpec = tween(200)
                ),
                exit = scaleOut(
                    animationSpec = tween(200)
                ) + fadeOut(
                    animationSpec = tween(200)
                )
            ) {
                FloatingActionButton(
                    onClick = { onNavigate("capture") }
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "capture")
                }
            }
        }
    ) { innerPadding ->
        // Render the actual screen content within the scaffold's padding.
        content(innerPadding)
    }
}


@Preview(showBackground = true)
@Composable
fun AppRootPreview() {
    SnaptricTheme {
        AppRootContent(
            currentRoute = "home",
            onNavigate = {}
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                Text("Content Area Preview")
            }
        }
    }
}
