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
            Text("→", fontSize = 24.sp)
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
