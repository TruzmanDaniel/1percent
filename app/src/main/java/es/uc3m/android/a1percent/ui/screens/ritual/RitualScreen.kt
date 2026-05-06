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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.uc3m.android.a1percent.R
import es.uc3m.android.a1percent.data.model.totalWeeks
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
                Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.ritual_skip_content_description), tint = Color.White.copy(alpha = 0.6f))
            }
        }

        if (uiState.showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = viewModel::onDismissDatePicker,
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { viewModel.onExtendDeadline(it) }
                    }) { Text(stringResource(R.string.ritual_date_confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::onDismissDatePicker) { Text(stringResource(R.string.ritual_date_cancel)) }
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
            text = if (uiState.isCatchUp) stringResource(R.string.ritual_summary_catch_up) else stringResource(R.string.ritual_summary_week_complete),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(goal.title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text("Semana ${uiState.weekNumber} de ${goal.totalWeeks()}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${uiState.goalProgress}% completado • ${uiState.weeksRemaining} semanas restantes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        if (uiState.isCatchUp) {
            Text(stringResource(R.string.ritual_summary_catch_up_message), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${uiState.tasksCompleted}/${uiState.totalTasks}", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.ritual_summary_missions_completed), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("+${uiState.xpEarned} XP", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }

        Spacer(Modifier.height(48.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text(stringResource(R.string.ritual_button_continue))
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
            text = if (uiState.epicMissionPassed) stringResource(R.string.ritual_epic_passed) else stringResource(R.string.ritual_epic_failed),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text(stringResource(R.string.ritual_button_continue))
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
        Text(stringResource(R.string.ritual_deadline_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("Progreso alcanzado: ${goal.progress}%", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        if (goal.extensionCount > 0) {
            Text("Extensiones previas: ${goal.extensionCount}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(48.dp))
        Button(onClick = onExtend, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text(stringResource(R.string.ritual_deadline_extend_button))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onComplete, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text(stringResource(R.string.ritual_deadline_complete_button))
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
            text = if (isCatchUp) stringResource(R.string.ritual_feedback_question_catch_up) else stringResource(R.string.ritual_feedback_question_normal),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))

        val options = if (isCatchUp) {
            listOf(
                Triple(EnergyFeedback.SOBRADO, stringResource(R.string.ritual_feedback_energy_high), stringResource(R.string.ritual_feedback_energy_high_desc)),
                Triple(EnergyFeedback.PERFECTO, stringResource(R.string.ritual_feedback_normal), stringResource(R.string.ritual_feedback_normal_desc)),
                Triple(EnergyFeedback.AGOTADO, stringResource(R.string.ritual_feedback_tired), stringResource(R.string.ritual_feedback_tired_desc))
            )
        } else {
            listOf(
                Triple(EnergyFeedback.SOBRADO, stringResource(R.string.ritual_feedback_sobrado), stringResource(R.string.ritual_feedback_sobrado_desc)),
                Triple(EnergyFeedback.PERFECTO, stringResource(R.string.ritual_feedback_perfecto), stringResource(R.string.ritual_feedback_perfecto_desc)),
                Triple(EnergyFeedback.AGOTADO, stringResource(R.string.ritual_feedback_agotado), stringResource(R.string.ritual_feedback_agotado_desc))
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
        Text(stringResource(R.string.ritual_intensity_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("%.1f".format(old), fontSize = 36.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("→", fontSize = 24.sp)
            Text("%.1f".format(new), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = if (isIncrease) Color(0xFF4CAF50) else Color(0xFFFF5722))
        }
        Spacer(Modifier.height(48.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text(stringResource(R.string.ritual_button_continue))
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
        Text(stringResource(R.string.ritual_generating_message), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
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
        Text(stringResource(R.string.ritual_complete_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        if (uiState.newIntensity != null) {
            Text("Nivel: ${"%.1f".format(uiState.newIntensity)}", style = MaterialTheme.typography.titleMedium)
        }
        Text("${uiState.goalProgress}% completado", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFD700))
        Spacer(Modifier.height(48.dp))
        Button(onClick = onFinished, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text(stringResource(R.string.ritual_complete_view_missions))
        }
    }
}
