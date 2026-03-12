package es.uc3m.android.a1percent.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * "Clase Sellada"
 * Used to organize screens, and define clearly what components are navigable screens and not just drawable elements
 * Define ROUTES
 *
 * ============
 *  REFERENCES
 * ============
 *
 */

// DESTINATIONS
sealed class AppScreens(val route: String, val label: String = "", val icon: ImageVector? = null) {

    object HomeScreen    : AppScreens("home",     "Home",     Icons.Default.Home)
    object ProfileScreen : AppScreens("profile")  // Not top-level, no label/icon needed

    object TargetsScreen : AppScreens("targets",  "Targets",  Icons.Default.Flag)
    object SocialScreen  : AppScreens("social",   "Social",   Icons.Default.Group)
    object ProgressScreen: AppScreens("progress", "Progress", Icons.Default.BarChart)

    companion object {
        // MAIN SCREENS: Screens rendered as items inside the BottomNavBar
        val mainScreens = listOf(HomeScreen, TargetsScreen, SocialScreen, ProgressScreen)

        // TOP-LEVEL SCREENS: Screens/routes where the BottomNavBar is visible.
            // It can include all mainScreens plus other screens if needed in the future.
        val topLevelScreens = mainScreens + listOf<AppScreens>(ProfileScreen)
    }
}