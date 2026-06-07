package com.snaptric

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.snaptric.core.designsystem.theme.SnaptricTheme
import com.snaptric.navigation.AppNavHost
import com.snaptric.navigation.TopLevelDestination
import dagger.hilt.android.AndroidEntryPoint

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

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    AppRootContent(
        currentRoute = currentRoute,
        onNavigate = { route -> navController.navigate(route) }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }

}

@Composable
fun AppRootContent(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
){
    val bottomBarDestinations = listOf(
        TopLevelDestination.Home,
        TopLevelDestination.Properties,
        TopLevelDestination.Settings
    )
    Scaffold(
        bottomBar = {
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
