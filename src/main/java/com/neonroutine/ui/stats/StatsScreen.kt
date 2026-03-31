package com.neonroutine.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neonroutine.ui.theme.DesignStyle
import com.neonroutine.ui.theme.LocalAppShapes
import com.neonroutine.ui.theme.LocalDesignStyle
import com.neonroutine.ui.theme.glassPanel
import com.neonroutine.ui.viewmodel.TaskViewModel
import java.time.LocalDate

@Composable
fun StatsScreen(viewModel: TaskViewModel) {
    val stats by viewModel.statsData.collectAsState()
    val appShapes = LocalAppShapes.current
    val designStyle = LocalDesignStyle.current
    val today = LocalDate.now()
    
    val currentMonthLength = today.lengthOfMonth()
    val todayDate = today.dayOfMonth

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Streak Card
        Card(
            modifier = Modifier.fillMaxWidth()
                .then(
                    if (designStyle == DesignStyle.GLASSMORPHISM) {
                        Modifier.glassPanel(shape = appShapes.card)
                    } else Modifier
                ),
            shape = appShapes.card,
            colors = CardDefaults.cardColors(
                containerColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.Transparent 
                                 else if (designStyle == DesignStyle.BRUTAL_MINIMAL) MaterialTheme.colorScheme.surface 
                                 else MaterialTheme.colorScheme.primaryContainer
            ),
            border = if (designStyle == DesignStyle.BRUTAL_MINIMAL) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline) else null,
            elevation = CardDefaults.cardElevation(if (designStyle == DesignStyle.BRUTAL_MINIMAL || designStyle == DesignStyle.GLASSMORPHISM) 0.dp else appShapes.cardElevation)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.LocalFireDepartment, null, tint = Color(0xFFFF6B35), modifier = Modifier.size(48.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    val textColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White
                                   else if (designStyle == DesignStyle.BRUTAL_MINIMAL) MaterialTheme.colorScheme.onSurface 
                                   else MaterialTheme.colorScheme.onPrimaryContainer
                    Text("${stats.streak} Day Streak", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, color = textColor)
                    Text(if (stats.streak > 0) "Keep it up! 🔥" else "Start completing tasks to build a streak!",
                        style = MaterialTheme.typography.bodyMedium, 
                        color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White.copy(alpha=0.7f) else textColor)
                }
            }
        }

        // Circular Progress Indicator Card
        Card(
            modifier = Modifier.fillMaxWidth()
                .then(
                    if (designStyle == DesignStyle.GLASSMORPHISM) {
                        Modifier.glassPanel(shape = appShapes.card)
                    } else Modifier
                ),
            shape = appShapes.card,
            colors = CardDefaults.cardColors(
                containerColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.Transparent else MaterialTheme.colorScheme.surface
            ),
            border = if (designStyle == DesignStyle.BRUTAL_MINIMAL) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline) else null,
            elevation = CardDefaults.cardElevation(if (designStyle == DesignStyle.BRUTAL_MINIMAL || designStyle == DesignStyle.GLASSMORPHISM) 0.dp else appShapes.cardElevation)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Overall Progress", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.SemiBold,
                        color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White else Color.Unspecified
                    )
                    Text(
                        "Monthly Completion", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White.copy(alpha=0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                    val animatedOverall by animateFloatAsState(stats.overallPercent, tween(1000), label = "overall")
                    val primaryColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White else MaterialTheme.colorScheme.primary
                    val trackColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White.copy(alpha=0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = trackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = appShapes.progressStrokeWidth.toPx().coerceAtMost(24f), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = primaryColor,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedOverall,
                            useCenter = false,
                            style = Stroke(width = appShapes.progressStrokeWidth.toPx().coerceAtMost(24f), cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        "${(animatedOverall * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        SleepTrackerCard(viewModel)
        Spacer(Modifier.height(16.dp))

        // Line Chart (Daily Trend)
        Text(
            "Daily Trend", 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.SemiBold,
            color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White else Color.Unspecified
        )
        Card(
            modifier = Modifier.fillMaxWidth()
                .then(
                    if (designStyle == DesignStyle.GLASSMORPHISM) {
                        Modifier.glassPanel(shape = appShapes.card)
                    } else Modifier
                ),
            shape = appShapes.card,
            colors = CardDefaults.cardColors(
                containerColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.Transparent else MaterialTheme.colorScheme.surface
            ),
            border = if (designStyle == DesignStyle.BRUTAL_MINIMAL) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline) else null,
            elevation = CardDefaults.cardElevation(if (designStyle == DesignStyle.BRUTAL_MINIMAL || designStyle == DesignStyle.GLASSMORPHISM) 0.dp else appShapes.cardElevation)
        ) {

            Column(modifier = Modifier.padding(16.dp)) {
                val lineColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White else MaterialTheme.colorScheme.primary
                val gradientColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White.copy(alpha=0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(vertical = 8.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxDays = currentMonthLength
                        val width = size.width
                        val height = size.height
                        
                        val path = Path()
                        val points = mutableListOf<Offset>()
                        
                        // We will only draw line & area up to today
                        val plotUntil = todayDate.coerceAtMost(maxDays)
                        
                        for (day in 1..plotUntil) {
                            val percent = stats.dailyPercents[day] ?: 0f
                            val x = if (maxDays > 1) (day - 1) * width / (maxDays - 1) else 0f
                            val y = height - (percent * height)
                            points.add(Offset(x, y))
                            
                            if (day == 1) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }
                        
                        if (points.isNotEmpty()) {
                            // Draw Path
                            drawPath(
                                path = path,
                                color = lineColor,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                            )
                            
                            // Fill Gradient
                            val fillPath = Path()
                            fillPath.addPath(path)
                            fillPath.lineTo(points.last().x, height)
                            fillPath.lineTo(0f, height)
                            fillPath.close()
                            
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(gradientColor, Color.Transparent),
                                    startY = 0f,
                                    endY = height
                                )
                            )
                            
                            // Draw Data Points up to today
                            points.forEach { offset ->
                                drawCircle(
                                    color = lineColor,
                                    radius = 4.dp.toPx(),
                                    center = offset
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.dp.toPx(),
                                    center = offset
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Day 1", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White.copy(alpha=0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Day $currentMonthLength", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White.copy(alpha=0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Weekly Bar Chart
        Text(
            "Weekly Averages", 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.SemiBold,
            color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White else Color.Unspecified
        )
        Card(
            modifier = Modifier.fillMaxWidth()
                .then(
                    if (designStyle == DesignStyle.GLASSMORPHISM) {
                        Modifier.glassPanel(shape = RoundedCornerShape(16.dp))
                    } else Modifier
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.Transparent else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(if (designStyle == DesignStyle.GLASSMORPHISM) 0.dp else 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val maxWeeks = 5
                    for (weekIndex in 0 until maxWeeks) {
                        val avg = stats.weeklyAvgs.getOrElse(weekIndex) { 0f }
                        val animAvg by animateFloatAsState(avg, tween(800), label = "bar_$weekIndex")
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            Text(
                                "${(animAvg * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White.copy(alpha=0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .width(32.dp)
                                    .height((animAvg * 100).dp.coerceAtLeast(4.dp))
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        when {
                                            avg >= 0.8f -> Brush.verticalGradient(listOf(Color(0xFF81C784), Color(0xFF388E3C)))
                                            avg >= 0.5f -> Brush.verticalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFBC02D)))
                                            avg > 0f -> Brush.verticalGradient(listOf(Color(0xFFFFB74D), Color(0xFFF57C00)))
                                            else -> Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
                                        }
                                    )
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "W${weekIndex + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White.copy(alpha=0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SleepTrackerCard(viewModel: TaskViewModel) {
    val stats by viewModel.statsData.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val designStyle = LocalDesignStyle.current
    val today = java.time.LocalDate.now()
    val todayStr = today.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
    
    var showDialog by remember { mutableStateOf(false) }
    var editingSessionId by remember { mutableStateOf<String?>(null) }
    var dBedTime by remember { mutableStateOf("23:00") }
    var dWakeTime by remember { mutableStateOf("07:00") }
    
    fun showTimePicker(initTime: String, onTimeSet: (String) -> Unit) {
        val parts = initTime.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 7
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        android.app.TimePickerDialog(context, { _, hour, minute ->
            onTimeSet(String.format(java.util.Locale.ROOT, "%02d:%02d", hour, minute))
        }, h, m, true).show()
    }
    
    if (showDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingSessionId == null) "Add Sleep Log" else "Edit Sleep Log") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bed Time:")
                        androidx.compose.material3.OutlinedButton(onClick = { showTimePicker(dBedTime) { dBedTime = it } }) {
                            Text(dBedTime)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Wake Time:")
                        androidx.compose.material3.OutlinedButton(onClick = { showTimePicker(dWakeTime) { dWakeTime = it } }) {
                            Text(dWakeTime)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    val pb = dBedTime.split(":")
                    val pw = dWakeTime.split(":")
                    val bh = pb[0].toInt(); val bm = pb[1].toInt()
                    val wh = pw[0].toInt(); val wm = pw[1].toInt()
                    var dur = (wh * 60 + wm) - (bh * 60 + bm)
                    if (dur < 0) dur += 1440
                    
                    if (editingSessionId == null) {
                        viewModel.addSleepSession(today, dBedTime, dWakeTime, dur)
                    } else {
                        viewModel.updateSleepSession(today, editingSessionId!!, dBedTime, dWakeTime, dur)
                    }
                    showDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Text(
        "Sleep & Recovery", 
        style = MaterialTheme.typography.titleMedium, 
        fontWeight = FontWeight.SemiBold,
        color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White else Color.Unspecified
    )
    Card(
        modifier = Modifier.fillMaxWidth()
            .then(
                if (designStyle == DesignStyle.GLASSMORPHISM) {
                    Modifier.glassPanel(shape = RoundedCornerShape(16.dp))
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.Transparent else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (designStyle == DesignStyle.GLASSMORPHISM) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val totalHours = stats.sleepDurations[todayStr] ?: 0f
                Column {
                    Text(
                        "Recorded today", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White.copy(alpha=0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${String.format(java.util.Locale.ROOT, "%.1f", totalHours)} hrs", 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold, 
                        color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White else MaterialTheme.colorScheme.primary
                    )
                }
                
                androidx.compose.material3.Button(onClick = { 
                    dBedTime = "23:00"; dWakeTime = "07:00"
                    editingSessionId = null
                    showDialog = true 
                }) {
                    Text("+ Add Log")
                }
            }
            
            val todaySessions = stats.sleepSessions[todayStr] ?: emptyList()
            if (todaySessions.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                todaySessions.forEach { session ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White.copy(alpha=0.1f) 
                                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "${session.sleepTime} - ${session.wakeTime}", 
                                    fontWeight = FontWeight.Medium,
                                    color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White else Color.Unspecified
                                )
                                Text(
                                    "${session.durationMinutes / 60}h ${session.durationMinutes % 60}m", 
                                    style = MaterialTheme.typography.bodySmall, 
                                    color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White.copy(alpha=0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                androidx.compose.material3.IconButton(onClick = {
                                    dBedTime = session.sleepTime
                                    dWakeTime = session.wakeTime
                                    editingSessionId = session.id
                                    showDialog = true
                                }) {
                                    Icon(
                                        Icons.Filled.Edit, 
                                        "Edit", 
                                        modifier = Modifier.size(20.dp), 
                                        tint = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White else MaterialTheme.colorScheme.primary
                                    )
                                }
                                androidx.compose.material3.IconButton(onClick = {
                                    viewModel.removeSleepSession(today, session.id)
                                }) {
                                    Icon(Icons.Filled.Delete, "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                "7-Day Sleep Trend", 
                style = MaterialTheme.typography.labelSmall, 
                color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White.copy(alpha=0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            
            val lineColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White else MaterialTheme.colorScheme.primary
            val gradientColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White.copy(alpha=0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            
            Box(modifier = Modifier.fillMaxWidth().height(120.dp).padding(vertical = 8.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val limitDays = 7
                    val width = size.width
                    val height = size.height
                    
                    val path = Path()
                    val points = mutableListOf<Offset>()
                    
                    var maxHrs = 10f
                    val weekData = (0 until limitDays).map { i ->
                        val dStr = today.minusDays((limitDays - 1 - i).toLong()).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                        stats.sleepDurations[dStr] ?: 0f
                    }
                    val currentMax = weekData.maxOrNull() ?: 0f
                    if (currentMax > maxHrs) maxHrs = currentMax
                    if (maxHrs == 0f) maxHrs = 10f
                    
                    weekData.forEachIndexed { index, hrs ->
                        val x = if (limitDays > 1) index * width / (limitDays - 1) else 0f
                        val y = height - ((hrs / maxHrs).coerceIn(0f, 1f) * height)
                        points.add(Offset(x, y))
                        
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    
                    if (points.isNotEmpty()) {
                        drawPath(path = path, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                        
                        val fillPath = Path()
                        fillPath.addPath(path)
                        fillPath.lineTo(points.last().x, height)
                        fillPath.lineTo(0f, height)
                        fillPath.close()
                        
                        drawPath(fillPath, brush = Brush.verticalGradient(listOf(gradientColor, Color.Transparent), startY = 0f, endY = height))
                        
                        points.forEach { offset ->
                            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = offset)
                            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = offset)
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val limitDays = 7
                for (i in (limitDays - 1) downTo 0) {
                    val d = today.minusDays(i.toLong())
                    Text(d.dayOfWeek.name.take(3), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
