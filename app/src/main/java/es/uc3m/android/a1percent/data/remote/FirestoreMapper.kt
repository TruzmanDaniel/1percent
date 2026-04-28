package es.uc3m.android.a1percent.data.remote

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.serialization.json.*

object FirestoreMapper {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
        isLenient = true
        allowSpecialFloatingPointValues = true
        classDiscriminator = "type" 
    }

    inline fun <reified T> encodeToMap(value: T): Map<String, Any?>? {
        return try {
            val jsonElement = json.encodeToJsonElement(value)
            jsonElement.toFirestoreObject() as? Map<String, Any?>
        } catch (e: Exception) {
            Log.e("FirestoreMapper", "Error encoding", e)
            null
        }
    }

    inline fun <reified T> decodeFromMap(map: Map<String, Any?>): T? {
        return try {
            val jsonElement = map.toFirestoreJsonElement()
            json.decodeFromJsonElement<T>(jsonElement)
        } catch (e: Exception) {
            Log.e("FirestoreMapper", "Error decoding", e)
            null
        }
    }

    fun JsonElement.toFirestoreObject(): Any? {
        return when (this) {
            is JsonObject -> this.mapValues { it.value.toFirestoreObject() }
            is JsonArray -> this.map { it.toFirestoreObject() }
            is JsonPrimitive -> {
                when {
                    this.isString -> this.content
                    this.booleanOrNull != null -> this.booleanOrNull
                    this.longOrNull != null -> this.longOrNull
                    this.doubleOrNull != null -> this.doubleOrNull
                    else -> this.content
                }
            }
            JsonNull -> null
        }
    }

    fun Any?.toFirestoreJsonElement(): JsonElement {
        return when (this) {
            is Map<*, *> -> JsonObject(this.map { it.key.toString() to it.value.toFirestoreJsonElement() }.toMap())
            is List<*> -> JsonArray(this.map { it.toFirestoreJsonElement() })
            is String -> {
                if (this == "null") JsonNull
                else JsonPrimitive(this)
            }
            is Number -> JsonPrimitive(this)
            is Boolean -> JsonPrimitive(this)
            null -> JsonNull
            else -> JsonPrimitive(this.toString())
        }
    }
}

// Extensiones globales fuera del objeto para evitar problemas de inline con private
inline fun <reified T> DocumentSnapshot.toObjectSerializable(): T? {
    val dataMap = data ?: return null
    return FirestoreMapper.decodeFromMap<T>(dataMap)
}

inline fun <reified T> QuerySnapshot.toObjectsSerializable(): List<T> {
    return documents.mapNotNull { it.toObjectSerializable<T>() }
}

// Helper para guardar
inline fun <reified T> T.encodeToMap(): Map<String, Any?>? {
    return FirestoreMapper.encodeToMap(this)
}
