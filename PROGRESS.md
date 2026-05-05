# Progress Dashboard — Implementation Log

## Step 1 — Foundation (2026-05-05)

### What changed
| File | Change |
|---|---|
| `gradle/libs.versions.toml` | Added Vico `2.0.0` version + `vico-compose-m3` library entry |
| `app/build.gradle.kts` | Added `libs.vico.compose.m3` dependency |
| `data/model/Task.kt` | Added `completedAt: Long? = null` field |
| `data/TaskRespository.kt` | `toggleTaskCompletion` now writes `completedAt` (epoch ms) when a user marks a task done, and deletes the field when un-completing. `updateTaskStatus` does the same for the secondary completion path. |

### New tracking variable
- **`Task.completedAt: Long?`** — epoch milliseconds when the task was checked off. `null` for tasks not yet completed or created before this change. Stored in Firestore under the `tasks` collection. Used by Progress charts to group completions by day/week.

### Build status
`./gradlew compileDebugKotlin` — **SUCCESSFUL** (2 pre-existing warnings, 0 errors)

---

---

## Step 2 — Data Layer (2026-05-05)

### What changed
| File | Change |
|---|---|
| `data/WeeklySummaryRepository.kt` | Added `getSummaries(goalId, limit)` — fetches last N weekly summaries ordered by weekNumber DESC |
| `ui/screens/progress/ProgressUiState.kt` | Full rewrite: replaced mock string fields with real data fields + 3 supporting data classes (`GoalProgressItem`, `CategorySlice`, `GoalSparkline`) |
| `ui/screens/progress/ProgressViewModel.kt` | Full rewrite: observes `SessionRepository.currentUser`, `TaskRespository.observeTasks`, `GoalRepository.observeGoals`; computes XP curve, weekly rhythm, category breakdown, goal progress, and sparklines |
| `ui/screens/progress/ProgressScreen.kt` | Replaced mock content with clean skeleton — 6 empty card slots + loading spinner |

### Computed stats (in ProgressViewModel)
- **XP curve**: 30-day daily XP buckets from `task.completedAt` → cumulative sum
- **Weekly rhythm**: tasks grouped by day-of-week for current week and previous week
- **Category breakdown**: completed tasks counted per `Category` enum, fraction pre-computed
- **Goal progress**: active goals mapped to `GoalProgressItem(title, progress 0-1, category)`
- **Sparklines**: per active goal, last 6 `WeeklySummary` records → `tasksCompleted/totalTasks` rate

### Build status
`./gradlew compileDebugKotlin` — **SUCCESSFUL**

---

## Step 3 — Chart 5: Level/XP/Streak stat card (2026-05-05)

### What changed
| File | Change |
|---|---|
| `ui/screens/progress/ProgressScreen.kt` | Added `LevelXpStatCard` composable: level badge (indigo box), animated `LinearProgressIndicator` for XP, amber streak counter, emerald tasks-completed counter. Added `StatItem` helper. |

### Build status
`./gradlew compileDebugKotlin` — **SUCCESSFUL**

---

## Step 4 — Chart 2: Goals Progress bars (2026-05-05)

### What changed
| File | Change |
|---|---|
| `ui/screens/progress/ProgressScreen.kt` | Added `GoalsProgressCard` and `GoalProgressRow` composables: one horizontal progress bar per active goal, showing title, category label, fill percentage, and `LinearProgressIndicator`. Empty state when no active goals. |

### Build status
`./gradlew compileDebugKotlin` — **SUCCESSFUL**

---

## Step 5 — Chart 4: Category Breakdown donut (2026-05-05)

### What changed
| File | Change |
|---|---|
| `ui/screens/progress/ProgressScreen.kt` | Added `CategoryDonutCard`, `DonutChart` (pure `Canvas` with `drawArc`), and `LegendItem`. 7-color palette (`donutColors`) defined as top-level val. Each arc drawn with 26dp stroke and 2° gap. Center shows total task count. |

### Build status
`./gradlew compileDebugKotlin` — **SUCCESSFUL**

---

## Step 6 — Chart 1: XP Accumulation Curve (2026-05-05)

### What changed
| File | Change |
|---|---|
| `ui/screens/progress/ProgressScreen.kt` | Added `XpCurveCard` using Vico 2.0 `CartesianChartHost` + `rememberLineCartesianLayer`. Confirmed correct Vico 2.0 API: `CartesianChartModelProducer` + `runTransaction { lineSeries { series(points) } }`. Uses `modelProducer` parameter (not `model`). Empty state when all XP points are zero. |

### Vico 2.0 API notes (for future charts)
- Model class lives in `com.patrykandpatrick.vico.core.cartesian.data` (not `.model`)
- Static model: `CartesianChartModelProducer` + `LaunchedEffect { runTransaction { lineSeries/columnSeries { series(...) } } }`
- `CartesianChartHost` uses `modelProducer =` parameter
- `series(collection)` accepts `Collection<Number>` inside the DSL block
- `lineSeries {}` is a `Transaction` extension from `com.patrykandpatrick.vico.core.cartesian.data.lineSeries`

### Build status
`./gradlew compileDebugKotlin` — **SUCCESSFUL**

---

## Step 7 — Chart 3: Weekly Rhythm grouped bar chart (2026-05-05)

