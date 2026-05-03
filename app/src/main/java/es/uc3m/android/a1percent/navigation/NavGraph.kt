package es.uc3m.android.a1percent.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import es.uc3m.android.a1percent.MainActivity
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.ui.navigation.AppViewModel
import es.uc3m.android.a1percent.ui.navigation.BottomNavBar
import es.uc3m.android.a1percent.ui.navigation.DefaultTopBar
import es.uc3m.android.a1percent.ui.navigation.ExpandableFabMenu
import es.uc3m.android.a1percent.ui.screens.goal.CreateGoalCard
import es.uc3m.android.a1percent.ui.screens.home.HomeScreen
import es.uc3m.android.a1percent.ui.screens.login.LoginScreen
import es.uc3m.android.a1percent.ui.screens.login.RegisterScreen
import es.uc3m.android.a1percent.ui.screens.profile.ProfileScreen

import es.uc3m.android.a1percent.ui.screens.progress.ProgressScreen
import es.uc3m.android.a1percent.ui.screens.social.SocialScreen
import es.uc3m.android.a1percent.ui.screens.splash.SplashScreen
import es.uc3m.android.a1percent.ui.screens.targets.GoalDetailScreen
import es.uc3m.android.a1percent.ui.screens.targets.TargetsScreen
import es.uc3m.android.a1percent.ui.screens.tasks.CreateTaskCard

private const val TRANSITION_DURATION = 320
private const val FADE_DURATION = 220

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph() {
    viewModel<AppViewModel>()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentBaseRoute = currentRoute?.substringBefore("/")?.substringBefore("?")

    val currentUser by SessionRepository.currentUser.collectAsStateWithLifecycle()
    val pendingNavigation by MainActivity.pendingNavigation.collectAsStateWithLifecycle()

    // Handles notification tap when the app is already running (onNewIntent path).
    // When a destination arrives and the user is already on a main screen, navigate there.
    LaunchedEffect(pendingNavigation) {
        val dest = pendingNavigation ?: return@LaunchedEffect
        val route = currentRoute ?: return@LaunchedEffect
        if (route != AppScreens.SplashScreen.route && route != AppScreens.LoginScreen.route) {
            navController.navigate(dest) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = false
            }
            MainActivity.pendingNavigation.value = null
        }
    }

    val topLevelRoutes = AppScreens.topLevelScreens.map { it.route }.toSet()
    val currentScreenTitle = AppScreens.topLevelScreens
        .firstOrNull { it.route == currentRoute }?.label ?: ""

    // Sub-screens that live under a top-level route but should manage their own chrome
    val isSubScreen = currentRoute == AppScreens.TargetsScreen.route + "/goal/{goalId}"

    var isFabExpanded by remember { mutableStateOf(false) }
    var isTaskCardVisible by remember { mutableStateOf(false) }
    var isGoalCardVisible by remember { mutableStateOf(false) }

    val blurRadius by animateDpAsState(
        targetValue = if (isFabExpanded || isTaskCardVisible || isGoalCardVisible) 10.dp else 0.dp,
        label = "BlurAnimation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.blur(blurRadius),
            topBar = {
                if (currentBaseRoute in topLevelRoutes && !isSubScreen) {
                    DefaultTopBar(
                        title = currentScreenTitle,
                        onProfileClick = {
                            navController.navigate(AppScreens.ProfileScreen.route + "/placeholder")
                        }
                    )
                }
            },
            bottomBar = {
                if (currentBaseRoute in topLevelRoutes && !isSubScreen) {
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
                startDestination = AppScreens.SplashScreen.route,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn(tween(FADE_DURATION)) },
                exitTransition = { fadeOut(tween(FADE_DURATION)) },
                popEnterTransition = { fadeIn(tween(FADE_DURATION)) },
                popExitTransition = { fadeOut(tween(FADE_DURATION)) }
            ) {
                composable(route = AppScreens.SplashScreen.route) {
                    SplashScreen(
                        onSplashFinished = {
                            // If the user is already authenticated (Firebase session persisted),
                            // skip the login screen. If a notification tap requested a specific
                            // destination (e.g. "social"), go there; otherwise go to Home.
                            val destination = if (SessionRepository.currentUser.value != null) {
                                MainActivity.pendingNavigation.value
                                    ?.also { MainActivity.pendingNavigation.value = null }
                                    ?: AppScreens.HomeScreen.route
                            } else {
                                AppScreens.LoginScreen.route
                            }
                            navController.navigate(destination) {
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
                    arguments = listOf(navArgument(name = "param") { type = NavType.StringType }),
                    enterTransition = {
                        slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(TRANSITION_DURATION)) +
                        fadeIn(tween(TRANSITION_DURATION))
                    },
                    exitTransition = {
                        slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(TRANSITION_DURATION)) +
                        fadeOut(tween(TRANSITION_DURATION))
                    },
                    popEnterTransition = {
                        slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(TRANSITION_DURATION)) +
                        fadeIn(tween(TRANSITION_DURATION))
                    },
                    popExitTransition = {
                        slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(TRANSITION_DURATION)) +
                        fadeOut(tween(TRANSITION_DURATION))
                    }
                ) {
                    ProfileScreen(navController, it.arguments?.getString("param"))
                }

                composable(route = AppScreens.TargetsScreen.route) { TargetsScreen(navController) }

                composable(
                    route = AppScreens.TargetsScreen.route + "/goal/{goalId}",
                    arguments = listOf(navArgument(name = "goalId") { type = NavType.StringType }),
                    enterTransition = {
                        slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(TRANSITION_DURATION)) +
                        fadeIn(tween(TRANSITION_DURATION))
                    },
                    exitTransition = {
                        slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(TRANSITION_DURATION)) +
                        fadeOut(tween(TRANSITION_DURATION))
                    },
                    popEnterTransition = {
                        slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(TRANSITION_DURATION)) +
                        fadeIn(tween(TRANSITION_DURATION))
                    },
                    popExitTransition = {
                        slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(TRANSITION_DURATION)) +
                        fadeOut(tween(TRANSITION_DURATION))
                    }
                ) { backStackEntry ->
                    val goalId = backStackEntry.arguments?.getString("goalId") ?: return@composable
                    val viewModel: es.uc3m.android.a1percent.ui.screens.targets.GoalDetailViewModel =
                        androidx.lifecycle.viewmodel.compose.viewModel()
                    androidx.compose.runtime.LaunchedEffect(goalId) {
                        viewModel.loadGoal(goalId)
                    }
                    GoalDetailScreen(navController, goalId, viewModel)
                }

                composable(
                    route = AppScreens.SocialScreen.route + "?section={section}",
                    arguments = listOf(navArgument("section") {
                        type = NavType.StringType
                        defaultValue = "community"
                    })
                ) { backStackEntry ->
                    val section = backStackEntry.arguments?.getString("section") ?: "community"
                    SocialScreen(navController, initialSection = section)
                }
                composable(route = AppScreens.ProgressScreen.route) { ProgressScreen(navController) }
            }
        }

        if (currentBaseRoute in topLevelRoutes && !isSubScreen) {
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
            CreateGoalCard(onDismiss = { isGoalCardVisible = false })
        }
    }
}
