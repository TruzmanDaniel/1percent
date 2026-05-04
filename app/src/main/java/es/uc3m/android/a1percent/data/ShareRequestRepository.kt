package es.uc3m.android.a1percent.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.a1percent.data.model.ShareRequest
import es.uc3m.android.a1percent.data.remote.encodeToMap
import es.uc3m.android.a1percent.data.remote.toObjectsSerializable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object ShareRequestRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val collection = db.collection("shareRequests")

    suspend fun createRequest(
        fromUserId: String,
        fromUserName: String,
        toUserId: String,
        itemId: String,
        itemType: String,
        itemTitle: String
    ): Result<Unit> {
        return try {
            val ref = collection.document()
            val request = ShareRequest(
                id = ref.id,
                fromUserId = fromUserId,
                fromUserName = fromUserName,
                toUserId = toUserId,
                itemId = itemId,
                itemType = itemType,
                itemTitle = itemTitle,
                createdAt = System.currentTimeMillis()
            )
            ref.set(request.encodeToMap()!!).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ShareRequestRepo", "createRequest failed", e)
            Result.failure(e)
        }
    }

    fun observeIncomingRequests(userId: String): Flow<List<ShareRequest>> = callbackFlow {
        val registration = collection
            .whereEqualTo("toUserId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) trySend(snapshot.toObjectsSerializable<ShareRequest>())
            }
        awaitClose { registration.remove() }
    }

    suspend fun acceptRequest(request: ShareRequest): Result<Unit> {
        return try {
            when (request.itemType) {
                "TASK" -> TaskRespository.shareTask(request.itemId, request.toUserId).getOrThrow()
                "GOAL" -> GoalRepository.shareGoal(request.itemId, request.toUserId).getOrThrow()
            }
            collection.document(request.id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun denyRequest(requestId: String): Result<Unit> {
        return try {
            collection.document(requestId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
