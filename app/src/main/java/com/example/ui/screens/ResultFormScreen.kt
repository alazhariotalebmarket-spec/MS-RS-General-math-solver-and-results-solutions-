package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.core.DigitHelper
import com.example.core.LocalLanguage
import com.example.core.LocalStrings
import com.example.data.StudentResultEntity
import com.example.data.SubjectMark
import com.example.domain.ResultCalculator
import com.example.ui.components.MsRsTopBar
import com.example.ui.theme.LocalMsRsColors
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

private data class SubjectFormRow(
    val name: String,
    val fullMarks: String,
    val passMark: String,
    val obtainedMarks: String,
    val isEnabled: Boolean = true
)

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
    val lang = LocalLanguage.current
    val ext = LocalMsRsColors.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val json = remember { Json { ignoreUnknownKeys = true } }
    val fmt = { s: String -> DigitHelper.format(s, lang) }

    var examName by remember {
        mutableStateOf(
            initialExamName.ifBlank { defaultExamName.ifBlank { strings.examPresets.first() } }
        )
    }
    var institutionName by remember { mutableStateOf(defaultInstitution) }
    var className by remember {
        mutableStateOf(initialClassName.ifBlank { strings.classPresets[4] })
    }
    var yearSession by remember { mutableStateOf("2026") }
    var rollText by remember { mutableStateOf("1") }
    var rollManuallyEdited by remember { mutableStateOf(false) }
    var studentName by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var motherName by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf<String?>(null) }
    var existingCreatedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var matchingTemplate by remember { mutableStateOf<StudentResultEntity?>(null) }

    val subjectRows = remember {
        mutableStateListOf<SubjectFormRow>().apply {
            strings.defaultSubjects.forEach { name ->
                add(SubjectFormRow(name = name, fullMarks = "100", passMark = "33", obtainedMarks = ""))
            }
        }
    }

    LaunchedEffect(resultId) {
        if (resultId != 0L) {
            loadResult(resultId)?.let { entity ->
                examName = entity.examName
                institutionName = entity.institutionName
                className = entity.className
                yearSession = entity.yearSession
                rollText = entity.rollNumber.toString()
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
                    decoded.forEach { sub ->
                        subjectRows.add(
                            SubjectFormRow(
                                name = sub.name,
                                fullMarks = sub.fullMarks.toString(),
                                passMark = sub.passMark.toString(),
                                obtainedMarks = ResultCalculator.formatMarks(sub.obtainedMarks),
                                isEnabled = sub.isEnabled
                            )
                        )
                    }
                    while (subjectRows.size < 8) {
                        subjectRows.add(SubjectFormRow("", "100", "33", "", isEnabled = false))
                    }
                }
            }
        }
    }

    // Auto Roll Number generator whenever Class or Exam changes for new entries
    LaunchedEffect(className, examName, resultId) {
        if (resultId == 0L && !rollManuallyEdited && className.isNotBlank()) {
            val nextRoll = getNextRoll(className, examName)
            rollText = nextRoll.toString()
        }
    }

    // Look up matching student from another exam of the same class & roll for 1-tap autofill
    LaunchedEffect(className, rollText, resultId) {
        if (resultId == 0L && className.isNotBlank()) {
            val parsedRoll = DigitHelper.toEnglishDigits(rollText).toIntOrNull() ?: 0
            matchingTemplate = if (parsedRoll > 0) {
                findStudentTemplate(className, parsedRoll)
            } else {
                null
            }
        } else {
            matchingTemplate = null
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoPath = copyPhoto(uri)
        }
    }

    val currentSubjectModels = subjectRows.map { row ->
        val full = DigitHelper.toEnglishDigits(row.fullMarks).toIntOrNull() ?: 100
        val pass = DigitHelper.toEnglishDigits(row.passMark).toIntOrNull() ?: 33
        val obt = DigitHelper.toEnglishDigits(row.obtainedMarks).toDoubleOrNull() ?: 0.0
        SubjectMark(
            name = row.name.trim(),
            fullMarks = full,
            passMark = pass,
            obtainedMarks = obt,
            isEnabled = row.isEnabled
        )
    }
    val liveSummary = ResultCalculator.calculateSummary(currentSubjectModels)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(modifier = Modifier.statusBarsPadding()) {
                MsRsTopBar(
                    title = if (resultId == 0L) strings.createResultTitle else strings.editResultTitle,
                    onBack = onBack
                )
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${strings.obtainedTotal}: ${fmt(ResultCalculator.formatMarks(liveSummary.totalObtainedMarks))}/${fmt(liveSummary.totalFullMarks.toString())} (${fmt(String.format(java.util.Locale.US, "%.1f%%", liveSummary.percentage))})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "GPA: ${fmt(String.format(java.util.Locale.US, "%.2f", liveSummary.totalGpa))} • ${liveSummary.overallGrade} • ${if (liveSummary.isPassed) strings.passed else strings.failed}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (liveSummary.isPassed) ext.passGreen else ext.failRed
                        )
                    }
                    Button(
                        onClick = {
                            if (studentName.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar(strings.enterStudentNameError) }
                                return@Button
                            }
                            if (examName.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar(strings.enterExamNameError) }
                                return@Button
                            }
                            val parsedRoll = DigitHelper.toEnglishDigits(rollText).toIntOrNull() ?: 1
                            onSave(
                                resultId,
                                examName,
                                institutionName,
                                className,
                                yearSession,
                                parsedRoll,
                                studentName,
                                fatherName,
                                motherName,
                                photoPath,
                                liveSummary.evaluatedSubjects,
                                existingCreatedAt
                            )
                        },
                        modifier = Modifier.testTag("save_result_btn")
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (resultId == 0L) strings.saveResult else strings.updateResult)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Exam & Institution Details Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = institutionName,
                        onValueChange = { institutionName = it },
                        label = { Text(strings.institutionNameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("form_institution_input")
                    )

                    OutlinedTextField(
                        value = examName,
                        onValueChange = { examName = it },
                        label = { Text(strings.examNameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("form_exam_input")
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        strings.examPresets.forEach { preset ->
                            FilterChip(
                                selected = examName == preset,
                                onClick = { examName = preset },
                                label = { Text(preset) }
                            )
                        }
                    }

                    // Class Presets + Custom Class
                    OutlinedTextField(
                        value = className,
                        onValueChange = { className = it },
                        label = { Text(strings.classNameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("form_class_input")
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        strings.classPresets.forEach { cls ->
                            FilterChip(
                                selected = className == cls,
                                onClick = { className = cls },
                                label = { Text(cls) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = yearSession,
                            onValueChange = { yearSession = it },
                            label = { Text(strings.yearSessionLabel) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("form_year_input")
                        )
                        OutlinedTextField(
                            value = rollText,
                            onValueChange = {
                                rollText = it
                                rollManuallyEdited = true
                            },
                            label = { Text(strings.rollNumberLabel) },
                            supportingText = { Text(strings.autoRollHint) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("form_roll_input")
                        )
                    }
                }
            }

            // Student Info & Optional Photo Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val f = photoPath?.let { File(it) }
                            if (f != null && f.exists()) {
                                Image(
                                    painter = rememberAsyncImagePainter(f),
                                    contentDescription = strings.studentPhotoOptional,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(strings.studentPhotoOptional, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    modifier = Modifier.testTag("form_pick_photo_btn")
                                ) {
                                    Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.choosePhoto)
                                }
                                if (photoPath != null) {
                                    OutlinedButton(onClick = { photoPath = null }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    val template = matchingTemplate
                    if (resultId == 0L && template != null && template.studentName != studentName) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                if (institutionName.isBlank()) institutionName = template.institutionName
                                studentName = template.studentName
                                fatherName = template.fatherName
                                motherName = template.motherName
                                if (photoPath == null) photoPath = template.photoPath
                                val tmplSubs = runCatching {
                                    json.decodeFromString<List<SubjectMark>>(template.subjectsJson)
                                }.getOrDefault(emptyList())
                                if (tmplSubs.isNotEmpty()) {
                                    subjectRows.clear()
                                    tmplSubs.forEach { s ->
                                        subjectRows.add(
                                            SubjectFormRow(
                                                name = s.name,
                                                fullMarks = s.fullMarks.toString(),
                                                passMark = s.passMark.toString(),
                                                obtainedMarks = "",
                                                isEnabled = s.isEnabled
                                            )
                                        )
                                    }
                                    while (subjectRows.size < 8) {
                                        subjectRows.add(SubjectFormRow("", "100", "33", "", isEnabled = false))
                                    }
                                }
                            },
                            label = { Text("${strings.fillFromExistingStudent}: ${template.studentName}") },
                            modifier = Modifier.testTag("autofill_student_chip")
                        )
                    }

                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        label = { Text("${strings.studentNameLabel} *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("form_student_name_input")
                    )

                    OutlinedTextField(
                        value = fatherName,
                        onValueChange = { fatherName = it },
                        label = { Text(strings.fatherNameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("form_father_name_input")
                    )

                    OutlinedTextField(
                        value = motherName,
                        onValueChange = { motherName = it },
                        label = { Text(strings.motherNameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("form_mother_name_input")
                    )
                }
            }

            // 8 Editable Subjects Card + Quick Presets
            Text(
                text = strings.subjectsSectionTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = strings.subjectPresetLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            strings.madrasaSubjects.forEachIndexed { i, name ->
                                if (i < subjectRows.size) subjectRows[i] = subjectRows[i].copy(name = name, isEnabled = true)
                            }
                        },
                        label = { Text(strings.presetMadrasa) },
                        modifier = Modifier.testTag("preset_madrasa_btn")
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            strings.schoolSubjects.forEachIndexed { i, name ->
                                if (i < subjectRows.size) subjectRows[i] = subjectRows[i].copy(name = name, isEnabled = true)
                            }
                        },
                        label = { Text(strings.presetSchool) },
                        modifier = Modifier.testTag("preset_school_btn")
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            strings.nuraniSubjects.forEachIndexed { i, name ->
                                if (i < subjectRows.size) subjectRows[i] = subjectRows[i].copy(name = name, isEnabled = true)
                            }
                        },
                        label = { Text(strings.presetNurani) },
                        modifier = Modifier.testTag("preset_nurani_btn")
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            for (i in subjectRows.indices) {
                                subjectRows[i] = subjectRows[i].copy(fullMarks = "100", passMark = "33")
                            }
                        },
                        label = { Text(strings.setAll100) }
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            for (i in subjectRows.indices) {
                                subjectRows[i] = subjectRows[i].copy(fullMarks = "50", passMark = "17")
                            }
                        },
                        label = { Text(strings.setAll50) }
                    )
                }
            }

            subjectRows.forEachIndexed { idx, row ->
                val evalSub = liveSummary.evaluatedSubjects.getOrNull(idx)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (row.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${fmt((idx + 1).toString())}.",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = row.name,
                                onValueChange = { subjectRows[idx] = row.copy(name = it) },
                                label = { Text(strings.subjectNameLabel) },
                                singleLine = true,
                                enabled = row.isEnabled,
                                modifier = Modifier.weight(1f).testTag("subject_name_$idx")
                            )
                            Spacer(Modifier.width(8.dp))
                            if (row.isEnabled && evalSub != null) {
                                Surface(
                                    color = if (evalSub.isPassed) ext.passContainer else ext.failContainer,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "${evalSub.grade} (${fmt(String.format(java.util.Locale.US, "%.1f", evalSub.gpa))})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (evalSub.isPassed) ext.passGreen else ext.failRed,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            Switch(
                                checked = row.isEnabled,
                                onCheckedChange = { subjectRows[idx] = row.copy(isEnabled = it) }
                            )
                        }

                        if (row.isEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = row.fullMarks,
                                    onValueChange = { subjectRows[idx] = row.copy(fullMarks = it) },
                                    label = { Text(strings.fullMarksLabel) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = row.passMark,
                                    onValueChange = { subjectRows[idx] = row.copy(passMark = it) },
                                    label = { Text(strings.passMarkLabel) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = row.obtainedMarks,
                                    onValueChange = { subjectRows[idx] = row.copy(obtainedMarks = it) },
                                    label = { Text(strings.obtainedMarksLabel) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f).testTag("subject_marks_$idx")
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
