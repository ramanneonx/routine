package com.neonroutine.ui.month

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.neonroutine.data.model.CompletionState
import com.neonroutine.data.model.Entry
import com.neonroutine.data.model.Task
import com.neonroutine.ui.home.CycleStateButton
import com.neonroutine.ui.theme.DesignStyle
import com.neonroutine.ui.theme.LocalAppShapes
import com.neonroutine.ui.theme.LocalDesignStyle
import com.neonroutine.ui.theme.glassPanel
import com.neonroutine.ui.viewmodel.TaskViewModel
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.roundToInt

// ── Palette ──────────────────────────────────────────────────────────────────
private val Neon    = Color(0xFF7F77DD)
private val Emerald = Color(0xFF4CAF50)
private val Amber   = Color(0xFFFFC107)
private val Flame   = Color(0xFFFF6B35)
private val Sky     = Color(0xFF42A5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val stats by viewModel.statsData.collectAsState()
    val appShapes = LocalAppShapes.current
    val designStyle = LocalDesignStyle.current
    val context = LocalContext.current

    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceMuted = MaterialTheme.colorScheme.onSurfaceVariant

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDay by remember { mutableStateOf(LocalDate.now()) }
    var showMonthPicker by remember { mutableStateOf(false) }

    // Synchronize ViewModel active month for stats querying across history
    LaunchedEffect(currentMonth) {
        viewModel.setSelectedMonth(currentMonth)
        val start = currentMonth.atDay(1)
        val end = currentMonth.atEndOfMonth()
        viewModel.loadEntriesForRange(start, end)
    }

    val entriesMap by viewModel.entriesInRange.collectAsState()

    // Group entries for current month by date
    val entriesByDate = remember(entriesMap) {
        entriesMap.groupBy { it.date }
    }

    // Compute completion map for each day of the current month
    val completionMap = remember(tasks, entriesByDate, currentMonth) {
        val daysInMonth = currentMonth.lengthOfMonth()
        val result = mutableMapOf<LocalDate, Float>()
        for (dom in 1..daysInMonth) {
            val date = currentMonth.atDay(dom)
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val scheduled = tasks.filter { viewModel.isTaskScheduledForDate(it, date) }
            if (scheduled.isNotEmpty()) {
                val dayEntries = (entriesByDate[dateStr] ?: emptyList()).associateBy { it.taskId }
                val sum = scheduled.sumOf { task ->
                    viewModel.calculateCompletionForEntry(task, dayEntries[task.id]).toDouble()
                }
                result[date] = (sum / scheduled.size).toFloat()
            }
        }
        result
    }

    // Month stats calculations
    val totalScheduledDays = completionMap.size
    val perfectDaysCount = completionMap.values.count { it >= 1.0f }
    val monthAvgPercent = if (completionMap.isNotEmpty()) completionMap.values.average().toFloat() else 0f
    val isViewingCurrentMonth = currentMonth == YearMonth.now()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // ── 1. Month & Year Navigator Header ─────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (designStyle == DesignStyle.GLASSMORPHISM) Modifier.glassPanel(shape = appShapes.card)
                        else Modifier
                    )
                    .clip(appShapes.card)
                    .background(
                        if (designStyle == DesignStyle.GLASSMORPHISM) Color.Transparent
                        else MaterialTheme.colorScheme.surface
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    currentMonth = currentMonth.minusMonths(1)
                    selectedDay = currentMonth.atDay(1)
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month", tint = onSurface)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showMonthPicker = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${currentMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onSurface
                    )
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = "Pick Month",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isViewingCurrentMonth) {
                        TextButton(
                            onClick = {
                                currentMonth = YearMonth.now()
                                selectedDay = LocalDate.now()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Today", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = {
                        currentMonth = currentMonth.plusMonths(1)
                        selectedDay = currentMonth.atDay(1)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month", tint = onSurface)
                    }
                }
            }
        }

        // ── 2. Monthly KPI Overview Strip ────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MonthKpiMiniCard(
                    modifier = Modifier.weight(1f),
                    title = "Avg Score",
                    value = "${(monthAvgPercent * 100).roundToInt()}%",
                    color = if (monthAvgPercent >= 0.75f) Emerald else if (monthAvgPercent >= 0.5f) Amber else Sky,
                    designStyle = designStyle,
                    appShapes = appShapes
                )
                MonthKpiMiniCard(
                    modifier = Modifier.weight(1f),
                    title = "Perfect Days",
                    value = "$perfectDaysCount",
                    color = Amber,
                    designStyle = designStyle,
                    appShapes = appShapes
                )
                MonthKpiMiniCard(
                    modifier = Modifier.weight(1f),
                    title = "Days Active",
                    value = "$totalScheduledDays",
                    color = Flame,
                    designStyle = designStyle,
                    appShapes = appShapes
                )
            }
        }

        // ── 3. High-Definition Calendar Heatmap Grid ──────────────────────────
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (designStyle == DesignStyle.GLASSMORPHISM) Modifier.glassPanel(shape = appShapes.card)
                        else Modifier
                    ),
                shape = appShapes.card,
                colors = CardDefaults.cardColors(
                    containerColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.Transparent
                    else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(if (designStyle == DesignStyle.GLASSMORPHISM) 0.dp else 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    // Weekday headers
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { d ->
                            Text(
                                d,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = onSurfaceMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Calendar Days Grid
                    val firstDay = currentMonth.atDay(1)
                    val startOffset = (firstDay.dayOfWeek.value - 1) // 0=Mon .. 6=Sun
                    val daysInMonth = currentMonth.lengthOfMonth()
                    val totalCells = startOffset + daysInMonth
                    val rows = (totalCells + 6) / 7
                    val today = LocalDate.now()

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (r in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (c in 0..6) {
                                    val cellIndex = r * 7 + c
                                    val dayNum = cellIndex - startOffset + 1
                                    if (dayNum in 1..daysInMonth) {
                                        val day = currentMonth.atDay(dayNum)
                                        val isToday = day == today
                                        val isSelected = day == selectedDay
                                        val pct = completionMap[day]
                                        val hasPhoto = (entriesByDate[day.format(DateTimeFormatter.ISO_LOCAL_DATE)] ?: emptyList())
                                            .any { !it.photoPath.isNullOrBlank() }

                                        val cellColor = when {
                                            pct == null || pct <= 0f -> Color(0xFF2A2A3A).copy(alpha = 0.4f)
                                            pct >= 1f                -> Emerald.copy(alpha = 0.85f)
                                            pct >= 0.75f             -> Emerald.copy(alpha = 0.55f)
                                            pct >= 0.5f              -> Amber.copy(alpha = 0.70f)
                                            else                     -> Flame.copy(alpha = 0.60f)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(cellColor)
                                                .then(
                                                    if (isSelected) Modifier.border(2.dp, Neon, RoundedCornerShape(8.dp))
                                                    else if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                                    else Modifier
                                                )
                                                .clickable {
                                                    selectedDay = day
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "$dayNum",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isToday || isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                                    color = if (pct != null && pct > 0.4f) Color.White else onSurface
                                                )
                                                if (hasPhoto) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(Sky)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Legend Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Sky))
                            Text("Photo Logged", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = onSurfaceMuted)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("0%", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = onSurfaceMuted)
                            listOf(Color(0xFF2A2A3A), Flame.copy(0.6f), Amber.copy(0.7f), Emerald.copy(0.55f), Emerald.copy(0.85f)).forEach { c ->
                                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(c))
                            }
                            Text("100%", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = onSurfaceMuted)
                        }
                    }
                }
            }
        }

        // ── 4. Selected Day Detail Drawer ─────────────────────────────────────
        val selectedDateStr = selectedDay.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dayScheduledTasks = tasks.filter { viewModel.isTaskScheduledForDate(it, selectedDay) }
        val dayEntriesMap = (entriesByDate[selectedDateStr] ?: emptyList()).associateBy { it.taskId }
        val dayPct = completionMap[selectedDay] ?: 0f

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = selectedDay.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = onSurface
                    )
                    Text(
                        text = "${dayScheduledTasks.size} habits scheduled • ${(dayPct * 100).roundToInt()}% completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (dayPct >= 1f) Emerald.copy(alpha = 0.2f)
                            else if (dayPct >= 0.5f) Amber.copy(alpha = 0.2f)
                            else Flame.copy(alpha = 0.2f)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${(dayPct * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (dayPct >= 1f) Emerald else if (dayPct >= 0.5f) Amber else Flame
                    )
                }
            }
        }

        if (dayScheduledTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = appShapes.card,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No habits were scheduled for this day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = onSurfaceMuted
                        )
                    }
                }
            }
        } else {
            items(dayScheduledTasks, key = { "month_day_${it.id}_${selectedDateStr}" }) { task ->
                val entry = dayEntriesMap[task.id]
                val state = entry?.completionState ?: CompletionState.NONE
                val taskColor = try {
                    Color(android.graphics.Color.parseColor(task.color))
                } catch (_: Exception) { MaterialTheme.colorScheme.primary }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (designStyle == DesignStyle.GLASSMORPHISM) Modifier.glassPanel(shape = RoundedCornerShape(12.dp))
                            else Modifier
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.Transparent
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(if (designStyle == DesignStyle.GLASSMORPHISM) 0.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(taskColor)
                            )
                            Column {
                                Text(
                                    task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "+${task.pointsValue} pts • ${task.category.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = onSurfaceMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Photo preview thumbnail if logged
                        if (!entry?.photoPath.isNullOrBlank() && File(entry!!.photoPath!!).exists()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(entry.photoPath!!))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Selfie",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(0.4f), RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(10.dp))
                        }

                        // Interactive completion cycle button
                        CycleStateButton(state = state, color = taskColor) {
                            viewModel.cycleGridState(task.id, selectedDateStr, state)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }

    // ── Quick Month / Year Picker Dialog ─────────────────────────────────────
    if (showMonthPicker) {
        var pickerYear by remember { mutableIntStateOf(currentMonth.year) }
        AlertDialog(
            onDismissRequest = { showMonthPicker = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { pickerYear-- }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Year")
                    }
                    Text("$pickerYear", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { pickerYear++ }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Year")
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val months = listOf(
                        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                    )
                    for (row in 0..3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (col in 0..2) {
                                val mIdx = row * 3 + col
                                val isSelected = currentMonth.year == pickerYear && currentMonth.monthValue == mIdx + 1
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .clickable {
                                            currentMonth = YearMonth.of(pickerYear, mIdx + 1)
                                            selectedDay = currentMonth.atDay(1)
                                            showMonthPicker = false
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        months[mIdx],
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMonthPicker = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun MonthKpiMiniCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color,
    designStyle: DesignStyle,
    appShapes: com.neonroutine.ui.theme.AppShapes
) {
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
        elevation = CardDefaults.cardElevation(if (designStyle == DesignStyle.GLASSMORPHISM) 0.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = color)
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}
