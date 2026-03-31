package com.neonroutine.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neonroutine.NeonRoutineApp
import com.neonroutine.data.model.ColumnDef
import com.neonroutine.data.model.ColumnType
import com.neonroutine.data.model.CompletionState
import com.neonroutine.data.model.Entry
import com.neonroutine.data.model.HabitCategory
import com.neonroutine.data.model.Recurrence
import com.neonroutine.data.model.Task
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import com.neonroutine.widget.WidgetUpdater
import com.neonroutine.notifications.NotificationScheduler

/**
 * Immutable snapshot of all Stats-tab data, derived strictly from the DB.
 * No hardcoded values. Recomputed on every tasks/entries change.
 */
data class SleepSession(
    val id: String,
    val sleepTime: String,
    val wakeTime: String,
    val durationMinutes: Int
)

data class StatsData(
    /** day-of-month (1..31) -> completion 0..1f. Only contains days that have ≥1 scheduled task. */
    val dailyPercents: Map<Int, Float> = emptyMap(),
    /** week index (0-based) -> average completion 0..1f for that ISO-week group */
    val weeklyAvgs: List<Float> = emptyList(),
    /** overall completion fraction 0..1f across all habits × all days in month */
    val overallPercent: Float = 0f,
    /** taskId -> (title, color, completion 0..1f across scheduled days this month) */
    val perTaskCompletion: List<Triple<String,String,Float>> = emptyList(),
    /** current streak length in days */
    val streak: Int = 0,
    /** how many days had 100 % completion */
    val perfectDays: Int = 0,
    /** best completion day -> day-of-month */
    val bestDayOfMonth: Int? = null,
    val bestDayPercent: Float = 0f,
    val totalDaysTracked: Int = 0,
    /** sleep durations for each day in hours, derived from 'sys_sleep' task entries */
    val sleepDurations: Map<String, Float> = emptyMap(),
    /** detailed sleep sessions per date string, supporting multiple sessions a day */
    val sleepSessions: Map<String, List<SleepSession>> = emptyMap()
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as NeonRoutineApp).repository
    private val json = Json { ignoreUnknownKeys = true }

    // All active (non-archived) tasks
    val tasks: StateFlow<List<Task>> = repo.getAllActiveTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Stats: reactive, data-driven ─────────────────────────────────────────
    // Live entries for the whole current month so Room triggers recomposition.
    private val _statsMonthStart = LocalDate.now().withDayOfMonth(1)
    private val _statsMonthEnd   = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth())

    private val _monthEntriesFlow = repo.getEntriesInRange(
        _statsMonthStart.format(DateTimeFormatter.ISO_LOCAL_DATE),
        _statsMonthEnd.format(DateTimeFormatter.ISO_LOCAL_DATE)
    )

    /**
     * Single source of truth for the Stats tab.
     * Combines tasks + live month-entries and computes every metric from scratch.
     * Emits a new [StatsData] on every entry/task change → guarantees real-time.
     */
    val statsData: StateFlow<StatsData> = combine(tasks, _monthEntriesFlow) { taskList, monthEntries ->
        computeStatsData(taskList, monthEntries)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsData())

    private fun computeStatsData(taskList: List<Task>, monthEntries: List<Entry>): StatsData {
        if (taskList.isEmpty()) return StatsData()

        val today = LocalDate.now()
        val monthStart = today.withDayOfMonth(1)
        val monthLast  = today.withDayOfMonth(today.lengthOfMonth())
        val trackUntil = today // don't show future days as 0 %

        // Index entries by date string
        val entriesByDate: Map<String, List<Entry>> = monthEntries.groupBy { it.date }

        // ── 1. Daily percentage array ────────────────────────────────────────
        val dailyPercents = mutableMapOf<Int, Float>()
        var d = monthStart
        while (!d.isAfter(trackUntil)) {
            val dateStr = d.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val scheduled = taskList.filter { isTaskScheduledForDate(it, d) }
            if (scheduled.isNotEmpty()) {
                val dayEntries = (entriesByDate[dateStr] ?: emptyList()).associateBy { it.taskId }
                val sum = scheduled.sumOf { task ->
                    calculateCompletionForEntry(task, dayEntries[task.id]).toDouble()
                }
                dailyPercents[d.dayOfMonth] = (sum / scheduled.size).toFloat()
            }
            d = d.plusDays(1)
        }

        // ── 2. Weekly averages (group by calendar week within the month) ─────
        // week 0 = days 1-7, week 1 = days 8-14, etc.
        val weeklyAvgs = mutableListOf<Float>()
        for (weekIdx in 0..4) {
            val dayStart = weekIdx * 7 + 1
            val dayEnd   = minOf(dayStart + 6, trackUntil.dayOfMonth)
            if (dayStart > trackUntil.dayOfMonth) break
            val vals = (dayStart..dayEnd).mapNotNull { dailyPercents[it] }
            weeklyAvgs.add(if (vals.isNotEmpty()) vals.average().toFloat() else 0f)
        }

        // ── 3. Overall % ─────────────────────────────────────────────────────
        val overallPercent = if (dailyPercents.isNotEmpty())
            dailyPercents.values.average().toFloat() else 0f

        // ── 4. Per-task completion ────────────────────────────────────────────
        val perTaskCompletion = taskList.map { task ->
            var scheduledDays = 0
            var completedSum = 0.0
            var t = monthStart
            while (!t.isAfter(trackUntil)) {
                if (isTaskScheduledForDate(task, t)) {
                    scheduledDays++
                    val dateStr = t.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val entry = (entriesByDate[dateStr] ?: emptyList()).find { it.taskId == task.id }
                    completedSum += calculateCompletionForEntry(task, entry)
                }
                t = t.plusDays(1)
            }
            val frac = if (scheduledDays > 0) (completedSum / scheduledDays).toFloat() else 0f
            Triple(task.id, task.title to task.color, frac)
        }.map { (_, titleColor, frac) ->
            Triple(titleColor.first, titleColor.second, frac)
        }

        // ── 5. Streak (synchronous, bounded to 365 days) ─────────────────────
        var streak = 0
        var sd = today
        var lookback = 365
        while (lookback > 0) {
            val sdStr = sd.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val scheduled = taskList.filter { isTaskScheduledForDate(it, sd) }
            if (scheduled.isEmpty()) { sd = sd.minusDays(1); lookback--; continue }
            // For days outside month we don't have live entries — approximate via dailyPercents
            val pct = if (!sd.isBefore(monthStart) && !sd.isAfter(trackUntil)) {
                dailyPercents[sd.dayOfMonth] ?: 0f
            } else {
                // Requires a blocking call — skip for days outside current month window
                break
            }
            if (pct >= 1f) { streak++; sd = sd.minusDays(1); lookback-- }
            else break
        }

        // ── 6. Gamification extras ────────────────────────────────────────────
        val perfectDays = dailyPercents.values.count { it >= 1f }
        val bestEntry = dailyPercents.maxByOrNull { it.value }
        val totalDaysTracked = dailyPercents.size

        // ── 7. Sleep Tracking Array ──────────────────────────────────────────
        val sleepDurations = mutableMapOf<String, Float>()
        val sleepSessions = mutableMapOf<String, List<SleepSession>>()
        for ((dateStr, dayEntries) in entriesByDate) {
            val sleepEntry = dayEntries.find { it.taskId == "sys_sleep" }
            if (sleepEntry != null && sleepEntry.valuesJson.isNotBlank()) {
                try {
                    val obj = json.decodeFromString<JsonObject>(sleepEntry.valuesJson)
                    val sessionsArray = obj["sessions"]?.jsonArray
                    if (sessionsArray != null) {
                        var dailyDur = 0
                        val dailySessions = mutableListOf<SleepSession>()
                        for (elem in sessionsArray) {
                            val seshObj = elem.jsonObject
                            val sId = seshObj["id"]?.jsonPrimitive?.content ?: java.util.UUID.randomUUID().toString()
                            val sTime = seshObj["sleep_time"]?.jsonPrimitive?.content ?: ""
                            val wTime = seshObj["wake_time"]?.jsonPrimitive?.content ?: ""
                            val dur = seshObj["duration_minutes"]?.jsonPrimitive?.intOrNull ?: 0
                            dailyDur += dur
                            dailySessions.add(SleepSession(sId, sTime, wTime, dur))
                        }
                        sleepDurations[dateStr] = dailyDur / 60f
                        sleepSessions[dateStr] = dailySessions
                    } else {
                        // Legacy fallback for single session entry
                        val duration = obj["duration_minutes"]?.jsonPrimitive?.intOrNull
                        val sTime = obj["sleep_time"]?.jsonPrimitive?.content ?: ""
                        val wTime = obj["wake_time"]?.jsonPrimitive?.content ?: ""
                        if (duration != null) {
                            sleepDurations[dateStr] = duration / 60f
                            val legacySession = SleepSession(java.util.UUID.randomUUID().toString(), sTime, wTime, duration)
                            sleepSessions[dateStr] = listOf(legacySession)
                        }
                    }
                } catch (e: Exception) {}
            }
        }

        return StatsData(
            dailyPercents = dailyPercents,
            weeklyAvgs = weeklyAvgs,
            overallPercent = overallPercent,
            perTaskCompletion = perTaskCompletion,
            streak = streak,
            perfectDays = perfectDays,
            bestDayOfMonth = bestEntry?.key,
            bestDayPercent = bestEntry?.value ?: 0f,
            totalDaysTracked = totalDaysTracked,
            sleepDurations = sleepDurations,
            sleepSessions = sleepSessions
        )
    }

    // Selected date for views
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Entries for the selected date
    private val _entriesForDate = MutableStateFlow<Map<String, Entry>>(emptyMap())
    val entriesForDate: StateFlow<Map<String, Entry>> = _entriesForDate.asStateFlow()

    // Entries for a date range (week/month views)
    private val _entriesInRange = MutableStateFlow<List<Entry>>(emptyList())
    val entriesInRange: StateFlow<List<Entry>> = _entriesInRange.asStateFlow()

    // UI Events for toasts and errors
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent

    init {
        // Reload entries whenever selectedDate changes
        viewModelScope.launch {
            _selectedDate.collect { date ->
                loadEntriesForDate(date)
            }
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    private suspend fun loadEntriesForDate(date: LocalDate) {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        repo.getEntriesForDate(dateStr).collect { entries ->
            _entriesForDate.value = entries.associateBy { it.taskId }
        }
    }

    fun loadEntriesForRange(startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch {
            val start = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val end = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            repo.getEntriesInRange(start, end).collect { entries ->
                _entriesInRange.value = entries
            }
        }
    }

    private suspend fun updateWidgets() {
        WidgetUpdater.updateAllWidgets(getApplication())
    }

    // -- Task CRUD --
    suspend fun getTaskById(id: String): Task? = repo.getTaskById(id)

    fun addTask(task: Task) {
        viewModelScope.launch { 
            repo.insertTask(task)
            NotificationScheduler.scheduleRemindersForTask(getApplication(), task)
            updateWidgets()
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch { 
            repo.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
            NotificationScheduler.scheduleRemindersForTask(getApplication(), task)
            updateWidgets()
        }
    }

    fun archiveTask(taskId: String) {
        viewModelScope.launch { 
            repo.archiveTask(taskId)
            NotificationScheduler.cancelRemindersForTask(getApplication(), taskId)
            updateWidgets()
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repo.deleteEntriesForTask(task.id)
            repo.deleteTask(task)
            NotificationScheduler.cancelRemindersForTask(getApplication(), task.id)
            updateWidgets()
        }
    }

    // -- Grid cell cycling: None -> Completed -> Partial -> Missed -> None
    fun cycleGridState(taskId: String, dateStr: String, currentState: CompletionState) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val targetDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            if (targetDate.isBefore(LocalDate.now().minusDays(7))) {
                _uiEvent.emit("Cannot edit habits older than 7 days ⏰")
                return@launch
            }

            val nextState = when (currentState) {
                CompletionState.NONE -> CompletionState.COMPLETED
                CompletionState.COMPLETED -> CompletionState.PARTIAL
                CompletionState.PARTIAL -> CompletionState.MISSED
                CompletionState.MISSED -> CompletionState.NONE
            }
            val existing = repo.getEntry(taskId, dateStr)
            val now = System.currentTimeMillis()
            val task = repo.getTaskById(taskId)
            
            val currentValues: MutableMap<String, kotlinx.serialization.json.JsonElement> =
                if (existing != null && existing.valuesJson.isNotBlank()) {
                    try { json.decodeFromString<JsonObject>(existing.valuesJson).toMutableMap() } catch(e:Exception){ mutableMapOf() }
                } else mutableMapOf()
                
            if (task != null && nextState == CompletionState.COMPLETED) {
                // Fill all tick boxes for accurate Home representation
                val columns = parseColumns(task.columnsJson)
                for (col in columns) {
                    if (col.type == ColumnType.TICK) currentValues[col.id] = JsonPrimitive(true)
                }
            } else if (task != null && nextState == CompletionState.NONE) {
                // Clear all tick boxes for accurate Home representation
                val columns = parseColumns(task.columnsJson)
                for (col in columns) {
                    if (col.type == ColumnType.TICK) currentValues[col.id] = JsonPrimitive(false)
                }
            }

            val newJson = JsonObject(currentValues).toString()
            val newEntry = if (existing != null) {
                existing.copy(completionState = nextState, valuesJson = newJson, updatedAt = now)
            } else {
                Entry(taskId = taskId, date = dateStr, completionState = nextState, valuesJson = newJson, updatedAt = now)
            }
            
            // Optimistic UI mapping
            val currentMap = _entriesForDate.value.toMutableMap()
            if (dateStr == _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)) {
                currentMap[taskId] = newEntry
                _entriesForDate.value = currentMap
            }
            
            if (existing != null) {
                repo.updateEntry(newEntry)
            } else {
                repo.insertEntry(newEntry)
            }
            updateWidgets()
        }
    }

    // -- Entry operations --
    fun updateEntryValue(taskId: String, date: LocalDate, columnId: String, value: Any) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (date.isBefore(LocalDate.now().minusDays(7))) {
                _uiEvent.emit("Cannot edit habits older than 7 days ⏰")
                return@launch
            }

            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val existing = repo.getEntry(taskId, dateStr)
            val task = repo.getTaskById(taskId) ?: return@launch
            
            val currentValues: MutableMap<String, kotlinx.serialization.json.JsonElement> =
                if (existing != null && existing.valuesJson.isNotBlank()) {
                    try { json.decodeFromString<JsonObject>(existing.valuesJson).toMutableMap() } catch(e:Exception){ mutableMapOf() }
                } else mutableMapOf()

            currentValues[columnId] = when (value) {
                is Boolean -> JsonPrimitive(value)
                is Int -> JsonPrimitive(value)
                is String -> JsonPrimitive(value)
                else -> JsonPrimitive(value.toString())
            }

            val newJson = JsonObject(currentValues).toString()
            val now = System.currentTimeMillis()
            
            // Calculate strictly to auto-sync with Grid format
            val tempEntry = if (existing != null) existing.copy(valuesJson = newJson) else Entry(taskId = taskId, date = dateStr, valuesJson = newJson, completionState = CompletionState.NONE)
            val completionPct = calculateCompletionForEntry(task, tempEntry)
            val estimatedState = when {
                completionPct >= 1f -> CompletionState.COMPLETED
                completionPct > 0f -> CompletionState.PARTIAL
                else -> CompletionState.NONE
            }

            val newEntry = if (existing != null) {
                existing.copy(completionState = estimatedState, valuesJson = newJson, updatedAt = now)
            } else {
                Entry(taskId = taskId, date = dateStr, completionState = estimatedState, valuesJson = newJson, updatedAt = now)
            }

            // Optimistic mapping
            val currentMap = _entriesForDate.value.toMutableMap()
            if (dateStr == _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)) {
                currentMap[taskId] = newEntry
                _entriesForDate.value = currentMap
            }

            if (existing != null) {
                repo.updateEntry(newEntry)
            } else {
                repo.insertEntry(newEntry)
            }
            updateWidgets()
        }
    }

    fun quickCompleteTask(taskId: String, task: Task, date: LocalDate) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (date.isBefore(LocalDate.now().minusDays(7))) {
                _uiEvent.emit("Cannot edit habits older than 7 days ⏰")
                return@launch
            }

            val columns = parseColumns(task.columnsJson)
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val existing = repo.getEntry(taskId, dateStr)
            val currentValues: MutableMap<String, kotlinx.serialization.json.JsonElement> =
                if (existing != null && existing.valuesJson.isNotBlank()) {
                    try { json.decodeFromString<JsonObject>(existing.valuesJson).toMutableMap() } catch(e: Exception){ mutableMapOf() }
                } else {
                    mutableMapOf()
                }

            for (col in columns) {
                if (col.type == ColumnType.TICK) {
                    currentValues[col.id] = JsonPrimitive(true)
                }
            }

            val newJson = JsonObject(currentValues).toString()
            val now = System.currentTimeMillis()
            val newEntry = if (existing != null) {
                existing.copy(completionState = CompletionState.COMPLETED, valuesJson = newJson, updatedAt = now)
            } else {
                Entry(taskId = taskId, date = dateStr, completionState = CompletionState.COMPLETED, valuesJson = newJson, updatedAt = now)
            }

            val currentMap = _entriesForDate.value.toMutableMap()
            if (dateStr == _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)) {
                currentMap[taskId] = newEntry
                _entriesForDate.value = currentMap
            }

            if (existing != null) {
                repo.updateEntry(newEntry)
            } else {
                repo.insertEntry(newEntry)
            }
            updateWidgets()
        }
    }

    fun savePhotoToEntry(taskId: String, date: LocalDate, absoluteFileName: String) {
        viewModelScope.launch {
            if (date.isBefore(LocalDate.now().minusDays(7))) {
                _uiEvent.emit("Cannot edit habits older than 7 days ⏰")
                return@launch
            }

            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val existing = repo.getEntry(taskId, dateStr)
            val now = System.currentTimeMillis()
            
            if (existing != null) {
                repo.updateEntry(existing.copy(photoPath = absoluteFileName, updatedAt = now))
            } else {
                repo.insertEntry(Entry(taskId = taskId, date = dateStr, photoPath = absoluteFileName, updatedAt = now))
            }
            updateWidgets()
        }
    }

    private suspend fun getExistingSleepSessionsJson(dateStr: String): Pair<Entry?, MutableList<JsonObject>> {
        val existing = repo.getEntry("sys_sleep", dateStr)
        val sessionsList = mutableListOf<JsonObject>()
        if (existing != null && existing.valuesJson.isNotBlank()) {
            try {
                val obj = json.decodeFromString<JsonObject>(existing.valuesJson)
                val existingSessions = obj["sessions"]?.jsonArray
                if (existingSessions != null) {
                    for (elem in existingSessions) {
                        sessionsList.add(elem.jsonObject)
                    }
                } else {
                    // Migrate legacy data on the fly
                    val oldDur = obj["duration_minutes"]?.jsonPrimitive?.intOrNull
                    if (oldDur != null) {
                        sessionsList.add(JsonObject(mapOf(
                            "id" to JsonPrimitive(java.util.UUID.randomUUID().toString()),
                            "sleep_time" to (obj["sleep_time"] ?: JsonPrimitive("")),
                            "wake_time" to (obj["wake_time"] ?: JsonPrimitive("")),
                            "duration_minutes" to JsonPrimitive(oldDur)
                        )))
                    }
                }
            } catch (e: Exception) { }
        }
        return Pair(existing, sessionsList)
    }

    private fun persistSleepSessions(existing: Entry?, dateStr: String, sessionsList: List<JsonObject>) {
        viewModelScope.launch {
            val newJson = JsonObject(mapOf("sessions" to kotlinx.serialization.json.JsonArray(sessionsList))).toString()
            val now = System.currentTimeMillis()
            if (existing != null) {
                repo.updateEntry(existing.copy(valuesJson = newJson, updatedAt = now))
            } else {
                repo.insertEntry(Entry(taskId = "sys_sleep", date = dateStr, valuesJson = newJson, updatedAt = now))
            }
        }
    }

    fun addSleepSession(date: LocalDate, sleepTime: String, wakeTime: String, durationMinutes: Int) {
        viewModelScope.launch {
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val (existing, sessionsList) = getExistingSleepSessionsJson(dateStr)
            
            val newSession = JsonObject(mapOf(
                "id" to JsonPrimitive(java.util.UUID.randomUUID().toString()),
                "sleep_time" to JsonPrimitive(sleepTime),
                "wake_time" to JsonPrimitive(wakeTime),
                "duration_minutes" to JsonPrimitive(durationMinutes)
            ))
            sessionsList.add(newSession)
            persistSleepSessions(existing, dateStr, sessionsList)
        }
    }

    fun updateSleepSession(date: LocalDate, sessionId: String, sleepTime: String, wakeTime: String, durationMinutes: Int) {
        viewModelScope.launch {
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val (existing, sessionsList) = getExistingSleepSessionsJson(dateStr)
            
            val idx = sessionsList.indexOfFirst { it["id"]?.jsonPrimitive?.content == sessionId }
            if (idx != -1) {
                sessionsList[idx] = JsonObject(mapOf(
                    "id" to JsonPrimitive(sessionId),
                    "sleep_time" to JsonPrimitive(sleepTime),
                    "wake_time" to JsonPrimitive(wakeTime),
                    "duration_minutes" to JsonPrimitive(durationMinutes)
                ))
            }
            persistSleepSessions(existing, dateStr, sessionsList)
        }
    }

    fun removeSleepSession(date: LocalDate, sessionId: String) {
        viewModelScope.launch {
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val (existing, sessionsList) = getExistingSleepSessionsJson(dateStr)
            
            sessionsList.removeAll { it["id"]?.jsonPrimitive?.content == sessionId }
            persistSleepSessions(existing, dateStr, sessionsList)
        }
    }

    // -- Helpers --
    fun parseColumns(columnsJson: String): List<ColumnDef> {
        return try {
            json.decodeFromString<List<ColumnDef>>(columnsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getColumnValue(entry: Entry?, columnId: String): Any? {
        if (entry == null) return null
        return try {
            val obj = json.decodeFromString<JsonObject>(entry.valuesJson)
            val element = obj[columnId] ?: return null
            val prim = element.jsonPrimitive
            when {
                prim.booleanOrNull != null -> prim.boolean
                prim.intOrNull != null -> prim.int
                else -> prim.content
            }
        } catch (e: Exception) {
            null
        }
    }

    fun isTaskScheduledForDate(task: Task, date: LocalDate): Boolean {
        return task.isScheduledForDate(date)
    }

    fun calculateCompletionForEntry(task: Task, entry: Entry?): Float {
        val columns = parseColumns(task.columnsJson)
        if (columns.isEmpty()) {
            if (entry == null) return 0f
            return when (entry.completionState) {
                CompletionState.COMPLETED -> 1f
                CompletionState.PARTIAL -> 0.5f
                else -> 0f
            }
        }
        if (entry == null) return 0f

        var completed = 0
        for (col in columns) {
            val value = getColumnValue(entry, col.id)
            val done = when (col.type) {
                ColumnType.TICK -> value == true
                ColumnType.SCORE -> {
                    val score = (value as? Int) ?: 0
                    score > 0
                }
                ColumnType.GRADE -> {
                    val grade = (value as? String) ?: ""
                    grade.isNotBlank()
                }
                ColumnType.NOTE -> {
                    val note = (value as? String) ?: ""
                    note.isNotBlank()
                }
            }
            if (done) completed++
        }
        return completed.toFloat() / columns.size
    }

    // -- Gamification --
    suspend fun getTotalPoints(): Int {
        val allTasks = repo.getAllActiveTasksOnce()
        val allEntries = repo.getAllEntries()
        return allEntries.sumOf { entry ->
            val task = allTasks.find { it.id == entry.taskId }
            when (entry.completionState) {
                CompletionState.COMPLETED -> task?.pointsValue ?: 10
                CompletionState.PARTIAL -> (task?.pointsValue ?: 10) / 2
                else -> 0
            }
        }
    }

    // Stats helpers
    suspend fun getStreakCount(): Int {
        val allTasks = repo.getAllActiveTasksOnce()
        if (allTasks.isEmpty()) return 0
        var streak = 0
        var date = LocalDate.now()
        var maxLookback = 365 // Prevent infinite loop if no past tasks
        
        while (maxLookback > 0) {
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val scheduledTasks = allTasks.filter { isTaskScheduledForDate(it, date) }
            if (scheduledTasks.isEmpty()) {
                date = date.minusDays(1)
                maxLookback--
                continue
            }
            val entries = repo.getEntriesInRangeOnce(dateStr, dateStr)
            val entryMap = entries.associateBy { it.taskId }
            val allDone = scheduledTasks.all { task ->
                calculateCompletionForEntry(task, entryMap[task.id]) >= 1f
            }
            if (allDone) {
                streak++
                date = date.minusDays(1)
                maxLookback--
            } else {
                break
            }
        }
        return streak
    }

    suspend fun getCompletionForDateRange(startDate: LocalDate, endDate: LocalDate): Map<LocalDate, Float> {
        val allTasks = repo.getAllActiveTasksOnce()
        val startStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val endStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val entries = repo.getEntriesInRangeOnce(startStr, endStr)
        val entriesByDate = entries.groupBy { it.date }

        val result = mutableMapOf<LocalDate, Float>()
        var d = startDate
        while (!d.isAfter(endDate)) {
            val dateStr = d.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val scheduled = allTasks.filter { isTaskScheduledForDate(it, d) }
            if (scheduled.isNotEmpty()) {
                val dateEntries = entriesByDate[dateStr] ?: emptyList()
                val entryMap = dateEntries.associateBy { it.taskId }
                val avg = scheduled.map { calculateCompletionForEntry(it, entryMap[it.id]) }.average().toFloat()
                result[d] = avg
            }
            d = d.plusDays(1)
        }
        return result
    }

    // Export/Import
    suspend fun exportAllData(): String {
        val allTasks = repo.getAllActiveTasksOnce()
        val allEntries = repo.getAllEntries()
        val tasksJson = json.encodeToString(ListSerializer(Task.serializer()), allTasks)
        val entriesJson = json.encodeToString(ListSerializer(Entry.serializer()), allEntries)
        return """{"tasks":$tasksJson,"entries":$entriesJson}"""
    }

    fun importData(jsonStr: String) {
        viewModelScope.launch {
            try {
                val obj = json.decodeFromString<JsonObject>(jsonStr)
                val tasksArray = obj["tasks"]?.toString() ?: "[]"
                val entriesArray = obj["entries"]?.toString() ?: "[]"
                val importedTasks = json.decodeFromString<List<Task>>(tasksArray)
                val importedEntries = json.decodeFromString<List<Entry>>(entriesArray)
                for (t in importedTasks) repo.insertTask(t)
                for (e in importedEntries) repo.insertEntry(e)
            } catch (_: Exception) { }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repo.clearAllData()
            updateWidgets()
        }
    }
}
