package es.uc3m.android.a1percent.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import es.uc3m.android.a1percent.data.model.enums.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object TaskCategoryRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _customCategories = MutableStateFlow<List<String>>(emptyList())
    val customCategories: StateFlow<List<String>> = _customCategories.asStateFlow()

    val predefinedCategories: List<Category> = Category.entries.toList()
    val inferenceCandidates: List<Category> = predefinedCategories

    private var activeListener: ListenerRegistration? = null

    init {
        scope.launch {
            SessionRepository.currentUser.collect { user ->
                android.util.Log.d("TaskCategoryRepo", "User changed: ${user?.id}")
                activeListener?.remove()
                activeListener = null
                _customCategories.value = emptyList()

                if (user != null) {
                    android.util.Log.d("TaskCategoryRepo", "Registering listener for user categories: ${user.id}")
                    activeListener = db.collection("users")
                        .document(user.id)
                        .collection("categories")
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                android.util.Log.e("TaskCategoryRepo", "❌ Error in listener: ${error.message}", error)
                                return@addSnapshotListener
                            }
                            
                            if (snapshot != null) {
                                val categories = snapshot.documents.mapNotNull { it.getString("name") }
                                android.util.Log.d("TaskCategoryRepo", "✅ Updated categories: $categories")
                                _customCategories.value = categories
                            }
                        }
                }
            }
        }
    }

    suspend fun addCustomCategory(userId: String, rawName: String): String? {
        val normalized = rawName.trim()
        if (normalized.isEmpty()) return null

        val existing = _customCategories.value.firstOrNull { it.equals(normalized, ignoreCase = true) }
        if (existing != null) return existing

        return try {
            android.util.Log.d("TaskCategoryRepo", "Trying to add custom category: $normalized for user: $userId")
            
            val docRef = db.collection("users").document(userId).collection("categories")
                .add(mapOf("name" to normalized))
                .await()
            
            android.util.Log.d("TaskCategoryRepo", "✅ Successfully added custom category: ${docRef.id}")
            normalized
        } catch (e: Exception) {
            android.util.Log.e("TaskCategoryRepo", "❌ Error adding custom category: ${e.message}", e)
            null
        }
    }

    suspend fun deleteCustomCategory(userId: String, name: String): Result<Unit> {
        return try {
            val snapshot = db.collection("users").document(userId).collection("categories")
                .whereEqualTo("name", name)
                .get()
                .await()
            snapshot.documents.forEach { it.reference.delete().await() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
