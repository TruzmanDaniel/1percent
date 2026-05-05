package es.uc3m.android.a1percent.data.ai

import es.uc3m.android.a1percent.BuildConfig
import es.uc3m.android.a1percent.data.CreditManager
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.WeeklySummary
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

object AICoachService {

    private val openAIApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAIApi::class.java)
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
        userId: String,
        weekNumber: Int
    ): Result<List<Task>> {
        val creditResult = CreditManager.deductCredit(userId)
        if (creditResult.isFailure) {
            return Result.failure(creditResult.exceptionOrNull()!!)
        }

        return try {
            val prompt = buildPrompt(goal, weeklySummary, isWeekend, userFeedback)
            val request = ChatCompletionRequest(
                model = "gpt-4o-mini",
                messages = listOf(ChatMessage(role = "user", content = prompt))
            )
            val response = openAIApi.createChatCompletion(
                authorization = "Bearer ${BuildConfig.OPENAI_API_KEY}",
                request = request
            )
            val text = response.choices.firstOrNull()?.message?.content
                ?: throw IllegalStateException("Empty AI response")
            val cleanJson = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = json.decodeFromString<AiTaskListResponse>(cleanJson)

            if (parsed.tasks.isEmpty()) {
                throw IllegalStateException("AI returned empty task list")
            }

            val tasks = parsed.tasks.map { it.toTask(goal.id, weekNumber, goal.category) }
            Result.success(tasks)
        } catch (e: Exception) {
            CreditManager.refundCredit(userId)
            Result.failure(e)
        }
    }

    private fun buildPrompt(
        goal: Goal,
        weeklySummary: WeeklySummary?,
        isWeekend: Boolean,
        userFeedback: String?
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

        return """
            Eres un coach personal basado en la filosofía del 1% de mejora diaria.
            Genera exactamente 7 misiones diarias para el siguiente objetivo:

            Objetivo: ${goal.title}
            Categoría: ${goal.category.displayName}
            Nivel de intensidad actual: ${goal.currentIntensity}

            $timeContext

            $summaryContext

            $feedbackContext

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

    fun calculateNewIntensity(
        currentIntensity: Float,
        epicPassed: Boolean,
        feedback: String?,
        maxIntensity: Float
    ): Float {
        if (!epicPassed) return currentIntensity

        val baseGrowth = currentIntensity * Math.pow(1.01, 7.0).toFloat()

        val adjusted = when (feedback) {
            "SOBRADO" -> baseGrowth * 1.05f
            "AGOTADO" -> baseGrowth * 0.90f
            else -> baseGrowth
        }

        return minOf(adjusted, maxIntensity)
    }

    fun calculateCatchUpIntensity(
        currentIntensity: Float,
        feedback: String?,
        maxIntensity: Float
    ): Float {
        val reduced = currentIntensity * 0.85f

        val adjusted = when (feedback) {
            "SOBRADO" -> reduced * 1.05f
            "AGOTADO" -> reduced * 0.90f
            else -> reduced
        }

        return minOf(adjusted, maxIntensity)
    }
}

private interface OpenAIApi {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}

private data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7
)

private data class ChatMessage(
    val role: String,
    val content: String
)

private data class ChatCompletionResponse(
    val choices: List<Choice>
)

private data class Choice(
    val message: ChatMessage
)
