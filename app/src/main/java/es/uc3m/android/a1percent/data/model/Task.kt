package es.uc3m.android.a1percent.data.model

import es.uc3m.android.a1percent.data.model.enums.MissionFeedback
import es.uc3m.android.a1percent.data.model.enums.Category
import es.uc3m.android.a1percent.data.model.enums.TaskStatus
import es.uc3m.android.a1percent.data.model.enums.TaskType
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Pure model for standalone tasks and goal mission steps.
 * Next step: add repository mapping or Room annotations when persistence is introduced.
 */
@Serializable
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val type: TaskType,
    val difficulty: Int,
    val xp: Int,
    val energyCost: Int?,

    val deadline: TaskDeadline? = null,  // null or TaskDeadLine (ThisWeek or OnDate)
    val status: TaskStatus = TaskStatus.PENDING,

    val category: Category = Category.PERSONAL,
    val customCategoryName: String? = null, // If not null, this task uses a user-custom category

    val goalId: String? = null, // If it has a value, this task is a Mission derived from a Goal
    val isAiGenerated: Boolean = false,
    val order: Int? = null,
    val microfeedback: MissionFeedback? = null,
    val dayIndex: Int? = null,
    val weekNumber: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val ownerId: String = "",
    val sharedWith: List<String> = emptyList(),
    val completedBy: List<String> = emptyList(),
    val completedAt: Long? = null
) {

    init {
        require(difficulty in 1..5) { "Task difficulty must be between 1 and 5" }
        require(customCategoryName?.isNotBlank() != false) {
            "customCategoryName cannot be blank"
        }
    }
}
