package com.example.calculatingpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.calculatingpaper.data.AppPreferences
import com.example.calculatingpaper.view.screens.MainScreen
import com.example.calculatingpaper.view.screens.SettingsScreen
import com.example.calculatingpaper.viewmodel.NoteViewModel
import com.example.calculatingpaper.ui.theme.CalculatingPaperTheme

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
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val noteViewModel: NoteViewModel = viewModel()
    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context) }

    NavHost(
        navController = navController,
        startDestination = AppDestinations.MAIN_SCREEN
    ) {
        composable(AppDestinations.MAIN_SCREEN) {
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
    }
}