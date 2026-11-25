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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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

private data class MathQuickAction(
    val label: String,
    val insertText: String,
    val replaceAll: Boolean = false,
    val isCommand: Boolean = false
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

    var useBanglaDigits by remember(lang) { mutableStateOf(lang == AppLanguage.BANGLA) }
    var useKeyboardInput by remember { mutableStateOf(false) }
    var expression by remember { mutableStateOf("") }
    var lastSolution by remember { mutableStateOf<MathSolveResult.Success?>(null) }
    var lastSavedId by remember { mutableStateOf<Long?>(null) }
    val sessionHistory = remember { mutableStateListOf<String>() }

    fun display(s: String): String =
        if (useBanglaDigits) DigitHelper.toBanglaDigits(s) else DigitHelper.toEnglishDigits(s)

    // Non-persisted live preview while typing
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

    val quickSymbols = remember(lang, useBanglaDigits) {
        val isBn = lang == AppLanguage.BANGLA
        listOf(
            MathQuickAction(if (isBn) "লসাগু" else "LCM", if (isBn) "লসাগু " else "LCM ", replaceAll = true, isCommand = true),
            MathQuickAction(if (isBn) "গসাগু" else "GCD", if (isBn) "গসাগু " else "GCD ", replaceAll = true, isCommand = true),
            MathQuickAction(if (isBn) "গড়" else "AVG", if (isBn) "গড় " else "AVG ", replaceAll = true, isCommand = true),
            MathQuickAction(if (isBn) "এর %" else "% of", if (isBn) " এর " else "% of ", isCommand = true),
            MathQuickAction("x", "x"),
            MathQuickAction("=", "="),
            MathQuickAction("√", "√"),
            MathQuickAction("²", "²"),
            MathQuickAction("^", "^"),
            MathQuickAction("%", "%"),
            MathQuickAction("/", "/"),
            MathQuickAction(",", ", "),
            MathQuickAction("!", "!"),
            MathQuickAction("π", "π"),
            MathQuickAction("∛", "∛"),
            MathQuickAction(") (", " ) ")
        )
    }

    val samplePresets = remember(lang, useBanglaDigits) {
        if (lang == AppLanguage.BANGLA) {
            listOf(
                display("2x+5=15"),
                display("x²-5x+6=0"),
                display("1/2 + 3/4"),
                display("200 এর 15%"),
                display("লসাগু 12, 18, 24"),
                display("√144 + 5² - 3!"),
                display("2 ) 6 (")
            )
        } else {
            listOf(
                display("2x+5=15"),
                display("x²-5x+6=0"),
                display("1/2 + 3/4"),
                display("15% of 200"),
                display("LCM 12, 18, 24"),
                display("√144 + 5² - 3!"),
                display("2 ) 6 (")
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Integrated Emerald Gradient Top Header + Sleek Mode Pill Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(ext.emeraldGradientStart, ext.emeraldGradientEnd)
                        )
                    )
                    .statusBarsPadding()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
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
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (lang == AppLanguage.BANGLA) "খাতার নিয়মে ধাপে ধাপে সমাধান" else "Step-by-Step Khata Solutions",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.goldBright,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Quick Mode Switch Action in Top Bar
                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = CircleShape,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                useKeyboardInput = !useKeyboardInput
                            }
                            .testTag("calc_input_mode_action")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (useKeyboardInput) Icons.Filled.Dialpad else Icons.Filled.Keyboard,
                                contentDescription = if (useKeyboardInput) strings.calcKeypadToggle else strings.calcKeyboardToggle,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (useKeyboardInput) strings.calcKeypadToggle else strings.calcKeyboardToggle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Segmented Digit Toggle + Session History Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Segmented Bangla / English Digit Pill
                    Surface(
                        color = Color.Black.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            DigitSegmentPill(
                                label = "বাংলা (০-৯)",
                                selected = useBanglaDigits,
                                onClick = {
                                    useBanglaDigits = true
                                    expression = DigitHelper.toBanglaDigits(expression)
                                },
                                modifier = Modifier.testTag("calc_bangla_toggle")
                            )
                            DigitSegmentPill(
                                label = "EN (0-9)",
                                selected = !useBanglaDigits,
                                onClick = {
                                    useBanglaDigits = false
                                    expression = DigitHelper.toEnglishDigits(expression)
                                },
                                modifier = Modifier.testTag("calc_english_toggle")
                            )
                        }
                    }

                    // Input Mode Chip
                    Surface(
                        color = if (useKeyboardInput) ext.goldBright else Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { useKeyboardInput = !useKeyboardInput }
                            .testTag("calc_input_mode_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (useKeyboardInput) Icons.Filled.Dialpad else Icons.Filled.Keyboard,
                                contentDescription = null,
                                tint = if (useKeyboardInput) Color(0xFF1A281E) else Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (useKeyboardInput) strings.calcKeypadToggle else strings.calcKeyboardToggle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (useKeyboardInput) Color(0xFF1A281E) else Color.White,
                                maxLines = 1
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Ruled Khata Display + Live Answer + Step-by-Step Solution Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                color = ext.khataPaper,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 2.dp,
                border = BorderStroke(1.2.dp, ext.khataLine)
            ) {
                val khataLineColor = ext.khataLine
                val khataMarginColor = ext.khataMargin
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val gap = 30.dp.toPx()
                            var y = gap
                            while (y < size.height) {
                                drawLine(
                                    color = khataLineColor.copy(alpha = 0.72f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                                y += gap
                            }
                            val marginX = 28.dp.toPx()
                            drawLine(
                                color = khataMarginColor.copy(alpha = 0.55f),
                                start = Offset(marginX, 0f),
                                end = Offset(marginX, size.height),
                                strokeWidth = 1.4.dp.toPx()
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 38.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Session recent chips if available
                        if (sessionHistory.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                items(sessionHistory) { prev ->
                                    Surface(
                                        color = Color.White.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, ext.khataLine),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
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
                                onValueChange = { expression = it },
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
                                    focusedContainerColor = Color.White.copy(alpha = 0.92f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.85f),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = ext.khataLine
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
                            // Keypad Mode Display
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = if (expression.isEmpty()) display("0") else display(expression),
                                    fontSize = if (expression.length > 18) 24.sp else 32.sp,
                                    lineHeight = 36.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (expression.isEmpty()) Color(0xFF8AA093) else ext.solverBlueStart,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("calc_display_text")
                                )

                                // Live Instant Answer Preview Pill before pressing '='
                                AnimatedVisibility(
                                    visible = livePreview != null && expression.isNotBlank() && lastSolution?.originalInput != display(expression),
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    if (livePreview != null) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier.padding(top = 6.dp)
                                        ) {
                                            Text(
                                                text = "= ${display(livePreview)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Empty State & 1-Tap Quick Examples
                        if (expression.isEmpty() && lastSolution == null) {
                            Spacer(Modifier.height(2.dp))
                            Surface(
                                color = Color.White.copy(alpha = 0.78f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, ext.khataLine)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (lang == AppLanguage.BANGLA) "দ্রুত উদাহরণে ট্যাপ করুন:" else "Try quick examples:",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(samplePresets) { sample ->
                                            Surface(
                                                color = ext.solverBlueContainer,
                                                shape = RoundedCornerShape(50),
                                                border = BorderStroke(1.dp, ext.solverBlueStart.copy(alpha = 0.25f)),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .clickable {
                                                        expression = sample
                                                        solveNow(sample)
                                                    }
                                            ) {
                                                Text(
                                                    text = sample,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = ext.onSolverBlueContainer,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Solved Khata Result Block
                        val sol = lastSolution
                        val savedId = lastSavedId
                        AnimatedVisibility(
                            visible = sol != null && savedId != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            if (sol != null && savedId != null) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    HorizontalDivider(color = ext.khataLine)

                                    // Notebook Work Box
                                    Surface(
                                        color = Color.White.copy(alpha = 0.94f),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, ext.khataLine),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = if (lang == AppLanguage.BANGLA) "খাতার সমাধান:" else "Khata Work:",
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

                                    // Answer Highlight + Action Buttons (Copy & Eye View)
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
                                                                    if (lang == AppLanguage.BANGLA) "উত্তর কপি হয়েছে: $ans" else "Copied: $ans"
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
                                                            text = if (lang == AppLanguage.BANGLA) "খাতা / PDF" else "Khata / PDF",
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

                                    // Step-by-step teacher explanation
                                    Surface(
                                        color = Color.White.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, ext.khataLine)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = strings.stepByStepTitle,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            sol.explanationSteps.forEach { s ->
                                                Text(
                                                    text = "• ${display(s)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF283C30)
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

            // Sculpted Keypad Deck
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
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        // Smart Function & Command Ribbon
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(quickSymbols) { item ->
                                val chipBg = if (item.isCommand) ext.goldContainer else ext.solverBlueContainer
                                val chipFg = if (item.isCommand) ext.onGoldContainer else ext.onSolverBlueContainer
                                val chipBorder = if (item.isCommand) ext.onGoldContainer.copy(alpha = 0.28f) else ext.solverBlueStart.copy(alpha = 0.25f)
                                Surface(
                                    color = chipBg,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, chipBorder),
                                    modifier = Modifier
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (item.replaceAll && expression.isBlank()) {
                                                expression = item.insertText
                                            } else if (item.replaceAll) {
                                                expression = item.insertText + expression
                                            } else {
                                                expression += item.insertText
                                            }
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
                                            color = chipFg,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Main Tactile 4-Column Keypad
                        val keys = listOf(
                            listOf("C", "(", ")", "÷"),
                            listOf("7", "8", "9", "×"),
                            listOf("4", "5", "6", "-"),
                            listOf("1", "2", "3", "+"),
                            listOf("0", ".", "⌫", "=")
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            keys.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    row.forEach { key ->
                                        val isOperator = key in listOf("÷", "×", "-", "+", "(", ")")
                                        val isEquals = key == "="
                                        val isClear = key == "C"
                                        val isBackspace = key == "⌫"
                                        val label = if (key in "0".."9") display(key) else key

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
                                            isClear -> ext.failRed.copy(alpha = 0.25f)
                                            isBackspace -> ext.onGoldContainer.copy(alpha = 0.28f)
                                            isOperator -> ext.solverBlueStart.copy(alpha = 0.25f)
                                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        }

                                        Surface(
                                            color = if (isEquals) Color.Transparent else bg,
                                            shape = RoundedCornerShape(15.dp),
                                            shadowElevation = if (isEquals) 4.dp else 1.dp,
                                            border = BorderStroke(1.dp, border),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
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
                                                        isClear -> {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            expression = ""
                                                            lastSolution = null
                                                            lastSavedId = null
                                                        }
                                                        isBackspace -> {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            if (expression.isNotEmpty()) {
                                                                expression = expression.dropLast(1)
                                                            }
                                                        }
                                                        isEquals -> solveNow()
                                                        else -> {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                } else {
                                                    Text(
                                                        text = label,
                                                        fontSize = if (isEquals) 24.sp else 20.sp,
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
                }
            }
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
