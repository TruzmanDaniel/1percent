package es.uc3m.android.a1percent.ui.screens.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.uc3m.android.a1percent.data.model.Task

@Composable
fun AiProposalCard(
    state: AiNegotiationState,
    proposedTasks: List<Task>,
    canNegotiate: Boolean,
    errorMessage: String?,
    onEasier: () -> Unit,
    onAccept: () -> Unit,
    onHarder: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (state) {
            AiNegotiationState.GENERATING -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Generando tus misiones semanales...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            AiNegotiationState.PROPOSAL_READY -> {
                Text(
                    text = "Tus misiones generadas por IA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    proposedTasks.forEach { task ->
                        ProposedTaskItem(task = task)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEasier,
                        enabled = canNegotiate,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Muy fácil")
                    }

                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Me gusta")
                    }

                    OutlinedButton(
                        onClick = onHarder,
                        enabled = canNegotiate,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Muy difícil")
                    }
                }
            }

            AiNegotiationState.ERROR -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "Error al generar misiones",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = onRetry) {
                        Text("Reintentar")
                    }
                }
            }

            else -> {}
        }
    }
}

@Composable
private fun ProposedTaskItem(task: Task) {
    val isEpic = task.dayIndex == 7

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEpic)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Día ${task.dayIndex}${if (isEpic) " - ÉPICA" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isEpic) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "+${task.xp} XP",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
