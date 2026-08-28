package com.neonroutine.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neonroutine.ui.theme.DesignStyle
import com.neonroutine.ui.theme.LocalAppShapes
import com.neonroutine.ui.theme.LocalDesignStyle
import com.neonroutine.ui.theme.glassPanel
import com.neonroutine.ui.viewmodel.TaskViewModel
import com.neonroutine.util.AutoSleepDetector
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// ── Palette consistent with NeonRoutine ───────────────────────────────────────
private val Neon  = Color(0xFF7F77DD)
private val Amber = Color(0xFFFFC107)
private val Flame = Color(0xFFFF6B35)
private val Emerald = Color(0xFF4CAF7D)
private val Sky   = Color(0xFF42A5F5)

@Composable
fun StatsScreen(viewModel: TaskViewModel) {
    val stats by viewModel.statsData.collectAsState()
    val appShapes = LocalAppShapes.current
    val designStyle = LocalDesignStyle.current
    val today = LocalDate.now()
    val currentMonthLength = today.lengthOfMonth()
    val todayDom = today.dayOfMonth

    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceMuted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // ── Section: Hero KPI row ─────────────────────────────────────────────
        SectionLabel("Overview", onSurface)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.LocalFireDepartment,
                iconColor = Flame,
                value = stats.streak.toString(),
                label = "Day\nStreak",
                designStyle = designStyle,
                appShapes = appShapes
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Star,
                iconColor = Amber,
                value = stats.perfectDays.toString(),
                label = "Perfect\nDays",
                designStyle = designStyle,
                appShapes = appShapes
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.CheckCircle,
                iconColor = Emerald,
                value = "${(stats.overallPercent * 100).roundToInt()}%",
                label = "Month\nScore",
                designStyle = designStyle,
                appShapes = appShapes
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.TrendingUp,
                iconColor = Sky,
                value = stats.totalDaysTracked.toString(),
                label = "Days\nTracked",
                designStyle = designStyle,
                appShapes = appShapes
            )
        }

        // ── Section: Monthly Calendar Heatmap ─────────────────────────────────
        SectionLabel("Monthly Heatmap", onSurface)
        StatCard(designStyle, appShapes) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    today.month.name.lowercase().replaceFirstChar { it.uppercase() } + " ${today.year}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )
                Spacer(Modifier.height(4.dp))
                MonthHeatmap(
                    today = today,
                    dailyPercents = stats.dailyPercents,
                    onSurfaceMuted = onSurfaceMuted
                )
                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("None", style = MaterialTheme.typography.labelSmall, color = onSurfaceMuted)
                    Spacer(Modifier.width(4.dp))
                    listOf(0.2f, 0.4f, 0.6f, 0.8f, 1f).forEach { pct ->
                        Box(
                            modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp))
                                .background(heatColor(pct))
                        )
                        Spacer(Modifier.width(2.dp))
                    }
                    Text("100%", style = MaterialTheme.typography.labelSmall, color = onSurfaceMuted)
                }
            }
        }

        // ── Section: Daily Trend Line Chart ───────────────────────────────────
        SectionLabel("Daily Trend — This Month", onSurface)
        StatCard(designStyle, appShapes) {
            Column(modifier = Modifier.padding(16.dp)) {
                val lineColor = MaterialTheme.colorScheme.primary
                val gradColor = lineColor.copy(alpha = 0.25f)

                // Animate all points
                val animProgress = remember { Animatable(0f) }
                LaunchedEffect(stats.dailyPercents) {
                    animProgress.animateTo(1f, tween(1000))
                }

                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp).padding(top = 8.dp, bottom = 4.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val plotUntil = todayDom.coerceAtMost(currentMonthLength)
                        if (plotUntil < 1) return@Canvas

                        // Horizontal grid lines at 25/50/75/100%
                        for (pct in listOf(0.25f, 0.5f, 0.75f, 1f)) {
                            val y = h - (pct * h)
                            drawLine(
                                color = lineColor.copy(alpha = 0.12f),
                                start = Offset(0f, y), end = Offset(w, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Collect points
                        val points = (1..plotUntil).mapNotNull { day ->
                            val pct = stats.dailyPercents[day] ?: return@mapNotNull null
                            val x = if (plotUntil > 1) (day - 1).toFloat() / (plotUntil - 1) * w else 0f
                            val y = h - (pct * h * animProgress.value)
                            Offset(x, y)
                        }
                        if (points.size < 2) {
                            points.firstOrNull()?.let {
                                drawCircle(lineColor, 6.dp.toPx(), it)
                            }
                            return@Canvas
                        }

                        // Cubic bezier path
                        val path = Path()
                        path.moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            val cpX = (prev.x + curr.x) / 2f
                            path.cubicTo(cpX, prev.y, cpX, curr.y, curr.x, curr.y)
                        }

                        // Gradient fill
                        val fillPath = Path().apply {
                            addPath(path)
                            lineTo(points.last().x, h)
                            lineTo(points.first().x, h)
                            close()
                        }
                        drawPath(
                            fillPath,
                            brush = Brush.verticalGradient(listOf(gradColor, Color.Transparent), 0f, h)
                        )

                        // Line stroke
                        drawPath(path, lineColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

                        // Dots on each data point
                        points.forEach { pt ->
                            drawCircle(lineColor, 4.5f.dp.toPx(), pt)
                            drawCircle(Color.White, 2.dp.toPx(), pt)
                        }
                    }
                }

                // X-axis labels
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("1", "8", "15", "22", today.lengthOfMonth().toString()).forEach { d ->
                        Text(d, style = MaterialTheme.typography.labelSmall, color = onSurfaceMuted)
                    }
                }

                // Y-axis legend
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    listOf("25%", "50%", "75%", "100%").forEach { label ->
                        Text(label, style = MaterialTheme.typography.labelSmall, color = onSurfaceMuted,
                            modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }

        // ── Section: Weekly Bar Chart ─────────────────────────────────────────
        SectionLabel("Weekly Performance", onSurface)
        StatCard(designStyle, appShapes) {
            Column(modifier = Modifier.padding(16.dp)) {
                val maxBarHeight = 140.dp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    stats.weeklyAvgs.forEachIndexed { weekIndex, avg ->
                        val animAvg by animateFloatAsState(avg, tween(900, delayMillis = weekIndex * 80), label = "w$weekIndex")
                        val barColor = when {
                            avg >= 0.8f -> listOf(Color(0xFF66BB6A), Emerald)
                            avg >= 0.5f -> listOf(Amber, Color(0xFFFF8F00))
                            avg > 0f    -> listOf(Color(0xFFFFB74D), Flame)
                            else        -> listOf(Color(0xFF333333), Color(0xFF222222))
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.width(44.dp)
                        ) {
                            Text(
                                "${(avg * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (avg > 0f) onSurface else onSurfaceMuted,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height((animAvg * maxBarHeight.value).coerceAtLeast(4f).dp)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(Brush.verticalGradient(barColor))
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "W${weekIndex + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = onSurfaceMuted
                            )
                        }
                    }
                    // Fill remaining weeks if fewer than 5
                    repeat((5 - stats.weeklyAvgs.size).coerceAtLeast(0)) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.width(44.dp)
                        ) {
                            Text("—", style = MaterialTheme.typography.labelSmall, color = onSurfaceMuted,
                                modifier = Modifier.padding(bottom = 4.dp))
                            Box(
                                modifier = Modifier.width(36.dp).height(4.dp)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(Color(0xFF2A2A2A))
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("W${stats.weeklyAvgs.size + it + 1}",
                                style = MaterialTheme.typography.labelSmall, color = onSurfaceMuted)
                        }
                    }
                }
            }
        }

        // ── Section: Per-Habit Completion Bars ───────────────────────────────
        if (stats.perTaskCompletion.isNotEmpty()) {
            SectionLabel("Habit Breakdown", onSurface)
            StatCard(designStyle, appShapes) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    stats.perTaskCompletion
                        .sortedByDescending { it.third }
                        .take(10)
                        .forEach { (title, colorHex, frac) ->
                            val animFrac by animateFloatAsState(frac, tween(900), label = title)
                            val taskColor = try {
                                Color(android.graphics.Color.parseColor(colorHex))
                            } catch (e: Exception) { Neon }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.size(10.dp).clip(CircleShape)
                                                .background(taskColor)
                                        )
                                        Text(
                                            title,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 180.dp)
                                        )
                                    }
                                    Text(
                                        "${(frac * 100).roundToInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = taskColor
                                    )
                                }
                                // Background track
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(7.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(taskColor.copy(alpha = 0.15f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(animFrac.coerceIn(0f, 1f))
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                Brush.horizontalGradient(listOf(taskColor.copy(0.7f), taskColor))
                                            )
                                    )
                                }
                            }
                        }
                }
            }
        }

        // ── Section: Sleep Tracker (Manual & Automatic) ──────────────────────────
        SleepTrackerCard(viewModel)

        Spacer(Modifier.height(24.dp))
    }
}

