package com.shohan.khatago.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shohan.khatago.core.di.AppContainer
import com.shohan.khatago.data.repository.AppSettings
import com.shohan.khatago.ui.components.KhataGoBottomBar
import com.shohan.khatago.ui.navigation.Destination
import com.shohan.khatago.ui.navigation.KhataGoNavHost
import com.shohan.khatago.ui.navigation.TAB_ROUTES
import com.shohan.khatago.ui.screens.settings.LockGate
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.KhataGoTheme

/**
 * Root of the KhataGo UI.
 *
 * Responsibilities kept here and nowhere else: providing the dependency
 * container, deciding which of the five tabs is showing the bottom bar, and
 * holding the App Lock gate above everything else.
 */
@Composable
fun KhataGoApp(container: AppContainer) {
    CompositionLocalProvider(LocalAppContainer provides container) {
        KhataGoTheme {
            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val route = backStackEntry?.destination?.route
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            var unlocked by remember { mutableStateOf(false) }
            LaunchedEffect(settings.appLockEnabled) {
                if (!settings.appLockEnabled) unlocked = false
            }

            val lockRequired = settings.appLockEnabled &&
                settings.hasPin &&
                !unlocked &&
                route != null &&
                route != Destination.SPLASH

            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    containerColor = CanvasWhite,
                    bottomBar = {
                        if (route != null && route in TAB_ROUTES) {
                            KhataGoBottomBar(
                                currentRoute = route,
                                onSelect = { target ->
                                    navController.navigate(target) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { padding ->
                    KhataGoNavHost(
                        navController = navController,
                        startDestination = Destination.SPLASH,
                        modifier = Modifier.padding(padding)
                    )
                }

                if (lockRequired) {
                    LockGate(onUnlocked = { unlocked = true })
                }
            }
        }
    }
}
