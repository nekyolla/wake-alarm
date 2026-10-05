package com.kindness.wakealarm

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kindness.wakealarm.data.SettingsRepository
import com.kindness.wakealarm.ui.history.HistoryScreen
import com.kindness.wakealarm.ui.home.HomeScreen
import com.kindness.wakealarm.ui.keywords.KeywordListScreen
import com.kindness.wakealarm.ui.onboarding.OnboardingScreen
import com.kindness.wakealarm.ui.onboarding.PermissionOnboardingScreen
import com.kindness.wakealarm.ui.settings.SettingsScreen
import com.kindness.wakealarm.ui.theme.WakeAlarmTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
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

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val KEYWORDS = "keywords"
    const val PERMISSIONS = "permissions"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
}

@Composable
fun WakeAlarmApp(appViewModel: AppViewModel = viewModel()) {
    val onboardingCompleted by appViewModel.onboardingCompleted.collectAsStateWithLifecycle()

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val completed = onboardingCompleted ?: return@Box
        val navController = rememberNavController()
        val slide = tween<androidx.compose.ui.unit.IntOffset>(300)

        NavHost(
            navController = navController,
            startDestination = if (completed) Routes.HOME else Routes.ONBOARDING,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, slide) + fadeIn(tween(300))
            },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, slide) + fadeOut(tween(200))
            }
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
                    onNavigateToKeywords = { navController.navigate(Routes.KEYWORDS) },
                    onNavigateToPermissions = { navController.navigate(Routes.PERMISSIONS) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                    onNavigateToHistory = { navController.navigate(Routes.HISTORY) }
                )
            }

            composable(Routes.KEYWORDS) {
                KeywordListScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.PERMISSIONS) {
                PermissionOnboardingScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onReplayOnboarding = { navController.navigate(Routes.ONBOARDING) }
                )
            }

            composable(Routes.HISTORY) {
                HistoryScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
