package es.uc3m.android.a1percent.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ProgressScreen(navController: NavController) {
    ProgressBodyContent(navController)
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun ProgressBodyContent(navController: NavController) {
    val sections = listOf(
        "1% Curve",
        "Completed Tasks (Total & This Month)",
        "Task Category Heatmap",
        "XP Gained Over the Month",
        "Average Difficulty of Completed Tasks",
        "Level Evolution and Progress to Next Level",
        "Cumulative XP Chart (Exponential Progress)"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Progress Dashboard",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        item {
            Text(
                text = "Preview mode: these blocks are placeholders and will be replaced with real charts once data integration is ready.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProgressCard(
                    title = "Weekly XP Snapshot",
                    subtitle = "Fast visual check",
                    modifier = Modifier
                        .weight(1f)
                        .height(210.dp)
                ) {
                    MockBarGroupChart(listOf(0.25f, 0.42f, 0.36f, 0.58f, 0.62f, 0.49f))
                }

                ProgressCard(
                    title = "Task Pace Snapshot",
                    subtitle = "Completion rhythm",
                    modifier = Modifier
                        .weight(1f)
                        .height(210.dp)
                ) {
                    MockLineTrendChart()
                }
            }
        }

        sections.forEach { sectionTitle ->
            item {
            when (sectionTitle) {
                "1% Curve" -> {
                    ProgressCard(title = sectionTitle, subtitle = "Daily improvement trend") {
                        MockLineTrendChart()
                    }
                }

                "Completed Tasks (Total & This Month)" -> {
                    ProgressCard(title = sectionTitle, subtitle = "Global vs monthly comparison") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricRow(label = "Total", value = "248")
                            MetricRow(label = "This month", value = "34")
                            MockBarGroupChart(listOf(0.35f, 0.62f, 0.48f, 0.80f, 0.57f))
                        }
                    }
                }

                "Task Category Heatmap" -> {
                    ProgressCard(title = sectionTitle, subtitle = "Intensity by category") {
                        MockHeatMapGrid()
                    }
                }

                "XP Gained Over the Month" -> {
                    ProgressCard(title = sectionTitle, subtitle = "Weekly accumulation") {
                        MockBarGroupChart(listOf(0.22f, 0.31f, 0.44f, 0.38f, 0.56f, 0.71f, 0.65f))
                    }
                }

                "Average Difficulty of Completed Tasks" -> {
                    ProgressCard(title = sectionTitle, subtitle = "Current average: 3.4 / 5") {
                        MockDifficultyBlocks()
                    }
                }

                "Level Evolution and Progress to Next Level" -> {
                    ProgressCard(title = sectionTitle, subtitle = "Level 8 -> progress to level 9") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            MockLineTrendChart()
                            Text(
                                text = "340 / 500 XP",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                else -> {
                    ProgressCard(title = sectionTitle, subtitle = "Accumulated growth curve") {
                        MockExponentialBlocks()
                    }
                }
            }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Note: mock values for visual prototyping.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProgressCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}



@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun MockBarGroupChart(values: List<Float>) {
    // TODO: Temporary fake chart placeholder. Remove when real chart library/data is integrated.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { index, rawValue ->
            val clamped = rawValue.coerceIn(0.1f, 1f)
            val barColor = if (index % 2 == 0) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
            } else {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height((clamped * 96f).dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun MockLineTrendChart() {
    // TODO: Temporary fake chart placeholder. Remove when real chart library/data is integrated.
    val points = listOf(0.15f, 0.22f, 0.31f, 0.44f, 0.52f, 0.67f, 0.82f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        points.forEachIndexed { index, point ->
            val width = if (index % 3 == 0) 18.dp else 12.dp
            Box(
                modifier = Modifier
                    .width(width)
                    .height((point * 100f).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (index == points.lastIndex) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer
                    )
            )
        }
    }
}

@Composable
private fun MockHeatMapGrid() {
    // TODO: Temporary fake chart placeholder. Remove when real chart library/data is integrated.
    val shades = listOf(0.18f, 0.32f, 0.46f, 0.62f, 0.82f)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(4) { rowIndex ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(7) { colIndex ->
                    val alpha = shades[(rowIndex + colIndex) % shades.size]
                    Box(
                        modifier = Modifier
                            .size(if ((rowIndex + colIndex) % 3 == 0) 24.dp else 20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

@Composable
private fun MockDifficultyBlocks() {
    // TODO: Temporary fake chart placeholder. Remove when real chart library/data is integrated.
    val blocks = listOf(
        0.25f to 0.45f,
        0.50f to 0.60f,
        0.35f to 0.72f,
        0.60f to 0.38f
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { (left, right) ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height((left * 100f).dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height((right * 100f).dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                )
            }
        }
    }
}

@Composable
private fun MockExponentialBlocks() {
    // TODO: Temporary fake chart placeholder. Remove when real chart library/data is integrated.
    val widths = listOf(0.18f, 0.24f, 0.32f, 0.43f, 0.58f, 0.76f, 0.94f)

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        widths.forEachIndexed { index, widthFactor ->
            val color: Color = if (index < 3) {
                MaterialTheme.colorScheme.primaryContainer
            } else if (index < 5) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFactor)
                    .height(if (index % 2 == 0) 14.dp else 18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}
