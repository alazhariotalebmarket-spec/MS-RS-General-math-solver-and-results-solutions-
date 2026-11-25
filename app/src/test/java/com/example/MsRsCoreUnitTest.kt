package com.example

import com.example.core.AppLanguage
import com.example.data.ComputedStudentResult
import com.example.data.ExamTerm
import com.example.data.MathLayoutType
import com.example.data.SubjectMark
import com.example.domain.MathSolveResult
import com.example.domain.MathSolverEngine
import com.example.domain.ResultCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MsRsCoreUnitTest {

    @Test
    fun `bangla addition solves with vertical khata steps`() {
        val res = MathSolverEngine.solve("২৫ + ১৭", useBanglaDigits = true, language = AppLanguage.BANGLA)
        assertTrue(res is MathSolveResult.Success)
        val ok = res as MathSolveResult.Success
        assertEquals("৪২", ok.answerText)
        assertEquals(MathLayoutType.VERTICAL, ok.layoutType)
        assertTrue(ok.khataLines.any { it.contains("৪২") })
    }

    @Test
    fun `bangla bracket long division parses and solves`() {
        val res = MathSolverEngine.solve("২ ) ৬ (", useBanglaDigits = true, language = AppLanguage.BANGLA)
        assertTrue(res is MathSolveResult.Success)
        val ok = res as MathSolveResult.Success
        assertEquals("৩", ok.answerText)
        assertEquals(MathLayoutType.LONG_DIVISION, ok.layoutType)
        assertTrue(ok.khataLines.any { it.contains("×") })
    }

    @Test
    fun `compound bodmas expression solves accurately`() {
        val res = MathSolverEngine.solve("10 + 5 × 2", useBanglaDigits = false, language = AppLanguage.ENGLISH)
        assertTrue(res is MathSolveResult.Success)
        assertEquals("20", (res as MathSolveResult.Success).answerText)
    }

    @Test
    fun `divide by zero returns error`() {
        val res = MathSolverEngine.solve("10 ÷ 0", useBanglaDigits = false, language = AppLanguage.ENGLISH)
        assertTrue(res is MathSolveResult.Error)
        assertTrue((res as MathSolveResult.Error).isDivideByZero)
    }

    @Test
    fun `result calculator computes gpa pass and fail rule`() {
        val passing = List(8) { idx ->
            SubjectMark(name = "Sub $idx", fullMarks = 100, passMark = 33, obtainedMarks = 85.0)
        }
        val passSummary = ResultCalculator.calculateSummary(passing)
        assertTrue(passSummary.isPassed)
        assertEquals(5.0, passSummary.totalGpa, 0.001)
        assertEquals("A+", passSummary.overallGrade)

        val failingOne = passing.toMutableList().apply {
            this[2] = this[2].copy(obtainedMarks = 25.0)
        }
        val failSummary = ResultCalculator.calculateSummary(failingOne)
        assertFalse(failSummary.isPassed)
        assertEquals(0.0, failSummary.totalGpa, 0.001)
        assertEquals("F", failSummary.overallGrade)
    }

    @Test
    fun `github repo url parser extracts owner and repo cleanly`() {
        assertEquals(
            "ferdausharfin/ms-rs-math-results",
            com.example.domain.BackupAndGitHubManager.extractOwnerRepo("https://github.com/ferdausharfin/ms-rs-math-results/releases/latest")
        )
        assertEquals(
            "user/repo",
            com.example.domain.BackupAndGitHubManager.extractOwnerRepo("user/repo.git")
        )
    }

    @Test
    fun `exam term classification recognizes 1st 2nd and annual in bangla and english`() {
        assertEquals(ExamTerm.FIRST_TERM, ExamTerm.classify("১ম সাময়িক পরীক্ষা"))
        assertEquals(ExamTerm.FIRST_TERM, ExamTerm.classify("প্রথম সাময়িক ২০২৬"))
        assertEquals(ExamTerm.FIRST_TERM, ExamTerm.classify("1st Terminal Exam"))

        assertEquals(ExamTerm.SECOND_TERM, ExamTerm.classify("২য় সাময়িক পরীক্ষা"))
        assertEquals(ExamTerm.SECOND_TERM, ExamTerm.classify("দ্বিতীয় সাময়িক পরীক্ষা"))
        assertEquals(ExamTerm.SECOND_TERM, ExamTerm.classify("অর্ধ-বার্ষিক পরীক্ষা"))
        assertEquals(ExamTerm.SECOND_TERM, ExamTerm.classify("2nd Terminal Exam"))

        assertEquals(ExamTerm.ANNUAL, ExamTerm.classify("বার্ষিক পরীক্ষা"))
        assertEquals(ExamTerm.ANNUAL, ExamTerm.classify("Annual Exam 2026"))
    }

    @Test
    fun `combined results separate each class and merge 3 exams accurately`() {
        fun makeComputed(
            id: Long,
            className: String,
            examName: String,
            roll: Int,
            studentName: String,
            marks: Double
        ): ComputedStudentResult {
            val subs = listOf(
                SubjectMark("বাংলা", 100, 33, marks),
                SubjectMark("গণিত", 100, 33, marks)
            )
            val entity = ResultCalculator.buildEntity(
                id = id,
                examName = examName,
                institutionName = "Test School",
                className = className,
                yearSession = "2026",
                rollNumber = roll,
                studentName = studentName,
                fatherName = "",
                motherName = "",
                photoPath = null,
                subjects = subs,
                createdAt = 1000L + id
            )
            val evaluated = ResultCalculator.calculateSummary(subs).evaluatedSubjects
            return ComputedStudentResult(entity, evaluated, null, 100.0, 1)
        }

        val all = listOf(
            // Class 1 Roll 1 (3 exams)
            makeComputed(1, "Class 1", "১ম সাময়িক পরীক্ষা", 1, "Rahim", 80.0),
            makeComputed(2, "Class 1", "২য় সাময়িক পরীক্ষা", 1, "Rahim", 85.0),
            makeComputed(3, "Class 1", "বার্ষিক পরীক্ষা", 1, "Rahim", 90.0),
            // Class 1 Roll 2 (3 exams, higher marks -> merit #1 in Class 1)
            makeComputed(4, "Class 1", "১ম সাময়িক পরীক্ষা", 2, "Karim", 92.0),
            makeComputed(5, "Class 1", "২য় সাময়িক পরীক্ষা", 2, "Karim", 94.0),
            makeComputed(6, "Class 1", "বার্ষিক পরীক্ষা", 2, "Karim", 95.0),
            // Class 2 Roll 1 (separate class -> merit #1 in Class 2)
            makeComputed(7, "Class 2", "১ম সাময়িক পরীক্ষা", 1, "Nusrat", 70.0),
            makeComputed(8, "Class 2", "বার্ষিক পরীক্ষা", 1, "Nusrat", 75.0)
        )

        val combined = ResultCalculator.computeCombinedResults(all)
        assertEquals(3, combined.size)

        val karim = combined.first { it.studentName == "Karim" }
        assertEquals("Class 1", karim.className)
        assertEquals(3, karim.examsCount)
        assertEquals(1, karim.meritPosition)
        assertEquals(562.0, karim.totalObtainedMarks, 0.01) // (92+94+95)*2
        assertEquals(600, karim.totalFullMarks)
        assertEquals("A+", karim.overallGrade)

        val rahim = combined.first { it.studentName == "Rahim" }
        assertEquals("Class 1", rahim.className)
        assertEquals(2, rahim.meritPosition)
        assertEquals(510.0, rahim.totalObtainedMarks, 0.01) // (80+85+90)*2
        assertEquals(100.0, rahim.classPassRate, 0.01)

        val nusrat = combined.first { it.studentName == "Nusrat" }
        assertEquals("Class 2", nusrat.className)
        assertEquals(2, nusrat.examsCount)
        assertEquals(1, nusrat.meritPosition) // independent merit #1 in Class 2
        assertEquals(290.0, nusrat.totalObtainedMarks, 0.01)
        assertEquals(400, nusrat.totalFullMarks)
    }
}
