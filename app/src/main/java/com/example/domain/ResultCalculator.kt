package com.example.domain

import com.example.data.CombinedStudentResult
import com.example.data.CombinedSubjectRow
import com.example.data.ComputedStudentResult
import com.example.data.ExamTerm
import com.example.data.StudentResultEntity
import com.example.data.SubjectMark
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

data class GradeResult(
    val grade: String,
    val gpa: Double,
    val isPassed: Boolean
)

data class CalculatedResultSummary(
    val evaluatedSubjects: List<SubjectMark>,
    val totalFullMarks: Int,
    val totalObtainedMarks: Double,
    val percentage: Double,
    val totalGpa: Double,
    val overallGrade: String,
    val isPassed: Boolean
)

object ResultCalculator {

    private val json = Json { ignoreUnknownKeys = true }

    fun calculateSubjectGrade(obtained: Double, fullMarks: Int, passMark: Int): GradeResult {
        if (fullMarks <= 0 || obtained < passMark) {
            return GradeResult(grade = "F", gpa = 0.0, isPassed = false)
        }
        val pct = (obtained / fullMarks.toDouble()) * 100.0
        return when {
            pct >= 80.0 -> GradeResult("A+", 5.00, true)
            pct >= 70.0 -> GradeResult("A", 4.00, true)
            pct >= 60.0 -> GradeResult("A-", 3.50, true)
            pct >= 50.0 -> GradeResult("B", 3.00, true)
            pct >= 40.0 -> GradeResult("C", 2.00, true)
            else -> GradeResult("D", 1.00, true)
        }
    }

    fun gpaToGrade(gpa: Double, isPassed: Boolean): String {
        if (!isPassed) return "F"
        return when {
            gpa >= 5.0 -> "A+"
            gpa >= 4.0 -> "A"
            gpa >= 3.5 -> "A-"
            gpa >= 3.0 -> "B"
            gpa >= 2.0 -> "C"
            gpa >= 1.0 -> "D"
            else -> "F"
        }
    }

    fun calculateSummary(subjects: List<SubjectMark>): CalculatedResultSummary {
        val evaluated = subjects.map { sub ->
            if (!sub.isEnabled) {
                sub.copy(grade = "-", gpa = 0.0, isPassed = true)
            } else {
                val res = calculateSubjectGrade(sub.obtainedMarks, sub.fullMarks, sub.passMark)
                sub.copy(grade = res.grade, gpa = res.gpa, isPassed = res.isPassed)
            }
        }
        val active = evaluated.filter { it.isEnabled && it.name.isNotBlank() }
        if (active.isEmpty()) {
            return CalculatedResultSummary(
                evaluatedSubjects = evaluated,
                totalFullMarks = 0,
                totalObtainedMarks = 0.0,
                percentage = 0.0,
                totalGpa = 0.0,
                overallGrade = "F",
                isPassed = false
            )
        }

        val totalFull = active.sumOf { it.fullMarks }
        val totalObtained = active.sumOf { it.obtainedMarks }
        val pct = if (totalFull > 0) (totalObtained * 100.0) / totalFull else 0.0
        val allPassed = active.all { it.isPassed }
        val avgGpa = if (allPassed) {
            val raw = active.sumOf { it.gpa } / active.size.toDouble()
            ((raw * 100.0).roundToInt() / 100.0).coerceAtMost(5.0)
        } else {
            0.0
        }
        val overallGrade = gpaToGrade(avgGpa, allPassed)

        return CalculatedResultSummary(
            evaluatedSubjects = evaluated,
            totalFullMarks = totalFull,
            totalObtainedMarks = totalObtained,
            percentage = (pct * 100.0).roundToInt() / 100.0,
            totalGpa = avgGpa,
            overallGrade = overallGrade,
            isPassed = allPassed
        )
    }

