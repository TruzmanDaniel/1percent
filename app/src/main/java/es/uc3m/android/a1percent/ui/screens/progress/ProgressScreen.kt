package es.uc3m.android.a1percent.ui.screens.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.remember
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import es.uc3m.android.a1percent.R

private val donutColors = listOf(
    Color(0xFF818CF8), // indigo
    Color(0xFF34D399), // emerald
    Color(0xFFFBBF24), // amber
    Color(0xFFF472B6), // pink
    Color(0xFF60A5FA), // blue
    Color(0xFFA78BFA), // violet
    Color(0xFF2DD4BF), // teal
)

@Composable
fun ProgressScreen(
    navController: NavController,
    viewModel: ProgressViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressBodyContent(uiState = uiState)
}

@Composable
private fun ProgressBodyContent(uiState: ProgressUiState) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.progress_title),
                style = MaterialTheme.typography.headlineSmall
            )
        }

        // Chart 5 — Level / XP / Streak
        item { LevelXpStatCard(uiState) }

        // Chart 2 — Goals progress
        item { GoalsProgressCard(uiState.activeGoals) }

        // Chart 1 — XP accumulation curve
        item { XpCurveCard(uiState.xpCurvePoints) }

        // Chart 3 — Weekly rhythm
        item { WeeklyRhythmCard(uiState.thisWeekByDay, uiState.lastWeekByDay) }

        // Chart 4 — Category breakdown
        item { CategoryDonutCard(uiState.categoryBreakdown) }

        // Chart 6 — Weekly sparklines
        item { GoalSparklinesCard(uiState.goalSparklines) }
    }
}

@Composable
private fun GoalSparklinesCard(sparklines: List<GoalSparkline>) {
    ProgressCard(title = stringResource(R.string.progress_goal_sparklines_title), subtitle = stringResource(R.string.progress_goal_sparklines_subtitle)) {
        if (sparklines.isEmpty()) {
            Text(
                text = stringResource(R.string.progress_goal_sparklines_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                sparklines.forEach { sparkline ->
                    GoalSparklineRow(sparkline)
                }
            }
        }
    }
}

@Composable
private fun GoalSparklineRow(sparkline: GoalSparkline) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = sparkline.goalTitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val modelProducer = remember(sparkline.goalTitle) { CartesianChartModelProducer() }
        LaunchedEffect(sparkline.weeklyCompletionRates) {
            modelProducer.runTransaction {
                lineSeries { series(sparkline.weeklyCompletionRates) }
            }
        }
        CartesianChartHost(
            chart = rememberCartesianChart(rememberLineCartesianLayer()),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        )
    }
}

@Composable
private fun WeeklyRhythmCard(thisWeek: List<Int>, lastWeek: List<Int>) {
    ProgressCard(title = stringResource(R.string.progress_weekly_rhythm_title), subtitle = stringResource(R.string.progress_weekly_rhythm_subtitle)) {
        if (thisWeek.none { it > 0 } && lastWeek.none { it > 0 }) {
            Text(
                text = stringResource(R.string.progress_weekly_rhythm_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@ProgressCard
        }
        val modelProducer = remember { CartesianChartModelProducer() }
        LaunchedEffect(thisWeek, lastWeek) {
            modelProducer.runTransaction {
                columnSeries {
                    series(thisWeek)
                    series(lastWeek)
                }
            }
        }
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dayFormatter = CartesianValueFormatter { _, value, _ ->
            days.getOrElse(value.toInt()) { "" }
        }
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = dayFormatter)
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(
                color = MaterialTheme.colorScheme.primary,
                label = stringResource(R.string.progress_weekly_this_week),
                percent = ""
            )
            LegendItem(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                label = stringResource(R.string.progress_weekly_last_week),
                percent = ""
            )
        }
    }
}

@Composable
private fun XpCurveCard(points: List<Float>) {
    ProgressCard(title = stringResource(R.string.progress_xp_curve_title), subtitle = stringResource(R.string.progress_xp_curve_subtitle)) {
        if (points.none { it > 0f }) {
            Text(
                text = stringResource(R.string.progress_xp_curve_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@ProgressCard
        }
        val modelProducer = remember { CartesianChartModelProducer() }
        LaunchedEffect(points) {
            modelProducer.runTransaction { lineSeries { series(points) } }
        }
        CartesianChartHost(
            chart = rememberCartesianChart(rememberLineCartesianLayer()),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        )
    }
}

@Composable
private fun CategoryDonutCard(breakdown: List<CategorySlice>) {
    ProgressCard(title = stringResource(R.string.progress_category_breakdown_title), subtitle = stringResource(R.string.progress_category_breakdown_subtitle)) {
        if (breakdown.isEmpty()) {
            Text(
                text = stringResource(R.string.progress_category_breakdown_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val total = breakdown.sumOf { it.count }
                DonutChart(
                    slices = breakdown,
                    total = total,
                    modifier = Modifier.size(130.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    breakdown.forEachIndexed { index, slice ->
                        LegendItem(
                            color = donutColors[index % donutColors.size],
                            label = slice.label,
                            percent = "${(slice.fraction * 100).toInt()}%"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    slices: List<CategorySlice>,
    total: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 26.dp.toPx()
            val gapDegrees = 2f
            val diameter = size.minDimension - strokePx
            val arcTopLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            var startAngle = -90f
            slices.forEachIndexed { index, slice ->
                val sweep = (slice.fraction * 360f - gapDegrees).coerceAtLeast(0f)
                drawArc(
                    color = donutColors[index % donutColors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                )
                startAngle += slice.fraction * 360f
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$total",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.progress_category_donut_tasks_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, percent: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = percent,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GoalsProgressCard(goals: List<GoalProgressItem>) {
    ProgressCard(title = stringResource(R.string.progress_goals_title), subtitle = stringResource(R.string.progress_goals_subtitle)) {
        if (goals.isEmpty()) {
            Text(
                text = stringResource(R.string.progress_goals_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                goals.forEach { goal -> GoalProgressRow(goal) }
            }
        }
    }
}

@Composable
private fun GoalProgressRow(goal: GoalProgressItem) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = goal.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.category.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(goal.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        LinearProgressIndicator(
            progress = { goal.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer
        )
    }
}

@Composable
private fun LevelXpStatCard(uiState: ProgressUiState) {
    ProgressCard(title = stringResource(R.string.progress_level_xp_title), subtitle = stringResource(R.string.progress_level_xp_subtitle)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${uiState.userLevel}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Level ${uiState.userLevel}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${uiState.currentXp} / ${uiState.xpToNextLevel} XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val xpProgress = (uiState.currentXp.toFloat() / uiState.xpToNextLevel.coerceAtLeast(1))
                    .coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { xpProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = stringResource(R.string.progress_stat_streak),
                value = "${uiState.streakDays} days",
                color = MaterialTheme.colorScheme.tertiary
            )
            StatItem(
                label = stringResource(R.string.progress_stat_completed),
                value = "${uiState.totalTasksCompleted} tasks",
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun ProgressCard(
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
