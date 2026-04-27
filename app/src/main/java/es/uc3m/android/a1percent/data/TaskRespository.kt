package es.uc3m.android.a1percent.data

import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.TaskDeadline
import es.uc3m.android.a1percent.data.model.enums.MissionFeedback
import es.uc3m.android.a1percent.data.model.enums.TaskStatus
import es.uc3m.android.a1percent.data.model.enums.TaskType
import kotlinx.coroutines.tasks.await

object TaskRespository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private fun userTasksCollection(userId: String) =
        db.collection("users").document(userId).collection("tasks")

    suspend fun saveTask(userId: String, task: Task): Result<Unit> {
        return try {
            val data = buildMap<String, Any?> {
                put("id", task.id)
                put("title", task.title)
                put("description", task.description)
                put("type", task.type.name)
                put("difficulty", task.difficulty)
                put("xp", task.xp)
                put("energyCost", task.energyCost)
                put("status", task.status.name)
                put("goalId", task.goalId)
                put("isAiGenerated", task.isAiGenerated)
                put("order", task.order)
                put("microfeedback", task.microfeedback?.name)
                put("createdAt", task.createdAt)
                put("category", task.category.name)
                put("customCategoryName", task.customCategoryName)

                when (val deadline = task.deadline) {
                    null -> Unit
                    TaskDeadline.ThisWeek -> {
                        put("deadlineType", "THIS_WEEK")
                    }
                    is TaskDeadline.OnDate -> {
                        put("deadlineType", "ON_DATE")
                        put("deadlineEpochDay", deadline.epochDay)
                    }
                }
            }
            userTasksCollection(userId).document(task.id).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTasks(userId: String): Result<List<Task>> {
        return try {
                // Snapshot: copy in memory of the query result
            val snapshot = userTasksCollection(userId).get().await()    //await: suspends execution until async operation ends, doesnt block main flow

            val tasks = snapshot.documents.mapNotNull { doc ->
                try {
                    val deadlineType = doc.getString("deadlineType")
                    val deadline = when (deadlineType) {
                        "THIS_WEEK" -> TaskDeadline.ThisWeek
                        "ON_DATE" -> TaskDeadline.OnDate(doc.getLong("deadlineEpochDay") ?: 0L)
                        else -> null
                    }
                    Task(
                        id = doc.getString("id") ?: doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        type = TaskType.valueOf(doc.getString("type") ?: "ONE_TIME"),
                        difficulty = doc.getLong("difficulty")?.toInt() ?: 1,
                        xp = doc.getLong("xp")?.toInt() ?: 0,
                        energyCost = doc.getLong("energyCost")?.toInt(),
                        status = TaskStatus.valueOf(doc.getString("status") ?: "PENDING"),
                        goalId = doc.getString("goalId"),
                        isAiGenerated = doc.getBoolean("isAiGenerated") ?: false,
                        order = doc.getLong("order")?.toInt(),
                        microfeedback = doc.getString("microfeedback")?.let {
                            runCatching { MissionFeedback.valueOf(it) }.getOrNull()
                        },
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        deadline = deadline,
                        category = doc.getString("category")?.let {
                            runCatching { es.uc3m.android.a1percent.data.model.enums.Category.valueOf(it) }.getOrNull()
                        } ?: es.uc3m.android.a1percent.data.model.enums.Category.AUTOMATIC,
                        customCategoryName = doc.getString("customCategoryName")
                    )
                } catch (_: Exception) { null }
            }
            Result.success(tasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}