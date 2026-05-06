package es.uc3m.android.a1percent.ui.screens.profile

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.model.enums.RelationshipStatus
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.PaddingValues
import es.uc3m.android.a1percent.navigation.AppScreens
import androidx.compose.material3.Switch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, text: String?, viewModel: ProfileViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(text) {
        viewModel.loadUser(text)
    }

    Scaffold(
        topBar = {
            ProfileTopBar(
                username = uiState.user?.name ?: "Profile",
                onBack = { navController.popBackStack() }
            )
        }
    ) { innerPadding ->
        ProfileBodyContent(
            navController = navController,
            uiState = uiState,
            onPickImage = { uri -> viewModel.uploadProfilePicture(uri) },
            onFriendAction = { viewModel.onFriendAction(uiState.user?.id ?: return@ProfileBodyContent) },
            onAcceptRequest = { viewModel.onAcceptRequest(uiState.user?.id ?: return@ProfileBodyContent) },
            onRejectRequest = { viewModel.onRejectRequest(uiState.user?.id ?: return@ProfileBodyContent) },
            onVacationToggle = { viewModel.onToggleVacationMode() },
            modifier = innerPadding
        )
    }
}

@Composable
fun ProfileBodyContent(
    navController: NavController,
    uiState: ProfileUiState,
    onPickImage: (Uri) -> Unit = {},
    onFriendAction: () -> Unit = {},
    onAcceptRequest: () -> Unit = {},
    onRejectRequest: () -> Unit = {},
    onVacationToggle: () -> Unit = {},
    modifier: PaddingValues = PaddingValues(0.dp)
) {
    val user = uiState.user
    val isOwn = uiState.isOwnProfile
    val context = LocalContext.current

    var showImagePickerDialog by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onPickImage(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { onPickImage(it) }
        }
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

    if (showImagePickerDialog) {
        AlertDialog(
            onDismissRequest = { showImagePickerDialog = false },
            title = { Text("Change Profile Picture") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showImagePickerDialog = false
                            val uri = createTempImageUri(context)
                            cameraImageUri = uri
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Take Photo")
                    }
                    TextButton(
                        onClick = {
                            showImagePickerDialog = false
                            pickImageLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Choose from Gallery")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImagePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(modifier)
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
                                if (isOwn) Modifier.clickable { showImagePickerDialog = true }
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
                                if (isOwn) Modifier.clickable { showImagePickerDialog = true }
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
                            .clickable { showImagePickerDialog = true },
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
            if (isOwn) {
                StatRow(label = "Credits", value = user.availableCredits.toString())
            }
        }

        if (isOwn) {
            val isVacation = user.isVacationMode == true
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isVacation) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            if (isVacation) "Modo Vacaciones activo" else "Modo Vacaciones",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (isVacation && user.vacationStartDate != null) {
                            val days = ((System.currentTimeMillis() - user.vacationStartDate) / (24 * 3600 * 1000)).toInt()
                            Text("Desde hace $days días", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = isVacation,
                        onCheckedChange = { onVacationToggle() }
                    )
                }
            }
        }

        if (!isOwn) {
            val relationship = uiState.relationship
            val isIncomingRequest = relationship?.status == RelationshipStatus.PENDING && relationship.userBId == uiState.currentUserId
            when {
                isIncomingRequest -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onAcceptRequest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Accept request", tint = Color(0xFF4CAF50))
                    }
                    IconButton(
                        onClick = onRejectRequest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Reject request", tint = MaterialTheme.colorScheme.error)
                    }
                }
                relationship == null -> Button(
                    onClick = onFriendAction,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Friend")
                }
                relationship.status == RelationshipStatus.PENDING -> OutlinedButton(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                ) {
                    Text("Request Pending")
                }
                relationship.status == RelationshipStatus.FRIENDS -> OutlinedButton(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                ) {
                    Text("Friends")
                }
                else -> {}
            }
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
                Text("Back")
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

private fun createTempImageUri(context: Context): Uri {
    val tempFile = File.createTempFile("profile_photo_", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
}
