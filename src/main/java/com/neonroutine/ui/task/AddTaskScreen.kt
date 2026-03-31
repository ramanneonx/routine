package com.neonroutine.ui.task

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TextButton
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.neonroutine.data.model.ColumnDef
import com.neonroutine.data.model.ColumnType
import com.neonroutine.data.model.HabitCategory
import com.neonroutine.data.model.Recurrence
import com.neonroutine.data.model.Task
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

val iconOptions: List<Pair<String, ImageVector>> = listOf(
    "default" to Icons.Filled.Star,
    "exercise" to Icons.Filled.FitnessCenter,
    "study" to Icons.Filled.School,
    "work" to Icons.Filled.Work,
    "read" to Icons.AutoMirrored.Filled.MenuBook,
    "music" to Icons.Filled.MusicNote,
    "meditation" to Icons.Filled.SelfImprovement,
    "water" to Icons.Filled.WaterDrop
)

fun getIconForKey(key: String): ImageVector {
    return iconOptions.find { it.first == key }?.second ?: Icons.Filled.Star
}

val colorOptions = listOf(
    "#7F77DD", "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4",
    "#FFEAA7", "#DDA0DD", "#FF8C42", "#98D8C8", "#F7DC6F",
    "#BB8FCE", "#85C1E9", "#F0B27A", "#82E0AA"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    onBack: () -> Unit = {},
    onSave: (Task) -> Unit = {},
    existingTask: Task? = null
) {
    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var selectedColor by remember { mutableStateOf(existingTask?.color ?: colorOptions[0]) }
    var selectedIcon by remember { mutableStateOf(existingTask?.iconKey ?: "default") }
    var recurrence by remember { mutableStateOf(existingTask?.recurrence ?: Recurrence.DAILY) }
    var selectedCategory by remember { mutableStateOf(existingTask?.category ?: HabitCategory.HEALTH) }
    var selectedDays by remember {
        mutableStateOf(
            if (existingTask != null) {
                try { Json.decodeFromString<List<Int>>(existingTask.recurrenceDays).toSet() }
                catch (_: Exception) { emptySet() }
            } else emptySet()
        )
    }
    var intervalDays by remember {
        mutableIntStateOf(
            if (existingTask != null && existingTask.recurrence == Recurrence.INTERVAL) {
                try { Json.decodeFromString<List<Int>>(existingTask.recurrenceDays).firstOrNull() ?: 2 } catch (_: Exception) { 2 }
            } else 2
        )
    }
    val columns = remember {
        mutableStateListOf<ColumnDef>().apply {
            if (existingTask != null) {
                try {
                    addAll(Json.decodeFromString<List<ColumnDef>>(existingTask.columnsJson))
                } catch (_: Exception) { }
            }
            if (isEmpty()) {
                add(ColumnDef(UUID.randomUUID().toString(), "Done", ColumnType.TICK))
            }
        }
    }
    
    val reminders = remember {
        mutableStateListOf<String>().apply {
            if (existingTask != null) {
                try {
                    addAll(Json.decodeFromString<List<String>>(existingTask.remindersJson))
                } catch (_: Exception) { }
            }
        }
    }
    
    val timers = remember {
        mutableStateListOf<String>().apply {
            if (existingTask != null) {
                try {
                    addAll(Json.decodeFromString<List<String>>(existingTask.timersJson))
                } catch (_: Exception) { }
            }
        }
    }
    
    var showTimePicker by remember { mutableStateOf(false) }
    var reminderMsgEdit by remember { mutableStateOf("") }
    val timePickerState = rememberTimePickerState()
    
    var showTimerDialog by remember { mutableStateOf(false) }
    var timerMinsEdit by remember { mutableStateOf("25") }
    var timerLabelEdit by remember { mutableStateOf("Focus") }
    
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingTask != null) "Edit Task" else "New Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                    try {
                                        val task = Task(
                                            id = existingTask?.id ?: UUID.randomUUID().toString(),
                                            title = title,
                                            color = selectedColor,
                                            iconKey = selectedIcon,
                                            category = selectedCategory,
                                            recurrence = recurrence,
                                            recurrenceDays = if (recurrence == Recurrence.INTERVAL) {
                                                Json.encodeToString<List<Int>>(listOf(if (intervalDays <= 0) 1 else intervalDays))
                                            } else {
                                                Json.encodeToString<List<Int>>(selectedDays.toList())
                                            },
                                            startDate = existingTask?.startDate ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                                            endDate = existingTask?.endDate,
                                            columnsJson = Json.encodeToString<List<ColumnDef>>(columns.toList()),
                                            remindersJson = Json.encodeToString<List<String>>(reminders.toList()),
                                            timersJson = Json.encodeToString<List<String>>(timers.toList()),
                                            updatedAt = System.currentTimeMillis()
                                        )
                                        onSave(task)
                                        onBack()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error saving task: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Task Name
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Color Picker
            Text("Color", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colorOptions.forEach { hex ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    val isSelected = selectedColor == hex
                    val size by animateDpAsState(targetValue = if (isSelected) 48.dp else 40.dp, animationSpec = tween(300), label = "colorSize")
                    
                    Box(
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            // Icon Picker
            Text("Icon", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                iconOptions.forEach { (key, icon) ->
                    val isSelected = selectedIcon == key
                    val size by animateDpAsState(targetValue = if (isSelected) 56.dp else 48.dp, animationSpec = tween(300), label = "iconSize")
                    Box(
                        modifier = Modifier
                            .size(size)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedIcon = key },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon, contentDescription = key,
                            modifier = Modifier.size(if (isSelected) 28.dp else 24.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Category Picker
            Text("Category", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HabitCategory.entries.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text("${cat.emoji} ${cat.label}") }
                    )
                }
            }

            // Recurrence
            Text("Recurrence", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Recurrence.entries.forEach { rec ->
                    FilterChip(
                        selected = recurrence == rec,
                        onClick = { recurrence = rec },
                        label = { Text(rec.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Custom days selector
            AnimatedVisibility(visible = recurrence == Recurrence.CUSTOM) {
                Column {
                    Text("Select Days", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        dayNames.forEachIndexed { index, name ->
                            val dayNum = index + 1
                            FilterChip(
                                selected = dayNum in selectedDays,
                                onClick = {
                                    selectedDays = if (dayNum in selectedDays)
                                        selectedDays - dayNum else selectedDays + dayNum
                                },
                                label = { Text(name) }
                            )
                        }
                    }
                }
            }

            // Specific interval selector
            AnimatedVisibility(visible = recurrence == Recurrence.INTERVAL) {
                Column {
                    Text("Repeat Interval", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Every ")
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = if (intervalDays == 0) "" else intervalDays.toString(),
                            onValueChange = { newVal ->
                                val parsed = newVal.filter { it.isDigit() }.toIntOrNull()
                                if (parsed != null && parsed > 0) intervalDays = parsed
                                else if (newVal.isEmpty()) intervalDays = 0
                            },
                            singleLine = true,
                            modifier = Modifier.width(60.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (intervalDays == 1) "day" else "days")
                    }
                    if (intervalDays == 2) {
                        Text("This means alternate days.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                    } else if (intervalDays == 3) {
                        Text("This means skipping two days.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            // Reminders Section
            Text("Reminders", style = MaterialTheme.typography.titleSmall)
            Text("Add exact times for the app to send push notifications",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
                
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                reminders.forEachIndexed { index, item ->
                    val parts = item.split("|")
                    val timeStr = parts.getOrNull(0) ?: item
                    val msg = parts.getOrNull(1) ?: "Reminder"
                    InputChip(
                        selected = false,
                        onClick = { },
                        label = { Text("$timeStr ($msg)") },
                        trailingIcon = {
                            Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp).clickable { reminders.removeAt(index) })
                        }
                    )
                }
                if (reminders.size < 10) {
                    FilterChip(
                        selected = false,
                        onClick = { 
                            reminderMsgEdit = ""
                            showTimePicker = true 
                        },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = "Add Time", modifier = Modifier.size(16.dp)) },
                        label = { Text("Add Time") }
                    )
                }
            }

            if (showTimePicker) {
                BasicAlertDialog(onDismissRequest = { showTimePicker = false }) {
                    Card {
                        Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Select Reminder Time", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(16.dp))
                            TimePicker(state = timePickerState)
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = reminderMsgEdit,
                                onValueChange = { reminderMsgEdit = it },
                                label = { Text("Custom Message (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                                TextButton(onClick = {
                                    val hr = timePickerState.hour.toString().padStart(2, '0')
                                    val min = timePickerState.minute.toString().padStart(2, '0')
                                    val cleanMsg = reminderMsgEdit.replace("|", " ").trim()
                                    val finalStr = if (cleanMsg.isNotBlank()) "$hr:$min|$cleanMsg" else "$hr:$min"
                                    reminders.add(finalStr)
                                    showTimePicker = false
                                }) { Text("OK") }
                            }
                        }
                    }
                }
            }

            // Timers Section
            Text("Inbuilt Timers", style = MaterialTheme.typography.titleSmall)
            Text("Add dedicated countdown timers for focused work (e.g. 25 min Study)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
                
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                timers.forEachIndexed { index, item ->
                    val parts = item.split("|")
                    val mins = parts.getOrNull(0) ?: "0"
                    val label = parts.getOrNull(1) ?: "Timer"
                    InputChip(
                        selected = false,
                        onClick = { },
                        label = { Text("$mins min - $label") },
                        trailingIcon = {
                            Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp).clickable { timers.removeAt(index) })
                        }
                    )
                }
                if (timers.size < 5) {
                    FilterChip(
                        selected = false,
                        onClick = { showTimerDialog = true },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = "Add Timer", modifier = Modifier.size(16.dp)) },
                        label = { Text("Add Timer") }
                    )
                }
            }

            if (showTimerDialog) {
                BasicAlertDialog(onDismissRequest = { showTimerDialog = false }) {
                    Card {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Create Timer", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = timerMinsEdit,
                                onValueChange = { timerMinsEdit = it.filter { c -> c.isDigit() } },
                                label = { Text("Duration (minutes)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = timerLabelEdit,
                                onValueChange = { timerLabelEdit = it },
                                label = { Text("Timer Label") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { showTimerDialog = false }) { Text("Cancel") }
                                TextButton(onClick = {
                                    val m = timerMinsEdit.toIntOrNull() ?: 25
                                    val cleanMsg = timerLabelEdit.replace("|", " ").trim()
                                    timers.add("$m|${if (cleanMsg.isNotBlank()) cleanMsg else "Timer"}")
                                    showTimerDialog = false
                                }) { Text("OK") }
                            }
                        }
                    }
                }
            }

            // Column Builder
            Text("Tracking Columns", style = MaterialTheme.typography.titleSmall)
            Text("Add up to 8 columns to track different aspects of this task",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            columns.forEachIndexed { index, col ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            var labelEdit by remember(col.id) { mutableStateOf(col.label) }
                            OutlinedTextField(
                                value = labelEdit,
                                onValueChange = { newLabel ->
                                    labelEdit = newLabel
                                    columns[index] = col.copy(label = newLabel)
                                },
                                label = { Text("Label") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Type selector
                            var typeExpanded by remember { mutableStateOf(false) }
                            Box {
                                AssistChip(
                                    onClick = { typeExpanded = true },
                                    label = { Text(col.type.name) }
                                )
                                DropdownMenu(
                                    expanded = typeExpanded,
                                    onDismissRequest = { typeExpanded = false }
                                ) {
                                    ColumnType.entries.forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type.name) },
                                            onClick = {
                                                columns[index] = col.copy(type = type)
                                                typeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Max score for SCORE type
                            AnimatedVisibility(visible = col.type == ColumnType.SCORE) {
                                var maxScoreText by remember(col.id) {
                                    mutableStateOf((col.maxScore ?: 10).toString())
                                }
                                OutlinedTextField(
                                    value = maxScoreText,
                                    onValueChange = { newVal ->
                                        maxScoreText = newVal
                                        val parsed = newVal.toIntOrNull()
                                        if (parsed != null) columns[index] = col.copy(maxScore = parsed)
                                    },
                                    label = { Text("Max Score") },
                                    singleLine = true,
                                    modifier = Modifier.width(120.dp),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        // Delete column button
                        if (columns.size > 1) {
                            IconButton(onClick = { columns.removeAt(index) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Add column button
            if (columns.size < 8) {
                Button(
                    onClick = {
                        columns.add(ColumnDef(UUID.randomUUID().toString(), "", ColumnType.TICK))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Column")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
