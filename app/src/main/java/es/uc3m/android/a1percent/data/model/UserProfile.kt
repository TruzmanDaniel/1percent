package es.uc3m.android.a1percent.data.model

/**
 * Pure user profile model.
 * Next step: connect this model to profile state, repository and remote/local sources.
 */
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val password: String = "", // Added for local logic, usually handled by Firebase Auth
    val level: Int = 1,
    val currentXp: Int = 0,
    val xpToNextLevel: Int = 100,
    val avatarUrl: String? = null,
    val streakDays: Int = 0,
    val totalTasksCompleted: Int = 0
)
