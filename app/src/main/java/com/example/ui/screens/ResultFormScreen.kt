package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
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

// Dark Green-Black Theme Constants
private val DarkScreenBg = Color(0xFF0F1A13)
private val DarkCardBg = Color(0xFF1A2E22)
private val DarkBorder = Color(0xFF2E6B4A)
private val DarkHint = Color(0xFF9DB8A6)
private val DarkGreenAccent = Color(0xFF4ADE80)

private val BanglaDigitsVisualTransformation = VisualTransformation { text ->
    TransformedText(
        AnnotatedString(DigitHelper.toBanglaDigits(text.text)),
        OffsetMapping.Identity
    )
}

private data class SubjectEntryState(
    val name: String,
    val fullMarks: Int = 100,
    val passMark: Int? = null,
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
    val keyboardController = LocalSoftwareKeyboardController.current
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

    var fatherName by remember { mutableStateOf("") }
    var motherName by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf<String?>(null) }
    var existingCreatedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var headerExpanded by remember { mutableStateOf(true) }
    var templateStudent by remember { mutableStateOf<StudentResultEntity?>(null) }
    var activeSheet by remember { mutableStateOf<ActiveBottomSheet?>(null) }

    val subjectFocusRequesters = remember { List(8) { FocusRequester() } }
    val studentNameFocusRequester = remember { FocusRequester() }

    val subjectRows = remember {
        mutableStateListOf<SubjectEntryState>().apply {
            SUBJECT_OPTIONS.forEach { defaultName ->
                add(SubjectEntryState(name = defaultName, fullMarks = 100, passMark = null, obtainedMarks = null))
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
                                    passMark = if (sub.passMark in PASS_MARK_OPTIONS) sub.passMark else null,
                                    obtainedMarks = if (obtInt in 0..100) obtInt else null
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

    LaunchedEffect(className, examName, resultId) {
        if (resultId == 0L && !rollManuallyEdited && className.isNotBlank()) {
            val nextRoll = getNextRoll(className, examName)
            rollNumber = nextRoll.coerceIn(1, 500)
        }
    }

    LaunchedEffect(className, rollNumber, resultId) {
        if (resultId == 0L && className.isNotBlank()) {
            val found = findStudentTemplate(className, rollNumber)
            templateStudent = if (found != null && found.studentName.isNotBlank()) found else null
        } else {
            templateStudent = null
        }
    }

    val currentSubjectModels = subjectRows.map { row ->
        SubjectMark(
            name = row.name.trim(),
            fullMarks = 100,
            passMark = row.passMark ?: 33,
            obtainedMarks = (row.obtainedMarks ?: 0).toDouble(),
            isEnabled = true
        )
    }
    val liveSummary = ResultCalculator.calculateSummary(currentSubjectModels)
    val enteredCount = subjectRows.count { it.obtainedMarks != null }

    Scaffold(
        containerColor = DarkScreenBg,
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
                .background(DarkScreenBg)
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ধাপ ১: প্রতিষ্ঠান ও পরীক্ষার তথ্য (সুন্দরভাবে গুছানো ও সংক্ষিপ্তযোগ্য)
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.2.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { headerExpanded = !headerExpanded }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "১. প্রতিষ্ঠান ও পরীক্ষার তথ্য",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DarkGreenAccent
                            )
                            if (!headerExpanded) {
                                Text(
                                    text = "$examName • $className • ${toBn(yearSession)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = if (headerExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (headerExpanded) "সংক্ষিপ্ত করুন" else "বিস্তারিত দেখুন",
                            tint = DarkGreenAccent
                        )
                    }

                    AnimatedVisibility(visible = headerExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // মাদ্রাসার নাম
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SectionFieldTitle("মাদ্রাসার নাম")
                                DarkDarkTextField(
                                    value = institutionName,
                                    onValueChange = { institutionName = it },
                                    placeholderText = "মাদ্রাসার নাম লিখুন",
                                    imeAction = ImeAction.Next,
                                    onNext = { studentNameFocusRequester.requestFocus() },
                                    modifier = Modifier.testTag("form_institution_input")
                                )
                            }

                            // পরীক্ষার নাম
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SectionFieldTitle("পরীক্ষার নাম")
                                DarkBottomSheetDropdownBox(
                                    value = examName,
                                    onClick = { activeSheet = ActiveBottomSheet.Exam },
                                    modifier = Modifier.testTag("form_exam_input")
                                )
                            }

                            // শ্রেণী / জামাত
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SectionFieldTitle("শ্রেণী / জামাত")
                                DarkBottomSheetDropdownBox(
                                    value = className,
                                    onClick = { activeSheet = ActiveBottomSheet.ClassName },
                                    modifier = Modifier.testTag("form_class_input")
                                )
                            }

                            // সাল / সেশন
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SectionFieldTitle("সাল / সেশন")
                                DarkBottomSheetDropdownBox(
                                    value = toBn(yearSession),
                                    onClick = { activeSheet = ActiveBottomSheet.YearSession },
                                    modifier = Modifier.testTag("form_year_input")
                                )
                            }
                        }
                    }
                }
            }

            // ধাপ ২: শিক্ষার্থীর তথ্য ও দ্রুত রোল পরিবর্তন
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.2.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "২. শিক্ষার্থীর তথ্য",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkGreenAccent
                    )

                    // শিক্ষার্থীর নাম
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SectionFieldTitle("শিক্ষার্থীর নাম")
                        DarkDarkTextField(
                            value = studentName,
                            onValueChange = { studentName = it },
                            placeholderText = "শিক্ষার্থীর নাম লিখুন",
                            imeAction = ImeAction.Next,
                            onNext = { subjectFocusRequesters[0].requestFocus() },
                            modifier = Modifier
                                .focusRequester(studentNameFocusRequester)
                                .testTag("form_student_name_input")
                        )
                    }

                    // রোল নাম্বার (- / Dropdown Sheet / +)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SectionFieldTitle("রোল নাম্বার")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RollStepButton(
                                icon = Icons.Filled.Remove,
                                contentDescription = "আগের রোল",
                                enabled = rollNumber > 1,
                                onClick = {
                                    if (rollNumber > 1) {
                                        rollNumber -= 1
                                        rollManuallyEdited = true
                                    }
                                }
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                DarkBottomSheetDropdownBox(
                                    value = toBn(rollNumber),
                                    onClick = { activeSheet = ActiveBottomSheet.RollNumber },
                                    modifier = Modifier.testTag("form_roll_input")
                                )
                            }
                            RollStepButton(
                                icon = Icons.Filled.Add,
                                contentDescription = "পরবর্তী রোল",
                                enabled = rollNumber < 500,
                                onClick = {
                                    if (rollNumber < 500) {
                                        rollNumber += 1
                                        rollManuallyEdited = true
                                    }
                                }
                            )
                        }
                    }

                    // আগের পরীক্ষা থেকে অটো-ফিল সাজেশন
                    val tpl = templateStudent
                    if (tpl != null && studentName.isBlank()) {
                        Surface(
                            color = DarkScreenBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, DarkGreenAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    studentName = tpl.studentName
                                    if (institutionName.isBlank() && tpl.institutionName.isNotBlank()) {
                                        institutionName = tpl.institutionName
                                    }
                                    fatherName = tpl.fatherName
                                    motherName = tpl.motherName
                                    val prevSubs = runCatching {
                                        json.decodeFromString<List<SubjectMark>>(tpl.subjectsJson)
                                    }.getOrDefault(emptyList())
                                    if (prevSubs.isNotEmpty()) {
                                        for (i in 0 until 8) {
                                            val ps = prevSubs.getOrNull(i)
                                            if (ps != null) {
                                                subjectRows[i] = subjectRows[i].copy(
                                                    name = ps.name.ifBlank { SUBJECT_OPTIONS[i] },
                                                    passMark = if (ps.passMark in PASS_MARK_OPTIONS) ps.passMark else subjectRows[i].passMark
                                                )
                                            }
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = DarkGreenAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "আগের তথ্য বসান: ${tpl.studentName}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // লাইভ ফলাফল প্রিভিউ (অটোমেটিক হিসাব)
            Surface(
                color = DarkCardBg,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.3.dp, DarkGreenAccent.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "মোট নম্বর (${toBn(enteredCount)}/৮ বিষয়)",
                            fontSize = 12.sp,
                            color = DarkHint
                        )
                        Text(
                            text = "${toBn(ResultCalculator.formatMarks(liveSummary.totalObtainedMarks))} / ${toBn(liveSummary.totalFullMarks)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "জিপিএ", fontSize = 12.sp, color = DarkHint)
                        Text(
                            text = toBn(String.format(java.util.Locale.US, "%.2f", liveSummary.totalGpa)),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkGreenAccent
                        )
                    }
                    Surface(
                        color = if (liveSummary.isPassed) DarkBorder else ext.failContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (liveSummary.isPassed) "পাস • ${liveSummary.overallGrade}" else "ফেল • F",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (liveSummary.isPassed) DarkGreenAccent else ext.failRed,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // ধাপ ৩: ৮টি বিষয়ের নাম ও নম্বর (দ্রুত এন্ট্রি)
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "৩. বিষয়ের নাম ও নম্বর (সরাসরি টাইপ বা বাছাই করুন)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkGreenAccent
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "সব পাস নম্বর:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkHint
                        )
                        PASS_MARK_OPTIONS.forEach { passOpt ->
                            val allMatch = subjectRows.all { it.passMark == passOpt }
                            Surface(
                                color = if (allMatch) DarkBorder else DarkScreenBg,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, if (allMatch) DarkGreenAccent else DarkBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        for (i in subjectRows.indices) {
                                            subjectRows[i] = subjectRows[i].copy(passMark = passOpt)
                                        }
                                    }
                            ) {
                                Text(
                                    text = toBn(passOpt),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (allMatch) DarkGreenAccent else Color.White,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            subjectRows.forEachIndexed { idx, row ->
                val evalSub = liveSummary.evaluatedSubjects.getOrNull(idx)
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, DarkBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Top line: Subject number badge + [বিষয়ের নাম Dropdown] + Live Grade Pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "বিষয় ${toBn(idx + 1)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkGreenAccent
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
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        DarkBottomSheetDropdownBox(
                            value = row.name,
                            onClick = { activeSheet = ActiveBottomSheet.SubjectName(idx) },
                            modifier = Modifier.testTag("subject_name_$idx")
                        )

                        // Bottom line: Left [পূর্ণমান ১০০], Middle [পাস Dropdown], Right [প্রাপ্ত নাম্বার: টাইপ + ড্রপডাউন]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // পূর্ণমান (Always fixed ১০০)
                            Column(
                                modifier = Modifier.weight(0.78f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "পূর্ণমান",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DarkHint,
                                    maxLines = 1
                                )
                                Surface(
                                    color = DarkScreenBg,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, DarkBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = "১০০",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkGreenAccent,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // পাস
                            Column(
                                modifier = Modifier.weight(0.92f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "পাস",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DarkHint,
                                    maxLines = 1
                                )
                                DarkBottomSheetDropdownBox(
                                    value = row.passMark?.let { toBn(it) } ?: "বাছুন",
                                    isPlaceholder = row.passMark == null,
                                    onClick = { activeSheet = ActiveBottomSheet.PassMark(idx) },
                                    compactHorizontalPadding = true,
                                    modifier = Modifier.testTag("subject_pass_$idx")
                                )
                            }

                            // প্রাপ্ত নাম্বার (সরাসরি টাইপ + ড্রপডাউন শিট)
                            Column(
                                modifier = Modifier.weight(1.35f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "প্রাপ্ত নাম্বার",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DarkHint,
                                    maxLines = 1
                                )
                                HybridMarkInputBox(
                                    mark = row.obtainedMarks,
                                    onMarkChange = { newMark ->
                                        subjectRows[idx] = subjectRows[idx].copy(obtainedMarks = newMark)
                                    },
                                    onOpenSheet = { activeSheet = ActiveBottomSheet.ObtainedMarks(idx) },
                                    imeAction = if (idx < 7) ImeAction.Next else ImeAction.Done,
                                    onNextOrDone = {
                                        if (idx < 7) {
                                            subjectFocusRequesters[idx + 1].requestFocus()
                                        } else {
                                            keyboardController?.hide()
                                        }
                                    },
                                    modifier = Modifier
                                        .focusRequester(subjectFocusRequesters[idx])
                                        .testTag("subject_marks_$idx")
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // সংরক্ষণ করুন বাটন
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
                    containerColor = DarkGreenAccent,
                    contentColor = Color(0xFF0F1A13)
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

    // ALL Bottom Sliding Sheets (Dark Green-Black Theme)
    val sheet = activeSheet
    if (sheet != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
            containerColor = DarkScreenBg,
            dragHandle = {
                Surface(
                    color = DarkGreenAccent,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(vertical = 10.dp).width(40.dp).height(4.dp)
                ) {}
            }
        ) {
            when (sheet) {
                ActiveBottomSheet.Exam -> {
                    DarkStringListSheetContent(
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
                    DarkStringListSheetContent(
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
                    DarkStringListSheetContent(
                        title = "সাল / সেশন নির্বাচন করুন",
                        options = YEAR_OPTIONS,
                        selected = yearSession,
                        displayMapper = { toBn(it) },
                        onSelect = {
                            yearSession = it
                            activeSheet = null
                        }
                    )
                }

                ActiveBottomSheet.RollNumber -> {
                    DarkNumberGridSheetContent(
                        title = "রোল নাম্বার নির্বাচন করুন (১–৫০০)",
                        numbers = ROLL_OPTIONS,
                        selected = rollNumber,
                        formatNumber = { toBn(it) },
                        ranges = listOf(
                            "১–১০০" to (1..100),
                            "১০১–২০০" to (101..200),
                            "২০১–৩০০" to (201..300),
                            "৩০১–৪০০" to (301..400),
                            "৪০১–৫০০" to (401..500)
                        ),
                        onSelect = { picked ->
                            rollNumber = picked
                            rollManuallyEdited = true
                            activeSheet = null
                        }
                    )
                }

                is ActiveBottomSheet.SubjectName -> {
                    val idx = sheet.index
                    DarkStringListSheetContent(
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
                    DarkStringListSheetContent(
                        title = "পাস নম্বর নির্বাচন করুন",
                        options = PASS_MARK_OPTIONS.map { toBn(it) },
                        selected = subjectRows[idx].passMark?.let { toBn(it) } ?: "",
                        onSelect = { pickedBn ->
                            val pickedInt = DigitHelper.toEnglishDigits(pickedBn).toIntOrNull() ?: 33
                            subjectRows[idx] = subjectRows[idx].copy(passMark = pickedInt)
                            activeSheet = null
                        }
                    )
                }

                is ActiveBottomSheet.ObtainedMarks -> {
                    val idx = sheet.index
                    DarkNumberGridSheetContent(
                        title = "${subjectRows[idx].name} — প্রাপ্ত নাম্বার (১–১০০)",
                        numbers = OBTAINED_MARK_OPTIONS,
                        selected = subjectRows[idx].obtainedMarks,
                        formatNumber = { toBn(it) },
                        ranges = listOf(
                            "সব (১–১০০)" to (1..100),
                            "৭৬–১০০" to (76..100),
                            "৫১–৭৫" to (51..75),
                            "২৬–৫০" to (26..50),
                            "১–২৫" to (1..25)
                        ),
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
private fun RollStepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = DarkScreenBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.2.dp, if (enabled) DarkBorder else DarkBorder.copy(alpha = 0.35f)),
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) DarkGreenAccent else DarkHint.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun HybridMarkInputBox(
    mark: Int?,
    onMarkChange: (Int?) -> Unit,
    onOpenSheet: () -> Unit,
    imeAction: ImeAction,
    onNextOrDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rawDigits = mark?.toString() ?: ""

    Surface(
        color = DarkScreenBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.3.dp, if (mark != null) DarkGreenAccent.copy(alpha = 0.7f) else DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (rawDigits.isEmpty()) {
                    Text(
                        text = "০–১০০",
                        fontSize = 15.sp,
                        color = DarkHint,
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = rawDigits,
                    onValueChange = { incoming ->
                        val digitsOnly = DigitHelper.toEnglishDigits(incoming).filter { it.isDigit() }
                        if (digitsOnly.isEmpty()) {
                            onMarkChange(null)
                        } else {
                            val parsed = digitsOnly.take(3).toIntOrNull()
                            if (parsed != null && parsed <= 100) {
                                onMarkChange(parsed)
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    cursorBrush = SolidColor(DarkGreenAccent),
                    visualTransformation = BanglaDigitsVisualTransformation,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = imeAction
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { onNextOrDone() },
                        onDone = { onNextOrDone() }
                    ),
                    modifier = modifier.fillMaxWidth()
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                    .clickable(onClick = onOpenSheet),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "তালিকা থেকে বাছুন",
                    tint = DarkGreenAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionFieldTitle(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}

@Composable
private fun DarkDarkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderText: String,
    imeAction: ImeAction = ImeAction.Done,
    onNext: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholderText,
                fontSize = 16.sp,
                color = DarkHint
            )
        },
        textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onNext?.invoke() }),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DarkGreenAccent,
            unfocusedBorderColor = DarkBorder,
            focusedContainerColor = DarkScreenBg,
            unfocusedContainerColor = DarkScreenBg,
            cursorColor = DarkGreenAccent,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun DarkBottomSheetDropdownBox(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
    compactHorizontalPadding: Boolean = false
) {
    Surface(
        color = DarkScreenBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.3.dp, DarkBorder),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
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
                fontSize = if (compactHorizontalPadding) 15.sp else 17.sp,
                fontWeight = if (isPlaceholder) FontWeight.Medium else FontWeight.SemiBold,
                color = if (isPlaceholder) DarkHint else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Select",
                tint = DarkGreenAccent,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun DarkStringListSheetContent(
    title: String,
    options: List<String>,
    selected: String,
    displayMapper: (String) -> String = { it },
    onSelect: (String) -> Unit,
    headerActionLabel: String? = null,
    onHeaderAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkScreenBg)
            .navigationBarsPadding()
            .padding(bottom = 14.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        if (headerActionLabel != null && onHeaderAction != null) {
            Surface(
                color = DarkCardBg,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.2.dp, DarkGreenAccent),
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
                        tint = DarkGreenAccent,
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
        HorizontalDivider(color = DarkBorder)
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
                    color = if (isSelected) DarkBorder else DarkCardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isSelected) DarkGreenAccent else DarkBorder),
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
                            text = displayMapper(item),
                            fontSize = 17.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = Color.White
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = DarkGreenAccent,
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
private fun DarkNumberGridSheetContent(
    title: String,
    numbers: List<Int>,
    selected: Int?,
    formatNumber: (Int) -> String,
    ranges: List<Pair<String, IntRange>> = emptyList(),
    onSelect: (Int) -> Unit
) {
    var selectedRangeIndex by remember(ranges) { mutableIntStateOf(0) }
    val filteredNumbers = remember(numbers, ranges, selectedRangeIndex) {
        if (ranges.isEmpty()) numbers
        else {
            val activeRange = ranges[selectedRangeIndex].second
            numbers.filter { it in activeRange }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkScreenBg)
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
        if (ranges.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ranges.forEachIndexed { idx, (label, _) ->
                    val active = idx == selectedRangeIndex
                    Surface(
                        color = if (active) DarkBorder else DarkCardBg,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, if (active) DarkGreenAccent else DarkBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { selectedRangeIndex = idx }
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) DarkGreenAccent else Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = DarkBorder)
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredNumbers) { num ->
                val isSelected = num == selected
                Surface(
                    color = if (isSelected) DarkBorder else DarkCardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isSelected) DarkGreenAccent else DarkBorder),
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
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
