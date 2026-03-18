package es.uc3m.android.a1percent.data.model

/**
 * Status of the friendship between two users.
 */
enum class RelationshipStatus {
    PENDING, // Request sent but not accepted
    FRIENDS, // Mutual friendship
    BLOCKED  // Relationship restricted
}

/**
 * Represents a row in the "Friendship Table".
 * Links two users by their unique IDs.
 */
data class UserRelationship(
    val userAId: String,
    val userBId: String,
    val status: RelationshipStatus = RelationshipStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)
