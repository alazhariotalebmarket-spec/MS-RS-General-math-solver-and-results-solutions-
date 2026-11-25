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
data class ResultFormRoute(val resultId: Long = 0L)

@Serializable
data class ResultMarksheetRoute(val resultId: Long)

@Serializable
data object StudyToolsRoute

enum class BottomTab {
    HOME, HISTORY, BACKUP, ABOUT
}
