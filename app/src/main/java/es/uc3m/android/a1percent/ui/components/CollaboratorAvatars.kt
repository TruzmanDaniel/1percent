package es.uc3m.android.a1percent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import es.uc3m.android.a1percent.data.model.UserProfile

@Composable
fun CollaboratorAvatars(
    sharedWith: List<String>,
    currentUserId: String,
    friends: List<UserProfile>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 3,
    avatarSize: Int = 24
) {
    val collaboratorIds = sharedWith.filter { it != currentUserId }
    if (collaboratorIds.isEmpty()) return

    val resolved = collaboratorIds.mapNotNull { id -> friends.find { it.id == id } }
    if (resolved.isEmpty()) return

    val visible = resolved.take(maxVisible)
    val overflow = resolved.size - maxVisible

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((-6).dp)
    ) {
        visible.forEach { user ->
            Box(
                modifier = Modifier
                    .size(avatarSize.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!user.avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = user.name,
                        modifier = Modifier.size(avatarSize.dp).clip(CircleShape)
                    )
                } else {
                    Text(
                        text = user.name.take(1).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        if (overflow > 0) {
            Box(
                modifier = Modifier
                    .size(avatarSize.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$overflow",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}
