package es.uc3m.android.a1percent.ui.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.uc3m.android.a1percent.navigation.AppScreens

// REUSABLE NAV BAR
@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit, // Function defined in NavGraph.kt when we define this parameter
    onAddClick: () -> Unit // Function defining what happens when the add button is clicked
) {
    NavigationBar {

        // DYNAMICALLY Build items only for the main screens shown inside the BottomNavBar
        val screens = AppScreens.mainScreens
        val midIndex = screens.size / 2  // index 2 → after Targets, before Social

        // BOTTOM NAVIGATION BAR BUTTONS in Order (order defined in AppScreens), with ADD Button in the middle
        screens.forEachIndexed { index, screen ->

            // Insert 'Add' button before the middle item
            if (index == midIndex) {
                NavigationBarItem(
                    selected = false,
                    onClick = onAddClick,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Add",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    label = null,
                    alwaysShowLabel = false
                )
            }

            val selected = currentRoute == screen.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(screen.route) },
                icon = {
                    screen.icon?.let {
                        Icon(imageVector = it, contentDescription = screen.label)
                    }
                },
                label = { if (selected) Text(screen.label) },
                alwaysShowLabel = false
            )
        }
    }
}
