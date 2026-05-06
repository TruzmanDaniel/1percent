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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await

/**
 * Singleton repository acting as the "Friendship Table" for the application.
 * Manages relations between user IDs in Firestore.
 */

object SocialRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val relationshipsCollection = db.collection("relationships")

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

    // List of CONFIRMED friends
    fun observeFriends(userId: String): Flow<List<UserProfile>> {
        return combine(friendshipTable, UserRepository.allUsers) { relations, users ->
            val friendIds = relations
                .filter { rel ->
                    rel.status == RelationshipStatus.FRIENDS &&
                    (rel.userAId == userId || rel.userBId == userId)
                }
                .map { rel ->
                    if (rel.userAId == userId) rel.userBId else rel.userAId
                }
            friendIds.mapNotNull { id -> users.find { it.id == id } }
        }
    }

    // List of RECEIVED pending requests (user is userBId)
    fun observePendingRequests(userId: String): Flow<List<UserProfile>> {
        return combine(friendshipTable, UserRepository.allUsers) { relations, users ->
            val requesterIds = relations
                .filter { rel ->
                    rel.status == RelationshipStatus.PENDING && rel.userBId == userId
                }
                .map { it.userAId }
            requesterIds.mapNotNull { id -> users.find { it.id == id } }
        }
    }

    fun areFriends(user1Id: String, user2Id: String): Boolean {
        return _friendshipTable.value.any { rel ->
            rel.status == RelationshipStatus.FRIENDS &&
            ((rel.userAId == user1Id && rel.userBId == user2Id) ||
             (rel.userAId == user2Id && rel.userBId == user1Id))
        }
    }

    fun getRelationshipBetween(user1Id: String, user2Id: String): UserRelationship? {
        return _friendshipTable.value.find { rel ->
            (rel.userAId == user1Id && rel.userBId == user2Id) ||
            (rel.userAId == user2Id && rel.userBId == user1Id)
        }
    }

    private fun hasPendingRequest(user1Id: String, user2Id: String): Boolean {
        return _friendshipTable.value.any { rel ->
            rel.status == RelationshipStatus.PENDING &&
            ((rel.userAId == user1Id && rel.userBId == user2Id) ||
             (rel.userAId == user2Id && rel.userBId == user1Id))
        }
    }

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

    suspend fun acceptFriendRequest(userA: String, userB: String) {
        val docId = if (userA < userB) "${userA}_${userB}" else "${userB}_${userA}"
        try {
            relationshipsCollection.document(docId).update("status", RelationshipStatus.FRIENDS.name).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun rejectFriendRequest(userA: String, userB: String) {
        val docId = if (userA < userB) "${userA}_${userB}" else "${userB}_${userA}"
        try {
            relationshipsCollection.document(docId).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
