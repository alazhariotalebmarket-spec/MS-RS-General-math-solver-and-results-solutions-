package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MsRsDao {
    // Math History
    @Query("SELECT * FROM math_history ORDER BY createdAt DESC")
    fun getAllMathHistory(): Flow<List<MathHistoryEntity>>

    @Query("SELECT * FROM math_history ORDER BY createdAt DESC")
    suspend fun getAllMathHistorySnapshot(): List<MathHistoryEntity>

    @Query("SELECT * FROM math_history WHERE id = :id")
    suspend fun getMathById(id: Long): MathHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMath(item: MathHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMath(items: List<MathHistoryEntity>)

    @Query("DELETE FROM math_history WHERE id = :id")
    suspend fun deleteMathById(id: Long)

    @Query("DELETE FROM math_history")
    suspend fun clearAllMath()

    // Student Results
    @Query("SELECT * FROM student_results ORDER BY updatedAt DESC")
    fun getAllResults(): Flow<List<StudentResultEntity>>

    @Query("SELECT * FROM student_results ORDER BY updatedAt DESC")
    suspend fun getAllResultsSnapshot(): List<StudentResultEntity>

    @Query("SELECT * FROM student_results WHERE id = :id")
    suspend fun getResultById(id: Long): StudentResultEntity?

    @Query("SELECT COALESCE(MAX(rollNumber), 0) FROM student_results WHERE className = :className AND (:examName = '' OR examName = :examName)")
    suspend fun getMaxRollForClass(className: String, examName: String): Int

    @Query("SELECT * FROM student_results WHERE LOWER(TRIM(className)) = LOWER(TRIM(:className)) ORDER BY updatedAt DESC")
    suspend fun getResultsByClassSnapshot(className: String): List<StudentResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: StudentResultEntity): Long

    @Update
    suspend fun updateResult(result: StudentResultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllResults(items: List<StudentResultEntity>)

    @Query("DELETE FROM student_results WHERE id = :id")
    suspend fun deleteResultById(id: Long)

    @Query("DELETE FROM student_results")
    suspend fun clearAllResults()
}
