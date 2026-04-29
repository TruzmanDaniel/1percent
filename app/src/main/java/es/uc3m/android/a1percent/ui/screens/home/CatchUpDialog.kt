package es.uc3m.android.a1percent.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CatchUpDialog(
    goalTitle: String,
    onFeedback: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "¡Has vuelto!",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = goalTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("Llevas un tiempo sin entrar. ¡Vamos a retomar el ritmo!")
                Text(
                    text = "¿Cómo te sientes para volver?",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onFeedback("SOBRADO") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Con energía")
                }
                Button(
                    onClick = { onFeedback("PERFECTO") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Normal")
                }
                OutlinedButton(
                    onClick = { onFeedback("AGOTADO") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cansado")
                }
            }
        }
    )
}
