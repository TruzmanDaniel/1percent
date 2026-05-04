# Refactoring: Dynamic Profiles, Sharing & Filtering — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix ProfileTopBar to show the viewed user's name, remove Postpone from list views, add Goal sharing with mission propagation and collaborator UI, and fix Home/Targets filtering.

**Architecture:** Incremental feature-by-feature approach. Each task produces a self-contained commit verified by the user before proceeding. Changes touch ViewModels, Composables, and one Repository — no new modules or dependencies.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), Firebase Firestore, StateFlow/ViewModel MVVM

**Commit Protocol:** No AI references in commit messages, code comments, or metadata. Checklist presented before every commit. User must confirm before any `git add`/`git commit`.

---

## File Map

| File | Action | Task(s) |
|------|--------|---------|
| `app/src/main/java/es/uc3m/android/a1percent/navigation/NavGraph.kt` | Modify | 1 |
| `app/src/main/java/es/uc3m/android/a1percent/ui/screens/profile/ProfileScreen.kt` | Modify | 1 |
| `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsScreen.kt` | Modify | 2, 5, 6 |
| `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsViewModel.kt` | Modify | 2, 8 |
| `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsUiState.kt` | Modify | 2 |
| `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/GoalDetailScreen.kt` | Modify | 2, 4 |
| `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/GoalDetailViewModel.kt` | Modify | 2, 4 |
| `app/src/main/java/es/uc3m/android/a1percent/data/GoalRepository.kt` | Modify | 3 |
| `app/src/main/java/es/uc3m/android/a1percent/ui/components/CollaboratorAvatars.kt` | Create | 6 |
| `app/src/main/java/es/uc3m/android/a1percent/ui/screens/home/HomeFilters.kt` | Modify | 7 |
| `app/src/main/java/es/uc3m/android/a1percent/ui/screens/home/HomeViewModel.kt` | Modify | 7 |
| `app/src/main/java/es/uc3m/android/a1percent/ui/screens/home/HomeUiState.kt` | No change | — |
| `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsFilters.kt` | Modify | 8 |

---

## Task 1: Dynamic ProfileTopBar

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/navigation/NavGraph.kt:104-117`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/profile/ProfileScreen.kt:30-44`

- [ ] **Step 1: Remove ProfileTopBar from NavGraph.kt**

In `NavGraph.kt`, replace the `ProfileTopBar` block (lines 105-109) so the Scaffold no longer renders a topBar for ProfileScreen. ProfileScreen will manage its own top bar.

Replace lines 104-117:
```kotlin
            topBar = {
                if (currentBaseRoute == AppScreens.ProfileScreen.route) {
                    ProfileTopBar(
                        username = currentUser?.name ?: "Profile",
                        onBack = { navController.popBackStack() }
                    )
                } else if (currentBaseRoute in topLevelRoutes && !isSubScreen) {
                    DefaultTopBar(
                        title = currentScreenTitle,
                        onProfileClick = {
                            navController.navigate(AppScreens.ProfileScreen.route + "/placeholder")
                        }
                    )
                }
            },
```

With:
```kotlin
            topBar = {
                if (currentBaseRoute in topLevelRoutes && !isSubScreen) {
                    DefaultTopBar(
                        title = currentScreenTitle,
                        onProfileClick = {
                            navController.navigate(AppScreens.ProfileScreen.route + "/placeholder")
                        }
                    )
                }
            },
```

Remove the now-unused import `es.uc3m.android.a1percent.ui.screens.profile.ProfileTopBar` from line 47.

- [ ] **Step 2: Add ProfileTopBar inside ProfileScreen.kt**

In `ProfileScreen.kt`, wrap the existing `ProfileBodyContent` with a `Scaffold` that includes the `ProfileTopBar` using the viewed profile's name.

Replace the `ProfileScreen` composable (lines 30-44):
```kotlin
@Composable
fun ProfileScreen(navController: NavController, text: String?, viewModel: ProfileViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(text) {
        viewModel.loadUser(text)
    }

    ProfileBodyContent(
        navController = navController,
        uiState = uiState,
        onPickImage = { uri -> viewModel.uploadProfilePicture(uri) },
        onFriendAction = { viewModel.onFriendAction(uiState.user?.id ?: return@ProfileBodyContent) }
    )
}
```

With:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, text: String?, viewModel: ProfileViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(text) {
        viewModel.loadUser(text)
    }

    Scaffold(
        topBar = {
            ProfileTopBar(
                username = uiState.user?.name ?: "Profile",
                onBack = { navController.popBackStack() }
            )
        }
    ) { innerPadding ->
        ProfileBodyContent(
            navController = navController,
            uiState = uiState,
            onPickImage = { uri -> viewModel.uploadProfilePicture(uri) },
            onFriendAction = { viewModel.onFriendAction(uiState.user?.id ?: return@ProfileBodyContent) },
            modifier = innerPadding
        )
    }
}
```

Add these imports to `ProfileScreen.kt`:
```kotlin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
```

- [ ] **Step 3: Update ProfileBodyContent to accept padding**

In `ProfileScreen.kt`, update `ProfileBodyContent` to accept a `modifier` parameter for inner padding.

Change the signature (line 47):
```kotlin
@Composable
fun ProfileBodyContent(
    navController: NavController,
    uiState: ProfileUiState,
    onPickImage: (android.net.Uri) -> Unit = {},
    onFriendAction: () -> Unit = {}
) {
```

To:
```kotlin
@Composable
fun ProfileBodyContent(
    navController: NavController,
    uiState: ProfileUiState,
    onPickImage: (android.net.Uri) -> Unit = {},
    onFriendAction: () -> Unit = {},
    modifier: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp)
) {
```

And update the root Column (line 72) to apply the padding:
```kotlin
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
```

- [ ] **Step 4: Build and verify**

Run:
```bash
cd C:/1percent && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL. The ProfileTopBar now shows the viewed user's name.

- [ ] **Step 5: Present checklist and commit after user confirmation**

Present checklist to user. After confirmation:
```bash
git add app/src/main/java/es/uc3m/android/a1percent/navigation/NavGraph.kt app/src/main/java/es/uc3m/android/a1percent/ui/screens/profile/ProfileScreen.kt
git commit -m "feat: make ProfileTopBar display viewed profile name instead of current user"
```

---

## Task 2: Remove Postpone from All List Views

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsScreen.kt:136-153, 206-207, 269, 326, 381-389, 491-496`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsViewModel.kt:183-198`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsUiState.kt:31`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/GoalDetailScreen.kt:91, 126-148`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/GoalDetailViewModel.kt:93-106`

- [ ] **Step 1: Remove Postpone chip from TaskRowWithActions**

In `TargetsScreen.kt`, remove the Postpone AssistChip from `TaskRowWithActions` (lines 492-496):

Remove:
```kotlin
                AssistChip(
                    onClick = onTaskPostpone,
                    label = { Icon(Icons.Default.Schedule, contentDescription = "Postpone", modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
```

Remove the `onTaskPostpone: () -> Unit,` parameter from the `TaskRowWithActions` signature (line 386).

Remove the `Icons.Default.Schedule` import if it becomes unused (line 32).

- [ ] **Step 2: Remove DatePickerDialog from TargetsScreen**

In `TargetsScreen.kt`, remove the entire DatePicker dialog block (lines 136-153):

Remove:
```kotlin
        // DatePicker dialog for postpone
        val showDatePicker = uiState.showDatePickerForTask
        if (showDatePicker != null) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { viewModel.onDatePickerDismissed() },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.onDatePickerResult(showDatePicker, millis / 86_400_000L)
                        }
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onDatePickerDismissed() }) { Text("Cancel") }
                }
            ) { DatePicker(state = datePickerState) }
        }
```

Remove the `onTaskPostpone` parameter from `TargetsBodyContent` (line 207), `TasksTabContent` (line 269), and their call sites (lines 115, 237, 326).

- [ ] **Step 3: Remove postpone from TargetsViewModel**

In `TargetsViewModel.kt`, remove the three methods (lines 183-198):

Remove:
```kotlin
    fun onTaskPostpone(taskId: String) {
        _uiState.update { it.copy(showDatePickerForTask = taskId) }
    }

    fun onDatePickerResult(taskId: String, epochDay: Long) {
        viewModelScope.launch {
            TaskRespository.updateTaskDeadline(taskId, TaskDeadline.OnDate(epochDay)).onFailure { error ->
                _uiState.update { it.copy(errorMessage = "Error updating deadline: ${error.message}") }
            }
        }
        _uiState.update { it.copy(showDatePickerForTask = null) }
    }

    fun onDatePickerDismissed() {
        _uiState.update { it.copy(showDatePickerForTask = null) }
    }
