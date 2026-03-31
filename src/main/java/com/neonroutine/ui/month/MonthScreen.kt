package com.neonroutine.ui.month

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neonroutine.data.model.CompletionState
import com.neonroutine.ui.home.CycleStateButton
import com.neonroutine.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val entriesMap by viewModel.entriesForDate.collectAsState()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // Completion data for the month
    val completionMap = remember { mutableStateMapOf<LocalDate, Float>() }
    LaunchedEffect(currentMonth, tasks) {
        val start = currentMonth.atDay(1)
        val end = currentMonth.atEndOfMonth()
        val data = viewModel.getCompletionForDateRange(start, end)
        completionMap.clear()
        completionMap.putAll(data)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Month navigation
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous month")
            }
            Text(
                "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next month")
            }
        }

        // Day of week headers
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { d ->
                Text(
                    d, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Calendar grid
        val firstDay = currentMonth.atDay(1)
        val startOffset = (firstDay.dayOfWeek.value - 1) // Mon=0
        val daysInMonth = currentMonth.lengthOfMonth()
        val totalCells = startOffset + daysInMonth
        val today = LocalDate.now()

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Empty cells for offset
            items(startOffset) { Box(Modifier.aspectRatio(1f)) }

            items(daysInMonth) { index ->
                val day = currentMonth.atDay(index + 1)
                val isToday = day == today
                val completion = completionMap[day]

                val bgColor = when {
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    completion != null && completion >= 1f -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                    completion != null && completion > 0f -> Color(0xFFFFC107).copy(alpha = 0.2f)
                    else -> Color.Transparent
                }

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .clickable {
                            selectedDay = day
                            viewModel.selectDate(day)
                            showBottomSheet = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                        // Completion dot
                        if (completion != null && completion > 0f) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (completion >= 1f) Color(0xFF4CAF50) else Color(0xFFFFC107)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }

    // Bottom sheet for selected day
    if (showBottomSheet && selectedDay != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
                selectedDay = null
            },
            sheetState = sheetState
        ) {
            val day = selectedDay!!
            val scheduled = tasks.filter { viewModel.isTaskScheduledForDate(it, day) }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    day.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.FULL)),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(12.dp))
                if (scheduled.isEmpty()) {
                    Text("No tasks for this day", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(scheduled, key = { it.id }) { task ->
                            val entry = entriesMap[task.id]
                            val state = entry?.completionState ?: CompletionState.NONE
                            val taskColor = try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(task.color)) } catch (_: Exception) { androidx.compose.ui.graphics.Color(0xFF7F77DD) }
                            Card(modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(task.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    val dateStr = day.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                                    CycleStateButton(state = state, color = taskColor) {
                                        viewModel.cycleGridState(task.id, dateStr, state)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
