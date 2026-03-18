package es.uc3m.android.a1percent.data

import es.uc3m.android.a1percent.data.model.RelationshipStatus
import es.uc3m.android.a1percent.data.model.UserProfile
import es.uc3m.android.a1percent.data.model.UserRelationship
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Singleton repository acting as the "Friendship Table" for the application.
 * Manages relations between user IDs only, following the decoupling principle.
 */
object SocialRepository {

    // Internal "Table" of friendships. 
    // In a real database, this would be a separate collection/table.
    private val _friendshipTable = MutableStateFlow<List<UserRelationship>>(
        listOf(
            // Initial mock relationships
            UserRelationship("user-1", "user-2", RelationshipStatus.FRIENDS),
            UserRelationship("user-1", "user-3", RelationshipStatus.PENDING)
        )
    )
    val friendshipTable: StateFlow<List<UserRelationship>> = _friendshipTable.asStateFlow()

    /**
     * Finds all friends of a specific user by querying the friendship table.
     * Checks if the user is in either userAId or userBId column.
     */
    fun getFriendsOfUser(userId: String): List<UserProfile> {
        val friendIds = _friendshipTable.value
            .filter { rel ->
                rel.status == RelationshipStatus.FRIENDS && 
                (rel.userAId == userId || rel.userBId == userId)
            }
            .map { rel ->
                if (rel.userAId == userId) rel.userBId else rel.userAId
            }

        return friendIds.mapNotNull { UserRepository.findUserById(it) }
    }

    /**
     * Returns true if two users are currently friends.
     */
    fun areFriends(user1Id: String, user2Id: String): Boolean {
        return _friendshipTable.value.any { rel ->
            rel.status == RelationshipStatus.FRIENDS &&
            ((rel.userAId == user1Id && rel.userBId == user2Id) ||
             (rel.userAId == user2Id && rel.userBId == user1Id))
        }
    }

    /**
     * Creates a new "row" in the friendship table as PENDING.
     */
    fun sendFriendRequest(fromId: String, toId: String) {
        if (areFriends(fromId, toId)) return
        
        _friendshipTable.update { it + UserRelationship(fromId, toId, RelationshipStatus.PENDING) }
    }

    /**
     * Updates the status of an existing relationship to FRIENDS.
     */
    fun acceptFriendRequest(userA: String, userB: String) {
        _friendshipTable.update { list ->
            list.map { rel ->
                if ((rel.userAId == userA && rel.userBId == userB) ||
                    (rel.userAId == userB && rel.userBId == userA)) {
                    rel.copy(status = RelationshipStatus.FRIENDS)
                } else rel
            }
        }
    }
}
