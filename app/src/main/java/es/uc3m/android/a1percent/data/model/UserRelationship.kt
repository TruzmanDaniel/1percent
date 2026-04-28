package es.uc3m.android.a1percent.data.model

import es.uc3m.android.a1percent.data.model.enums.RelationshipStatus
import kotlinx.serialization.Serializable

/**
 * Represents a row in the "Friendship Table".
 * Links two users by their unique IDs.
 */
@Serializable
data class UserRelationship(
    val userAId: String,
    val userBId: String,
    val status: RelationshipStatus = RelationshipStatus.PENDING, // Default value for now
    val createdAt: Long = System.currentTimeMillis()
)
