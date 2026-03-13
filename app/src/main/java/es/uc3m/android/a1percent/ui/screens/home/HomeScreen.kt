package es.uc3m.android.a1percent.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.a1percent.data.model.Task
import es.uc3m.android.a1percent.data.model.enums.TaskStatus
import es.uc3m.android.a1percent.navigation.AppScreens

@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = viewModel()) {
    // Observing the screen state (DATA) to update the UI when it changes
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeBodyContent(uiState = uiState)
}

@Composable
fun HomeBodyContent(
    uiState: HomeUiState
) {
    // Scrollable Column containing the elements
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // 1. HEADER SECTION
        item {
            HeaderSection(uiState = uiState)
        }

        // 2. TASKS SECTION
        item {
            Text(
                text = "Today's missions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
            // CAMBIAR uiState, ahora solo maneja un GOAL!!!!!! - - -  - - -  TODO
        items(uiState.tasks) { task ->
            val goalTitle = if (task.goalId != null) uiState.goal.title else null
            TaskItem(task = task, goalTitle = goalTitle)
        }

        // 3. PROGRESS SECTION
        item {
            ProgressSection()
        }
    }
}


// Private composables

@Composable
private fun HeaderSection(uiState: HomeUiState) {
    val user = uiState.user
    val xpProgress = user.currentXp / user.xpToNextLevel.toFloat()

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Hey ${user.name}!",
            style = MaterialTheme.typography.headlineSmall
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Level ${user.level}  ·  ${user.currentXp} / ${user.xpToNextLevel} XP",
                style = MaterialTheme.typography.bodyMedium 
            )
        }

        LinearProgressIndicator(
            progress = { xpProgress },
            modifier = Modifier.fillMaxWidth().height(8.dp)
        )

        Text(
            text = "🔥 ${user.streakDays} day streak",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun TaskItem(task: Task, goalTitle: String?) {
    val isCompleted = task.status == TaskStatus.COMPLETED

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isCompleted,
            onCheckedChange = null   // non-interactive for now
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = task.title,
                style = if (isCompleted)
                    MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough)
                else
                    MaterialTheme.typography.bodyLarge
            )

            if (goalTitle != null) {
                Text(
                    text = "⚡ Mission · $goalTitle",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Text(
            text = "+${task.xp} XP",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ProgressSection() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Your 1% progress",
            style = MaterialTheme.typography.titleMedium
        )

        Box(
            modifier = Modifier.fillMaxWidth().height(150.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("📈 Progress chart coming soon")
        }

        Text(
            text = "1% better every day = 37x better in a year",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
