package es.uc3m.android.a1percent.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import es.uc3m.android.a1percent.ui.navigation.BottomNavBar
import es.uc3m.android.a1percent.ui.screens.HomeScreen
import es.uc3m.android.a1percent.ui.screens.ProfileScreen
import es.uc3m.android.a1percent.ui.screens.ProgressScreen
import es.uc3m.android.a1percent.ui.screens.SocialScreen
import es.uc3m.android.a1percent.ui.screens.TargetsScreen

/**
 * BASE LAYOUT. Global integration with Scaffold.
 */
@Composable
fun NavGraph(
    onAddClick: () -> Unit = {} // Function defining what happens when the add button is clicked
) {
    val navController = rememberNavController() // Constant that we will use in each screen. It is the GPS
    val navBackStackEntry by navController.currentBackStackEntryAsState() // Current route
    val currentRoute = navBackStackEntry?.destination?.route

    // Routes/Screens where the BottomNavBar must be visible
    val topLevelRoutes = AppScreens.topLevelScreens.map { it.route }.toSet()

    // Scaffold integrated here for optimizating screens nesting
    Scaffold(
        // Will only be shown in top-level routes
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onAddClick = onAddClick,
                    onNavigate = { route -> // Callback function
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true }
                            launchSingleTop = true // If we already in Home and Home clicked, avoids duplicating in stack
                            restoreState = true // If previously visited (state saved in stack), retrieve it instead of recreating screen
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost( // Container for the routes. The Map
            navController = navController,
            startDestination = AppScreens.HomeScreen.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            composable(route = AppScreens.HomeScreen.route) {
                HomeScreen(navController)
            }

            composable(
                route = AppScreens.ProfileScreen.route + "/{param}",   // This screen is receiving a parameter with its route
                arguments = listOf(navArgument(name = "param") { // The parameter as an argument called 'param'
                    type = NavType.StringType
                }
                )
            ) {
                ProfileScreen(navController, it.arguments?.getString("param"))
            }

            composable(route = AppScreens.TargetsScreen.route) {
                TargetsScreen(navController)
            }

            composable(route = AppScreens.SocialScreen.route) {
                SocialScreen(navController)
            }

            composable(route = AppScreens.ProgressScreen.route) {
                ProgressScreen(navController)
            }

//        composable("login") {
//            LoginScreen(navController)
//        }
//        composable("createGoal") {
//            CreateGoalScreen(navController)
//        }
        }
    }
}