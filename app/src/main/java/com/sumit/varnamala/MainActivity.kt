package com.sumit.varnamala

import com.sumit.varnamala.data.Screen
import com.sumit.varnamala.ui.screens.CameraScreen
import com.sumit.varnamala.ui.screens.GalleryScreen
import com.sumit.varnamala.ui.screens.HistoryScreen
import com.sumit.varnamala.ui.screens.ImageDetailScreen
import com.sumit.varnamala.ui.screens.SettingsScreen
import com.sumit.varnamala.ui.screens.LanguageScreen
import com.sumit.varnamala.ui.screens.ModelsScreen
import com.sumit.varnamala.ui.screens.OnboardingLanguageScreen
import com.sumit.varnamala.ui.screens.TextInputScreen
import com.sumit.varnamala.data.preferences.ThemeSettingsRepository
import com.sumit.varnamala.data.preferences.AppLanguageRepository
import com.sumit.varnamala.data.preferences.OnboardingRepository
import com.sumit.varnamala.ui.theme.VarnamalaTheme
import com.sumit.varnamala.service.OverlayManager
import com.sumit.varnamala.utils.ScreenCaptureHelper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import android.content.res.Configuration
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

            // Update locale when language changes
            val currentLanguage by languageRepository.appLanguage.collectAsState(initial = "en")
            LaunchedEffect(currentLanguage) {
                val locale = Locale(currentLanguage)
                Locale.setDefault(locale)
                val config = Configuration(resources.configuration)
                config.setLocale(locale)

                resources.updateConfiguration(config, resources.displayMetrics)
                applicationContext.resources.updateConfiguration(
                    config,
                    applicationContext.resources.displayMetrics
                )
            }

            val isDarkTheme by themeSettingsRepository.isDarkTheme.collectAsState(initial = true)

            val hasSelectedAppLanguage by onboardingRepository.hasSelectedAppLanguageLoaded.collectAsState(initial = null)
            val hasExplicitAppLanguage by languageRepository.hasExplicitAppLanguageLoaded.collectAsState(initial = null)

            LaunchedEffect(isDarkTheme) {
                WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                
                val windowInsetsController = WindowInsetsControllerCompat(
                    activity.window, 
                    activity.window.decorView
                )

                windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme
                windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
            }

            VarnamalaTheme(darkTheme = isDarkTheme) {
                val startDestination = when {
                    hasSelectedAppLanguage == null || hasExplicitAppLanguage == null -> null
                    hasSelectedAppLanguage == true || hasExplicitAppLanguage == true -> Screen.Home.route
                    else -> Screen.OnboardingLanguage.route
                }

                if (startDestination == null) {
                    Box(modifier = Modifier.fillMaxSize())
                } else {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.fillMaxSize()
                    ) {
                            composable(
                                Screen.OnboardingLanguage.route,
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
                            ) {
                                OnboardingLanguageScreen(navController)
                            }
                        composable(
                            Screen.Home.route,
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
                        ) {
                            CameraScreen(navController)
                        }
                        composable(
                            Screen.Gallery.route,
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
                        ) {
                            GalleryScreen(navController)
                        }
                        composable(
                            Screen.Settings.route,
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
                        ) {
                            SettingsScreen(
                                navController = navController,
                                themeRepository = themeSettingsRepository
                            )
                        }
                        composable(
                            Screen.Language.route,
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
                        ) {
                            LanguageScreen(navController)
                        }
                        composable(
                            Screen.Models.route,
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
                        ) {
                            ModelsScreen(navController)
                        }
                        composable(
                            Screen.TextInput.route,
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
                        ) {
                            TextInputScreen(navController)
                        }
                        composable(
                            Screen.History.route,
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
                        ) {
                            HistoryScreen(navController)
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
                            }
                        ) { backStackEntry ->
                            val imageId = backStackEntry.arguments?.getLong("id") ?: -1L
                            ImageDetailScreen(
                                navController = navController,
                                imageId = imageId
                            )
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
