package es.uc3m.android.a1percent.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.ui.navigation.BottomNavBar
import es.uc3m.android.a1percent.ui.navigation.DefaultTopBar
import es.uc3m.android.a1percent.ui.navigation.ExpandableFabMenu
import es.uc3m.android.a1percent.ui.screens.creategoal.CreateGoalScreen
import es.uc3m.android.a1percent.ui.screens.home.HomeScreen
import es.uc3m.android.a1percent.ui.screens.login.LoginScreen
import es.uc3m.android.a1percent.ui.screens.profile.ProfileScreen
import es.uc3m.android.a1percent.ui.screens.profile.ProfileTopBar
import es.uc3m.android.a1percent.ui.screens.progress.ProgressScreen
import es.uc3m.android.a1percent.ui.screens.social.SocialScreen
import es.uc3m.android.a1percent.ui.screens.targets.TargetsScreen
import es.uc3m.android.a1percent.ui.screens.tasks.CreateTaskScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentBaseRoute = currentRoute?.substringBefore("/")

    // Observer changed to SessionRepository for the current authenticated user
    val currentUser by SessionRepository.currentUser.collectAsStateWithLifecycle()
    
    val topLevelRoutes = AppScreens.topLevelScreens.map { it.route }.toSet()
    val currentScreenTitle = AppScreens.topLevelScreens
        .firstOrNull { it.route == currentRoute }?.label ?: ""

    var isFabExpanded by remember { mutableStateOf(false) }

    val blurRadius by animateDpAsState(
        targetValue = if (isFabExpanded) 10.dp else 0.dp,
        label = "BlurAnimation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.blur(blurRadius),
            topBar = {
                if (currentBaseRoute == AppScreens.ProfileScreen.route) {
                    ProfileTopBar(
                        username = currentUser?.name ?: "Profile",
                        onBack = { navController.popBackStack() }
                    )
                } else if(currentBaseRoute in topLevelRoutes) {
                    DefaultTopBar(
                        title = currentScreenTitle,
                        onProfileClick = {
                            navController.navigate(AppScreens.ProfileScreen.route + "/placeholder")
                        }
                    )
                }
            },
            bottomBar = {
                if (currentBaseRoute in topLevelRoutes) {
                    BottomNavBar(
                        currentRoute = currentRoute,
                        isFabExpanded = isFabExpanded,
                        onAddClick = { isFabExpanded = !isFabExpanded },
                        onNavigate = { route ->
                            isFabExpanded = false
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppScreens.LoginScreen.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(route = AppScreens.LoginScreen.route) { LoginScreen(navController) }
                composable(route = AppScreens.HomeScreen.route) { HomeScreen(navController) }
                composable(
                    route = AppScreens.ProfileScreen.route + "/{param}",
                    arguments = listOf(navArgument(name = "param") { type = NavType.StringType })
                ) {
                    ProfileScreen(navController, it.arguments?.getString("param"))
                }
                composable(route = AppScreens.TargetsScreen.route) { TargetsScreen(navController) }
                composable(route = AppScreens.SocialScreen.route) { SocialScreen(navController) }
                composable(route = AppScreens.ProgressScreen.route) { ProgressScreen(navController) }
                composable(route = AppScreens.CreateTaskScreen.route) { CreateTaskScreen(navController) }
                composable(route = AppScreens.CreateGoalScreen.route) { CreateGoalScreen(navController) }
            }
        }

        if (currentBaseRoute in topLevelRoutes) {
            ExpandableFabMenu(
                isExpanded = isFabExpanded,
                onClose = { isFabExpanded = false },
                onAddTask = { 
                    isFabExpanded = false
                    navController.navigate(AppScreens.CreateTaskScreen.route) 
                },
                onAddGoal = { 
                    isFabExpanded = false
                    navController.navigate(AppScreens.CreateGoalScreen.route) 
                }
            )
        }
    }
}
