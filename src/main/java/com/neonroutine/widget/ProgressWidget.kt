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
import androidx.glance.layout.size
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
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ProgressWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(80.dp, 80.dp),     // Tiny — just %
            DpSize(160.dp, 80.dp),    // Small — % + label
            DpSize(160.dp, 160.dp)    // Medium — full progress panel
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getInstance(context)
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val today = LocalDate.now()

        val allTasks = try { database.taskDao().getAllActiveTasksOnce() } catch (_: Exception) { emptyList() }
        val entries  = try { database.entryDao().getEntriesInRangeOnce(todayStr, todayStr) } catch (_: Exception) { emptyList() }
        val entryMap = entries.associateBy { it.taskId }

        val scheduled = allTasks.filter { isScheduled(it, today) }
        val total     = scheduled.size
        val completed = scheduled.count { entryMap[it.id]?.completionState == CompletionState.COMPLETED }
        val partial   = scheduled.count { entryMap[it.id]?.completionState == CompletionState.PARTIAL }
        val pct       = if (total > 0) ((completed + partial * 0.5f) / total * 100).toInt() else 0

        // Build category summary (top 3 done)
        val doneLabels = scheduled
            .filter { entryMap[it.id]?.completionState == CompletionState.COMPLETED }
            .take(3)
            .map { it.title }

        provideContent {
            GlanceTheme {
                val size = androidx.glance.LocalSize.current
                when {
                    size.width < 120.dp -> TinyLayout(pct)
                    size.height < 120.dp -> SmallLayout(pct, completed, total)
                    else -> MediumLayout(pct, completed, total, doneLabels)
                }
            }
        }
    }

    @Composable
    private fun TinyLayout(pct: Int) {
        val accent = accentForPct(pct)
        Box(
            modifier = GlanceModifier.fillMaxSize()
                .background(Color(0xFF0D0D0D)).cornerRadius(16.dp)
                .clickable(actionStartActivity<MainActivity>()).padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$pct%", style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(accent),
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                ))
                Text("done", style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color.White.copy(alpha = 0.5f)),
                    fontSize = 9.sp, textAlign = TextAlign.Center
                ))
            }
        }
    }

    @Composable
    private fun SmallLayout(pct: Int, completed: Int, total: Int) {
        val accent = accentForPct(pct)
        val bgColor = bgForPct(pct)
        Row(
            modifier = GlanceModifier.fillMaxSize()
                .background(bgColor).cornerRadius(16.dp)
                .clickable(actionStartActivity<MainActivity>()).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$pct%", style = TextStyle(
                color = androidx.glance.unit.ColorProvider(accent),
                fontSize = 26.sp, fontWeight = FontWeight.Bold
            ))
            Spacer(GlanceModifier.width(10.dp))
            Column {
                Text("TODAY", style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color.White.copy(alpha = 0.5f)),
                    fontSize = 9.sp, fontWeight = FontWeight.Bold
                ))
                Text("$completed/$total", style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color.White),
                    fontSize = 13.sp, fontWeight = FontWeight.Medium
                ))
                Text("habits done", style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color.White.copy(alpha = 0.5f)),
                    fontSize = 9.sp
                ))
            }
        }
    }

    @Composable
    private fun MediumLayout(pct: Int, completed: Int, total: Int, doneLabels: List<String>) {
        val accent  = accentForPct(pct)
        val bgColor = bgForPct(pct)
        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(bgColor).cornerRadius(16.dp)
                .clickable(actionStartActivity<MainActivity>()).padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("📊 Progress", style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color.White),
                    fontSize = 13.sp, fontWeight = FontWeight.Bold
                ), modifier = GlanceModifier.defaultWeight())
                Text("$completed/$total", style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color.White.copy(alpha = 0.6f)),
                    fontSize = 11.sp
                ))
            }
            Spacer(GlanceModifier.height(10.dp))
            // Giant % display
            Text("$pct%", style = TextStyle(
                color = androidx.glance.unit.ColorProvider(accent),
                fontSize = 38.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            ))
            Text(
                if (pct >= 100) "🏆 ALL DONE!" else if (pct >= 50) "GREAT PROGRESS" else "KEEP GOING",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(Color.White.copy(alpha = 0.6f)),
                    fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                )
            )
            if (doneLabels.isNotEmpty()) {
                Spacer(GlanceModifier.height(8.dp))
                doneLabels.forEach { label ->
                    Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp)) {
                        Text("✓ ", style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(accent), fontSize = 10.sp
                        ))
                        Text(label, style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(Color.White.copy(alpha = 0.7f)),
                            fontSize = 10.sp
                        ))
                    }
                }
            }
        }
    }

    private fun accentForPct(pct: Int): Color = when {
        pct >= 100 -> Color(0xFF4CAF50)
        pct >= 50  -> Color(0xFF7F77DD)
        else       -> Color(0xFFFF6B35)
    }

    private fun bgForPct(pct: Int): Color = when {
        pct >= 100 -> Color(0xFF0D2010)
        pct >= 50  -> Color(0xFF0D0D20)
        else       -> Color(0xFF0D0D0D)
    }

    private fun isScheduled(task: Task, date: LocalDate): Boolean {
        return task.isScheduledForDate(date)
    }
}
