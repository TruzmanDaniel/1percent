package es.uc3m.android.a1percent.data

import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.enums.TaskStatus
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
                    trySend(snapshot.toObjectsSerializable<Task>())
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
            Result.success(snapshot.toObjectsSerializable<Task>())
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

            val updatedTask = currentTask.copy(status = status)
            tasksCollection.document(taskId).set(updatedTask.encodeToMap()!!).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
