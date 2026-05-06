package es.uc3m.android.a1percent.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import es.uc3m.android.a1percent.data.model.MilestoneRecord
import es.uc3m.android.a1percent.data.remote.encodeToMap
import es.uc3m.android.a1percent.data.remote.toObjectsSerializable
import kotlinx.coroutines.tasks.await

object MilestoneRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private fun milestonesCollection(goalId: String) =
        db.collection("goals").document(goalId).collection("milestones")

    suspend fun saveMilestone(goalId: String, record: MilestoneRecord): Result<Unit> {
        return try {
            milestonesCollection(goalId)
                .document(record.id)
                .set(record.encodeToMap()!!)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMilestones(goalId: String): Result<List<MilestoneRecord>> {
        return try {
            val snapshot = milestonesCollection(goalId)
                .orderBy("unlockedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            Result.success(snapshot.toObjectsSerializable<MilestoneRecord>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
