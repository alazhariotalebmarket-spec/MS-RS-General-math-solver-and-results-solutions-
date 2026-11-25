package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute

@Serializable
data object MainTabsRoute

@Serializable
data object CameraSolverRoute

@Serializable
data object CalculatorSolverRoute

@Serializable
data class MathSolutionRoute(val mathId: Long)

@Serializable
data object ResultListRoute

@Serializable
data class ResultFormRoute(
    val resultId: Long = 0L,
    val initialClassName: String = "",
    val initialExamName: String = ""
)

@Serializable
data class ResultMarksheetRoute(val resultId: Long)

@Serializable
data class CombinedMarksheetRoute(val combinedKey: String)

@Serializable
data object StudyToolsRoute

enum class BottomTab {
    HOME, HISTORY, BACKUP, ABOUT
}
