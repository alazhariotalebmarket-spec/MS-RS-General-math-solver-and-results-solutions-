package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.AppLanguage
import com.example.core.DigitHelper
import com.example.ui.theme.LocalMsRsColors

enum class CurriculumFormulaType(
    val titleBn: String,
    val titleEn: String,
    val subtitleBn: String,
    val subtitleEn: String,
    val gradeTagBn: String,
    val gradeTagEn: String
) {
    LCM("লসাগু (LCM)", "LCM", "২ বা ততোধিক সংখ্যার লসাগু", "Least Common Multiple", "৪র্থ–৮ম", "Cl 4-8"),
    GCD("গসাগু (GCD)", "GCD / HCF", "২ বা ততোধিক সংখ্যার গসাগু", "Greatest Common Divisor", "৪র্থ–৮ম", "Cl 4-8"),
    PRIME("মৌলিক উৎপাদক", "Prime Factors", "সংখ্যার মৌলিক উৎপাদকে বিশ্লেষণ", "Prime Factorization", "৫ম–৮ম", "Cl 5-8"),
    SIMPLE_INTEREST("সরল মুনাফা", "Simple Interest", "I = (P × n × r) ÷ 100", "I = (P × n × r) / 100", "৭ম–১০ম", "Cl 7-10"),
    COMPOUND_INTEREST("চক্রবৃদ্ধি মুনাফা", "Compound Interest", "C = P(1 + r/100)^n", "C = P(1 + r/100)^n", "৮ম–১০ম", "Cl 8-10"),
    PERCENT_OF("শতকরা (%)", "Percentage", "কোনো সংখ্যার % নির্ণয়", "X% of Y calculation", "৫ম–১০ম", "Cl 5-10"),
    PROFIT_LOSS("লাভ ও ক্ষতি", "Profit & Loss", "ক্রয় ও বিক্রয়মূল্য থেকে %", "Cost vs Selling Price", "৫ম–১০ম", "Cl 5-10"),
    FRACTION("ভগ্নাংশ (+ - × ÷)", "Fractions", "লব ও হরের যোগ/বিয়োগ/গুণ/ভাগ", "Add, Sub, Mul, Div", "৪র্থ–৭ম", "Cl 4-7"),
    AREA_RECT("আয়ত ও বর্গ", "Rect & Square", "ক্ষেত্রফল ও পরিসীমা নির্ণয়", "Area & Perimeter", "৪র্থ–১০ম", "Cl 4-10"),
    CIRCLE("বৃত্তের পরিমাপ", "Circle", "ব্যাসার্ধ থেকে ক্ষেত্রফল ও পরিধি", "Area & Circumference", "৬ষ্ঠ–১০ম", "Cl 6-10"),
    TRIANGLE("ত্রিভুজের ক্ষেত্রফল", "Triangle Area", "ভূমি-উচ্চতা বা ৩ বাহু (হিরন)", "Base-Height or 3 Sides", "৬ষ্ঠ–১০ম", "Cl 6-10"),
    AVG_MEDIAN("গড় ও মধ্যক", "Mean & Median", "উপাত্তের গড় অথবা মধ্যক", "Average or Median", "৫ম–১০ম", "Cl 5-10"),
    LONG_DIVISION("ভাগ খাতা ) (", "Long Division", "খাতার নিয়মে ভাগফল ও ভাগশেষ", "Step-by-step Division", "১ম–৫ম", "Cl 1-5"),
    QUADRATIC("দ্বিঘাত সমীকরণ", "Quadratic Eq.", "ax² + bx + c = 0 সমাধান", "Solve ax² + bx + c = 0", "৯ম–১০ম", "Cl 9-10")
}

