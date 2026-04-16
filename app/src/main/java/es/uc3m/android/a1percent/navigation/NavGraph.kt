package es.uc3m.android.a1percent.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
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
import es.uc3m.android.a1percent.ui.screens.goal.CreateGoalCard
import es.uc3m.android.a1percent.ui.screens.home.HomeScreen
import es.uc3m.android.a1percent.ui.screens.login.LoginScreen
import es.uc3m.android.a1percent.ui.screens.login.RegisterScreen
import es.uc3m.android.a1percent.ui.screens.profile.ProfileScreen
import es.uc3m.android.a1percent.ui.screens.profile.ProfileTopBar
import es.uc3m.android.a1percent.ui.screens.progress.ProgressScreen
import es.uc3m.android.a1percent.ui.screens.social.SocialScreen
import es.uc3m.android.a1percent.ui.screens.splash.SplashScreen
import es.uc3m.android.a1percent.ui.screens.targets.GoalDetailScreen
import es.uc3m.android.a1percent.ui.screens.targets.TargetsScreen
import es.uc3m.android.a1percent.ui.screens.tasks.CreateTaskCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentBaseRoute = currentRoute?.substringBefore("/") // Also current route but with no arguments --> Baseroute

    // Observer changed to SessionRepository for the current authenticated user
    val currentUser by SessionRepository.currentUser.collectAsStateWithLifecycle()
    
    val topLevelRoutes = AppScreens.topLevelScreens.map { it.route }.toSet()
    val currentScreenTitle = AppScreens.topLevelScreens
        .firstOrNull { it.route == currentRoute }?.label ?: ""

    var isFabExpanded by remember { mutableStateOf(false) }
    var isTaskCardVisible by remember { mutableStateOf(false) }
    var isGoalCardVisible by remember { mutableStateOf(false) }

    val blurRadius by animateDpAsState(
        targetValue = if (isFabExpanded || isTaskCardVisible || isGoalCardVisible) 10.dp else 0.dp,
        label = "BlurAnimation"
    )

    // Box over Scaffold so we can Overlay elements

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.blur(blurRadius), // When fab expanded (overlay) scaffold is blur (as a blurry background)
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
                        onAddClick = { isFabExpanded = !isFabExpanded }, // Toggle (logic is controlled in ExpandableFabMenu.kt)
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
                startDestination = AppScreens.SplashScreen.route, // TODO: if user already auth, go to HomeScreen
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(route = AppScreens.SplashScreen.route) {
                    SplashScreen(
                        onSplashFinished = {
                            // Go to real start destination
                            navController.navigate(AppScreens.LoginScreen.route) {
                                popUpTo(AppScreens.SplashScreen.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(route = AppScreens.LoginScreen.route) { LoginScreen(navController) }
                composable(route = AppScreens.RegisterScreen.route) { RegisterScreen(navController) }
                composable(route = AppScreens.HomeScreen.route) { HomeScreen(navController) }
                composable(
                    route = AppScreens.ProfileScreen.route + "/{param}",
                    arguments = listOf(navArgument(name = "param") { type = NavType.StringType })
                ) {
                    ProfileScreen(navController, it.arguments?.getString("param")) // Pass something (as userId as an argument)
                }
                composable(route = AppScreens.TargetsScreen.route) { TargetsScreen(navController) }
                composable(
                    route = AppScreens.TargetsScreen.route + "/goal/{goalId}",
                    arguments = listOf(navArgument(name = "goalId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val goalId = backStackEntry.arguments?.getString("goalId") ?: return@composable
                    val viewModel: es.uc3m.android.a1percent.ui.screens.targets.GoalDetailViewModel = 
                        androidx.lifecycle.viewmodel.compose.viewModel()
                    // Load the goal when the screen is first composed
                    androidx.compose.runtime.LaunchedEffect(goalId) {
                        viewModel.loadGoal(goalId)
                    }
                    GoalDetailScreen(navController, goalId, viewModel)
                }
                composable(route = AppScreens.SocialScreen.route) { SocialScreen(navController) }
                composable(route = AppScreens.ProgressScreen.route) { ProgressScreen(navController) }
            }
        }

        // FAB Menu not blurred as it is outside the Scaffold
        if (currentBaseRoute in topLevelRoutes) {
            ExpandableFabMenu(
                isExpanded = isFabExpanded,
                onClose = { isFabExpanded = false },
                onAddTask = { 
                    isFabExpanded = false
                    isTaskCardVisible = true
                },
                onAddGoal = { 
                    isFabExpanded = false
                    isGoalCardVisible = true
                }
            )
        }

        if (isTaskCardVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isTaskCardVisible = false }
            )

            // Over the box, we render the card
            CreateTaskCard(onDismiss = { isTaskCardVisible = false })
        }

        if (isGoalCardVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isGoalCardVisible = false }
            )

            // Over the box, we render the goal card
            CreateGoalCard(onDismiss = { isGoalCardVisible = false })
        }
    }
}
