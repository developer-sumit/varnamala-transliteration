package com.sumit.varnamala

import com.sumit.varnamala.data.Screen
import com.sumit.varnamala.data.TransliterationHistoryRepository
import com.sumit.varnamala.data.preferences.UserPreferencesRepository
import com.sumit.varnamala.ui.components.LockGate
import com.sumit.varnamala.ui.screens.HelpScreen
import com.sumit.varnamala.ui.screens.ModelManagerScreen
import com.sumit.varnamala.ui.screens.VoiceScreen
import kotlinx.coroutines.flow.first
import com.sumit.varnamala.ui.screens.CameraScreen
import com.sumit.varnamala.ui.screens.GalleryScreen
import com.sumit.varnamala.ui.screens.HistoryScreen
import com.sumit.varnamala.ui.screens.ImageDetailScreen
import com.sumit.varnamala.ui.screens.SettingsScreen
import com.sumit.varnamala.ui.screens.LanguageScreen
import com.sumit.varnamala.ui.screens.OnboardingLanguageScreen
import com.sumit.varnamala.ui.screens.ResourceSetupScreen
import com.sumit.varnamala.ocr.paddle.PaddleModelProvisioner
import com.sumit.varnamala.ui.screens.OcrLanguageScreen
import com.sumit.varnamala.ui.screens.TextInputScreen
import com.sumit.varnamala.ui.screens.TransliterationLanguageScreen
import com.sumit.varnamala.data.preferences.ThemeSettingsRepository
import com.sumit.varnamala.data.preferences.AppLanguageRepository
import com.sumit.varnamala.data.preferences.OnboardingRepository
import com.sumit.varnamala.ui.theme.VarnamalaTheme
import com.sumit.varnamala.service.OverlayManager
import com.sumit.varnamala.utils.ScreenCaptureHelper
import com.sumit.varnamala.ui.components.BottomNavBar

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import android.content.res.Configuration
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument

class MainActivity : FragmentActivity() {
    /** Set by the widget, quick tile and launcher shortcut: open the camera on the home screen. */
    private val openCameraRequest = androidx.compose.runtime.mutableIntStateOf(0)

