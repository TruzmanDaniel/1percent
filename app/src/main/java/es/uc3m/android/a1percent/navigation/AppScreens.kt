package es.uc3m.android.a1percent.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppScreens(val route: String, val label: String = "", val icon: ImageVector? = null) {

    object LoginScreen   : AppScreens("login",    "Login")
    object HomeScreen    : AppScreens("home",     "Home",     Icons.Default.Home)
    object ProfileScreen : AppScreens("profile", "Profile")

    object TargetsScreen : AppScreens("targets",  "Targets",  Icons.Default.Flag)
    object SocialScreen  : AppScreens("social",   "Social",   Icons.Default.Group)
    object ProgressScreen: AppScreens("progress", "Progress", Icons.Default.BarChart)

    // Nuevas pantallas (No Top-Level)
    object CreateTaskScreen : AppScreens("create_task", "Create Task")
    object CreateGoalScreen : AppScreens("create_goal", "Create Goal")

    companion object {

        // MAIN SCREENS: the ones that appear in the Bottom Nav Bar
        val mainScreens = listOf(HomeScreen, TargetsScreen, SocialScreen, ProgressScreen)

        // TOP-LEVEL SCREENS: show TopNavBar (able to navigate to profile page) and BottomNavBar
        val topLevelScreens = mainScreens + listOf<AppScreens>()
    }
}
