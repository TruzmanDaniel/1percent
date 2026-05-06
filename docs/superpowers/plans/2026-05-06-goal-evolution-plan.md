# Goal Evolution System — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bifurcate the goal system into Finite (deadline-based projects) and Infinite (lifelong habits), replace the AlertDialog weekly ritual with an immersive full-screen experience, and add pause/vacation functionality.

**Architecture:** Enfoque B — computed `GoalType` property on `Goal` derived from `deadline != null`, centralized display logic in `GoalExtensions.kt`, dedicated `RitualScreen` with state-machine `RitualViewModel`, and `PausedBy` enum to resolve individual vs vacation pause conflicts. All existing patterns (singleton repositories, `MutableStateFlow<UiState>`, kotlinx.serialization, Firestore via `encodeToMap()`) are preserved.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Firebase Firestore, OpenAI GPT-4o-mini via Retrofit, kotlinx.serialization, Vico charts

**Spec:** `docs/superpowers/specs/2026-05-06-goal-evolution-design.md`

---

## File Map

### New Files
| File | Responsibility |
|------|---------------|
| `data/model/enums/GoalType.kt` | `FINITE`, `INFINITE` enum |
| `data/model/enums/PausedBy.kt` | `USER`, `VACATION` enum |
| `data/model/GoalExtensions.kt` | Computed display properties for Goal |
| `data/model/MilestoneRecord.kt` | Persistence model for unlocked milestones |
| `data/MilestoneRepository.kt` | Firestore CRUD for milestones subcollection |
| `ui/screens/ritual/RitualStep.kt` | Step enum for ritual state machine |
| `ui/screens/ritual/RitualUiState.kt` | UI state data class |
| `ui/screens/ritual/RitualViewModel.kt` | Ritual business logic + AI generation |
| `ui/screens/ritual/RitualScreen.kt` | Full-screen composable with step rendering |
| `test/.../GoalExtensionsTest.kt` | Unit tests for extension functions |
| `test/.../IntensityCalculationTest.kt` | Unit tests for intensity formulas |

### Modified Files
| File | Change |
|------|--------|
| `data/model/Goal.kt` | Add `weeklyStreak`, `extensionCount`, `streakStartDate`, `pausedBy` fields + computed `goalType` |
| `data/model/UserProfile.kt` | Add `isVacationMode`, `vacationStartDate` |
| `data/model/enums/AiRoadmapStatus.kt` | Add `PAUSED` value |
| `data/ai/AICoachService.kt` | Differentiated prompts + intensity curves for finite/infinite |
| `data/XpManager.kt` | Add `awardMilestoneBonus()` + update `awardGoalCompletionBonus()` |
| `data/GoalRepository.kt` | Add `pauseGoal()`, `resumeGoal()`, `activateVacationMode()`, `deactivateVacationMode()` |
| `navigation/AppScreens.kt` | Add `RitualScreen` route |
| `navigation/NavGraph.kt` | Add ritual composable route, pass goalId arg |
| `ui/screens/home/HomeViewModel.kt` | Replace dialog trigger with navigation to ritual route |
| `ui/screens/home/HomeScreen.kt` | Remove ritual dialogs, add vacation banner |
| `ui/screens/home/HomeUiState.kt` | Remove ritual dialog fields, add vacation fields |
| `ui/screens/targets/GoalDetailScreen.kt` | Differentiated header (finite vs infinite) + pause menu option |
| `ui/screens/targets/GoalDetailViewModel.kt` | Add pause/resume goal actions |
| `ui/screens/targets/TargetsScreen.kt` | Differentiated goal cards (finite vs infinite) + paused badge |
| `ui/screens/profile/ProfileScreen.kt` | Add vacation mode toggle |
| `ui/screens/profile/ProfileViewModel.kt` | Add vacation mode toggle logic |
| `ui/screens/progress/ProgressScreen.kt` | Add milestones section |
| `ui/screens/progress/ProgressViewModel.kt` | Load milestones data |

---

## Task 1: New Enums (GoalType, PausedBy) + Update AiRoadmapStatus

**Files:**
- Create: `app/src/main/java/es/uc3m/android/a1percent/data/model/enums/GoalType.kt`
- Create: `app/src/main/java/es/uc3m/android/a1percent/data/model/enums/PausedBy.kt`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/data/model/enums/AiRoadmapStatus.kt`

- [ ] **Step 1: Create GoalType enum**

```kotlin
package es.uc3m.android.a1percent.data.model.enums

import kotlinx.serialization.Serializable

@Serializable
enum class GoalType { FINITE, INFINITE }
```

- [ ] **Step 2: Create PausedBy enum**

```kotlin
package es.uc3m.android.a1percent.data.model.enums

import kotlinx.serialization.Serializable

@Serializable
enum class PausedBy { USER, VACATION }
```

- [ ] **Step 3: Add PAUSED to AiRoadmapStatus**

Replace the full contents of `AiRoadmapStatus.kt` with:

```kotlin
package es.uc3m.android.a1percent.data.model.enums

import kotlinx.serialization.Serializable

@Serializable
enum class AiRoadmapStatus {
    NONE,
    NEGOTIATING,
    READY,
    PAUSED
}
```

- [ ] **Step 4: Verify the project compiles**

Run: `./gradlew assembleDebug` from the project root.
Expected: BUILD SUCCESSFUL (new enums are unused so far — no compile errors).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/data/model/enums/GoalType.kt \
       app/src/main/java/es/uc3m/android/a1percent/data/model/enums/PausedBy.kt \
       app/src/main/java/es/uc3m/android/a1percent/data/model/enums/AiRoadmapStatus.kt
git commit -m "feat: add GoalType, PausedBy enums and PAUSED status to AiRoadmapStatus"
```

---

## Task 2: Update Goal and UserProfile Models

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/data/model/Goal.kt`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/data/model/UserProfile.kt`

- [ ] **Step 1: Add new fields to Goal.kt**

Replace the full file with:

```kotlin
package es.uc3m.android.a1percent.data.model

import es.uc3m.android.a1percent.data.model.enums.AiRoadmapStatus
import es.uc3m.android.a1percent.data.model.enums.Category
import es.uc3m.android.a1percent.data.model.enums.GoalStatus
import es.uc3m.android.a1percent.data.model.enums.GoalType
import es.uc3m.android.a1percent.data.model.enums.PausedBy
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.UUID

@Serializable
data class Goal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val category: Category,
    val difficulty: Int,
    val xp: Int,
    val deadline: Long? = null,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val progress: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val ownerId: String = "",
    val sharedWith: List<String> = emptyList(),
    val currentIntensity: Float = difficulty.toFloat(),
    val nextGenerationDate: Long? = null,
    val aiRoadmapStatus: AiRoadmapStatus = AiRoadmapStatus.NONE,
    val weeklyStreak: Int = 0,
    val extensionCount: Int = 0,
    val streakStartDate: Long? = null,
    val pausedBy: PausedBy? = null
) {
    init {
        require(difficulty in 1..5) { "Goal difficulty must be between 1 and 5" }
        require(progress in 0..100) { "Goal progress must be between 0 and 100" }
    }

    @Transient
    val goalType: GoalType = if (deadline != null) GoalType.FINITE else GoalType.INFINITE
}
```

Note: `@Transient` prevents kotlinx.serialization from encoding `goalType` to Firestore. The value is computed from `deadline` at construction time.

- [ ] **Step 2: Add vacation fields to UserProfile.kt**

Replace the full file with:

```kotlin
package es.uc3m.android.a1percent.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val password: String = "",
    val createdAt: Long? = null,
    val level: Int = 1,
    val currentXp: Int = 0,
    val xpToNextLevel: Int = 100,
    val avatarUrl: String? = null,
    val streakDays: Int = 0,
    val totalTasksCompleted: Int = 0,
    val availableCredits: Int = 5,
    val creditsResetDate: Long? = null,
    val lastActivityDate: Long? = null,
    val isVacationMode: Boolean = false,
    val vacationStartDate: Long? = null
)
```

