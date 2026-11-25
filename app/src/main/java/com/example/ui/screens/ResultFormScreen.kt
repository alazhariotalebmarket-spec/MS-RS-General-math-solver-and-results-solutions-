package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.DigitHelper
import com.example.core.LocalStrings
import com.example.data.StudentResultEntity
import com.example.data.SubjectMark
import com.example.domain.ResultCalculator
import com.example.ui.components.MsRsTopBar
import com.example.ui.theme.LocalMsRsColors
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

private val EXAM_OPTIONS = listOf(
    "সাপ্তাহিক",
    "মাসিক",
    "১ম সাময়িক",
    "দ্বিতীয় সাময়িক",
    "অর্ধ বার্ষিক",
    "বার্ষিক",
    "মডেল টেস্ট",
    "বোর্ড পরীক্ষা"
)

private val CLASS_OPTIONS = listOf(
    "প্লে",
    "নার্সারি",
    "Class 1",
    "Class 2",
    "Class 3",
    "Class 4",
    "Class 5",
    "Class 6",
    "Class 7",
    "Class 8",
    "Class 9",
    "Class 10",
    "Class 11",
    "Class 12",
    "নাজেরা",
    "হিফজ"
)

private val YEAR_OPTIONS: List<String> = (2026..2100).map { it.toString() }

private val ROLL_OPTIONS: List<Int> = (1..500).toList()

private val SUBJECT_OPTIONS = listOf(
    "আরবী লিখিত",
    "আরবী মৌখিক",
    "বাংলা",
    "ইংরেজি",
    "অংক",
    "সাধারণ জ্ঞান",
    "আকাঈদ ও ফিকাহ",
    "দোয়া ও হাদিস"
)

private val PASS_MARK_OPTIONS = listOf(11, 17, 33, 50)

private val OBTAINED_MARK_OPTIONS: List<Int> = (1..100).toList()

private val BrandGreen = Color(0xFF0E7A3D)
private val SoftGreenBg = Color(0xFFF3FAF6)
private val SoftGreenBorder = Color(0xFFB7DEC8)

private data class SubjectEntryState(
    val name: String,
    val fullMarks: Int = 100,
    val passMark: Int = 33,
    val obtainedMarks: Int? = null
)

private sealed interface ActiveBottomSheet {
    data object Exam : ActiveBottomSheet
    data object ClassName : ActiveBottomSheet
    data object YearSession : ActiveBottomSheet
    data object RollNumber : ActiveBottomSheet
    data class SubjectName(val index: Int) : ActiveBottomSheet
    data class PassMark(val index: Int) : ActiveBottomSheet
    data class ObtainedMarks(val index: Int) : ActiveBottomSheet
}

private fun resolveInitialExam(initialExamName: String, defaultExamName: String): String {
    val candidate = initialExamName.ifBlank { defaultExamName }.trim()
    if (candidate in EXAM_OPTIONS) return candidate
    val low = candidate.lowercase()
    return when {
        low.contains("১ম") || low.contains("1st") || low.contains("প্রথম") -> "১ম সাময়িক"
        low.contains("২য়") || low.contains("২য়") || low.contains("দ্বিতীয়") || low.contains("2nd") -> "দ্বিতীয় সাময়িক"
        low.contains("অর্ধ") || low.contains("half") -> "অর্ধ বার্ষিক"
        low.contains("বার্ষিক") || low.contains("annual") -> "বার্ষিক"
        low.contains("মডেল") || low.contains("model") -> "মডেল টেস্ট"
        low.contains("বোর্ড") || low.contains("board") -> "বোর্ড পরীক্ষা"
        low.contains("মাসিক") || low.contains("monthly") -> "মাসিক"
        low.contains("সাপ্তাহিক") || low.contains("weekly") -> "সাপ্তাহিক"
        else -> EXAM_OPTIONS.first()
    }
}

