package com.neonroutine.ui.week

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeekScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val entriesMap by viewModel.entriesForDate.collectAsState()

    // Calculate week start (Monday)
    var weekStart by remember { mutableStateOf(selectedDate.with(DayOfWeek.MONDAY)) }
    val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }

    LaunchedEffect(selectedDate) {
        weekStart = selectedDate.with(DayOfWeek.MONDAY)
    }

    val scheduledTasks = tasks.filter { viewModel.isTaskScheduledForDate(it, selectedDate) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Week navigation
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { 
                val newWeek = weekStart.minusWeeks(1)
                weekStart = newWeek
                viewModel.selectDate(newWeek) 
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous week")
            }
            Text(
                "${weekStart.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${weekStart.year}",
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = { 
                val newWeek = weekStart.plusWeeks(1)
                weekStart = newWeek
                viewModel.selectDate(newWeek) 
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next week")
            }
        }

        // 7-day strip
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            weekDays.forEach { day ->
                val isSelected = day == selectedDate
                val isToday = day == LocalDate.now()
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                else -> Color.Transparent
                            }
                        )
                        .clickable { viewModel.selectDate(day) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isToday && !isSelected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                                else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${day.dayOfMonth}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Tasks for selected day
        if (scheduledTasks.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No tasks scheduled", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("for ${selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp).weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(scheduledTasks, key = { it.id }) { task ->
                    val entry = entriesMap[task.id]
                    val state = entry?.completionState ?: CompletionState.NONE
                    val taskColor = try { Color(android.graphics.Color.parseColor(task.color)) } catch (_: Exception) { Color(0xFF7F77DD) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(taskColor)
                            )
                            androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
                            Text(task.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            val dateStr = selectedDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                            CycleStateButton(state = state, color = taskColor) {
                                viewModel.cycleGridState(task.id, dateStr, state)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
