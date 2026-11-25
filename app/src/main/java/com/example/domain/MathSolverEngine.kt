package com.example.domain

import com.example.core.AppLanguage
import com.example.core.DigitHelper
import com.example.data.MathLayoutType
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

sealed class MathSolveResult {
    data class Success(
        val originalInput: String,
        val normalizedExpression: String,
        val answerText: String,
        val khataLines: List<String>,
        val explanationSteps: List<String>,
        val layoutType: MathLayoutType
    ) : MathSolveResult()

    data class Error(val message: String, val isDivideByZero: Boolean = false) : MathSolveResult()
}

object MathSolverEngine {

    fun normalizeInput(raw: String): String {
        var s = DigitHelper.toEnglishDigits(raw.trim())
        s = s.replace('×', '*')
            .replace('x', '*')
            .replace('X', '*')
            .replace('÷', '/')
            .replace('−', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace("=", "")
            .trim()
        return s
    }

    fun solve(
        rawInput: String,
        useBanglaDigits: Boolean,
        language: AppLanguage,
        preferredLayout: MathLayoutType? = null
    ): MathSolveResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return MathSolveResult.Error("Empty")
        }

        // Check if input is South-Asian bracket division format: e.g. "2 ) 6 (" or "২ ) ৬ ( ৩"
        val engRaw = DigitHelper.toEnglishDigits(trimmed)
        val bracketDivRegex = Regex("""^\s*(\d+(?:\.\d+)?)\s*\)\s*(\d+(?:\.\d+)?)(?:\s*\(\s*\d*(?:\.\d+)?)?\s*$""")
        val divMatch = bracketDivRegex.find(engRaw)
        if (divMatch != null) {
            val divisorStr = divMatch.groupValues[1]
            val dividendStr = divMatch.groupValues[2]
            val normalized = "$dividendStr/$divisorStr"
            return solveBinaryDivision(
                rawInput = "$dividendStr ÷ $divisorStr",
                normalized = normalized,
                dividendStr = dividendStr,
                divisorStr = divisorStr,
                useBangla = useBanglaDigits,
                language = language
            )
        }

        val normalized = normalizeInput(trimmed).replace("\\s+".toRegex(), "")
        if (normalized.isEmpty()) {
            return MathSolveResult.Error("Invalid")
        }

        // Check simple binary operation for rich vertical Khata format
        val binaryRegex = Regex("""^(-?\d+(?:\.\d+)?)([+\-*/])(\d+(?:\.\d+)?)$""")
        val binMatch = binaryRegex.find(normalized)
        if (binMatch != null) {
            val leftStr = binMatch.groupValues[1]
            val op = binMatch.groupValues[2]
            val rightStr = binMatch.groupValues[3]
            return when (op) {
                "+" -> solveBinaryAddition(trimmed, normalized, leftStr, rightStr, useBanglaDigits, language, preferredLayout)
                "-" -> solveBinarySubtraction(trimmed, normalized, leftStr, rightStr, useBanglaDigits, language, preferredLayout)
                "*" -> solveBinaryMultiplication(trimmed, normalized, leftStr, rightStr, useBanglaDigits, language, preferredLayout)
                "/" -> solveBinaryDivision(trimmed, normalized, leftStr, rightStr, useBanglaDigits, language)
                else -> evaluateCompound(trimmed, normalized, useBanglaDigits, language, preferredLayout)
            }
        }

