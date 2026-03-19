package es.uc3m.android.a1percent.data.model.enums

/**
 * Status of the friendship between two users.
 */
enum class RelationshipStatus {
    PENDING, // Request sent but not accepted
    FRIENDS, // Mutual friendship
    BLOCKED  // Relationship restricted
}