### What changed
| File | Change |
|---|---|
| `ui/screens/progress/ProgressScreen.kt` | Added `WeeklyRhythmCard`: Vico `rememberColumnCartesianLayer` with two `columnSeries` (this week + last week, Mon–Sun). Legend row reuses `LegendItem`. Empty state when both weeks are all zeros. |

### Build status
`./gradlew compileDebugKotlin` — **SUCCESSFUL**

---

## Step 8 — Chart 6: Goal Weekly Sparklines (2026-05-05)

### What changed
| File | Change |
|---|---|
| `ui/screens/progress/ProgressScreen.kt` | Added `GoalSparklinesCard` and `GoalSparklineRow`: one compact Vico line chart (80dp tall) per active goal with weekly summary data. Each sparkline has its own `CartesianChartModelProducer` keyed by `goalTitle`. Empty state when no weekly data exists. |

### Build status
`./gradlew compileDebugKotlin` — **SUCCESSFUL**

---

## Implementation complete (2026-05-05)

All 6 charts implemented. Zero mock data remaining in the Progress Screen.

| Chart | Type | Library | Data source |
|---|---|---|---|
| Level & XP | Stat card + progress bar | Pure Compose | `UserProfile.level/currentXp/xpToNextLevel/streakDays` |
| Goals Progress | Horizontal bars | Pure Compose | `Goal.progress` (0–100) |
| 1% Curve | Area line chart | Vico | `Task.completedAt` → 30-day cumulative XP |
| Weekly Rhythm | Grouped column chart | Vico | `Task.completedAt` → Mon–Sun counts, this/last week |
| Category Breakdown | Donut + legend | Pure Compose Canvas | `Task.category` grouped by count |
| Goal Weekly Trend | Sparkline per goal | Vico | `WeeklySummary.tasksCompleted/totalTasks` |

---

## Step 9 — Polish: M3 Theme + Column Colors + Day Labels (2026-05-05)

### What changed
| File | Change |
|---|---|
| `MainActivity.kt` | Wrapped `Surface` + `NavGraph` in `ProvideVicoTheme(rememberM3VicoTheme())`. All Vico charts app-wide now read `MaterialTheme.colorScheme` — indigo primary for series 0, emerald secondary for series 1. |
| `ui/screens/progress/ProgressScreen.kt` | `WeeklyRhythmCard`: added `CartesianValueFormatter` mapping index 0–6 to Mon–Sun, passed to `HorizontalAxis.rememberBottom(valueFormatter = ...)` on the column chart. Chart height increased to 180dp to accommodate the axis label row. |

### Build status
`./gradlew compileDebugKotlin` — **SUCCESSFUL**

### Open TODOs
None — all originally listed items resolved.

---

## Step 10 — XP Infrastructure (2026-05-05)

### Problem
`UserProfile.currentXp`, `level`, `totalTasksCompleted`, and `streakDays` were never updated anywhere in the codebase. Completing a task only toggled `completedBy` — no XP was awarded.

### What changed
| File | Change |
|---|---|
| `data/model/UserProfile.kt` | Added `lastActivityDate: Long? = null` — epoch ms of midnight on the last day a task was completed, used for streak calculation |
| `data/XpManager.kt` | New singleton. `awardXp(userId, task)` runs a Firestore transaction: adds XP, increments `totalTasksCompleted`, level-up loop (`xpToNext = level × 100`), streak logic (consecutive day → `streak++`, same day → no change, gap → reset to 1). `revokeXp` reverses on un-completion. Both push the updated profile back into `SessionRepository` so the UI reacts immediately. |
| `ui/screens/home/HomeViewModel.kt` | `onTaskChecked` now reads the task's `completedBy` state *before* toggling, then calls `XpManager.awardXp` or `revokeXp` accordingly. |

### Level progression formula
`xpToNextLevel = currentLevel × 100`  
Level 1 → 100 XP, Level 2 → 200 XP, Level 3 → 300 XP, …

### Build status
`./gradlew compileDebugKotlin` — **SUCCESSFUL**

---

## Step 11 — XP Infrastructure Fixes (2026-05-05)

### Bugs fixed

| # | File | Bug | Fix |
|---|---|---|---|
| 1 | `data/XpManager.kt` | Transaction + second Firestore read + `updateUserProfile` chain meant any failure in the second write left `_currentUser` at 0 XP silently | Rewrote to use `SessionRepository.currentUser.value` (in-memory profile), compute locally, single `updateUserProfile` call — one write, guaranteed `_currentUser` update |
| 2 | `ui/screens/progress/ProgressViewModel.kt` | `startObservingData` was called on every `currentUser` emission (including every XP award), cancelling and restarting all Firestore observations in a loop | Added `observedUserId` tracker — `startObservingData` now only fires when the logged-in user *changes* (login/logout), not on profile updates |
| 3 | `ui/screens/progress/ProgressViewModel.kt` | XP curve filter used `it.completedAt >= thirtyDaysAgo` on `Long?` — Kotlin smart-cast on member properties inside lambdas is unreliable | Rewrote with explicit `val ca = task.completedAt; if (ca != null && ca >= ...)` |

### Build status
`./gradlew compileDebugKotlin` — **SUCCESSFUL**