    /** An image another app shared to Varnamala, waiting to be opened. */
    private val sharedImage = androidx.compose.runtime.mutableStateOf<android.net.Uri?>(null)

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            ACTION_OPEN_CAMERA -> openCameraRequest.intValue++
            Intent.ACTION_SEND -> if (intent.type?.startsWith("image/") == true) {
                sharedImage.value = if (android.os.Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> if (intent.type?.startsWith("image/") == true) {
                // Several images shared at once: open the first; the rest can be shared again.
                sharedImage.value = if (android.os.Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)?.firstOrNull()
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableArrayListExtra<android.net.Uri>(Intent.EXTRA_STREAM)?.firstOrNull()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        // History asks for the owner again after the app has been in the background.
        com.sumit.varnamala.utils.AppLockSession.unlocked = false
    }

    companion object {
        const val ACTION_OPEN_CAMERA = "com.sumit.varnamala.OPEN_CAMERA"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val activity = context as Activity
            val navController = rememberNavController()

            val themeSettingsRepository = remember {
                ThemeSettingsRepository(context)
            }
            val languageRepository = remember {
                AppLanguageRepository(context)
            }

            val onboardingRepository = remember {
                OnboardingRepository(context)
            }
            val paddleModels = remember { PaddleModelProvisioner(context) }

            // Update locale when language changes
            val currentLanguage by languageRepository.appLanguage.collectAsState(initial = "en")
            LaunchedEffect(currentLanguage) {
                val locale = Locale(currentLanguage)
                Locale.setDefault(locale)
                val config = Configuration(resources.configuration)
                config.setLocale(locale)

                resources.updateConfiguration(config, resources.displayMetrics)
                applicationContext.resources.updateConfiguration(
                    config, applicationContext.resources.displayMetrics
                )
            }

            val isDarkTheme by themeSettingsRepository.isDarkTheme.collectAsState(initial = true)

            val hasSelectedAppLanguage by onboardingRepository.hasSelectedAppLanguageLoaded.collectAsState(
                initial = null
            )
            val hasExplicitAppLanguage by languageRepository.hasExplicitAppLanguageLoaded.collectAsState(
                initial = null
            )
            val hasRequiredOcrResources by onboardingRepository.hasRequiredOcrResources.collectAsState(initial = false)

            LaunchedEffect(isDarkTheme) {
                WindowCompat.setDecorFitsSystemWindows(activity.window, false)

                val windowInsetsController = WindowInsetsControllerCompat(
                    activity.window, activity.window.decorView
                )

                windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme
                windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
            }

            VarnamalaTheme(darkTheme = isDarkTheme) {
                val startDestination = when {
                    hasSelectedAppLanguage == null || hasExplicitAppLanguage == null -> null
                    !hasRequiredOcrResources || !paddleModels.isReady(PaddleModelProvisioner.Tier.TINY) -> Screen.ResourceSetup.route
                    hasSelectedAppLanguage == true || hasExplicitAppLanguage == true -> Screen.Home.route
                    else -> Screen.OnboardingLanguage.route
                }

                if (startDestination == null) {
                    Box(modifier = Modifier.fillMaxSize())
                } else {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val bottomBarRoutes = setOf(
                        Screen.Home.route, Screen.TextInput.route, Screen.Gallery.route
                    )
                    val showBottomBar = currentRoute in bottomBarRoutes

                    // Hand-offs from other apps and the home screen. They wait until setup is done.
                    val sharedUri = sharedImage.value
                    LaunchedEffect(sharedUri, currentRoute) {
                        if (sharedUri != null && currentRoute != null && currentRoute != Screen.ResourceSetup.route &&
                            currentRoute != Screen.SharedImage.route
                        ) {
                            navController.navigate(Screen.SharedImage.route)
                        }
                    }
                    val cameraRequest = openCameraRequest.intValue
                    LaunchedEffect(cameraRequest, currentRoute) {
                        if (cameraRequest > 0 && currentRoute != null && currentRoute != Screen.ResourceSetup.route) {
                            if (currentRoute != Screen.Home.route) {
                                navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } }
                            }
                            openCameraRequest.intValue = 0
                        }
                    }
                    // Delete old scans once per launch, if the user asked for that.
                    LaunchedEffect(Unit) {
                        val days = UserPreferencesRepository(context).historyRetentionDays.first()
                        if (days > 0) TransliterationHistoryRepository(context).pruneOlderThan(days)
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(Screen.ResourceSetup.route) { ResourceSetupScreen(navController) }
                            composable(Screen.OnboardingLanguage.route, enterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Left,
                                    animationSpec = tween(300)
                                )
                            }, exitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
                                    animationSpec = tween(300)
                                )
                            }) {
                                OnboardingLanguageScreen(navController)
                            }
                            composable(Screen.Home.route, enterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Left,
                                    animationSpec = tween(300)
                                )
                            }, exitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
                                    animationSpec = tween(300)
                                )
                            }) {
                                CameraScreen(navController)
                            }
                            composable(Screen.Gallery.route, enterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Left,
                                    animationSpec = tween(300)
                                )
                            }, exitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
                                    animationSpec = tween(300)
                                )
                            }) {
                                GalleryScreen(navController)
                            }
                            composable(Screen.Settings.route, enterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Left,
                                    animationSpec = tween(300)
                                )
                            }, exitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
                                    animationSpec = tween(300)
                                )
                            }) {
                                SettingsScreen(
                                    navController = navController,
                                    themeRepository = themeSettingsRepository
                                )
                            }
                            composable(Screen.Language.route, enterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Left,
                                    animationSpec = tween(300)
                                )
                            }, exitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
                                    animationSpec = tween(300)
                                )
                            }) {
                                LanguageScreen(navController)
                            }
                            composable(Screen.OcrLanguage.route, enterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Left,
                                    animationSpec = tween(300)
                                )
                            }, exitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
                                    animationSpec = tween(300)
                                )
                            }) {
                                OcrLanguageScreen(navController)
                            }
                            composable(
                                route = Screen.TransliterationLanguage.route,
                                arguments = listOf(navArgument("role") { type = NavType.StringType }),
                                enterTransition = {
                                    slideIntoContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Companion.Left,
                                        animationSpec = tween(300)
                                    )
                                },
                                exitTransition = {
                                    slideOutOfContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
                                        animationSpec = tween(300)
                                    )
                                }
                            ) { backStackEntry ->
                                TransliterationLanguageScreen(
                                    navController = navController,
                                    role = backStackEntry.arguments?.getString("role")
                                        ?: Screen.TransliterationLanguage.ROLE_TARGET
                                )
                            }
                            composable(Screen.TextInput.route, enterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Left,
                                    animationSpec = tween(300)
                                )
                            }, exitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
                                    animationSpec = tween(300)
                                )
                            }) {
                                TextInputScreen(navController)
                            }
                            composable(Screen.History.route, enterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Left,
                                    animationSpec = tween(300)
                                )
                            }, exitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
                                    animationSpec = tween(300)
                                )
                            }) {
                                LockGate { HistoryScreen(navController) }
                            }
                            composable(Screen.Voice.route) { VoiceScreen(navController) }
                            composable(Screen.Models.route) { ModelManagerScreen(navController) }
                            composable(Screen.Help.route) { HelpScreen(navController) }
                            composable(Screen.SharedImage.route) {
                                ImageDetailScreen(navController = navController, imageId = -1L, sharedUri = sharedImage.value)
                            }
                            composable(
                                route = Screen.ImageDetail.route,
                                arguments = listOf(navArgument("id") { type = NavType.LongType }),
                                enterTransition = {
                                    slideIntoContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Companion.Left,
                                        animationSpec = tween(300)
                                    )
                                },
                                exitTransition = {
                                    slideOutOfContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Companion.Right,
                                        animationSpec = tween(300)
                                    )
                                }) { backStackEntry ->
                                val imageId = backStackEntry.arguments?.getLong("id") ?: -1L
                                ImageDetailScreen(
                                    navController = navController, imageId = imageId
                                )
                            }
                        }

                        // Bottom Navigation Bar
                        if (showBottomBar) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                            ) {
                                BottomNavBar(
                                    navController = navController,
                                    currentRoute = currentRoute
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ScreenCaptureHelper.REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                // Screen capture permission granted, start overlay service
                OverlayManager.startOverlay(this, resultCode, data)
                Toast.makeText(
                    this,
                    getString(R.string.overlay_enabled),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.overlay_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
