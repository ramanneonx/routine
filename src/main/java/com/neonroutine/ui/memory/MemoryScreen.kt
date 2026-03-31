package com.neonroutine.ui.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.neonroutine.data.model.Task
import com.neonroutine.ui.theme.DesignStyle
import com.neonroutine.ui.theme.LocalAppShapes
import com.neonroutine.ui.theme.LocalDesignStyle
import com.neonroutine.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    viewModel: TaskViewModel,
    onBack: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val entries by viewModel.entriesInRange.collectAsState()
    val appShapes = LocalAppShapes.current
    val designStyle = LocalDesignStyle.current

    // Load last 30 days on open
    LaunchedEffect(Unit) {
        val today = java.time.LocalDate.now()
        viewModel.loadEntriesForRange(today.minusDays(30), today)
    }

    // UI states
    var isPlaying by remember { mutableStateOf(false) }
    var currentFrameIndex by remember { mutableIntStateOf(0) }
    
    val context = LocalContext.current
    
    // Filter ALL entries that have a photo
    val taskPhotos = remember(entries) {
        entries
            .filter { !it.photoPath.isNullOrBlank() }
            .sortedBy { it.date }
            .mapNotNull { it.photoPath }
    }

    // Flipbook Engine (3 FPS -> 333ms per frame)
    LaunchedEffect(isPlaying) {
        if (isPlaying && taskPhotos.isNotEmpty()) {
            currentFrameIndex = 0
            while (currentFrameIndex < taskPhotos.size) {
                delay(333) // 3 frames per second approx
                currentFrameIndex++
            }
            isPlaying = false
            currentFrameIndex = 0 // reset visually or hold last frame?
        }
    }

    Scaffold(
        containerColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.Transparent else MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Memory Time-Lapse", 
                        fontWeight = FontWeight.Bold,
                        color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White else Color.Unspecified
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.Transparent else MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (taskPhotos.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "No selfies logged yet.\nTap the camera icon on the habit grid to start recording your progress!",
                        color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White.copy(alpha=0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "All Memories Timeline",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White else Color.Unspecified
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(appShapes.card)
                        .background(Color.Black)
                        .then(if (designStyle == DesignStyle.BRUTAL_MINIMAL) Modifier.border(2.dp, MaterialTheme.colorScheme.outline, appShapes.card) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isPlaying && currentFrameIndex == 0) {
                        // Display the cover photo (first entry) overlayed with a Play button
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(File(taskPhotos.first()))
                                .crossfade(true)
                                .build(),
                            contentDescription = "Cover Frame",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { isPlaying = true },
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play Frame", tint = Color.White, modifier = Modifier.size(48.dp))
                        }
                    } else {
                        // Display the current frame exactly without crossfade for sharp time lapse snap
                        val idx = currentFrameIndex.coerceAtMost(taskPhotos.size - 1)
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(File(taskPhotos[idx]))
                                .build(),
                            contentDescription = "Frame $idx",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Progress bar
                        Box(
                            modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 8.dp).fillMaxWidth()
                        ) {
                            val progress = (idx + 1).toFloat() / taskPhotos.size
                            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.DarkGray)) {
                                Box(modifier = Modifier.fillMaxWidth(progress).height(4.dp).background(MaterialTheme.colorScheme.primary))
                            }
                        }
                    }
                }

                if (!isPlaying) {
                    Button(
                        onClick = { isPlaying = true; currentFrameIndex = 0 },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = appShapes.card,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                        Spacer(Modifier.width(8.dp))
                        Text("Generate Time-Lapse (${taskPhotos.size} Days)")
                    }
                }
            }
        }
    }
}