        return evaluateCompound(trimmed, normalized, useBanglaDigits, language, preferredLayout)
    }

    private fun formatNumber(value: Double): String {
        val rounded = value.roundToLong()
        return if (abs(value - rounded.toDouble()) < 1e-9) {
            rounded.toString()
        } else {
            val formatted = String.format(java.util.Locale.US, "%.4f", value)
                .trimEnd('0')
                .trimEnd('.')
            formatted
        }
    }

    private fun digits(text: String, useBangla: Boolean): String {
        return if (useBangla) DigitHelper.toBanglaDigits(text) else DigitHelper.toEnglishDigits(text)
    }

    private fun solveBinaryAddition(
        rawInput: String,
        normalized: String,
        leftStr: String,
        rightStr: String,
        useBangla: Boolean,
        language: AppLanguage,
        preferredLayout: MathLayoutType?
    ): MathSolveResult {
        val a = leftStr.toDoubleOrNull() ?: return MathSolveResult.Error("Invalid")
        val b = rightStr.toDoubleOrNull() ?: return MathSolveResult.Error("Invalid")
        val sum = a + b
        val ansStr = formatNumber(sum)

        val isIntegers = !leftStr.contains('.') && !rightStr.contains('.') && a >= 0 && b >= 0
        val khataLines = mutableListOf<String>()
        val steps = mutableListOf<String>()

        if (isIntegers) {
            val width = maxOf(leftStr.length, rightStr.length + 2, ansStr.length + 2)
            khataLines += leftStr.padStart(width)
            khataLines += ("+ $rightStr").padStart(width)
            khataLines += "-".repeat(width)
            khataLines += ansStr.padStart(width)

            // Column by column carry steps
            val aRev = leftStr.reversed()
            val bRev = rightStr.reversed()
            val maxLen = maxOf(aRev.length, bRev.length)
            var carry = 0
            for (i in 0 until maxLen) {
                val da = aRev.getOrNull(i)?.digitToIntOrNull() ?: 0
                val db = bRev.getOrNull(i)?.digitToIntOrNull() ?: 0
                val colSum = da + db + carry
                val digit = colSum % 10
                val nextCarry = colSum / 10
                val placeEn = when (i) {
                    0 -> "Ones"
                    1 -> "Tens"
                    2 -> "Hundreds"
                    3 -> "Thousands"
                    else -> "Place ${i + 1}"
                }
                val placeBn = when (i) {
                    0 -> "একক ঘর"
                    1 -> "দশক ঘর"
                    2 -> "শতক ঘর"
                    3 -> "হাজার ঘর"
                    else -> "${i + 1}-তম ঘর"
                }
                val carryTxtBn = if (carry > 0) " + হাতের $carry" else ""
                val carryTxtEn = if (carry > 0) " + carry $carry" else ""
                val nextCarryBn = if (nextCarry > 0) " (বসে $digit, হাতে থাকে $nextCarry)" else " (বসে $digit)"
                val nextCarryEn = if (nextCarry > 0) " (write $digit, carry $nextCarry)" else " (write $digit)"
                steps += if (language == AppLanguage.BANGLA) {
                    "$placeBn: $da + $db$carryTxtBn = $colSum$nextCarryBn"
                } else {
                    "$placeEn: $da + $db$carryTxtEn = $colSum$nextCarryEn"
                }
                carry = nextCarry
            }
            if (carry > 0) {
                steps += if (language == AppLanguage.BANGLA) {
                    "বাকি হাতের $carry বামে বসাই"
                } else {
                    "Bring down remaining carry $carry"
                }
            }
        } else {
            val width = maxOf(leftStr.length, rightStr.length + 2, ansStr.length + 2)
            khataLines += leftStr.padStart(width)
            khataLines += ("+ $rightStr").padStart(width)
            khataLines += "-".repeat(width)
            khataLines += ansStr.padStart(width)
            steps += if (language == AppLanguage.BANGLA) {
                "দশমিক মিলিয়ে উপর-নিচ যোগ করি: $leftStr + $rightStr = $ansStr"
            } else {
                "Align decimal points and add vertically: $leftStr + $rightStr = $ansStr"
            }
        }

        return MathSolveResult.Success(
            originalInput = digits("$leftStr + $rightStr", useBangla),
            normalizedExpression = normalized,
            answerText = digits(ansStr, useBangla),
            khataLines = khataLines.map { digits(it, useBangla) },
            explanationSteps = steps.map { digits(it, useBangla) },
            layoutType = preferredLayout ?: MathLayoutType.VERTICAL
        )
    }

    private fun solveBinarySubtraction(
        rawInput: String,
        normalized: String,
        leftStr: String,
        rightStr: String,
        useBangla: Boolean,
        language: AppLanguage,
        preferredLayout: MathLayoutType?
    ): MathSolveResult {
        val a = leftStr.toDoubleOrNull() ?: return MathSolveResult.Error("Invalid")
        val b = rightStr.toDoubleOrNull() ?: return MathSolveResult.Error("Invalid")
        val diff = a - b
        val ansStr = formatNumber(diff)

        val khataLines = mutableListOf<String>()
        val steps = mutableListOf<String>()
        val isPositiveInt = !leftStr.contains('.') && !rightStr.contains('.') && a >= b && b >= 0

        if (isPositiveInt) {
            val width = maxOf(leftStr.length, rightStr.length + 2, ansStr.length + 2)
            khataLines += leftStr.padStart(width)
            khataLines += ("- $rightStr").padStart(width)
            khataLines += "-".repeat(width)
            khataLines += ansStr.padStart(width)

            val aRev = leftStr.reversed().map { it.digitToInt() }.toMutableList()
            val bRev = rightStr.reversed().map { it.digitToInt() }
            for (i in aRev.indices) {
                val db = bRev.getOrNull(i) ?: 0
                var da = aRev[i]
                val placeBn = when (i) {
                    0 -> "একক ঘর"
                    1 -> "দশক ঘর"
                    2 -> "শতক ঘর"
                    3 -> "হাজার ঘর"
                    else -> "${i + 1}-তম ঘর"
                }
                val placeEn = when (i) {
                    0 -> "Ones"
                    1 -> "Tens"
                    2 -> "Hundreds"
                    3 -> "Thousands"
                    else -> "Place ${i + 1}"
                }
                if (da < db && i + 1 < aRev.size) {
                    aRev[i + 1] -= 1
                    da += 10
                    val res = da - db
                    steps += if (language == AppLanguage.BANGLA) {
                        "$placeBn: বামের ঘর থেকে ১ (১০) ধার নিয়ে $da - $db = $res"
                    } else {
                        "$placeEn: Borrow 10 from next column -> $da - $db = $res"
                    }
                } else {
                    val res = da - db
                    steps += if (language == AppLanguage.BANGLA) {
                        "$placeBn: $da - $db = $res"
                    } else {
                        "$placeEn: $da - $db = $res"
                    }
                }
            }
        } else {
            val width = maxOf(leftStr.length, rightStr.length + 2, ansStr.length + 2)
            khataLines += leftStr.padStart(width)
            khataLines += ("- $rightStr").padStart(width)
            khataLines += "-".repeat(width)
            khataLines += ansStr.padStart(width)
            steps += if (language == AppLanguage.BANGLA) {
                "বিয়োগ করি: $leftStr - $rightStr = $ansStr"
            } else {
                "Subtract vertically: $leftStr - $rightStr = $ansStr"
            }
        }

        return MathSolveResult.Success(
            originalInput = digits("$leftStr - $rightStr", useBangla),
            normalizedExpression = normalized,
            answerText = digits(ansStr, useBangla),
            khataLines = khataLines.map { digits(it, useBangla) },
            explanationSteps = steps.map { digits(it, useBangla) },
            layoutType = preferredLayout ?: MathLayoutType.VERTICAL
        )
    }

    private fun solveBinaryMultiplication(
        rawInput: String,
        normalized: String,
        leftStr: String,
        rightStr: String,
        useBangla: Boolean,
        language: AppLanguage,
        preferredLayout: MathLayoutType?
    ): MathSolveResult {
        val a = leftStr.toDoubleOrNull() ?: return MathSolveResult.Error("Invalid")
        val b = rightStr.toDoubleOrNull() ?: return MathSolveResult.Error("Invalid")
        val prod = a * b
        val ansStr = formatNumber(prod)

        val khataLines = mutableListOf<String>()
        val steps = mutableListOf<String>()
        val isPositiveInt = !leftStr.contains('.') && !rightStr.contains('.') && a >= 0 && b >= 0 && rightStr.length <= 4

        if (isPositiveInt) {
            val aLong = leftStr.toLong()
            val bDigitsRev = rightStr.reversed().map { it.digitToInt() }
            val partials = bDigitsRev.mapIndexed { idx, d ->
                val p = aLong * d
                if (idx == 0) p.toString() else "${p}${"0".repeat(idx)}"
            }
            val width = maxOf(leftStr.length, rightStr.length + 2, ansStr.length + 2, partials.maxOf { it.length } + 2)
            khataLines += leftStr.padStart(width)
            khataLines += ("× $rightStr").padStart(width)
            khataLines += "-".repeat(width)

            if (bDigitsRev.size > 1) {
                partials.forEachIndexed { idx, pStr ->
                    val prefix = if (idx == partials.lastIndex) "+ " else "  "
                    khataLines += ("$prefix$pStr").padStart(width)
                    val multDigit = bDigitsRev[idx]
                    steps += if (language == AppLanguage.BANGLA) {
                        "${idx + 1}ম ধাপ: $leftStr × $multDigit${"0".repeat(idx)} = $pStr"
                    } else {
                        "Step ${idx + 1}: $leftStr × $multDigit${"0".repeat(idx)} = $pStr"
                    }
                }
                khataLines += "-".repeat(width)
                steps += if (language == AppLanguage.BANGLA) {
                    "সবগুলো গুণফল যোগ করে পাই = $ansStr"
                } else {
                    "Add partial products = $ansStr"
                }
            } else {
                steps += if (language == AppLanguage.BANGLA) {
                    "$leftStr কে $rightStr দিয়ে গুণ করে পাই = $ansStr"
                } else {
                    "Multiply $leftStr by $rightStr = $ansStr"
                }
            }
            khataLines += ansStr.padStart(width)
        } else {
            val width = maxOf(leftStr.length, rightStr.length + 2, ansStr.length + 2)
            khataLines += leftStr.padStart(width)
            khataLines += ("× $rightStr").padStart(width)
            khataLines += "-".repeat(width)
            khataLines += ansStr.padStart(width)
            steps += if (language == AppLanguage.BANGLA) {
                "গুণ করি: $leftStr × $rightStr = $ansStr"
            } else {
                "Multiply: $leftStr × $rightStr = $ansStr"
            }
        }

        return MathSolveResult.Success(
            originalInput = digits("$leftStr × $rightStr", useBangla),
            normalizedExpression = normalized,
            answerText = digits(ansStr, useBangla),
            khataLines = khataLines.map { digits(it, useBangla) },
            explanationSteps = steps.map { digits(it, useBangla) },
            layoutType = preferredLayout ?: MathLayoutType.VERTICAL
        )
    }

    private fun solveBinaryDivision(
        rawInput: String,
        normalized: String,
        dividendStr: String,
        divisorStr: String,
        useBangla: Boolean,
        language: AppLanguage
    ): MathSolveResult {
        val dividend = dividendStr.toDoubleOrNull() ?: return MathSolveResult.Error("Invalid")
        val divisor = divisorStr.toDoubleOrNull() ?: return MathSolveResult.Error("Invalid")
        if (abs(divisor) < 1e-12) {
            return MathSolveResult.Error("Division by zero", isDivideByZero = true)
        }

        val quotientDouble = dividend / divisor
        val ansStr = formatNumber(quotientDouble)

        val khataLines = mutableListOf<String>()
        val steps = mutableListOf<String>()

        val isPosInt = !dividendStr.contains('.') && !divisorStr.contains('.') && dividend >= 0 && divisor > 0
        if (isPosInt) {
            val div = divisorStr.toLong()
            val dividendDigits = dividendStr.map { it.digitToInt() }
            val intQuotient = dividend.toLong() / div
            val intRemainder = dividend.toLong() % div

            val header = "$divisorStr ) $dividendStr ( $intQuotient"
            val indentBase = "$divisorStr ) ".length
            khataLines += header

            var current = 0L
            var stepCount = 0
            for (i in dividendDigits.indices) {
                current = current * 10 + dividendDigits[i]
                if (current >= div || stepCount > 0 || i == dividendDigits.lastIndex) {
                    val qDigit = current / div
                    val product = qDigit * div
                    val rem = current - product
                    if (qDigit > 0 || stepCount > 0 || i == dividendDigits.lastIndex) {
                        stepCount++
                        val endCol = indentBase + i + 1
                        val curStr = current.toString()
                        val prodStr = product.toString()
                        if (stepCount > 1) {
                            khataLines += curStr.padStart(endCol)
                        }
                        khataLines += prodStr.padStart(endCol)
                        val barLen = maxOf(curStr.length, prodStr.length) + 1
                        khataLines += ("-".repeat(barLen)).padStart(endCol)

                        steps += if (language == AppLanguage.BANGLA) {
                            "${stepCount}ম ধাপ: $current এর মধ্যে $divisor যায় $qDigit বার ($divisor × $qDigit = $product), বিয়োগফল = $rem"
                        } else {
                            "Step $stepCount: $divisor goes into $current -> $qDigit times ($divisor × $qDigit = $product), remainder = $rem"
                        }
                        current = rem
                    }
                }
            }
            val finalMarker = if (intRemainder == 0L) "×" else intRemainder.toString()
            val endCol = indentBase + dividendDigits.size
            khataLines += finalMarker.padStart(endCol)

            if (intRemainder == 0L) {
                steps += if (language == AppLanguage.BANGLA) {
                    "ভাগশেষ = ০ (নিঃশেষে বিভাজ্য), ভাগফল = $intQuotient"
                } else {
                    "Remainder = 0 (exact division), Quotient = $intQuotient"
                }
            } else {
                steps += if (language == AppLanguage.BANGLA) {
                    "ভাগফল = $intQuotient, ভাগশেষ = $intRemainder (দশমিকে উত্তর: $ansStr)"
                } else {
                    "Quotient = $intQuotient, Remainder = $intRemainder (Decimal: $ansStr)"
                }
            }
        } else {
            khataLines += "$divisorStr ) $dividendStr ( $ansStr"
            steps += if (language == AppLanguage.BANGLA) {
                "$dividendStr কে $divisorStr দিয়ে ভাগ করে পাই = $ansStr"
            } else {
                "Divide $dividendStr by $divisorStr = $ansStr"
            }
        }

        return MathSolveResult.Success(
            originalInput = digits("$dividendStr ÷ $divisorStr", useBangla),
            normalizedExpression = normalized,
            answerText = digits(ansStr, useBangla),
            khataLines = khataLines.map { digits(it, useBangla) },
            explanationSteps = steps.map { digits(it, useBangla) },
            layoutType = MathLayoutType.LONG_DIVISION
        )
    }

    private fun evaluateCompound(
        rawInput: String,
        normalized: String,
        useBangla: Boolean,
        language: AppLanguage,
        preferredLayout: MathLayoutType?
    ): MathSolveResult {
        return try {
            val steps = mutableListOf<String>()
            val parser = RecursiveParser(normalized, steps, language)
            val result = parser.parseExpression()
            if (result.isNaN() || result.isInfinite()) {
                return MathSolveResult.Error("Division by zero", isDivideByZero = true)
            }
            val ansStr = formatNumber(result)
            val prettyInput = rawInput
                .replace('*', '×')
                .replace('/', '÷')

            val khataLines = mutableListOf<String>()
            khataLines += "= $prettyInput"
            steps.forEach { s ->
                khataLines += "= $s"
            }
            khataLines += "= $ansStr"

            val explain = if (steps.isEmpty()) {
                listOf(
                    if (language == AppLanguage.BANGLA) "প্রদত্ত রাশি: $prettyInput = $ansStr"
                    else "Expression: $prettyInput = $ansStr"
                )
            } else {
                steps.mapIndexed { index, s ->
                    if (language == AppLanguage.BANGLA) "ধাপ ${index + 1}: $s"
                    else "Step ${index + 1}: $s"
                } + (if (language == AppLanguage.BANGLA) "চূড়ান্ত উত্তর = $ansStr" else "Final Answer = $ansStr")
            }

            MathSolveResult.Success(
                originalInput = digits(prettyInput, useBangla),
                normalizedExpression = normalized,
                answerText = digits(ansStr, useBangla),
                khataLines = khataLines.distinct().map { digits(it, useBangla) },
                explanationSteps = explain.map { digits(it, useBangla) },
                layoutType = preferredLayout ?: MathLayoutType.CALCULATOR
            )
        } catch (e: ArithmeticException) {
            MathSolveResult.Error("Division by zero", isDivideByZero = true)
        } catch (e: Exception) {
            MathSolveResult.Error("Invalid expression")
        }
    }

    private class RecursiveParser(
        private val str: String,
        private val steps: MutableList<String>,
        private val language: AppLanguage
    ) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parseExpression(): Double {
            nextChar()
            val x = parseAddSub()
            if (pos < str.length) throw IllegalArgumentException("Unexpected char")
            return x
        }

        private fun parseAddSub(): Double {
            var x = parseMulDiv()
            while (true) {
                if (eat('+'.code)) {
                    val y = parseMulDiv()
                    val res = x + y
                    steps += "${formatVal(x)} + ${formatVal(y)} = ${formatVal(res)}"
                    x = res
                } else if (eat('-'.code)) {
                    val y = parseMulDiv()
                    val res = x - y
                    steps += "${formatVal(x)} - ${formatVal(y)} = ${formatVal(res)}"
                    x = res
                } else {
                    return x
                }
            }
        }

        private fun parseMulDiv(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*'.code)) {
                    val y = parseFactor()
                    val res = x * y
                    steps += "${formatVal(x)} × ${formatVal(y)} = ${formatVal(res)}"
                    x = res
                } else if (eat('/'.code)) {
                    val y = parseFactor()
                    if (abs(y) < 1e-12) throw ArithmeticException("Division by zero")
                    val res = x / y
                    steps += "${formatVal(x)} ÷ ${formatVal(y)} = ${formatVal(res)}"
                    x = res
                } else {
                    return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            val x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseAddSub()
                if (!eat(')'.code)) throw IllegalArgumentException("Missing ')'")
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else {
                throw IllegalArgumentException("Unexpected char")
            }
            return x
        }

        private fun formatVal(v: Double): String {
            val r = floor(v)
            return if (abs(v - r) < 1e-9) v.toLong().toString()
            else String.format(java.util.Locale.US, "%.3f", v).trimEnd('0').trimEnd('.')
        }
    }
}
