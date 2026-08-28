package com.neonroutine.ui.grid

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neonroutine.data.model.CompletionState
import com.neonroutine.data.model.HabitCategory
import com.neonroutine.data.model.Task
import com.neonroutine.ui.viewmodel.TaskViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

import com.neonroutine.ui.theme.LocalDesignStyle
import com.neonroutine.ui.theme.DesignStyle
import com.neonroutine.ui.theme.LocalAppShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridViewScreen(viewModel: TaskViewModel) {
    val designStyle = LocalDesignStyle.current
    val tasks by viewModel.tasks.collectAsState()
    val entriesRange by viewModel.entriesInRange.collectAsState()

    val today = LocalDate.now()
    var gridMonth by remember { mutableStateOf(YearMonth.now()) }
    val yearMonth = gridMonth
    val daysInMonth = yearMonth.lengthOfMonth()
    val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val monthStart = yearMonth.atDay(1)
    val monthEnd = yearMonth.atEndOfMonth()
    val isCurrentMonth = yearMonth == YearMonth.now()
    // For past months, evaluate all days; for current month, only up to today
    val evaluateTo: LocalDate = if (isCurrentMonth) today else if (yearMonth.isBefore(YearMonth.now())) monthEnd else monthStart.minusDays(1)

    // Load entries whenever displayed month changes
    androidx.compose.runtime.LaunchedEffect(yearMonth) {
        viewModel.loadEntriesForRange(monthStart, monthEnd)
    }

    val entryMap = remember(entriesRange) {
        entriesRange.groupBy { it.taskId }.mapValues { (_, list) -> list.associateBy { it.date } }
    }

    // Category filter
    var selectedCategory by remember { mutableStateOf<HabitCategory?>(null) }
    val filteredTasks = if (selectedCategory == null) tasks else tasks.filter { it.category == selectedCategory }

    Column(modifier = Modifier.fillMaxSize()) {
        // Month header with navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { gridMonth = gridMonth.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Month",
                    tint = MaterialTheme.colorScheme.onSurface)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    monthName.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${yearMonth.year} • ${tasks.size} Habits",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isCurrentMonth) {
                    androidx.compose.material3.TextButton(
                        onClick = { gridMonth = YearMonth.now() },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) { Text("Now", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary) }
                }
                IconButton(onClick = { gridMonth = gridMonth.plusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month",
                        tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendDot(Color(0xFF4CAF50), "Done")
                LegendDot(Color(0xFFFF9800), "Partial")
                LegendDot(Color(0xFFF44336), "Missed")
            }
        }

        // Category filter chips
        val scrollState0 = rememberScrollState()
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState0)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = selectedCategory == null, onClick = { selectedCategory = null }, label = { Text("All") })
            HabitCategory.entries.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                    label = { Text("${cat.emoji} ${cat.label}") }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // The 2D Grid Surface (Unified Horizontal Scroll + Lazy Vertical Scroll)
        val gridHorizontalScroll = rememberScrollState()
        val CELL_SIZE = 28.dp
        val LABEL_WIDTH = 120.dp

        val totalWidth = LABEL_WIDTH + (CELL_SIZE + 4.dp) * daysInMonth

        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(gridHorizontalScroll)
        ) {
            LazyColumn(modifier = Modifier
                .width(totalWidth)
                .fillMaxHeight()
            ) {
                // Week headers row
                item {
                Row(modifier = Modifier.padding(start = LABEL_WIDTH)) {
                    val weekWidth = (CELL_SIZE + 4.dp) * 7
                    (0..4).forEach { w ->
                        val startDay = w * 7 + 1
                        if (startDay <= daysInMonth) {
                            val endDay = minOf(startDay + 6, daysInMonth)
                            val width = (CELL_SIZE + 4.dp) * (endDay - startDay + 1)
                            Box(
                                modifier = Modifier
                                    .width(width)
                                    .padding(horizontal = 2.dp, vertical = 2.dp)
                                    .background(
                                        if (designStyle == DesignStyle.GLASSMORPHISM) Color.White.copy(alpha=0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), 
                                        RoundedCornerShape(4.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Week ${w + 1}", 
                                    style = MaterialTheme.typography.labelSmall, 
                                    fontSize = 9.sp, 
                                    color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White else MaterialTheme.colorScheme.primary, 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Day headers row
            item {
                Row(modifier = Modifier.padding(start = LABEL_WIDTH)) {
                    (1..daysInMonth).forEach { day ->
                        val date = yearMonth.atDay(day)
                        val isToday = date == today
                        val dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3)
                        Column(
                            modifier = Modifier.width(CELL_SIZE + 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$day",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = if (isToday) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // Group by category
            val grouped = filteredTasks.groupBy { it.category }
            HabitCategory.entries.forEach { cat ->
                val catTasks = grouped[cat] ?: return@forEach
                item(key = "cat_${cat.name}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${cat.emoji} ${cat.label.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                items(catTasks, key = { it.id }, contentType = { "grid_row" }) { task ->
                    val taskColor = try { Color(android.graphics.Color.parseColor(task.color)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                    val taskEntries = entryMap[task.id] ?: emptyMap()

                    // Compute completion % only for days up to evaluateTo in the viewed month
                    val scheduledDays = (1..daysInMonth).count { d ->
                        val date = yearMonth.atDay(d)
                        !date.isAfter(evaluateTo) && viewModel.isTaskScheduledForDate(task, date)
                    }
                    val completedDays = taskEntries.values.count { it.completionState == CompletionState.COMPLETED }
                    val completionPct = if (scheduledDays > 0) ((completedDays.toFloat() / scheduledDays) * 100).toInt() else 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Habit name label
                        Row(
                            modifier = Modifier.width(LABEL_WIDTH).padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(taskColor)
                            )
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    task.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "$completionPct%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                    // Day cells row
                    Row {
                        (1..daysInMonth).forEach { day ->
                            val date = yearMonth.atDay(day)
                            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            val entry = taskEntries[dateStr]
                            val state = entry?.completionState ?: CompletionState.NONE
                            val isFuture = date.isAfter(today)

                            GridCell(
                                state = state,
                                isFuture = isFuture,
                                isToday = date == today,
                                taskColor = taskColor,
                                cellSize = CELL_SIZE,
                                onClick = {
                                    if (!isFuture) viewModel.cycleGridState(task.id, dateStr, state)
                                }
                            )
                        }
                    }
                    }
                }
            }

            // Bottom Summary Rows
            item(key = "summary_row") {
                Spacer(Modifier.height(16.dp))
                // Row 1: DAILY COMPLETION %
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "DAILY COMPLETION",
                        modifier = Modifier.width(LABEL_WIDTH).padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row {
                        (1..daysInMonth).forEach { day ->
                            val date = yearMonth.atDay(day)
                            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            val scheduledForDay = filteredTasks.filter { viewModel.isTaskScheduledForDate(it, date) }
                            val total = scheduledForDay.size
                            val completed = scheduledForDay.count { task ->
                                val entry = entryMap[task.id]?.get(dateStr)
                                entry?.completionState == CompletionState.COMPLETED
                            }
                            val pct = if (total > 0) ((completed.toFloat() / total) * 100).toInt() else 0

                            Box(modifier = Modifier.width(CELL_SIZE + 4.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    if (date.isAfter(today)) "-" else "$pct%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Row 2: Daily Completed
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Daily Completed",
                        modifier = Modifier.width(LABEL_WIDTH).padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        (1..daysInMonth).forEach { day ->
                            val date = yearMonth.atDay(day)
                            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            val scheduledForDay = filteredTasks.filter { viewModel.isTaskScheduledForDate(it, date) }
                            val completed = scheduledForDay.count { task ->
                                val entry = entryMap[task.id]?.get(dateStr)
                                entry?.completionState == CompletionState.COMPLETED
                            }

                            Box(modifier = Modifier.width(CELL_SIZE + 4.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    if (date.isAfter(today)) "-" else "$completed",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Row 3: Daily Total
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Daily Total",
                        modifier = Modifier.width(LABEL_WIDTH).padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        (1..daysInMonth).forEach { day ->
                            val date = yearMonth.atDay(day)
                            val scheduledForDay = filteredTasks.filter { viewModel.isTaskScheduledForDate(it, date) }
                            val total = scheduledForDay.size

                            Box(modifier = Modifier.width(CELL_SIZE + 4.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    "$total",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}
}

@Composable
fun GridCell(
    state: CompletionState,
    isFuture: Boolean,
    isToday: Boolean,
    taskColor: Color,
    cellSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = when {
            isFuture -> Color.Transparent
            state == CompletionState.COMPLETED -> Color(0xFF4CAF50).copy(alpha = 0.85f)
            state == CompletionState.PARTIAL -> Color(0xFFFF9800).copy(alpha = 0.85f)
            state == CompletionState.MISSED -> Color(0xFFF44336).copy(alpha = 0.85f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        animationSpec = tween(200),
        label = "cellBg"
    )

    // Two-phase pop: compress on tap → spring bounce back to full size
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.72f
            state != CompletionState.NONE && !isFuture -> 1f
            else -> 0.82f
        },
        animationSpec = if (pressed)
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
        else
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "cellScale"
    )

    Box(
        modifier = Modifier
            .padding(2.dp)
            .size(cellSize)
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .then(if (state == CompletionState.NONE && !isFuture) Modifier.border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(6.dp)) else Modifier)
            .then(if (isToday && !isFuture && state == CompletionState.NONE) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)) else Modifier)
            .alpha(if (isFuture) 0.2f else 1f)
            .clickable(enabled = !isFuture) {
                pressed = true
                onClick()
                // Release the press state slightly after so the spring fires
                pressed = false
            },
        contentAlignment = Alignment.Center
    ) {
        if (state == CompletionState.COMPLETED) {
            androidx.compose.material3.Icon(
                Icons.Filled.Check,
                contentDescription = "Done",
                tint = Color.White,
                modifier = Modifier.size(cellSize * 0.75f)
            )
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
