package es.uc3m.android.a1percent.data.model

import es.uc3m.android.a1percent.data.model.enums.MissionFeedback
import es.uc3m.android.a1percent.data.model.enums.TaskStatus
import es.uc3m.android.a1percent.data.model.enums.TaskType
import java.util.UUID

/**
 * Pure model for standalone tasks and goal mission steps.
 * Next step: add repository mapping or Room annotations when persistence is introduced.
 */
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val type: TaskType,
    val difficulty: Int,
    val xp: Int,
    val energyCost: Int,
    val deadline: Long? = null,
    val status: TaskStatus = TaskStatus.PENDING,
    val goalId: String? = null, // If it has a value, this task is a Mission derived from a Goal
    val isAiGenerated: Boolean = false,
    val order: Int? = null,
    val microfeedback: MissionFeedback? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(difficulty in 1..5) { "Task difficulty must be between 1 and 5" }
    }
}
