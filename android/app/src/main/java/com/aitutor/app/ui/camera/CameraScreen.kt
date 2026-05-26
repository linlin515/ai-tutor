package com.aitutor.app.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.aitutor.app.ui.screen.camera.components.SubjectSelector
import com.google.mlkit.vision.common.InputImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import com.aitutor.app.ui.theme.AiTutorTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGetImage::class)
@Composable
fun CameraScreen(
    onNavigateToChat: (String) -> Unit = {},
    viewModel: CameraViewModel = hiltViewModel()
) {
    val state = viewModel.uiState
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var capturedFileUri by remember { mutableStateOf<Uri?>(null) }

    // OCR result collected from ViewModel
    val ocrResult by viewModel.ocrResult.collectAsState()
    val isOcrProcessing by remember { derivedStateOf { viewModel.isOcrProcessing } }

    // Trigger OCR when a photo is captured
    LaunchedEffect(state.capturedImageUri) {
        val uriStr = state.capturedImageUri
        if (uriStr != null && !state.isAnalyzing) {
            try {
                val inputStream = context.contentResolver.openInputStream(Uri.parse(uriStr))
                if (inputStream != null) {
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    if (bitmap != null) {
                        viewModel.processImageForOcr(bitmap)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            capturedFileUri = uri
            viewModel.onPhotoCaptured(uri.toString())
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Request permission on first composition
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("拍照解题") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.resetState(); onNavigateToChat("") }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!hasCameraPermission) {
                // Permission denied
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "需要相机权限才能拍照解题",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("授予权限")
                    }
                }
            } else if (state.showPreview) {
                // Photo preview
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Show actual image preview using Coil
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.capturedImageUri != null) {
                            AsyncImage(
                                model = state.capturedImageUri,
                                contentDescription = "拍摄的照片",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    // Subject selector (only when not yet analyzing)
                    if (!state.isAnalyzing && state.analysisResult == null) {
                        SubjectSelector(
                            selectedSubject = state.selectedSubject,
                            onSubjectChanged = { viewModel.onSubjectChanged(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (state.isAnalyzing) {
                        if (state.isCompressing) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("图片压缩中...")
                        } else if (state.solveEvents.isNotEmpty()) {
                            // Show live solve events
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.3f)
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "AI 分析中...",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                // Show latest OCR result
                                val lastOcr = state.solveEvents.filterIsInstance<com.aitutor.app.data.remote.dto.SolveEventUi.Ocr>().lastOrNull()
                                if (lastOcr != null) {
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = "识别结果:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = lastOcr.text.take(100) + if (lastOcr.text.length > 100) "..." else "",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }

                                // Show step progress
                                val currentStep = state.solveEvents.filterIsInstance<com.aitutor.app.data.remote.dto.SolveEventUi.Step>().lastOrNull()
                                if (currentStep != null) {
                                    LinearProgressIndicator(
                                        progress = { currentStep.step.toFloat() / currentStep.total.toFloat() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "步骤 ${currentStep.step}/${currentStep.total}: ${currentStep.title}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "正在生成解答...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("AI 分析中...")
                        }
                    } else if (state.analysisResult != null) {
                        // Analysis done
                        Text(
                            text = "分析完成！",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Show latest steps summary
                        val steps = state.solveEvents.filterIsInstance<com.aitutor.app.data.remote.dto.SolveEventUi.Step>()
                        if (steps.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.2f)
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "解题步骤:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                steps.forEach { step ->
                                    Text(
                                        text = "步骤 ${step.step}/${step.total}: ${step.title}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Button(onClick = {
                            val convId = state.conversationId
                            viewModel.resetState()
                            if (convId > 0) {
                                onNavigateToChat("$convId")
                            } else {
                                onNavigateToChat("")
                            }
                        }) {
                            Text("查看解答")
                        }
                    } else if (state.errorMessage != null) {
                        // Error state
                        Text(
                            text = state.errorMessage ?: "分析失败",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OutlinedButton(onClick = { viewModel.retakePhoto() }) {
                                Text("重拍")
                            }
                            Button(onClick = { viewModel.confirmPhoto() }) {
                                Text("重试")
                            }
                        }
                    } else {
                        // OCR result preview (between subject selector and action buttons)
                        if (isOcrProcessing || ocrResult != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "OCR 识别结果",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (isOcrProcessing) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Text(
                                                text = "正在识别文字...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else if (ocrResult != null) {
                                        Text(
                                            text = ocrResult!!,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 5,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Action buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OutlinedButton(onClick = { viewModel.retakePhoto() }) {
                                Text("重拍")
                            }
                            Button(onClick = { viewModel.confirmPhoto() }) {
                                Text("确认使用")
                            }
                        }
                    }
                }
            } else {
                // Camera preview
                Box(modifier = Modifier.fillMaxSize()) {
                    // CameraX Preview + ImageCapture + ImageAnalysis (ML Kit OCR)
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also { p ->
                                        p.setSurfaceProvider(this@apply.surfaceProvider)
                                    }
                                    // Configure ImageCapture
                                    val imageCaptureBuilder = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                        .setTargetRotation(this@apply.display?.rotation ?: 0)
                                        .build()
                                    imageCapture = imageCaptureBuilder

                                    // --- ML Kit OCR: ImageAnalysis use case ---
                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setTargetResolution(android.util.Size(1280, 720))
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .setTargetRotation(this@apply.display?.rotation ?: 0)
                                        .build()
                                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                        analyzeOcrFrame(imageProxy, viewModel)
                                    }

                                    val cameraSelector = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                                        CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            imageCaptureBuilder,
                                            imageAnalysis
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // OCR Canvas overlay: draws bounding boxes on top of camera preview
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawOcrOverlay(
                            viewModel = viewModel,
                            canvasWidth = size.width,
                            canvasHeight = size.height
                        )
                    }

                    // Subject selector overlay at top
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                    ) {
                        SubjectSelector(
                            selectedSubject = state.selectedSubject,
                            onSubjectChanged = { viewModel.onSubjectChanged(it) },
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 8.dp
                            )
                        )
                    }

                    // OCR bottom card: shows detected text floating above controls
                    val ocrBoxes = viewModel.ocrResults
                    if (ocrBoxes.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.7f)
                            )
                        ) {
                            val displayText = ocrBoxes.joinToString("  ") { it.text }
                            Text(
                                text = displayText,
                                modifier = Modifier.padding(12.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Bottom controls
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Gallery button
                        IconButton(
                            onClick = { galleryLauncher.launch("image/*") }
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = "相册",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(32.dp))

                        // Capture button
                        FilledIconButton(
                            onClick = {
                                val capture = imageCapture ?: return@FilledIconButton
                                val photoFile = File(
                                    context.cacheDir,
                                    "captured_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
                                )
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                capture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            val savedUri = Uri.fromFile(photoFile)
                                            capturedFileUri = savedUri
                                            viewModel.onPhotoCaptured(savedUri.toString())
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            exception.printStackTrace()
                                        }
                                    }
                                )
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "拍照",
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(32.dp))

                        // Switch camera
                        IconButton(onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                                CameraSelector.LENS_FACING_FRONT
                            else
                                CameraSelector.LENS_FACING_BACK
                            // Clear existing capture reference to be recreated in recomposition
                            imageCapture = null
                        }) {
                            Icon(
                                Icons.Default.FlipCameraAndroid,
                                contentDescription = "切换摄像头",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ML Kit OCR helper: runs on every camera frame via ImageAnalysis.Analyzer
// ---------------------------------------------------------------------------
@OptIn(ExperimentalGetImage::class)
private fun analyzeOcrFrame(
    imageProxy: ImageProxy,
    viewModel: CameraViewModel
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }

    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

    viewModel.textRecognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            val boxes = mutableListOf<OcrBoundingBox>()
            for (block in visionText.textBlocks) {
                val box = block.boundingBox ?: continue
                boxes.add(
                    OcrBoundingBox(
                        text = block.text,
                        left = box.left,
                        top = box.top,
                        right = box.right,
                        bottom = box.bottom
                    )
                )
            }
            viewModel.onOcrResult(
                boxes = boxes,
                frameWidth = imageProxy.width,
                frameHeight = imageProxy.height,
                rotationDegrees = rotationDegrees
            )
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

// ---------------------------------------------------------------------------
// OCR overlay drawing function (called from Canvas DrawScope)
// ---------------------------------------------------------------------------
private fun DrawScope.drawOcrOverlay(
    viewModel: CameraViewModel,
    canvasWidth: Float,
    canvasHeight: Float
) {
    val boxes = viewModel.ocrResults
    val imgWidth = viewModel.ocrFrameWidth
    val imgHeight = viewModel.ocrFrameHeight
    val rotation = viewModel.ocrRotationDegrees

    if (boxes.isEmpty() || imgWidth <= 0 || imgHeight <= 0) return

    // Effective display dimensions after applying rotation
    val (displayWidth, displayHeight) = if (rotation == 90 || rotation == 270) {
        imgHeight.toFloat() to imgWidth.toFloat()
    } else {
        imgWidth.toFloat() to imgHeight.toFloat()
    }

    val scaleX = canvasWidth / displayWidth
    val scaleY = canvasHeight / displayHeight

    for (box in boxes) {
        // Transform from raw image space to display space (handle rotation)
        val (dl, dt, dr, db) = when (rotation) {
            0 -> {
                val l = box.left.toFloat()
                val t = box.top.toFloat()
                val r = box.right.toFloat()
                val b = box.bottom.toFloat()
                floatArrayOf(l, t, r, b)
            }
            90 -> {
                // Image rotated 90° clockwise: (x, y) -> (y, imgWidth - x)
                val l = box.top.toFloat()
                val t = (imgWidth - box.right).toFloat()
                val r = box.bottom.toFloat()
                val b = (imgWidth - box.left).toFloat()
                floatArrayOf(l, t, r, b)
            }
            180 -> {
                val l = (imgWidth - box.right).toFloat()
                val t = (imgHeight - box.bottom).toFloat()
                val r = (imgWidth - box.left).toFloat()
                val b = (imgHeight - box.top).toFloat()
                floatArrayOf(l, t, r, b)
            }
            270 -> {
                // Image rotated 270° clockwise: (x, y) -> (imgHeight - y, x)
                val l = (imgHeight - box.bottom).toFloat()
                val t = box.left.toFloat()
                val r = (imgHeight - box.top).toFloat()
                val b = box.right.toFloat()
                floatArrayOf(l, t, r, b)
            }
            else -> {
                floatArrayOf(
                    box.left.toFloat(), box.top.toFloat(),
                    box.right.toFloat(), box.bottom.toFloat()
                )
            }
        }

        // Scale to canvas pixel coordinates
        val left = dl * scaleX
        val top = dt * scaleY
        val right = dr * scaleX
        val bottom = db * scaleY

        // Semi-transparent green fill to highlight text region
        drawRect(
            color = Color(0x2200FF00),
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top)
        )

        // Green outline stroke
        drawRect(
            color = Color(0xFF00FF00),
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

// ===== Preview =====
@Preview(name = "拍照解题 预览", showBackground = true, backgroundColor = 0xFF1C1B1F, showSystemUi = false, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "拍照解题 预览 (深色)", showBackground = true, backgroundColor = 0xFFFEFBFF, showSystemUi = false, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCameraScreen() { AiTutorTheme { CameraScreen() } }
