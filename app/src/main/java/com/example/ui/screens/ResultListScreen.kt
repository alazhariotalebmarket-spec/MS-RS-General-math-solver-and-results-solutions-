package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.core.DigitHelper
import com.example.core.LocalLanguage
import com.example.core.LocalStrings
import com.example.data.ComputedStudentResult
import com.example.domain.PdfAndImageExporter
import com.example.domain.ResultCalculator
import com.example.ui.components.EyeViewButton
import com.example.ui.components.MsRsTopBar
import com.example.ui.theme.LocalMsRsColors
import kotlinx.coroutines.launch
import java.io.File

private enum class PassFilter { ALL, PASS, FAIL }

@Composable
fun ResultListScreen(
    results: List<ComputedStudentResult>,
    onBack: () -> Unit,
    onCreateResult: () -> Unit,
    onOpenMarksheet: (Long) -> Unit
) {
    val ctx = LocalContext.current
    val strings = LocalStrings.current
    val lang = LocalLanguage.current
    val ext = LocalMsRsColors.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val fmt = { s: String -> DigitHelper.format(s, lang) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf<String?>(null) }
    var selectedExam by remember { mutableStateOf<String?>(null) }
    var passFilter by remember { mutableStateOf(PassFilter.ALL) }

    val availableClasses = remember(results) {
        results.map { it.entity.className }.filter { it.isNotBlank() }.distinct()
    }
    val availableExams = remember(results) {
        results.map { it.entity.examName }.filter { it.isNotBlank() }.distinct()
    }

    val filtered = remember(results, searchQuery, selectedClass, selectedExam, passFilter) {
        val q = searchQuery.trim().lowercase()
        val engQ = DigitHelper.toEnglishDigits(q)
        results.filter { item ->
            val e = item.entity
            val matchesSearch = q.isEmpty() ||
                e.studentName.lowercase().contains(q) ||
                e.className.lowercase().contains(q) ||
                e.examName.lowercase().contains(q) ||
                e.rollNumber.toString().contains(engQ)
            val matchesClass = selectedClass == null || e.className == selectedClass
            val matchesExam = selectedExam == null || e.examName == selectedExam
            val matchesPass = when (passFilter) {
                PassFilter.ALL -> true
                PassFilter.PASS -> e.isPassed
                PassFilter.FAIL -> !e.isPassed
            }
            matchesSearch && matchesClass && matchesExam && matchesPass
        }
    }

    val meritPdfSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                PdfAndImageExporter.writeClassMeritListPdfToUri(ctx, uri, filtered, lang, strings)
                snackbarHostState.showSnackbar("Class Merit List PDF Saved")
            }
        }
    }

    val totalCount = filtered.size
    val passedCount = filtered.count { it.entity.isPassed }
    val failedCount = totalCount - passedCount
    val passRate = if (totalCount > 0) (passedCount * 100.0) / totalCount else 0.0

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(modifier = Modifier.statusBarsPadding()) {
                MsRsTopBar(
                    title = strings.resultListTitle,
                    onBack = onBack,
                    actions = {
                        if (filtered.isNotEmpty()) {
                            IconButton(
                                onClick = { meritPdfSaver.launch("MSRS_Class_Merit_List.pdf") },
                                modifier = Modifier.testTag("export_merit_pdf_btn")
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = strings.classMeritPdf, tint = Color.White)
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateResult,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("add_result_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = strings.createResultTitle)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search field
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchResultsHint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("result_search_input")
                )
            }

            // Pass/Fail & Class & Exam Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = passFilter == PassFilter.ALL,
                            onClick = { passFilter = PassFilter.ALL },
                            label = { Text(strings.allFilter) }
                        )
                        FilterChip(
                            selected = passFilter == PassFilter.PASS,
                            onClick = { passFilter = PassFilter.PASS },
                            label = { Text(strings.passed) }
                        )
                        FilterChip(
                            selected = passFilter == PassFilter.FAIL,
                            onClick = { passFilter = PassFilter.FAIL },
                            label = { Text(strings.failed) }
                        )
                        availableClasses.forEach { cls ->
                            FilterChip(
                                selected = selectedClass == cls,
                                onClick = { selectedClass = if (selectedClass == cls) null else cls },
                                label = { Text(cls) }
                            )
                        }
                    }

                    if (availableExams.size > 1) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableExams.forEach { exam ->
                                FilterChip(
                                    selected = selectedExam == exam,
                                    onClick = { selectedExam = if (selectedExam == exam) null else exam },
                                    label = { Text(exam, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                            }
                        }
                    }
                }
            }

            // Class Statistics Summary Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatMiniItem(strings.totalStudentsLabel, fmt(totalCount.toString()), MaterialTheme.colorScheme.onPrimaryContainer)
                        StatMiniItem(strings.passedStudentsLabel, fmt(passedCount.toString()), ext.passGreen)
                        StatMiniItem(strings.failedStudentsLabel, fmt(failedCount.toString()), ext.failRed)
                        StatMiniItem(
                            strings.classPassRate,
                            fmt(String.format(java.util.Locale.US, "%.0f%%", passRate)),
                            MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (filtered.isNotEmpty()) {
                item {
                    OutlinedButton(
                        onClick = { meritPdfSaver.launch("MSRS_Class_Merit_List.pdf") },
                        modifier = Modifier.fillMaxWidth().testTag("merit_pdf_banner_btn")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.classMeritPdf, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = strings.noResultsYet,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.entity.id }) { computed ->
                    val e = computed.entity
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenMarksheet(e.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Photo or Initial Avatar
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                val photoFile = e.photoPath?.let { File(it) }
                                if (photoFile != null && photoFile.exists()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(photoFile),
                                        contentDescription = e.studentName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = e.studentName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${e.className} • ${strings.rollNumberLabel}: ${fmt(e.rollNumber.toString())} • ${e.examName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = if (e.isPassed) ext.passContainer else ext.failContainer,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = if (e.isPassed) "${strings.passed} (${e.overallGrade})" else strings.failed,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (e.isPassed) ext.passGreen else ext.failRed,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "GPA ${fmt(String.format(java.util.Locale.US, "%.2f", e.totalGpa))} • ${fmt(ResultCalculator.formatMarks(e.totalObtainedMarks))}/${fmt(e.totalFullMarks.toString())}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    computed.meritPosition?.let { pos ->
                                        Surface(
                                            color = ext.goldContainer,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "#${fmt(pos.toString())}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = ext.onGoldContainer,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            EyeViewButton(
                                contentDescription = strings.viewMarksheet,
                                testTag = "result_eye_${e.id}",
                                onClick = { onOpenMarksheet(e.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMiniItem(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
