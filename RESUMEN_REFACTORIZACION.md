# Informe Técnico de Refactorización: Sistema de Datos 1percent

## 1. Implementación de Kotlin Serialization en Modelos
Se ha integrado `kotlinx.serialization` para automatizar la persistencia. Esto permite que Firestore maneje tipos complejos de Kotlin (Enums y Sealed Classes) sin código de mapeo manual.

### Ejemplo en Modelos (`Task.kt`)
**Antes:** Mapeo manual y campos excluidos de Firestore.
**Ahora (Estructura Profesional):**
```kotlin
@Serializable
data class Task(
    val id: String,
    val title: String,
    val deadline: TaskDeadline? = null, // Serialización automática de Sealed Interface
    val ownerId: String = "",           // Control de propiedad
    val sharedWith: List<String> = emptyList() // Control de acceso colaborativo
)
```

**Manejo de Sealed Classes (`TaskDeadline.kt`):**
Se configuró un discriminador de tipo para que Firestore sepa qué clase instanciar al leer:
```kotlin
@Serializable
sealed interface TaskDeadline {
    @SerialName("on_date")
    data class OnDate(val epochDay: Long) : TaskDeadline
    // Firestore guardará: { "type": "on_date", "epochDay": 12345 }
}
```

---

## 2. Refactorización de Repositorios: Del Mapeo Imperativo al Declarativo
Se ha sustituido el mapeo campo por campo por un sistema de mapeo genérico basado en extensiones.

### Comparativa en `TaskRepository.kt`
**Código Anterior (Imperativo/Frágil):**
```kotlin
// Había que extraer cada campo manualmente
val tasks = snapshot.documents.mapNotNull { doc ->
    Task(
        id = doc.id,
        title = doc.getString("title") ?: "",
        difficulty = doc.getLong("difficulty")?.toInt() ?: 1,
        // ... (repetir para cada uno de los 15 campos)
    )
}
```

**Código Nuevo (Declarativo/Robusto):**
```kotlin
// El mapper se encarga de la estructura completa en una línea
val tasks = snapshot.toObjectsSerializable<Task>()
```

---

## 3. Migración de Arquitectura de Colecciones
Para permitir la funcionalidad de **Compartir**, se movieron los datos de subcolecciones privadas a colecciones raíz protegidas por lógica de negocio.

*   **Ruta Antigua:** `users/{uid}/tasks/{taskId}` (Aislado)
*   **Ruta Nueva:** `/tasks/{taskId}` (Compartible)

**Nueva Lógica de Consulta (Query):**
```kotlin
// Esta consulta devuelve tanto las tareas propias como las compartidas
tasksCollection.whereArrayContains("sharedWith", userId)
```

---

## 4. Implementación Real del Sistema Social
Se eliminó el sistema "Mock" basado en memoria y se implementó una base de datos de relaciones real.

### Lógica de Identificadores Únicos
Para evitar que dos usuarios tengan dos documentos de amistad distintos, se implementó una clave determinista:
```kotlin
// SocialRepository.kt
val docId = if (fromId < toId) "${fromId}_${toId}" else "${toId}_${fromId}"
relationshipsCollection.document(docId).set(relationship)
```

---

## 5. Gestión de Errores y Robustez de Datos
Se implementó un `FirestoreMapper` personalizado para resolver problemas comunes del SDK de Firebase en Android:

1.  **Flexibilidad de Tipos**: Convierte automáticamente `Long` de Firestore a `Int` de Kotlin.
2.  **Sanitización de Nulos**: Detecta si un campo se guardó como el string `"null"` (error común de Firebase) y lo restaura a un valor `null` real para evitar fallos de parseo.
3.  **Reactividad**: Todos los flujos de datos ahora usan `callbackFlow` y `SnapshotListeners`, permitiendo que la interfaz de usuario se actualice en milisegundos cuando hay cambios en el servidor.

---
*Fin del informe técnico - 28 de Abril de 2026*
