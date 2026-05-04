package es.uc3m.android.a1percent.ui.screens.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import es.uc3m.android.a1percent.data.SocialRepository
import es.uc3m.android.a1percent.data.model.UserProfile
import es.uc3m.android.a1percent.navigation.AppScreens
import es.uc3m.android.a1percent.data.model.enums.RelationshipStatus

private enum class SocialSection(val label: String) {
    // COMMUNITY("Community"),
    FRIENDS("Friends"),
    GROUPS("Groups")
}

@Composable
fun SocialScreen(
    navController: NavController,
    initialSection: String = "friends",
    viewModel: SocialViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val startSection = SocialSection.entries.firstOrNull {
        it.name.equals(initialSection, ignoreCase = true)
    } ?: SocialSection.FRIENDS
    var selectedSection by rememberSaveable(initialSection) { mutableStateOf(startSection) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        SectionSelector(
            selected = selectedSection,
            onSelect = { selectedSection = it },
            modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
        )

        when (selectedSection) {
            SocialSection.FRIENDS -> FriendsSection(
                currentUser = uiState.currentUser,
                friends = uiState.friends,
                pendingRequests = uiState.pendingRequests,
                searchQuery = uiState.searchQuery,
                searchResults = uiState.searchResults,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onSendFriendRequest = viewModel::sendFriendRequest,
                onProfileClick = { friendId ->
                    navController.navigate(AppScreens.ProfileScreen.route + "/$friendId")
                },
                onAcceptRequest = viewModel::acceptRequest,
                onRejectRequest = viewModel::rejectRequest
            )

            SocialSection.GROUPS -> GroupsSection()
        }
    }
}

@Composable
private fun SectionSelector(
    selected: SocialSection,
    onSelect: (SocialSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SocialSection.entries.forEach { section ->
            FilterChip(
                selected = selected == section,
                onClick = { onSelect(section) },
                label = { Text(section.label) }
            )
        }
    }
}

@Composable
private fun FriendsSection(
    currentUser: UserProfile?,
    friends: List<UserProfile>,
    pendingRequests: List<UserProfile>,
    searchQuery: String,
    searchResults: List<UserProfile>,
    onSearchQueryChange: (String) -> Unit,
    onSendFriendRequest: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Find people by name...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.large,
                singleLine = true
            )
        }

        if (searchQuery.length < 2) {
            item {
                Text(
                    text = "Type at least 2 characters to search users.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        } else if (searchResults.isEmpty()) {
            item {
                Text(
                    text = "No users found for \"$searchQuery\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        } else {
            item {
                Text(
                    text = "Search results (${searchResults.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(searchResults) { user ->
                val isAlreadyFriend = friends.any { it.id == user.id }
                val currentUserId = currentUser?.id.orEmpty()
                val relationship = if (currentUserId.isNotEmpty()) {
                    SocialRepository.getRelationshipBetween(currentUserId, user.id)
                } else null
                val isIncomingRequest = relationship?.status == RelationshipStatus.PENDING && relationship.userBId == currentUserId && relationship.userAId == user.id
                val isOutgoingRequest = relationship?.status == RelationshipStatus.PENDING && relationship.userAId == currentUserId && relationship.userBId == user.id
                UserListItem(
                    user = user,
                    onProfileClick = { onProfileClick(user.id) },
                    trailingIcon = {
                        if (isAlreadyFriend) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Already friends",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else if (isIncomingRequest) {
                            Row {
                                IconButton(onClick = { onAcceptRequest(user.id) }) {
                                    Icon(Icons.Default.Check, contentDescription = "Accept request", tint = Color(0xFF4CAF50))
                                }
                                IconButton(onClick = { onRejectRequest(user.id) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Reject request", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        } else if (isOutgoingRequest) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.height(36.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Pending",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            IconButton(onClick = { onSendFriendRequest(user.id) }) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = "Send friend request",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
            }
        }

        if (pendingRequests.isNotEmpty()) {
            item {
                Text(
                    text = "Friend Requests (${pendingRequests.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            items(pendingRequests) { requester ->
                UserListItem(
                    user = requester,
                    onProfileClick = { onProfileClick(requester.id) },
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { onAcceptRequest(requester.id) }) {
                                Icon(Icons.Default.Check, contentDescription = "Accept", tint = Color(0xFF4CAF50))
                            }
                            IconButton(onClick = { onRejectRequest(requester.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Reject", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
        }

        item {
            Text(
                text = "My Friends",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        if (friends.isEmpty() && pendingRequests.isEmpty()) {
            item {
                Text(
                    text = "You don't have friends yet. Search users to discover people.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            items(friends) { friend ->
                UserListItem(
                    user = friend,
                    onProfileClick = { onProfileClick(friend.id) }
                )
            }
        }
    }
}


@Composable
private fun GroupsSection() {
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

@Composable
fun UserListItem(
    user: UserProfile,
    onProfileClick: (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val progress = if (user.xpToNextLevel > 0) user.currentXp.toFloat() / user.xpToNextLevel else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onProfileClick != null) Modifier.clickable { onProfileClick() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar: mostrar foto si existe, sino icono default
                if (!user.avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = user.name,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = user.name,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

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
