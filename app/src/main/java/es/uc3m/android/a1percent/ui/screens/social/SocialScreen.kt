package es.uc3m.android.a1percent.ui.screens.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.a1percent.data.model.UserProfile
import es.uc3m.android.a1percent.navigation.AppScreens

private enum class SocialSection(val label: String) {
    // Secciones de Social por el momento, podemos cambiarlas a futuro
    COMMUNITY("Community"),
    FRIENDS("Friends"),
    GROUPS("Groups")
}

@Composable
fun SocialScreen(
    navController: NavController,
    viewModel: SocialViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedSection by rememberSaveable { mutableStateOf(SocialSection.COMMUNITY) } // Debería estar en ViewModel?

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        SectionSelector(
            selected = selectedSection,
            onSelect = { selectedSection = it }, // function: update value of selectedSection with passed value it
            modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
        )

        when (selectedSection) {

            SocialSection.FRIENDS -> FriendsSection(
                friends = uiState.friends,
                onProfileClick = { friendId ->
                    navController.navigate(AppScreens.ProfileScreen.route + "/$friendId")
                }
            )

            SocialSection.COMMUNITY -> CommunitySection(
                uiState = uiState,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onProfileClick = { userId ->
                    navController.navigate(AppScreens.ProfileScreen.route + "/$userId")
                },
                onSendFriendRequest = viewModel::sendFriendRequest
            )

            SocialSection.GROUPS -> GroupsSection(
            )
        }
    }
}

@Composable
private fun SectionSelector(
    selected: SocialSection,
    onSelect: (SocialSection) -> Unit, // funcion que recibe SocialSection (ese valor recibido es 'it' al llamarla)
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Create a Chip for each SocialSection (automatically creates chips depending on the created enum)
        SocialSection.entries.forEach { section ->
            FilterChip(
                selected = selected == section,
                onClick = { onSelect(section) }, // passed value is clicked 'section'
                label = { Text(section.label) }
            )
        }
    }
}

@Composable
private fun FriendsSection(
    friends: List<UserProfile>,
    onProfileClick: (String) -> Unit
) {
    Text(
        text = "My Friends",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    if (friends.isEmpty()) {
        Text(
            text = "You don't have friends yet. Go to Community to discover people.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 24.dp)
        )
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(friends) { friend ->
            UserListItem(
                user = friend,
                onProfileClick = { onProfileClick(friend.id) }
            )
        }
    }
}

@Composable
private fun CommunitySection(
    uiState: SocialUiState,
    onSearchQueryChange: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onSendFriendRequest: (String) -> Unit
) {
    Text(
        text = "Community",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Find people by name...") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        shape = MaterialTheme.shapes.large,
        singleLine = true
    )

    if (uiState.searchQuery.length < 2) {
        Text(
            text = "Type at least 2 characters to search users.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        return
    }

    if (uiState.searchResults.isEmpty()) {
        Text(
            text = "No users found for \"${uiState.searchQuery}\".",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(uiState.searchResults) { user ->
            UserListItem(
                user = user,
                onProfileClick = { onProfileClick(user.id) },
                // TODO: request button only if its not a friend, if its a friend icon of already friends
                trailingIcon = {
                    IconButton(onClick = { onSendFriendRequest(user.id) }) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = "Add Friend",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun GroupsSection() {
    // ACTUALLY HARD-CODED / MOCK --> we have to create a class object of groups (Future task)
    Text(
        text = "Groups",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Study Sprint", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Shared goals and weekly check-ins.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Fitness Crew", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Track habits and challenge streaks together.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// DISPLAY of USERS PREVIEW
@Composable
fun UserListItem(
    user: UserProfile,
    onProfileClick: () -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val progress = if (user.xpToNextLevel > 0) user.currentXp.toFloat() / user.xpToNextLevel else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProfileClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Avatar Circle & Username
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = user.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                // User info/stats
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .weight(1f)
                ) {
                    Text(text = user.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        text = "Lvl ${user.level} • ${user.streakDays} day streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (trailingIcon != null) {
                    trailingIcon()
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar for Level
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progress to Lvl ${user.level + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${user.currentXp} / ${user.xpToNextLevel} XP",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
