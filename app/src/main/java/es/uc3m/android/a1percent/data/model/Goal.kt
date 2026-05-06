package es.uc3m.android.a1percent.data.model

import es.uc3m.android.a1percent.data.model.enums.AiRoadmapStatus
import es.uc3m.android.a1percent.data.model.enums.Category
import es.uc3m.android.a1percent.data.model.enums.GoalStatus
import es.uc3m.android.a1percent.data.model.enums.GoalType
import es.uc3m.android.a1percent.data.model.enums.PausedBy
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.UUID

@Serializable
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
    val createdAt: Long = System.currentTimeMillis(),
    val ownerId: String = "",
    val sharedWith: List<String> = emptyList(),
    val currentIntensity: Float = difficulty.toFloat(),
    val nextGenerationDate: Long? = null,
    val aiRoadmapStatus: AiRoadmapStatus = AiRoadmapStatus.NONE,
    val weeklyStreak: Int = 0,
    val extensionCount: Int = 0,
    val streakStartDate: Long? = null,
    val pausedBy: PausedBy? = null
) {
    init {
        require(difficulty in 1..5) { "Goal difficulty must be between 1 and 5" }
        require(progress in 0..100) { "Goal progress must be between 0 and 100" }
    }

    @Transient
    val goalType: GoalType = if (deadline != null) GoalType.FINITE else GoalType.INFINITE
}
