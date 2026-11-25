package com.example

import com.example.core.AppLanguage
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
}
