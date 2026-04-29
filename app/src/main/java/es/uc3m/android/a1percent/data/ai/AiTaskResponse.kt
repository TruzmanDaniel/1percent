package es.uc3m.android.a1percent.data.ai

import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.enums.Category
import es.uc3m.android.a1percent.data.model.enums.TaskType
import kotlinx.serialization.Serializable

@Serializable
data class AiTaskListResponse(
    val tasks: List<AiTaskResponse>
)

@Serializable
data class AiTaskResponse(
    val title: String,
    val description: String = "",
    val xp: Int,
    val difficulty: Int,
    val dayIndex: Int
) {
    fun toTask(goalId: String, weekNumber: Int, category: Category): Task {
        return Task(
            title = title,
            description = description,
            type = TaskType.DAILY,
            difficulty = difficulty.coerceIn(1, 5),
            xp = xp,
            energyCost = null,
            category = category,
            goalId = goalId,
            isAiGenerated = true,
            dayIndex = dayIndex,
            weekNumber = weekNumber
        )
    }
}
