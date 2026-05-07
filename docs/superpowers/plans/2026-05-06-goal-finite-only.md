# Goal Finite-Only Refactor — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate infinite goals entirely, making all goals finite with mandatory deadlines, and reconvert the ritual/XP/AI systems accordingly.

**Architecture:** Bottom-up refactor: data model first (Goal, extensions, enums), then business logic (XP, AI), then UI layer (create, targets, detail, progress, ritual). Each task produces a compilable state. Tests are updated alongside each model change.

**Tech Stack:** Kotlin, Jetpack Compose, Firebase Firestore, OpenAI API (gpt-4o-mini), kotlinx.serialization

**Spec:** `docs/superpowers/specs/2026-05-06-goal-evolution-design.md`

---

### Task 1: Delete GoalType enum, MilestoneRecord, and MilestoneRepository

**Files:**
- Delete: `app/src/main/java/es/uc3m/android/a1percent/data/model/enums/GoalType.kt`
- Delete: `app/src/main/java/es/uc3m/android/a1percent/data/model/MilestoneRecord.kt`
- Delete: `app/src/main/java/es/uc3m/android/a1percent/data/MilestoneRepository.kt`

- [ ] **Step 1: Delete the three files**

Delete these files entirely:
- `app/src/main/java/es/uc3m/android/a1percent/data/model/enums/GoalType.kt`
- `app/src/main/java/es/uc3m/android/a1percent/data/model/MilestoneRecord.kt`
- `app/src/main/java/es/uc3m/android/a1percent/data/MilestoneRepository.kt`

- [ ] **Step 2: Commit**

```bash
git add -A
git commit -m "refactor: delete GoalType enum, MilestoneRecord, and MilestoneRepository"
```

> **Note:** The project will NOT compile after this task. That's expected — the next tasks fix all references.

---

### Task 2: Refactor Goal.kt — remove infinite fields, make deadline non-nullable

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/data/model/Goal.kt`

- [ ] **Step 1: Update Goal.kt**

Replace the entire file content with:

```kotlin
package es.uc3m.android.a1percent.data.model

import es.uc3m.android.a1percent.data.model.enums.AiRoadmapStatus
import es.uc3m.android.a1percent.data.model.enums.Category
import es.uc3m.android.a1percent.data.model.enums.GoalStatus
import es.uc3m.android.a1percent.data.model.enums.PausedBy
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Goal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val category: Category,
    val difficulty: Int,
    val xp: Int,
    val deadline: Long,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val progress: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val ownerId: String = "",
    val sharedWith: List<String> = emptyList(),
    val currentIntensity: Float = difficulty.toFloat(),
    val nextGenerationDate: Long? = null,
    val aiRoadmapStatus: AiRoadmapStatus = AiRoadmapStatus.NONE,
    val extensionCount: Int = 0,
    val pausedBy: PausedBy? = null
) {
    init {
        require(difficulty in 1..5) { "Goal difficulty must be between 1 and 5" }
        require(progress in 0..100) { "Goal progress must be between 0 and 100" }
    }
}
```

Key changes:
- `deadline: Long? = null` → `deadline: Long` (non-nullable, no default)
- Removed `weeklyStreak: Int = 0`
- Removed `streakStartDate: Long? = null`
- Removed `@Transient val goalType` computed property
- Removed imports for `GoalType`, `Transient`

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/data/model/Goal.kt
git commit -m "refactor: make Goal.deadline non-nullable, remove weeklyStreak/streakStartDate/goalType"
```

---

### Task 3: Rewrite GoalExtensions.kt — remove infinite functions, add isDeadlineWeek

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/data/model/GoalExtensions.kt`

- [ ] **Step 1: Replace GoalExtensions.kt**

Replace the entire file content with:

```kotlin
package es.uc3m.android.a1percent.data.model

import java.util.Calendar

fun Goal.weeksRemaining(): Int {
    val now = System.currentTimeMillis()
    val remainingMs = (deadline - now).coerceAtLeast(0)
    return (remainingMs / (7L * 24 * 3600 * 1000)).toInt()
}

fun Goal.totalWeeks(): Int {
    val weekMs = 7L * 24 * 3600 * 1000
    return ((deadline - createdAt + weekMs - 1) / weekMs).toInt().coerceAtLeast(1)
}

fun Goal.weekLabel(currentWeek: Int): String {
    return "Semana $currentWeek de ${totalWeeks()}"
}

fun Goal.isDeadlineWeek(): Boolean {
    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val weekStart = cal.timeInMillis
    val weekEnd = weekStart + 7L * 24 * 3600 * 1000
    return deadline in weekStart until weekEnd
}
```

Key changes:
- Removed: `isFinite`, `isInfinite`, `progressDisplay()`, `intensityDisplay()`, `streakDisplay()`, `nextMilestone()`, `justReachedMilestone()`
- Simplified: `weeksRemaining()` returns `Int` (not `Int?`), no type guard
- Simplified: `totalWeeks()` is now public, no null check on deadline
- Simplified: `weekLabel()` always shows "Semana X de Y"
- Added: `isDeadlineWeek()` using Monday-to-Sunday week boundaries

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/data/model/GoalExtensions.kt
git commit -m "refactor: simplify GoalExtensions for finite-only, add isDeadlineWeek()"
```

