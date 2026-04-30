package es.uc3m.android.a1percent.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.navigation.AppScreens

@Composable
fun ProfileScreen(navController: NavController, text: String?, viewModel: ProfileViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(text) {
        viewModel.loadUser(text)
    }

    ProfileBodyContent(navController, uiState, onPickImage = { uri ->
        viewModel.uploadProfilePicture(uri)
    })
}

@Composable
fun ProfileBodyContent(
    navController: NavController,
    uiState: ProfileUiState,
    onPickImage: (android.net.Uri) -> Unit = {}
) {
    val user = uiState.user
    val isOwn = uiState.isOwnProfile

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onPickImage(it) }
    }

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
            // Avatar with optional edit badge
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isUploadingAvatar) {
                    CircularProgressIndicator(modifier = Modifier.size(100.dp))
                } else if (!user.avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = "Profile avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .then(
                                if (isOwn) Modifier.clickable { pickImageLauncher.launch("image/*") }
                                else Modifier
                            )
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile avatar",
                        modifier = Modifier
                            .size(100.dp)
                            .then(
                                if (isOwn) Modifier.clickable { pickImageLauncher.launch("image/*") }
                                else Modifier
                            ),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Camera badge for own profile
                if (isOwn && !uiState.isUploadingAvatar) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.BottomEnd)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable { pickImageLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change photo",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

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

            uiState.uploadError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
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
            StatRow(label = "Streak", value = "${user.streakDays} days")
            StatRow(label = "Tasks Completed", value = user.totalTasksCompleted.toString())
        }

        HorizontalDivider()

        Spacer(modifier = Modifier.weight(1f))

        // ACTIONS BASED ON PROFILE OWNERSHIP
        if (isOwn) {
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