@Composable
fun CurriculumFormulaGrid(
    language: AppLanguage,
    onSelectFormula: (CurriculumFormulaType) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMsRsColors.current
    val isBn = language == AppLanguage.BANGLA
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(CurriculumFormulaType.entries) { formula ->
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectFormula(formula) }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBn) formula.titleBn else formula.titleEn,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = ext.solverBlueContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isBn) formula.gradeTagBn else formula.gradeTagEn,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ext.onSolverBlueContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = if (isBn) formula.subtitleBn else formula.subtitleEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormulaInputBottomSheet(
    formula: CurriculumFormulaType,
    language: AppLanguage,
    useBanglaDigits: Boolean,
    onDismiss: () -> Unit,
    onSubmitExpression: (String) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var field1 by remember(formula) { mutableStateOf("") }
    var field2 by remember(formula) { mutableStateOf("") }
    var field3 by remember(formula) { mutableStateOf("") }
    var field4 by remember(formula) { mutableStateOf("") }
    var selectedOp by remember(formula) { mutableStateOf("+") }
    var avgMode by remember(formula) { mutableStateOf("AVG") }
    var errorMsg by remember(formula) { mutableStateOf<String?>(null) }

    fun cleanNum(raw: String): String = DigitHelper.toEnglishDigits(raw.trim())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Functions,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = if (isBn) formula.titleBn else formula.titleEn,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (isBn) formula.subtitleBn else formula.subtitleEn,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider()

            when (formula) {
                CurriculumFormulaType.LCM,
                CurriculumFormulaType.GCD -> {
                    FormulaNumberField(
                        value = field1,
                        onValueChange = { field1 = it; errorMsg = null },
                        label = if (isBn) "সংখ্যাগুলো লিখুন (কমা বা স্পেস দিয়ে)" else "Enter numbers (comma or space separated)",
                        hint = if (useBanglaDigits) "যেমন: ১২, ১৮, ২৪" else "e.g. 12, 18, 24",
                        keyboardType = KeyboardType.Text
                    )
                }

                CurriculumFormulaType.PRIME -> {
                    FormulaNumberField(
                        value = field1,
                        onValueChange = { field1 = it; errorMsg = null },
                        label = if (isBn) "সংখ্যাটি লিখুন" else "Enter integer (> 1)",
                        hint = if (useBanglaDigits) "যেমন: ১২০" else "e.g. 120"
                    )
                }

                CurriculumFormulaType.SIMPLE_INTEREST,
                CurriculumFormulaType.COMPOUND_INTEREST -> {
                    FormulaNumberField(
                        value = field1,
                        onValueChange = { field1 = it; errorMsg = null },
                        label = if (isBn) "আসল / মূলধন (P)" else "Principal (P)",
                        hint = if (useBanglaDigits) "যেমন: ১০০০০" else "e.g. 10000"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FormulaNumberField(
                            value = field2,
                            onValueChange = { field2 = it; errorMsg = null },
                            label = if (isBn) "সময় বছর (n)" else "Time in years (n)",
                            hint = if (useBanglaDigits) "যেমন: ৩" else "e.g. 3",
                            modifier = Modifier.weight(1f)
                        )
                        FormulaNumberField(
                            value = field3,
                            onValueChange = { field3 = it; errorMsg = null },
                            label = if (isBn) "মুনাফার হার % (r)" else "Rate % (r)",
                            hint = if (useBanglaDigits) "যেমন: ৮" else "e.g. 8",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                CurriculumFormulaType.PERCENT_OF -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FormulaNumberField(
                            value = field1,
                            onValueChange = { field1 = it; errorMsg = null },
                            label = if (isBn) "মূল সংখ্যা" else "Base Number",
                            hint = if (useBanglaDigits) "যেমন: ৫০০" else "e.g. 500",
                            modifier = Modifier.weight(1f)
                        )
                        FormulaNumberField(
                            value = field2,
                            onValueChange = { field2 = it; errorMsg = null },
                            label = if (isBn) "শতকরা (%)" else "Percent (%)",
                            hint = if (useBanglaDigits) "যেমন: ১৫" else "e.g. 15",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                CurriculumFormulaType.PROFIT_LOSS -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FormulaNumberField(
                            value = field1,
                            onValueChange = { field1 = it; errorMsg = null },
                            label = if (isBn) "ক্রয়মূল্য (CP)" else "Cost Price",
                            hint = if (useBanglaDigits) "যেমন: ৫০০" else "e.g. 500",
                            modifier = Modifier.weight(1f)
                        )
                        FormulaNumberField(
                            value = field2,
                            onValueChange = { field2 = it; errorMsg = null },
                            label = if (isBn) "বিক্রয়মূল্য (SP)" else "Selling Price",
                            hint = if (useBanglaDigits) "যেমন: ৬০০" else "e.g. 600",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                CurriculumFormulaType.FRACTION -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FormulaNumberField(
                            value = field1,
                            onValueChange = { field1 = it; errorMsg = null },
                            label = if (isBn) "১ম লব" else "Num 1",
                            hint = "1",
                            modifier = Modifier.weight(1f)
                        )
                        FormulaNumberField(
                            value = field2,
                            onValueChange = { field2 = it; errorMsg = null },
                            label = if (isBn) "১ম হর" else "Den 1",
                            hint = "2",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("+", "-", "×", "÷").forEach { op ->
                            val selected = selectedOp == op
                            Surface(
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedOp = op }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = op,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FormulaNumberField(
                            value = field3,
                            onValueChange = { field3 = it; errorMsg = null },
                            label = if (isBn) "২য় লব" else "Num 2",
                            hint = "3",
                            modifier = Modifier.weight(1f)
                        )
                        FormulaNumberField(
                            value = field4,
                            onValueChange = { field4 = it; errorMsg = null },
                            label = if (isBn) "২য় হর" else "Den 2",
                            hint = "4",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                CurriculumFormulaType.AREA_RECT -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FormulaNumberField(
                            value = field1,
                            onValueChange = { field1 = it; errorMsg = null },
                            label = if (isBn) "দৈর্ঘ্য / বাহু" else "Length / Side",
                            hint = if (useBanglaDigits) "১২" else "12",
                            modifier = Modifier.weight(1f)
                        )
                        FormulaNumberField(
                            value = field2,
                            onValueChange = { field2 = it; errorMsg = null },
                            label = if (isBn) "প্রস্থ (বর্গ হলে খালি)" else "Width (blank = square)",
                            hint = if (useBanglaDigits) "৮" else "8",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                CurriculumFormulaType.CIRCLE -> {
                    FormulaNumberField(
                        value = field1,
                        onValueChange = { field1 = it; errorMsg = null },
                        label = if (isBn) "ব্যাসার্ধ (r)" else "Radius (r)",
                        hint = if (useBanglaDigits) "যেমন: ৭" else "e.g. 7"
                    )
                }

                CurriculumFormulaType.TRIANGLE -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FormulaNumberField(
                            value = field1,
                            onValueChange = { field1 = it; errorMsg = null },
                            label = if (isBn) "ভূমি / বাহু a" else "Base / Side a",
                            hint = "10",
                            modifier = Modifier.weight(1f)
                        )
                        FormulaNumberField(
                            value = field2,
                            onValueChange = { field2 = it; errorMsg = null },
                            label = if (isBn) "উচ্চতা / বাহু b" else "Height / Side b",
                            hint = "6",
                            modifier = Modifier.weight(1f)
                        )
                        FormulaNumberField(
                            value = field3,
                            onValueChange = { field3 = it; errorMsg = null },
                            label = if (isBn) "বাহু c (ঐচ্ছিক)" else "Side c (opt)",
                            hint = "",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                CurriculumFormulaType.AVG_MEDIAN -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("AVG" to (if (isBn) "গড় (Mean)" else "Average"), "MED" to (if (isBn) "মধ্যক (Median)" else "Median")).forEach { (code, title) ->
                            val selected = avgMode == code
                            Surface(
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { avgMode = code }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = title,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    FormulaNumberField(
                        value = field1,
                        onValueChange = { field1 = it; errorMsg = null },
                        label = if (isBn) "সংখ্যাগুলো লিখুন (কমা বা স্পেস দিয়ে)" else "Enter numbers",
                        hint = if (useBanglaDigits) "১০, ১৫, ২০, ২৫" else "10, 15, 20, 25",
                        keyboardType = KeyboardType.Text
                    )
                }

                CurriculumFormulaType.LONG_DIVISION -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FormulaNumberField(
                            value = field1,
                            onValueChange = { field1 = it; errorMsg = null },
                            label = if (isBn) "ভাজক (যা দিয়ে ভাগ)" else "Divisor",
                            hint = if (useBanglaDigits) "১২" else "12",
                            modifier = Modifier.weight(1f)
                        )
                        FormulaNumberField(
                            value = field2,
                            onValueChange = { field2 = it; errorMsg = null },
                            label = if (isBn) "ভাজ্য (যাকে ভাগ)" else "Dividend",
                            hint = if (useBanglaDigits) "১৪৪" else "144",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                CurriculumFormulaType.QUADRATIC -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FormulaNumberField(
                            value = field1,
                            onValueChange = { field1 = it; errorMsg = null },
                            label = "a (x²)",
                            hint = "1",
                            modifier = Modifier.weight(1f)
                        )
                        FormulaNumberField(
                            value = field2,
                            onValueChange = { field2 = it; errorMsg = null },
                            label = "b (x)",
                            hint = "-5",
                            modifier = Modifier.weight(1f)
                        )
                        FormulaNumberField(
                            value = field3,
                            onValueChange = { field3 = it; errorMsg = null },
                            label = if (isBn) "ধ্রুবক c" else "Const c",
                            hint = "6",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (errorMsg != null) {
                Text(
                    text = errorMsg!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = {
                    val expr = buildExpressionForFormula(
                        formula = formula,
                        f1 = cleanNum(field1),
                        f2 = cleanNum(field2),
                        f3 = cleanNum(field3),
                        f4 = cleanNum(field4),
                        op = selectedOp,
                        avgMode = avgMode,
                        isBn = isBn
                    )
                    if (expr == null) {
                        errorMsg = if (isBn) "অনুগ্রহ করে সঠিক সংখ্যা লিখুন" else "Please enter valid numbers"
                    } else {
                        onSubmitExpression(expr)
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Filled.Calculate, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isBn) "সমাধান করুন" else "Solve Now",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun FormulaNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    hint: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Decimal
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        placeholder = { Text(hint) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    )
}

private fun buildExpressionForFormula(
    formula: CurriculumFormulaType,
    f1: String,
    f2: String,
    f3: String,
    f4: String,
    op: String,
    avgMode: String,
    isBn: Boolean
): String? {
    fun numsIn(s: String) = Regex("""-?\d+(?:\.\d+)?""").findAll(s).map { it.value }.toList()

    return when (formula) {
        CurriculumFormulaType.LCM -> {
            val n = numsIn(f1)
            if (n.size < 2) null else "${if (isBn) "লসাগু" else "LCM"} ${n.joinToString(", ")}"
        }
        CurriculumFormulaType.GCD -> {
            val n = numsIn(f1)
            if (n.size < 2) null else "${if (isBn) "গসাগু" else "GCD"} ${n.joinToString(", ")}"
        }
        CurriculumFormulaType.PRIME -> {
            val n = f1.toLongOrNull()
            if (n == null || n < 2) null else "${if (isBn) "মৌলিক উৎপাদক" else "Prime"} $n"
        }
        CurriculumFormulaType.SIMPLE_INTEREST -> {
            val p = f1.toDoubleOrNull() ?: return null
            val n = f2.toDoubleOrNull() ?: return null
            val r = f3.toDoubleOrNull() ?: return null
            "${if (isBn) "মুনাফা" else "Interest"} P=$p n=$n r=$r"
        }
        CurriculumFormulaType.COMPOUND_INTEREST -> {
            val p = f1.toDoubleOrNull() ?: return null
            val n = f2.toDoubleOrNull() ?: return null
            val r = f3.toDoubleOrNull() ?: return null
            "${if (isBn) "চক্রবৃদ্ধি" else "Compound"} P=$p n=$n r=$r"
        }
        CurriculumFormulaType.PERCENT_OF -> {
            val base = f1.toDoubleOrNull() ?: return null
            val pct = f2.toDoubleOrNull() ?: return null
            if (isBn) "$base এর $pct%" else "$pct% of $base"
        }
        CurriculumFormulaType.PROFIT_LOSS -> {
            val cp = f1.toDoubleOrNull() ?: return null
            val sp = f2.toDoubleOrNull() ?: return null
            "${if (isBn) "লাভ" else "Profit"} $cp, $sp"
        }
        CurriculumFormulaType.FRACTION -> {
            val n1 = f1.toLongOrNull() ?: return null
            val d1 = f2.toLongOrNull() ?: return null
            val n2 = f3.toLongOrNull() ?: return null
            val d2 = f4.toLongOrNull() ?: return null
            if (d1 == 0L || d2 == 0L) return null
            val cleanOp = when (op) {
                "×" -> "*"
                "÷" -> "/"
                else -> op
            }
            "$n1/$d1 $cleanOp $n2/$d2"
        }
        CurriculumFormulaType.AREA_RECT -> {
            val l = f1.toDoubleOrNull() ?: return null
            val w = f2.toDoubleOrNull()
            if (w != null) "${if (isBn) "ক্ষেত্রফল" else "Area"} $l, $w"
            else "${if (isBn) "ক্ষেত্রফল" else "Area"} $l"
        }
        CurriculumFormulaType.CIRCLE -> {
            val r = f1.toDoubleOrNull() ?: return null
            "${if (isBn) "বৃত্ত" else "Circle"} $r"
        }
        CurriculumFormulaType.TRIANGLE -> {
            val a = f1.toDoubleOrNull() ?: return null
            val b = f2.toDoubleOrNull() ?: return null
            val c = f3.toDoubleOrNull()
            if (c != null) "${if (isBn) "ত্রিভুজ" else "Triangle"} $a, $b, $c"
            else "${if (isBn) "ত্রিভুজ" else "Triangle"} $a, $b"
        }
        CurriculumFormulaType.AVG_MEDIAN -> {
            val n = numsIn(f1)
            if (n.size < 2) null
            else if (avgMode == "MED") "${if (isBn) "মধ্যক" else "Median"} ${n.joinToString(", ")}"
            else "${if (isBn) "গড়" else "AVG"} ${n.joinToString(", ")}"
        }
        CurriculumFormulaType.LONG_DIVISION -> {
            val divisor = f1.toDoubleOrNull() ?: return null
            val dividend = f2.toDoubleOrNull() ?: return null
            if (divisor == 0.0) null else "$divisor ) $dividend ("
        }
        CurriculumFormulaType.QUADRATIC -> {
            val a = f1.toDoubleOrNull() ?: return null
            val b = f2.toDoubleOrNull() ?: return null
            val c = f3.toDoubleOrNull() ?: return null
            val bTerm = if (b >= 0) "+${b}x" else "${b}x"
            val cTerm = if (c >= 0) "+$c" else "$c"
            "${a}x²$bTerm$cTerm=0"
        }
    }
}
