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
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.text.TextStyle
import com.neonroutine.MainActivity
import com.neonroutine.data.db.AppDatabase
import com.neonroutine.data.model.CompletionState
import com.neonroutine.data.model.Entry
import com.neonroutine.data.model.Recurrence
import com.neonroutine.data.model.Task
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RoutineWidget : GlanceAppWidget() {

    // Responsive: small, medium, large layouts
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(100.dp, 100.dp),   // Small
            DpSize(250.dp, 110.dp),   // Medium
            DpSize(250.dp, 220.dp)    // Large
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getInstance(context)
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val today = LocalDate.now()

        val allTasks = try { database.taskDao().getAllActiveTasksOnce() } catch (e: Exception) { emptyList() }
        val entries = try { database.entryDao().getEntriesInRangeOnce(todayStr, todayStr) } catch (e: Exception) { emptyList() }
        val entryMap = entries.associateBy { it.taskId }
        val scheduledTasks = allTasks.filter { isTaskScheduledForDate(it, today) }

        val completed = scheduledTasks.count { entryMap[it.id]?.completionState == CompletionState.COMPLETED }
        val total = scheduledTasks.size
        val pct = if (total > 0) ((completed.toFloat() / total) * 100).toInt() else 0

        provideContent {
            GlanceTheme {
                androidx.glance.LocalSize.current.let { size ->
                    when {
                        size.width < 200.dp -> SmallLayout(pct, completed, total)
                        size.height < 160.dp -> MediumLayout(scheduledTasks, entryMap, pct, completed, total)
                        else -> LargeLayout(scheduledTasks, entryMap, pct, completed, total)
                    }
                }
            }
        }
    }

    // ─── Small: just a progress circle + percentage ───────────────────────────
    @Composable
    private fun SmallLayout(pct: Int, completed: Int, total: Int) {
        val accent = Color(0xFF7F77DD)
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF0D0D0D))
                .cornerRadius(16.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$pct%",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(accent),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    "$completed/$total",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color.White.copy(alpha = 0.6f)),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }

    // ─── Medium: header + compact checks ─────────────────────────────────────
    @Composable
    private fun MediumLayout(
        tasks: List<Task>,
        entryMap: Map<String, Entry>,
        pct: Int,
        completed: Int,
        total: Int
    ) {
        val accent = Color(0xFF7F77DD)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF0D0D0D))
                .cornerRadius(16.dp)
                .padding(10.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "⚡ NeonRoutine",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(accent),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    "$pct% • $completed/$total",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color.White.copy(alpha = 0.6f)),
                        fontSize = 10.sp
                    )
                )
            }
            Spacer(GlanceModifier.height(6.dp))
            // Show first 3 habits inline
            tasks.take(3).forEach { task ->
                val entry = entryMap[task.id]
                val isDone = entry?.completionState == CompletionState.COMPLETED
                val taskColor = try { Color(android.graphics.Color.parseColor(task.color)) } catch (e: Exception) { accent }
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clickable(
                            actionRunCallback<ToggleTaskAction>(
                                actionParametersOf(
                                    ActionParameters.Key<String>("taskId") to task.id,
                                    ActionParameters.Key<String>("currentState") to (entry?.completionState?.name ?: CompletionState.NONE.name)
                                )
                            )
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(16.dp)
                            .background(if (isDone) taskColor else Color(0xFF333333))
                            .cornerRadius(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Text("✓", style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.White), fontSize = 10.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        task.title,
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(if (isDone) Color.White.copy(alpha = 0.5f) else Color.White),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }

    // ─── Large: full list with all habits scrollable ──────────────────────────
    @Composable
    private fun LargeLayout(
        tasks: List<Task>,
        entryMap: Map<String, Entry>,
        pct: Int,
        completed: Int,
        total: Int
    ) {
        val accent = Color(0xFF7F77DD)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF0D0D0D))
                .cornerRadius(16.dp)
                .padding(12.dp)
        ) {
            // Header row
            Row(
                modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "⚡ Today's Habits",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color.White),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    "$pct%",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(accent),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(GlanceModifier.height(4.dp))
            // Progress bar approximation
            Row(modifier = GlanceModifier.fillMaxWidth().height(3.dp)) {
                 Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .height(3.dp)
                        .background(color = accent.copy(alpha = 0.25f))
                        .cornerRadius(2.dp)
                ) {}
            }
            Spacer(GlanceModifier.height(8.dp))

            if (tasks.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No habits today 🎯",
                        style = TextStyle(color = androidx.glance.unit.ColorProvider(Color.White.copy(alpha = 0.4f)), fontSize = 12.sp)
                    )
                }
            } else {
                LazyColumn {
                    items(tasks) { task ->
                        val entry = entryMap[task.id]
                        val isDone = entry?.completionState == CompletionState.COMPLETED
                        val isPartial = entry?.completionState == CompletionState.PARTIAL
                        val taskColor = try { Color(android.graphics.Color.parseColor(task.color)) } catch (e: Exception) { accent }

                        // Parse current value for quantitative tasks
                        val currentVal = try {
                            val vals = kotlinx.serialization.json.Json.decodeFromString<Map<String, Int>>(entry?.valuesJson ?: "{}")
                            vals["value"] ?: 0
                        } catch (e: Exception) { 0 }

                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .background(Color(0xFF1A1A1A))
                                .cornerRadius(10.dp)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Completion checkmark / Toggle
                            Box(
                                modifier = GlanceModifier
                                    .size(26.dp)
                                    .background(
                                        when {
                                            isDone -> taskColor
                                            isPartial -> taskColor.copy(alpha = 0.5f)
                                            else -> Color(0xFF2C2C2C)
                                        }
                                    )
                                    .cornerRadius(8.dp)
                                    .clickable(
                                        actionRunCallback<ToggleTaskAction>(
                                            actionParametersOf(
                                                ActionParameters.Key<String>("taskId") to task.id,
                                                ActionParameters.Key<String>("currentState") to (entry?.completionState?.name ?: CompletionState.NONE.name)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    when {
                                        isDone -> "✓"
                                        isPartial -> "~"
                                        else -> ""
                                    },
                                    style = TextStyle(
                                        color = androidx.glance.unit.ColorProvider(Color.White),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(GlanceModifier.width(10.dp))
                            
                            // 2. Task text
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    task.title,
                                    style = TextStyle(
                                        color = androidx.glance.unit.ColorProvider(
                                            if (isDone) Color.White.copy(alpha = 0.45f) else Color.White
                                        ),
                                        fontSize = 13.sp,
                                        fontWeight = if (isDone) FontWeight.Normal else FontWeight.Medium
                                    )
                                )
                                if (task.targetQuantity > 1) {
                                    Text(
                                        "$currentVal / ${task.targetQuantity}",
                                        style = TextStyle(
                                            color = androidx.glance.unit.ColorProvider(taskColor.copy(alpha = 0.7f)),
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }

                            // 3. Quick increment button for quantitative tasks
                            if (task.targetQuantity > 1 && !isDone) {
                                Box(
                                    modifier = GlanceModifier
                                        .size(30.dp)
                                        .background(taskColor.copy(alpha = 0.2f))
                                        .cornerRadius(15.dp)
                                        .clickable(
                                            actionRunCallback<IncrementTaskAction>(
                                                actionParametersOf(ActionParameters.Key<String>("taskId") to task.id)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+", style = TextStyle(color = androidx.glance.unit.ColorProvider(taskColor), fontSize = 18.sp, fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isTaskScheduledForDate(task: Task, date: LocalDate): Boolean {
        return task.isScheduledForDate(date)
    }
}

