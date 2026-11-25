package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.AppLanguage
import com.example.core.DigitHelper
import com.example.core.LocalLanguage
import com.example.core.LocalStrings
import com.example.data.MathLayoutType
import com.example.data.MathSourceType
import com.example.domain.CameraMathRecognizer
import com.example.domain.MathSolveResult
import com.example.ui.components.MsRsTopBar
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalMsRsColors
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CameraSolverScreen(
    onBack: () -> Unit,
    onOpenCalculator: () -> Unit,
    onOpenSolution: (Long) -> Unit,
    onSolveExpression: (
        raw: String,
        useBangla: Boolean,
        source: MathSourceType,
        layout: MathLayoutType?,
        onSolved: (Long, MathSolveResult.Success) -> Unit,
        onError: (MathSolveResult.Error) -> Unit
    ) -> Unit
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val strings = LocalStrings.current
    val lang = LocalLanguage.current
    val ext = LocalMsRsColors.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var flashEnabled by remember { mutableStateOf(false) }
    var autoScanEnabled by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var useBanglaDigits by remember(lang) { mutableStateOf(lang == AppLanguage.BANGLA) }
    var expressionText by remember { mutableStateOf("") }
    var detectedLayout by remember { mutableStateOf<MathLayoutType?>(null) }
    var lastSavedId by remember { mutableStateOf<Long?>(null) }
    var lastSolution by remember { mutableStateOf<MathSolveResult.Success?>(null) }
    var lastAutoScanMs by remember { mutableLongStateOf(0L) }

    var cameraControl by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    fun triggerSolve(raw: String, layout: MathLayoutType?) {
        onSolveExpression(
            raw,
            useBanglaDigits,
            MathSourceType.CAMERA,
            layout,
            { savedId, success ->
                lastSavedId = savedId
                lastSolution = success
            },
            { err ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (err.isDivideByZero) strings.divideByZeroError else strings.invalidExpression
                    )
                }
            }
        )
    }

    fun processBitmap(bmp: Bitmap) {
        if (isProcessing) return
        isProcessing = true
        scope.launch {
            runCatching {
                val cropped = cropCenterBand(bmp)
                CameraMathRecognizer.recognizeFromBitmap(cropped)
            }.onSuccess { rec ->
                isProcessing = false
                if (rec.expression.isNotBlank()) {
                    expressionText = if (useBanglaDigits) {
                        DigitHelper.toBanglaDigits(rec.expression)
                    } else {
                        DigitHelper.toEnglishDigits(rec.expression)
                    }
                    detectedLayout = rec.detectedLayout
                    triggerSolve(rec.expression, rec.detectedLayout)
                } else {
                    snackbarHostState.showSnackbar(strings.invalidExpression)
                }
            }.onFailure {
                isProcessing = false
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri)) { decoder, _, _ ->
                            decoder.isMutableRequired = true
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(ctx.contentResolver, uri)
                    }
                }.getOrNull()
                if (bmp != null) processBitmap(bmp)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(modifier = Modifier.statusBarsPadding()) {
                MsRsTopBar(
                    title = strings.cameraSolverTitle,
                    onBack = onBack,
                    actions = {
                        IconButton(
                            onClick = onOpenCalculator,
                            modifier = Modifier.testTag("camera_to_calc_btn")
                        ) {
                            Icon(Icons.Filled.Calculate, contentDescription = strings.quickCalculator, tint = Color.White)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Camera Preview Window
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF0A1D12))
                    .border(2.dp, ext.gold, RoundedCornerShape(22.dp))
            ) {
                if (hasCameraPermission) {
                    AndroidView(
                        factory = { context ->
                            val previewView = PreviewView(context)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                            cameraProviderFuture.addListener({
                                val provider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCapture = capture

                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                analysis.setAnalyzer(analysisExecutor) { proxy ->
                                    val now = System.currentTimeMillis()
                                    if (autoScanEnabled && !isProcessing && now - lastAutoScanMs > 2400L) {
                                        lastAutoScanMs = now
                                        val bmp = proxy.toBitmap()
                                        processBitmap(bmp)
                                    }
                                    proxy.close()
                                }

                                runCatching {
                                    provider.unbindAll()
                                    cameraControl = provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        capture,
                                        analysis
                                    )
                                }
                            }, ContextCompat.getMainExecutor(context))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Target Viewfinder Box
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.84f)
                            .height(112.dp)
                            .border(2.dp, ext.goldBright, RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    ) {
                        Text(
                            text = strings.pointCameraPrompt,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Flash & Auto-Scan overlay chips
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = if (autoScanEnabled) ext.goldBright else Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { autoScanEnabled = !autoScanEnabled }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = strings.autoScan,
                                    tint = if (autoScanEnabled) Color(0xFF102018) else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = strings.autoScan,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (autoScanEnabled) Color(0xFF102018) else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                flashEnabled = !flashEnabled
                                cameraControl?.cameraControl?.enableTorch(flashEnabled)
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                        ) {
                            Icon(
                                imageVector = if (flashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                contentDescription = strings.flash,
                                tint = if (flashEnabled) ext.goldBright else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = ext.goldBright,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = strings.cameraPermissionTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = strings.cameraPermissionDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = ext.goldBright, contentColor = Color(0xFF112219))
                        ) {
                            Text(strings.grantPermission, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ext.goldBright)
                    }
                }
            }

            // Capture & Gallery Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val cap = imageCapture
                        if (cap != null && hasCameraPermission) {
                            isProcessing = true
                            cap.takePicture(
                                ContextCompat.getMainExecutor(ctx),
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val bmp = image.toBitmap()
                                        image.close()
                                        isProcessing = false
                                        processBitmap(bmp)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        isProcessing = false
                                    }
                                }
                            )
                        }
                    },
                    enabled = hasCameraPermission && !isProcessing,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("camera_scan_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ext.solverBlueStart)
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.scanNow, maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.testTag("camera_pick_photo_btn")
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.pickPhoto, maxLines = 1)
                }
            }

            // Digit System & Expression Editor
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = useBanglaDigits,
                            onClick = {
                                useBanglaDigits = true
                                expressionText = DigitHelper.toBanglaDigits(expressionText)
                            },
                            label = { Text(strings.banglaDigitsLabel) }
                        )
                        FilterChip(
                            selected = !useBanglaDigits,
                            onClick = {
                                useBanglaDigits = false
                                expressionText = DigitHelper.toEnglishDigits(expressionText)
                            },
                            label = { Text(strings.englishDigitsLabel) }
                        )
                    }

                    Text(
                        text = strings.supportsBothDigits,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = expressionText,
                        onValueChange = { expressionText = it },
                        label = { Text(strings.detectedExpression) },
                        placeholder = { Text(if (useBanglaDigits) "উদা: ২৫ + ১৭ বা ২ ) ৬ (" else "e.g. 25 + 17 or 2 ) 6 (") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("camera_expression_input")
                    )

                    // Quick math tokens helper row for easy tapping
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickSamples = if (useBanglaDigits) {
                            listOf("+", "-", "×", "÷", ")", "(", "১+১", "৬÷২")
                        } else {
                            listOf("+", "-", "×", "÷", ")", "(", "1+1", "6÷2")
                        }
                        quickSamples.forEach { tok ->
                            Surface(
                                color = ext.solverBlueContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (tok.length > 1) expressionText = tok else expressionText += tok
                                    }
                            ) {
                                Text(
                                    text = tok,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ext.onSolverBlueContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { triggerSolve(expressionText, detectedLayout) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("camera_solve_submit_btn")
                    ) {
                        Text(strings.solveNow, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Instant Khata Solution Preview Card
            val sol = lastSolution
            val savedId = lastSavedId
            if (sol != null && savedId != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = ext.khataPaper),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.khataSolutionTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { onOpenSolution(savedId) },
                                modifier = Modifier.testTag("camera_eye_solution_btn")
                            ) {
                                Icon(Icons.Filled.Visibility, contentDescription = strings.viewSolution)
                                Spacer(Modifier.width(6.dp))
                                Text(strings.viewSolution)
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ext.khataLine),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                sol.khataLines.forEach { line ->
                                    Text(
                                        text = line,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        sol.explanationSteps.forEach { step ->
                            Text(
                                text = "• $step",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "✓ ${strings.autoSavedHistory}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.passGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun cropCenterBand(src: Bitmap): Bitmap {
    val w = src.width
    val h = src.height
    val cw = (w * 0.85f).toInt().coerceAtLeast(1)
    val ch = (h * 0.45f).toInt().coerceAtLeast(1)
    val x = ((w - cw) / 2).coerceAtLeast(0)
    val y = ((h - ch) / 2).coerceAtLeast(0)
    return Bitmap.createBitmap(src, x, y, cw.coerceAtMost(w - x), ch.coerceAtMost(h - y))
}
