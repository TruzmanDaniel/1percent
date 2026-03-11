package es.uc3m.android.a1percent.navigation

import es.uc3m.android.a1percent.ui.screens.CreateGoalScreen
import es.uc3m.android.a1percent.ui.screens.HomeScreen
import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import es.uc3m.android.a1percent.ui.screens.HomeScreen1
import es.uc3m.android.a1percent.ui.screens.LoginScreen
import es.uc3m.android.a1percent.ui.screens.ProfileScreen

@Composable
fun NavGraph() {

    val navController = rememberNavController() // Constant that we will use in each screen. It is the GPS

    NavHost(                                // Container that hosts destinations. The Map
        navController = navController,
        startDestination = AppScreens.HomeScreen1.route
    ) {

        composable(route = AppScreens.HomeScreen1.route) {
            HomeScreen1()
        }
        composable(route = AppScreens.ProfileScreen.route) {
            ProfileScreen()
        }




//        composable("login") {
//            LoginScreen(navController)
//        }
//        composable("home") {
//            HomeScreen(navController)
//        }
//        composable("createGoal") {
//            CreateGoalScreen(navController)
//        }

    }
}