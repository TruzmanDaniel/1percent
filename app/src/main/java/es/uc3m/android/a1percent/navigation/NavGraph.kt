package es.uc3m.android.a1percent.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import es.uc3m.android.a1percent.ui.screens.HomeScreen
import es.uc3m.android.a1percent.ui.screens.LoginScreen

@Composable
fun NavGraph() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {

        composable("login") {
            LoginScreen(navController)
        }

        composable("home") {
            HomeScreen()
        }

    }
}