---

### Task 4: Rewrite GoalExtensionsTest.kt and IntensityCalculationTest.kt

**Files:**
- Modify: `app/src/test/java/es/uc3m/android/a1percent/data/model/GoalExtensionsTest.kt`
- Modify: `app/src/test/java/es/uc3m/android/a1percent/data/ai/IntensityCalculationTest.kt`

- [ ] **Step 1: Rewrite GoalExtensionsTest.kt**

Replace the entire file content with:

```kotlin
package es.uc3m.android.a1percent.data.model

import es.uc3m.android.a1percent.data.model.enums.Category
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class GoalExtensionsTest {

    private fun goal(
        deadline: Long = System.currentTimeMillis() + 52 * 7 * 24 * 3600 * 1000L,
        progress: Int = 35,
        extensionCount: Int = 0
    ) = Goal(
        title = "Marathon",
        category = Category.FITNESS,
        difficulty = 3,
        xp = 100,
        deadline = deadline,
        progress = progress,
        extensionCount = extensionCount
    )

    @Test
    fun `weeksRemaining returns weeks until deadline`() {
        val fourWeeksFromNow = System.currentTimeMillis() + 4 * 7 * 24 * 3600 * 1000L
        val g = goal(deadline = fourWeeksFromNow)
        val remaining = g.weeksRemaining()
        assertTrue(remaining in 3..5)
    }

    @Test
    fun `weeksRemaining returns 0 when deadline has passed`() {
        val pastDeadline = System.currentTimeMillis() - 7 * 24 * 3600 * 1000L
        assertEquals(0, goal(deadline = pastDeadline).weeksRemaining())
    }

    @Test
    fun `totalWeeks calculates from creation to deadline`() {
        val now = System.currentTimeMillis()
        val g = Goal(
            title = "Test",
            category = Category.FITNESS,
            difficulty = 3,
            xp = 100,
            deadline = now + 10 * 7 * 24 * 3600 * 1000L,
            createdAt = now
        )
        assertEquals(10, g.totalWeeks())
    }

    @Test
    fun `totalWeeks returns at least 1`() {
        val now = System.currentTimeMillis()
        val g = Goal(
            title = "Test",
            category = Category.FITNESS,
            difficulty = 3,
            xp = 100,
            deadline = now + 1000L,
            createdAt = now
        )
        assertEquals(1, g.totalWeeks())
    }

    @Test
    fun `weekLabel shows current week and total`() {
        assertEquals("Semana 8 de 52", goal().weekLabel(8))
    }

    @Test
    fun `isDeadlineWeek returns true when deadline is this week`() {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        val midWeek = cal.timeInMillis
        assertTrue(goal(deadline = midWeek).isDeadlineWeek())
    }

    @Test
    fun `isDeadlineWeek returns false when deadline is next week`() {
        val nextWeek = System.currentTimeMillis() + 10 * 24 * 3600 * 1000L
        assertFalse(goal(deadline = nextWeek).isDeadlineWeek())
    }
}
```

- [ ] **Step 2: Update IntensityCalculationTest.kt**

Replace the entire file content with:

```kotlin
package es.uc3m.android.a1percent.data.ai

import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.enums.Category
import org.junit.Assert.*
import org.junit.Test

class IntensityCalculationTest {

    private fun goal(
        deadline: Long = System.currentTimeMillis() + 20 * 7 * 24 * 3600 * 1000L,
        createdAt: Long = System.currentTimeMillis(),
        currentIntensity: Float = 3.0f,
        difficulty: Int = 3
    ) = Goal(
        title = "Test",
        category = Category.FITNESS,
        difficulty = difficulty,
        xp = 100,
        deadline = deadline,
        createdAt = createdAt,
        currentIntensity = currentIntensity
    )

    @Test
    fun `caps at difficulty x 2_0`() {
        val fourWeeks = System.currentTimeMillis() + 4 * 7 * 24 * 3600 * 1000L
        val g = goal(deadline = fourWeeks, difficulty = 3, currentIntensity = 5.8f)
        val result = AICoachService.calculateNewIntensity(
            goal = g,
            epicPassed = true,
            feedback = "SOBRADO"
        )
        assertTrue(result <= 3 * 2.0f)
    }

    @Test
    fun `sprint final accelerates growth for goals with less than 4 weeks`() {
        val twoWeeks = System.currentTimeMillis() + 2 * 7 * 24 * 3600 * 1000L
        val sprintGoal = goal(deadline = twoWeeks, difficulty = 5, currentIntensity = 3.0f)
        val normalGoal = goal(difficulty = 5, currentIntensity = 3.0f)
        val normalGrowth = AICoachService.calculateNewIntensity(
            goal = normalGoal,
            epicPassed = true,
            feedback = "PERFECTO"
        )
        val sprintGrowth = AICoachService.calculateNewIntensity(
            goal = sprintGoal,
            epicPassed = true,
            feedback = "PERFECTO"
        )
        assertTrue(sprintGrowth > normalGrowth)
    }

    @Test
    fun `agotado feedback reduces intensity`() {
        val g = goal(currentIntensity = 3.0f)
        val normal = AICoachService.calculateNewIntensity(
            goal = g, epicPassed = true, feedback = "PERFECTO"
        )
        val agotado = AICoachService.calculateNewIntensity(
            goal = g, epicPassed = true, feedback = "AGOTADO"
        )
        assertTrue(agotado < normal)
    }

    @Test
    fun `no change when epic not passed`() {
        val g = goal(currentIntensity = 3.0f)
        val result = AICoachService.calculateNewIntensity(
            goal = g, epicPassed = false, feedback = "SOBRADO"
        )
        assertEquals(3.0f, result, 0.001f)
    }
}
```

