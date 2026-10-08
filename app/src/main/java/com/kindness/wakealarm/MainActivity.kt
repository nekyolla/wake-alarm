package com.kindness.wakealarm

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kindness.wakealarm.data.SettingsRepository
import com.kindness.wakealarm.ui.components.BottomTab
import com.kindness.wakealarm.ui.components.StarfieldBackground
import com.kindness.wakealarm.ui.components.WakeBottomBar
import com.kindness.wakealarm.ui.history.HistoryScreen
import com.kindness.wakealarm.ui.home.HomeScreen
import com.kindness.wakealarm.ui.keywords.KeywordListScreen
import com.kindness.wakealarm.ui.onboarding.OnboardingScreen
import com.kindness.wakealarm.ui.onboarding.PermissionOnboardingScreen
import com.kindness.wakealarm.ui.settings.SettingsScreen
import com.kindness.wakealarm.ui.theme.WakeAlarmTheme
import com.kindness.wakealarm.util.AppLocale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The UI is always dark, so system bar icons are always light
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setContent {
            WakeAlarmTheme {
                WakeAlarmApp()
            }
        }
    }
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)

    /** null while loading, so the first frame never flashes the wrong start screen. */
    val onboardingCompleted: StateFlow<Boolean?> = settingsRepository.onboardingCompletedFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted() }
    }
}

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val KEYWORDS = "keywords"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val PERMISSIONS = "permissions"

    /** Top-level destinations shown in the bottom bar. */
    val TABS = listOf(HOME, KEYWORDS, HISTORY, SETTINGS)
}

/**
 * Switch tabs like a bottom bar should: one copy per tab, each keeping its own state.
 * Pops to Home rather than the graph's start destination, which is Onboarding on first run and
 * no longer on the back stack after it finishes.
 */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WakeAlarmApp(appViewModel: AppViewModel = viewModel()) {
    val onboardingCompleted by appViewModel.onboardingCompleted.collectAsStateWithLifecycle()

    StarfieldBackground(Modifier.fillMaxSize()) {
        val completed = onboardingCompleted ?: return@StarfieldBackground
        // Decided once: finishing onboarding navigates explicitly, so the graph never has to change
        val startDestination = remember { if (completed) Routes.HOME else Routes.ONBOARDING }
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        // Hidden while typing so the keyword field sits right on top of the keyboard
        val showBottomBar = currentRoute in Routes.TABS && !WindowInsets.isImeVisible

        val tabs = listOf(
            BottomTab(Routes.HOME, stringResource(R.string.tab_home), Icons.Outlined.Home, Icons.Filled.Home),
            BottomTab(Routes.KEYWORDS, stringResource(R.string.tab_keywords), Icons.Outlined.Key, Icons.Filled.Key),
            BottomTab(Routes.HISTORY, stringResource(R.string.tab_history), Icons.Outlined.History, Icons.Filled.History),
            BottomTab(Routes.SETTINGS, stringResource(R.string.tab_settings), Icons.Outlined.Settings, Icons.Filled.Settings)
        )
        val slide = tween<IntOffset>(300)

        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    // Animate the reserved height too, so content doesn't jump when the bar hides
                    enter = slideInVertically { it } + expandVertically() + fadeIn(),
                    exit = slideOutVertically { it } + shrinkVertically() + fadeOut()
                ) {
                    WakeBottomBar(tabs = tabs, selectedRoute = currentRoute, onSelect = navController::navigateToTab)
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier
                    .padding(innerPadding)
                    // Landscape: keep content clear of a side navigation bar and display cutouts
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(160)) }
            ) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(onFinish = {
                        appViewModel.completeOnboarding()
                        // Clear everything (also when replayed from Settings) so Home is the only screen left
                        navController.navigate(Routes.HOME) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    })
                }

                composable(Routes.HOME) {
                    HomeScreen(
                        onNavigateToKeywords = { navController.navigateToTab(Routes.KEYWORDS) },
                        onNavigateToPermissions = { navController.navigate(Routes.PERMISSIONS) },
                        onNavigateToHistory = { navController.navigateToTab(Routes.HISTORY) }
                    )
                }

                composable(Routes.KEYWORDS) {
                    KeywordListScreen()
                }

                composable(Routes.HISTORY) {
                    HistoryScreen()
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(onReplayOnboarding = { navController.navigate(Routes.ONBOARDING) })
                }

                composable(
                    Routes.PERMISSIONS,
                    enterTransition = {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, slide) + fadeIn(tween(300))
                    },
                    popExitTransition = {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, slide) + fadeOut(tween(200))
                    }
                ) {
                    PermissionOnboardingScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
