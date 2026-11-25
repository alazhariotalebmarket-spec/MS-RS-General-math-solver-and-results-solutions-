package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.DigitHelper
import com.example.core.LocalLanguage
import com.example.core.LocalStrings
import com.example.data.MathHistoryEntity
import com.example.data.MathLayoutType
import com.example.domain.PdfAndImageExporter
import com.example.ui.components.MsRsTopBar
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalMsRsColors
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Composable
fun MathSolutionScreen(
    mathId: Long,
    loadMath: suspend (Long) -> MathHistoryEntity?,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val strings = LocalStrings.current
    val lang = LocalLanguage.current
    val ext = LocalMsRsColors.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val json = remember { Json { ignoreUnknownKeys = true } }

    var math by remember { mutableStateOf<MathHistoryEntity?>(null) }
    var useBanglaView by remember(lang) { mutableStateOf(lang == AppLanguage.BANGLA) }

    LaunchedEffect(mathId) {
        math = loadMath(mathId)
        math?.let { useBanglaView = it.useBanglaDigits }
    }

    val pdfSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val item = math
        if (uri != null && item != null) {
            scope.launch {
                PdfAndImageExporter.writeMathPdfToUri(ctx, uri, item, if (useBanglaView) AppLanguage.BANGLA else AppLanguage.ENGLISH, strings)
                snackbarHostState.showSnackbar("PDF Saved")
            }
        }
    }

    val imageSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        val item = math
        if (uri != null && item != null) {
            scope.launch {
                PdfAndImageExporter.writeMathImageToUri(ctx, uri, item, if (useBanglaView) AppLanguage.BANGLA else AppLanguage.ENGLISH, strings)
                snackbarHostState.showSnackbar("Image Saved")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(modifier = Modifier.statusBarsPadding()) {
                MsRsTopBar(title = strings.khataSolutionTitle, onBack = onBack)
            }
        }
    ) { innerPadding ->
        val item = math
        if (item == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val fmt = { s: String ->
            if (useBanglaView) DigitHelper.toBanglaDigits(s) else DigitHelper.toEnglishDigits(s)
        }
        val khataLines = remember(item) {
            runCatching { json.decodeFromString<List<String>>(item.khataLinesJson) }.getOrDefault(emptyList())
        }
        val steps = remember(item) {
            runCatching { json.decodeFromString<List<String>>(item.explanationStepsJson) }.getOrDefault(emptyList())
        }
        val layoutLabel = when (item.layoutType) {
            MathLayoutType.VERTICAL.name -> strings.verticalFormat
            MathLayoutType.LONG_DIVISION.name -> strings.longDivisionFormat
            MathLayoutType.HORIZONTAL.name -> strings.horizontalFormat
            else -> strings.calculatorFormat
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Digit Toggle + Format Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ext.solverBlueContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = layoutLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.onSolverBlueContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = useBanglaView,
                        onClick = { useBanglaView = true },
                        label = { Text("বাংলা") }
                    )
                    FilterChip(
                        selected = !useBanglaView,
                        onClick = { useBanglaView = false },
                        label = { Text("EN") }
                    )
                }
            }

            // Ruled Notebook Khata Sheet
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ext.khataPaper),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Ruled horizontal lines + red left margin
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val lineGap = 32.dp.toPx()
                        var y = 32.dp.toPx()
                        while (y < size.height) {
                            drawLine(
                                color = ext.khataLine,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                            y += lineGap
                        }
                        val marginX = 36.dp.toPx()
                        drawLine(
                            color = ext.khataMargin.copy(alpha = 0.65f),
                            start = Offset(marginX, 0f),
                            end = Offset(marginX, size.height),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 48.dp, end = 16.dp, top = 18.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "${fmt(item.expression)} = ?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = ext.solverBlueStart
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            khataLines.forEach { line ->
                                Text(
                                    text = fmt(line),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = strings.stepByStepTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            steps.forEach { s ->
                                Text(
                                    text = "• ${fmt(s)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${strings.finalAnswer}: ${fmt(item.answer)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }

            // Action Buttons: Share, Print, Download PDF, Save Image
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            val imgFile = PdfAndImageExporter.generateMathImageFile(
                                ctx, item, if (useBanglaView) AppLanguage.BANGLA else AppLanguage.ENGLISH, strings
                            )
                            PdfAndImageExporter.shareFile(ctx, imgFile, "image/png", strings.share)
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("math_share_btn")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.share)
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val pdfFile = PdfAndImageExporter.generateMathPdfFile(
                                ctx, item, if (useBanglaView) AppLanguage.BANGLA else AppLanguage.ENGLISH, strings
                            )
                            PdfAndImageExporter.printPdfFile(ctx, pdfFile, "MSRS_Math_${item.id}")
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("math_print_btn")
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.print)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { pdfSaver.launch("MSRS_Math_Solution_${item.id}.pdf") },
                    colors = ButtonDefaults.buttonColors(containerColor = ext.solverBlueStart),
                    modifier = Modifier.weight(1f).testTag("math_download_pdf_btn")
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.downloadPdf)
                }
                OutlinedButton(
                    onClick = { imageSaver.launch("MSRS_Math_Solution_${item.id}.png") },
                    modifier = Modifier.weight(1f).testTag("math_save_image_btn")
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.saveImage)
                }
            }
        }
    }
}
