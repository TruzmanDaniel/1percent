package es.uc3m.android.a1percent.data

import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.a1percent.data.model.UserProfile
import es.uc3m.android.a1percent.data.model.UserRelationship
import es.uc3m.android.a1percent.data.model.enums.RelationshipStatus
import es.uc3m.android.a1percent.data.remote.encodeToMap
import es.uc3m.android.a1percent.data.remote.toObjectsSerializable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await

/**
 * Singleton repository acting as the "Friendship Table" for the application.
 * Manages relations between user IDs in Firestore.
 */

object SocialRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val relationshipsCollection = db.collection("relationships")

    // Internal "Table" of friendships synced with Firestore
    private val _friendshipTable = MutableStateFlow<List<UserRelationship>>(emptyList())
    val friendshipTable: StateFlow<List<UserRelationship>> = _friendshipTable.asStateFlow()

    init {
        observeRelationships()
    }

    private fun observeRelationships() {
        relationshipsCollection.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val relations = snapshot.toObjectsSerializable<UserRelationship>()
                _friendshipTable.update { relations }
            }
        }
    }

    // Reactive list of friends for a specific user
    fun observeFriends(userId: String): Flow<List<UserProfile>> {
        return friendshipTable.map { relations ->
            relationListToFriendProfiles(relations, userId)
        }
    }

    private fun relationListToFriendProfiles(
        relations: List<UserRelationship>,
        userId: String
    ): List<UserProfile> {
        val friendIds = relations
            .filter { rel ->
                rel.status == RelationshipStatus.FRIENDS && 
                (rel.userAId == userId || rel.userBId == userId)
            }
            .map { rel ->
                if (rel.userAId == userId) rel.userBId else rel.userAId // get the friend's ID from the IDs pair
            }

        return friendIds.mapNotNull { UserRepository.findUserById(it) }
    }

    // Check if two users are currently friends
    fun areFriends(user1Id: String, user2Id: String): Boolean {
        return _friendshipTable.value.any { rel ->
            rel.status == RelationshipStatus.FRIENDS &&
            ((rel.userAId == user1Id && rel.userBId == user2Id) ||
             (rel.userAId == user2Id && rel.userBId == user1Id))
        }
    }

    // Check if pending request exists between two users
    private fun hasPendingRequest(user1Id: String, user2Id: String): Boolean {
        return _friendshipTable.value.any { rel ->
            rel.status == RelationshipStatus.PENDING &&
            ((rel.userAId == user1Id && rel.userBId == user2Id) ||
             (rel.userAId == user2Id && rel.userBId == user1Id))
        }
    }

    // Creates a new row in the friendship table as PENDING.
    suspend fun sendFriendRequest(fromId: String, toId: String) {
        if (fromId == toId) return
        if (areFriends(fromId, toId)) return
        if (hasPendingRequest(fromId, toId)) return
         
        val relationship = UserRelationship(fromId, toId, RelationshipStatus.PENDING)
        val docId = if (fromId < toId) "${fromId}_${toId}" else "${toId}_${fromId}"
        
        try {
            relationshipsCollection.document(docId).set(relationship.encodeToMap()!!).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Updates the status of an existing relationship to FRIENDS.
    suspend fun acceptFriendRequest(userA: String, userB: String) {
        val docId = if (userA < userB) "${userA}_${userB}" else "${userB}_${userA}"
        try {
            relationshipsCollection.document(docId).update("status", RelationshipStatus.FRIENDS.name).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
