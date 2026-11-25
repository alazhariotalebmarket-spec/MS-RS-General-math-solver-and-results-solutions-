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
