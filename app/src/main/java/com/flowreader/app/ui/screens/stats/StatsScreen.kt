package com.flowreader.app.ui.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowreader.app.R
import com.flowreader.app.core.designsystem.component.FlowStateHost
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.app.core.util.FlowFormatters
import com.flowreader.app.domain.model.DailyStats
import com.flowreader.app.ui.components.durationString
import com.flowreader.app.ui.components.durationText
import com.flowreader.app.ui.components.spokenDateString
import com.flowreader.app.domain.model.ReadingReport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showWeeklyGoalDialog by remember { mutableStateOf(false) }
    var showMonthlyGoalDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) }
            )
        }
    ) { paddingValues ->
        FlowStateHost(
            isLoading = uiState.isLoading,
            isEmpty = false,
            error = uiState.error,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            onRetry = { viewModel.clearError() }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(FlowSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(FlowSpacing.lg)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.stats_today_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Timer,
                            title = stringResource(R.string.stats_read_time),
                            value = durationText(uiState.todayReadTime),
                            subtitle = stringResource(R.string.stats_subtitle_today)
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            title = stringResource(R.string.stats_read_pages),
                            value = "${uiState.todayReadPages}",
                            subtitle = stringResource(R.string.stats_unit_pages)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.stats_total_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.AccessTime,
                            title = stringResource(R.string.stats_total_time),
                            value = durationText(uiState.totalReadTime),
                            subtitle = stringResource(R.string.stats_subtitle_total)
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.AutoStories,
                            title = stringResource(R.string.stats_total_pages),
                            value = "${uiState.totalReadPages}",
                            subtitle = stringResource(R.string.stats_unit_pages)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Book,
                            title = stringResource(R.string.stats_books_read),
                            value = "${uiState.totalBooks}",
                            subtitle = stringResource(R.string.stats_unit_books)
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.LocalFireDepartment,
                            title = stringResource(R.string.stats_streak),
                            value = "${uiState.currentStreak}",
                            subtitle = stringResource(R.string.stats_unit_days)
                        )
                    }
                }

                if (uiState.recentDailyStats.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.stats_trend_section),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        ReadTimeBarChart(
                            dailyStats = uiState.recentDailyStats,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.stats_report_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                uiState.weeklyReport?.let { report ->
                    item {
                        ReadingReportCard(
                            report = report,
                            goalMinutes = uiState.weeklyGoalMinutes,
                            onGoalClick = { showWeeklyGoalDialog = true }
                        )
                    }
                }

                uiState.monthlyReport?.let { report ->
                    item {
                        ReadingReportCard(
                            report = report,
                            goalMinutes = uiState.monthlyGoalMinutes,
                            onGoalClick = { showMonthlyGoalDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showWeeklyGoalDialog) {
        GoalDialog(
            title = stringResource(R.string.stats_goal_weekly),
            currentGoal = uiState.weeklyGoalMinutes,
            onGoalChange = { viewModel.updateWeeklyGoal(it) },
            onDismiss = { showWeeklyGoalDialog = false }
        )
    }

    if (showMonthlyGoalDialog) {
        GoalDialog(
            title = stringResource(R.string.stats_goal_monthly),
            currentGoal = uiState.monthlyGoalMinutes,
            onGoalChange = { viewModel.updateMonthlyGoal(it) },
            onDismiss = { showMonthlyGoalDialog = false }
        )
    }
}

@Composable
private fun ReadingReportCard(
    report: ReadingReport,
    goalMinutes: Int,
    onGoalClick: () -> Unit
) {
    val goalSeconds = goalMinutes * 60L
    val progress = if (goalSeconds > 0) (report.totalReadTime.toFloat() / goalSeconds).coerceIn(0f, 1f) else 0f
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(report.rangeLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onGoalClick) { Text(stringResource(R.string.stats_goal_button, goalMinutes)) }
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text(
                stringResource(
                    R.string.stats_report_summary,
                    durationText(report.totalReadTime),
                    report.totalReadPages
                )
            )
            val noneLabel = stringResource(R.string.stats_value_none)
            Text(stringResource(R.string.stats_fastest_day, report.fastestReadingDay?.date ?: noneLabel))
            Text(stringResource(R.string.stats_most_read_book, report.mostReadBookTitle ?: noneLabel))
            if (progress < 1f) {
                Text(
                    text = stringResource(
                        R.string.stats_goal_remaining,
                        ((goalSeconds - report.totalReadTime).coerceAtLeast(0) / 60).toInt()
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun GoalDialog(
    title: String,
    currentGoal: Int,
    onGoalChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var goal by remember(currentGoal) { mutableStateOf(currentGoal.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(stringResource(R.string.stats_minutes, goal.toInt()))
                Slider(value = goal, onValueChange = { goal = it }, valueRange = 30f..3000f)
            }
        },
        confirmButton = {
            TextButton(onClick = { onGoalChange(goal.toInt()); onDismiss() }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReadTimeBarChart(
    dailyStats: List<DailyStats>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val axisColor = MaterialTheme.colorScheme.outlineVariant

    val maxReadTime = dailyStats.maxOfOrNull { it.totalReadTime } ?: 0L
    val todayDate = dailyStats.lastOrNull()?.date

    // Each column carries semantics so TalkBack reads the date and duration instead of announcing an
    // unlabelled Canvas. Built from resources via Context rather than stringResource because this runs
    // inside remember. The v51 chart also painted every bar twice — an identical surfaceVariant rect
    // completely covered by the primary one — so the track is now the full column height and the value
    // bar is drawn once.
    val context = LocalContext.current
    val chartDescription = remember(dailyStats, context) {
        if (dailyStats.isEmpty()) {
            context.getString(R.string.stats_chart_empty_desc)
        } else {
            dailyStats.joinToString(context.getString(R.string.stats_chart_separator)) { stat ->
                context.getString(
                    R.string.stats_chart_entry,
                    context.spokenDateString(stat.date),
                    context.durationString(stat.totalReadTime)
                )
            }
        }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(FlowSpacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.stats_trend_card_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.stats_trend_peak, durationText(maxReadTime)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(FlowSpacing.sm))

            if (dailyStats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.stats_trend_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .semantics { contentDescription = chartDescription }
                ) {
                    val slot = size.width / dailyStats.size
                    val barWidth = slot * 0.55f
                    val chartHeight = size.height - 4f

                    drawRect(
                        color = axisColor,
                        topLeft = Offset(0f, chartHeight),
                        size = Size(size.width, 2f)
                    )

                    dailyStats.forEachIndexed { index, stat ->
                        val x = index * slot + (slot - barWidth) / 2f
                        drawRect(
                            color = trackColor,
                            topLeft = Offset(x, 0f),
                            size = Size(barWidth, chartHeight)
                        )
                        val barHeight = if (maxReadTime > 0) {
                            (stat.totalReadTime.toFloat() / maxReadTime) * chartHeight
                        } else {
                            0f
                        }
                        if (barHeight > 0f) {
                            drawRect(
                                color = primaryColor,
                                topLeft = Offset(x, chartHeight - barHeight),
                                size = Size(barWidth, barHeight)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(FlowSpacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    dailyStats.forEach { stat ->
                        Text(
                            text = FlowFormatters.shortDate(stat.date),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (stat.date == todayDate) FontWeight.Bold else FontWeight.Normal,
                            color = if (stat.date == todayDate) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
