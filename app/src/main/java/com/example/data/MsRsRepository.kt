package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class MsRsRepository(private val dao: MsRsDao) {

    private val json = Json { ignoreUnknownKeys = true }

    val allMathHistory: Flow<List<MathHistoryEntity>> = dao.getAllMathHistory()

    val allComputedResults: Flow<List<ComputedStudentResult>> = dao.getAllResults().map { list ->
        computeGroupMetrics(list)
    }

    suspend fun getMathById(id: Long): MathHistoryEntity? = dao.getMathById(id)

    suspend fun saveMath(item: MathHistoryEntity): Long = dao.insertMath(item)

    suspend fun deleteMath(id: Long) = dao.deleteMathById(id)

    suspend fun getResultById(id: Long): StudentResultEntity? = dao.getResultById(id)

    suspend fun getNextRollNumber(className: String, examName: String): Int {
        return dao.getMaxRollForClass(className.trim(), examName.trim()) + 1
    }

    suspend fun saveResult(result: StudentResultEntity): Long {
        return if (result.id == 0L) {
            dao.insertResult(result)
        } else {
            dao.updateResult(result)
            result.id
        }
    }

    suspend fun deleteResult(id: Long) = dao.deleteResultById(id)

    suspend fun getAllMathSnapshot(): List<MathHistoryEntity> = dao.getAllMathHistorySnapshot()

    suspend fun getAllResultsSnapshot(): List<StudentResultEntity> = dao.getAllResultsSnapshot()

    suspend fun restoreBackupData(
        maths: List<MathHistoryEntity>,
        results: List<StudentResultEntity>,
        replaceExisting: Boolean = false
    ) {
        if (replaceExisting) {
            dao.clearAllMath()
            dao.clearAllResults()
        }
        if (maths.isNotEmpty()) {
            dao.insertAllMath(maths.map { it.copy(id = 0) })
        }
        if (results.isNotEmpty()) {
            dao.insertAllResults(results.map { it.copy(id = 0) })
        }
    }

    fun decodeSubjects(subjectsJson: String): List<SubjectMark> {
        return runCatching {
            json.decodeFromString<List<SubjectMark>>(subjectsJson)
        }.getOrDefault(emptyList())
    }

    private fun computeGroupMetrics(list: List<StudentResultEntity>): List<ComputedStudentResult> {
        val grouped = list.groupBy {
            "${it.examName.trim().lowercase()}|${it.className.trim().lowercase()}"
        }
        val meritMap = mutableMapOf<Long, Int?>()
        val passRateMap = mutableMapOf<Long, Double>()
        val groupSizeMap = mutableMapOf<Long, Int>()

        for ((_, group) in grouped) {
            val total = group.size
            val passedStudents = group.filter { it.isPassed }
                .sortedWith(
                    compareByDescending<StudentResultEntity> { it.totalObtainedMarks }
                        .thenByDescending { it.totalGpa }
                        .thenBy { it.rollNumber }
                )
            val passRate = if (total > 0) (passedStudents.size * 100.0) / total else 0.0
            passedStudents.forEachIndexed { idx, student ->
                meritMap[student.id] = idx + 1
            }
            group.forEach { student ->
                if (!student.isPassed) meritMap[student.id] = null
                passRateMap[student.id] = passRate
                groupSizeMap[student.id] = total
            }
        }

        return list.map { entity ->
            ComputedStudentResult(
                entity = entity,
                subjects = decodeSubjects(entity.subjectsJson),
                meritPosition = meritMap[entity.id],
                classPassRate = passRateMap[entity.id] ?: 0.0,
                classTotalStudents = groupSizeMap[entity.id] ?: 1
            )
        }
    }
}
