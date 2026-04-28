package es.uc3m.android.a1percent.data.model.enums

import kotlinx.serialization.Serializable

/**
 * Status of the friendship between two users.
 */
@Serializable
enum class RelationshipStatus {
    PENDING, // Request sent but not accepted
    FRIENDS, // Mutual friendship
    BLOCKED  // Relationship restricted
}

