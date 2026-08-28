package com.neonroutine.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.neonroutine.data.model.*
import com.neonroutine.ui.theme.DesignStyle
import com.neonroutine.ui.theme.LocalAppShapes
import com.neonroutine.ui.theme.LocalDesignStyle
import com.neonroutine.ui.theme.glassHover
import com.neonroutine.ui.theme.glassPanel
import com.neonroutine.ui.viewmodel.TaskViewModel
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: TaskViewModel,
    onAddTask: () -> Unit = {},
    onEditTask: (String) -> Unit = {},
    onNavigateToTimer: (String, Int) -> Unit = { _, _ -> },
    onNavigateToCamera: (String) -> Unit = {}
) {
    val tasks by viewModel.tasks.collectAsState()
    val stats by viewModel.statsData.collectAsState()
    val entriesMap by viewModel.entriesForDate.collectAsState()
    val entriesRange by viewModel.entriesInRange.collectAsState()
    val today = LocalDate.now()
    val greetingHour = java.time.LocalTime.now().hour
    val appShapes = LocalAppShapes.current
    val designStyle = LocalDesignStyle.current
    val context = LocalContext.current

    // Custom greeting and quote from Settings
    val appPrefs = remember { (context.applicationContext as com.neonroutine.NeonRoutineApp).appPreferences }
    val customGreeting by appPrefs.homeGreeting.collectAsState()
    val motivationQuote by appPrefs.motivationQuote.collectAsState()

    val yearMonth = YearMonth.of(today.year, today.month)
    LaunchedEffect(yearMonth) {
        viewModel.loadEntriesForRange(today.withDayOfMonth(1), today.withDayOfMonth(yearMonth.lengthOfMonth()))
    }
    
    val fullEntryMap = remember(entriesRange) {
        entriesRange.groupBy { it.taskId }.mapValues { (_, list) -> list.associateBy { it.date } }
    }

    var currentPhotoTask by remember { mutableStateOf<Task?>(null) }
    var currentPhotoFile by remember { mutableStateOf<File?>(null) }
    var showPhotoDialogForTask by remember { mutableStateOf<Task?>(null) }
    var showTimerSelectDialogForTask by remember { mutableStateOf<Task?>(null) }
    var longPressedTask by remember { mutableStateOf<Task?>(null) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentPhotoTask != null && currentPhotoFile != null) {
            viewModel.savePhotoToEntry(currentPhotoTask!!.id, today, currentPhotoFile!!.absolutePath)
        }
        currentPhotoTask = null
        currentPhotoFile = null
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && currentPhotoTask != null) {
            val taskSnapshot = currentPhotoTask
            currentPhotoTask = null
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val savedPath = com.neonroutine.util.PhotoStorageUtil.importFromGallery(
                    context = context,
                    uri = uri,
                    taskId = taskSnapshot!!.id,
                    date = today
                )
                if (savedPath != null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        viewModel.savePhotoToEntry(taskSnapshot.id, today, savedPath)
                    }
                }
            }
        } else {
            currentPhotoTask = null
        }
    }

    // Time-based greeting prefix + custom message from Settings
    val timeGreeting = when {
        greetingHour < 12 -> "Good morning! ☀️"
        greetingHour < 17 -> "Good afternoon! 👋"
        else -> "Good evening! 🌙"
    }
    // If user has customized the greeting, show it; otherwise fall back to time-based
    val greeting = if (customGreeting != com.neonroutine.data.prefs.AppPreferences.DEFAULT_HOME_GREETING) customGreeting else timeGreeting

    val scheduledTasks = remember(tasks) { tasks.filter { viewModel.isTaskScheduledForDate(it, today) } }
    val completedCount = remember(scheduledTasks, entriesMap) {
        scheduledTasks.count { task ->
            val e = entriesMap[task.id]
            e?.completionState == CompletionState.COMPLETED
        }
    }
    val partialCount = remember(scheduledTasks, entriesMap) {
        scheduledTasks.count { task ->
            val e = entriesMap[task.id]
            e?.completionState == CompletionState.PARTIAL
        }
    }
    val totalCount = scheduledTasks.size
    val completionPct = if (totalCount > 0) ((completedCount + partialCount * 0.5f) / totalCount) else 0f

    val streak = stats.streak
    val totalPoints = stats.totalPoints

    val level = (totalPoints / 100) + 1
    val levelProgress = (totalPoints % 100) / 100f

    longPressedTask?.let { task ->
        AlertDialog(
            onDismissRequest = { longPressedTask = null },
            title = { Text(task.title, fontWeight = FontWeight.Bold) },
            text = { Text("What would you like to do?") },
            confirmButton = {
                Button(
                    onClick = {
                        longPressedTask = null
                        onEditTask(task.id)
                    }
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit Habit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        longPressedTask = null
                        viewModel.archiveTask(task.id)
                    }
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text("Archive", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showPhotoDialogForTask != null) {
            val task = showPhotoDialogForTask!!
            AlertDialog(
                onDismissRequest = { showPhotoDialogForTask = null },
                title = { Text("Add Photo for ${task.title}") },
                text = { Text("Choose a source for your photographic memory.") },
                confirmButton = {
                    TextButton(onClick = {
                        showPhotoDialogForTask = null
                        onNavigateToCamera(task.id)
                    }) {
                        Text("Face Camera")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        currentPhotoTask = task
                        showPhotoDialogForTask = null
                        galleryLauncher.launch("image/*")
                    }) {
                        Text("Gallery")
                    }
                }
            )
        }

        if (showTimerSelectDialogForTask != null) {
            val task = showTimerSelectDialogForTask!!
            val timersList = try { kotlinx.serialization.json.Json.decodeFromString<List<String>>(task.timersJson) } catch(e:Exception){ emptyList() }
            AlertDialog(
                onDismissRequest = { showTimerSelectDialogForTask = null },
                title = { Text("Select Timer") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        timersList.forEachIndexed { index, timerStr ->
                            val parts = timerStr.split("|")
                            val mins = parts.getOrNull(0) ?: "0"
                            val lbl = parts.getOrNull(1) ?: "Timer"
                            Button(
                                onClick = {
                                    showTimerSelectDialogForTask = null
                                    onNavigateToTimer(task.id, index)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(Icons.Filled.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.width(8.dp))
                                Text("$mins min - $lbl", color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTimerSelectDialogForTask = null }) { Text("Cancel") }
                }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "2026 IS GONE BE MINE",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "⏰ I WILL BE THE MOST DISCIPLINED EVER 🥇",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(greeting, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (motivationQuote.isNotBlank()) {
                                Text(
                                    motivationQuote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        Text(
                            today.dayOfWeek.getDisplayName(JavaTextStyle.FULL, Locale.getDefault()) + ", " +
                                    today.format(DateTimeFormatter.ofPattern("MMM d")),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = completionPct,
                        animationSpec = tween(1200, delayMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        label = "progressRing"
                    )
                    
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            strokeWidth = 14.dp,
                            strokeCap = StrokeCap.Round
                        )
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 14.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${(animatedProgress * 100).roundToInt()}%",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "COMPLETED",
                                style = MaterialTheme.typography.labelMedium,
                                letterSpacing = 2.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "$completedCount of $totalCount Habits",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatMiniCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.LocalFireDepartment,
                        iconColor = Color(0xFFFF5722),
                        value = "$streak",
                        label = "Day Streak"
                    )
                    StatMiniCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.EmojiEvents,
                        iconColor = Color(0xFFFFC107),
                        value = "Lvl $level",
                        label = "Level"
                    )
                    StatMiniCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Star,
                        iconColor = Color(0xFF9C27B0),
                        value = "$totalPoints",
                        label = "Points"
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .then(
                            if (designStyle == DesignStyle.GLASSMORPHISM) {
                                Modifier.glassPanel(shape = RoundedCornerShape(16.dp))
                            } else Modifier
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.Transparent 
                                         else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(if (designStyle == DesignStyle.GLASSMORPHISM) 0.dp else 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Level $level Progress", 
                                style = MaterialTheme.typography.titleSmall, 
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${(levelProgress * 100).toInt()}% to Lvl ${level + 1}", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        val animLevelPct by animateFloatAsState(targetValue = levelProgress, animationSpec = tween(800), label = "lvl")
                        LinearProgressIndicator(
                            progress = { animLevelPct },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
                            strokeCap = StrokeCap.Round,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Today's Habits", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${scheduledTasks.size} habits", 
                        style = MaterialTheme.typography.labelMedium, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val grouped = scheduledTasks.groupBy { it.category }
            HabitCategory.entries.forEach { cat ->
                val catTasks = grouped[cat] ?: return@forEach
                item(key = "home_cat_${cat.name}") {
                    Text(
                        "${cat.emoji} ${cat.label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 20.dp, top = 4.dp)
                    )
                }
                items(catTasks, key = { "home_${it.id}" }, contentType = { "habit_card" }) { task ->
                    val entry = entriesMap[task.id]
                    val state = entry?.completionState ?: CompletionState.NONE
                    val taskColor = try { Color(android.graphics.Color.parseColor(task.color)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }

                    val habitInteractionSource = remember { MutableInteractionSource() }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .then(
                                if (designStyle == DesignStyle.GLASSMORPHISM) {
                                    Modifier.glassPanel(shape = appShapes.card)
                                        .glassHover(habitInteractionSource)
                                } else Modifier
                            )
                            .combinedClickable(
                                interactionSource = habitInteractionSource,
                                indication = LocalIndication.current,
                                onClick = {},
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    longPressedTask = task
                                }
                            ),
                        shape = appShapes.card,
                        colors = CardDefaults.cardColors(
                            containerColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.Transparent 
                                             else MaterialTheme.colorScheme.surface
                        ),
                        border = if (designStyle == DesignStyle.BRUTAL_MINIMAL) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline) else null,
                        elevation = CardDefaults.cardElevation(if (designStyle == DesignStyle.BRUTAL_MINIMAL || designStyle == DesignStyle.GLASSMORPHISM) 0.dp else appShapes.cardElevation)
                    ) {
                        Row(
                            modifier = Modifier
                                .background(Brush.horizontalGradient(listOf(taskColor.copy(0.08f), Color.Transparent)))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(appShapes.card).background(taskColor.copy(0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cat.emoji, fontSize = 18.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    task.title, 
                                    style = MaterialTheme.typography.titleSmall, 
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "+${task.pointsValue} pts • ${cat.label}", 
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Camera/Gallery selfie button
                            if (state == CompletionState.COMPLETED) {
                                IconButton(
                                    onClick = { showPhotoDialogForTask = task },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.CameraAlt,
                                        contentDescription = "Log Selfie",
                                        tint = if (entry?.photoPath.isNullOrBlank()) {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        } else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                            
                            val timersList = try { kotlinx.serialization.json.Json.decodeFromString<List<String>>(task.timersJson) } catch(e:Exception){ emptyList() }
                            if (timersList.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        if (timersList.size == 1) onNavigateToTimer(task.id, 0)
                                        else showTimerSelectDialogForTask = task
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Filled.Timer, contentDescription = "Start Timer", tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.width(8.dp))
                            }

                            // State cycler button
                            CycleStateButton(state = state, color = taskColor) {
                                val dateStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                viewModel.cycleGridState(task.id, dateStr, state)
                            }
                        }
                    }
                }
            }

            if (scheduledTasks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎯", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No habits for today!", 
                            style = MaterialTheme.typography.titleMedium, 
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Tap + to add your first habit", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant, 
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddTask,
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .then(if (designStyle == DesignStyle.GLASSMORPHISM) Modifier.glassHover() else Modifier)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Habit", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
fun CycleStateButton(state: CompletionState, color: Color, onClick: () -> Unit) {
    val appShapes = LocalAppShapes.current
    val designStyle = LocalDesignStyle.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    
    val (bgColor, label) = when (state) {
        CompletionState.COMPLETED -> Color(0xFF4CAF50) to "✓"
        CompletionState.PARTIAL -> Color(0xFFFF9800) to "~"
        CompletionState.MISSED -> Color(0xFFF44336) to "✗"
        CompletionState.NONE -> color.copy(0.15f) to "○"
    }
    val animBg by animateColorAsState(targetValue = bgColor, animationSpec = tween(300), label = "stateColor")

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.78f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "btnScale"
    )
    
    val borderModifier = if (designStyle == DesignStyle.BRUTAL_MINIMAL) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.outline, appShapes.card)
    } else Modifier

    Box(
        modifier = Modifier
            .size(36.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(appShapes.card)
            .background(animBg)
            .then(borderModifier)
            .clickable {
                try { view.playSoundEffect(android.view.SoundEffectConstants.CLICK) } catch (_: Exception) {}
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                pressed = true
                onClick()
                pressed = false
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = label,
            transitionSpec = {
                (fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.8f))
                    .togetherWith(fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 1.2f))
            },
            label = "btnLabelAnim"
        ) { targetLabel ->
            Text(
                targetLabel,
                style = MaterialTheme.typography.labelLarge,
                color = if (state == CompletionState.NONE) color else Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatMiniCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    value: String,
    label: String
) {
    val appShapes = LocalAppShapes.current
    val designStyle = LocalDesignStyle.current
    
    Card(
        modifier = modifier
            .then(
                if (designStyle == DesignStyle.GLASSMORPHISM) {
                    Modifier.glassPanel(shape = appShapes.card).glassHover()
                } else Modifier
            ),
        shape = appShapes.card,
        colors = CardDefaults.cardColors(
            containerColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.Transparent 
                             else if (designStyle == DesignStyle.BRUTAL_MINIMAL) MaterialTheme.colorScheme.surface 
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (designStyle == DesignStyle.BRUTAL_MINIMAL) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline) else null,
        elevation = CardDefaults.cardElevation(if (designStyle == DesignStyle.BRUTAL_MINIMAL || designStyle == DesignStyle.GLASSMORPHISM) 0.dp else appShapes.cardElevation)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                value, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                label, 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                fontSize = 9.sp
            )
        }
    }
}

@Composable
fun MonthlyTrendLineChart(entriesMap: Map<String, Map<String, Entry>>, tasks: List<Task>, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val daysInMonth = YearMonth.of(today.year, today.month).lengthOfMonth()
    
    val dailyProgress = (1..daysInMonth).map { day ->
        val date = today.withDayOfMonth(day)
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val scheduled = tasks.filter { task ->
            // In a real app we'd use viewModel.isTaskScheduledForDate but we don't have viewModel here directly
            // so we'll just check if the task wasn't created strictly after this date.
            // For simplicity in UI, we just count completed entries across all tasks that exist.
            true 
        }
        val completed = scheduled.count { task ->
            val entriesForTask = entriesMap[task.id]
            entriesForTask?.get(dateStr)?.completionState == CompletionState.COMPLETED
        }
        completed.toFloat()
    }
    
    val maxVal = maxOf(1f, dailyProgress.maxOrNull() ?: 1f)
    
    val lineColor = MaterialTheme.colorScheme.primary
    val gradientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / (dailyProgress.size - 1).coerceAtLeast(1)
        
        val path = Path()
        val fillPath = Path()
        
        dailyProgress.forEachIndexed { i, value ->
            val x = i * stepX
            val y = height - (value / maxVal * height)
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        
        fillPath.lineTo(width, height)
        fillPath.lineTo(0f, height)
        fillPath.close()
        
        drawPath(fillPath, brush = Brush.verticalGradient(listOf(gradientColor, Color.Transparent)))
        drawPath(path, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        
        // Draw dots
        dailyProgress.forEachIndexed { i, value ->
            val x = i * stepX
            val y = height - (value / maxVal * height)
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(x, y))
            drawCircle(color = lineColor, radius = 2.5.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
fun DashboardAnalyticsRow(entriesMap: Map<String, Map<String, Entry>>, tasks: List<Task>) {
    val today = LocalDate.now()
    val daysInMonth = YearMonth.of(today.year, today.month).lengthOfMonth()
    
    val dailyProgress = (1..daysInMonth).map { day ->
        val date = today.withDayOfMonth(day)
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val completed = tasks.count { task ->
            entriesMap[task.id]?.get(dateStr)?.completionState == CompletionState.COMPLETED
        }
        val total = tasks.size
        if (total > 0) completed.toFloat() / total else 0f
    }
    
    val weeks = dailyProgress.chunked(7)
    val weeklyAverages = weeks.map { weekDays -> 
        if (weekDays.isEmpty()) 0f else weekDays.average().toFloat() 
    }
    
    val monthlyTotal = if (dailyProgress.isNotEmpty()) dailyProgress.average().toFloat() else 0f
    val designStyle = LocalDesignStyle.current
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // Bar Chart
        Card(
            modifier = Modifier.weight(1f)
                .then(
                    if (designStyle == DesignStyle.GLASSMORPHISM) {
                        Modifier.glassPanel(shape = RoundedCornerShape(16.dp))
                    } else Modifier
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.Transparent 
                                 else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(if (designStyle == DesignStyle.GLASSMORPHISM) 0.dp else 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "WEEKLY AVERAGES", 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.Bold,
                    color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White else Color.Unspecified
                )
                Spacer(Modifier.height(12.dp))
                WeeklyBarChart(weeklyAverages, modifier = Modifier.fillMaxWidth().height(100.dp))
            }
        }
        
        // Circular Progress
        Card(
            modifier = Modifier.weight(1f)
                .then(
                    if (designStyle == DesignStyle.GLASSMORPHISM) {
                        Modifier.glassPanel(shape = RoundedCornerShape(16.dp))
                    } else Modifier
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.Transparent 
                                 else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(if (designStyle == DesignStyle.GLASSMORPHISM) 0.dp else 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "MONTHLY OVERALL", 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.Bold,
                    color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White else Color.Unspecified
                )
                Spacer(Modifier.height(12.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                    CircularProgressIndicator(
                        progress = { monthlyTotal },
                        modifier = Modifier.size(100.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha=0.15f),
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        "${(monthlyTotal * 100).roundToInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White else Color.Unspecified
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyBarChart(weeklyAverages: List<Float>, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barCount = weeklyAverages.size
        val spacing = 8.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = (width - totalSpacing) / barCount
        
        weeklyAverages.forEachIndexed { i, avg ->
            val x = i * (barWidth + spacing)
            val barHeight = avg * height
            val y = height - barHeight
            
            drawRoundRect(
                color = barColor.copy(alpha = 0.15f),
                topLeft = Offset(x, 0f),
                size = androidx.compose.ui.geometry.Size(barWidth, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
            
            if (barHeight > 0) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
            }
        }
    }
}
