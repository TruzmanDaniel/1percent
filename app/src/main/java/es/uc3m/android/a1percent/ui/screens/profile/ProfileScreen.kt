package es.uc3m.android.a1percent.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.navigation.AppScreens

@Composable
fun ProfileScreen(navController: NavController, text: String?, viewModel: ProfileViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Trigger user loading when the screen is shown or 'text' (userId) changes
    LaunchedEffect(text) {
        viewModel.loadUser(text)
    }

    ProfileBodyContent(navController, uiState)
}

@Composable
fun ProfileBodyContent(navController: NavController, uiState: ProfileUiState) {
    val user = uiState.user
    val isOwn = uiState.isOwnProfile

    if (user == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Profile not available")
        }
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // SECTION 1: AVATAR + NAME
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile avatar",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineMedium
            )
            
            if (!isOwn) {
                Text(
                    text = "Viewing Public Profile",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        HorizontalDivider()

        // SECTION 2: STATISTICS
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Stats",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            StatRow(label = "Level", value = user.level.toString())
            StatRow(label = "XP", value = "${user.currentXp} / ${user.xpToNextLevel}")
            StatRow(label = "🔥 Streak", value = "${user.streakDays} days")
            StatRow(label = "Tasks Completed", value = user.totalTasksCompleted.toString())
        }

        HorizontalDivider()

        Spacer(modifier = Modifier.weight(1f))

        // ACTIONS BASED ON PROFILE OWNERSHIP
        if (isOwn) {
            // LOGOUT BUTTON (Only for own profile)
            Button(
                onClick = {
                    SessionRepository.logout()
                    navController.navigate(AppScreens.LoginScreen.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("Logout")
            }
        } else {
            // BACK BUTTON (For other profiles)
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Community")
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
