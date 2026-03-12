package es.uc3m.android.a1percent.data.model

import es.uc3m.android.a1percent.data.model.enums.Category
import es.uc3m.android.a1percent.data.model.enums.GoalStatus
import java.util.UUID

/**
 * Pure model for user goals.
 * Next step: map this model to persistence/network DTOs when data layer is added.
 */
data class Goal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val category: Category,
    val difficulty: Int,
    val xp: Int,
    val deadline: Long? = null,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val progress: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(difficulty in 1..5) { "Goal difficulty must be between 1 and 5" }
        require(progress in 0..100) { "Goal progress must be between 0 and 100" }
    }
}
