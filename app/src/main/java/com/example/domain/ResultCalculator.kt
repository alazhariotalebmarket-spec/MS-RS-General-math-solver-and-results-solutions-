package com.example.domain

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
}
