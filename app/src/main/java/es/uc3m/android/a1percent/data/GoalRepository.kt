package es.uc3m.android.a1percent.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.remote.encodeToMap
import es.uc3m.android.a1percent.data.remote.toObjectSerializable
import es.uc3m.android.a1percent.data.remote.toObjectsSerializable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object GoalRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val goalsCollection = db.collection("goals")

    suspend fun saveGoal(userId: String, goal: Goal): Result<Unit> {
        return try {
            val goalRef = goalsCollection.document(goal.id)
            val finalGoal = goal.copy(
                ownerId = userId,
                sharedWith = listOf(userId)
            )
            goalRef.set(finalGoal.encodeToMap()!!).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

        // Keeps listening Firestore changes in real-time
    fun observeGoals(userId: String): Flow<List<Goal>> = callbackFlow {
        val registration = goalsCollection
            .whereArrayContains("sharedWith", userId) // Observe also goals shared with user
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjectsSerializable<Goal>())
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun getGoals(userId: String): Result<List<Goal>> {
        return try {
            val snapshot = goalsCollection
                .whereArrayContains("sharedWith", userId)
                .get()
                .await()
            Result.success(snapshot.toObjectsSerializable<Goal>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGoalById(goalId: String): Result<Goal?> {
        return try {
            val doc = goalsCollection.document(goalId).get().await()
            Result.success(doc.toObjectSerializable<Goal>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGoal(goal: Goal): Result<Unit> {
        return try {
            goalsCollection.document(goal.id).set(goal.encodeToMap()!!).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGoalWithMissions(goalId: String): Result<Unit> {
        return try {
            val tasksSnapshot = FirebaseFirestore.getInstance()
                .collection("tasks")
                .whereEqualTo("goalId", goalId)
                .get()
                .await()

            val batch = db.batch()
            tasksSnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.delete(goalsCollection.document(goalId))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun shareGoal(goalId: String, friendUserId: String): Result<Unit> {
        return try {
            goalsCollection.document(goalId)
                .update("sharedWith", FieldValue.arrayUnion(friendUserId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