- [ ] **Step 3: Verify the project compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL. Existing code uses `Goal(...)` with named parameters and defaults, so new fields with defaults won't break callers.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/data/model/Goal.kt \
       app/src/main/java/es/uc3m/android/a1percent/data/model/UserProfile.kt
git commit -m "feat: add weeklyStreak, extensionCount, pausedBy to Goal + vacation fields to UserProfile"
```

---

## Task 3: GoalExtensions + Unit Tests

**Files:**
- Create: `app/src/main/java/es/uc3m/android/a1percent/data/model/GoalExtensions.kt`
- Create: `app/src/test/java/es/uc3m/android/a1percent/data/model/GoalExtensionsTest.kt`

- [ ] **Step 1: Write the tests first**

Create `app/src/test/java/es/uc3m/android/a1percent/data/model/GoalExtensionsTest.kt`:

```kotlin
package es.uc3m.android.a1percent.data.model

import es.uc3m.android.a1percent.data.model.enums.Category
import es.uc3m.android.a1percent.data.model.enums.GoalType
import org.junit.Assert.*
import org.junit.Test

class GoalExtensionsTest {

    private fun finiteGoal(
        deadline: Long = System.currentTimeMillis() + 52 * 7 * 24 * 3600 * 1000L,
        weeklyStreak: Int = 0,
        currentIntensity: Float = 3.0f,
        progress: Int = 35,
        extensionCount: Int = 0
    ) = Goal(
        title = "Marathon",
        category = Category.FITNESS,
        difficulty = 3,
        xp = 100,
        deadline = deadline,
        progress = progress,
        currentIntensity = currentIntensity,
        weeklyStreak = weeklyStreak,
        extensionCount = extensionCount
    )

    private fun infiniteGoal(
        weeklyStreak: Int = 0,
        currentIntensity: Float = 3.5f
    ) = Goal(
        title = "Run better",
        category = Category.FITNESS,
        difficulty = 3,
        xp = 100,
        deadline = null,
        currentIntensity = currentIntensity,
        weeklyStreak = weeklyStreak
    )

    @Test
    fun `finite goal has FINITE type`() {
        assertEquals(GoalType.FINITE, finiteGoal().goalType)
        assertTrue(finiteGoal().isFinite)
        assertFalse(finiteGoal().isInfinite)
    }

    @Test
    fun `infinite goal has INFINITE type`() {
        assertEquals(GoalType.INFINITE, infiniteGoal().goalType)
        assertTrue(infiniteGoal().isInfinite)
        assertFalse(infiniteGoal().isFinite)
    }

    @Test
    fun `weekLabel shows total weeks for finite goals`() {
        assertEquals("Semana 8 de 52", finiteGoal().weekLabel(8))
    }

    @Test
    fun `weekLabel omits total for infinite goals`() {
        assertEquals("Semana 8", infiniteGoal().weekLabel(8))
    }

    @Test
    fun `progressDisplay returns value for finite, null for infinite`() {
        assertEquals(35, finiteGoal(progress = 35).progressDisplay())
        assertNull(infiniteGoal().progressDisplay())
    }

    @Test
    fun `intensityDisplay returns formatted string for infinite, null for finite`() {
        assertEquals("3.5", infiniteGoal(currentIntensity = 3.5f).intensityDisplay())
        assertNull(finiteGoal().intensityDisplay())
    }

    @Test
    fun `streakDisplay returns formatted string for infinite, null for finite`() {
        assertEquals("12 sem", infiniteGoal(weeklyStreak = 12).streakDisplay())
        assertNull(finiteGoal().streakDisplay())
    }

    @Test
    fun `weeksRemaining returns weeks for finite, null for infinite`() {
        val fourWeeksFromNow = System.currentTimeMillis() + 4 * 7 * 24 * 3600 * 1000L
        val goal = finiteGoal(deadline = fourWeeksFromNow)
        val remaining = goal.weeksRemaining()
        assertNotNull(remaining)
        assertTrue(remaining!! in 3..5) // Allow for rounding
        assertNull(infiniteGoal().weeksRemaining())
    }

    @Test
    fun `nextMilestone returns correct next milestone for infinite goals`() {
        assertEquals(4, infiniteGoal(weeklyStreak = 0).nextMilestone())
        assertEquals(4, infiniteGoal(weeklyStreak = 2).nextMilestone())
        assertEquals(12, infiniteGoal(weeklyStreak = 4).nextMilestone())
        assertEquals(12, infiniteGoal(weeklyStreak = 8).nextMilestone())
        assertEquals(26, infiniteGoal(weeklyStreak = 12).nextMilestone())
        assertEquals(52, infiniteGoal(weeklyStreak = 26).nextMilestone())
        assertEquals(56, infiniteGoal(weeklyStreak = 52).nextMilestone()) // Second cycle
        assertNull(finiteGoal().nextMilestone())
    }

    @Test
    fun `justReachedMilestone detects correct milestone`() {
        assertNull(infiniteGoal(weeklyStreak = 3).justReachedMilestone())
        assertEquals(4, infiniteGoal(weeklyStreak = 4).justReachedMilestone())
        assertEquals(12, infiniteGoal(weeklyStreak = 12).justReachedMilestone())
        assertEquals(26, infiniteGoal(weeklyStreak = 26).justReachedMilestone())
        assertEquals(52, infiniteGoal(weeklyStreak = 52).justReachedMilestone())
    }

    @Test
    fun `justReachedMilestone prioritizes larger milestone`() {
        // 52 is divisible by 4 — should return 52, not 4
        assertEquals(52, infiniteGoal(weeklyStreak = 52).justReachedMilestone())
        // 12 is divisible by 4 — should return 12, not 4
        assertEquals(12, infiniteGoal(weeklyStreak = 12).justReachedMilestone())
    }

    @Test
    fun `justReachedMilestone works in second cycle`() {
        assertEquals(4, infiniteGoal(weeklyStreak = 56).justReachedMilestone()) // 56 % 4 == 0, 56 % 12 != 0
        assertEquals(12, infiniteGoal(weeklyStreak = 60).justReachedMilestone()) // 60 % 12 == 0, 60 % 26 != 0
    }

    @Test
    fun `justReachedMilestone returns null for finite goals`() {
        assertNull(finiteGoal(weeklyStreak = 4).justReachedMilestone())
    }

