package com.nsvault.app.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nsvault.app.designsystem.theme.Dimens
import com.nsvault.app.designsystem.theme.VaultColors
import com.nsvault.app.designsystem.theme.VaultGradients
import com.nsvault.app.designsystem.theme.VaultMotion
import com.nsvault.app.feature.auth.changepin.ChangePinScreen
import com.nsvault.app.feature.home.HomeScreen
import com.nsvault.app.feature.library.LibraryScreen
import com.nsvault.app.feature.player.PlayerScreen
import com.nsvault.app.feature.recorder.RecorderScreen
import com.nsvault.app.feature.settings.SettingsScreen

private val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Rounded.Home, Route.Home),
    BottomNavItem("Record", Icons.Rounded.Mic, Route.Recorder),
    BottomNavItem("Library", Icons.Rounded.LibraryMusic, Route.Library),
    BottomNavItem("Settings", Icons.Rounded.Settings, Route.Settings),
)

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Route,
)

private val tabDestinations = setOf(Route.Home, Route.Library, Route.Settings)

@Composable
fun NSVaultNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.let { dest ->
        tabDestinations.any { dest.hasRoute(it::class) }
    } ?: true

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = Route.Home,
                enterTransition = {
                    fadeIn(VaultMotion.enter()) +
                        slideInVertically(VaultMotion.enter()) { it / 24 }
                },
                exitTransition = { fadeOut(VaultMotion.exit()) },
                popEnterTransition = { fadeIn(VaultMotion.enter()) },
                popExitTransition = { fadeOut(VaultMotion.exit()) + slideOutVertically(VaultMotion.exit()) { it / 24 } },
            ) {
                composable<Route.Home> {
                    HomeScreen(
                        onStartRecording = { navController.navigate(Route.Recorder) },
                        onOpenLibrary = { navController.navigate(Route.Library) { popUpTo(navController.graph.findStartDestination().id) { saveState = true } } },
                        onOpenSettings = { navController.navigate(Route.Settings) { popUpTo(navController.graph.findStartDestination().id) { saveState = true } } },
                        onOpenRecording = { id -> navController.navigate(Route.Player(id)) },
                    )
                }
                composable<Route.Recorder> {
                    RecorderScreen(
                        onClose = {
                            if (navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            } else {
                                navController.navigate(Route.Home) { popUpTo(Route.Home) { inclusive = true } }
                            }
                        },
                    )
                }
                composable<Route.Player> {
                    PlayerScreen(
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<Route.Library> {
                    LibraryScreen(
                        onBack = { navController.popBackStack() },
                        onOpenRecording = { id -> navController.navigate(Route.Player(id)) },
                    )
                }
                composable<Route.Settings> {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onChangePin = { navController.navigate(Route.ChangePin) },
                    )
                }
                composable<Route.ChangePin> {
                    ChangePinScreen(
                        onBack = { navController.popBackStack() },
                        onDone = { navController.popBackStack() },
                    )
                }
            }
        }

        if (showBottomBar) {
            VaultBottomBar(
                items = bottomNavItems,
                currentDestination = currentDestination,
                onItemClick = { item ->
                    if (item.route is Route.Recorder) {
                        navController.navigate(Route.Recorder)
                    } else {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun VaultBottomBar(
    items: List<BottomNavItem>,
    currentDestination: androidx.navigation.NavDestination?,
    onItemClick: (BottomNavItem) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(VaultColors.Surface.copy(alpha = 0.96f))
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = Dimens.spaceSm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
                val isRecord = item.route is Route.Recorder

                if (isRecord) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Button,
                                onClick = { onItemClick(item) },
                            )
                            .background(brush = VaultGradients.Aurora, shape = CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = VaultColors.OnAccent,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                                onClick = { onItemClick(item) },
                            )
                            .padding(horizontal = Dimens.spaceMd, vertical = Dimens.spaceXs),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) VaultColors.AuroraCyan else VaultColors.TextTertiary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) VaultColors.AuroraCyan else VaultColors.TextTertiary,
                        )
                    }
                }
            }
        }
    }
}
