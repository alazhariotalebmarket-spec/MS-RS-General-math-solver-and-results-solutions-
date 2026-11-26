package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.AppLanguage
import com.example.core.DigitHelper
import com.example.core.LocalLanguage
import com.example.core.LocalStrings
import com.example.data.MathLayoutType
import com.example.data.MathSourceType
import com.example.domain.MathSolveResult
import com.example.domain.MathSolverEngine
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalMsRsColors
import kotlinx.coroutines.launch

private enum class CalcPadTab {
    BASIC,
    SCIENTIFIC,
    FORMULAS
}

private data class MathQuickAction(
    val label: String,
    val insertText: String
)

private val SMART_FUNCTION_TOKENS = listOf(
    "asin(", "acos(", "atan(", "sin(", "cos(", "tan(",
    "log(", "ln(", "√(", "∛(", " ) "
)

@Composable
fun CalculatorSolverScreen(
    onBack: () -> Unit,
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
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val isBn = lang == AppLanguage.BANGLA

    var useBanglaDigits by remember(lang) { mutableStateOf(isBn) }
    var useKeyboardInput by remember { mutableStateOf(false) }
    var activePadTab by remember { mutableStateOf(CalcPadTab.BASIC) }
    var activeFormulaSheet by remember { mutableStateOf<CurriculumFormulaType?>(null) }

    var expression by remember { mutableStateOf("") }
    var lastSolution by remember { mutableStateOf<MathSolveResult.Success?>(null) }
    var lastSavedId by remember { mutableStateOf<Long?>(null) }
    var lastNumericAns by remember { mutableStateOf<String?>(null) }
    var justSolved by remember { mutableStateOf(false) }
    val sessionHistory = remember { mutableStateListOf<String>() }

    val exprHorizontalScroll = rememberScrollState()

    fun display(s: String): String =
        if (useBanglaDigits) DigitHelper.toBanglaDigits(s) else DigitHelper.toEnglishDigits(s)

    LaunchedEffect(expression) {
        exprHorizontalScroll.animateScrollTo(exprHorizontalScroll.maxValue)
    }

    val livePreview = remember(expression, useBanglaDigits, lang) {
        if (expression.isBlank()) {
            null
        } else {
            (MathSolverEngine.solve(expression, useBanglaDigits, lang) as? MathSolveResult.Success)?.answerText
        }
    }

    fun solveNow(exprToSolve: String = expression) {
        if (exprToSolve.isBlank()) return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        keyboardController?.hide()
        onSolveExpression(
            exprToSolve,
            useBanglaDigits,
            MathSourceType.CALCULATOR,
            null,
            { savedId, success ->
                lastSavedId = savedId
                lastSolution = success
                justSolved = true
                val engAns = DigitHelper.toEnglishDigits(success.answerText).trim()
                if (Regex("""^-?\d+(?:\.\d+)?$""").matches(engAns)) {
                    lastNumericAns = engAns
                }
                val trimmed = exprToSolve.trim()
                sessionHistory.remove(trimmed)
                sessionHistory.add(0, trimmed)
                if (sessionHistory.size > 6) {
                    sessionHistory.removeAt(sessionHistory.lastIndex)
                }
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

    fun smartBackspace() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        justSolved = false
        if (expression.isEmpty()) return
        val matchedToken = SMART_FUNCTION_TOKENS.firstOrNull { expression.endsWith(it) }
        expression = if (matchedToken != null) {
            expression.dropLast(matchedToken.length)
        } else {
            expression.dropLast(1)
        }
    }

    fun appendKeypadToken(token: String, isOperator: Boolean = false) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (justSolved) {
            justSolved = false
            val ans = lastNumericAns
            if (isOperator && ans != null) {
                expression = display(ans) + token
                return
            } else if (!isOperator) {
                expression = token
                return
            }
        }
        expression += token
    }

    val basicQuickRow = remember {
        listOf(
            MathQuickAction("x", "x"),
            MathQuickAction("=", "="),
            MathQuickAction(",", ", "),
            MathQuickAction("√", "√("),
            MathQuickAction("x²", "²"),
            MathQuickAction("%", "%"),
            MathQuickAction("/", "/"),
            MathQuickAction("π", "π"),
            MathQuickAction("^", "^"),
            MathQuickAction(") (", " ) ")
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(ext.emeraldGradientStart, ext.emeraldGradientEnd)
                        )
                    )
                    .statusBarsPadding()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("top_bar_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.calculatorSolverTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isBn) "শিশু–১০ম শ্রেণি • খাতার নিয়মে পূর্ণ সমাধান" else "Play–Class 10 • Step-by-Step Khata",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.goldBright,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 1-Tap Bangla (১-০) / English (1-0) Digit Toggle
                    Surface(
                        color = Color.Black.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            DigitSegmentPill(
                                label = "১-০ বাং",
                                selected = useBanglaDigits,
                                onClick = {
                                    useBanglaDigits = true
                                    expression = DigitHelper.toBanglaDigits(expression)
                                },
                                modifier = Modifier.testTag("calc_bangla_toggle")
                            )
                            DigitSegmentPill(
                                label = "1-0 EN",
                                selected = !useBanglaDigits,
                                onClick = {
                                    useBanglaDigits = false
                                    expression = DigitHelper.toEnglishDigits(expression)
                                },
                                modifier = Modifier.testTag("calc_english_toggle")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Clean Precision Digital Display Card (Separate from lined Khata)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 6.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 2.dp,
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Display Utility Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (useKeyboardInput) ext.goldContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(
                                1.dp,
                                if (useKeyboardInput) ext.onGoldContainer.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    useKeyboardInput = !useKeyboardInput
                                }
                                .testTag("calc_input_mode_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = if (useKeyboardInput) Icons.Filled.Dialpad else Icons.Filled.Keyboard,
                                    contentDescription = null,
                                    tint = if (useKeyboardInput) ext.onGoldContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (useKeyboardInput) strings.calcKeypadToggle else strings.calcKeyboardToggle,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (useKeyboardInput) ext.onGoldContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (lastNumericAns != null) {
                                Surface(
                                    color = ext.solverBlueContainer,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, ext.solverBlueStart.copy(alpha = 0.28f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { appendKeypadToken(display(lastNumericAns!!)) }
                                ) {
                                    Text(
                                        text = "Ans: ${display(lastNumericAns!!)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ext.onSolverBlueContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "DEG",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Session History Chips
                    if (sessionHistory.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 1.dp)
                        ) {
                            items(sessionHistory) { prev ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            justSolved = false
                                            expression = display(prev)
                                        }
                                ) {
                                    Text(
                                        text = display(prev),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (useKeyboardInput) {
                        OutlinedTextField(
                            value = expression,
                            onValueChange = {
                                justSolved = false
                                expression = it
                            },
                            placeholder = {
                                Text(
                                    text = strings.calcInputHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingIcon = {
                                if (expression.isNotEmpty()) {
                                    IconButton(onClick = {
                                        expression = ""
                                        lastSolution = null
                                        lastSavedId = null
                                        justSolved = false
                                    }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            textStyle = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = ext.solverBlueStart
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { solveNow() }),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("calc_keyboard_input")
                        )

                        Button(
                            onClick = { solveNow() },
                            enabled = expression.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("calc_solve_now_btn")
                        ) {
                            Icon(Icons.Filled.Functions, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.solveNow, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    } else {
                        // Digital Expression + Live Preview Line
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(exprHorizontalScroll),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = if (expression.isEmpty()) display("0") else display(expression),
                                    fontSize = when {
                                        expression.length > 22 -> 24.sp
                                        expression.length > 14 -> 29.sp
                                        else -> 35.sp
                                    },
                                    lineHeight = 40.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (expression.isEmpty()) Color(0xFF8AA093) else ext.solverBlueStart,
                                    maxLines = 1,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.testTag("calc_display_text")
                                )
                            }

                            AnimatedVisibility(
                                visible = livePreview != null && expression.isNotBlank() && lastSolution?.originalInput != display(expression),
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                if (livePreview != null) {
                                    Text(
                                        text = "= ${display(livePreview)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Ruled Khata Solution Paper (Middle Expandable Workspace)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                color = ext.khataPaper,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 1.dp,
                border = BorderStroke(1.2.dp, ext.khataLine)
            ) {
                val khataLineColor = ext.khataLine
                val khataMarginColor = ext.khataMargin
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val gap = 28.dp.toPx()
                            var y = gap
                            while (y < size.height) {
                                drawLine(
                                    color = khataLineColor.copy(alpha = 0.65f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                                y += gap
                            }
                            val marginX = 26.dp.toPx()
                            drawLine(
                                color = khataMarginColor.copy(alpha = 0.5f),
                                start = Offset(marginX, 0f),
                                end = Offset(marginX, size.height),
                                strokeWidth = 1.4.dp.toPx()
                            )
                        }
                ) {
                    val sol = lastSolution
                    val savedId = lastSavedId
                    if (sol != null && savedId != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(start = 34.dp, end = 12.dp, top = 10.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Khata Notebook Lines
                            Surface(
                                color = Color.White.copy(alpha = 0.94f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, ext.khataLine),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = if (isBn) "খাতার সমাধান:" else "Khata Work:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ext.solverBlueStart
                                    )
                                    sol.khataLines.forEach { line ->
                                        Text(
                                            text = display(line),
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF112419)
                                        )
                                    }
                                }
                            }

                            // Answer Highlight + Copy & Khata PDF
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = strings.finalAnswer,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = display(sol.answerText),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = Color.White,
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    val ans = display(sol.answerText)
                                                    clipboardManager.setText(AnnotatedString(ans))
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            if (isBn) "উত্তর কপি হয়েছে: $ans" else "Copied: $ans"
                                                        )
                                                    }
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.ContentCopy,
                                                    contentDescription = "Copy",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier
                                                .heightIn(min = 40.dp)
                                                .clip(RoundedCornerShape(50))
                                                .clickable { onOpenSolution(savedId) }
                                                .testTag("calc_eye_solution_btn")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Visibility,
                                                    contentDescription = strings.viewSolution,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = if (isBn) "খাতা / PDF" else "Khata / PDF",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Numbered Step-by-Step Explanation
                            Surface(
                                color = Color.White.copy(alpha = 0.88f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, ext.khataLine)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = strings.stepByStepTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    sol.explanationSteps.forEachIndexed { idx, s ->
                                        Row(
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = CircleShape,
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = display("${idx + 1}"),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                            Text(
                                                text = display(s),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF22362B),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Clean Empty Khata Note (NO quick examples)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 36.dp, end = 16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (isBn) {
                                    "অংক লিখে '=' চাপুন — খাতার নিয়মে ধাপে ধাপে সমাধান দেখতে পাবেন"
                                } else {
                                    "Enter any math & press '=' for step-by-step Khata work"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 3. Professional 3-Tab Keypad Deck (1,2,3 / 4,5,6 / 7,8,9 / 0 Order!)
            if (!useKeyboardInput) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        // 3-Mode Segmented Tab Selector
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PadModeTab(
                                    label = if (isBn) "ক্যালকুলেটর" else "Basic",
                                    icon = Icons.Filled.Calculate,
                                    selected = activePadTab == CalcPadTab.BASIC,
                                    onClick = { activePadTab = CalcPadTab.BASIC },
                                    modifier = Modifier.weight(1f)
                                )
                                PadModeTab(
                                    label = if (isBn) "বৈজ্ঞানিক" else "Scientific",
                                    icon = Icons.Filled.Science,
                                    selected = activePadTab == CalcPadTab.SCIENTIFIC,
                                    onClick = { activePadTab = CalcPadTab.SCIENTIFIC },
                                    modifier = Modifier.weight(1f)
                                )
                                PadModeTab(
                                    label = if (isBn) "সূত্র (১-১০)" else "Formulas",
                                    icon = Icons.Filled.Functions,
                                    selected = activePadTab == CalcPadTab.FORMULAS,
                                    onClick = { activePadTab = CalcPadTab.FORMULAS },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        when (activePadTab) {
                            CalcPadTab.FORMULAS -> {
                                CurriculumFormulaGrid(
                                    language = lang,
                                    onSelectFormula = { activeFormulaSheet = it }
                                )
                            }

                            CalcPadTab.BASIC -> {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(basicQuickRow) { item ->
                                        Surface(
                                            color = ext.solverBlueContainer,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, ext.solverBlueStart.copy(alpha = 0.25f)),
                                            modifier = Modifier
                                                .height(35.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    val isOp = item.insertText in listOf("/", "^", "%")
                                                    appendKeypadToken(item.insertText, isOperator = isOp)
                                                }
                                                .testTag("calc_ext_${item.label}")
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            ) {
                                                Text(
                                                    text = item.label,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ext.onSolverBlueContainer,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                OrderedNumericPad(
                                    keyHeight = 48.dp,
                                    displayDigit = ::display,
                                    onClear = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        expression = ""
                                        lastSolution = null
                                        lastSavedId = null
                                        justSolved = false
                                    },
                                    onBackspace = { smartBackspace() },
                                    onEquals = { solveNow() },
                                    onKey = { token, isOp -> appendKeypadToken(token, isOp) }
                                )
                            }

                            CalcPadTab.SCIENTIFIC -> {
                                val sciRows = listOf(
                                    listOf(
                                        MathQuickAction("sin", "sin("),
                                        MathQuickAction("cos", "cos("),
                                        MathQuickAction("tan", "tan("),
                                        MathQuickAction("log", "log("),
                                        MathQuickAction("ln", "ln(")
                                    ),
                                    listOf(
                                        MathQuickAction("√", "√("),
                                        MathQuickAction("∛", "∛("),
                                        MathQuickAction("x²", "²"),
                                        MathQuickAction("x^y", "^"),
                                        MathQuickAction("x!", "!")
                                    ),
                                    listOf(
                                        MathQuickAction("π", "π"),
                                        MathQuickAction("e", "e"),
                                        MathQuickAction("x", "x"),
                                        MathQuickAction("=", "="),
                                        MathQuickAction("Ans", lastNumericAns?.let { display(it) } ?: display("0"))
                                    )
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    sciRows.forEach { row ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            row.forEach { item ->
                                                Surface(
                                                    color = ext.goldContainer.copy(alpha = 0.72f),
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, ext.onGoldContainer.copy(alpha = 0.24f)),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(34.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            val isOp = item.insertText in listOf("^", "²")
                                                            appendKeypadToken(item.insertText, isOperator = isOp)
                                                        }
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = item.label,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ext.onGoldContainer,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                OrderedNumericPad(
                                    keyHeight = 42.dp,
                                    displayDigit = ::display,
                                    onClear = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        expression = ""
                                        lastSolution = null
                                        lastSavedId = null
                                        justSolved = false
                                    },
                                    onBackspace = { smartBackspace() },
                                    onEquals = { solveNow() },
                                    onKey = { token, isOp -> appendKeypadToken(token, isOp) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val formula = activeFormulaSheet
    if (formula != null) {
        FormulaInputBottomSheet(
            formula = formula,
            language = lang,
            useBanglaDigits = useBanglaDigits,
            onDismiss = { activeFormulaSheet = null },
            onSubmitExpression = { builtExpr ->
                val formatted = display(builtExpr)
                expression = formatted
                solveNow(formatted)
                activePadTab = CalcPadTab.BASIC
            }
        )
    }
}

@Composable
private fun OrderedNumericPad(
    keyHeight: Dp,
    displayDigit: (String) -> String,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onEquals: () -> Unit,
    onKey: (token: String, isOperator: Boolean) -> Unit
) {
    val ext = LocalMsRsColors.current
    // Ordered strictly 1,2,3 -> 4,5,6 -> 7,8,9 -> 0 (and ১,২,৩ -> ৪,৫,৬ -> ৭,৮,৯ -> ০)
    val keys = listOf(
        listOf("AC", "(", ")", "÷"),
        listOf("1", "2", "3", "×"),
        listOf("4", "5", "6", "-"),
        listOf("7", "8", "9", "+"),
        listOf("0", ".", "⌫", "=")
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { key ->
                    val isOperator = key in listOf("÷", "×", "-", "+", "(", ")")
                    val isEquals = key == "="
                    val isClear = key == "AC"
                    val isBackspace = key == "⌫"
                    val label = if (key in "0".."9") displayDigit(key) else key

                    val bg = when {
                        isClear -> ext.failContainer
                        isBackspace -> ext.goldContainer
                        isOperator -> ext.solverBlueContainer
                        else -> MaterialTheme.colorScheme.background
                    }
                    val fg = when {
                        isEquals -> Color.White
                        isClear -> ext.failRed
                        isBackspace -> ext.onGoldContainer
                        isOperator -> ext.onSolverBlueContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    val border = when {
                        isEquals -> ext.emeraldGradientStart
                        isClear -> ext.failRed.copy(alpha = 0.28f)
                        isBackspace -> ext.onGoldContainer.copy(alpha = 0.28f)
                        isOperator -> ext.solverBlueStart.copy(alpha = 0.26f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                    }

                    Surface(
                        color = if (isEquals) Color.Transparent else bg,
                        shape = RoundedCornerShape(15.dp),
                        shadowElevation = if (isEquals) 4.dp else 1.dp,
                        border = BorderStroke(1.dp, border),
                        modifier = Modifier
                            .weight(1f)
                            .height(keyHeight)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(15.dp))
                            .then(
                                if (isEquals) {
                                    Modifier.background(
                                        Brush.linearGradient(
                                            listOf(ext.emeraldGradientStart, ext.emeraldGradientEnd)
                                        )
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable {
                                when {
                                    isClear -> onClear()
                                    isBackspace -> onBackspace()
                                    isEquals -> onEquals()
                                    else -> onKey(label, key in listOf("÷", "×", "-", "+"))
                                }
                            }
                            .testTag("calc_key_${if (key == "AC") "C" else key}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isBackspace) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Backspace",
                                    tint = fg,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = label,
                                    fontSize = if (isEquals) 24.sp else if (isClear) 15.sp else 21.sp,
                                    fontWeight = FontWeight.ExtraBold,
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

@Composable
private fun PadModeTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        shape = RoundedCornerShape(11.dp),
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DigitSegmentPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) Color.White else Color.Transparent,
        shape = RoundedCornerShape(50),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color(0xFF0E7A3D) else Color.White,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}
