package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
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
import com.example.ui.components.MsRsTopBar
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalMsRsColors
import kotlinx.coroutines.launch

private data class MathQuickAction(
    val label: String,
    val insertText: String,
    val replaceAll: Boolean = false
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

    var useBanglaDigits by remember(lang) { mutableStateOf(lang == AppLanguage.BANGLA) }
    var useKeyboardInput by remember { mutableStateOf(false) }
    var expression by remember { mutableStateOf("") }
    var lastSolution by remember { mutableStateOf<MathSolveResult.Success?>(null) }
    var lastSavedId by remember { mutableStateOf<Long?>(null) }

    fun display(s: String): String =
        if (useBanglaDigits) DigitHelper.toBanglaDigits(s) else DigitHelper.toEnglishDigits(s)

    fun solveNow(exprToSolve: String = expression) {
        if (exprToSolve.isBlank()) return
        keyboardController?.hide()
        onSolveExpression(
            exprToSolve,
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

    val quickSymbols = remember(lang, useBanglaDigits) {
        val isBn = lang == AppLanguage.BANGLA
        listOf(
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
            MathQuickAction(") (", " ) "),
            MathQuickAction(if (isBn) "লসাগু" else "LCM", if (isBn) "লসাগু " else "LCM ", replaceAll = true),
            MathQuickAction(if (isBn) "গসাগু" else "GCD", if (isBn) "গসাগু " else "GCD ", replaceAll = true),
            MathQuickAction(if (isBn) "গড়" else "AVG", if (isBn) "গড় " else "AVG ", replaceAll = true),
            MathQuickAction(if (isBn) "এর %" else "% of", if (isBn) " এর " else "% of ")
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(modifier = Modifier.statusBarsPadding()) {
                MsRsTopBar(
                    title = strings.calculatorSolverTitle,
                    onBack = onBack,
                    actions = {
                        IconButton(
                            onClick = { useKeyboardInput = !useKeyboardInput },
                            modifier = Modifier.testTag("calc_input_mode_action")
                        ) {
                            Icon(
                                imageVector = if (useKeyboardInput) Icons.Filled.Dialpad else Icons.Filled.Keyboard,
                                contentDescription = if (useKeyboardInput) strings.calcKeypadToggle else strings.calcKeyboardToggle,
                                tint = Color.White
                            )
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
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Digit Language Switch + Keyboard/Keypad Mode Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = useBanglaDigits,
                    onClick = {
                        useBanglaDigits = true
                        expression = DigitHelper.toBanglaDigits(expression)
                    },
                    label = { Text(strings.banglaDigitsLabel, maxLines = 1) },
                    modifier = Modifier.testTag("calc_bangla_toggle")
                )
                FilterChip(
                    selected = !useBanglaDigits,
                    onClick = {
                        useBanglaDigits = false
                        expression = DigitHelper.toEnglishDigits(expression)
                    },
                    label = { Text(strings.englishDigitsLabel, maxLines = 1) },
                    modifier = Modifier.testTag("calc_english_toggle")
                )
                FilterChip(
                    selected = useKeyboardInput,
                    onClick = { useKeyboardInput = !useKeyboardInput },
                    leadingIcon = {
                        Icon(
                            imageVector = if (useKeyboardInput) Icons.Filled.Dialpad else Icons.Filled.Keyboard,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = {
                        Text(
                            text = if (useKeyboardInput) strings.calcKeypadToggle else strings.calcKeyboardToggle,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.testTag("calc_input_mode_toggle")
                )
            }

            // Display + Khata Solution area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = ext.khataPaper),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { solveNow() }),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("calc_keyboard_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { solveNow() },
                                enabled = expression.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("calc_solve_now_btn")
                            ) {
                                Icon(Icons.Filled.Functions, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(strings.solveNow, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Sample problem pills for instant testing
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(samplePresets) { sample ->
                                Surface(
                                    color = ext.solverBlueContainer,
                                    shape = RoundedCornerShape(50),
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
                    } else {
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
                        if (expression.isEmpty() && lastSolution == null) {
                            Text(
                                text = strings.calcInputHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(top = 4.dp)
                            ) {
                                items(samplePresets) { sample ->
                                    Surface(
                                        color = ext.solverBlueContainer,
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
                                                expression = sample
                                                solveNow(sample)
                                            }
                                    ) {
                                        Text(
                                            text = sample,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ext.onSolverBlueContainer,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val sol = lastSolution
                    val savedId = lastSavedId
                    AnimatedVisibility(visible = sol != null && savedId != null) {
                        if (sol != null && savedId != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, ext.khataLine),
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
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
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

            if (!useKeyboardInput) {
                // Extended Math Bar (algebra, powers, roots, percentage, LCM/GCD/AVG, division bracket)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(quickSymbols) { item ->
                        Surface(
                            color = ext.solverBlueContainer,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ext.solverBlueStart.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
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
                                    color = ext.onSolverBlueContainer,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Main Keypad
                val keys = listOf(
                    listOf("C", "(", ")", "÷"),
                    listOf("7", "8", "9", "×"),
                    listOf("4", "5", "6", "-"),
                    listOf("1", "2", "3", "+"),
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
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isEquals) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(14.dp))
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
                                                isEquals -> solveNow()
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
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Text(
                                                text = label,
                                                fontSize = 20.sp,
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
}
