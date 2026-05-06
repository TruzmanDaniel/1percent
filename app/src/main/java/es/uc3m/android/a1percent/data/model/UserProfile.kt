package es.uc3m.android.a1percent.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val password: String = "",
    val createdAt: Long? = null,
    val level: Int = 1,
    val currentXp: Int = 0,
    val xpToNextLevel: Int = 100,
    val avatarUrl: String? = null,
    val streakDays: Int = 0,
    val totalTasksCompleted: Int = 0,
    val availableCredits: Int = 5,
    val creditsResetDate: Long? = null,
    val lastActivityDate: Long? = null,
    val isVacationMode: Boolean = false,
    val vacationStartDate: Long? = null
)
