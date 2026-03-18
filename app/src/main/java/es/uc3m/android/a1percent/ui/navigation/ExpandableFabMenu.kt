package es.uc3m.android.a1percent.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ExpandableFabMenu(
    isExpanded: Boolean,
    onClose: () -> Unit,
    onAddTask: () -> Unit,
    onAddGoal: () -> Unit
) {
    val radius = 120.dp // Radio del semicírculo
    val angleRad = Math.toRadians(45.0) // 45 grados en radianes
    
    // Calculamos desplazamientos (X = R * sin(th), Y = -R * cos(th))
    val offsetX = (radius.value * sin(angleRad)).dp
    val offsetY = (radius.value * cos(angleRad)).dp

    Box(modifier = Modifier.fillMaxSize()) {
        // Overlay semitransparente
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClose() }
            )
        }

        // Contenedor del menú centrado en la base
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp) // Alineado con el centro del FAB original
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // GOAL (-45º -> Izquierda y arriba)
            FabMenuItem(
                visible = isExpanded,
                label = "Goal",
                icon = Icons.Default.Flag,
                modifier = Modifier.offset(x = -offsetX, y = -offsetY),
                onClick = {
                    onAddGoal()
                    onClose()
                }
            )

            // TASK (45º -> Derecha y arriba)
            FabMenuItem(
                visible = isExpanded,
                label = "Task",
                icon = Icons.Default.Assignment,
                modifier = Modifier.offset(x = offsetX, y = -offsetY),
                onClick = {
                    onAddTask()
                    onClose()
                }
            )
        }
    }
}

@Composable
private fun FabMenuItem(
    visible: Boolean,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FloatingActionButton(
                onClick = onClick,
                modifier = Modifier.size(56.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = androidx.compose.foundation.shape.CircleShape
            ) {
                Icon(imageVector = icon, contentDescription = label)
            }
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 12.sp
            )
        }
    }
}
