package es.uc3m.android.a1percent.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import es.uc3m.android.a1percent.navigation.AppScreens

// REUSABLE NAV BAR
@Composable
fun BottomNavBar(
    currentRoute: String?,
    isFabExpanded: Boolean,
    onNavigate: (String) -> Unit,
    onAddClick: () -> Unit  // Function when + is clicked
) {
    NavigationBar {
        // DYNAMICALLY Build items only for the main screens shown inside the BottomNavBar
        val screens = AppScreens.mainScreens
        val midIndex = screens.size / 2

        // Rotation animation for the FAB icon
        val rotation by animateFloatAsState(
            targetValue = if (isFabExpanded) 135f else 0f,
            label = "FabRotation"
        )

        screens.forEachIndexed { index, screen ->
            // Insert 'Add' button in the middle
            if (index == midIndex) {
                NavigationBarItem(
                    selected = false,
                    onClick = onAddClick,
                    icon = {
                        Icon(
                            imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.AddCircle,
                            contentDescription = if (isFabExpanded) "Close" else "Add",
                            tint = if (isFabExpanded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(32.dp)
                                .rotate(rotation)
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
