package es.uc3m.android.a1percent.navigation

/**
 * "Clase Sellada"
 * Used to organize all our screens, and define clearly what components are navigable screens and not just drawable elements
 * Define ROUTES
 *
 * ============
 *  REFERENCES
 * ============
 *
 */

sealed class AppScreens(val route: String) {
    object HomeScreen1: AppScreens("homeScreen")
    object ProfileScreen: AppScreens("profileScreen")

}