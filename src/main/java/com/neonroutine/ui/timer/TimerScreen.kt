package com.neonroutine.ui.timer

import android.app.Activity
import android.media.RingtoneManager
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neonroutine.data.model.Task
import com.neonroutine.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    taskId: String,
    timerIndex: Int,
    viewModel: TaskViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var task by remember { mutableStateOf<Task?>(null) }
    var totalSeconds by remember { mutableIntStateOf(25 * 60) }
    var label by remember { mutableStateOf("Timer") }

    LaunchedEffect(taskId) {
        task = viewModel.getTaskById(taskId)
        task?.let { t ->
            try {
                val timersList = Json.decodeFromString<List<String>>(t.timersJson)
                if (timerIndex in timersList.indices) {
                    val parts = timersList[timerIndex].split("|")
                    val mins = parts.getOrNull(0)?.toIntOrNull() ?: 25
                    label = parts.getOrNull(1) ?: "Timer"
                    totalSeconds = mins * 60
                }
            } catch (e: Exception) { }
        }
    }

    var timeLeft by remember(totalSeconds) { mutableIntStateOf(totalSeconds) }
    var isRunning by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(isRunning, timeLeft) {
        if (isRunning && timeLeft > 0) {
            delay(1000L)
            timeLeft--
            if (timeLeft == 0) {
                isRunning = false
                try {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    RingtoneManager.getRingtone(context, uri).play()
                } catch (e: Exception) {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(label) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val progress = animateFloatAsState(targetValue = timeLeft.toFloat() / totalSeconds.toFloat(), label = "timer")
            val primaryColor = task?.color?.let { try { Color(android.graphics.Color.parseColor(it)) } catch(e:Exception){null} } ?: MaterialTheme.colorScheme.primary

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = trackColor,
                        startAngle = 0f, sweepAngle = 360f, useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f, sweepAngle = progress.value * 360f, useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val minutesStr = (timeLeft / 60).toString().padStart(2, '0')
                    val secondsStr = (timeLeft % 60).toString().padStart(2, '0')
                    Text(
                        text = "$minutesStr:$secondsStr",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (task != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = task!!.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = primaryColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(64.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                IconButton(
                    onClick = {
                        isRunning = false
                        timeLeft = totalSeconds
                    },
                    modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = "Stop", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                IconButton(
                    onClick = { isRunning = !isRunning },
                    modifier = Modifier.size(80.dp).background(primaryColor, CircleShape)
                ) {
                    Icon(
                        if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}
