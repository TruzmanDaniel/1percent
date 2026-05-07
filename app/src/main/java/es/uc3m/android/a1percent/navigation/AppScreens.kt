package es.uc3m.android.a1percent.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import es.uc3m.android.a1percent.R

sealed class AppScreens(
    val route: String,
    @StringRes val labelRes: Int? = null,
    val icon: ImageVector? = null
) {

    object SplashScreen  : AppScreens("splash")
    object LoginScreen   : AppScreens("login",    R.string.nav_login_label)
    object RegisterScreen : AppScreens("register", R.string.nav_register_label)
    object HomeScreen    : AppScreens("home",     R.string.nav_home_label,     Icons.Default.Home)
    object ProfileScreen : AppScreens("profile",  R.string.nav_profile_label)

    object TargetsScreen : AppScreens("targets",  R.string.nav_targets_label,  Icons.Default.Flag)
    object SocialScreen  : AppScreens("social",   R.string.nav_social_label,   Icons.Default.Group)
    object ProgressScreen: AppScreens("progress", R.string.nav_progress_label, Icons.Default.BarChart)
    object RitualScreen : AppScreens("ritual", R.string.nav_ritual_label)

    companion object {

        // MAIN SCREENS: the ones that appear in the Bottom Nav Bar
        val mainScreens = listOf(HomeScreen, TargetsScreen, SocialScreen, ProgressScreen)

        // TOP-LEVEL SCREENS: show TopNavBar (able to navigate to profile page) and BottomNavBar
        val topLevelScreens = mainScreens + listOf<AppScreens>()
    }
}
