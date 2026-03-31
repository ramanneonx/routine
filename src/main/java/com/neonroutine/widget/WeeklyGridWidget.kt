package com.neonroutine.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.neonroutine.MainActivity
import com.neonroutine.data.db.AppDatabase
import com.neonroutine.data.model.CompletionState
import com.neonroutine.data.model.Entry
import com.neonroutine.data.model.Recurrence
import com.neonroutine.data.model.Task
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class WeeklyGridWidget : GlanceAppWidget() {

    companion object {
        // Short day labels Mon..Sun
        private val DAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")
        private val ACCENT = Color(0xFF7F77DD)
        private val DONE_COLOR = Color(0xFF4CAF50)
        private val PARTIAL_COLOR = Color(0xFFFF9800)
        private val BG = Color(0xFF0D0D0D)
        private val CELL_BG = Color(0xFF1C1C1C)
        private val HEADER_TEXT = Color(0x99FFFFFF)
        private val FMT = DateTimeFormatter.ISO_LOCAL_DATE
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(160.dp, 110.dp),   // Compact – today's column only
            DpSize(300.dp, 160.dp),   // Medium  – up to 4 habits × 7 days
            DpSize(300.dp, 300.dp),   // Large   – full scrollable grid
        )
    )

    // ── Data helpers ──────────────────────────────────────────────────────────

    /** Monday of the current week */
    private fun weekStart(): LocalDate {
        val today = LocalDate.now()
        return today.with(DayOfWeek.MONDAY)
    }

    /** The 7 dates of the current week (Mon..Sun) */
    private fun weekDates(): List<LocalDate> {
        val start = weekStart()
        return (0..6).map { start.plusDays(it.toLong()) }
    }

    private fun isScheduled(task: Task, date: LocalDate): Boolean {
        return task.isScheduledForDate(date)
    }

    // ── provideGlance ─────────────────────────────────────────────────────────

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getInstance(context)
        val dates = weekDates()
        val startStr = dates.first().format(FMT)
        val endStr   = dates.last().format(FMT)
        val today    = LocalDate.now()

        val allTasks = try { db.taskDao().getAllActiveTasksOnce() } catch (_: Exception) { emptyList() }
        val entries  = try { db.entryDao().getEntriesInRangeOnce(startStr, endStr) } catch (_: Exception) { emptyList() }

        // entryMap[dateStr][taskId] → Entry
        val entryMap: Map<String, Map<String, Entry>> = entries
            .groupBy { it.date }
            .mapValues { (_, list) -> list.associateBy { it.taskId } }

        // Only tasks that appear at least once this week
        val weekTasks = allTasks.filter { t -> dates.any { d -> isScheduled(t, d) } }

        provideContent {
            GlanceTheme {
                val size = androidx.glance.LocalSize.current
                when {
                    size.width < 240.dp -> CompactLayout(weekTasks, entryMap, dates, today)
                    size.height < 240.dp -> MediumLayout(weekTasks, entryMap, dates, today)
                    else -> LargeLayout(weekTasks, entryMap, dates, today)
                }
            }
        }
    }

    // ── COMPACT: today column + today's habits ────────────────────────────────
    @Composable
    private fun CompactLayout(
        tasks: List<Task>,
        entryMap: Map<String, Map<String, Entry>>,
        dates: List<LocalDate>,
        today: LocalDate
    ) {
        val todayStr = today.format(FMT)
        val todayEntries = entryMap[todayStr] ?: emptyMap()
        val todayTasks = tasks.filter { isScheduled(it, today) }
        val done = todayTasks.count { todayEntries[it.id]?.completionState == CompletionState.COMPLETED }
        val total = todayTasks.size

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(BG)
                .cornerRadius(16.dp)
                .padding(10.dp)
        ) {
            // Header
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "📅 Today",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(ACCENT),
                        fontSize = 12.sp, fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    "$done/$total",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(HEADER_TEXT),
                        fontSize = 10.sp
                    )
                )
            }
            Spacer(GlanceModifier.height(6.dp))

            // Today's task cells
            todayTasks.take(4).forEach { task ->
                val entry = todayEntries[task.id]
                val state = entry?.completionState ?: CompletionState.NONE
                val taskColor = parseColor(task.color, ACCENT)
                TaskCellRow(task, state, taskColor, todayStr, isFuture = false)
                Spacer(GlanceModifier.height(4.dp))
            }

            if (todayTasks.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No habits today 🎯",
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(HEADER_TEXT),
                            fontSize = 11.sp, textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }

    // ── MEDIUM: day headers + up to 5 habits × 7 day cells ───────────────────
    @Composable
    private fun MediumLayout(
        tasks: List<Task>,
        entryMap: Map<String, Map<String, Entry>>,
        dates: List<LocalDate>,
        today: LocalDate
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(BG)
                .cornerRadius(16.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            // Title row
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "⚡ Week Grid",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(ACCENT),
                        fontSize = 12.sp, fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>())
                )
                Text(
                    weekRangeLabel(dates),
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(HEADER_TEXT),
                        fontSize = 9.sp
                    )
                )
            }
            Spacer(GlanceModifier.height(5.dp))

            // Day headers
            DayHeaderRow(dates, today, labelWidth = 60.dp)
            Spacer(GlanceModifier.height(4.dp))

            // Habit rows (up to 5 for medium)
            tasks.take(5).forEach { task ->
                HabitGridRow(task, entryMap, dates, today)
                Spacer(GlanceModifier.height(3.dp))
            }
        }
    }

    // ── LARGE: full scrollable grid ───────────────────────────────────────────
    @Composable
    private fun LargeLayout(
        tasks: List<Task>,
        entryMap: Map<String, Map<String, Entry>>,
        dates: List<LocalDate>,
        today: LocalDate
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(BG)
                .cornerRadius(16.dp)
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            // Title row
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "⚡ Weekly Habit Grid",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color.White),
                        fontSize = 14.sp, fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>())
                )
                Text(
                    weekRangeLabel(dates),
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(HEADER_TEXT),
                        fontSize = 10.sp
                    )
                )
            }
            Spacer(GlanceModifier.height(6.dp))

            // Day headers
            DayHeaderRow(dates, today, labelWidth = 70.dp)
            Spacer(GlanceModifier.height(5.dp))

            if (tasks.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Add habits in the app 🎯",
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(HEADER_TEXT),
                            fontSize = 12.sp, textAlign = TextAlign.Center
                        )
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(tasks) { task ->
                        HabitGridRow(task, entryMap, dates, today)
                        Spacer(GlanceModifier.height(4.dp))
                    }
                }
            }
        }
    }

    // ── Shared composables ────────────────────────────────────────────────────

    @Composable
    private fun DayHeaderRow(dates: List<LocalDate>, today: LocalDate, labelWidth: androidx.compose.ui.unit.Dp) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Spacer for habit name column
            Box(modifier = GlanceModifier.width(labelWidth)) {}
            dates.forEach { date ->
                val isToday = date == today
                val isFuture = date.isAfter(today)
                Box(
                    modifier = GlanceModifier.defaultWeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        DAY_LABELS[date.dayOfWeek.value - 1],
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(
                                when {
                                    isToday  -> ACCENT
                                    isFuture -> Color(0x44FFFFFF)
                                    else     -> HEADER_TEXT
                                }
                            ),
                            fontSize = 9.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun HabitGridRow(
        task: Task,
        entryMap: Map<String, Map<String, Entry>>,
        dates: List<LocalDate>,
        today: LocalDate
    ) {
        val taskColor = parseColor(task.color, ACCENT)
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(CELL_BG)
                .cornerRadius(8.dp)
                .padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Habit name label — truncated to ~10 chars
            Box(modifier = GlanceModifier.width(70.dp)) {
                Text(
                    task.title.take(9) + if (task.title.length > 9) "…" else "",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color.White.copy(alpha = 0.85f)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            }

            // 7 day cells
            dates.forEach { date ->
                val isScheduled = isScheduled(task, date)
                val isFuture = date.isAfter(today)
                val dateStr = date.format(FMT)
                val entry = entryMap[dateStr]?.get(task.id)
                val state = entry?.completionState ?: CompletionState.NONE

                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isScheduled) {
                        // Not scheduled – show a subtle dim dash
                        Box(
                            modifier = GlanceModifier
                                .size(20.dp)
                                .background(Color(0xFF111111))
                                .cornerRadius(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("·", style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0x22FFFFFF)),
                                fontSize = 10.sp, textAlign = TextAlign.Center
                            ))
                        }
                    } else if (isFuture) {
                        // Future day – show empty ring
                        Box(
                            modifier = GlanceModifier
                                .size(20.dp)
                                .background(Color(0xFF1A1A1A))
                                .cornerRadius(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("○", style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(Color(0x33FFFFFF)),
                                fontSize = 9.sp, textAlign = TextAlign.Center
                            ))
                        }
                    } else {
                        // Tappable cell
                        val cellBg = when (state) {
                            CompletionState.COMPLETED -> taskColor
                            CompletionState.PARTIAL   -> taskColor.copy(alpha = 0.45f)
                            CompletionState.MISSED    -> Color(0xFF3A1818)
                            CompletionState.NONE      -> Color(0xFF242424)
                        }
                        val cellText = when (state) {
                            CompletionState.COMPLETED -> "✓"
                            CompletionState.PARTIAL   -> "~"
                            CompletionState.MISSED    -> "✕"
                            CompletionState.NONE      -> ""
                        }
                        Box(
                            modifier = GlanceModifier
                                .size(20.dp)
                                .background(cellBg)
                                .cornerRadius(4.dp)
                                .clickable(
                                    actionRunCallback<GridToggleCellAction>(
                                        actionParametersOf(
                                            ActionParameters.Key<String>("taskId") to task.id,
                                            ActionParameters.Key<String>("dateStr") to dateStr,
                                            ActionParameters.Key<String>("currentState") to state.name
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                cellText,
                                style = TextStyle(
                                    color = androidx.glance.unit.ColorProvider(Color.White),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    /** Single-row task cell for compact layout */
    @Composable
    private fun TaskCellRow(
        task: Task,
        state: CompletionState,
        taskColor: Color,
        dateStr: String,
        isFuture: Boolean
    ) {
        val isDone = state == CompletionState.COMPLETED
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(CELL_BG)
                .cornerRadius(8.dp)
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .clickable(
                    actionRunCallback<GridToggleCellAction>(
                        actionParametersOf(
                            ActionParameters.Key<String>("taskId") to task.id,
                            ActionParameters.Key<String>("dateStr") to dateStr,
                            ActionParameters.Key<String>("currentState") to state.name
                        )
                    )
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(18.dp)
                    .background(if (isDone) taskColor else Color(0xFF2C2C2C))
                    .cornerRadius(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) Text("✓", style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color.White),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold
                ))
            }
            Spacer(GlanceModifier.width(8.dp))
            Text(
                task.title,
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(
                        if (isDone) Color.White.copy(alpha = 0.45f) else Color.White
                    ),
                    fontSize = 11.sp
                )
            )
        }
    }

    private fun weekRangeLabel(dates: List<LocalDate>): String {
        val fmt = DateTimeFormatter.ofPattern("MMM d")
        return "${dates.first().format(fmt)} – ${dates.last().format(fmt)}"
    }

    private fun parseColor(hex: String, fallback: Color): Color = try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) { fallback }
}
