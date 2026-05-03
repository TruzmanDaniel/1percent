# Refactoring & Feature Design: Dynamic Profiles, Sharing, and Filtering

**Date:** 2026-05-03
**Status:** Approved
**Approach:** Incremental by feature (A)

---

## 1. Dynamic ProfileScreen

### Problem
`ProfileTopBar` in `NavGraph.kt` always displays `currentUser?.name` (the authenticated user), even when viewing another user's profile.

### Solution
Move `ProfileTopBar` out of `NavGraph.kt` and into `ProfileScreen.kt`, where it has access to `uiState.user?.name` — the profile currently being viewed.

### Files Changed
- **NavGraph.kt** — Remove the `ProfileTopBar` rendering block for the ProfileScreen route. ProfileScreen will manage its own top bar.
- **ProfileScreen.kt** — Add `ProfileTopBar` inside the composable, using `uiState.user?.name` for the username parameter.

### Behavior
- Own profile: displays the authenticated user's name (same as before).
- Friend's profile: displays the friend's name from the loaded profile.

---

## 2. Remove "Postpone" from All List Views

### Problem
"Postpone" is available as a direct action in task list rows (TargetsScreen, GoalDetailScreen). This should be centralized inside the "Edit" flow only.

### Changes

#### TargetsScreen.kt
- Remove the "Postpone" chip (Schedule icon) from `TaskRowWithActions`.
- Remove the `DatePickerDialog` triggered by postpone (~lines 137-153).
- Remove `onTaskPostpone` callback from the composable signature.

#### TargetsViewModel.kt
- Remove `onTaskPostpone()` as a direct list action.
- Remove `onDatePickerResult()` and `onDatePickerDismissed()` as list-level actions.
- Date editing remains available through the `editingTask` flow (Edit screen).

#### GoalDetailScreen.kt
- Remove `onTaskPostpone` from `TaskRowWithActions` in the missions list.
- Remove the associated `DatePickerDialog` (~lines 126-148).

#### GoalDetailViewModel.kt
- Remove `onTaskPostpone()`, `onDatePickerResult()`, `onDatePickerDismissed()`.
- Date changes only through the edit flow.

#### HomeScreen
- No changes (already does not have postpone).

### Note
The date-change functionality is preserved within the "Edit task" flow, which already exists via `editingTask` state in TargetsViewModel.

---

## 3. Sharing: Rules, Propagation, and UI

### 3a. Business Rules

| Entity | Individually Shareable? | Mechanism |
|--------|------------------------|-----------|
| Task (goalId == null) | Yes | `TaskRepository.shareTask()` |
| Goal | Yes | `GoalRepository.shareGoal()` + propagation |
| Mission (task with goalId != null) | No | Shared automatically when parent Goal is shared |

### 3b. Goal Share Propagation

**GoalRepository.shareGoal()** currently only updates the Goal document. Must also propagate to child tasks:

```
shareGoal(goalId, friendUserId):
  1. Update goal.sharedWith with arrayUnion(friendUserId)
  2. Query tasks where goalId == goalId
  3. Firestore WriteBatch: for each child task, arrayUnion(friendUserId) to sharedWith
```

Use a Firestore WriteBatch for atomicity.

### 3c. Hide Share on Missions

- **TargetsScreen (TaskRowWithActions):** Share chip visible only if `task.goalId == null`.
- **GoalDetailScreen:** Missions do not show individual share button (already the case).
- **HomeScreen:** If a task has `goalId != null`, hide the share action in its row.

### 3d. Share Button on GoalDetailScreen

Add a "Share" button/icon in the Goal header section of GoalDetailScreen.

**GoalDetailViewModel additions:**
- State: `showShareSheet: Boolean`, `friends: List<UserProfile>`, `shareTargetGoal: Goal?`
- Actions: `onShareGoalRequested()`, `onShareWithFriend(friendUserId, friendName)`, `onShareDismissed()`
- Load friends via `SocialRepository.observeFriends()`.

Opens the existing `ShareBottomSheet` component.

### 3e. Collaborator Avatars in Lists

For tasks and goals with non-empty `sharedWith`:
- Show stacked circular avatars (24dp) below the title in each row.
- Max 3 visible + "+N" indicator if more.
- Resolve userIds using the friends list from `SocialRepository` (already loaded in ViewModels).

### 3f. Collaborator Indicator in Detail Views

In GoalDetailScreen header and Task detail modal:
- Expandable/dropdown section showing the full list of shared users (avatar + name).
- Hidden when `sharedWith` is empty.

---

## 4. Filtering

### 4a. HomeScreen — Completed Checkbox Bugfix

**Problem:** `HomeViewModel.applyFiltersAndSort()` hardcodes `tasks.filter { it.status == TaskStatus.PENDING }`, preventing completed tasks from ever reaching the UI.

**Solution:**
- Add `statusFilter: HomeStatusFilter` (ALL, PENDING, COMPLETED) to `HomeFilters`.
- Replace the hardcoded PENDING filter with status-aware logic:
  - `PENDING` (default): show only pending tasks.
  - `COMPLETED`: show only completed tasks.
  - `ALL`: show all tasks.
- The checkbox in task rows already uses `task.status == TaskStatus.COMPLETED` — once completed tasks reach the UI, checkboxes display correctly.

**Files changed:** `HomeViewModel.kt`, `HomeUiState.kt` (add `HomeStatusFilter` enum and field to `HomeFilters`).

### 4b. TargetsScreen — New Filters

**Existing and working (no changes needed):**
- Missions toggle (`task.goalId != null`) — already functional.
- Sort by Date ascending (`TaskSort.DEADLINE_ASC`) — already functional.

**Fix needed — Shared tasks filter:**
- Current `TaskQuickFilter.SHARED` always returns `true` (no-op).
- New behavior as a toggle with two modes:
  - **Filter OFF (default):** Show all tasks (own + shared with me). No ownership filtering.
  - **Filter ON ("Shared with me"):** Show only tasks where `task.ownerId != currentUserId` (tasks others shared with me).
- `currentUserId` available via `SessionRepository.currentUser`.

**Files changed:** `TargetsViewModel.kt` (update `SHARED` filter logic).

---

## Commit Protocol

All commits follow these rules:
- Professional, technical commit messages (e.g., `feat: implement real-time shared tasks and profile bar fix`).
- No references to AI tooling in commit messages, code comments, or metadata.
- Detailed checklist presented before any `git add` or `git commit`.
- No commit proceeds without explicit user confirmation.

---

## Implementation Order

1. ProfileScreen dynamic top bar (isolated, no dependencies)
2. Remove Postpone from list views (UI-only, no new logic)
3. Sharing: propagation + UI (repo changes + new UI components)
4. Filtering: HomeScreen bugfix + TargetsScreen shared filter (depends on sharing being correct)
