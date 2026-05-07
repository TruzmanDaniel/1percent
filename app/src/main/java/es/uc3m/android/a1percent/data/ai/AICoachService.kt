package es.uc3m.android.a1percent.data.ai

import com.google.firebase.auth.FirebaseAuth
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.WeeklySummary
import es.uc3m.android.a1percent.data.model.weeksRemaining
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

object AICoachService {

    private val cloudFunctionApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://us-central1-uc3m-it-2026-16504-g04-96.cloudfunctions.net/app/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudFunctionApi::class.java)
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    suspend fun generateWeeklyTasks(
        goal: Goal,
        weeklySummary: WeeklySummary?,
        isWeekend: Boolean,
        userFeedback: String?,
        weekNumber: Int,
        isDeadlineWeek: Boolean = false
    ): Result<AiGenerationResult> {
        return try {
            val idToken = FirebaseAuth.getInstance().currentUser
                ?.getIdToken(false)?.await()?.token
                ?: throw IllegalStateException("User not authenticated")

            val prompt = buildPrompt(goal, weeklySummary, isWeekend, userFeedback, isDeadlineWeek)
            val request = GenerateMissionsRequest(prompt = prompt)
            val response = cloudFunctionApi.generateMissions(
                authorization = "Bearer $idToken",
                request = request
            )

            val cleanJson = response.content.trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = json.decodeFromString<AiTaskListResponse>(cleanJson)

            if (parsed.tasks.isEmpty()) {
                throw IllegalStateException("AI returned empty task list")
            }

            val tasks = parsed.tasks.map { it.toTask(goal.id, weekNumber, goal.category) }
            Result.success(AiGenerationResult(tasks = tasks, availableCredits = response.availableCredits))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(
        goal: Goal,
        weeklySummary: WeeklySummary?,
        isWeekend: Boolean,
        userFeedback: String?,
        isDeadlineWeek: Boolean
    ): String {
        val timeContext = if (isWeekend) {
            "Es fin de semana: propón tareas que requieran más tiempo pero menos recursos técnicos."
        } else {
            "Es día laborable: misiones rápidas (<15 min) centradas en la constancia."
        }

        val summaryContext = if (weeklySummary != null) {
            """
            Resumen semana anterior:
            - Tareas completadas: ${weeklySummary.tasksCompleted}/${weeklySummary.totalTasks}
            - Misión épica superada: ${if (weeklySummary.epicMissionPassed) "Sí" else "No"}
            - Feedback del usuario: ${weeklySummary.userFeedback?.displayName ?: "Sin feedback"}
            """.trimIndent()
        } else {
            "Es la primera semana de este objetivo. Empieza con tareas de calibración."
        }

        val feedbackContext = if (userFeedback != null) {
            "El usuario ha indicado que las tareas anteriores fueron: $userFeedback. Ajusta la dificultad."
        } else {
            ""
        }

        val goalTypeContext = buildGoalTypeContext(goal)

        val missionRules = if (isDeadlineWeek) {
            """
            REGLAS (SEMANA FINAL):
            - Esta es la ÚLTIMA SEMANA del proyecto. El deadline es inminente.
            - Genera entre 4 y 5 misiones diarias enfocadas en cerrar lo pendiente
            - Añade 1 MISIÓN ÉPICA FINAL que represente la culminación del proyecto
            - La misión épica debe ser el ÚLTIMO dayIndex y tener difficulty = ${goal.difficulty}
            - Total: entre 5 y 6 misiones (dayIndex consecutivos empezando en 1)
            - Las tareas deben ser concretas, accionables y medibles
            - Adapta la dificultad al nivel de intensidad proporcionado

            Responde SOLO con JSON válido, sin markdown, sin texto extra.
            Usa este formato exacto:
            {
              "tasks": [
                {"title": "...", "description": "...", "difficulty": 2, "dayIndex": 1},
                {"title": "...", "description": "...", "difficulty": 3, "dayIndex": 2},
                {"title": "...", "description": "...", "difficulty": 3, "dayIndex": 3},
                {"title": "...", "description": "...", "difficulty": 3, "dayIndex": 4},
                {"title": "MISIÓN ÉPICA FINAL: ...", "description": "...", "difficulty": ${goal.difficulty}, "dayIndex": 5}
              ]
            }
            """.trimIndent()
        } else {
            """
            REGLAS:
            - Genera exactamente 7 tareas con dayIndex de 1 a 7
            - Los días 1-6 son misiones normales
            - El día 7 es la MISIÓN ÉPICA: un reto de alta intensidad
            - La dificultad de cada tarea debe ser entre 1 y 5
            - Las tareas deben ser concretas, accionables y medibles
            - Adapta la dificultad al nivel de intensidad proporcionado

            Responde SOLO con JSON válido, sin markdown, sin texto extra.
            Usa este formato exacto:
            {
              "tasks": [
                {"title": "...", "description": "...", "difficulty": 2, "dayIndex": 1},
                {"title": "...", "description": "...", "difficulty": 2, "dayIndex": 2},
                {"title": "...", "description": "...", "difficulty": 2, "dayIndex": 3},
                {"title": "...", "description": "...", "difficulty": 2, "dayIndex": 4},
                {"title": "...", "description": "...", "difficulty": 3, "dayIndex": 5},
                {"title": "...", "description": "...", "difficulty": 3, "dayIndex": 6},
                {"title": "MISIÓN ÉPICA: ...", "description": "...", "difficulty": 5, "dayIndex": 7}
              ]
            }
            """.trimIndent()
        }

        return """
            Eres un coach personal basado en la filosofía del 1% de mejora diaria.
            Genera exactamente 7 misiones diarias para el siguiente objetivo:

            Objetivo: ${goal.title}
            Categoría: ${goal.category.displayName}
            Nivel de intensidad actual: ${goal.currentIntensity}

            $goalTypeContext

            $timeContext

            $summaryContext

            $feedbackContext

            $missionRules
        """.trimIndent()
    }

    private fun buildGoalTypeContext(goal: Goal): String {
        val weeksLeft = goal.weeksRemaining()
        val totalWeeks = ((goal.deadline - goal.createdAt) / (7L * 24 * 3600 * 1000)).toInt().coerceAtLeast(1)
        return """
            CONTEXTO DEL PROYECTO:
            - Tipo: Proyecto con fecha límite
            - Semanas restantes: $weeksLeft de $totalWeeks
            - Progreso actual: ${goal.progress}%
            - Extensiones usadas: ${goal.extensionCount}

            DIRECTRIZ: Este es un proyecto con fecha de examen. Diseña las misiones
            para un progreso lineal que se intensifique gradualmente hacia el deadline.
            Si quedan pocas semanas, prioriza las tareas más críticas para el objetivo
            final. La misión épica debe simular un "ensayo general" del reto final.
        """.trimIndent()
    }

    fun calculateNewIntensity(
        goal: Goal,
        epicPassed: Boolean,
        feedback: String?
    ): Float {
        if (!epicPassed) return goal.currentIntensity

        val maxIntensity = goal.difficulty * 2.0f

        val weeksLeft = goal.weeksRemaining()
        val growthMultiplier = if (weeksLeft <= 4) {
            1.0 + (4.0 - weeksLeft) / 4.0
        } else {
            1.0
        }

        val baseGrowth = goal.currentIntensity * Math.pow(1.01, 7.0 * growthMultiplier).toFloat()

        val adjusted = when (feedback) {
            "SOBRADO" -> baseGrowth * 1.05f
            "AGOTADO" -> baseGrowth * 0.90f
            else -> baseGrowth
        }

        return minOf(adjusted, maxIntensity)
    }

    fun calculateCatchUpIntensity(
        goal: Goal,
        feedback: String?
    ): Float {
        val maxIntensity = goal.difficulty * 2.0f
        val reduced = goal.currentIntensity * 0.85f

        val adjusted = when (feedback) {
            "SOBRADO" -> reduced * 1.05f
            "AGOTADO" -> reduced * 0.90f
            else -> reduced
        }

        return minOf(adjusted, maxIntensity)
    }
}

private interface CloudFunctionApi {
    @POST("generate-missions")
    suspend fun generateMissions(
        @Header("Authorization") authorization: String,
        @Body request: GenerateMissionsRequest
    ): GenerateMissionsResponse
}

private data class GenerateMissionsRequest(
    val prompt: String
)

private data class GenerateMissionsResponse(
    val content: String,
    val availableCredits: Int
)

data class AiGenerationResult(
    val tasks: List<Task>,
    val availableCredits: Int
)
