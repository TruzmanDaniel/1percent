package es.uc3m.android.a1percent.ui.screens.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalCard(
    onDismiss: () -> Unit,
    viewModel: CreateGoalViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        val glassShape = RoundedCornerShape(24.dp)
        val glassGradient = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
            )
        )

        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = glassShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = glassGradient)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Create Goal", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = {
                        viewModel.resetState()
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (uiState.aiState == AiNegotiationState.IDLE) {
                        OutlinedTextField(
                            value = uiState.goalName,
                            onValueChange = viewModel::onGoalNameChange,
                            label = { Text("Goal Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column {
                            Text(
                                text = "Difficulty: ${uiState.difficulty.toInt()}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Slider(
                                value = uiState.difficulty,
                                onValueChange = viewModel::onDifficultyChange,
                                valueRange = 1f..5f,
                                steps = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "+${uiState.difficulty.toInt() * 50} XP",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                            TextButton(onClick = { viewModel.onShowDatePicker() }) {
                                Text(if (uiState.deadlineEpochMillis != null) "Cambiar fecha" else "Seleccionar fecha")
                            }
                        }

                        if (uiState.showDatePicker) {
                            val datePickerState = rememberDatePickerState(
                                initialSelectedDateMillis = uiState.deadlineEpochMillis
                                    ?: (System.currentTimeMillis() + 30L * 24 * 3600 * 1000)
                            )
                            DatePickerDialog(
                                onDismissRequest = { viewModel.onDismissDatePicker() },
                                confirmButton = {
                                    TextButton(onClick = {
                                        datePickerState.selectedDateMillis?.let {
                                            viewModel.onDeadlineSelected(it)
                                        }
                                    }) { Text("OK") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { viewModel.onDismissDatePicker() }) {
                                        Text("Cancel")
                                    }
                                }
                            ) {
                                DatePicker(state = datePickerState)
                            }
                        }

                        if (uiState.availableCredits > 0) {
                            Text(
                                text = "AI credits: ${uiState.availableCredits} remaining",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        AiProposalCard(
                            state = uiState.aiState,
                            proposedTasks = uiState.proposedTasks,
                            canNegotiate = uiState.canNegotiate,
                            errorMessage = uiState.errorMessage,
                            onEasier = { viewModel.adjustAndRegenerate(isEasier = true) },
                            onAccept = {
                                viewModel.acceptProposal(
                                    onSuccess = {
                                        viewModel.resetState()
                                        onDismiss()
                                    },
                                    onError = {}
                                )
                            },
                            onHarder = { viewModel.adjustAndRegenerate(isEasier = false) },
                            onRetry = { viewModel.generateProposal() }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.aiState == AiNegotiationState.IDLE) {
                        Button(
                            onClick = {
                                viewModel.resetState()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                viewModel.createGoal(
                                    onSuccess = {
                                        viewModel.resetState()
                                        onDismiss()
                                    },
                                    onError = {}
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = uiState.canCreateGoal
                        ) {
                            Text("Create (No AI)")
                        }

                        Button(
                            onClick = { viewModel.generateProposal() },
                            modifier = Modifier.weight(1f),
                            enabled = uiState.canGenerateAi
                        ) {
                            Text("Create with AI")
                        }
                    }
                }
            }
        }
    }
}
