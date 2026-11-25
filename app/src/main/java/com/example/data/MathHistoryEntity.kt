package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class MathLayoutType {
    HORIZONTAL,
    VERTICAL,
    LONG_DIVISION,
    CALCULATOR
}

@Serializable
enum class MathSourceType {
    CALCULATOR
}

@Serializable
@Entity(tableName = "math_history")
data class MathHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val normalizedExpression: String,
    val answer: String,
    val khataLinesJson: String,
    val explanationStepsJson: String,
    val layoutType: String = MathLayoutType.CALCULATOR.name,
    val sourceType: String = MathSourceType.CALCULATOR.name,
    val useBanglaDigits: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
