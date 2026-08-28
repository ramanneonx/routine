package com.neonroutine.ui.memory

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.neonroutine.ui.theme.DesignStyle
import com.neonroutine.ui.theme.LocalAppShapes
import com.neonroutine.ui.theme.LocalDesignStyle
import com.neonroutine.ui.theme.glassPanel
import com.neonroutine.ui.viewmodel.TaskViewModel
import com.neonroutine.util.PhotoStorageUtil
import com.neonroutine.util.TimeLapseVideoExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Reload entries every time this screen becomes visible
    // Using a timestamp key so re-entering the tab triggers a fresh load
    var loadKey by remember { mutableLongStateOf(System.currentTimeMillis()) }
    DisposableEffect(Unit) {
        loadKey = System.currentTimeMillis()
        onDispose { }
    }
    LaunchedEffect(loadKey) {
        val today = LocalDate.now()
        viewModel.loadEntriesForRange(today.minusDays(365), today)
    }

    // Filter ALL entries that have a valid existing photo file
    val memoryEntries = remember(entries) {
        entries
            .filter { !it.photoPath.isNullOrBlank() && File(it.photoPath).exists() }
            .sortedBy { it.date }
    }

    val photoPaths = remember(memoryEntries) { memoryEntries.map { it.photoPath!! } }
    val dateLabels = remember(memoryEntries) { memoryEntries.map { it.date } }

    // ── UI State ────────────────────────────────────────────────────────────
    var selectedTab by remember { mutableIntStateOf(0) }  // 0 = Player, 1 = Grid
    var isPlaying by remember { mutableStateOf(false) }
    var currentFrameIndex by remember { mutableIntStateOf(0) }
    var fps by remember { mutableIntStateOf(4) }
    var isLooping by remember { mutableStateOf(true) }

    // Full-screen viewer
    var selectedFullPhotoPath by remember { mutableStateOf<String?>(null) }

    // Export dialog state
    var showExportDialog by remember { mutableStateOf(false) }
    var isExportingVideo by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var exportedVideoFile by remember { mutableStateOf<File?>(null) }

    // Export config (controlled inside dialog)
    var exportSecondsPerPhoto by remember { mutableFloatStateOf(1.5f) }
    var exportShowDateBanner by remember { mutableStateOf(true) }
    var exportShowWatermark by remember { mutableStateOf(true) }
    // Which photos are selected for export (indices); starts as all selected
    var exportSelectedIndices by remember(photoPaths) {
        mutableStateOf(photoPaths.indices.toMutableSet().toSet())
    }

    // Preloaded Bitmaps for butter-smooth playback
    val preloadedFrames = remember { mutableStateMapOf<Int, Bitmap>() }
    var isPreloading by remember { mutableStateOf(false) }

    LaunchedEffect(photoPaths) {
        if (photoPaths.isNotEmpty()) {
            isPreloading = true
            withContext(Dispatchers.IO) {
                photoPaths.forEachIndexed { index, path ->
                    if (!preloadedFrames.containsKey(index)) {
                        val bmp = PhotoStorageUtil.decodeAndCorrectOrientation(path, targetMaxDim = 960)
                        if (bmp != null) preloadedFrames[index] = bmp
                    }
                }
            }
            isPreloading = false
        }
    }

    // Playback ticker
    LaunchedEffect(isPlaying, fps, photoPaths.size) {
        if (isPlaying && photoPaths.isNotEmpty()) {
            val frameDelayMs = (1000L / fps).coerceAtLeast(40L)
            while (isPlaying) {
                delay(frameDelayMs)
                if (currentFrameIndex < photoPaths.size - 1) {
                    currentFrameIndex++
                } else {
                    if (isLooping) currentFrameIndex = 0 else isPlaying = false
                }
            }
        }
    }

    // Share exported video
    fun shareVideo(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Time-Lapse Video"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing video: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Export dialog trigger ────────────────────────────────────────────────
    fun startExport() {
        if (isExportingVideo) return
        val selectedPaths = exportSelectedIndices.sorted().mapNotNull { photoPaths.getOrNull(it) }
        val selectedDates = exportSelectedIndices.sorted().mapNotNull { dateLabels.getOrNull(it) }
        if (selectedPaths.isEmpty()) {
            Toast.makeText(context, "Select at least one photo", Toast.LENGTH_SHORT).show()
            return
        }
        isExportingVideo = true
        exportProgress = 0f
        showExportDialog = false
        scope.launch {
            val result = TimeLapseVideoExporter.exportTimeLapseVideo(
                context = context,
                photoPaths = selectedPaths,
                dateLabels = selectedDates,
                config = TimeLapseVideoExporter.ExportConfig(
                    secondsPerPhoto = exportSecondsPerPhoto,
                    showDateBanner = exportShowDateBanner,
                    showWatermark = exportShowWatermark
                ),
                onProgress = { p -> exportProgress = p.percentage }
            )
            isExportingVideo = false
            if (result != null) {
                exportedVideoFile = result
                shareVideo(result)
            } else {
                Toast.makeText(context, "Export failed — try again", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Main Scaffold ────────────────────────────────────────────────────────
    Scaffold(
        containerColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.Transparent
                         else MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Photographic Memories",
                        fontWeight = FontWeight.Bold,
                        color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White else Color.Unspecified
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                            tint = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (photoPaths.isNotEmpty() && !isExportingVideo) {
                        IconButton(onClick = { showExportDialog = true }) {
                            Icon(
                                Icons.Filled.Movie,
                                contentDescription = "Export Time-Lapse",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (designStyle == DesignStyle.GLASSMORPHISM) Color.Transparent
                                     else MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Empty State ────────────────────────────────────────────────
            if (photoPaths.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt, contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Text(
                            "No selfies logged yet 📸",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White else Color.Unspecified
                        )
                        Text(
                            "Take daily face check-ins using the Camera to build your transformation time-lapse!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (designStyle == DesignStyle.GLASSMORPHISM) Color.White.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {

                // ── Tab bar ───────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("⚡ Time-Lapse Player", "🖼️ Photo Gallery (${photoPaths.size})")
                        .forEachIndexed { index, label ->
                            val isSelected = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                }

                if (selectedTab == 0) {
                    // ── TIME-LAPSE PLAYER ─────────────────────────────────
                    val activeIndex = currentFrameIndex.coerceIn(0, photoPaths.size - 1)
                    val activeBitmap = preloadedFrames[activeIndex]
                    val currentDate = dateLabels.getOrNull(activeIndex) ?: ""

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(appShapes.card)
                            .background(Color.Black)
                            .then(
                                if (designStyle == DesignStyle.BRUTAL_MINIMAL)
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.outline, appShapes.card)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (activeBitmap != null) {
                            Image(
                                bitmap = activeBitmap.asImageBitmap(),
                                contentDescription = "Frame $activeIndex",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(photoPaths[activeIndex])).build(),
                                contentDescription = "Frame $activeIndex",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Day badge (top-left)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart).padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Day ${activeIndex + 1} / ${photoPaths.size} • $currentDate",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold, color = Color.White
                            )
                        }

                        // Play overlay when stopped
                        if (!isPlaying) {
                            IconButton(
                                onClick = { isPlaying = true },
                                modifier = Modifier.size(72.dp)
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                            ) {
                                Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(48.dp))
                            }
                        }

                        // Bottom progress bar
                        Box(
                            modifier = Modifier.align(Alignment.BottomStart)
                                .fillMaxWidth().height(4.dp).background(Color.DarkGray)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((activeIndex + 1).toFloat() / photoPaths.size)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }

                    // Scrubber
                    Slider(
                        value = activeIndex.toFloat(),
                        onValueChange = { isPlaying = false; currentFrameIndex = it.toInt() },
                        valueRange = 0f..(photoPaths.size - 1).toFloat().coerceAtLeast(1f),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Controls row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = { fps = when (fps) { 2 -> 4; 4 -> 8; 8 -> 12; else -> 2 } },
                            label = { Text("${fps}x FPS") }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { isPlaying = false; if (currentFrameIndex > 0) currentFrameIndex-- }) {
                                Icon(Icons.Filled.SkipPrevious, "Prev")
                            }
                            IconButton(
                                onClick = { isPlaying = !isPlaying },
                                modifier = Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    "Play/Pause", tint = Color.White, modifier = Modifier.size(28.dp)
                                )
                            }
                            IconButton(onClick = { isPlaying = false; if (currentFrameIndex < photoPaths.size - 1) currentFrameIndex++ }) {
                                Icon(Icons.Filled.SkipNext, "Next")
                            }
                        }
                        FilterChip(
                            selected = isLooping,
                            onClick = { isLooping = !isLooping },
                            label = { Text(if (isLooping) "🔁 Loop" else "➡️ Once") }
                        )
                    }

                    // Export button
                    Button(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Movie, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export MP4 Time-Lapse…")
                    }

                } else {
                    // ── PHOTO GRID ────────────────────────────────────────
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(photoPaths) { index, path ->
                            val date = dateLabels.getOrNull(index) ?: ""
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.DarkGray)
                                    .clickable { selectedFullPhotoPath = path },
                                contentAlignment = Alignment.BottomStart
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(File(path)).crossfade(true).build(),
                                    contentDescription = "Photo $date",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(date, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Export Config Dialog ─────────────────────────────────────────────────
    if (showExportDialog && photoPaths.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Text("Export Time-Lapse Video", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Seconds per photo slider ──────────────────────────
                    Column {
                        Text(
                            "Duration per photo: ${String.format("%.1f", exportSecondsPerPhoto)}s",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Total video: ~${String.format("%.0f", exportSecondsPerPhoto * exportSelectedIndices.size)}s  •  ${exportSelectedIndices.size} photos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = exportSecondsPerPhoto,
                            onValueChange = { exportSecondsPerPhoto = it },
                            valueRange = 0.5f..5f,
                            steps = 18,  // 0.25s increments
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0.5s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("5s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider()

                    // ── Branding toggles ──────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show date on video", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = exportShowDateBanner, onCheckedChange = { exportShowDateBanner = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show NeonRoutine watermark", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = exportShowWatermark, onCheckedChange = { exportShowWatermark = it })
                    }

                    HorizontalDivider()

                    // ── Photo selection: Select All / None ────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Photos: ${exportSelectedIndices.size} / ${photoPaths.size} selected",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { exportSelectedIndices = photoPaths.indices.toSet() }) {
                                Text("All")
                            }
                            TextButton(onClick = { exportSelectedIndices = emptySet() }) {
                                Text("None")
                            }
                        }
                    }

                    // Mini photo chip grid for selection
                    val chunkedPaths = photoPaths.chunked(6)
                    chunkedPaths.forEachIndexed { rowIdx, chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            chunk.forEachIndexed { colIdx, path ->
                                val globalIdx = rowIdx * 6 + colIdx
                                val isSelected = globalIdx in exportSelectedIndices
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(
                                            2.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            exportSelectedIndices = if (isSelected)
                                                exportSelectedIndices - globalIdx
                                            else
                                                exportSelectedIndices + globalIdx
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current).data(File(path)).crossfade(true).build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (!isSelected) {
                                        Box(
                                            modifier = Modifier.fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.5f))
                                        )
                                    }
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier.size(16.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                .align(Alignment.TopEnd).offset((-2).dp, 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        "⚠️ Video saves to app only — use the share sheet to send/save it.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { startExport() },
                    enabled = exportSelectedIndices.isNotEmpty()
                ) {
                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export ${exportSelectedIndices.size} Photos")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Export Progress Dialog ───────────────────────────────────────────────
    if (isExportingVideo) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Encoding Time-Lapse…") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { exportProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${(exportProgress * 100).toInt()}%  •  H.264 MP4",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Don't close the app…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {}
        )
    }

    // ── Full-Screen Photo Viewer ─────────────────────────────────────────────
    selectedFullPhotoPath?.let { fullPath ->
        AlertDialog(
            onDismissRequest = { selectedFullPhotoPath = null },
            title = { Text("Photo Preview") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth().height(380.dp)
                        .clip(RoundedCornerShape(12.dp)).background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(File(fullPath)).build(),
                        contentDescription = "Full Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedFullPhotoPath = null }) { Text("Close") }
            }
        )
    }
}
