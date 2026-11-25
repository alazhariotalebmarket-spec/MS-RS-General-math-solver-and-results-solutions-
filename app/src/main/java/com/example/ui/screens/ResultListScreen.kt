package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.data.CombinedStudentResult
import com.example.data.ComputedStudentResult
import com.example.data.ExamTerm
import com.example.domain.PdfAndImageExporter
import com.example.domain.ResultCalculator
import com.example.ui.components.EyeViewButton
import com.example.ui.components.MsRsTopBar
import com.example.ui.theme.LocalMsRsColors
import kotlinx.coroutines.launch
import java.io.File

private enum class PassFilter { ALL, PASS, FAIL }

private enum class ExamModeTab {
    ALL_EXAMS,
    FIRST_TERM,
    SECOND_TERM,
    ANNUAL,
    COMBINED
}

@Composable
fun ResultListScreen(
    results: List<ComputedStudentResult>,
    combinedResults: List<CombinedStudentResult>,
    onBack: () -> Unit,
    onCreateResult: (initialClassName: String, initialExamName: String) -> Unit,
    onOpenMarksheet: (Long) -> Unit,
    onOpenCombinedMarksheet: (String) -> Unit
) {
    val ctx = LocalContext.current
    val strings = LocalStrings.current
    val lang = LocalLanguage.current
    val ext = LocalMsRsColors.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val fmt = { s: String -> DigitHelper.format(s, lang) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedClass by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedExamTab by rememberSaveable { mutableStateOf(ExamModeTab.ALL_EXAMS) }
    var passFilter by rememberSaveable { mutableStateOf(PassFilter.ALL) }

    // Canonical class list: classes with existing data + standard presets
    val classEntries = remember(results, strings.classPresets) {
        val counts = results.groupBy { it.entity.className.trim() }
            .filterKeys { it.isNotBlank() }
            .mapValues { it.value.size }
        val customClasses = counts.keys.filter { it !in strings.classPresets }
        val ordered = (strings.classPresets + customClasses)
            .filter { counts.containsKey(it) || it in strings.classPresets }
            .distinct()
        // Put classes with results first (preserving preset order), then empty presets
        val withData = ordered.filter { (counts[it] ?: 0) > 0 }
        val withoutData = ordered.filter { (counts[it] ?: 0) == 0 }
        (withData + withoutData).map { cls -> cls to (counts[cls] ?: 0) }
    }

    val filteredSingle = remember(results, searchQuery, selectedClass, selectedExamTab, passFilter) {
        val q = searchQuery.trim().lowercase()
        val engQ = DigitHelper.toEnglishDigits(q)
        results.filter { item ->
            val e = item.entity
            val matchesSearch = q.isEmpty() ||
                e.studentName.lowercase().contains(q) ||
                e.className.lowercase().contains(q) ||
                e.examName.lowercase().contains(q) ||
                e.rollNumber.toString().contains(engQ)
            val matchesClass = selectedClass == null || e.className.trim().equals(selectedClass!!.trim(), ignoreCase = true)
            val term = ExamTerm.classify(e.examName)
            val matchesExam = when (selectedExamTab) {
                ExamModeTab.ALL_EXAMS -> true
                ExamModeTab.FIRST_TERM -> term == ExamTerm.FIRST_TERM
                ExamModeTab.SECOND_TERM -> term == ExamTerm.SECOND_TERM
                ExamModeTab.ANNUAL -> term == ExamTerm.ANNUAL
                ExamModeTab.COMBINED -> false
            }
            val matchesPass = when (passFilter) {
                PassFilter.ALL -> true
                PassFilter.PASS -> e.isPassed
                PassFilter.FAIL -> !e.isPassed
            }
            matchesSearch && matchesClass && matchesExam && matchesPass
        }.sortedWith(
            compareBy<ComputedStudentResult> { it.entity.className.lowercase() }
                .thenBy { it.entity.rollNumber }
                .thenByDescending { it.entity.updatedAt }
        )
    }

    val filteredCombined = remember(combinedResults, searchQuery, selectedClass, passFilter) {
        val q = searchQuery.trim().lowercase()
        val engQ = DigitHelper.toEnglishDigits(q)
        combinedResults.filter { item ->
            val matchesSearch = q.isEmpty() ||
                item.studentName.lowercase().contains(q) ||
                item.className.lowercase().contains(q) ||
                item.rollNumber.toString().contains(engQ)
            val matchesClass = selectedClass == null || item.className.trim().equals(selectedClass!!.trim(), ignoreCase = true)
            val matchesPass = when (passFilter) {
                PassFilter.ALL -> true
                PassFilter.PASS -> item.isPassed
                PassFilter.FAIL -> !item.isPassed
            }
            matchesSearch && matchesClass && matchesPass
        }
    }

    val isCombinedMode = selectedExamTab == ExamModeTab.COMBINED

    val meritPdfSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                if (isCombinedMode) {
                    PdfAndImageExporter.writeCombinedMeritListPdfToUri(ctx, uri, filteredCombined, lang, strings)
                    snackbarHostState.showSnackbar("Combined Merit List PDF Saved")
                } else {
                    PdfAndImageExporter.writeClassMeritListPdfToUri(ctx, uri, filteredSingle, lang, strings)
                    snackbarHostState.showSnackbar("Class Merit List PDF Saved")
                }
            }
        }
    }

    val totalCount = if (isCombinedMode) filteredCombined.size else filteredSingle.size
    val passedCount = if (isCombinedMode) filteredCombined.count { it.isPassed } else filteredSingle.count { it.entity.isPassed }
    val failedCount = totalCount - passedCount
    val passRate = if (totalCount > 0) (passedCount * 100.0) / totalCount else 0.0

    val fabDefaultExam = when (selectedExamTab) {
        ExamModeTab.FIRST_TERM -> strings.examPresets.getOrElse(0) { strings.firstTermShort }
        ExamModeTab.SECOND_TERM -> strings.examPresets.getOrElse(1) { strings.secondTermShort }
        ExamModeTab.ANNUAL -> strings.examPresets.getOrElse(2) { strings.annualTermShort }
        else -> ""
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(modifier = Modifier.statusBarsPadding()) {
                MsRsTopBar(
                    title = selectedClass?.let { "${strings.resultListTitle} ($it)" } ?: strings.resultListTitle,
                    onBack = onBack,
                    actions = {
                        if (totalCount > 0) {
                            IconButton(
                                onClick = {
                                    val prefix = selectedClass?.replace(" ", "_") ?: "All"
                                    val fileName = if (isCombinedMode) {
                                        "MSRS_Combined_Merit_${prefix}.pdf"
                                    } else {
                                        "MSRS_Merit_${prefix}.pdf"
                                    }
                                    meritPdfSaver.launch(fileName)
                                },
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
                onClick = { onCreateResult(selectedClass.orEmpty(), fabDefaultExam) },
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

            // Separate Class Selector Strip (প্রত্যেক শ্রেণীর আলাদা রেজাল্ট)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = strings.classFilter,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedClass == null,
                            onClick = { selectedClass = null },
                            label = {
                                Text(
                                    "${strings.allClassesLabel} (${fmt(results.size.toString())})",
                                    maxLines = 1
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("class_chip_all")
                        )
                        classEntries.forEach { (cls, count) ->
                            val isSel = selectedClass == cls
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedClass = if (isSel) null else cls },
                                label = {
                                    Text(
                                        text = if (count > 0) "$cls (${fmt(count.toString())})" else cls,
                                        maxLines = 1
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("class_chip_$cls")
                            )
                        }
                    }
                }
            }

            // Exam Mode Tabs: সব পরীক্ষা | ১ম সাময়িক | ২য় সাময়িক | বার্ষিক | সমন্বিত
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = strings.examFilter,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val examTabs = listOf(
                            ExamModeTab.ALL_EXAMS to strings.allExamsTab,
                            ExamModeTab.FIRST_TERM to strings.firstTermShort,
                            ExamModeTab.SECOND_TERM to strings.secondTermShort,
                            ExamModeTab.ANNUAL to strings.annualTermShort,
                            ExamModeTab.COMBINED to strings.combinedTab
                        )
                        examTabs.forEach { (tab, label) ->
                            val isSel = selectedExamTab == tab
                            val isCombinedChip = tab == ExamModeTab.COMBINED
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedExamTab = tab },
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSel || isCombinedChip) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                },
                                colors = if (isCombinedChip) {
                                    FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ext.gold,
                                        selectedLabelColor = Color(0xFF1A1400)
                                    )
                                } else {
                                    FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
                                },
                                modifier = Modifier.testTag("exam_tab_${tab.name.lowercase()}")
                            )
                        }
                    }

                    // Pass / Fail Filter Row
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

            if (totalCount > 0) {
                item {
                    OutlinedButton(
                        onClick = {
                            val prefix = selectedClass?.replace(" ", "_") ?: "All"
                            val fileName = if (isCombinedMode) {
                                "MSRS_Combined_Merit_${prefix}.pdf"
                            } else {
                                "MSRS_Merit_${prefix}.pdf"
                            }
                            meritPdfSaver.launch(fileName)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("merit_pdf_banner_btn")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isCombinedMode) strings.combinedMeritPdf else strings.classMeritPdf,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isCombinedMode) {
                if (filteredCombined.isEmpty()) {
                    item {
                        EmptyResultCard(
                            title = strings.noResultsYet,
                            subtitle = strings.noCombinedResultsTip
                        )
                    }
                } else {
                    items(filteredCombined, key = { it.key }) { combined ->
                        CombinedStudentRowCard(
                            combined = combined,
                            fmt = fmt,
                            onClick = { onOpenCombinedMarksheet(combined.key) }
                        )
                    }
                }
            } else {
                if (filteredSingle.isEmpty()) {
                    item {
                        EmptyResultCard(title = strings.noResultsYet, subtitle = null)
                    }
                } else {
                    items(filteredSingle, key = { it.entity.id }) { computed ->
                        SingleStudentRowCard(
                            computed = computed,
                            fmt = fmt,
                            onClick = { onOpenMarksheet(computed.entity.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyResultCard(title: String, subtitle: String?) {
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
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SingleStudentRowCard(
    computed: ComputedStudentResult,
    fmt: (String) -> String,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    val ext = LocalMsRsColors.current
    val e = computed.entity

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                onClick = onClick
            )
        }
    }
}

@Composable
private fun CombinedStudentRowCard(
    combined: CombinedStudentResult,
    fmt: (String) -> String,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    val ext = LocalMsRsColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, ext.gold.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ext.goldContainer),
                contentAlignment = Alignment.Center
            ) {
                val photoFile = combined.photoPath?.let { File(it) }
                if (photoFile != null && photoFile.exists()) {
                    Image(
                        painter = rememberAsyncImagePainter(photoFile),
                        contentDescription = combined.studentName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = ext.onGoldContainer
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = combined.studentName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${combined.className} • ${strings.rollNumberLabel}: ${fmt(combined.rollNumber.toString())} • ${fmt(combined.examsCount.toString())}/${fmt("3")} ${strings.examsIncludedLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    TermMiniPill(strings.firstTermShort, combined.firstTerm, fmt)
                    TermMiniPill(strings.secondTermShort, combined.secondTerm, fmt)
                    TermMiniPill(strings.annualTermShort, combined.annual, fmt)
                }
                Spacer(Modifier.height(5.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (combined.isPassed) ext.passContainer else ext.failContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (combined.isPassed) "${strings.passed} (${combined.overallGrade})" else strings.failed,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (combined.isPassed) ext.passGreen else ext.failRed,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "GPA ${fmt(String.format(java.util.Locale.US, "%.2f", combined.totalGpa))} • ${fmt(ResultCalculator.formatMarks(combined.totalObtainedMarks))}/${fmt(combined.totalFullMarks.toString())}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    combined.meritPosition?.let { pos ->
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
                testTag = "combined_eye_${combined.key}",
                onClick = onClick
            )
        }
    }
}

@Composable
private fun TermMiniPill(
    label: String,
    exam: ComputedStudentResult?,
    fmt: (String) -> String
) {
    val strings = LocalStrings.current
    Surface(
        color = if (exam != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = if (exam != null) {
                "$label: ${fmt(ResultCalculator.formatMarks(exam.entity.totalObtainedMarks))}"
            } else {
                "$label: ${strings.notEnteredShort}"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1
        )
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
