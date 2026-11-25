package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.AppLanguage
import com.example.core.DigitHelper
import com.example.core.LocalLanguage
import com.example.core.LocalStrings
import com.example.data.MathLayoutType
import com.example.data.MathSourceType
import com.example.domain.MathSolveResult
import com.example.ui.components.MsRsTopBar
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalMsRsColors
import kotlinx.coroutines.launch

@Composable
fun CalculatorSolverScreen(
    onBack: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenSolution: (Long) -> Unit,
    onSolveExpression: (
        raw: String,
        useBangla: Boolean,
        source: MathSourceType,
        layout: MathLayoutType?,
        onSolved: (Long, MathSolveResult.Success) -> Unit,
        onError: (MathSolveResult.Error) -> Unit
    ) -> Unit
) {
    val strings = LocalStrings.current
    val lang = LocalLanguage.current
    val ext = LocalMsRsColors.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var useBanglaDigits by remember(lang) { mutableStateOf(lang == AppLanguage.BANGLA) }
    var expression by remember { mutableStateOf("") }
    var lastSolution by remember { mutableStateOf<MathSolveResult.Success?>(null) }
    var lastSavedId by remember { mutableStateOf<Long?>(null) }

    fun display(s: String): String =
        if (useBanglaDigits) DigitHelper.toBanglaDigits(s) else DigitHelper.toEnglishDigits(s)

    fun pressEquals() {
        if (expression.isBlank()) return
        onSolveExpression(
            expression,
            useBanglaDigits,
            MathSourceType.CALCULATOR,
            null,
            { savedId, success ->
                lastSavedId = savedId
                lastSolution = success
            },
            { err ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (err.isDivideByZero) strings.divideByZeroError else strings.invalidExpression
                    )
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(modifier = Modifier.statusBarsPadding()) {
                MsRsTopBar(
                    title = strings.calculatorSolverTitle,
                    onBack = onBack,
                    actions = {
                        IconButton(
                            onClick = onOpenCamera,
                            modifier = Modifier.testTag("calc_to_camera_btn")
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = strings.quickCameraSolve, tint = Color.White)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Digit Language Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = useBanglaDigits,
                    onClick = {
                        useBanglaDigits = true
                        expression = DigitHelper.toBanglaDigits(expression)
                    },
                    label = { Text(strings.banglaDigitsLabel) },
                    modifier = Modifier.testTag("calc_bangla_toggle")
                )
                FilterChip(
                    selected = !useBanglaDigits,
                    onClick = {
                        useBanglaDigits = false
                        expression = DigitHelper.toEnglishDigits(expression)
                    },
                    label = { Text(strings.englishDigitsLabel) },
                    modifier = Modifier.testTag("calc_english_toggle")
                )
            }

            // Display + Khata Solution area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = ext.khataPaper),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (expression.isEmpty()) display("0") else display(expression),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ext.solverBlueStart,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calc_display_text")
                    )

                    val sol = lastSolution
                    val savedId = lastSavedId
                    AnimatedVisibility(visible = sol != null && savedId != null) {
                        if (sol != null && savedId != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ext.khataLine),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        sol.khataLines.forEach { line ->
                                            Text(
                                                text = display(line),
                                                fontFamily = JetBrainsMonoFontFamily,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${strings.finalAnswer}: ${display(sol.answerText)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(
                                        onClick = { onOpenSolution(savedId) },
                                        modifier = Modifier.testTag("calc_eye_solution_btn")
                                    ) {
                                        Icon(Icons.Filled.Visibility, contentDescription = strings.viewSolution)
                                        Spacer(Modifier.width(4.dp))
                                        Text(strings.viewSolution)
                                    }
                                }

                                sol.explanationSteps.forEach { s ->
                                    Text(
                                        text = "• ${display(s)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Keypad
            val keys = listOf(
                listOf("C", "(", ")", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "-"),
                listOf("1", "2", "3", "+"),
                listOf("0", ".", "⌫", "=")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { key ->
                            val isOperator = key in listOf("÷", "×", "-", "+", "(", ")")
                            val isEquals = key == "="
                            val isClear = key == "C"
                            val isBackspace = key == "⌫"
                            val label = if (key in "0".."9") display(key) else key

                            val bg = when {
                                isEquals -> MaterialTheme.colorScheme.primary
                                isClear -> ext.failContainer
                                isOperator -> ext.solverBlueContainer
                                else -> MaterialTheme.colorScheme.surface
                            }
                            val fg = when {
                                isEquals -> Color.White
                                isClear -> ext.failRed
                                isOperator -> ext.onSolverBlueContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Surface(
                                color = bg,
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isEquals) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        when {
                                            isClear -> {
                                                expression = ""
                                                lastSolution = null
                                                lastSavedId = null
                                            }
                                            isBackspace -> {
                                                if (expression.isNotEmpty()) {
                                                    expression = expression.dropLast(1)
                                                }
                                            }
                                            isEquals -> pressEquals()
                                            else -> {
                                                expression += label
                                            }
                                        }
                                    }
                                    .testTag("calc_key_$key")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isBackspace) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Backspace",
                                            tint = fg,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else {
                                        Text(
                                            text = label,
                                            fontSize = 21.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = fg
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
