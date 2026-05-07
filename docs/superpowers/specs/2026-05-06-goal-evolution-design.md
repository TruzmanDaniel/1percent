# Design: Refactoring Goals to Finite-Only Architecture

**Fecha**: 2026-05-06
**Estado**: En revision
**Alcance**: Eliminacion de goals infinitos, unificacion a sistema finito, reconversion del ritual, Epic Completion mechanic

---

## 1. Background & Motivation

The current architecture bifurcates Goals into "Finite" (with a deadline) and "Infinite" (no deadline). This dual system introduces complexity across the data model, AI prompts, gamification logic, and UI rendering. This refactor eliminates infinite goals entirely, standardizing on a purely finite, mission-based project management system.

---

## 2. Decisions

| Topic | Decision |
|-------|----------|
| Weekly streaks | Delete (`Goal.weeklyStreak`, `streakStartDate`) |
| Daily streaks | Keep (`UserProfile.streakDays`) — untouched |
| Milestones | Delete entirely (`MilestoneRecord`, `MilestoneRepository`, `awardMilestoneBonus`) |
| Ritual semanal | Reconvert to show finite goal progress + epic weekly bonus |
| GoalStatus | Keep all 5: ACTIVE, PAUSED, COMPLETED, ARCHIVED, UPCOMING |
| Epic Completion | Fewer daily missions + 1 Epic in final week; client-side detection |
| Migration | None — DB will be reset, no production users |
| Intensity curve | All goals use finite curve (cap `difficulty * 2.0`, acceleration in final 4 weeks) |
| AI prompt | Improve finite prompt with deadline week mode and UPCOMING handling |
| Approach | Client-side deadline week detection — AI only generates content, never calculates dates |

---

## 3. Data Model

### 3.1 Goal.kt

`deadline` becomes non-nullable. `weeklyStreak`, `streakStartDate`, and computed `goalType` are removed.

```kotlin
data class Goal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val category: Category,
    val difficulty: Int,
    val xp: Int,
    val deadline: Long,                         // Non-nullable, mandatory
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
)
```

### 3.2 Files to Delete

- `GoalType.kt` — FINITE/INFINITE enum no longer exists
- `MilestoneRecord.kt` — milestone data class
- `MilestoneRepository.kt` — milestone Firestore persistence

### 3.3 GoalExtensions.kt

**Delete:**
- `isFinite`, `isInfinite` — no types to check
- `streakDisplay()` — weekly streaks removed
- `nextMilestone()`, `justReachedMilestone()` — milestones removed
- `intensityDisplay()` — only showed for infinite goals
- `progressDisplay()` — guard no longer needed, progress always applies

**Keep and simplify (remove type guards):**
- `weeksRemaining(): Int` — deadline is always present, returns non-nullable
- `totalWeeks(): Int` — same simplification

**Add:**
- `isDeadlineWeek(): Boolean` — returns true if `deadline` falls within the current Monday-to-Sunday week

### 3.4 Enums Unchanged

- `GoalStatus`: ACTIVE, PAUSED, COMPLETED, ARCHIVED, UPCOMING — no changes
- `PausedBy`: USER, VACATION — no changes

---

## 4. XP & Gamification

### 4.1 XpManager.kt

**Delete:** `awardMilestoneBonus()` — milestones no longer exist

**Keep unchanged:**
- `awardTaskXp()` — `difficulty * 10` + deadline bonus per task completion
- `awardEpicWeeklyBonus()` — `difficulty * 30` per ritual completion
- `awardGoalCompletionBonus()` — `difficulty * 50` on goal completion
- `applyXpGain()` — level progression (`n * 100` XP per level)

### 4.2 XP Flow

| Event | XP | When |
|-------|-----|------|
| Daily mission completed | `difficulty * 10` + bonus | Each task completed |
| Epic weekly bonus | `difficulty * 30` | Weekly ritual completed |
| Goal completion bonus | `difficulty * 50` | Epic final mission completed or manual completion |

Daily streak (`UserProfile.streakDays`) remains untouched — tracked in `awardTaskXp()`.

---

## 5. AI Coach Service

### 5.1 buildGoalTypeContext() — Rewrite

Delete the `INFINITE` branch entirely (current lines 166-179). The finite branch becomes the only path, with two modes:

**Normal mode** (`isDeadlineWeek = false`):
- Same as current finite prompt: weeks remaining, progress %, intensity, extension count
- Instructs AI to generate 7 daily missions (dayIndex 1-7)

**Deadline week mode** (`isDeadlineWeek = true`):
- New prompt section instructing AI to generate 4-5 daily closure missions + 1 Epic final mission
- Epic mission must be the highest `dayIndex` and have `difficulty = goal.difficulty`

Deadline week prompt addition:

```
Esta es la ULTIMA SEMANA del proyecto "{title}". El deadline es el {fecha}.
Progreso actual: {progress}%. Intensidad: {intensity}.

Genera entre 4 y 5 misiones diarias enfocadas en cerrar lo pendiente,
mas 1 MISION EPICA FINAL que represente la culminacion del proyecto.
La mision epica debe ser el ultimo dayIndex y tener difficulty = {goal.difficulty}.
Total de misiones esta semana: entre 5 y 6 (en vez de las 7 habituales).
```

### 5.2 calculateNewIntensity() — Simplification

- Remove the `GoalType.INFINITE` branch that capped intensity at `difficulty * 1.5`
- All goals use: cap `difficulty * 2.0f`
- Acceleration in final 4 weeks: growth multiplier = `1.0 + (4 - weeksRemaining) / 4.0`
- Feedback modifiers unchanged: SOBRADO +5%, AGOTADO -10%

### 5.3 generateWeeklyTasks() — Minor Adjustment

- New parameter: `isDeadlineWeek: Boolean`
- Passed through to `buildGoalTypeContext()` to switch prompt mode
- Response parsing unchanged — still a `List<AiTaskResponse>`
- No `isEpic` field needed in the model — Epic detection is positional: in a deadline week, the task with the highest `dayIndex` is the Epic

---

## 6. UI Changes

### 6.1 CreateGoalCard.kt / CreateGoalViewModel.kt

- Remove the "Proyecto con fecha limite" / "Habito de por vida" toggle entirely
- DatePicker is always visible and mandatory — no toggle needed
- `deadlineEpochMillis` becomes non-nullable in UiState
- Create button disabled until both title and deadline are set
- Default deadline suggestion: 30 days from today (existing behavior)

### 6.2 TargetsScreen.kt

- Remove differentiated cards for infinite goals (Level + Streak + Milestone display)
- All goal cards show unified layout: progress bar, weeks remaining, intensity
- Remove import and usage of `streakDisplay`
- Sort by "Progress" applies to all goals uniformly

### 6.3 GoalDetailScreen.kt

- Remove `if (goal.isInfinite)` branch from GoalHeaderCard entirely
- All goals show: progress bar, weeks remaining, intensity, mission list
- Pause/resume toggle unchanged
- Add visual `COMPLETED` badge when `goal.status == GoalStatus.COMPLETED`
- Add "Completar goal" button for manual completion (safety net)

### 6.4 ProgressScreen.kt

- Remove Milestones section entirely (the one that says "Maintain weekly Streaks to unlock Milestones")
- Keep Chart 5 (Level / XP / Streak) — "Streak" here refers to daily streak (`streakDays`), unaffected

### 6.5 HomeScreen.kt

No changes — only displays daily streak (`streakDays`).

### 6.6 ProfileScreen.kt

No changes — only displays `streakDays`.

### 6.7 SocialScreen.kt

- Replace "Track habits and challenge streaks together" with relevant copy for finite goals (e.g., "Track goals and challenge progress together")
- Keep day streak display in user profiles

---

## 7. Ritual Reconversion

### 7.1 RitualViewModel.kt — New Flow

**Current flow (to delete):**
1. Increment `weeklyStreak`
2. Check milestones reached
3. Show streak in weeks
4. Award milestone bonus if applicable

**New flow:**
1. Show week summary: missions completed vs total for that goal
2. Show goal progress: `progress %`, weeks remaining to deadline
3. Collect user feedback (SOBRADO / BIEN / AGOTADO) — existing mechanic
4. Award `awardEpicWeeklyBonus()` (`difficulty * 30`)
5. Trigger mission generation for next week
6. If next week is deadline week, pass `isDeadlineWeek = true` to `generateWeeklyTasks()`

**Delete from RitualViewModel:**
- `newWeeklyStreak` calculation logic
- `computeVisibleSteps()` streak and milestone calculation
- `awardMilestoneBonus()` call
- Goal update of `weeklyStreak` and `streakStartDate` fields