```

Remove the `TaskDeadline` import if unused.

- [ ] **Step 4: Remove showDatePickerForTask from TargetsUiState**

In `TargetsUiState.kt`, remove line 31:
```kotlin
    val showDatePickerForTask: String? = null,
```

- [ ] **Step 5: Remove postpone from GoalDetailScreen**

In `GoalDetailScreen.kt`, remove `onTaskPostpone` from the `TaskRowWithActions` call (line 91):

Change:
```kotlin
                        TaskRowWithActions(
                            task = mission,
                            parentGoalTitle = goal.title,
                            onTaskDetail = { viewModel.onMissionClicked(mission) },
                            onTaskComplete = { viewModel.onTaskComplete(mission.id) },
                            onTaskPostpone = { viewModel.onTaskPostpone(mission.id) },
                            onTaskDelete = { viewModel.onTaskDelete(mission.id) }
                        )
```

To:
```kotlin
                        TaskRowWithActions(
                            task = mission,
                            parentGoalTitle = goal.title,
                            onTaskDetail = { viewModel.onMissionClicked(mission) },
                            onTaskComplete = { viewModel.onTaskComplete(mission.id) },
                            onTaskDelete = { viewModel.onTaskDelete(mission.id) }
                        )
```

Remove the DatePickerDialog block (lines 126-148):

Remove:
```kotlin
        if (uiState.showDatePickerForTask != null) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = System.currentTimeMillis()
            )
            DatePickerDialog(
                onDismissRequest = { viewModel.onDatePickerDismissed() },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.onDatePickerResult(
                                uiState.showDatePickerForTask!!,
                                millis / 86_400_000L
                            )
                        }
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onDatePickerDismissed() }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
```

Remove now-unused imports: `DatePicker`, `DatePickerDialog`, `rememberDatePickerState`.

- [ ] **Step 6: Remove postpone from GoalDetailViewModel**

In `GoalDetailViewModel.kt`, remove `showDatePickerForTask` from `GoalDetailUiState` (line 24):

Change:
```kotlin
data class GoalDetailUiState(
    val goal: Goal? = null,
    val missions: List<Task> = emptyList(),
    val selectedMission: Task? = null,
    val showDatePickerForTask: String? = null
)
```

To:
```kotlin
data class GoalDetailUiState(
    val goal: Goal? = null,
    val missions: List<Task> = emptyList(),
    val selectedMission: Task? = null
)
```

Remove the three methods (lines 93-106):
```kotlin
    fun onTaskPostpone(taskId: String) {
        _uiState.update { it.copy(showDatePickerForTask = taskId) }
    }

    fun onDatePickerResult(taskId: String, epochDay: Long) {
        viewModelScope.launch {
            TaskRespository.updateTaskDeadline(taskId, TaskDeadline.OnDate(epochDay))
        }
        _uiState.update { it.copy(showDatePickerForTask = null) }
    }

    fun onDatePickerDismissed() {
        _uiState.update { it.copy(showDatePickerForTask = null) }
    }
```

Remove the `TaskDeadline` import if unused.

- [ ] **Step 7: Build and verify**

Run:
```bash
cd C:/1percent && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL. Postpone is gone from all list views.

- [ ] **Step 8: Present checklist and commit after user confirmation**

Present checklist to user. After confirmation:
```bash
git add app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsScreen.kt app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsViewModel.kt app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsUiState.kt app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/GoalDetailScreen.kt app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/GoalDetailViewModel.kt
git commit -m "refactor: remove postpone action from list views, centralize in edit flow"
```

---