    fun buildEntity(
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
    ): StudentResultEntity {
        val summary = calculateSummary(subjects)
        val encodedSubjects = json.encodeToString(summary.evaluatedSubjects)
        return StudentResultEntity(
            id = id,
            examName = examName.trim(),
            institutionName = institutionName.trim(),
            className = className.trim(),
            yearSession = yearSession.trim(),
            rollNumber = rollNumber,
            studentName = studentName.trim(),
            fatherName = fatherName.trim(),
            motherName = motherName.trim(),
            photoPath = photoPath,
            subjectsJson = encodedSubjects,
            totalFullMarks = summary.totalFullMarks,
            totalObtainedMarks = summary.totalObtainedMarks,
            percentage = summary.percentage,
            totalGpa = summary.totalGpa,
            overallGrade = summary.overallGrade,
            isPassed = summary.isPassed,
            createdAt = if (id == 0L) System.currentTimeMillis() else createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun formatMarks(v: Double): String {
        val r = v.roundToInt()
        return if (kotlin.math.abs(v - r) < 1e-6) r.toString()
        else String.format(java.util.Locale.US, "%.1f", v)
    }

    fun combinedKeyFor(className: String, yearSession: String, rollNumber: Int): String {
        val cls = className.trim().lowercase()
        val yr = yearSession.trim().lowercase().ifBlank { "default" }
        return "$cls|$yr|$rollNumber"
    }

    fun computeCombinedResults(allComputed: List<ComputedStudentResult>): List<CombinedStudentResult> {
        val termRecords = allComputed.filter { ExamTerm.classify(it.entity.examName) != null }
        if (termRecords.isEmpty()) return emptyList()

        val byStudent = termRecords.groupBy {
            combinedKeyFor(it.entity.className, it.entity.yearSession, it.entity.rollNumber)
        }

        val rawCombined = byStudent.map { (key, entries) ->
            val firstTerm = entries
                .filter { ExamTerm.classify(it.entity.examName) == ExamTerm.FIRST_TERM }
                .maxByOrNull { it.entity.updatedAt }
            val secondTerm = entries
                .filter { ExamTerm.classify(it.entity.examName) == ExamTerm.SECOND_TERM }
                .maxByOrNull { it.entity.updatedAt }
            val annual = entries
                .filter { ExamTerm.classify(it.entity.examName) == ExamTerm.ANNUAL }
                .maxByOrNull { it.entity.updatedAt }

            val orderedPrefs = listOfNotNull(annual, secondTerm, firstTerm)
            val primary = orderedPrefs.first()
            val studentName = orderedPrefs.firstNotNullOfOrNull { it.entity.studentName.takeIf(String::isNotBlank) } ?: primary.entity.studentName
            val fatherName = orderedPrefs.firstNotNullOfOrNull { it.entity.fatherName.takeIf(String::isNotBlank) } ?: ""
            val motherName = orderedPrefs.firstNotNullOfOrNull { it.entity.motherName.takeIf(String::isNotBlank) } ?: ""
            val institutionName = orderedPrefs.firstNotNullOfOrNull { it.entity.institutionName.takeIf(String::isNotBlank) } ?: ""
            val photoPath = orderedPrefs.firstNotNullOfOrNull { it.entity.photoPath?.takeIf(String::isNotBlank) }
            val className = primary.entity.className.trim()
            val yearSession = primary.entity.yearSession.trim()

            // Collect ordered unique subject names across the entered exams
            val subjectOrder = linkedMapOf<String, String>()
            listOfNotNull(firstTerm, secondTerm, annual).forEach { exam ->
                exam.subjects.filter { it.isEnabled && it.name.isNotBlank() }.forEach { sub ->
                    val norm = sub.name.trim().lowercase()
                    if (!subjectOrder.containsKey(norm)) {
                        subjectOrder[norm] = sub.name.trim()
                    }
                }
            }

            fun findSub(exam: ComputedStudentResult?, normName: String): SubjectMark? {
                return exam?.subjects?.firstOrNull {
                    it.isEnabled && it.name.trim().lowercase() == normName
                }
            }

            val subjectRows = subjectOrder.map { (norm, displayName) ->
                val s1 = findSub(firstTerm, norm)
                val s2 = findSub(secondTerm, norm)
                val s3 = findSub(annual, norm)
                val combinedFull = (s1?.fullMarks ?: 0) + (s2?.fullMarks ?: 0) + (s3?.fullMarks ?: 0)
                val combinedPass = (s1?.passMark ?: 0) + (s2?.passMark ?: 0) + (s3?.passMark ?: 0)
                val combinedObt = (s1?.obtainedMarks ?: 0.0) + (s2?.obtainedMarks ?: 0.0) + (s3?.obtainedMarks ?: 0.0)
                val g = calculateSubjectGrade(combinedObt, combinedFull, combinedPass)
                CombinedSubjectRow(
                    name = displayName,
                    firstTermObtained = s1?.obtainedMarks,
                    firstTermFull = s1?.fullMarks,
                    secondTermObtained = s2?.obtainedMarks,
                    secondTermFull = s2?.fullMarks,
                    annualObtained = s3?.obtainedMarks,
                    annualFull = s3?.fullMarks,
                    combinedFullMarks = combinedFull,
                    combinedPassMark = combinedPass,
                    combinedObtainedMarks = combinedObt,
                    grade = g.grade,
                    gpa = g.gpa,
                    isPassed = g.isPassed
                )
            }

            val totalFull = subjectRows.sumOf { it.combinedFullMarks }
            val totalObt = subjectRows.sumOf { it.combinedObtainedMarks }
            val pct = if (totalFull > 0) ((totalObt * 100.0) / totalFull * 100.0).roundToInt() / 100.0 else 0.0
            val allPassed = subjectRows.isNotEmpty() && subjectRows.all { it.isPassed }
            val avgGpa = if (allPassed) {
                val raw = subjectRows.sumOf { it.gpa } / subjectRows.size.toDouble()
                ((raw * 100.0).roundToInt() / 100.0).coerceAtMost(5.0)
            } else {
                0.0
            }
            val grade = gpaToGrade(avgGpa, allPassed)
            val examsCount = listOfNotNull(firstTerm, secondTerm, annual).size
            val latestUpdated = listOfNotNull(firstTerm, secondTerm, annual).maxOf { it.entity.updatedAt }

            CombinedStudentResult(
                key = key,
                className = className,
                yearSession = yearSession,
                rollNumber = primary.entity.rollNumber,
                studentName = studentName,
                fatherName = fatherName,
                motherName = motherName,
                institutionName = institutionName,
                photoPath = photoPath,
                firstTerm = firstTerm,
                secondTerm = secondTerm,
                annual = annual,
                examsCount = examsCount,
                subjectRows = subjectRows,
                totalFullMarks = totalFull,
                totalObtainedMarks = totalObt,
                percentage = pct,
                totalGpa = avgGpa,
                overallGrade = grade,
                isPassed = allPassed,
                meritPosition = null,
                classPassRate = 0.0,
                classTotalStudents = 1,
                updatedAt = latestUpdated
            )
        }

        // Compute class-wise independent merit position & pass rate
        val byClassYear = rawCombined.groupBy {
            "${it.className.trim().lowercase()}|${it.yearSession.trim().lowercase().ifBlank { "default" }}"
        }
        val meritMap = mutableMapOf<String, Int?>()
        val passRateMap = mutableMapOf<String, Double>()
        val classSizeMap = mutableMapOf<String, Int>()

        for ((_, classList) in byClassYear) {
            val total = classList.size
            val passedList = classList.filter { it.isPassed }.sortedWith(
                compareByDescending<CombinedStudentResult> { it.totalObtainedMarks }
                    .thenByDescending { it.totalGpa }
                    .thenBy { it.rollNumber }
            )
            val passRate = if (total > 0) (passedList.size * 100.0) / total else 0.0
            passedList.forEachIndexed { index, item ->
                meritMap[item.key] = index + 1
            }
            classList.forEach { item ->
                if (!item.isPassed) meritMap[item.key] = null
                passRateMap[item.key] = passRate
                classSizeMap[item.key] = total
            }
        }

        return rawCombined.map { item ->
            item.copy(
                meritPosition = meritMap[item.key],
                classPassRate = passRateMap[item.key] ?: 0.0,
                classTotalStudents = classSizeMap[item.key] ?: 1
            )
        }.sortedWith(
            compareBy<CombinedStudentResult> { it.className.lowercase() }
                .thenBy { it.meritPosition ?: Int.MAX_VALUE }
                .thenBy { it.rollNumber }
        )
    }
}
