package es.uc3m.android.a1percent.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.TaskDeadline
import es.uc3m.android.a1percent.data.model.enums.TaskStatus
import es.uc3m.android.a1percent.data.remote.FirestoreMapper
import es.uc3m.android.a1percent.data.remote.encodeToMap
import es.uc3m.android.a1percent.data.remote.toObjectSerializable
import es.uc3m.android.a1percent.data.remote.toObjectsSerializable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object TaskRespository {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val tasksCollection = db.collection("tasks")

    // CREATE Task in Firestore database
    suspend fun saveTask(userId: String, task: Task): Result<Unit> {
        return try {
            val taskRef = tasksCollection.document()
            val finalTask = task.copy(
                id = taskRef.id,
                ownerId = userId,
                sharedWith = listOf(userId)
            )
            taskRef.set(finalTask.encodeToMap()!!).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observes tasks shared with the user in real-time.
     */
    fun observeTasks(userId: String): Flow<List<Task>> = callbackFlow {
        val registration = tasksCollection
            .whereArrayContains("sharedWith", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val sorted = snapshot.toObjectsSerializable<Task>()
                        .sortedWith(TaskDeadlineResolver.taskDeadlineComparator())
                    trySend(sorted)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun getTasks(userId: String): Result<List<Task>> {
        return try {
            val snapshot = tasksCollection
                .whereArrayContains("sharedWith", userId)
                .get()
                .await()
            Result.success(
                snapshot.toObjectsSerializable<Task>()
                    .sortedWith(TaskDeadlineResolver.taskDeadlineComparator())
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            tasksCollection.document(taskId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTaskStatus(taskId: String, status: TaskStatus): Result<Unit> {
        return try {
            val snapshot = tasksCollection.document(taskId).get().await()
            val currentTask = snapshot.toObjectSerializable<Task>()
                ?: throw IllegalStateException("Task $taskId not found")

            val updatedTask = currentTask.copy(
                status = status,
                completedAt = if (status == TaskStatus.COMPLETED) System.currentTimeMillis() else null
            )
            tasksCollection.document(taskId).set(updatedTask.encodeToMap()!!).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleTaskCompletion(taskId: String, userId: String): Result<Unit> {
        return try {
            val snapshot = tasksCollection.document(taskId).get().await()
            val currentTask = snapshot.toObjectSerializable<Task>()
                ?: throw IllegalStateException("Task $taskId not found")

            val isCompleted = userId in currentTask.completedBy
            val completedByUpdate = if (isCompleted) FieldValue.arrayRemove(userId) else FieldValue.arrayUnion(userId)
            val completedAtUpdate: Any = if (!isCompleted) System.currentTimeMillis() else FieldValue.delete()
            tasksCollection.document(taskId).update(
                mapOf("completedBy" to completedByUpdate, "completedAt" to completedAtUpdate)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveTaskBatch(userId: String, tasks: List<Task>): Result<Unit> {
        return try {
            val batch = db.batch()
            tasks.forEach { task ->
                val taskRef = tasksCollection.document()
                val finalTask = task.copy(
                    id = taskRef.id,
                    ownerId = userId,
                    sharedWith = listOf(userId)
                )
                batch.set(taskRef, finalTask.encodeToMap()!!)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTaskDeadline(taskId: String, newDeadline: TaskDeadline): Result<Unit> {
        return try {
            val deadlineMap = FirestoreMapper.encodeToMap(newDeadline)
            tasksCollection.document(taskId).update("deadline", deadlineMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTask(task: Task): Result<Unit> {
        return try {
            tasksCollection.document(task.id).set(task.encodeToMap()!!).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateXpAwarded(taskId: String, xpAwarded: Int?): Result<Unit> {
        return try {
            val value: Any = xpAwarded ?: FieldValue.delete()
            tasksCollection.document(taskId).update("xpAwarded", value).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTask(taskId: String): Result<Task?> {
        return try {
            val doc = tasksCollection.document(taskId).get().await()
            Result.success(doc.toObjectSerializable<Task>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun shareTask(taskId: String, friendUserId: String): Result<Unit> {
        return try {
            tasksCollection.document(taskId)
                .update("sharedWith", FieldValue.arrayUnion(friendUserId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