- [ ] **Step 3: Run tests to verify they compile** (they won't pass yet until AICoachService is updated in Task 6)

```bash
cd app && ../gradlew compileDebugUnitTestKotlin 2>&1 | head -30
```

- [ ] **Step 4: Commit**

```bash
git add app/src/test/java/es/uc3m/android/a1percent/data/model/GoalExtensionsTest.kt app/src/test/java/es/uc3m/android/a1percent/data/ai/IntensityCalculationTest.kt
git commit -m "test: rewrite GoalExtensions and Intensity tests for finite-only model"
```

---

### Task 5: Remove awardMilestoneBonus from XpManager.kt

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/data/XpManager.kt`

- [ ] **Step 1: Remove awardMilestoneBonus and its imports**

In `XpManager.kt`:

1. Remove the import `import es.uc3m.android.a1percent.data.model.MilestoneRecord` (line 4)
2. Remove the entire `awardMilestoneBonus` function (lines 78-98):

```kotlin
    // DELETE THIS ENTIRE FUNCTION:
    suspend fun awardMilestoneBonus(userId: String, goal: Goal, milestone: Int): Result<Unit> {
        ...
    }
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/data/XpManager.kt
git commit -m "refactor: remove awardMilestoneBonus from XpManager"
```

---

### Task 6: Rewrite AICoachService — remove infinite branches, add isDeadlineWeek

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/data/ai/AICoachService.kt`

- [ ] **Step 1: Update imports**

Replace the imports section (lines 1-17) with:

```kotlin
package es.uc3m.android.a1percent.data.ai

import es.uc3m.android.a1percent.BuildConfig
import es.uc3m.android.a1percent.data.CreditManager
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.WeeklySummary
import es.uc3m.android.a1percent.data.model.weeksRemaining
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
```

Removed: `GoalType`, `isFinite`, `nextMilestone` imports.

- [ ] **Step 2: Add isDeadlineWeek parameter to generateWeeklyTasks**

Replace the `generateWeeklyTasks` function signature (line 35-42) with:

```kotlin
    suspend fun generateWeeklyTasks(
        goal: Goal,
        weeklySummary: WeeklySummary?,
        isWeekend: Boolean,
        userFeedback: String?,
        userId: String,
        weekNumber: Int,
        isDeadlineWeek: Boolean = false
    ): Result<List<Task>> {
```

And update the `buildPrompt` call inside it (line 49) to pass `isDeadlineWeek`:

```kotlin
            val prompt = buildPrompt(goal, weeklySummary, isWeekend, userFeedback, isDeadlineWeek)
```

- [ ] **Step 3: Add isDeadlineWeek to buildPrompt**

Replace the `buildPrompt` signature (lines 75-80) with:

```kotlin
    private fun buildPrompt(
        goal: Goal,
        weeklySummary: WeeklySummary?,
        isWeekend: Boolean,
        userFeedback: String?,
        isDeadlineWeek: Boolean
    ): String {
```

- [ ] **Step 4: Update the REGLAS section in buildPrompt**

Replace the rules section in the prompt string (lines 122-141) to handle deadline week:

```kotlin
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
```

Then in the return string, replace the REGLAS block with `$missionRules`.

- [ ] **Step 5: Rewrite buildGoalTypeContext — remove infinite branch**

Replace the entire `buildGoalTypeContext` function (lines 146-181) with:

```kotlin
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
```

- [ ] **Step 6: Simplify calculateNewIntensity — remove infinite cap**

Replace the entire `calculateNewIntensity` function (lines 183-212) with:

```kotlin
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
```

- [ ] **Step 7: Simplify calculateCatchUpIntensity — remove infinite cap**

Replace the entire `calculateCatchUpIntensity` function (lines 214-233) with:

```kotlin
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
```

- [ ] **Step 8: Run tests**

```bash
cd app && ../gradlew testDebugUnitTest 2>&1 | tail -20
```

Expected: all tests pass.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/data/ai/AICoachService.kt
git commit -m "refactor: rewrite AICoachService for finite-only goals with deadline week mode"
```

---

### Task 7: Update CreateGoalCard and CreateGoalViewModel — mandatory deadline

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/goal/CreateGoalUiState.kt`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/goal/CreateGoalViewModel.kt`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/goal/CreateGoalCard.kt`

- [ ] **Step 1: Update CreateGoalUiState.kt**

Replace the entire file content with:

```kotlin
package es.uc3m.android.a1percent.ui.screens.goal

import es.uc3m.android.a1percent.data.model.Task

data class CreateGoalUiState(
    val goalName: String = "",
    val difficulty: Float = 3f,
    val deadlineEpochMillis: Long? = null,
    val showDatePicker: Boolean = false,
    val isLoading: Boolean = false,
    val aiState: AiNegotiationState = AiNegotiationState.IDLE,
    val proposedTasks: List<Task> = emptyList(),
    val negotiationCount: Int = 0,
    val errorMessage: String? = null,
    val availableCredits: Int = 5
) {
    val canCreateGoal: Boolean
        get() = goalName.isNotBlank() && deadlineEpochMillis != null
            && !isLoading && aiState != AiNegotiationState.GENERATING

    val canNegotiate: Boolean
        get() = negotiationCount < 3 && aiState == AiNegotiationState.PROPOSAL_READY

    val canGenerateAi: Boolean
        get() = goalName.isNotBlank() && deadlineEpochMillis != null
            && !isLoading && availableCredits > 0
            && aiState != AiNegotiationState.GENERATING
}
```

Changes: removed `hasDeadline: Boolean`. `canCreateGoal` and `canGenerateAi` now require `deadlineEpochMillis != null`.

- [ ] **Step 2: Update CreateGoalViewModel.kt**

Remove the `onToggleDeadline` function entirely (lines 35-43).

Replace `onDeadlineSelected` (line 45-47) with:

```kotlin
    fun onDeadlineSelected(epochMillis: Long) {
        _uiState.update { it.copy(deadlineEpochMillis = epochMillis, showDatePicker = false) }
    }
```

Update `onDismissDatePicker` (lines 53-57) to:

```kotlin
    fun onDismissDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }
```

In `generateProposal()` (line 70-76), the `tempGoal` must include a deadline:

```kotlin
            val tempGoal = Goal(
                title = _uiState.value.goalName,
                category = Category.PERSONAL,
                difficulty = _uiState.value.difficulty.toInt(),
                xp = _uiState.value.difficulty.toInt() * 50,
                currentIntensity = currentIntensity,
                deadline = _uiState.value.deadlineEpochMillis
                    ?: (System.currentTimeMillis() + 30L * 24 * 3600 * 1000)
            )
```

No other changes needed — `acceptProposal()` and `createGoal()` already pass `deadlineEpochMillis` which is now required to be non-null by the UI gate.

- [ ] **Step 3: Update CreateGoalCard.kt**

Remove the `Switch` toggle and its `Row` container (lines 138-163). Replace with a mandatory deadline display:

```kotlin
                        Column {
                            Text(
                                text = "Fecha límite",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            val deadlineMs = uiState.deadlineEpochMillis
                            if (deadlineMs != null) {
                                val formatted = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                                    .format(Date(deadlineMs))
                                Text(
                                    text = "Deadline: $formatted",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            } else {
                                Text(
                                    text = "Selecciona una fecha",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
```

Replace the "Cambiar fecha" button section (lines 165-169) with an always-visible button:

```kotlin
                        TextButton(onClick = { viewModel.onShowDatePicker() }) {
                            Text(if (uiState.deadlineEpochMillis != null) "Cambiar fecha" else "Seleccionar fecha")
                        }
```

Update the DatePicker initialSelectedDateMillis (line 173) — remove the `hasDeadline` guard:

```kotlin
                        if (uiState.showDatePicker) {
                            val datePickerState = rememberDatePickerState(
                                initialSelectedDateMillis = uiState.deadlineEpochMillis
                                    ?: (System.currentTimeMillis() + 30L * 24 * 3600 * 1000)
                            )
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/ui/screens/goal/
git commit -m "refactor: make deadline mandatory in goal creation UI"
```

---

### Task 8: Update TargetsScreen.kt — remove infinite goal cards

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsScreen.kt`

- [ ] **Step 1: Update imports**

Remove these imports:
```kotlin
import es.uc3m.android.a1percent.data.model.intensityDisplay
import es.uc3m.android.a1percent.data.model.isFinite
import es.uc3m.android.a1percent.data.model.nextMilestone
import es.uc3m.android.a1percent.data.model.streakDisplay
```

- [ ] **Step 2: Unify the GoalCompactItem card body**

In the `GoalCompactItem` composable (around line 661-718), replace the entire `if (goal.isFinite) { ... } else { ... }` block with the unified finite layout only:

```kotlin
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Progreso", style = MaterialTheme.typography.labelSmall)
                    Text("${goal.progress}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
                LinearProgressIndicator(
                    progress = { goal.progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            val weeksLeft = goal.weeksRemaining()
            Text(
                "$weeksLeft semanas restantes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
```

- [ ] **Step 3: Update the category chip color**

Replace the chip color conditional (around line 647-649):
```kotlin
// Old:
containerColor = if (goal.isFinite) MaterialTheme.colorScheme.tertiaryContainer
    else MaterialTheme.colorScheme.primaryContainer
// New:
containerColor = MaterialTheme.colorScheme.tertiaryContainer
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsScreen.kt
git commit -m "refactor: unify TargetsScreen goal cards to finite-only layout"
```

---

### Task 9: Update GoalDetailScreen.kt — remove infinite branch, add COMPLETED badge and manual complete

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/GoalDetailScreen.kt`

- [ ] **Step 1: Update imports**

Remove these imports:
```kotlin
import es.uc3m.android.a1percent.data.model.intensityDisplay
import es.uc3m.android.a1percent.data.model.isFinite
import es.uc3m.android.a1percent.data.model.streakDisplay
```

Add:
```kotlin
import es.uc3m.android.a1percent.data.model.enums.GoalStatus
import androidx.compose.material3.Button
```

- [ ] **Step 2: Replace GoalHeaderCard body**

Replace the `if (goal.isFinite) { ... } else { ... }` block (lines 229-251) with the unified layout:

```kotlin
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Progreso", style = MaterialTheme.typography.labelSmall)
                    Text(text = "${goal.progress}%", style = MaterialTheme.typography.labelSmall)
                }
                LinearProgressIndicator(
                    progress = { goal.progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatColumn("Progreso", "${goal.progress}%")
                StatColumn("Semanas", "${goal.weeksRemaining()}")
                StatColumn("Intensidad", "%.1f".format(goal.currentIntensity))
            }

            if (goal.status == GoalStatus.COMPLETED) {
                Text(
                    text = "COMPLETADO",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
```

- [ ] **Step 3: Add manual completion button below the GoalHeaderCard**

In the `LazyColumn` (after the GoalHeaderCard `item` block, around line 118), add:

```kotlin
                item {
                    if (goal.status != GoalStatus.COMPLETED && goal.status != GoalStatus.ARCHIVED) {
                        Button(
                            onClick = { viewModel.onCompleteGoal() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Completar objetivo")
                        }
                    }
                }
```

- [ ] **Step 4: Add onCompleteGoal to GoalDetailViewModel**

In `GoalDetailViewModel.kt`, add this function:

```kotlin
    fun onCompleteGoal() {
        val goal = _uiState.value.goal ?: return
        val userId = SessionRepository.currentUser.value?.id ?: return
        viewModelScope.launch {
            val completed = goal.copy(status = GoalStatus.COMPLETED, progress = 100)
            GoalRepository.updateGoal(completed)
            XpManager.awardGoalCompletionBonus(userId, goal)
            loadGoalForUser(userId, goal.id)
            _uiState.update { it.copy(snackbarMessage = "¡Objetivo completado!") }
        }
    }
```

Add the necessary import:
```kotlin
import es.uc3m.android.a1percent.data.model.enums.GoalStatus
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/GoalDetailScreen.kt app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/GoalDetailViewModel.kt
git commit -m "refactor: unify GoalDetailScreen, add COMPLETED badge and manual complete button"
```

---

### Task 10: Update ProgressScreen — remove Milestones section

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/progress/ProgressScreen.kt`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/progress/ProgressUiState.kt`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/progress/ProgressViewModel.kt`

- [ ] **Step 1: Remove MilestonesCard call from ProgressScreen.kt**

Delete the line (around line 115):
```kotlin
        item { MilestonesCard(uiState.milestones) }
```

Delete the entire `MilestonesCard` composable function (starts at line 165, roughly 40 lines).

- [ ] **Step 2: Remove milestones from ProgressUiState.kt**

Remove the field:
```kotlin
    val milestones: List<GoalMilestoneItem> = emptyList()
```

Delete the `GoalMilestoneItem` data class entirely (lines 52-57).

- [ ] **Step 3: Remove milestone loading from ProgressViewModel.kt**

Remove the import:
```kotlin
import es.uc3m.android.a1percent.data.MilestoneRepository
```

Remove the `loadMilestones()` function entirely and its call in the goals observer.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/ui/screens/progress/
git commit -m "refactor: remove Milestones section from ProgressScreen"
```

---

### Task 11: Update SocialScreen copy

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/social/SocialScreen.kt`

- [ ] **Step 1: Update copy text**

Replace line 313:
```kotlin
// Old:
"Track habits and challenge streaks together.",
// New:
"Track goals and challenge progress together.",
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/ui/screens/social/SocialScreen.kt
git commit -m "refactor: update SocialScreen copy for finite-only goals"
```

---

### Task 12: Reconvert RitualStep, RitualUiState, and RitualViewModel

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/ritual/RitualStep.kt`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/ritual/RitualUiState.kt`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/ritual/RitualViewModel.kt`

- [ ] **Step 1: Update RitualStep.kt — remove MILESTONE**

Replace the entire file:

```kotlin
package es.uc3m.android.a1percent.ui.screens.ritual

enum class RitualStep {
    SUMMARY,
    EPIC_RESULT,
    DEADLINE_CHECK,
    FEEDBACK,
    INTENSITY_CHANGE,
    GENERATING,
    COMPLETE
}
```

- [ ] **Step 2: Update RitualUiState.kt — remove streak, add progress fields**

Replace the entire file:

```kotlin
package es.uc3m.android.a1percent.ui.screens.ritual

import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.enums.EnergyFeedback

data class RitualUiState(
    val goal: Goal? = null,
    val visibleSteps: List<RitualStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val tasksCompleted: Int = 0,
    val totalTasks: Int = 0,
    val epicMissionPassed: Boolean = false,
    val xpEarned: Int = 0,
    val selectedFeedback: EnergyFeedback? = null,
    val newIntensity: Float? = null,
    val oldIntensity: Float? = null,
    val newDeadline: Long? = null,
    val isGenerating: Boolean = false,
    val generationComplete: Boolean = false,
    val isCatchUp: Boolean = false,
    val goalCompleted: Boolean = false,
    val showDatePicker: Boolean = false,
    val weekNumber: Int = 0,
    val goalProgress: Int = 0,
    val weeksRemaining: Int = 0
) {
    val currentStep: RitualStep?
        get() = visibleSteps.getOrNull(currentStepIndex)

    val canSkip: Boolean
        get() = currentStep in listOf(RitualStep.SUMMARY, RitualStep.EPIC_RESULT, RitualStep.INTENSITY_CHANGE)

    val isLastStep: Boolean
        get() = currentStepIndex >= visibleSteps.lastIndex
}
```

Changes: removed `milestoneReached`, `newWeeklyStreak`. Added `goalProgress`, `weeksRemaining`.

- [ ] **Step 3: Rewrite RitualViewModel.kt**

Replace the entire file:

```kotlin
package es.uc3m.android.a1percent.ui.screens.ritual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.uc3m.android.a1percent.data.GoalRepository
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.TaskRespository
import es.uc3m.android.a1percent.data.WeeklySummaryRepository
import es.uc3m.android.a1percent.data.XpManager
import es.uc3m.android.a1percent.data.ai.AICoachService
import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.WeeklySummary
import es.uc3m.android.a1percent.data.model.enums.AiRoadmapStatus
import es.uc3m.android.a1percent.data.model.enums.EnergyFeedback
import es.uc3m.android.a1percent.data.model.enums.GoalStatus
import es.uc3m.android.a1percent.data.model.enums.TaskStatus
import es.uc3m.android.a1percent.data.model.isDeadlineWeek
import es.uc3m.android.a1percent.data.model.weeksRemaining
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class RitualViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RitualUiState())
    val uiState: StateFlow<RitualUiState> = _uiState.asStateFlow()

    companion object {
        private const val SEVEN_DAYS_MILLIS = 7L * 24 * 60 * 60 * 1000
    }

    fun loadRitual(goalId: String) {
        val userId = SessionRepository.currentUser.value?.id ?: return
        viewModelScope.launch {
            val goal = GoalRepository.getGoalById(goalId).getOrNull() ?: return@launch
            val tasks = TaskRespository.getTasks(userId).getOrNull() ?: emptyList()
            val goalTasks = tasks.filter { it.goalId == goalId && it.isAiGenerated }

            val completed = goalTasks.count { it.status == TaskStatus.COMPLETED }
            val epicTask = goalTasks.maxByOrNull { it.dayIndex ?: 0 }
            val epicPassed = epicTask?.status == TaskStatus.COMPLETED
            val xpEarned = goalTasks.filter { it.status == TaskStatus.COMPLETED }
                .sumOf { it.xpAwarded ?: it.xp }

            val now = System.currentTimeMillis()
            val isCatchUp = (now - (goal.nextGenerationDate ?: 0)) > SEVEN_DAYS_MILLIS * 2

            val latestSummary = WeeklySummaryRepository.getLatestSummary(goal.id).getOrNull()
            val weekNumber = (latestSummary?.weekNumber ?: 0) + 1

            val steps = computeVisibleSteps(goal, isCatchUp)

            _uiState.update {
                RitualUiState(
                    goal = goal,
                    visibleSteps = steps,
                    tasksCompleted = completed,
                    totalTasks = goalTasks.size,
                    epicMissionPassed = epicPassed,
                    xpEarned = xpEarned,
                    oldIntensity = goal.currentIntensity,
                    isCatchUp = isCatchUp,
                    weekNumber = weekNumber,
                    goalProgress = goal.progress,
                    weeksRemaining = goal.weeksRemaining()
                )
            }
        }
    }

    private fun computeVisibleSteps(goal: Goal, isCatchUp: Boolean): List<RitualStep> {
        if (isCatchUp) {
            return listOf(
                RitualStep.SUMMARY,
                RitualStep.FEEDBACK,
                RitualStep.INTENSITY_CHANGE,
                RitualStep.GENERATING,
                RitualStep.COMPLETE
            )
        }

        val steps = mutableListOf(
            RitualStep.SUMMARY,
            RitualStep.EPIC_RESULT
        )

        val now = System.currentTimeMillis()
        if (goal.deadline in now..(now + SEVEN_DAYS_MILLIS)) {
            steps.add(RitualStep.DEADLINE_CHECK)
        }

        steps.add(RitualStep.FEEDBACK)
        steps.add(RitualStep.INTENSITY_CHANGE)
        steps.add(RitualStep.GENERATING)
        steps.add(RitualStep.COMPLETE)
        return steps
    }

    fun onNextStep() {
        _uiState.update { it.copy(currentStepIndex = it.currentStepIndex + 1) }
    }

    fun onSkipToFeedback() {
        val feedbackIndex = _uiState.value.visibleSteps.indexOf(RitualStep.FEEDBACK)
        if (feedbackIndex >= 0) {
            _uiState.update { it.copy(currentStepIndex = feedbackIndex) }
        }
    }

    fun onFeedbackSelected(feedback: EnergyFeedback) {
        val goal = _uiState.value.goal ?: return
        val isCatchUp = _uiState.value.isCatchUp

        val newIntensity = if (isCatchUp) {
            AICoachService.calculateCatchUpIntensity(goal, feedback.name)
        } else {
            AICoachService.calculateNewIntensity(
                goal = goal,
                epicPassed = _uiState.value.epicMissionPassed,
                feedback = feedback.name
            )
        }

        _uiState.update {
            it.copy(
                selectedFeedback = feedback,
                newIntensity = newIntensity,
                currentStepIndex = it.currentStepIndex + 1
            )
        }
    }

    fun onExtendDeadline(newDeadline: Long) {
        _uiState.update {
            it.copy(
                newDeadline = newDeadline,
                showDatePicker = false
            )
        }
        onNextStep()
    }

    fun onCompleteGoal() {
        val goal = _uiState.value.goal ?: return
        val userId = SessionRepository.currentUser.value?.id ?: return

        viewModelScope.launch {
            val completedGoal = goal.copy(
                status = GoalStatus.COMPLETED,
                progress = 100
            )
            GoalRepository.updateGoal(completedGoal)
            XpManager.awardGoalCompletionBonus(userId, goal)
            _uiState.update { it.copy(goalCompleted = true) }
        }
    }

    fun onShowDatePicker() {
        _uiState.update { it.copy(showDatePicker = true) }
    }

    fun onDismissDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    fun onStartGeneration() {
        val goal = _uiState.value.goal ?: return
        val userId = SessionRepository.currentUser.value?.id ?: return
        val feedback = _uiState.value.selectedFeedback ?: return
        val newIntensity = _uiState.value.newIntensity ?: return
        val isCatchUp = _uiState.value.isCatchUp
        val weekNumber = _uiState.value.weekNumber

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }

            val latestSummary = WeeklySummaryRepository.getLatestSummary(goal.id).getOrNull()

            if (!isCatchUp) {
                val summary = WeeklySummary(
                    goalId = goal.id,
                    userId = userId,
                    weekNumber = weekNumber - 1,
                    tasksCompleted = _uiState.value.tasksCompleted,
                    totalTasks = _uiState.value.totalTasks,
                    epicMissionPassed = _uiState.value.epicMissionPassed,
                    userFeedback = feedback,
                    intensityUsed = goal.currentIntensity
                )
                WeeklySummaryRepository.saveSummary(goal.id, summary)

                if (_uiState.value.epicMissionPassed) {
                    XpManager.awardEpicWeeklyBonus(userId, goal)
                }

                val totalWeeks = ((goal.deadline - goal.createdAt) / SEVEN_DAYS_MILLIS).toInt().coerceAtLeast(1)
                val newProgress = ((weekNumber * 100) / totalWeeks).coerceIn(0, 100)
                GoalRepository.updateGoal(goal.copy(progress = newProgress))
            }

            var updatedGoal = goal.copy(currentIntensity = newIntensity)

            if (_uiState.value.newDeadline != null) {
                updatedGoal = updatedGoal.copy(
                    deadline = _uiState.value.newDeadline!!,
                    extensionCount = goal.extensionCount + 1
                )
            }

            val isWeekend = Calendar.getInstance().let {
                it.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)
            }

            val nextWeekIsDeadline = run {
                val nextWeekGoal = updatedGoal.copy(
                    nextGenerationDate = System.currentTimeMillis() + SEVEN_DAYS_MILLIS
                )
                val nextWeekStart = System.currentTimeMillis() + SEVEN_DAYS_MILLIS
                val nextWeekEnd = nextWeekStart + SEVEN_DAYS_MILLIS
                nextWeekGoal.deadline in nextWeekStart..nextWeekEnd
            }

            val result = AICoachService.generateWeeklyTasks(
                goal = updatedGoal,
                weeklySummary = if (isCatchUp) null else latestSummary,
                isWeekend = isWeekend,
                userFeedback = feedback.name,
                userId = userId,
                weekNumber = weekNumber,
                isDeadlineWeek = nextWeekIsDeadline
            )

            result.onSuccess { tasks ->
                TaskRespository.saveTaskBatch(userId, tasks.map { it.copy(goalId = goal.id) })

                val finalGoal = updatedGoal.copy(
                    nextGenerationDate = System.currentTimeMillis() + SEVEN_DAYS_MILLIS
                )
                GoalRepository.updateGoal(finalGoal)

                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        generationComplete = true,
                        currentStepIndex = it.currentStepIndex + 1
                    )
                }
            }

            result.onFailure {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }
}
```

Key changes:
- Removed all `weeklyStreak`, `streakStartDate`, `milestoneReached` logic
- Removed `isInfinite`/`isFinite` imports and checks
- Removed `MILESTONE` step from `computeVisibleSteps`
- Added `isDeadlineWeek` detection and pass-through to `generateWeeklyTasks`
- Progress now calculated for all goals (removed the `if (goal.isFinite)` guard)
- Epic task detection uses `maxByOrNull { it.dayIndex }` instead of hardcoded `dayIndex == 7`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/ui/screens/ritual/
git commit -m "refactor: reconvert ritual to finite-only flow, remove streaks/milestones"
```

---

### Task 13: Update RitualScreen.kt — replace streak UI with progress

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/ritual/RitualScreen.kt`

- [ ] **Step 1: Update imports**

Remove:
```kotlin
import es.uc3m.android.a1percent.data.model.isFinite
import es.uc3m.android.a1percent.data.model.weekLabel
```

Add:
```kotlin
import es.uc3m.android.a1percent.data.model.weeksRemaining
```

- [ ] **Step 2: Remove MILESTONE case from AnimatedContent**

Delete the line (around line 104):
```kotlin
                RitualStep.MILESTONE -> MilestoneStep(uiState, onNext = viewModel::onNextStep)
```

- [ ] **Step 3: Delete the MilestoneStep composable**

Delete the entire `MilestoneStep` function (lines 312-338).

- [ ] **Step 4: Update SummaryStep — replace weekLabel with progress info**

In `SummaryStep` (around line 161), replace:
```kotlin
        Text(goal.weekLabel(uiState.weekNumber), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
```
with:
```kotlin
        Text("Semana ${uiState.weekNumber} de ${goal.totalWeeks()}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${uiState.goalProgress}% completado • ${uiState.weeksRemaining} semanas restantes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
```

Add the import:
```kotlin
import es.uc3m.android.a1percent.data.model.totalWeeks
```

- [ ] **Step 5: Update CompleteStep — replace streak with progress**

In `CompleteStep` (around line 364-369), replace the streak display:
```kotlin
        // Old:
        if (uiState.newWeeklyStreak > 0) {
            Text("Racha: ${uiState.newWeeklyStreak} semanas", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFD700))
        }
        // New:
        Text("${uiState.goalProgress}% completado", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFD700))
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/ui/screens/ritual/RitualScreen.kt
git commit -m "refactor: update RitualScreen UI for finite-only progress display"
```

---

### Task 14: Final build and test verification

**Files:** None — verification only.

- [ ] **Step 1: Run full compilation**

```bash
cd app && ../gradlew compileDebugKotlin 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run all unit tests**

```bash
cd app && ../gradlew testDebugUnitTest 2>&1 | tail -30
```

Expected: all tests pass.

- [ ] **Step 3: Fix any remaining compilation errors**

Search for stale references to deleted symbols:

```bash
grep -rn "GoalType\|isFinite\|isInfinite\|weeklyStreak\|streakStartDate\|streakDisplay\|nextMilestone\|justReachedMilestone\|intensityDisplay\|progressDisplay\|MilestoneRecord\|MilestoneRepository\|awardMilestoneBonus" app/src/main/java/ --include="*.kt"
```

Fix any remaining references. Common ones:
- Stale imports in files not explicitly listed above
- References in `WeeklySummary` or other models not yet checked

- [ ] **Step 4: Commit any remaining fixes**

```bash
git add -A
git commit -m "fix: resolve remaining stale references to infinite goal code"
```

- [ ] **Step 5: Run the app on emulator/device**

```bash
cd app && ../gradlew installDebug
```

Verify manually:
- Goal creation requires a deadline
- Goals show progress bar, weeks remaining, intensity
- Ritual flow shows progress instead of streak
- ProgressScreen has no milestones section
- GoalDetailScreen has "Completar objetivo" button
