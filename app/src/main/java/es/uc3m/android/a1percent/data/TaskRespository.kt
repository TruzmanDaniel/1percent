package es.uc3m.android.a1percent.data

import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.TaskDeadline
import es.uc3m.android.a1percent.data.model.enums.Category
import es.uc3m.android.a1percent.data.model.enums.TaskStatus
import es.uc3m.android.a1percent.data.model.enums.TaskType
import kotlinx.coroutines.tasks.await

object TaskRespository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private fun userTasksCollection(userId: String) =
        db.collection("users").document(userId).collection("tasks")

    suspend fun saveTask(userId: String, task: Task): Result<Unit> {
        return try {
            val data = mapOf(
                "id" to task.id,
                "title" to task.title,
                "description" to task.description,
                "type" to task.type.name,
                "difficulty" to task.difficulty,
                "xp" to task.xp,
                "energyCost" to task.energyCost,
                "status" to task.status.name,
                "goalId" to task.goalId,
                "isAiGenerated" to task.isAiGenerated,
                "createdAt" to task.createdAt,
                "deadlineType" to when (task.deadline) {
                    null -> null
                    TaskDeadline.ThisWeek -> "THIS_WEEK"
                    is TaskDeadline.OnDate -> "ON_DATE"
                },
                "deadlineEpochDay" to when (val d = task.deadline) {
                    is TaskDeadline.OnDate -> d.epochDay
                    else -> null
                }
            )
            userTasksCollection(userId).document(task.id).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTasks(userId: String): Result<List<Task>> {
        return try {
            val snapshot = userTasksCollection(userId).get().await()
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
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        deadline = deadline
                    )
                } catch (e: Exception) { null }
            }
            Result.success(tasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}