## Task 3: Goal Share Propagation in Repository

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/data/GoalRepository.kt:99-108`

- [ ] **Step 1: Update shareGoal to propagate to child tasks**

In `GoalRepository.kt`, replace the `shareGoal` method (lines 99-108):

```kotlin
    suspend fun shareGoal(goalId: String, friendUserId: String): Result<Unit> {
        return try {
            goalsCollection.document(goalId)
                .update("sharedWith", FieldValue.arrayUnion(friendUserId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
```

With:
```kotlin
    suspend fun shareGoal(goalId: String, friendUserId: String): Result<Unit> {
        return try {
            val batch = db.batch()

            batch.update(
                goalsCollection.document(goalId),
                "sharedWith", FieldValue.arrayUnion(friendUserId)
            )

            val childTasks = FirebaseFirestore.getInstance()
                .collection("tasks")
                .whereEqualTo("goalId", goalId)
                .get()
                .await()

            childTasks.documents.forEach { doc ->
                batch.update(doc.reference, "sharedWith", FieldValue.arrayUnion(friendUserId))
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
```

- [ ] **Step 2: Build and verify**

Run:
```bash
cd C:/1percent && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Present checklist and commit after user confirmation**

Present checklist to user. After confirmation:
```bash
git add app/src/main/java/es/uc3m/android/a1percent/data/GoalRepository.kt
git commit -m "feat: propagate sharedWith to child tasks when sharing a goal"
```

---

## Task 4: Share Button on GoalDetailScreen

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/GoalDetailViewModel.kt`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/GoalDetailScreen.kt`

- [ ] **Step 1: Add sharing state and actions to GoalDetailViewModel**

In `GoalDetailViewModel.kt`, add imports at the top:
```kotlin
import es.uc3m.android.a1percent.data.GoalRepository
import es.uc3m.android.a1percent.data.SocialRepository
import es.uc3m.android.a1percent.data.model.UserProfile
```

Update `GoalDetailUiState` to include sharing fields:
```kotlin
data class GoalDetailUiState(
    val goal: Goal? = null,
    val missions: List<Task> = emptyList(),
    val selectedMission: Task? = null,
    val showShareSheet: Boolean = false,
    val friends: List<UserProfile> = emptyList(),
    val snackbarMessage: String? = null
)
```

In the `init` block, after the existing `SessionRepository.currentUser` observer, add a friends observer. Replace the init block:
```kotlin
    init {
        SessionRepository.currentUser
            .onEach { user ->
                val goalId = currentGoalId
                if (user == null || goalId == null) {
                    _uiState.value = GoalDetailUiState()
                } else {
                    loadGoalForUser(user.id, goalId)
                }
            }
            .launchIn(viewModelScope)
    }
```

With:
```kotlin
    init {
        SessionRepository.currentUser
            .onEach { user ->
                val goalId = currentGoalId
                if (user == null || goalId == null) {
                    _uiState.value = GoalDetailUiState()
                } else {
                    loadGoalForUser(user.id, goalId)
                    SocialRepository.observeFriends(user.id)
                        .onEach { friends ->
                            _uiState.update { it.copy(friends = friends) }
                        }
                        .launchIn(viewModelScope)
                }
            }
            .launchIn(viewModelScope)
    }
```

Add these methods after the existing action methods:
```kotlin
    fun onShareGoalRequested() {
        _uiState.update { it.copy(showShareSheet = true) }
    }

    fun onShareWithFriend(friendUserId: String, friendName: String) {
        val goal = _uiState.value.goal ?: return
        viewModelScope.launch {
            GoalRepository.shareGoal(goal.id, friendUserId).onSuccess {
                _uiState.update { it.copy(
                    showShareSheet = false,
                    snackbarMessage = "Shared with $friendName"
                ) }
            }.onFailure { error ->
                _uiState.update { it.copy(
                    showShareSheet = false,
                    snackbarMessage = "Error sharing: ${error.message}"
                ) }
            }
        }
    }

    fun onShareDismissed() {
        _uiState.update { it.copy(showShareSheet = false) }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
```

- [ ] **Step 2: Add Share button and ShareBottomSheet to GoalDetailScreen**

In `GoalDetailScreen.kt`, add these imports:
```kotlin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import es.uc3m.android.a1percent.ui.components.ShareBottomSheet
```

Inside the `GoalDetailScreen` composable, add a snackbar host state and LaunchedEffect after `val missions = uiState.missions`:
```kotlin
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearSnackbarMessage()
    }
```

Add a Share IconButton to the TopAppBar `actions` parameter. Change the TopAppBar:
```kotlin
            TopAppBar(
                title = { Text(goal?.title ?: "Goal Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
```

To:
```kotlin
            TopAppBar(
                title = { Text(goal?.title ?: "Goal Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (goal != null) {
                        IconButton(onClick = { viewModel.onShareGoalRequested() }) {
                            Icon(Icons.Default.Share, contentDescription = "Share goal")
                        }
                    }
                }
            )
```

Add the `ShareBottomSheet` and `SnackbarHost` after the DatePicker removal area (at the end of the Scaffold content, alongside the TaskDetailModal block):
```kotlin
    if (uiState.showShareSheet && goal != null) {
        ShareBottomSheet(
            itemName = goal.title,
            friends = uiState.friends,
            onShareWith = { userId, name -> viewModel.onShareWithFriend(userId, name) },
            onDismiss = { viewModel.onShareDismissed() }
        )
    }
```

- [ ] **Step 3: Build and verify**

Run:
```bash
cd C:/1percent && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Present checklist and commit after user confirmation**

Present checklist to user. After confirmation:
```bash
git add app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/GoalDetailViewModel.kt app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/GoalDetailScreen.kt
git commit -m "feat: add share button to goal detail screen with friend selection"
```

---

## Task 5: Hide Share on Missions

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsScreen.kt:381-389, 502-506`

- [ ] **Step 1: Conditionally show Share chip in TaskRowWithActions**

In `TargetsScreen.kt`, the `TaskRowWithActions` composable already has a `task` parameter. Add a condition to hide the Share chip when `task.goalId != null` (i.e., it's a mission).

Change the Share AssistChip block (lines 502-506):
```kotlin
                AssistChip(
                    onClick = onTaskShare,
                    label = { Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
```

To:
```kotlin
                if (task.goalId == null) {
                    AssistChip(
                        onClick = onTaskShare,
                        label = { Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }
```

- [ ] **Step 2: Build and verify**

Run:
```bash
cd C:/1percent && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL. Share chip hidden for missions.

- [ ] **Step 3: Present checklist and commit after user confirmation**

Present checklist to user. After confirmation:
```bash
git add app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsScreen.kt
git commit -m "feat: hide share action on missions, only allow sharing standalone tasks"
```

---

## Task 6: Collaborator Avatars in Lists and Detail Views

**Files:**
- Create: `app/src/main/java/es/uc3m/android/a1percent/ui/components/CollaboratorAvatars.kt`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsScreen.kt`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/GoalDetailScreen.kt`

- [ ] **Step 1: Create CollaboratorAvatars composable**

Create `app/src/main/java/es/uc3m/android/a1percent/ui/components/CollaboratorAvatars.kt`:

```kotlin
package es.uc3m.android.a1percent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import es.uc3m.android.a1percent.data.model.UserProfile

@Composable
fun CollaboratorAvatars(
    sharedWith: List<String>,
    currentUserId: String,
    friends: List<UserProfile>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 3,
    avatarSize: Int = 24
) {
    val collaboratorIds = sharedWith.filter { it != currentUserId }
    if (collaboratorIds.isEmpty()) return

    val resolved = collaboratorIds.mapNotNull { id -> friends.find { it.id == id } }
    if (resolved.isEmpty()) return

    val visible = resolved.take(maxVisible)
    val overflow = resolved.size - maxVisible

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((-6).dp)
    ) {
        visible.forEach { user ->
            Box(
                modifier = Modifier
                    .size(avatarSize.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!user.avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = user.name,
                        modifier = Modifier.size(avatarSize.dp).clip(CircleShape)
                    )
                } else {
                    Text(
                        text = user.name.take(1).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        if (overflow > 0) {
            Box(
                modifier = Modifier
                    .size(avatarSize.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$overflow",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}
```

- [ ] **Step 2: Add CollaboratorAvatars to TaskRowWithActions**

In `TargetsScreen.kt`, inside `TaskRowWithActions`, add the `CollaboratorAvatars` composable and pass the required state. First, update the signature to include `friends` and `currentUserId`:

Add parameters to `TaskRowWithActions`:
```kotlin
internal fun TaskRowWithActions(
    task: Task,
    parentGoalTitle: String? = null,
    friends: List<UserProfile> = emptyList(),
    currentUserId: String = "",
    onTaskDetail: () -> Unit = {},
    onTaskComplete: () -> Unit,
    onTaskDelete: () -> Unit,
    onTaskEdit: () -> Unit = {},
    onTaskShare: () -> Unit = {}
) {
```

Inside the Card Column, after the type badge row (after line 478 — closing of the status badge row) and before `HorizontalDivider()`, add:

```kotlin
            CollaboratorAvatars(
                sharedWith = task.sharedWith,
                currentUserId = currentUserId,
                friends = friends
            )
```

Add the import:
```kotlin
import es.uc3m.android.a1percent.ui.components.CollaboratorAvatars
```

Update all call sites of `TaskRowWithActions` to pass `friends` and `currentUserId`:

In `TasksTabContent` (items block), add the parameters:
```kotlin
            TaskRowWithActions(
                task = task,
                parentGoalTitle = task.goalId?.let { uiState.goalTitleById[it] },
                friends = uiState.friends,
                currentUserId = SessionRepository.currentUser.value?.id ?: "",
                ...
            )
```

Add the import for `SessionRepository`:
```kotlin
import es.uc3m.android.a1percent.data.SessionRepository
```

In `GoalDetailScreen.kt`, update the `TaskRowWithActions` call to also pass `friends` and `currentUserId`:
```kotlin
                        TaskRowWithActions(
                            task = mission,
                            parentGoalTitle = goal.title,
                            friends = uiState.friends,
                            currentUserId = SessionRepository.currentUser.value?.id ?: "",
                            onTaskDetail = { viewModel.onMissionClicked(mission) },
                            onTaskComplete = { viewModel.onTaskComplete(mission.id) },
                            onTaskDelete = { viewModel.onTaskDelete(mission.id) }
                        )
```

- [ ] **Step 3: Add CollaboratorAvatars to GoalHeaderCard**

In `GoalDetailScreen.kt`, update `GoalHeaderCard` to accept and display collaborators. Change its signature:

```kotlin
@Composable
private fun GoalHeaderCard(
    goal: Goal,
    friends: List<UserProfile>,
    currentUserId: String
) {
```

Inside the card, after the last Row with XP/Difficulty (before the closing `}` of the Card Column), add:

```kotlin
            CollaboratorAvatars(
                sharedWith = goal.sharedWith,
                currentUserId = currentUserId,
                friends = friends,
                avatarSize = 28
            )
```

Update the call site from:
```kotlin
                    GoalHeaderCard(goal = goal)
```
To:
```kotlin
                    GoalHeaderCard(
                        goal = goal,
                        friends = uiState.friends,
                        currentUserId = SessionRepository.currentUser.value?.id ?: ""
                    )
```

Add imports to `GoalDetailScreen.kt`:
```kotlin
import es.uc3m.android.a1percent.data.SessionRepository
import es.uc3m.android.a1percent.data.model.UserProfile
import es.uc3m.android.a1percent.ui.components.CollaboratorAvatars
```

- [ ] **Step 4: Build and verify**

Run:
```bash
cd C:/1percent && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Present checklist and commit after user confirmation**

Present checklist to user. After confirmation:
```bash
git add app/src/main/java/es/uc3m/android/a1percent/ui/components/CollaboratorAvatars.kt app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsScreen.kt app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/GoalDetailScreen.kt
git commit -m "feat: show collaborator avatars in task and goal list items"
```

---

## Task 7: HomeScreen Completed Checkbox Bugfix

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/home/HomeFilters.kt`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/home/HomeViewModel.kt:227-232, 290-303`

- [ ] **Step 1: Add status filter to HomeFilters**

In `HomeFilters.kt`, add a `HomeStatusFilter` enum and update `HomeFilters`, `HomeFilterKey`, and `buildHomeFilterUiItems`.

Replace the entire file content:
```kotlin
package es.uc3m.android.a1percent.ui.screens.home

data class HomeFilters(
    val showOnlyMissions: Boolean = false,
    val sortBy: HomeSort = HomeSort.NONE,
    val statusFilter: HomeStatusFilter = HomeStatusFilter.PENDING
)

enum class HomeStatusFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    COMPLETED("Completed")
}

enum class HomeFilterKey {
    STATUS,
    MISSIONS,
    SORT_BY_DATE
}

data class HomeFilterUiItem(
    val key: HomeFilterKey,
    val label: String,
    val isSelected: Boolean,
    val order: Int
)

fun buildHomeFilterUiItems(filters: HomeFilters): List<HomeFilterUiItem> {
    val items = listOf(
        HomeFilterUiItem(
            key = HomeFilterKey.STATUS,
            label = filters.statusFilter.label,
            isSelected = filters.statusFilter != HomeStatusFilter.PENDING,
            order = 0
        ),
        HomeFilterUiItem(
            key = HomeFilterKey.MISSIONS,
            label = "Missions",
            isSelected = filters.showOnlyMissions,
            order = 1
        ),
        HomeFilterUiItem(
            key = HomeFilterKey.SORT_BY_DATE,
            label = "Sort by Date",
            isSelected = filters.sortBy == HomeSort.DATE_ASC,
            order = 2
        )
    )
    return items.sortedWith(compareByDescending<HomeFilterUiItem> { it.isSelected }.thenBy { it.order })
}

enum class HomeSort {
    NONE,
    DATE_ASC
}
```

- [ ] **Step 2: Update HomeViewModel filter logic**

In `HomeViewModel.kt`, update `onFilterClicked` (lines 227-232) to handle the new STATUS key:

Replace:
```kotlin
    fun onFilterClicked(filterKey: HomeFilterKey) {
        when (filterKey) {
            HomeFilterKey.MISSIONS -> onMissionsFilterToggled()
            HomeFilterKey.SORT_BY_DATE -> onSortByDateToggled()
        }
    }
```

With:
```kotlin
    fun onFilterClicked(filterKey: HomeFilterKey) {
        when (filterKey) {
            HomeFilterKey.STATUS -> onStatusFilterToggled()
            HomeFilterKey.MISSIONS -> onMissionsFilterToggled()
            HomeFilterKey.SORT_BY_DATE -> onSortByDateToggled()
        }
    }
```

Add the `onStatusFilterToggled` method:
```kotlin
    fun onStatusFilterToggled() {
        _uiState.update { current ->
            val nextStatus = when (current.filters.statusFilter) {
                HomeStatusFilter.PENDING -> HomeStatusFilter.COMPLETED
                HomeStatusFilter.COMPLETED -> HomeStatusFilter.ALL
                HomeStatusFilter.ALL -> HomeStatusFilter.PENDING
            }
            val updatedFilters = current.filters.copy(statusFilter = nextStatus)
            reduceHomeState(current.copy(filters = updatedFilters))
        }
    }
```

Update `applyFiltersAndSort` (lines 290-303) to use the status filter:

Replace:
```kotlin
    private fun applyFiltersAndSort(tasks: List<Task>, filters: HomeFilters): List<Task> {
        val pendingTasks = tasks.filter { it.status == TaskStatus.PENDING }

        val filtered = if (filters.showOnlyMissions) {
            pendingTasks.filter { it.goalId != null }
        } else {
            pendingTasks
        }

        return when (filters.sortBy) {
            HomeSort.NONE -> filtered.sortedWith(TaskDeadlineResolver.taskDeadlineComparator())
            HomeSort.DATE_ASC -> filtered.sortedBy { TaskDeadlineResolver.toSortKey(it.deadline) }
        }
    }
```

With:
```kotlin
    private fun applyFiltersAndSort(tasks: List<Task>, filters: HomeFilters): List<Task> {
        val statusFiltered = when (filters.statusFilter) {
            HomeStatusFilter.PENDING -> tasks.filter { it.status == TaskStatus.PENDING }
            HomeStatusFilter.COMPLETED -> tasks.filter { it.status == TaskStatus.COMPLETED }
            HomeStatusFilter.ALL -> tasks
        }

        val filtered = if (filters.showOnlyMissions) {
            statusFiltered.filter { it.goalId != null }
        } else {
            statusFiltered
        }

        return when (filters.sortBy) {
            HomeSort.NONE -> filtered.sortedWith(TaskDeadlineResolver.taskDeadlineComparator())
            HomeSort.DATE_ASC -> filtered.sortedBy { TaskDeadlineResolver.toSortKey(it.deadline) }
        }
    }
```

- [ ] **Step 3: Build and verify**

Run:
```bash
cd C:/1percent && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL. Completed filter now shows completed tasks with checked checkboxes.

- [ ] **Step 4: Present checklist and commit after user confirmation**

Present checklist to user. After confirmation:
```bash
git add app/src/main/java/es/uc3m/android/a1percent/ui/screens/home/HomeFilters.kt app/src/main/java/es/uc3m/android/a1percent/ui/screens/home/HomeViewModel.kt
git commit -m "fix: completed filter now shows tasks with checked checkboxes on HomeScreen"
```

---

## Task 8: TargetsScreen Shared Tasks Filter

**Files:**
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsViewModel.kt:80-105`
- Modify: `app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsFilters.kt:11-13`

- [ ] **Step 1: Update SHARED filter label**

In `TargetsFilters.kt`, update the `SHARED` enum label (line 13):

Change:
```kotlin
    SHARED("Shared")
```

To:
```kotlin
    SHARED("Shared with me")
```

- [ ] **Step 2: Implement SHARED filter logic in TargetsViewModel**

In `TargetsViewModel.kt`, update `applyTaskFiltersAndSort` (lines 80-105). The SHARED filter currently returns `true` (no-op). Replace the filter logic:

Change:
```kotlin
    private fun applyTaskFiltersAndSort(filters: TaskFilters): List<Task> {
        val statusFiltered = filters.selectedStatus?.let { status ->
            allTasks.filter { it.status == status }
        } ?: allTasks

        val filtered = if (filters.quickFilters.isEmpty()) {
            statusFiltered
        } else {
            statusFiltered.filter { task ->
                filters.quickFilters.all { filter ->
                    when (filter) {
                        TaskQuickFilter.MISSIONS -> task.goalId != null
                        TaskQuickFilter.SHARED -> true  // TODO: replace with real shared/collaboration source
                    }
                }
            }
        }

        return when (filters.sort) {
            TaskSort.NONE -> filtered
            TaskSort.DEADLINE_ASC -> filtered.sortedBy { task ->
                TaskDeadlineResolver.toSortKey(task.deadline)
            }
            TaskSort.XP_DESC -> filtered.sortedByDescending { it.xp }
        }
    }
```

To:
```kotlin
    private fun applyTaskFiltersAndSort(filters: TaskFilters): List<Task> {
        val currentUserId = SessionRepository.currentUser.value?.id ?: ""

        val statusFiltered = filters.selectedStatus?.let { status ->
            allTasks.filter { it.status == status }
        } ?: allTasks

        val filtered = if (filters.quickFilters.isEmpty()) {
            statusFiltered
        } else {
            statusFiltered.filter { task ->
                filters.quickFilters.all { filter ->
                    when (filter) {
                        TaskQuickFilter.MISSIONS -> task.goalId != null
                        TaskQuickFilter.SHARED -> task.ownerId != currentUserId
                    }
                }
            }
        }

        return when (filters.sort) {
            TaskSort.NONE -> filtered
            TaskSort.DEADLINE_ASC -> filtered.sortedBy { task ->
                TaskDeadlineResolver.toSortKey(task.deadline)
            }
            TaskSort.XP_DESC -> filtered.sortedByDescending { it.xp }
        }
    }
```

- [ ] **Step 3: Build and verify**

Run:
```bash
cd C:/1percent && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL. "Shared with me" filter shows only tasks from other users.

- [ ] **Step 4: Present checklist and commit after user confirmation**

Present checklist to user. After confirmation:
```bash
git add app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsViewModel.kt app/src/main/java/es/uc3m/android/a1percent/ui/screens/targets/TargetsFilters.kt
git commit -m "feat: implement shared-with-me filter on TargetsScreen"
```
