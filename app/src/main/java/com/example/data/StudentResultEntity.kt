package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
data class SubjectMark(
    val name: String,
    val fullMarks: Int = 100,
    val passMark: Int = 33,
    val obtainedMarks: Double = 0.0,
    val grade: String = "F",
    val gpa: Double = 0.0,
    val isPassed: Boolean = false,
    val isEnabled: Boolean = true
)

@Serializable
@Entity(tableName = "student_results")
data class StudentResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examName: String,
    val institutionName: String,
    val className: String,
    val yearSession: String,
    val rollNumber: Int,
    val studentName: String,
    val fatherName: String = "",
    val motherName: String = "",
    val photoPath: String? = null,
    val subjectsJson: String,
    val totalFullMarks: Int,
    val totalObtainedMarks: Double,
    val percentage: Double,
    val totalGpa: Double,
    val overallGrade: String,
    val isPassed: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ComputedStudentResult(
    val entity: StudentResultEntity,
    val subjects: List<SubjectMark>,
    val meritPosition: Int?,
    val classPassRate: Double,
    val classTotalStudents: Int
)

enum class ExamTerm {
    FIRST_TERM,
    SECOND_TERM,
    ANNUAL;

    companion object {
        fun classify(examName: String): ExamTerm? {
            val n = examName.trim().lowercase()
            if (n.isEmpty()) return null
            return when {
                n.contains("১ম") || n.contains("প্রথম") ||
                    n.contains("1st") || n.contains("first") -> FIRST_TERM
                n.contains("২য়") || n.contains("২য়") || n.contains("দ্বিতীয়") || n.contains("দ্বিতীয়") ||
                    n.contains("অর্ধ-বার্ষিক") || n.contains("অর্ধ বার্ষিক") || n.contains("অর্ধবার্ষিক") ||
                    n.contains("2nd") || n.contains("second") ||
                    n.contains("half-yearly") || n.contains("half yearly") -> SECOND_TERM
                n.contains("বার্ষিক") || n.contains("annual") || n.contains("final") -> ANNUAL
                else -> null
            }
        }

        fun groupKey(examName: String): String {
            return when (classify(examName)) {
                FIRST_TERM -> "TERM_1"
                SECOND_TERM -> "TERM_2"
                ANNUAL -> "TERM_3"
                null -> examName.trim().lowercase()
            }
        }
    }
}

data class CombinedSubjectRow(
    val name: String,
    val firstTermObtained: Double?,
    val firstTermFull: Int?,
    val secondTermObtained: Double?,
    val secondTermFull: Int?,
    val annualObtained: Double?,
    val annualFull: Int?,
    val combinedFullMarks: Int,
    val combinedPassMark: Int,
    val combinedObtainedMarks: Double,
    val grade: String,
    val gpa: Double,
    val isPassed: Boolean
)

data class CombinedStudentResult(
    val key: String,
    val className: String,
    val yearSession: String,
    val rollNumber: Int,
    val studentName: String,
    val fatherName: String,
    val motherName: String,
    val institutionName: String,
    val photoPath: String?,
    val firstTerm: ComputedStudentResult?,
    val secondTerm: ComputedStudentResult?,
    val annual: ComputedStudentResult?,
    val examsCount: Int,
    val subjectRows: List<CombinedSubjectRow>,
    val totalFullMarks: Int,
    val totalObtainedMarks: Double,
    val percentage: Double,
    val totalGpa: Double,
    val overallGrade: String,
    val isPassed: Boolean,
    val meritPosition: Int?,
    val classPassRate: Double,
    val classTotalStudents: Int,
    val updatedAt: Long
)

