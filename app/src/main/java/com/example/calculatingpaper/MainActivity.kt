package com.example.calculatingpaper

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.calculatingpaper.data.AppPreferences
import com.example.calculatingpaper.view.screens.MainScreen
import com.example.calculatingpaper.view.screens.SettingsScreen
import com.example.calculatingpaper.view.screens.GraphScreen
import com.example.calculatingpaper.viewmodel.NoteViewModel
import com.example.calculatingpaper.ui.theme.CalculatingPaperTheme
import com.example.calculatingpaper.view.screens.AboutScreen
import com.example.calculatingpaper.view.screens.HelpScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalculatingPaperTheme {
                AppNavigation()
            }
        }
    }
}

object AppDestinations {
    const val MAIN_SCREEN = "main"
    const val SETTINGS_SCREEN = "settings"
    const val GRAPH_SCREEN = "graph/{equation}/{noteId}/{variablesJson}"
    const val HELP_SCREEN = "help"
    const val ABOUT_SCREEN = "about"

    fun createGraphRoute(equation: String, noteId: Long, variablesJson: String): String {
        val encodedEquation = android.net.Uri.encode(equation)
        val encodedVariables = android.net.Uri.encode(variablesJson)
        return "graph/$encodedEquation/$noteId/$encodedVariables"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val noteViewModel: NoteViewModel = viewModel()
    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context) }
    val activity = LocalContext.current as? ComponentActivity

    NavHost(
        navController = navController,
        startDestination = AppDestinations.MAIN_SCREEN,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }

    ) {

        composable(AppDestinations.MAIN_SCREEN) {
            LaunchedEffect(Unit) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            MainScreen(
                viewModel = noteViewModel,
                context = context,
                navController = navController
            )
        }

        composable(AppDestinations.SETTINGS_SCREEN) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                appPreferences = appPreferences
            )
        }

        composable(AppDestinations.HELP_SCREEN) {
            HelpScreen(onBack = { navController.popBackStack() })
        }

        composable(AppDestinations.ABOUT_SCREEN) {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = AppDestinations.GRAPH_SCREEN,
            arguments = listOf(
                navArgument("equation") { type = NavType.StringType },
                navArgument("noteId") { type = NavType.LongType },
                navArgument("variablesJson") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedEquation = backStackEntry.arguments?.getString("equation") ?: ""
            val equation = android.net.Uri.decode(encodedEquation)
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
            val variablesJson = backStackEntry.arguments?.getString("variablesJson") ?: ""


            LaunchedEffect(Unit) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }

            DisposableEffect(Unit) {
                onDispose {
                }
            }

            GraphScreen(
                navController = navController,
                equation = equation,
                noteId = noteId,
                variablesJson = variablesJson,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}