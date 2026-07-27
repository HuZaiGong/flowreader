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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowreader.app.core.designsystem.component.FlowStateHost
import com.flowreader.app.core.designsystem.token.FlowSpacing
import com.flowreader.app.core.util.FlowFormatters
import com.flowreader.app.domain.model.DailyStats
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
                title = { Text("阅读统计") }
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
                        text = "今日阅读",
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
                            title = "阅读时长",
                            value = FlowFormatters.duration(uiState.todayReadTime),
                            subtitle = "今日"
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            title = "阅读页数",
                            value = "${uiState.todayReadPages}",
                            subtitle = "页"
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "累计阅读",
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
                            title = "总时长",
                            value = FlowFormatters.duration(uiState.totalReadTime),
                            subtitle = "累计"
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.AutoStories,
                            title = "总页数",
                            value = "${uiState.totalReadPages}",
                            subtitle = "页"
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
                            title = "阅读书籍",
                            value = "${uiState.totalBooks}",
                            subtitle = "本"
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.LocalFireDepartment,
                            title = "连续阅读",
                            value = "${uiState.currentStreak}",
                            subtitle = "天"
                        )
                    }
                }

                if (uiState.recentDailyStats.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "最近7天趋势",
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
                        text = "阅读报告与目标",
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
            title = "周目标",
            currentGoal = uiState.weeklyGoalMinutes,
            onGoalChange = { viewModel.updateWeeklyGoal(it) },
            onDismiss = { showWeeklyGoalDialog = false }
        )
    }

    if (showMonthlyGoalDialog) {
        GoalDialog(
            title = "月目标",
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
                TextButton(onClick = onGoalClick) { Text("目标 ${goalMinutes}分钟") }
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text("时长 ${FlowFormatters.duration(report.totalReadTime)} · 页数 ${report.totalReadPages}")
            Text("最快阅读日：${report.fastestReadingDay?.date ?: "暂无"}")
            Text("最常读书籍：${report.mostReadBookTitle ?: "暂无"}")
            if (progress < 1f) {
                Text(
                    text = "还差 ${((goalSeconds - report.totalReadTime).coerceAtLeast(0) / 60)} 分钟达成目标",
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
                Text("${goal.toInt()} 分钟")
                Slider(value = goal, onValueChange = { goal = it }, valueRange = 30f..3000f)
            }
        },
        confirmButton = {
            TextButton(onClick = { onGoalChange(goal.toInt()); onDismiss() }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
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

    // Each column carries semantics so TalkBack reads "7 月 20 日阅读 35 分钟" instead of
    // announcing an unlabelled Canvas. The v51 chart also painted every bar twice — an identical
    // surfaceVariant rect completely covered by the primary one — so the track is now the full
    // column height and the value bar is drawn once.
    val chartDescription = remember(dailyStats) {
        if (dailyStats.isEmpty()) {
            "近期无阅读记录"
        } else {
            dailyStats.joinToString("，") { stat ->
                "${FlowFormatters.spokenDate(stat.date)}阅读${FlowFormatters.duration(stat.totalReadTime)}"
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
                Text("近 7 日阅读时长", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = "峰值 ${FlowFormatters.duration(maxReadTime)}",
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
                    Text("暂无阅读记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
