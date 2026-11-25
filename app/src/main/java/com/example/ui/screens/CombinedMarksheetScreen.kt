package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.core.DigitHelper
import com.example.core.LocalLanguage
import com.example.core.LocalStrings
import com.example.data.CombinedStudentResult
import com.example.data.ComputedStudentResult
import com.example.domain.PdfAndImageExporter
import com.example.domain.ResultCalculator
import com.example.ui.components.MsRsLogoBadge
import com.example.ui.components.MsRsTopBar
import com.example.ui.theme.LocalMsRsColors
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CombinedMarksheetScreen(
    combinedKey: String,
    allCombinedResults: List<CombinedStudentResult>,
    onBack: () -> Unit,
    onOpenSingleMarksheet: (Long) -> Unit,
    onAddMissingExam: (className: String, examName: String) -> Unit
) {
    val ctx = LocalContext.current
    val strings = LocalStrings.current
    val lang = LocalLanguage.current
    val ext = LocalMsRsColors.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val fmt = { s: String -> DigitHelper.format(s, lang) }

    val combined = remember(allCombinedResults, combinedKey) {
        allCombinedResults.firstOrNull { it.key == combinedKey }
    }

    val pdfSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val item = combined
        if (uri != null && item != null) {
            scope.launch {
                PdfAndImageExporter.writeCombinedMarksheetPdfToUri(ctx, uri, item, lang, strings)
                snackbarHostState.showSnackbar("Combined A4 Marksheet PDF Saved")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(modifier = Modifier.statusBarsPadding()) {
                MsRsTopBar(title = strings.combinedMarksheetTitle, onBack = onBack)
            }
        }
    ) { innerPadding ->
        if (combined == null) {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Double-Bordered Green & Gold Consolidated Marksheet
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .border(1.2.dp, ext.gold, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Banner
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MsRsLogoBadge(size = 44.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = combined.institutionName.ifBlank { "MS RS Institution" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${strings.combinedResultTitle} • ${fmt(combined.yearSession)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ext.goldBright,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Student Details + Photo
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = combined.studentName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (combined.fatherName.isNotBlank()) {
                                    Text("${strings.fatherNameLabel}: ${combined.fatherName}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (combined.motherName.isNotBlank()) {
                                    Text("${strings.motherNameLabel}: ${combined.motherName}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    text = "${strings.classNameLabel}: ${combined.className}  |  ${strings.rollNumberLabel}: ${fmt(combined.rollNumber.toString())}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                combined.meritPosition?.let { pos ->
                                    Text(
                                        text = "${strings.positionMerit}: #${fmt(pos.toString())}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ext.onGoldContainer
                                    )
                                }
                            }

                            Spacer(Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(1.dp, ext.gold, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                val f = combined.photoPath?.let { File(it) }
                                if (f != null && f.exists()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(f),
                                        contentDescription = combined.studentName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }

                    // 3 Individual Exam Term Summary Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TermSummaryChip(
                            title = strings.firstTermShort,
                            exam = combined.firstTerm,
                            presetExamName = strings.examPresets.getOrElse(0) { strings.firstTermShort },
                            className = combined.className,
                            fmt = fmt,
                            modifier = Modifier.weight(1f),
                            onOpenSingleMarksheet = onOpenSingleMarksheet,
                            onAddMissingExam = onAddMissingExam
                        )
                        TermSummaryChip(
                            title = strings.secondTermShort,
                            exam = combined.secondTerm,
                            presetExamName = strings.examPresets.getOrElse(1) { strings.secondTermShort },
                            className = combined.className,
                            fmt = fmt,
                            modifier = Modifier.weight(1f),
                            onOpenSingleMarksheet = onOpenSingleMarksheet,
                            onAddMissingExam = onAddMissingExam
                        )
                        TermSummaryChip(
                            title = strings.annualTermShort,
                            exam = combined.annual,
                            presetExamName = strings.examPresets.getOrElse(2) { strings.annualTermShort },
                            className = combined.className,
                            fmt = fmt,
                            modifier = Modifier.weight(1f),
                            onOpenSingleMarksheet = onOpenSingleMarksheet,
                            onAddMissingExam = onAddMissingExam
                        )
                    }

                    // Combined Subject-wise Table (Subject | 1st | 2nd | Annual | Total | Grade | GPA)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CombinedCellText(strings.subjectNameLabel, 2.0f, TextAlign.Start, Color.White, FontWeight.Bold)
                            CombinedCellText("১ম", 0.75f, TextAlign.Center, Color.White, FontWeight.Bold)
                            CombinedCellText("২য়", 0.75f, TextAlign.Center, Color.White, FontWeight.Bold)
                            CombinedCellText(strings.annualTermShort, 0.9f, TextAlign.Center, Color.White, FontWeight.Bold)
                            CombinedCellText(strings.totalCombinedCol, 1.15f, TextAlign.Center, Color.White, FontWeight.Bold)
                            CombinedCellText(strings.gradeLabel, 0.75f, TextAlign.Center, Color.White, FontWeight.Bold)
                            CombinedCellText(strings.gpaLabel, 0.8f, TextAlign.Center, Color.White, FontWeight.Bold)
                        }

                        combined.subjectRows.forEachIndexed { idx, row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (idx % 2 == 1) MaterialTheme.colorScheme.background
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .padding(horizontal = 6.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CombinedCellText(row.name, 2.0f, TextAlign.Start, MaterialTheme.colorScheme.onSurface, FontWeight.Medium)
                                CombinedCellText(
                                    row.firstTermObtained?.let { fmt(ResultCalculator.formatMarks(it)) } ?: "-",
                                    0.75f, TextAlign.Center, MaterialTheme.colorScheme.onSurface
                                )
                                CombinedCellText(
                                    row.secondTermObtained?.let { fmt(ResultCalculator.formatMarks(it)) } ?: "-",
                                    0.75f, TextAlign.Center, MaterialTheme.colorScheme.onSurface
                                )
                                CombinedCellText(
                                    row.annualObtained?.let { fmt(ResultCalculator.formatMarks(it)) } ?: "-",
                                    0.9f, TextAlign.Center, MaterialTheme.colorScheme.onSurface
                                )
                                CombinedCellText(
                                    "${fmt(ResultCalculator.formatMarks(row.combinedObtainedMarks))}/${fmt(row.combinedFullMarks.toString())}",
                                    1.15f, TextAlign.Center, MaterialTheme.colorScheme.onSurface, FontWeight.Bold
                                )
                                CombinedCellText(
                                    row.grade,
                                    0.75f,
                                    TextAlign.Center,
                                    if (row.isPassed) ext.passGreen else ext.failRed,
                                    FontWeight.Bold
                                )
                                CombinedCellText(
                                    fmt(String.format(java.util.Locale.US, "%.1f", row.gpa)),
                                    0.8f, TextAlign.Center, MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (idx < combined.subjectRows.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                            }
                        }
                    }

                    // Overall Combined Metrics Grid
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                CombinedSummaryBadge(strings.totalMarks, fmt(combined.totalFullMarks.toString()), Modifier.weight(1f))
                                CombinedSummaryBadge(strings.obtainedTotal, fmt(ResultCalculator.formatMarks(combined.totalObtainedMarks)), Modifier.weight(1f))
                                CombinedSummaryBadge(strings.percentage, fmt(String.format(java.util.Locale.US, "%.1f%%", combined.percentage)), Modifier.weight(1f))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                CombinedSummaryBadge(strings.gpaLabel, fmt(String.format(java.util.Locale.US, "%.2f", combined.totalGpa)), Modifier.weight(1f))
                                CombinedSummaryBadge(strings.gradeLabel, combined.overallGrade, Modifier.weight(1f))
                                CombinedSummaryBadge(
                                    strings.classPassRate,
                                    fmt(String.format(java.util.Locale.US, "%.1f%%", combined.classPassRate)),
                                    Modifier.weight(1f)
                                )
                            }
                            Surface(
                                color = if (combined.isPassed) ext.passContainer else ext.failContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${strings.statusLabel}: ${if (combined.isPassed) strings.passed else strings.failed} (${fmt(combined.examsCount.toString())}/${fmt("3")} ${strings.examsIncludedLabel})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (combined.isPassed) ext.passGreen else ext.failRed,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Signature & Date Footer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            HorizontalDivider(modifier = Modifier.width(120.dp), color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(4.dp))
                            Text(strings.teacherSignature, style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            HorizontalDivider(modifier = Modifier.width(100.dp), color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${strings.dateLabel}: ${DigitHelper.formatShortDate(combined.updatedAt, lang)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Action Buttons: Download PDF (A4), Share WhatsApp, Print
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { pdfSaver.launch("MSRS_Combined_${combined.studentName}_Roll_${combined.rollNumber}.pdf") },
                    modifier = Modifier.weight(1f).testTag("combined_download_pdf_btn")
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.downloadPdf, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Button(
                    onClick = {
                        scope.launch {
                            val pdfFile = PdfAndImageExporter.generateCombinedMarksheetPdfFile(ctx, combined, lang, strings)
                            PdfAndImageExporter.shareFile(
                                context = ctx,
                                file = pdfFile,
                                mimeType = "application/pdf",
                                title = strings.shareWhatsapp,
                                targetPackage = "com.whatsapp"
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ext.passGreen),
                    modifier = Modifier.weight(1f).testTag("combined_whatsapp_btn")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.shareWhatsapp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        val pdfFile = PdfAndImageExporter.generateCombinedMarksheetPdfFile(ctx, combined, lang, strings)
                        PdfAndImageExporter.printPdfFile(ctx, pdfFile, "MSRS_Combined_${combined.rollNumber}")
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("combined_print_btn")
            ) {
                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.print)
            }
        }
    }
}

@Composable
private fun TermSummaryChip(
    title: String,
    exam: ComputedStudentResult?,
    presetExamName: String,
    className: String,
    fmt: (String) -> String,
    modifier: Modifier = Modifier,
    onOpenSingleMarksheet: (Long) -> Unit,
    onAddMissingExam: (String, String) -> Unit
) {
    val strings = LocalStrings.current
    val ext = LocalMsRsColors.current
    val bg = when {
        exam == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        exam.entity.isPassed -> ext.passContainer.copy(alpha = 0.7f)
        else -> ext.failContainer.copy(alpha = 0.7f)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (exam == null) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            else if (exam.entity.isPassed) ext.passGreen.copy(alpha = 0.4f)
            else ext.failRed.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                if (exam != null) {
                    onOpenSingleMarksheet(exam.entity.id)
                } else {
                    onAddMissingExam(className, presetExamName)
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (exam != null) {
                val e = exam.entity
                Text(
                    text = "${fmt(ResultCalculator.formatMarks(e.totalObtainedMarks))}/${fmt(e.totalFullMarks.toString())}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (e.isPassed) ext.passGreen else ext.failRed,
                    maxLines = 1
                )
                Text(
                    text = "${e.overallGrade} • ${fmt(String.format(java.util.Locale.US, "%.1f", e.totalGpa))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            } else {
                Icon(
                    Icons.Filled.AddCircleOutline,
                    contentDescription = strings.addMissingExamMarks,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = strings.addMissingExamMarks,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.CombinedCellText(
    text: String,
    weight: Float,
    align: TextAlign,
    color: Color,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        text = text,
        color = color,
        fontWeight = fontWeight,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        textAlign = align,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 1.5.dp)
    )
}

@Composable
private fun CombinedSummaryBadge(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
