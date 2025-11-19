package com.sumit.varnamala

import com.sumit.varnamala.data.Screen
import com.sumit.varnamala.ui.screens.CameraScreen
import com.sumit.varnamala.ui.screens.GalleryScreen
import com.sumit.varnamala.ui.screens.ImageDetailScreen
import com.sumit.varnamala.ui.screens.SettingsScreen
import com.sumit.varnamala.ui.screens.LanguageScreen
import com.sumit.varnamala.data.preferences.ThemeSettingsRepository
import com.sumit.varnamala.data.preferences.AppLanguageRepository
import com.sumit.varnamala.ui.theme.VarnamalaTheme

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import android.content.res.Configuration
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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

            // Update locale when language changes
            val currentLanguage by languageRepository.appLanguage.collectAsState(initial = "en")
            LaunchedEffect(currentLanguage) {
                // Apply locale change immediately
                val locale = Locale(currentLanguage)
                Locale.setDefault(locale)
                val config = Configuration(resources.configuration)
                config.setLocale(locale)

                // Update resources configuration
                resources.updateConfiguration(config, resources.displayMetrics)

                // Also update the application context configuration
                applicationContext.resources.updateConfiguration(
                    config,
                    applicationContext.resources.displayMetrics
                )
            }

            val isDarkTheme by themeSettingsRepository.isDarkTheme.collectAsState(initial = true)

            SideEffect {
                WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                val windowInsetsController =
                    WindowInsetsControllerCompat(activity.window, activity.window.decorView)
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                windowInsetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

            VarnamalaTheme(darkTheme = isDarkTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
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
}