    @Test
    fun `justReachedMilestone returns null for zero streak`() {
        assertNull(infiniteGoal(weeklyStreak = 0).justReachedMilestone())
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "es.uc3m.android.a1percent.data.model.GoalExtensionsTest" --info`
Expected: FAIL — `GoalExtensions.kt` doesn't exist yet.

- [ ] **Step 3: Implement GoalExtensions.kt**

Create `app/src/main/java/es/uc3m/android/a1percent/data/model/GoalExtensions.kt`:

```kotlin
package es.uc3m.android.a1percent.data.model

import es.uc3m.android.a1percent.data.model.enums.GoalType

val Goal.isFinite: Boolean get() = goalType == GoalType.FINITE
val Goal.isInfinite: Boolean get() = goalType == GoalType.INFINITE

fun Goal.weekLabel(currentWeek: Int): String {
    return if (isFinite) {
        val totalWeeks = totalWeeks()
        "Semana $currentWeek de $totalWeeks"
    } else {
        "Semana $currentWeek"
    }
}

fun Goal.progressDisplay(): Int? = if (isFinite) progress else null

fun Goal.intensityDisplay(): String? {
    if (isFinite) return null
    return if (currentIntensity == currentIntensity.toLong().toFloat()) {
        currentIntensity.toLong().toString()
    } else {
        "%.1f".format(currentIntensity)
    }
}

fun Goal.streakDisplay(): String? =
    if (isInfinite && weeklyStreak > 0) "$weeklyStreak sem" else if (isInfinite) "0 sem" else null

fun Goal.weeksRemaining(): Int? {
    if (isInfinite || deadline == null) return null
    val now = System.currentTimeMillis()
    val remainingMs = (deadline - now).coerceAtLeast(0)
    return (remainingMs / (7L * 24 * 3600 * 1000)).toInt()
}

fun Goal.nextMilestone(): Int? {
    if (isFinite) return null
    val milestones = listOf(4, 12, 26, 52)
    for (m in milestones) {
        if (weeklyStreak < m) return m
    }
    // Second+ cycle: find the next milestone in the repeating 52-week cycle
    val posInCycle = weeklyStreak % 52
    for (m in milestones) {
        if (posInCycle < m) return weeklyStreak - posInCycle + m
    }
    return weeklyStreak - posInCycle + 52 + 4
}

fun Goal.justReachedMilestone(): Int? {
    if (isFinite || weeklyStreak == 0) return null
    val milestones = listOf(52, 26, 12, 4) // Descending: largest wins
    return milestones.firstOrNull { weeklyStreak % it == 0 }
}

private fun Goal.totalWeeks(): Int {
    if (deadline == null) return 0
    return ((deadline - createdAt) / (7L * 24 * 3600 * 1000)).toInt().coerceAtLeast(1)
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "es.uc3m.android.a1percent.data.model.GoalExtensionsTest" --info`
Expected: All 12 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/data/model/GoalExtensions.kt \
       app/src/test/java/es/uc3m/android/a1percent/data/model/GoalExtensionsTest.kt
git commit -m "feat: add GoalExtensions with display logic + unit tests"
```

---

## Task 4: MilestoneRecord Model + MilestoneRepository

**Files:**
- Create: `app/src/main/java/es/uc3m/android/a1percent/data/model/MilestoneRecord.kt`
- Create: `app/src/main/java/es/uc3m/android/a1percent/data/MilestoneRepository.kt`

- [ ] **Step 1: Create MilestoneRecord model**

```kotlin
package es.uc3m.android.a1percent.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class MilestoneRecord(
    val id: String = UUID.randomUUID().toString(),
    val milestone: Int,
    val weeklyStreak: Int,
    val xpAwarded: Int,
    val unlockedAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 2: Create MilestoneRepository**

```kotlin
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
```

- [ ] **Step 3: Verify the project compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/data/model/MilestoneRecord.kt \
       app/src/main/java/es/uc3m/android/a1percent/data/MilestoneRepository.kt
git commit -m "feat: add MilestoneRecord model and MilestoneRepository"
```

---

## Task 5: Update XpManager with Milestone Bonus

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/data/XpManager.kt`

- [ ] **Step 1: Add awardMilestoneBonus method**

Add the following method to `XpManager` object, after the existing `awardGoalCompletionBonus` method (after line 77):

```kotlin
    suspend fun awardMilestoneBonus(userId: String, goal: Goal, milestone: Int): Result<Unit> {
        val profile = SessionRepository.currentUser.value
            ?: return Result.failure(IllegalStateException("No logged-in user"))
        if (profile.id != userId) return Result.failure(IllegalStateException("User mismatch"))

        val multiplier = when (milestone) {
            4 -> 1; 12 -> 2; 26 -> 3; 52 -> 5
            else -> 1
        }
        val bonus = goal.difficulty * 40 * multiplier

        val record = MilestoneRecord(
            milestone = milestone,
            weeklyStreak = goal.weeklyStreak,
            xpAwarded = bonus
        )
        MilestoneRepository.saveMilestone(goal.id, record)

        val updated = applyXpGain(profile, bonus)
        return SessionRepository.updateUserProfile(updated)
    }
```

- [ ] **Step 2: Update awardGoalCompletionBonus to work for all finite goals**

Replace the `awardGoalCompletionBonus` method (lines 67-77) with:

```kotlin
    suspend fun awardGoalCompletionBonus(userId: String, goal: Goal): Result<Unit> {
        val profile = SessionRepository.currentUser.value
            ?: return Result.failure(IllegalStateException("No logged-in user"))
        if (profile.id != userId) return Result.failure(IllegalStateException("User mismatch"))

        val bonus = goal.difficulty * 50
        val updated = applyXpGain(profile, bonus)
        return SessionRepository.updateUserProfile(updated)
    }
```

Note: Removed the `if (goal.deadline == null) return` guard — completion bonus now applies to all goals that reach COMPLETED status (the caller decides when to award it).

- [ ] **Step 3: Add the missing import**

Add to the imports at the top of XpManager.kt:

```kotlin
import es.uc3m.android.a1percent.data.model.MilestoneRecord
```

- [ ] **Step 4: Verify the project compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/data/XpManager.kt
git commit -m "feat: add awardMilestoneBonus to XpManager + update goal completion bonus"
```

---

## Task 6: Differentiated AI Prompts + Intensity Curves

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/data/ai/AICoachService.kt`
- Create: `app/src/test/java/es/uc3m/android/a1percent/data/ai/IntensityCalculationTest.kt`

- [ ] **Step 1: Write intensity calculation tests**

Create `app/src/test/java/es/uc3m/android/a1percent/data/ai/IntensityCalculationTest.kt`:

```kotlin
package es.uc3m.android.a1percent.data.ai

import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.enums.Category
import org.junit.Assert.*
import org.junit.Test

class IntensityCalculationTest {

    private fun goal(
        deadline: Long? = null,
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
    fun `infinite goal caps at difficulty x 1_5`() {
        val infiniteGoal = goal(difficulty = 4, currentIntensity = 5.9f)
        val result = AICoachService.calculateNewIntensity(
            goal = infiniteGoal,
            epicPassed = true,
            feedback = "SOBRADO"
        )
        assertTrue(result <= 4 * 1.5f)
    }

    @Test
    fun `finite goal caps at difficulty x 2_0`() {
        val fourWeeks = System.currentTimeMillis() + 4 * 7 * 24 * 3600 * 1000L
        val finiteGoal = goal(deadline = fourWeeks, difficulty = 3, currentIntensity = 5.8f)
        val result = AICoachService.calculateNewIntensity(
            goal = finiteGoal,
            epicPassed = true,
            feedback = "SOBRADO"
        )
        assertTrue(result <= 3 * 2.0f)
    }

    @Test
    fun `sprint final accelerates growth for finite goals with less than 4 weeks`() {
        val twoWeeks = System.currentTimeMillis() + 2 * 7 * 24 * 3600 * 1000L
        val finiteGoal = goal(deadline = twoWeeks, difficulty = 5, currentIntensity = 3.0f)
        val normalGrowth = AICoachService.calculateNewIntensity(
            goal = goal(deadline = System.currentTimeMillis() + 20 * 7 * 24 * 3600 * 1000L, difficulty = 5, currentIntensity = 3.0f),
            epicPassed = true,
            feedback = "PERFECTO"
        )
        val sprintGrowth = AICoachService.calculateNewIntensity(
            goal = finiteGoal,
            epicPassed = true,
            feedback = "PERFECTO"
        )
        assertTrue(sprintGrowth > normalGrowth)
    }

    @Test
    fun `agotado feedback reduces intensity`() {
        val infiniteGoal = goal(currentIntensity = 3.0f)
        val normal = AICoachService.calculateNewIntensity(
            goal = infiniteGoal, epicPassed = true, feedback = "PERFECTO"
        )
        val agotado = AICoachService.calculateNewIntensity(
            goal = infiniteGoal, epicPassed = true, feedback = "AGOTADO"
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

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "es.uc3m.android.a1percent.data.ai.IntensityCalculationTest" --info`
Expected: FAIL — the new `calculateNewIntensity(goal, ...)` signature doesn't exist yet.

- [ ] **Step 3: Update AICoachService with new intensity calculations**

Replace `calculateNewIntensity` and `calculateCatchUpIntensity` methods (lines 138-171) with:

```kotlin
    fun calculateNewIntensity(
        goal: Goal,
        epicPassed: Boolean,
        feedback: String?
    ): Float {
        if (!epicPassed) return goal.currentIntensity

        val maxIntensity = if (goal.goalType == GoalType.FINITE) {
            goal.difficulty * 2.0f
        } else {
            goal.difficulty * 1.5f
        }

        val weeksLeft = goal.weeksRemaining()
        val growthMultiplier = if (goal.isFinite && weeksLeft != null && weeksLeft <= 4) {
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
        val maxIntensity = if (goal.isFinite) {
            goal.difficulty * 2.0f
        } else {
            goal.difficulty * 1.5f
        }

        val reduced = goal.currentIntensity * 0.85f

        val adjusted = when (feedback) {
            "SOBRADO" -> reduced * 1.05f
            "AGOTADO" -> reduced * 0.90f
            else -> reduced
        }

        return minOf(adjusted, maxIntensity)
    }
```

- [ ] **Step 4: Add required imports to AICoachService.kt**

Add at the top of the file:

```kotlin
import es.uc3m.android.a1percent.data.model.enums.GoalType
import es.uc3m.android.a1percent.data.model.isFinite
import es.uc3m.android.a1percent.data.model.weeksRemaining
```

- [ ] **Step 5: Update buildPrompt to include goal type context**

Replace the `buildPrompt` method (lines 71-136) with:

```kotlin
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

        val goalTypeContext = buildGoalTypeContext(goal)

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

    private fun buildGoalTypeContext(goal: Goal): String {
        return when (goal.goalType) {
            GoalType.FINITE -> {
                val weeksLeft = goal.weeksRemaining() ?: 0
                val totalWeeks = if (goal.deadline != null) {
                    ((goal.deadline - goal.createdAt) / (7L * 24 * 3600 * 1000)).toInt().coerceAtLeast(1)
                } else 0
                """
                CONTEXTO DEL PROYECTO:
                - Tipo: Proyecto finito con fecha límite
                - Semanas restantes: $weeksLeft de $totalWeeks
                - Progreso actual: ${goal.progress}%
                - Extensiones usadas: ${goal.extensionCount}

                DIRECTRIZ: Este es un proyecto con fecha de examen. Diseña las misiones
                para un progreso lineal que se intensifique gradualmente hacia el deadline.
                Si quedan pocas semanas, prioriza las tareas más críticas para el objetivo
                final. La misión épica debe simular un "ensayo general" del reto final.
                """.trimIndent()
            }
            GoalType.INFINITE -> {
                val nextMilestone = goal.nextMilestone()
                """
                CONTEXTO DEL HÁBITO:
                - Tipo: Hábito de por vida (sin fecha límite)
                - Racha actual: ${goal.weeklyStreak} semanas consecutivas
                - Próximo hito de constancia: ${nextMilestone ?: "N/A"} semanas

                DIRECTRIZ: Este es un hábito para toda la vida. Prioriza la variedad y
                la sostenibilidad a largo plazo. Evita la monotonía rotando tipos de
                actividad. La misión épica debe ser un pico de motivación y diversión,
                no un examen. ${if (goal.weeklyStreak > 12) "La racha es larga: introduce retos creativos para mantener el interés fresco." else ""}
                """.trimIndent()
            }
        }
    }
```

Also add the import for `nextMilestone`:

```kotlin
import es.uc3m.android.a1percent.data.model.nextMilestone
```

- [ ] **Step 6: Run intensity tests**

Run: `./gradlew test --tests "es.uc3m.android.a1percent.data.ai.IntensityCalculationTest" --info`
Expected: All 5 tests PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/data/ai/AICoachService.kt \
       app/src/test/java/es/uc3m/android/a1percent/data/ai/IntensityCalculationTest.kt
git commit -m "feat: differentiated AI prompts and intensity curves for finite/infinite goals"
```

---

## Task 7: GoalRepository — Pause/Resume/Vacation Methods

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/data/GoalRepository.kt`

- [ ] **Step 1: Add pause and resume methods**

Add these methods to `GoalRepository` object, after `shareGoal` (line 123):

```kotlin
    suspend fun pauseGoal(goal: Goal): Result<Unit> {
        val paused = goal.copy(
            aiRoadmapStatus = AiRoadmapStatus.PAUSED,
            pausedBy = PausedBy.USER
        )
        return updateGoal(paused)
    }

    suspend fun resumeGoal(goal: Goal): Result<Unit> {
        val resumed = goal.copy(
            aiRoadmapStatus = AiRoadmapStatus.READY,
            pausedBy = null,
            nextGenerationDate = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000
        )
        return updateGoal(resumed)
    }

    suspend fun activateVacationMode(userId: String): Result<Unit> {
        return try {
            val goals = getGoals(userId).getOrThrow()
            val batch = db.batch()

            goals.filter { it.aiRoadmapStatus == AiRoadmapStatus.READY }.forEach { goal ->
                val paused = goal.copy(
                    aiRoadmapStatus = AiRoadmapStatus.PAUSED,
                    pausedBy = PausedBy.VACATION
                )
                batch.set(goalsCollection.document(goal.id), paused.encodeToMap()!!)
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deactivateVacationMode(userId: String): Result<Unit> {
        return try {
            val goals = getGoals(userId).getOrThrow()
            val now = System.currentTimeMillis()
            val batch = db.batch()

            goals.filter { it.pausedBy == PausedBy.VACATION }.forEach { goal ->
                val resumed = goal.copy(
                    aiRoadmapStatus = AiRoadmapStatus.READY,
                    pausedBy = null,
                    nextGenerationDate = now + 7L * 24 * 60 * 60 * 1000
                )
                batch.set(goalsCollection.document(goal.id), resumed.encodeToMap()!!)
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
```

- [ ] **Step 2: Add required imports**

Add to GoalRepository.kt imports:

```kotlin
import es.uc3m.android.a1percent.data.model.enums.AiRoadmapStatus
import es.uc3m.android.a1percent.data.model.enums.PausedBy
```

- [ ] **Step 3: Verify the project compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/data/GoalRepository.kt
git commit -m "feat: add pause/resume/vacation methods to GoalRepository"
```

---

## Task 8: Ritual Screen — State, ViewModel, and Route

**Files:**
- Create: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/ritual/RitualStep.kt`
- Create: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/ritual/RitualUiState.kt`
- Create: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/ritual/RitualViewModel.kt`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/navigation/AppScreens.kt`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/navigation/NavGraph.kt`

- [ ] **Step 1: Create RitualStep enum**

```kotlin
package es.uc3m.android.a1percent.ui.screens.ritual

enum class RitualStep {
    SUMMARY,
    EPIC_RESULT,
    DEADLINE_CHECK,
    FEEDBACK,
    INTENSITY_CHANGE,
    MILESTONE,
    GENERATING,
    COMPLETE
}
```

- [ ] **Step 2: Create RitualUiState**

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
    val milestoneReached: Int? = null,
    val newDeadline: Long? = null,
    val isGenerating: Boolean = false,
    val generationComplete: Boolean = false,
    val isCatchUp: Boolean = false,
    val goalCompleted: Boolean = false,
    val showDatePicker: Boolean = false,
    val weekNumber: Int = 0,
    val newWeeklyStreak: Int = 0
) {
    val currentStep: RitualStep?
        get() = visibleSteps.getOrNull(currentStepIndex)

    val canSkip: Boolean
        get() = currentStep in listOf(RitualStep.SUMMARY, RitualStep.EPIC_RESULT, RitualStep.INTENSITY_CHANGE)

    val isLastStep: Boolean
        get() = currentStepIndex >= visibleSteps.lastIndex
}
```

- [ ] **Step 3: Create RitualViewModel**

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
import es.uc3m.android.a1percent.data.model.isFinite
import es.uc3m.android.a1percent.data.model.isInfinite
import es.uc3m.android.a1percent.data.model.justReachedMilestone
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
            val epicTask = goalTasks.find { it.dayIndex == 7 }
            val epicPassed = epicTask?.status == TaskStatus.COMPLETED
            val xpEarned = goalTasks.filter { it.status == TaskStatus.COMPLETED }
                .sumOf { it.xpAwarded ?: it.xp }

            val now = System.currentTimeMillis()
            val isCatchUp = (now - (goal.nextGenerationDate ?: 0)) > SEVEN_DAYS_MILLIS * 2

            val latestSummary = WeeklySummaryRepository.getLatestSummary(goal.id).getOrNull()
            val weekNumber = (latestSummary?.weekNumber ?: 0) + 1

            val newStreak = if (isCatchUp) {
                goal.weeklyStreak
            } else if (completed > 0) {
                goal.weeklyStreak + 1
            } else {
                0
            }

            val steps = computeVisibleSteps(goal, isCatchUp, newStreak)

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
                    newWeeklyStreak = newStreak
                )
            }
        }
    }

    private fun computeVisibleSteps(goal: Goal, isCatchUp: Boolean, newStreak: Int): List<RitualStep> {
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

        if (goal.isFinite && goal.deadline != null) {
            val now = System.currentTimeMillis()
            if (goal.deadline in now..(now + SEVEN_DAYS_MILLIS)) {
                steps.add(RitualStep.DEADLINE_CHECK)
            }
        }

        steps.add(RitualStep.FEEDBACK)
        steps.add(RitualStep.INTENSITY_CHANGE)

        val tempGoal = goal.copy(weeklyStreak = newStreak)
        if (tempGoal.isInfinite && tempGoal.justReachedMilestone() != null) {
            steps.add(RitualStep.MILESTONE)
            _uiState.update { it.copy(milestoneReached = tempGoal.justReachedMilestone()) }
        }

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
        val newStreak = _uiState.value.newWeeklyStreak
        val milestoneReached = _uiState.value.milestoneReached

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

                if (goal.isFinite && goal.deadline != null) {
                    val totalWeeks = ((goal.deadline - goal.createdAt) / SEVEN_DAYS_MILLIS).toInt().coerceAtLeast(1)
                    val newProgress = ((weekNumber * 100) / totalWeeks).coerceIn(0, 100)
                    GoalRepository.updateGoal(goal.copy(progress = newProgress))
                }

                if (milestoneReached != null) {
                    XpManager.awardMilestoneBonus(userId, goal.copy(weeklyStreak = newStreak), milestoneReached)
                }
            }

            var updatedGoal = goal.copy(
                currentIntensity = newIntensity,
                weeklyStreak = newStreak,
                streakStartDate = if (newStreak == 1 && goal.weeklyStreak == 0) {
                    System.currentTimeMillis()
                } else if (newStreak == 0) {
                    null
                } else {
                    goal.streakStartDate
                }
            )

            if (_uiState.value.newDeadline != null) {
                updatedGoal = updatedGoal.copy(
                    deadline = _uiState.value.newDeadline,
                    extensionCount = goal.extensionCount + 1
                )
            }

            val isWeekend = Calendar.getInstance().let {
                it.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)
            }

            val result = AICoachService.generateWeeklyTasks(
                goal = updatedGoal,
                weeklySummary = if (isCatchUp) null else latestSummary,
                isWeekend = isWeekend,
                userFeedback = feedback.name,
                userId = userId,
                weekNumber = weekNumber
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

- [ ] **Step 4: Add RitualScreen route to AppScreens.kt**

Add after `ProgressScreen` object in `AppScreens.kt`:

```kotlin
    object RitualScreen : AppScreens("ritual", "Ritual")
```

- [ ] **Step 5: Add ritual route to NavGraph.kt**

Add the following composable route inside the `NavHost` block, after the `ProgressScreen` composable (around line 233):

```kotlin
                composable(
                    route = AppScreens.RitualScreen.route + "/{goalId}",
                    arguments = listOf(navArgument("goalId") { type = NavType.StringType }),
                    enterTransition = {
                        fadeIn(tween(FADE_DURATION))
                    },
                    exitTransition = {
                        fadeOut(tween(FADE_DURATION))
                    }
                ) { backStackEntry ->
                    val goalId = backStackEntry.arguments?.getString("goalId") ?: return@composable
                    RitualScreen(
                        goalId = goalId,
                        onFinished = {
                            navController.popBackStack(AppScreens.HomeScreen.route, inclusive = false)
                        }
                    )
                }
```

Add the import at the top of NavGraph.kt:

```kotlin
import es.uc3m.android.a1percent.ui.screens.ritual.RitualScreen
```

- [ ] **Step 6: Verify the project compiles**

Run: `./gradlew assembleDebug`
Expected: FAIL — `RitualScreen` composable doesn't exist yet. This is expected; we'll create it in the next task.

- [ ] **Step 7: Commit what we have (screen is a stub for now)**

Create a temporary placeholder `RitualScreen.kt` so it compiles:

```kotlin
package es.uc3m.android.a1percent.ui.screens.ritual

import androidx.compose.runtime.Composable

@Composable
fun RitualScreen(goalId: String, onFinished: () -> Unit) {
    // Placeholder — implemented in Task 9
}
```

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

```bash
git add app/src/main/java/es/uc3m/android/a1percent/ui/screens/ritual/ \
       app/src/main/java/es/uc3m/android/a1percent/navigation/AppScreens.kt \
       app/src/main/java/es/uc3m/android/a1percent/navigation/NavGraph.kt
git commit -m "feat: add RitualViewModel state machine + ritual route in NavGraph"
```

---

## Task 9: RitualScreen Composable (Full-Screen Multi-Step)

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/ritual/RitualScreen.kt`

- [ ] **Step 1: Replace the placeholder with the full RitualScreen**

```kotlin
package es.uc3m.android.a1percent.ui.screens.ritual

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.uc3m.android.a1percent.data.model.isFinite
import es.uc3m.android.a1percent.data.model.weekLabel
import es.uc3m.android.a1percent.data.model.enums.EnergyFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RitualScreen(
    goalId: String,
    onFinished: () -> Unit,
    viewModel: RitualViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(goalId) { viewModel.loadRitual(goalId) }

    BackHandler { onFinished() }

    if (uiState.goalCompleted) {
        LaunchedEffect(Unit) { onFinished() }
        return
    }

    val goal = uiState.goal ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = uiState.currentStepIndex,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it } + fadeOut())
            },
            label = "RitualStep"
        ) { _ ->
            when (uiState.currentStep) {
                RitualStep.SUMMARY -> SummaryStep(uiState, onNext = viewModel::onNextStep)
                RitualStep.EPIC_RESULT -> EpicResultStep(uiState, onNext = viewModel::onNextStep)
                RitualStep.DEADLINE_CHECK -> DeadlineCheckStep(
                    uiState = uiState,
                    onExtend = viewModel::onShowDatePicker,
                    onComplete = viewModel::onCompleteGoal
                )
                RitualStep.FEEDBACK -> FeedbackStep(
                    isCatchUp = uiState.isCatchUp,
                    onFeedback = viewModel::onFeedbackSelected
                )
                RitualStep.INTENSITY_CHANGE -> IntensityChangeStep(uiState, onNext = viewModel::onNextStep)
                RitualStep.MILESTONE -> MilestoneStep(uiState, onNext = viewModel::onNextStep)
                RitualStep.GENERATING -> {
                    LaunchedEffect(Unit) { viewModel.onStartGeneration() }
                    GeneratingStep(uiState)
                }
                RitualStep.COMPLETE -> CompleteStep(uiState, onFinished = onFinished)
                null -> {}
            }
        }

        if (uiState.canSkip) {
            IconButton(
                onClick = viewModel::onSkipToFeedback,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Saltar", tint = Color.White.copy(alpha = 0.6f))
            }
        }

        if (uiState.showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = viewModel::onDismissDatePicker,
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { viewModel.onExtendDeadline(it) }
                    }) { Text("Confirmar") }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::onDismissDatePicker) { Text("Cancelar") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@Composable
private fun SummaryStep(uiState: RitualUiState, onNext: () -> Unit) {
    val goal = uiState.goal ?: return
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (uiState.isCatchUp) "¡Has vuelto!" else "¡Semana completada!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(goal.title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(goal.weekLabel(uiState.weekNumber), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        if (uiState.isCatchUp) {
            Text("Llevas un tiempo sin entrar.\n¡Vamos a retomar el ritmo!", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${uiState.tasksCompleted}/${uiState.totalTasks}", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("misiones completadas", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("+${uiState.xpEarned} XP", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }

        Spacer(Modifier.height(48.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text("Continuar")
        }
    }
}

@Composable
private fun EpicResultStep(uiState: RitualUiState, onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (uiState.epicMissionPassed) Icons.Default.EmojiEvents else Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = if (uiState.epicMissionPassed) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (uiState.epicMissionPassed) "¡Misión Épica superada!" else "La Épica se resistió esta semana",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text("Continuar")
        }
    }
}

@Composable
private fun DeadlineCheckStep(
    uiState: RitualUiState,
    onExtend: () -> Unit,
    onComplete: () -> Unit
) {
    val goal = uiState.goal ?: return
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Tu deadline ha llegado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("Progreso alcanzado: ${goal.progress}%", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        if (goal.extensionCount > 0) {
            Text("Extensiones previas: ${goal.extensionCount}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(48.dp))
        Button(onClick = onExtend, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text("Extender deadline")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onComplete, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text("Completar objetivo")
        }
    }
}

@Composable
private fun FeedbackStep(isCatchUp: Boolean, onFeedback: (EnergyFeedback) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isCatchUp) "¿Cómo te sientes para volver?" else "¿Cómo te has sentido esta semana?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))

        val options = if (isCatchUp) {
            listOf(
                Triple(EnergyFeedback.SOBRADO, "Con energía", "Estoy listo para volver fuerte"),
                Triple(EnergyFeedback.PERFECTO, "Normal", "Vamos a retomar el ritmo"),
                Triple(EnergyFeedback.AGOTADO, "Cansado", "Necesito ir suave")
            )
        } else {
            listOf(
                Triple(EnergyFeedback.SOBRADO, "Sobrado", "Me sobró energía"),
                Triple(EnergyFeedback.PERFECTO, "Perfecto", "Justo en el punto"),
                Triple(EnergyFeedback.AGOTADO, "Agotado", "Fue demasiado")
            )
        }

        options.forEach { (feedback, label, description) ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                onClick = { onFeedback(feedback) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun IntensityChangeStep(uiState: RitualUiState, onNext: () -> Unit) {
    val old = uiState.oldIntensity ?: 0f
    val new = uiState.newIntensity ?: old
    val isIncrease = new > old
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(64.dp), tint = if (isIncrease) Color(0xFF4CAF50) else Color(0xFFFF5722))
        Spacer(Modifier.height(24.dp))
        Text("Nivel de Intensidad", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("%.1f".format(old), fontSize = 36.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (isIncrease) "→" else "→", fontSize = 24.sp)
            Text("%.1f".format(new), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = if (isIncrease) Color(0xFF4CAF50) else Color(0xFFFF5722))
        }
        Spacer(Modifier.height(48.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text("Continuar")
        }
    }
}

@Composable
private fun MilestoneStep(uiState: RitualUiState, onNext: () -> Unit) {
    val milestone = uiState.milestoneReached ?: return
    val name = when (milestone) {
        4 -> "Primer Mes"
        12 -> "Trimestre de Hierro"
        26 -> "Medio Año Imparable"
        52 -> "Un Año Legendario"
        else -> "$milestone Semanas"
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🏆", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text("¡Hito alcanzado!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
        Spacer(Modifier.height(8.dp))
        Text(name, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Text("${uiState.newWeeklyStreak} semanas consecutivas", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(48.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text("Continuar")
        }
    }
}

@Composable
private fun GeneratingStep(uiState: RitualUiState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(24.dp))
        Text("Preparando tu próxima semana...", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CompleteStep(uiState: RitualUiState, onFinished: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFF4CAF50))
        Spacer(Modifier.height(24.dp))
        Text("¡Tu semana está lista!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        if (uiState.newIntensity != null) {
            Text("Nivel: ${"%.1f".format(uiState.newIntensity)}", style = MaterialTheme.typography.titleMedium)
        }
        if (uiState.newWeeklyStreak > 0) {
            Text("Racha: ${uiState.newWeeklyStreak} semanas", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFD700))
        }
        Spacer(Modifier.height(48.dp))
        Button(onClick = onFinished, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text("Ver misiones")
        }
    }
}
```

- [ ] **Step 2: Verify the project compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/ui/screens/ritual/RitualScreen.kt
git commit -m "feat: implement full-screen immersive RitualScreen with multi-step flow"
```

---

## Task 10: Wire HomeViewModel to Navigate to Ritual + Remove Dialogs

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/home/HomeViewModel.kt`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/home/HomeUiState.kt`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/home/HomeScreen.kt`

- [ ] **Step 1: Update HomeUiState — remove ritual dialog fields, add vacation + navigation**

Replace the full file with:

```kotlin
package es.uc3m.android.a1percent.ui.screens.home

import es.uc3m.android.a1percent.data.model.Goal
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.UserProfile

data class HomeUiState(
    val user: UserProfile? = null,
    val tasks: List<Task> = emptyList(),
    val visibleTasks: List<Task> = emptyList(),
    val filters: HomeFilters = HomeFilters(),
    val filterItems: List<HomeFilterUiItem> = buildHomeFilterUiItems(HomeFilters()),
    val goal: Goal? = null,
    val goals: List<Goal> = emptyList(),
    val navigateToRitual: String? = null,
    val isGeneratingWeek: Boolean = false,
    val selectedTask: Task? = null,
    val showDatePickerForTask: String? = null,
    val showShareSheet: Boolean = false,
    val shareTargetTask: Task? = null,
    val friends: List<UserProfile> = emptyList(),
    val snackbarMessage: String? = null,
    val sharedUserProfilesById: Map<String, UserProfile> = emptyMap(),
    val currentUserId: String = ""
)
```

- [ ] **Step 2: Update HomeViewModel — replace dialog trigger with navigation**

Replace `checkWeeklyRituals` method (lines 113-147) with:

```kotlin
    private fun checkWeeklyRituals(goals: List<Goal>) {
        val now = System.currentTimeMillis()

        val pendingGoal = goals.firstOrNull { goal ->
            goal.aiRoadmapStatus == AiRoadmapStatus.READY
                && goal.nextGenerationDate != null
                && now >= goal.nextGenerationDate
        } ?: return

        _uiState.update { it.copy(navigateToRitual = pendingGoal.id) }
    }
```

Remove the `onWeeklyFeedback` method entirely (lines 149-229) — this logic now lives in `RitualViewModel`.

Remove the `dismissRitual` method (lines 231-237) — no longer needed.

Add a new method to clear the navigation:

```kotlin
    fun onRitualNavigated() {
        _uiState.update { it.copy(navigateToRitual = null) }
    }
```

- [ ] **Step 3: Update HomeScreen — remove dialog composables, add ritual navigation**

In `HomeScreen.kt`, find the section where `WeeklyRitualDialog` and `CatchUpDialog` are shown (look for `if (uiState.showWeeklyRitual)` and `if (uiState.showCatchUp)`). Remove both dialog blocks entirely.

Add a `LaunchedEffect` near the top of the `HomeScreen` composable (after `val uiState by ...`):

```kotlin
    val ritualGoalId = uiState.navigateToRitual
    LaunchedEffect(ritualGoalId) {
        if (ritualGoalId != null) {
            viewModel.onRitualNavigated()
            navController.navigate(AppScreens.RitualScreen.route + "/$ritualGoalId")
        }
    }
```

Add the import:

```kotlin
import es.uc3m.android.a1percent.navigation.AppScreens
```

- [ ] **Step 4: Verify the project compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/ui/screens/home/HomeViewModel.kt \
       app/src/main/java/es/uc3m/android/a1percent/ui/screens/home/HomeUiState.kt \
       app/src/main/java/es/uc3m/android/a1percent/ui/screens/home/HomeScreen.kt
git commit -m "feat: replace ritual dialogs with navigation to immersive RitualScreen"
```

---

## Task 11: Differentiated Goal Cards in TargetsScreen

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsScreen.kt`

- [ ] **Step 1: Find and replace the GoalCompactItem composable**

Locate the `GoalCompactItem` composable in TargetsScreen.kt (the card used in the Goals tab). Replace it with a differentiated version that checks `goal.goalType`:

```kotlin
@Composable
private fun GoalCompactItem(
    goal: Goal,
    friends: List<UserProfile>,
    onGoalClicked: () -> Unit,
    onDeleteGoal: () -> Unit
) {
    val isPaused = goal.aiRoadmapStatus == AiRoadmapStatus.PAUSED
    val alpha = if (isPaused) 0.5f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onGoalClicked)
            .then(if (isPaused) Modifier else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = alpha)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(goal.category.displayName, style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (goal.isFinite) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                )
                if (isPaused) {
                    Text("Pausado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDeleteGoal, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                }
            }

            Text(goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (goal.isFinite) {
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
                if (weeksLeft != null) {
                    Text(
                        "$weeksLeft semanas restantes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Nivel", style = MaterialTheme.typography.labelSmall)
                        Text(
                            goal.intensityDisplay() ?: "1",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Racha", style = MaterialTheme.typography.labelSmall)
                        Text(
                            goal.streakDisplay() ?: "0 sem",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFFD700)
                        )
                    }
                }

                val nextMs = goal.nextMilestone()
                if (nextMs != null) {
                    Text(
                        "Próximo hito: $nextMs semanas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Add required imports to TargetsScreen.kt**

```kotlin
import es.uc3m.android.a1percent.data.model.enums.AiRoadmapStatus
import es.uc3m.android.a1percent.data.model.isFinite
import es.uc3m.android.a1percent.data.model.intensityDisplay
import es.uc3m.android.a1percent.data.model.streakDisplay
import es.uc3m.android.a1percent.data.model.weeksRemaining
import es.uc3m.android.a1percent.data.model.nextMilestone
```

- [ ] **Step 3: Verify the project compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsScreen.kt
git commit -m "feat: differentiated goal cards in TargetsScreen (finite vs infinite)"
```

---

## Task 12: Differentiated GoalDetailScreen Header + Pause Menu

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/GoalDetailScreen.kt`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/GoalDetailViewModel.kt`

- [ ] **Step 1: Add pause/resume to GoalDetailViewModel**

Add these methods to `GoalDetailViewModel` class, after `clearSnackbarMessage()`:

```kotlin
    fun onTogglePause() {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch {
            val result = if (goal.aiRoadmapStatus == AiRoadmapStatus.PAUSED) {
                GoalRepository.resumeGoal(goal)
            } else {
                GoalRepository.pauseGoal(goal)
            }
            result.onSuccess {
                val goalId = goal.id
                val userId = SessionRepository.currentUser.value?.id ?: return@onSuccess
                loadGoalForUser(userId, goalId)
                val action = if (goal.aiRoadmapStatus == AiRoadmapStatus.PAUSED) "reanudado" else "pausado"
                _uiState.update { it.copy(snackbarMessage = "Objetivo $action") }
            }
        }
    }
```

Add imports:

```kotlin
import es.uc3m.android.a1percent.data.model.enums.AiRoadmapStatus
```

- [ ] **Step 2: Replace GoalHeaderCard with differentiated version**

Replace the `GoalHeaderCard` composable in GoalDetailScreen.kt (lines 188-288) with:

```kotlin
@Composable
private fun GoalHeaderCard(
    goal: Goal,
    friends: List<UserProfile>,
    currentUserId: String,
    onProfileClicked: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = goal.title, style = MaterialTheme.typography.headlineSmall)

            if (goal.description.isNotEmpty()) {
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            if (goal.isFinite) {
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
                    StatColumn("Semanas", "${goal.weeksRemaining() ?: 0}")
                    StatColumn("Intensidad", "%.1f".format(goal.currentIntensity))
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatColumn("Nivel", goal.intensityDisplay() ?: "1")
                    StatColumn("Racha", goal.streakDisplay() ?: "0 sem")
                    StatColumn("Misiones", "—")
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Category", style = MaterialTheme.typography.labelSmall)
                    Text(text = goal.category.displayName, style = MaterialTheme.typography.bodyMedium)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Status", style = MaterialTheme.typography.labelSmall)
                    Text(text = goal.status.displayName, style = MaterialTheme.typography.bodyMedium)
                }
            }

            SharedWithDropdown(
                sharedProfiles = goal.sharedWith.mapNotNull { userId -> friends.find { it.id == userId } },
                currentUserId = currentUserId,
                onProfileClicked = onProfileClicked
            )
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
```

- [ ] **Step 3: Add pause button to TopAppBar actions**

In the `GoalDetailScreen` composable, update the `actions` block in the `TopAppBar` (around line 76):

```kotlin
                actions = {
                    if (goal != null) {
                        IconButton(onClick = { viewModel.onTogglePause() }) {
                            Icon(
                                if (goal.aiRoadmapStatus == AiRoadmapStatus.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (goal.aiRoadmapStatus == AiRoadmapStatus.PAUSED) "Reanudar" else "Pausar"
                            )
                        }
                        IconButton(onClick = { viewModel.onShareGoalRequested() }) {
                            Icon(Icons.Default.Share, contentDescription = "Share goal")
                        }
                    }
                }
```

- [ ] **Step 4: Add required imports to GoalDetailScreen.kt**

```kotlin
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.text.font.FontWeight
import es.uc3m.android.a1percent.data.model.enums.AiRoadmapStatus
import es.uc3m.android.a1percent.data.model.isFinite
import es.uc3m.android.a1percent.data.model.intensityDisplay
import es.uc3m.android.a1percent.data.model.streakDisplay
import es.uc3m.android.a1percent.data.model.weeksRemaining
```

- [ ] **Step 5: Verify the project compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/GoalDetailScreen.kt \
       app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/GoalDetailViewModel.kt
git commit -m "feat: differentiated GoalDetailScreen header + pause/resume toggle"
```

---

## Task 13: Vacation Mode in ProfileScreen

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/profile/ProfileScreen.kt`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/profile/ProfileViewModel.kt`

- [ ] **Step 1: Add vacation toggle to ProfileViewModel**

Add to `ProfileViewModel` class:

```kotlin
    fun onToggleVacationMode() {
        val user = uiState.value.user ?: return
        val currentUser = SessionRepository.currentUser.value ?: return
        if (user.id != currentUser.id) return

        viewModelScope.launch {
            if (currentUser.isVacationMode) {
                GoalRepository.deactivateVacationMode(currentUser.id)
                val updated = currentUser.copy(isVacationMode = false, vacationStartDate = null)
                SessionRepository.updateUserProfile(updated)
            } else {
                GoalRepository.activateVacationMode(currentUser.id)
                val updated = currentUser.copy(
                    isVacationMode = true,
                    vacationStartDate = System.currentTimeMillis()
                )
                SessionRepository.updateUserProfile(updated)
            }
        }
    }
```

Add import:

```kotlin
import es.uc3m.android.a1percent.data.GoalRepository
```

- [ ] **Step 2: Add vacation toggle UI to ProfileScreen**

In `ProfileBodyContent`, find the stats section for the own profile (where Level, XP, Streak, Credits are shown). Add a vacation toggle card after the stats section:

```kotlin
            if (isOwnProfile) {
                val isVacation = uiState.user?.isVacationMode == true
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isVacation) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                if (isVacation) "Modo Vacaciones activo" else "Modo Vacaciones",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (isVacation && uiState.user?.vacationStartDate != null) {
                                val days = ((System.currentTimeMillis() - uiState.user.vacationStartDate) / (24 * 3600 * 1000)).toInt()
                                Text("Desde hace $days días", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = isVacation,
                            onCheckedChange = { onVacationToggle() }
                        )
                    }
                }
            }
```

Pass the callback through `ProfileBodyContent` parameters:

```kotlin
fun ProfileBodyContent(
    // existing params...
    onVacationToggle: () -> Unit = {},
    // ...
)
```

And in `ProfileScreen` composable, pass it:

```kotlin
ProfileBodyContent(
    // existing params...
    onVacationToggle = { viewModel.onToggleVacationMode() },
    // ...
)
```

Add import:

```kotlin
import androidx.compose.material3.Switch
```

- [ ] **Step 3: Add vacation banner to HomeScreen**

In `HomeScreen.kt`, add a banner at the top of the task list when vacation mode is active:

```kotlin
    val user = uiState.user
    if (user?.isVacationMode == true) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Modo Vacaciones activo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    val days = if (user.vacationStartDate != null) {
                        ((System.currentTimeMillis() - user.vacationStartDate) / (24 * 3600 * 1000)).toInt()
                    } else 0
                    Text("Desde hace $days días", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
```

- [ ] **Step 4: Verify the project compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/ui/screens/profile/ProfileScreen.kt \
       app/src/main/java/es/uc3m/android/a1percent/ui/screens/profile/ProfileViewModel.kt \
       app/src/main/java/es/uc3m/android/a1percent/ui/screens/home/HomeScreen.kt
git commit -m "feat: add vacation mode toggle in ProfileScreen + banner in HomeScreen"
```

---

## Task 14: Update HomeViewModel Callers for New Intensity API

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/home/HomeViewModel.kt`

The old `HomeViewModel.onWeeklyFeedback` was removed in Task 10, but `checkWeeklyRituals` still needs to skip paused goals.

- [ ] **Step 1: Update checkWeeklyRituals to skip PAUSED goals**

The current filter in `checkWeeklyRituals` already checks `goal.aiRoadmapStatus == AiRoadmapStatus.READY`, which naturally excludes `PAUSED` goals. No code change needed — just verify.

- [ ] **Step 2: Run all unit tests**

Run: `./gradlew test --info`
Expected: All tests pass (GoalExtensionsTest + IntensityCalculationTest + ExampleUnitTest).

- [ ] **Step 3: Commit (if any fixes were needed)**

```bash
git add -A
git commit -m "fix: ensure paused goals are excluded from ritual checks"
```

---

## Task 15: Final Cleanup — Remove Dead Dialog Files

**Files:**
- Delete: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/home/WeeklyRitualDialog.kt`
- Delete: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/home/CatchUpDialog.kt`

- [ ] **Step 1: Verify no remaining references to the dialog files**

Search for imports of `WeeklyRitualDialog` and `CatchUpDialog` across the codebase. If any remain in `HomeScreen.kt`, remove those import lines.

- [ ] **Step 2: Delete the files**

```bash
git rm app/src/main/java/es/uc3m/android/a1percent/ui/screens/home/WeeklyRitualDialog.kt \
      app/src/main/java/es/uc3m/android/a1percent/ui/screens/home/CatchUpDialog.kt
```

- [ ] **Step 3: Verify the project compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run all tests one final time**

Run: `./gradlew test --info`
Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git commit -m "chore: remove obsolete WeeklyRitualDialog and CatchUpDialog"
```

---

## Task 16: Milestones Section in ProgressScreen (Spec 5.5)

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/progress/ProgressUiState.kt`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/progress/ProgressViewModel.kt`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/progress/ProgressScreen.kt`

- [ ] **Step 1: Add milestone data to ProgressUiState**

Add this data class at the bottom of `ProgressUiState.kt`:

```kotlin
data class GoalMilestoneItem(
    val goalTitle: String,
    val weekThreshold: Int,
    val milestoneName: String,
    val unlockedAt: Long
)
```

Add this field to `ProgressUiState`:

```kotlin
    // Chart 7 — Milestones achieved per goal
    val milestones: List<GoalMilestoneItem> = emptyList()
```

- [ ] **Step 2: Load milestones in ProgressViewModel**

Add import at the top of `ProgressViewModel.kt`:

```kotlin
import es.uc3m.android.a1percent.data.MilestoneRepository
```

Add a new method to the class, after `loadWeeklySummaries`:

```kotlin
    private fun loadMilestones(goals: List<Goal>) {
        viewModelScope.launch {
            val items = goals.flatMap { goal ->
                val records = MilestoneRepository.getMilestones(goal.id).getOrNull() ?: emptyList()
                records.map { record ->
                    val name = when (record.weekThreshold) {
                        4 -> "Primer Mes"
                        12 -> "Trimestre de Hierro"
                        26 -> "Medio Año Imparable"
                        52 -> "Un Año Legendario"
                        else -> "${record.weekThreshold} Semanas"
                    }
                    GoalMilestoneItem(
                        goalTitle = goal.title,
                        weekThreshold = record.weekThreshold,
                        milestoneName = name,
                        unlockedAt = record.unlockedAt
                    )
                }
            }.sortedByDescending { it.unlockedAt }
            _uiState.update { it.copy(milestones = items) }
        }
    }
```

Call it from the `goalsJob` observer, after `loadWeeklySummaries(goals)`:

```kotlin
        goalsJob = GoalRepository.observeGoals(userId)
            .onEach { goals ->
                recomputeGoalStats(goals)
                loadWeeklySummaries(goals)
                loadMilestones(goals)
            }
            .launchIn(viewModelScope)
```

- [ ] **Step 3: Add MilestonesCard composable to ProgressScreen**

Add this composable in `ProgressScreen.kt`, after `GoalSparklinesCard`:

```kotlin
@Composable
private fun MilestonesCard(milestones: List<GoalMilestoneItem>) {
    ProgressCard(title = "Hitos conseguidos", subtitle = "Badges desbloqueados por objetivo") {
        if (milestones.isEmpty()) {
            Text(
                text = "Mantén rachas semanales para desbloquear hitos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                milestones.forEach { milestone ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    milestone.milestoneName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    milestone.goalTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(
                            "${milestone.weekThreshold} sem",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
```

Add these imports to `ProgressScreen.kt`:

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.ui.text.font.FontWeight
```

- [ ] **Step 4: Add the milestones card to ProgressBodyContent**

In `ProgressBodyContent`, add after the sparklines `item`:

```kotlin
        // Chart 7 — Milestones achieved
        item { MilestonesCard(uiState.milestones) }
```

- [ ] **Step 5: Verify the project compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/es/uc3m/android/a1percent/ui/screens/progress/ProgressUiState.kt \
       app/src/main/java/es/uc3m/android/a1percent/ui/screens/progress/ProgressViewModel.kt \
       app/src/main/java/es/uc3m/android/a1percent/ui/screens/progress/ProgressScreen.kt
git commit -m "feat: add milestones section to ProgressScreen (spec 5.5)"
```

---

## Summary

| Task | Description | New Files | Modified Files |
|------|-------------|-----------|----------------|
| 1 | GoalType, PausedBy enums + PAUSED status | 2 | 1 |
| 2 | Goal + UserProfile model updates | 0 | 2 |
| 3 | GoalExtensions + unit tests | 2 | 0 |
| 4 | MilestoneRecord + MilestoneRepository | 2 | 0 |
| 5 | XpManager milestone bonus | 0 | 1 |
| 6 | AI prompts + intensity curves + tests | 1 | 1 |
| 7 | GoalRepository pause/vacation methods | 0 | 1 |
| 8 | Ritual state + ViewModel + route | 4 | 2 |
| 9 | RitualScreen composable | 1 | 0 |
| 10 | HomeViewModel → ritual navigation | 0 | 3 |
| 11 | Differentiated goal cards (TargetsScreen) | 0 | 1 |
| 12 | Differentiated GoalDetail header + pause | 0 | 2 |
| 13 | Vacation mode (Profile + Home banner) | 0 | 3 |
| 14 | Verify paused goal exclusion + test run | 0 | 0-1 |
| 15 | Remove dead dialog files | 0 | 0 (2 deleted) |
| 16 | Milestones section in ProgressScreen | 0 | 3 |
| **Total** | | **12 new** | **20 modified** |
