package com.neonroutine.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.neonroutine.util.PhotoStorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraOverlayScreen(
    outputFile: File,
    lastPhotoPath: String? = null,
    onPhotoTaken: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current

    var permissionStatus by remember {
        mutableIntStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        )
    }
    val hasPermission = permissionStatus == PackageManager.PERMISSION_GRANTED
    var permissionDeniedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionStatus = if (isGranted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        if (!isGranted) permissionDeniedOnce = true
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasPermission) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Camera Permission") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("📷", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(24.dp))
                Text(
                    "Camera Access Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "NeonRoutine requires camera access to align and capture your daily face transformation time-lapse photos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))

                if (permissionDeniedOnce) {
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open App Settings")
                    }
                } else {
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Grant Camera Permission")
                    }
                }
            }
        }
        return
    }

    // Camera settings
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    val cameraSelector = remember(lensFacing) {
        CameraSelector.Builder().requireLensFacing(lensFacing).build()
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var isCapturing by remember { mutableStateOf(false) }

    // Ghost / Onion skin overlay of last photo for face alignment
    var ghostOpacity by remember { mutableFloatStateOf(0.35f) }
    var showGhostOverlay by remember { mutableStateOf(lastPhotoPath != null) }
    var lastPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(lastPhotoPath) {
        if (!lastPhotoPath.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                lastPhotoBitmap = PhotoStorageUtil.decodeAndCorrectOrientation(lastPhotoPath, targetMaxDim = 720)
            }
        }
    }

    fun takePhoto() {
        if (isCapturing) return
        isCapturing = true
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

        outputFile.parentFile?.mkdirs()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        imageCapture.takePicture(
            outputOptions, executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    ContextCompat.getMainExecutor(context).execute {
                        isCapturing = false
                        onPhotoTaken(outputFile.absolutePath)
                    }
                }
                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                    exception.printStackTrace()
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Camera Preview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            update = { previewView ->
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                } catch (_: Exception) {}
            }
        )

        // Ghost Overlay (Last photo onion-skin for alignment)
        if (showGhostOverlay && lastPhotoBitmap != null) {
            Image(
                bitmap = lastPhotoBitmap!!.asImageBitmap(),
                contentDescription = "Alignment Ghost",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(ghostOpacity)
            )
        }

        // Face Stencil + Eye level & Center alignment guides
        Canvas(modifier = Modifier.fillMaxSize()) {
            val overlayColor = Color.Black.copy(alpha = 0.55f)
            val strokeColor = Color.Cyan.copy(alpha = 0.95f)
            val guideColor = Color.Cyan.copy(alpha = 0.45f)

            val cW = size.width
            val cH = size.height
            val ovalW = cW * 0.65f
            val ovalH = cH * 0.42f
            val cx = cW / 2f
            val cy = cH / 2.5f

            // Cutout oval
            val path = Path().apply {
                addRect(Rect(0f, 0f, cW, cH))
                addOval(Rect(cx - ovalW / 2f, cy - ovalH / 2f, cx + ovalW / 2f, cy + ovalH / 2f))
                fillType = PathFillType.EvenOdd
            }
            drawPath(path, color = overlayColor)

            // Dashed Face Oval
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 14f), 0f)
            drawOval(
                color = strokeColor,
                topLeft = Offset(cx - ovalW / 2f, cy - ovalH / 2f),
                size = Size(ovalW, ovalH),
                style = Stroke(width = 5f, pathEffect = dashEffect)
            )

            // Eye-level horizontal alignment guide
            val eyeY = cy - (ovalH * 0.12f)
            drawLine(
                color = guideColor,
                start = Offset(cx - ovalW * 0.35f, eyeY),
                end = Offset(cx + ovalW * 0.35f, eyeY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )

            // Vertical center symmetry guide
            drawLine(
                color = guideColor,
                start = Offset(cx, cy - ovalH * 0.40f),
                end = Offset(cx, cy + ovalH * 0.40f),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )
        }

        // Top Navigation & Actions Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            // Flip Camera (Front / Back)
            IconButton(
                onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                        CameraSelector.LENS_FACING_BACK
                    } else {
                        CameraSelector.LENS_FACING_FRONT
                    }
                },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Filled.Cameraswitch, contentDescription = "Flip Camera", tint = Color.White)
            }
        }

        // Guidance pill
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                "Align eyes & nose to center line",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Ghost opacity slider (when previous photo is available)
        if (lastPhotoBitmap != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ghost Overlay", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp))
                Switch(
                    checked = showGhostOverlay,
                    onCheckedChange = { showGhostOverlay = it },
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Capture Shutter Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            IconButton(
                onClick = { takePhoto() },
                enabled = !isCapturing,
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White, CircleShape)
                    .padding(6.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                } else {
                    Icon(
                        Icons.Filled.CameraAlt,
                        contentDescription = "Take Photo",
                        modifier = Modifier.size(36.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}