private fun resolveInitialClass(initialClassName: String): String {
    val c = initialClassName.trim()
    if (c in CLASS_OPTIONS) return c
    return when (c.lowercase()) {
        "play" -> "প্লে"
        "nursery" -> "নার্সারি"
        "নূরানি", "nurani" -> "নাজেরা"
        "hifz" -> "হিফজ"
        else -> "প্লে"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultFormScreen(
    resultId: Long,
    initialClassName: String = "",
    initialExamName: String = "",
    defaultInstitution: String,
    defaultExamName: String,
    loadResult: suspend (Long) -> StudentResultEntity?,
    getNextRoll: suspend (String, String) -> Int,
    findStudentTemplate: suspend (String, Int) -> StudentResultEntity?,
    copyPhoto: (android.net.Uri) -> String?,
    onSave: (
        id: Long,
        examName: String,
        institutionName: String,
        className: String,
        yearSession: String,
        rollNumber: Int,
        studentName: String,
        fatherName: String,
        motherName: String,
        photoPath: String?,
        subjects: List<SubjectMark>,
        createdAt: Long
    ) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val ext = LocalMsRsColors.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val json = remember { Json { ignoreUnknownKeys = true } }

    fun toBn(value: String): String = DigitHelper.toBanglaDigits(value)
    fun toBn(value: Int): String = DigitHelper.toBanglaDigits(value.toString())

    var institutionName by remember { mutableStateOf(defaultInstitution) }
    var examName by remember { mutableStateOf(resolveInitialExam(initialExamName, defaultExamName)) }
    var className by remember { mutableStateOf(resolveInitialClass(initialClassName)) }
    var yearSession by remember { mutableStateOf(YEAR_OPTIONS.first()) }
    var studentName by remember { mutableStateOf("") }
    var rollNumber by remember { mutableIntStateOf(1) }
    var rollManuallyEdited by remember { mutableStateOf(false) }

    // Preserved silently when editing existing records
    var fatherName by remember { mutableStateOf("") }
    var motherName by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf<String?>(null) }
    var existingCreatedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var activeSheet by remember { mutableStateOf<ActiveBottomSheet?>(null) }

    val subjectRows = remember {
        mutableStateListOf<SubjectEntryState>().apply {
            SUBJECT_OPTIONS.forEach { defaultName ->
                add(SubjectEntryState(name = defaultName, fullMarks = 100, passMark = 33, obtainedMarks = null))
            }
        }
    }

    LaunchedEffect(resultId) {
        if (resultId != 0L) {
            loadResult(resultId)?.let { entity ->
                institutionName = entity.institutionName
                examName = resolveInitialExam(entity.examName, defaultExamName)
                className = resolveInitialClass(entity.className)
                yearSession = DigitHelper.toEnglishDigits(entity.yearSession).ifBlank { YEAR_OPTIONS.first() }
                rollNumber = entity.rollNumber.coerceIn(1, 500)
                rollManuallyEdited = true
                studentName = entity.studentName
                fatherName = entity.fatherName
                motherName = entity.motherName
                photoPath = entity.photoPath
                existingCreatedAt = entity.createdAt

                val decoded = runCatching { json.decodeFromString<List<SubjectMark>>(entity.subjectsJson) }
                    .getOrDefault(emptyList())
                if (decoded.isNotEmpty()) {
                    subjectRows.clear()
                    for (i in 0 until 8) {
                        val sub = decoded.getOrNull(i)
                        if (sub != null) {
                            val obtInt = sub.obtainedMarks.roundToInt()
                            subjectRows.add(
                                SubjectEntryState(
                                    name = sub.name.ifBlank { SUBJECT_OPTIONS[i] },
                                    fullMarks = 100,
                                    passMark = if (sub.passMark in PASS_MARK_OPTIONS) sub.passMark else 33,
                                    obtainedMarks = if (obtInt in 1..100) obtInt else null
                                )
                            )
                        } else {
                            subjectRows.add(SubjectEntryState(name = SUBJECT_OPTIONS[i]))
                        }
                    }
                }
            }
        }
    }

    // Auto roll based on selected class & exam for new result
    LaunchedEffect(className, examName, resultId) {
        if (resultId == 0L && !rollManuallyEdited && className.isNotBlank()) {
            val nextRoll = getNextRoll(className, examName)
            rollNumber = nextRoll.coerceIn(1, 500)
        }
    }

    val currentSubjectModels = subjectRows.map { row ->
        SubjectMark(
            name = row.name.trim(),
            fullMarks = 100,
            passMark = row.passMark,
            obtainedMarks = (row.obtainedMarks ?: 0).toDouble(),
            isEnabled = true
        )
    }
    val liveSummary = ResultCalculator.calculateSummary(currentSubjectModels)

    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(modifier = Modifier.statusBarsPadding()) {
                MsRsTopBar(
                    title = if (resultId == 0L) "নতুন রেজাল্ট যোগ করুন" else strings.editResultTitle,
                    onBack = onBack
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SoftGreenBg)
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. মাদ্রাসার নাম
            FormCardContainer {
                SectionFieldTitle("১. মাদ্রাসার নাম")
                OutlinedTextField(
                    value = institutionName,
                    onValueChange = { institutionName = it },
                    placeholder = {
                        Text(
                            text = "মাদ্রাসার নাম লিখুন",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.Black),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = SoftGreenBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_institution_input")
                )
            }

            // 2. পরীক্ষার নাম
            FormCardContainer {
                SectionFieldTitle("২. পরীক্ষার নাম")
                BottomSheetDropdownBox(
                    value = examName,
                    onClick = { activeSheet = ActiveBottomSheet.Exam },
                    modifier = Modifier.testTag("form_exam_input")
                )
            }

            // 3. শ্রেণী / জামাত
            FormCardContainer {
                SectionFieldTitle("৩. শ্রেণী / জামাত")
                BottomSheetDropdownBox(
                    value = className,
                    onClick = { activeSheet = ActiveBottomSheet.ClassName },
                    modifier = Modifier.testTag("form_class_input")
                )
            }

            // 4. সাল / সেশন
            FormCardContainer {
                SectionFieldTitle("৪. সাল / সেশন")
                BottomSheetDropdownBox(
                    value = yearSession,
                    onClick = { activeSheet = ActiveBottomSheet.YearSession },
                    modifier = Modifier.testTag("form_year_input")
                )
            }

            // 5. শিক্ষার্থীর নাম
            FormCardContainer {
                SectionFieldTitle("৫. শিক্ষার্থীর নাম")
                OutlinedTextField(
                    value = studentName,
                    onValueChange = { studentName = it },
                    placeholder = {
                        Text(
                            text = "শিক্ষার্থীর নাম লিখুন",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.Black),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = SoftGreenBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_student_name_input")
                )
            }

            // 6. রোল নাম্বার
            FormCardContainer {
                SectionFieldTitle("৬. রোল নাম্বার")
                BottomSheetDropdownBox(
                    value = toBn(rollNumber),
                    onClick = { activeSheet = ActiveBottomSheet.RollNumber },
                    modifier = Modifier.testTag("form_roll_input")
                )
            }

            // 7. বিষয়ের নাম - 8 Subjects
            Text(
                text = "৭. বিষয়ের নাম ও নম্বর (৮টি বিষয়)",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BrandGreen,
                modifier = Modifier.padding(top = 4.dp, start = 2.dp)
            )

            subjectRows.forEachIndexed { idx, row ->
                val evalSub = liveSummary.evaluatedSubjects.getOrNull(idx)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, SoftGreenBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "বিষয় ${toBn(idx + 1)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreen
                            )
                            if (row.obtainedMarks != null && evalSub != null) {
                                Surface(
                                    color = if (evalSub.isPassed) ext.passContainer else ext.failContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${evalSub.grade} (${toBn(String.format(java.util.Locale.US, "%.1f", evalSub.gpa))})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (evalSub.isPassed) ext.passGreen else ext.failRed,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Top line: [বিষয়ের নাম Dropdown with Arrow] - Full width
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "বিষয়ের নাম",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2D4A38)
                            )
                            BottomSheetDropdownBox(
                                value = row.name,
                                onClick = { activeSheet = ActiveBottomSheet.SubjectName(idx) },
                                modifier = Modifier.testTag("subject_name_$idx")
                            )
                        }

                        // Below that line: Left [পূর্ণমান] (fixed ১০০), Right [পাস] & [প্রাপ্ত নাম্বার]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Left: পূর্ণমান (Non-editable fixed ১০০)
                            Column(
                                modifier = Modifier.weight(0.85f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "পূর্ণমান",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2D4A38),
                                    maxLines = 1
                                )
                                Surface(
                                    color = SoftGreenBg,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.2.dp, SoftGreenBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = "১০০",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandGreen,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // Right side: Two boxes side by side -> a) পাস  b) প্রাপ্ত নাম্বার
                            Row(
                                modifier = Modifier.weight(2.15f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // a) পাস
                                Column(
                                    modifier = Modifier.weight(0.95f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "পাস",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2D4A38),
                                        maxLines = 1
                                    )
                                    BottomSheetDropdownBox(
                                        value = toBn(row.passMark),
                                        onClick = { activeSheet = ActiveBottomSheet.PassMark(idx) },
                                        compactHorizontalPadding = true,
                                        modifier = Modifier.testTag("subject_pass_$idx")
                                    )
                                }

                                // b) প্রাপ্ত নাম্বার
                                Column(
                                    modifier = Modifier.weight(1.2f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "প্রাপ্ত নাম্বার",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2D4A38),
                                        maxLines = 1
                                    )
                                    BottomSheetDropdownBox(
                                        value = row.obtainedMarks?.let { toBn(it) } ?: "বাছুন",
                                        isPlaceholder = row.obtainedMarks == null,
                                        onClick = { activeSheet = ActiveBottomSheet.ObtainedMarks(idx) },
                                        compactHorizontalPadding = true,
                                        modifier = Modifier.testTag("subject_marks_$idx")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // 8. Full-width Big Green Save Button
            Button(
                onClick = {
                    if (studentName.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("অনুগ্রহ করে শিক্ষার্থীর নাম লিখুন")
                        }
                        return@Button
                    }
                    onSave(
                        resultId,
                        examName,
                        institutionName.trim(),
                        className,
                        yearSession,
                        rollNumber,
                        studentName.trim(),
                        fatherName,
                        motherName,
                        photoPath,
                        liveSummary.evaluatedSubjects,
                        existingCreatedAt
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("save_result_btn")
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "সংরক্ষণ করুন",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }

            Spacer(Modifier.height(14.dp))
        }
    }

    // Bottom Sliding Sheets for ALL Dropdowns
    val sheet = activeSheet
    if (sheet != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            when (sheet) {
                ActiveBottomSheet.Exam -> {
                    StringListSheetContent(
                        title = "পরীক্ষার নাম নির্বাচন করুন",
                        options = EXAM_OPTIONS,
                        selected = examName,
                        onSelect = {
                            examName = it
                            activeSheet = null
                        }
                    )
                }

                ActiveBottomSheet.ClassName -> {
                    StringListSheetContent(
                        title = "শ্রেণী / জামাত নির্বাচন করুন",
                        options = CLASS_OPTIONS,
                        selected = className,
                        onSelect = {
                            className = it
                            activeSheet = null
                        }
                    )
                }

                ActiveBottomSheet.YearSession -> {
                    StringListSheetContent(
                        title = "সাল / সেশন নির্বাচন করুন",
                        options = YEAR_OPTIONS,
                        selected = yearSession,
                        onSelect = {
                            yearSession = it
                            activeSheet = null
                        }
                    )
                }

                ActiveBottomSheet.RollNumber -> {
                    NumberGridSheetContent(
                        title = "রোল নাম্বার নির্বাচন করুন (১–৫০০)",
                        numbers = ROLL_OPTIONS,
                        selected = rollNumber,
                        formatNumber = { toBn(it) },
                        onSelect = { picked ->
                            rollNumber = picked
                            rollManuallyEdited = true
                            activeSheet = null
                        }
                    )
                }

                is ActiveBottomSheet.SubjectName -> {
                    val idx = sheet.index
                    StringListSheetContent(
                        title = "বিষয়ের নাম নির্বাচন করুন (৮টি বিষয়)",
                        options = SUBJECT_OPTIONS,
                        selected = subjectRows[idx].name,
                        headerActionLabel = "সব ৮টি বিষয় একসাথে সিলেক্ট করুন",
                        onHeaderAction = {
                            SUBJECT_OPTIONS.forEachIndexed { i, subjectName ->
                                if (i < subjectRows.size) {
                                    subjectRows[i] = subjectRows[i].copy(name = subjectName)
                                }
                            }
                            activeSheet = null
                        },
                        onSelect = { picked ->
                            subjectRows[idx] = subjectRows[idx].copy(name = picked)
                            activeSheet = null
                        }
                    )
                }

                is ActiveBottomSheet.PassMark -> {
                    val idx = sheet.index
                    StringListSheetContent(
                        title = "পাস নম্বর নির্বাচন করুন",
                        options = PASS_MARK_OPTIONS.map { toBn(it) },
                        selected = toBn(subjectRows[idx].passMark),
                        onSelect = { pickedBn ->
                            val pickedInt = DigitHelper.toEnglishDigits(pickedBn).toIntOrNull() ?: 33
                            subjectRows[idx] = subjectRows[idx].copy(passMark = pickedInt)
                            activeSheet = null
                        }
                    )
                }

                is ActiveBottomSheet.ObtainedMarks -> {
                    val idx = sheet.index
                    NumberGridSheetContent(
                        title = "${subjectRows[idx].name} — প্রাপ্ত নাম্বার (১–১০০)",
                        numbers = OBTAINED_MARK_OPTIONS,
                        selected = subjectRows[idx].obtainedMarks,
                        formatNumber = { toBn(it) },
                        onSelect = { picked ->
                            subjectRows[idx] = subjectRows[idx].copy(obtainedMarks = picked)
                            activeSheet = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FormCardContainer(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, SoftGreenBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SectionFieldTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = BrandGreen
    )
}

@Composable
private fun BottomSheetDropdownBox(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
    compactHorizontalPadding: Boolean = false
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.3.dp, SoftGreenBorder),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (compactHorizontalPadding) 10.dp else 14.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value,
                fontSize = if (compactHorizontalPadding) 16.sp else 17.sp,
                fontWeight = if (isPlaceholder) FontWeight.Medium else FontWeight.SemiBold,
                color = if (isPlaceholder) Color(0xFF6B7E73) else Color(0xFF102A1C),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Select",
                tint = BrandGreen,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun StringListSheetContent(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    headerActionLabel: String? = null,
    onHeaderAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 14.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = BrandGreen,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        if (headerActionLabel != null && onHeaderAction != null) {
            Surface(
                color = BrandGreen,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onHeaderAction)
                    .testTag("select_all_subjects_btn")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = headerActionLabel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        HorizontalDivider(color = SoftGreenBorder)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(options) { item ->
                val isSelected = item == selected
                Surface(
                    color = if (isSelected) BrandGreen else SoftGreenBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isSelected) BrandGreen else SoftGreenBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(item) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item,
                            fontSize = 17.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) Color.White else Color(0xFF102A1C)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberGridSheetContent(
    title: String,
    numbers: List<Int>,
    selected: Int?,
    formatNumber: (Int) -> String,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = BrandGreen,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
        HorizontalDivider(color = SoftGreenBorder)
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(numbers) { num ->
                val isSelected = num == selected
                Surface(
                    color = if (isSelected) BrandGreen else SoftGreenBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isSelected) BrandGreen else SoftGreenBorder),
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(num) }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = formatNumber(num),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF102A1C),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
