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
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.neonroutine.MainActivity
import com.neonroutine.data.db.AppDatabase
import com.neonroutine.data.model.CompletionState
import com.neonroutine.data.model.Recurrence
import com.neonroutine.data.model.Task
import com.neonroutine.data.prefs.AppPreferences
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StreakWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(80.dp, 80.dp),    // Tiny
            DpSize(160.dp, 80.dp),   // Small — streak + level
            DpSize(160.dp, 160.dp)   // Medium — streak + level + points bar
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var streak = 0
        var totalPts = 0
        var level = 1

        try {
            val database = AppDatabase.getInstance(context)
            val allTasks = database.taskDao().getAllActiveTasksOnce()

            // Calculate streak (consecutive days all tasks done)
            var date = LocalDate.now()
            var lookback = 365
            while (lookback > 0 && allTasks.isNotEmpty()) {
                val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val scheduled = allTasks.filter { isScheduled(it, date) }
                if (scheduled.isEmpty()) { date = date.minusDays(1); lookback--; continue }
                val entries = database.entryDao().getEntriesInRangeOnce(dateStr, dateStr)
                val entryMap = entries.associateBy { it.taskId }
                val allDone = scheduled.all { entryMap[it.id]?.completionState == CompletionState.COMPLETED }
                if (allDone) { streak++; date = date.minusDays(1); lookback-- }
                else break
            }

            // Total points → level
            val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val recentEntries = database.entryDao().getEntriesInRangeOnce("2000-01-01", todayStr)
            totalPts = recentEntries.sumOf { e ->
                val task = allTasks.find { it.id == e.taskId }
                when (e.completionState) {
                    CompletionState.COMPLETED -> task?.pointsValue ?: 10
                    CompletionState.PARTIAL   -> (task?.pointsValue ?: 10) / 2
                    else                      -> 0
                }
            }
            level = (totalPts / 100) + 1

        } catch (e: Exception) {
            e.printStackTrace()
        }

        val nextLevelPts = level * 100
        val progress = if (nextLevelPts > 0) (totalPts % 100).coerceIn(0, 100) else 0
        val widgetTitle = AppPreferences.readWidgetTitle(context)

        provideContent {
            GlanceTheme {
                val size = androidx.glance.LocalSize.current
                when {
                    size.width < 120.dp -> TinyLayout(streak)
                    size.height < 120.dp -> SmallLayout(streak, level, totalPts, widgetTitle)
                    else -> MediumLayout(streak, level, totalPts, progress, widgetTitle)
                }
            }
        }
    }

    @Composable
    private fun TinyLayout(streak: Int) {
        Box(
            modifier = GlanceModifier.fillMaxSize()
                .background(Color(0xFF0D0D0D)).cornerRadius(16.dp)
                .clickable(actionStartActivity<MainActivity>()).padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔥", style = TextStyle(fontSize = 18.sp, textAlign = TextAlign.Center))
                Text("$streak", style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color(0xFFFF6B35)),
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                ))
            }
        }
    }

    @Composable
    private fun SmallLayout(streak: Int, level: Int, points: Int, widgetTitle: String = AppPreferences.DEFAULT_WIDGET_TITLE) {
        Row(
            modifier = GlanceModifier.fillMaxSize()
                .background(Color(0xFF0D0D0D)).cornerRadius(16.dp)
                .clickable(actionStartActivity<MainActivity>()).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔥", style = TextStyle(fontSize = 16.sp))
                Text("$streak", style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color(0xFFFF6B35)),
                    fontSize = 22.sp, fontWeight = FontWeight.Bold
                ))
                Text("days", style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color.White.copy(alpha = 0.5f)),
                    fontSize = 8.sp
                ))
            }
            Spacer(GlanceModifier.width(12.dp))
            Column {
                Text("⭐ Level $level", style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color(0xFFFFC107)),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                ))
                Text("$points pts", style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color.White.copy(alpha = 0.6f)),
                    fontSize = 10.sp
                ))
                Text(
                    if (streak == 0) "Start today!" else if (streak == 1) "1 day strong!" else "$streak day streak!",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(Color.White.copy(alpha = 0.4f)),
                        fontSize = 9.sp
                    )
                )
            }
        }
    }

    @Composable
    private fun MediumLayout(streak: Int, level: Int, points: Int, progressPct: Int, widgetTitle: String = AppPreferences.DEFAULT_WIDGET_TITLE) {
        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(Color(0xFF0D0D0D)).cornerRadius(16.dp)
                .clickable(actionStartActivity<MainActivity>()).padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(widgetTitle, style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color.White),
                    fontSize = 13.sp, fontWeight = FontWeight.Bold
                ), modifier = GlanceModifier.defaultWeight())
                Text("⭐ Lvl $level", style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color(0xFFFFC107)),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                ))
            }
            Spacer(GlanceModifier.height(8.dp))
            Text("$streak", style = TextStyle(
                color = androidx.glance.unit.ColorProvider(Color(0xFFFF6B35)),
                fontSize = 44.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            ))
            Text("CONSECUTIVE DAYS", style = TextStyle(
                color = androidx.glance.unit.ColorProvider(Color.White.copy(alpha = 0.5f)),
                fontSize = 9.sp, textAlign = TextAlign.Center
            ))
            Spacer(GlanceModifier.height(10.dp))
            // Points bar label
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text("$points pts", style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color(0xFFFFC107)),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold
                ), modifier = GlanceModifier.defaultWeight())
                Text("→ Lvl ${level + 1}", style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color.White.copy(alpha = 0.4f)),
                    fontSize = 10.sp
                ))
            }
            Spacer(GlanceModifier.height(3.dp))
            // XP progress bar background
            Box(modifier = GlanceModifier.fillMaxWidth().height(5.dp).background(Color(0xFF2A2A2A)).cornerRadius(3.dp)) {}
        }
    }

    private fun isScheduled(task: Task, date: LocalDate): Boolean {
        return task.isScheduledForDate(date)
    }
}
