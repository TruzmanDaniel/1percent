package es.uc3m.android.a1percent.data.model

import com.google.firebase.firestore.Exclude
import es.uc3m.android.a1percent.data.model.enums.MissionFeedback
import es.uc3m.android.a1percent.data.model.enums.Category
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
    val energyCost: Int?,

    @get:Exclude
    val deadline: TaskDeadline? = null,  // null or TaskDeadLine (ThisWeek or OnDate)
    val status: TaskStatus = TaskStatus.PENDING,

    // AUTOMATIC: the app will infer the best predefined category in a future iteration.
    val category: Category = Category.AUTOMATIC,
    val customCategoryName: String? = null, // If not null, this task uses a user-custom category

    val goalId: String? = null, // If it has a value, this task is a Mission derived from a Goal
    val isAiGenerated: Boolean = false,
    val order: Int? = null,
    val microfeedback: MissionFeedback? = null,
    val createdAt: Long = System.currentTimeMillis()
) {

    // Firestore-friendly derived fields. They let us save `Task` directly while keeping
    // `deadline` as the domain-friendly representation used by the UI and sorting logic
    @Suppress("unused")
    val deadlineType: String?
        get() = when (deadline) {
            null -> null
            TaskDeadline.ThisWeek -> "THIS_WEEK"
            is TaskDeadline.OnDate -> "ON_DATE"
        }

    @Suppress("unused")
    val deadlineEpochDay: Long?
        get() = (deadline as? TaskDeadline.OnDate)?.epochDay

    init {
        require(difficulty in 1..5) { "Task difficulty must be between 1 and 5" }
        require(customCategoryName?.isNotBlank() != false) {
            "customCategoryName cannot be blank"
        }
    }
}