// ── Composable helpers ─────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(title: String, color: Color) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun StatCard(
    designStyle: DesignStyle,
    appShapes: com.neonroutine.ui.theme.AppShapes,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .then(
                if (designStyle == DesignStyle.GLASSMORPHISM) Modifier.glassPanel(shape = appShapes.card)
                else Modifier
            ),
        shape = appShapes.card,
        colors = CardDefaults.cardColors(
            containerColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.Transparent
                             else MaterialTheme.colorScheme.surface
        ),
        border = if (designStyle == DesignStyle.BRUTAL_MINIMAL)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                 else null,
        elevation = CardDefaults.cardElevation(
            if (designStyle == DesignStyle.GLASSMORPHISM || designStyle == DesignStyle.BRUTAL_MINIMAL) 0.dp
            else appShapes.cardElevation
        )
    ) { content() }
}

@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    value: String,
    label: String,
    designStyle: DesignStyle,
    appShapes: com.neonroutine.ui.theme.AppShapes
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onMuted   = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier.then(
            if (designStyle == DesignStyle.GLASSMORPHISM) Modifier.glassPanel(shape = appShapes.card)
            else Modifier
        ),
        shape = appShapes.card,
        colors = CardDefaults.cardColors(
            containerColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.Transparent
                             else MaterialTheme.colorScheme.surface
        ),
        border = if (designStyle == DesignStyle.BRUTAL_MINIMAL)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline) else null,
        elevation = CardDefaults.cardElevation(
            if (designStyle == DesignStyle.GLASSMORPHISM || designStyle == DesignStyle.BRUTAL_MINIMAL) 0.dp
            else appShapes.cardElevation
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(26.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = onSurface
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = onMuted,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun MonthHeatmap(
    today: LocalDate,
    dailyPercents: Map<Int, Float>,
    onSurfaceMuted: Color
) {
    val monthStart = today.withDayOfMonth(1)
    val daysInMonth = today.lengthOfMonth()
    val startDow = (monthStart.dayOfWeek.value - 1) % 7 // 0=Mon … 6=Sun

    val cellSize = 32.dp
    val cellGap = 4.dp
    val weekLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Column(verticalArrangement = Arrangement.spacedBy(cellGap)) {
        // Weekday headers
        Row(horizontalArrangement = Arrangement.spacedBy(cellGap)) {
            weekLabels.forEach { dayLabel ->
                Box(modifier = Modifier.size(cellSize), contentAlignment = Alignment.Center) {
                    Text(dayLabel, style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold, color = onSurfaceMuted, textAlign = TextAlign.Center)
                }
            }
        }

        // Build rows
        val totalCells = startDow + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(cellGap)) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayOfMonth = cellIndex - startDow + 1
                    if (dayOfMonth < 1 || dayOfMonth > daysInMonth) {
                        Box(modifier = Modifier.size(cellSize))
                    } else {
                        val pct = dailyPercents[dayOfMonth]
                        val isToday = dayOfMonth == today.dayOfMonth
                        val isFuture = dayOfMonth > today.dayOfMonth
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        isFuture -> Color(0xFF1A1A2E).copy(alpha = 0.4f)
                                        pct == null -> Color(0xFF2A2A3A).copy(alpha = 0.7f)
                                        else -> heatColor(pct)
                                    }
                                )
                                .then(
                                    if (isToday) Modifier.border(2.dp, Neon, RoundedCornerShape(6.dp))
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = when {
                                    isFuture -> onSurfaceMuted.copy(alpha = 0.4f)
                                    pct == null -> onSurfaceMuted.copy(alpha = 0.6f)
                                    pct > 0.5f -> Color.White
                                    else -> Color.White.copy(alpha = 0.75f)
                                },
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Maps 0..1 completion to a color from grey → amber → green */
private fun heatColor(pct: Float): Color = when {
    pct <= 0f   -> Color(0xFF2A2A3A)
    pct < 0.25f -> Color(0xFF4A3A2A)
    pct < 0.5f  -> Color(0xFFF57C00).copy(alpha = 0.75f)
    pct < 0.75f -> Color(0xFFFFC107).copy(alpha = 0.85f)
    pct < 1f    -> Color(0xFF66BB6A)
    else        -> Color(0xFF2E7D32)
}

// ── Sleep Tracker (Manual & Automatic Phone Inactivity Tracking) ──────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTrackerCard(viewModel: TaskViewModel) {
    val stats by viewModel.statsData.collectAsState()
    val context = LocalContext.current
    val designStyle = LocalDesignStyle.current
    val appShapes = LocalAppShapes.current
    val today = LocalDate.now()
    val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

    val appPrefs = remember { (context.applicationContext as com.neonroutine.NeonRoutineApp).appPreferences }
    val autoSleepEnabled by appPrefs.autoSleepEnabled.collectAsState()

    val onSurface = MaterialTheme.colorScheme.onSurface
    val onMuted   = MaterialTheme.colorScheme.onSurfaceVariant

    var showDialog by remember { mutableStateOf(false) }
    var editingSessionId by remember { mutableStateOf<String?>(null) }
    var dBedTime by remember { mutableStateOf("23:00") }
    var dWakeTime by remember { mutableStateOf("07:00") }
    var isAutoDetecting by remember { mutableStateOf(false) }

    val hasUsageAccess = remember(autoSleepEnabled) {
        AutoSleepDetector.hasUsageStatsPermission(context)
    }

    // Auto-detect on screen load if enabled and no session exists today
    LaunchedEffect(autoSleepEnabled) {
        if (autoSleepEnabled && hasUsageAccess && (stats.sleepDurations[todayStr] ?: 0f) == 0f) {
            viewModel.syncAutoDetectedSleep(today)
        }
    }

    fun showTimePicker(initTime: String, onTimeSet: (String) -> Unit) {
        val parts = initTime.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 7
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        android.app.TimePickerDialog(context, { _, hour, minute ->
            onTimeSet(String.format(java.util.Locale.ROOT, "%02d:%02d", hour, minute))
        }, h, m, true).show()
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingSessionId == null) "Add Sleep Log" else "Edit Sleep Log") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌙 Bed Time")
                        OutlinedButton(onClick = { showTimePicker(dBedTime) { dBedTime = it } }) {
                            Text(dBedTime)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("☀️ Wake Time")
                        OutlinedButton(onClick = { showTimePicker(dWakeTime) { dWakeTime = it } }) {
                            Text(dWakeTime)
                        }
                    }
                    // Preview duration
                    val pb = dBedTime.split(":")
                    val pw = dWakeTime.split(":")
                    val bTotal = (pb.getOrNull(0)?.toIntOrNull() ?: 23) * 60 + (pb.getOrNull(1)?.toIntOrNull() ?: 0)
                    val wTotal = (pw.getOrNull(0)?.toIntOrNull() ?: 7) * 60 + (pw.getOrNull(1)?.toIntOrNull() ?: 0)
                    var dur = wTotal - bTotal
                    if (dur < 0) dur += 1440
                    val hrs = dur / 60; val mins = dur % 60
                    Text(
                        "Duration: ${hrs}h ${mins}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hrs >= 7) Emerald else if (hrs >= 5) Amber else Flame,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val pb2 = dBedTime.split(":")
                    val pw2 = dWakeTime.split(":")
                    val bh = pb2.getOrNull(0)?.toIntOrNull() ?: 23
                    val bm = pb2.getOrNull(1)?.toIntOrNull() ?: 0
                    val wh = pw2.getOrNull(0)?.toIntOrNull() ?: 7
                    val wm = pw2.getOrNull(1)?.toIntOrNull() ?: 0
                    var dur = (wh * 60 + wm) - (bh * 60 + bm)
                    if (dur < 0) dur += 1440
                    if (editingSessionId == null) {
                        viewModel.addSleepSession(today, dBedTime, dWakeTime, dur)
                    } else {
                        viewModel.updateSleepSession(today, editingSessionId!!, dBedTime, dWakeTime, dur)
                    }
                    showDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }

    SectionLabel("Sleep & Recovery", onSurface)
    StatCard(designStyle, appShapes) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // Automatic tracking toggle strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.AutoMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            "Auto Phone Inactivity Sleep",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = onSurface
                        )
                        Text(
                            "Auto-detects sleep via phone screen time",
                            style = MaterialTheme.typography.labelSmall,
                            color = onMuted,
                            fontSize = 10.sp
                        )
                    }
                }
                Switch(
                    checked = autoSleepEnabled,
                    onCheckedChange = { enabled ->
                        appPrefs.setAutoSleepEnabled(enabled)
                        if (enabled && !hasUsageAccess) {
                            AutoSleepDetector.openUsageAccessSettings(context)
                        } else if (enabled) {
                            viewModel.syncAutoDetectedSleep(today)
                        }
                    },
                    modifier = Modifier.scale(0.85f)
                )
            }

            // Usage access permission alert if enabled but not granted
            if (autoSleepEnabled && !hasUsageAccess) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Flame.copy(alpha = 0.15f))
                        .border(1.dp, Flame.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = Flame, modifier = Modifier.size(16.dp))
                        Text(
                            "Usage Access required for automatic screen tracking",
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurface,
                            fontSize = 11.sp
                        )
                    }
                    Button(
                        onClick = { AutoSleepDetector.openUsageAccessSettings(context) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Grant", fontSize = 11.sp)
                    }
                }
            }

            // Header row: today's total + auto-sync / add buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val totalHours = stats.sleepDurations[todayStr] ?: 0f
                val sleepColor = when {
                    totalHours >= 7f -> Emerald
                    totalHours >= 5f -> Amber
                    totalHours > 0f  -> Flame
                    else             -> onMuted
                }
                Column {
                    Text("Tonight's Sleep", style = MaterialTheme.typography.bodySmall, color = onMuted)
                    Text(
                        if (totalHours > 0f) String.format(java.util.Locale.ROOT, "%.1f hrs", totalHours)
                        else "Not logged",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = sleepColor
                    )
                    if (totalHours > 0f) {
                        Text(
                            when {
                                totalHours >= 8f -> "Great sleep! 😴"
                                totalHours >= 7f -> "Good sleep ✅"
                                totalHours >= 5f -> "Short — aim for 7+ ⚠️"
                                else             -> "Low sleep — rest up 🔴"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = sleepColor.copy(alpha = 0.85f)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (autoSleepEnabled && hasUsageAccess) {
                        OutlinedButton(
                            onClick = {
                                isAutoDetecting = true
                                viewModel.syncAutoDetectedSleep(today)
                                isAutoDetecting = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Sync, contentDescription = "Auto-Sync", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Auto-Sync", fontSize = 12.sp)
                        }
                    }
                    Button(
                        onClick = { dBedTime = "23:00"; dWakeTime = "07:00"; editingSessionId = null; showDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) { Text("+ Manual", fontSize = 12.sp) }
                }
            }

            // Today's session list
            val todaySessions = stats.sleepSessions[todayStr] ?: emptyList()
            if (todaySessions.isNotEmpty()) {
                HorizontalDivider()
                todaySessions.forEach { session ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "${session.sleepTime} → ${session.wakeTime}",
                                fontWeight = FontWeight.SemiBold,
                                color = onSurface
                            )
                            Text(
                                "${session.durationMinutes / 60}h ${session.durationMinutes % 60}m",
                                style = MaterialTheme.typography.bodySmall, color = onMuted
                            )
                        }
                        Row {
                            IconButton(onClick = {
                                dBedTime = session.sleepTime; dWakeTime = session.wakeTime
                                editingSessionId = session.id; showDialog = true
                            }) {
                                Icon(Icons.Filled.Edit, "Edit", modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.removeSleepSession(today, session.id) }) {
                                Icon(Icons.Filled.Delete, "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // 7-day sleep bar chart
            HorizontalDivider()
            Text("7-Day Sleep Trend", style = MaterialTheme.typography.labelSmall, color = onMuted)
            Spacer(Modifier.height(4.dp))

            val limitDays = 7
            val weekData = (0 until limitDays).map { i ->
                val dStr = today.minusDays((limitDays - 1 - i).toLong())
                    .format(DateTimeFormatter.ISO_LOCAL_DATE)
                stats.sleepDurations[dStr] ?: 0f
            }
            val maxHrs = weekData.maxOrNull()?.coerceAtLeast(8f) ?: 8f

            // Sleep bar chart
            Row(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                val dayLabels = (0 until limitDays).map { i ->
                    today.minusDays((limitDays - 1 - i).toLong()).dayOfWeek.name.take(2)
                }
                weekData.forEachIndexed { idx, hrs ->
                    val animHrs by animateFloatAsState(hrs, tween(700, delayMillis = idx * 60), label = "sleep$idx")
                    val barColor = when {
                        hrs >= 7f -> Emerald
                        hrs >= 5f -> Amber
                        hrs > 0f  -> Flame
                        else      -> Color(0xFF2A2A3A)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (hrs > 0f) {
                            Text(
                                "${hrs.roundToInt()}h",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = barColor,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                        Box(
                            modifier = Modifier.width(28.dp)
                                .height(((animHrs / maxHrs) * 60).coerceAtLeast(if (hrs > 0f) 4f else 2f).dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(barColor)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            dayLabels[idx],
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = onMuted
                        )
                    }
                }
            }
        }
    }
}
