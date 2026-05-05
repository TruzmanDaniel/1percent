# XP System Design

## Overview

Complete XP economy for 1percent: formula-based XP from tasks and goal missions, early completion bonuses, weekly epic mission bonuses, and goal completion rewards. All XP logic centralized in `XpManager`.

## Formulas

| Event | Formula | Range |
|-------|---------|-------|
| Task/Mission completed | `difficulty * 10` | 10-50 XP |
| Early completion bonus | `+20%` of base (if `completedAt < deadline`) | 2-10 XP |
| Epic Mission weekly bonus | `goal.difficulty * 30` (if `epicMissionPassed == true`) | 30-150 XP |
| Goal completed (with deadline) | `goal.difficulty * 50` (if `progress == 100`) | 50-250 XP |
| Level up requirement | `level * 100` XP to advance | 100, 200, 300... |

**Goal completion bonus only applies to Goals with a deadline.** Goals without deadline are perpetual and only yield weekly epic bonuses.

## Data Model Changes

### Task — add `xpAwarded: Int? = null`

- Written when the task is completed, with the actual XP granted (base + bonus if applicable).
- `null` means not yet awarded.
- `task.xp` remains as the base value (`difficulty * 10`), used for display before completion.
- `revokeTaskXp` reads `xpAwarded` to subtract the exact amount, then resets it to `null`.

### No changes to `UserProfile`, `Goal`, or `WeeklySummary`

Existing fields (`difficulty`, `deadline`, `epicMissionPassed`, `progress`, `level`, `currentXp`, `xpToNextLevel`) are sufficient.

## XpManager Methods

All XP logic lives in `XpManager` (centralized, Approach A).

### `awardTaskXp(userId: String, task: Task): Result<Unit>`

1. Get current user profile.
2. Calculate `base = task.difficulty * 10`.
3. If task has a deadline and `completedAt < deadline`: `bonus = floor(base * 0.2)`.
4. `total = base + bonus`.
5. Write `xpAwarded = total` to the Task document in Firestore.
6. Call `applyXpGain(profile, total)` and update streak/activity as current `awardXp` does.
7. Update user profile in Firestore.

### `revokeTaskXp(userId: String, task: Task): Result<Unit>`

1. Read `task.xpAwarded` (the exact amount previously granted).
2. If `xpAwarded == null`, no-op (nothing was awarded).
3. Subtract `xpAwarded` from `profile.currentXp` (clamped to 0).
4. Decrement `totalTasksCompleted`.
5. Reset `xpAwarded = null` on the Task document.
6. Update user profile in Firestore.

**Note:** Level is not decremented on revoke — only `currentXp` decreases. This avoids confusing level-down UX.

### `awardEpicWeeklyBonus(userId: String, goal: Goal): Result<Unit>`

1. Calculate `bonus = goal.difficulty * 30`.
2. Call `applyXpGain(profile, bonus)`.
3. Update user profile in Firestore.

### `awardGoalCompletionBonus(userId: String, goal: Goal): Result<Unit>`

1. Guard: if `goal.deadline == null`, return early (no bonus for perpetual goals).
2. Calculate `bonus = goal.difficulty * 50`.
3. Call `applyXpGain(profile, bonus)`.
4. Update user profile in Firestore.

### `applyXpGain` (existing, unchanged)

Level n requires `n * 100` XP to advance. Carries over excess XP across multiple level-ups.

## Integration Points

### 1. Task completion toggle — `HomeViewModel.onTaskChecked()`

- Replace `XpManager.awardXp(userId, task)` with `XpManager.awardTaskXp(userId, task)`.
- Replace `XpManager.revokeXp(userId, task)` with `XpManager.revokeTaskXp(userId, task)`.

### 2. Manual task creation — `CreateTaskViewModel`

- Use the user-selected difficulty from the form instead of hardcoded `difficulty = 1`.
- Calculate `xp = difficulty * 10` instead of hardcoded `xp = 10`.

### 3. AI-generated missions — `AICoachService`

- Remove `xp` from the AI prompt JSON schema. The AI only returns `title`, `description`, `difficulty`, `dayIndex`.
- When parsing the AI response, calculate `xp = difficulty * 10` for each mission.

### 4. Weekly evaluation — `HomeViewModel` (WeeklySummary creation)

- After saving the `WeeklySummary`, if `epicMissionPassed == true`:
  - Call `XpManager.awardEpicWeeklyBonus(userId, goal)`.

### 5. Goal completion trigger

- Where `goal.progress` reaches 100 and `goal.deadline != null`:
  - Call `XpManager.awardGoalCompletionBonus(userId, goal)`.
- Locate the exact code path that updates `progress` to place this trigger.

### 6. Progress chart — `ProgressViewModel.recomputeTaskStats()`

- Change `task.xp` to `task.xpAwarded ?: task.xp` in the 30-day XP curve calculation, so the chart reflects actual XP granted (including early bonuses).
- Weekly epic bonuses and goal completion bonuses are not reflected in the task-based chart.

## Progression Examples

### Single task (difficulty 3, completed before deadline)

- Base: 3 * 10 = 30 XP
- Early bonus: floor(30 * 0.2) = 6 XP
- Total: 36 XP

### Goal (difficulty 3, 4 weeks, with deadline)

Typical week (7 missions, epic completed, some early):
- 6 normal missions (~dif 2-3): ~150 XP
- 1 epic mission (~dif 3): ~30 XP
- Early bonuses on ~3 missions: ~15 XP
- Epic weekly bonus: 3 * 30 = 90 XP
- **Week total: ~285 XP**

4 weeks + goal completion:
- 4 * ~285 = ~1140 XP
- Goal completion bonus: 3 * 50 = 150 XP
- **Goal total: ~1290 XP** (~4-5 level-ups from level 1)

### Progression pace

- Casual user (1-2 tasks/day, no Goals): ~20-40 XP/day, level up every 3-5 days
- Active user (tasks + 1 Goal): ~60-100 XP/day, level up every 1-3 days
- Early levels feel fast; level 10+ (1000+ XP to advance) creates long-term progression
