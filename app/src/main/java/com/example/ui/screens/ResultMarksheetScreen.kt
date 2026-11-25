package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.data.ComputedStudentResult
import com.example.domain.PdfAndImageExporter
import com.example.domain.ResultCalculator
import com.example.ui.components.MsRsLogoBadge
import com.example.ui.components.MsRsTopBar
import com.example.ui.theme.LocalMsRsColors
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ResultMarksheetScreen(
    resultId: Long,
    allResults: List<ComputedStudentResult>,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    val ctx = LocalContext.current
    val strings = LocalStrings.current
    val lang = LocalLanguage.current
    val ext = LocalMsRsColors.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val fmt = { s: String -> DigitHelper.format(s, lang) }

    val computed = remember(allResults, resultId) {
        allResults.firstOrNull { it.entity.id == resultId }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    val pdfSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val item = computed
        if (uri != null && item != null) {
            scope.launch {
                PdfAndImageExporter.writeMarksheetPdfToUri(ctx, uri, item, lang, strings)
                snackbarHostState.showSnackbar("A4 Marksheet PDF Saved")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(modifier = Modifier.statusBarsPadding()) {
                MsRsTopBar(title = strings.marksheetTitle, onBack = onBack)
            }
        }
    ) { innerPadding ->
        if (computed == null) {
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

        val e = computed.entity
        val activeSubjects = computed.subjects.filter { it.isEnabled && it.name.isNotBlank() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Double-Bordered Green & Gold Marksheet Card
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
                                    text = e.institutionName.ifBlank { "MS RS Institution" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${e.examName} • ${fmt(e.yearSession)}",
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
                                    text = e.studentName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (e.fatherName.isNotBlank()) {
                                    Text("${strings.fatherNameLabel}: ${e.fatherName}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (e.motherName.isNotBlank()) {
                                    Text("${strings.motherNameLabel}: ${e.motherName}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    text = "${strings.classNameLabel}: ${e.className}  |  ${strings.rollNumberLabel}: ${fmt(e.rollNumber.toString())}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                computed.meritPosition?.let { pos ->
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
                                val f = e.photoPath?.let { File(it) }
                                if (f != null && f.exists()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(f),
                                        contentDescription = e.studentName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }

                    // 8 Subjects Table (Responsive Column Weights)
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
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TableCellText(strings.subjectNameLabel, 2.4f, TextAlign.Start, Color.White, FontWeight.Bold)
                            TableCellText(strings.fullMarksLabel, 1f, TextAlign.Center, Color.White, FontWeight.Bold)
                            TableCellText(strings.passMarkLabel, 0.9f, TextAlign.Center, Color.White, FontWeight.Bold)
                            TableCellText(strings.obtainedMarksLabel, 1.1f, TextAlign.Center, Color.White, FontWeight.Bold)
                            TableCellText(strings.gradeLabel, 0.9f, TextAlign.Center, Color.White, FontWeight.Bold)
                            TableCellText(strings.gpaLabel, 1f, TextAlign.Center, Color.White, FontWeight.Bold)
                        }

                        activeSubjects.forEachIndexed { idx, sub ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (idx % 2 == 1) MaterialTheme.colorScheme.background
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TableCellText(sub.name, 2.4f, TextAlign.Start, MaterialTheme.colorScheme.onSurface, FontWeight.Medium)
                                TableCellText(fmt(sub.fullMarks.toString()), 1f, TextAlign.Center, MaterialTheme.colorScheme.onSurface)
                                TableCellText(fmt(sub.passMark.toString()), 0.9f, TextAlign.Center, MaterialTheme.colorScheme.onSurface)
                                TableCellText(fmt(ResultCalculator.formatMarks(sub.obtainedMarks)), 1.1f, TextAlign.Center, MaterialTheme.colorScheme.onSurface, FontWeight.Bold)
                                TableCellText(
                                    sub.grade,
                                    0.9f,
                                    TextAlign.Center,
                                    if (sub.isPassed) ext.passGreen else ext.failRed,
                                    FontWeight.Bold
                                )
                                TableCellText(fmt(String.format(java.util.Locale.US, "%.1f", sub.gpa)), 1f, TextAlign.Center, MaterialTheme.colorScheme.onSurface)
                            }
                            if (idx < activeSubjects.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                            }
                        }
                    }

                    // Result Summary Metrics Grid
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
                                SummaryBadge(strings.totalMarks, fmt(e.totalFullMarks.toString()), Modifier.weight(1f))
                                SummaryBadge(strings.obtainedTotal, fmt(ResultCalculator.formatMarks(e.totalObtainedMarks)), Modifier.weight(1f))
                                SummaryBadge(strings.percentage, fmt(String.format(java.util.Locale.US, "%.1f%%", e.percentage)), Modifier.weight(1f))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                SummaryBadge(strings.gpaLabel, fmt(String.format(java.util.Locale.US, "%.2f", e.totalGpa)), Modifier.weight(1f))
                                SummaryBadge(strings.gradeLabel, e.overallGrade, Modifier.weight(1f))
                                SummaryBadge(
                                    strings.classPassRate,
                                    fmt(String.format(java.util.Locale.US, "%.1f%%", computed.classPassRate)),
                                    Modifier.weight(1f)
                                )
                            }
                            Surface(
                                color = if (e.isPassed) ext.passContainer else ext.failContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${strings.statusLabel}: ${if (e.isPassed) strings.passed else strings.failed}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (e.isPassed) ext.passGreen else ext.failRed,
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
                                "${strings.dateLabel}: ${DigitHelper.formatShortDate(e.updatedAt, lang)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Action Buttons: Download PDF (A4), Share WhatsApp, Print, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { pdfSaver.launch("MSRS_Marksheet_${e.studentName}_Roll_${e.rollNumber}.pdf") },
                    modifier = Modifier.weight(1f).testTag("marksheet_download_pdf_btn")
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.downloadPdf, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Button(
                    onClick = {
                        scope.launch {
                            val pdfFile = PdfAndImageExporter.generateMarksheetPdfFile(ctx, computed, lang, strings)
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
                    modifier = Modifier.weight(1f).testTag("marksheet_whatsapp_btn")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.shareWhatsapp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val pdfFile = PdfAndImageExporter.generateMarksheetPdfFile(ctx, computed, lang, strings)
                            PdfAndImageExporter.printPdfFile(ctx, pdfFile, "MSRS_Marksheet_${e.rollNumber}")
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("marksheet_print_btn")
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.print, maxLines = 1)
                }

                OutlinedButton(
                    onClick = { onEdit(e.id) },
                    modifier = Modifier.weight(1f).testTag("marksheet_edit_btn")
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.edit, maxLines = 1)
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ext.failRed),
                    modifier = Modifier.weight(1f).testTag("marksheet_delete_btn")
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.delete, maxLines = 1)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(strings.confirmDeleteTitle) },
            text = { Text(strings.confirmDeleteDesc) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(resultId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ext.failRed)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TableCellText(
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
        fontSize = 11.sp,
        lineHeight = 15.sp,
        textAlign = align,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 2.dp)
    )
}

@Composable
private fun SummaryBadge(label: String, value: String, modifier: Modifier = Modifier) {
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