### 7.2 RitualUiState.kt

**Delete:**
- `newWeeklyStreak: Int`

**Add:**
- `weekMissionsCompleted: Int` — missions completed this week
- `weekMissionsTotal: Int` — total missions this week
- `goalProgress: Int` — current goal progress percentage
- `weeksRemaining: Int` — weeks until deadline

### 7.3 RitualScreen.kt

- Remove: "X semanas consecutivas", "Racha: X semanas" displays
- Replace with: progress bar for goal, "X% completado", "Y semanas restantes"
- Keep: feedback collection step, weekly summary step, intensity change animation

### 7.4 Deadline Week in Ritual

- If the ritual detects that the NEXT generation will be for a deadline week: passes `isDeadlineWeek = true` to `AICoachService.generateWeeklyTasks()`
- If the current week WAS the deadline week and the user completed the Epic mission: show celebration screen + award `awardGoalCompletionBonus()`

---

## 8. Goal Completion Logic

### 8.1 Automatic Completion (Epic Mission)

1. User completes the task with the highest `dayIndex` during a deadline week
2. Client-side detection in task completion flow identifies this as the Epic task of a deadline-week goal
3. Updates `goal.status = COMPLETED`, `goal.progress = 100`
4. Awards `awardGoalCompletionBonus()` (`difficulty * 50`)
5. Shows celebration UI

### 8.2 Manual Completion (Safety Net)

- User can mark any goal as completed from `GoalDetailScreen` at any time via "Completar goal" button
- Triggers same result: status COMPLETED, progress 100, XP bonus awarded
- Useful for: early completion, Epic mission generation failure, user preference

### 8.3 Post-Completion Behavior

- Goal shows `COMPLETED` badge in TargetsScreen and GoalDetailScreen
- AI ignores COMPLETED goals — no more missions generated
- Pending missions for the completed goal are cancelled (status set to reflect this)
- User can move the goal to `ARCHIVED` to remove from main view

### 8.4 Deadline Passed Without Completion

- Goal does NOT auto-complete — stays ACTIVE
- Next AI generation detects deadline has passed, generates "late closure" missions
- User options: complete manually, extend deadline (`extensionCount + 1`), or archive

---

## 9. Files Affected

### Delete entirely:
- `GoalType.kt`
- `MilestoneRecord.kt`
- `MilestoneRepository.kt`

### Major changes:
| File | Changes |
|------|---------|
| `Goal.kt` | Remove `weeklyStreak`, `streakStartDate`, `goalType`; make `deadline` non-nullable |
| `GoalExtensions.kt` | Remove 6 functions, add `isDeadlineWeek()`, simplify `weeksRemaining()` and `totalWeeks()` |
| `XpManager.kt` | Remove `awardMilestoneBonus()` |
| `AICoachService.kt` | Rewrite `buildGoalTypeContext()`, simplify `calculateNewIntensity()`, add `isDeadlineWeek` param to `generateWeeklyTasks()` |
| `CreateGoalCard.kt` | Remove finite/infinite toggle, make DatePicker always visible and mandatory |
| `CreateGoalViewModel.kt` | Make `deadlineEpochMillis` non-nullable in UiState |
| `TargetsScreen.kt` | Remove infinite goal cards, unify to single display layout |
| `GoalDetailScreen.kt` | Remove infinite branch, add COMPLETED badge, add manual completion button |
| `ProgressScreen.kt` | Remove Milestones section |
| `RitualViewModel.kt` | Rewrite to finite progress flow, remove streak/milestone logic |
| `RitualUiState.kt` | Replace `newWeeklyStreak` with progress state fields |
| `RitualScreen.kt` | Replace streak UI with progress bar and weeks remaining |

### Minor changes:
| File | Changes |
|------|---------|
| `SocialScreen.kt` | Update copy text |
| `TaskRepository.kt` | Add Epic mission completion detection in `toggleTaskCompletion()` |

### No changes needed:
- `HomeScreen.kt`, `ProfileScreen.kt`, `UserProfile.kt`
- `SessionRepository.kt`, `CreditManager.kt`
- `TaskDeadlineResolver.kt`, `NotificationHelper.kt`
- `GoalRepository.kt` (vacation mode already correct)
