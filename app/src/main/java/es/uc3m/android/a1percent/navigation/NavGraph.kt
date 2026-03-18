package es.uc3m.android.a1percent.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import es.uc3m.android.a1percent.data.UserRepository
import es.uc3m.android.a1percent.ui.navigation.BottomNavBar
import es.uc3m.android.a1percent.ui.navigation.DefaultTopBar
import es.uc3m.android.a1percent.ui.screens.home.HomeScreen
import es.uc3m.android.a1percent.ui.screens.login.LoginScreen
import es.uc3m.android.a1percent.ui.screens.profile.ProfileScreen
import es.uc3m.android.a1percent.ui.screens.profile.ProfileTopBar
import es.uc3m.android.a1percent.ui.screens.progress.ProgressScreen
import es.uc3m.android.a1percent.ui.screens.social.SocialScreen
import es.uc3m.android.a1percent.ui.screens.targets.TargetsScreen

/**
 * BASE LAYOUT. Global Scaffold integration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    onAddClick: () -> Unit = {} // Function defining what happens when the add button is clicked
) {
    val navController = rememberNavController() // Constant that we will use in each screen. It is the GPS

    val navBackStackEntry by navController.currentBackStackEntryAsState() // Current route
    val currentRoute = navBackStackEntry?.destination?.route
    val currentBaseRoute = currentRoute?.substringBefore("/")  // so base_route/param counts as base_route

    // Observe the current user for ProfileTopBar
    val currentUser by UserRepository.currentUser.collectAsStateWithLifecycle()

    // Routes/Screens where the BottomNavBar must be visible
    val topLevelRoutes = AppScreens.topLevelScreens.map { it.route }.toSet()

    // Title derived from the current screen label (single source of truth: AppScreens)
    val currentScreenTitle = AppScreens.topLevelScreens
        .firstOrNull { it.route == currentRoute }?.label ?: "" // It searches in AppScreens toplevel ones the first one which 'route' coincides which 'currentRoute', to obtain its label

    // Global Scaffold integrated here for optimizing screens nesting, with a shared architecture
    Scaffold(
        // TOP NAVIGATION BAR
        topBar = {

                // Top Nav Bar for Profile Screen:
                if (currentBaseRoute == AppScreens.ProfileScreen.route) {
                    ProfileTopBar(
                        username = currentUser?.name ?: "Profile",
                        onBack = { navController.popBackStack() }
                    )
                // Default Top Nav Bar (shows profile pic and current Screen title)
                } else if(currentBaseRoute in topLevelRoutes) {
                        DefaultTopBar(
                            title = currentScreenTitle,
                            onProfileClick = {
                                navController.navigate(AppScreens.ProfileScreen.route + "/placeholder")

                            }
                        )
                }
        },


        // BOTTOM NAVIGATION BAR
        // Will only be shown on top-level routes
        bottomBar = {
            if (currentBaseRoute in topLevelRoutes) {
                // BottomNavBar is a class/object
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
            startDestination = AppScreens.LoginScreen.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            composable(route = AppScreens.LoginScreen.route) {
                LoginScreen(navController)
            }

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
                ProfileScreen(navController, it.arguments?.getString("param")) // Aquí pasamos el parámetro param, que refiere al argumento 'text' de ProfileScreen
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
        }
    }
}
