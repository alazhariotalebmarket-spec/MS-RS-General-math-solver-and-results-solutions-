package com.example.domain

import com.example.core.AppLanguage
import com.example.core.DigitHelper
import com.example.data.MathLayoutType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cbrt
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sqrt

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
            .replace('÷', '/')
            .replace('−', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace('[', '(')
            .replace(']', ')')
            .replace('{', '(')
            .replace('}', ')')
            .replace("²", "^2")
            .replace("³", "^3")
        // Convert 'x' / 'X' to '*' only when between digits/closing-paren and digits/opening-paren
        s = s.replace(Regex("""(\d|\))\s*[xX]\s*(\d|\(|√)"""), "$1*$2")
        // Strip trailing '=' if used like "2+2="
        if (s.endsWith("=")) {
            s = s.dropLast(1)
        }
        return s.trim()
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

        val engRaw = DigitHelper.toEnglishDigits(trimmed)

        // 1. South-Asian bracket division: e.g. "2 ) 6 (" or "২ ) ৬ ( ৩"
        val bracketDivRegex = Regex("""^\s*(\d+(?:\.\d+)?)\s*\)\s*(\d+(?:\.\d+)?)(?:\s*\(\s*\d*(?:\.\d+)?)?\s*$""")
        val divMatch = bracketDivRegex.find(engRaw)
        if (divMatch != null) {
            val divisorStr = divMatch.groupValues[1]
            val dividendStr = divMatch.groupValues[2]
            return solveBinaryDivision(
                rawInput = "$dividendStr ÷ $divisorStr",
                normalized = "$dividendStr/$divisorStr",
                dividendStr = dividendStr,
                divisorStr = divisorStr,
                useBangla = useBanglaDigits,
                language = language
            )
        }

        // 2. LCM / GCD / Average
        trySolveAggregateCommand(engRaw, trimmed, useBanglaDigits, language)?.let { return it }

        // 2.1 Curriculum specials (Prime factors, Interest, Area, Median)
        trySolveCurriculumCommands(engRaw, trimmed, useBanglaDigits, language)?.let { return it }

        // 3. Percentage phrase: "200 এর 15%" or "15% of 200"
        trySolvePercentPhrase(engRaw, trimmed, useBanglaDigits, language)?.let { return it }

        // 4. Algebraic Equation (contains '=' with two sides and a variable)
        val cleanEqCandidate = engRaw
            .replace('×', '*')
            .replace('÷', '/')
            .replace('−', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace('[', '(')
            .replace(']', ')')
            .replace('{', '(')
            .replace('}', ')')
            .replace("²", "^2")
            .replace("³", "^3")
        val eqParts = cleanEqCandidate.split('=')
        if (eqParts.size == 2 && eqParts[0].isNotBlank() && eqParts[1].isNotBlank()) {
            trySolveEquation(
                rawInput = trimmed,
                leftRaw = eqParts[0].trim(),
                rightRaw = eqParts[1].trim(),
                useBangla = useBanglaDigits,
                language = language
            )?.let { return it }
        }

        val normalized = normalizeInput(trimmed).replace("\\s+".toRegex(), "")
        if (normalized.isEmpty()) {
            return MathSolveResult.Error("Invalid")
        }

        // 5. Smart Percentage Add/Subtract: e.g. "500 + 10%" or "500 - 20%"
        val pctAddSubRegex = Regex("""^(-?\d+(?:\.\d+)?)([+\-])(\d+(?:\.\d+)?)%$""")
        pctAddSubRegex.find(normalized)?.let { m ->
            val base = m.groupValues[1].toDoubleOrNull()
            val op = m.groupValues[2]
            val pctVal = m.groupValues[3].toDoubleOrNull()
            if (base != null && pctVal != null) {
                val delta = base * (pctVal / 100.0)
                val ans = if (op == "+") base + delta else base - delta
                val ansStr = formatNumber(ans)
                val deltaStr = formatNumber(delta)
                val baseStr = formatNumber(base)
                val pctStr = formatNumber(pctVal)
                val khata = listOf(
                    "= $baseStr $op ($baseStr × $pctStr ÷ 100)",
                    "= $baseStr $op $deltaStr",
                    "= $ansStr"
                )
                val steps = if (language == AppLanguage.BANGLA) {
                    listOf(
                        "ধাপ ১: $baseStr এর $pctStr% = ($baseStr × $pctStr) ÷ 100 = $deltaStr",
                        "ধাপ ২: $baseStr $op $deltaStr = $ansStr",
                        "চূড়ান্ত উত্তর = $ansStr"
                    )
                } else {
                    listOf(
                        "Step 1: $pctStr% of $baseStr = ($baseStr × $pctStr) ÷ 100 = $deltaStr",
                        "Step 2: $baseStr $op $deltaStr = $ansStr",
                        "Final Answer = $ansStr"
                    )
                }
                return MathSolveResult.Success(
                    originalInput = digits(trimmed, useBanglaDigits),
                    normalizedExpression = normalized,
                    answerText = digits(ansStr, useBanglaDigits),
                    khataLines = khata.map { digits(it, useBanglaDigits) },
                    explanationSteps = steps.map { digits(it, useBanglaDigits) },
                    layoutType = preferredLayout ?: MathLayoutType.CALCULATOR
                )
            }
        }

        // 6. Simple binary operation for rich vertical Khata format
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

        // 7. Pure fraction expression: e.g. "1/2 + 3/4" or "5/6 - 1/3" or "2/3 × 3/4"
        trySolvePureFractions(engRaw, trimmed, useBanglaDigits, language)?.let { return it }

        // 8. Compound BODMAS + Scientific Expression (powers, roots, %, !, implicit mul)
        return evaluateCompound(trimmed, normalized, useBanglaDigits, language, preferredLayout)
    }

    private fun trySolveAggregateCommand(
        engRaw: String,
        rawInput: String,
        useBangla: Boolean,
        language: AppLanguage
    ): MathSolveResult? {
        val lower = engRaw.trim().lowercase()
        val mode = when {
            lower.startsWith("lcm") || lower.startsWith("লসাগু") || lower.startsWith("ল.সা.গু") -> "LCM"
            lower.startsWith("gcd") || lower.startsWith("hcf") || lower.startsWith("গসাগু") || lower.startsWith("গ.সা.গু") -> "GCD"
            lower.startsWith("avg") || lower.startsWith("average") || lower.startsWith("গড়") -> "AVG"
            else -> return null
        }

        val numMatches = Regex("""-?\d+(?:\.\d+)?""").findAll(engRaw).mapNotNull { it.value.toDoubleOrNull() }.toList()
        if (numMatches.size < 2) {
            return MathSolveResult.Error("Need at least 2 numbers")
        }

        return when (mode) {
            "AVG" -> {
                val sum = numMatches.sum()
                val avg = sum / numMatches.size.toDouble()
                val sumStr = formatNumber(sum)
                val avgStr = formatNumber(avg)
                val numsJoined = numMatches.joinToString(" + ") { formatNumber(it) }
                val khata = listOf(
                    "= ($numsJoined) ÷ ${numMatches.size}",
                    "= $sumStr ÷ ${numMatches.size}",
                    "= $avgStr"
                )
                val steps = if (language == AppLanguage.BANGLA) {
                    listOf(
                        "ধাপ ১: সংখ্যাগুলোর যোগফল = $numsJoined = $sumStr",
                        "ধাপ ২: মোট সংখ্যা = ${numMatches.size}টি",
                        "ধাপ ৩: গড় = $sumStr ÷ ${numMatches.size} = $avgStr"
                    )
                } else {
                    listOf(
                        "Step 1: Sum of numbers = $numsJoined = $sumStr",
                        "Step 2: Count = ${numMatches.size}",
                        "Step 3: Average = $sumStr ÷ ${numMatches.size} = $avgStr"
                    )
                }
                MathSolveResult.Success(
                    originalInput = digits(rawInput, useBangla),
                    normalizedExpression = "AVG(${numMatches.joinToString(",") { formatNumber(it) }})",
                    answerText = digits(avgStr, useBangla),
                    khataLines = khata.map { digits(it, useBangla) },
                    explanationSteps = steps.map { digits(it, useBangla) },
                    layoutType = MathLayoutType.CALCULATOR
                )
            }
            "LCM" -> {
                val ints = numMatches.map { abs(it.roundToLong()) }.filter { it > 0L }
                if (ints.size < 2) return MathSolveResult.Error("Invalid")
                val working = ints.toMutableList()
                val factors = mutableListOf<Long>()
                val khata = mutableListOf<String>()
                var p = 2L
                while (working.any { it > 1L }) {
                    val divisibleCount = working.count { it % p == 0L }
                    if (divisibleCount >= 2 || (divisibleCount == 1 && working.count { it > 1L } == 1)) {
                        khata += "$p | ${working.joinToString(", ")}"
                        for (i in working.indices) {
                            if (working[i] % p == 0L) working[i] = working[i] / p
                        }
                        factors += p
                    } else {
                        p++
                        if (p > 10000L) break
                    }
                }
                // Include any remaining coprime numbers > 1
                val remaining = working.filter { it > 1L }
                khata += "    ${working.joinToString(", ")}"
                val allFactors = factors + remaining
                val lcm = allFactors.fold(1L) { acc, v -> acc * v }
                khata += "ল.সা.গু = ${allFactors.joinToString(" × ")} = $lcm".let {
                    if (language == AppLanguage.BANGLA) it else "LCM = ${allFactors.joinToString(" × ")} = $lcm"
                }
                val steps = if (language == AppLanguage.BANGLA) {
                    listOf(
                        "সংখ্যাগুলো: ${ints.joinToString(", ")}",
                        "মৌলিক উৎপাদক ও অবশিষ্ট: ${allFactors.joinToString(" × ")}",
                        "নির্ণেয় ল.সা.গু = $lcm"
                    )
                } else {
                    listOf(
                        "Numbers: ${ints.joinToString(", ")}",
                        "Prime factors & remainders: ${allFactors.joinToString(" × ")}",
                        "LCM = $lcm"
                    )
                }
                MathSolveResult.Success(
                    originalInput = digits(rawInput, useBangla),
                    normalizedExpression = "LCM(${ints.joinToString(",")})",
                    answerText = digits(lcm.toString(), useBangla),
                    khataLines = khata.map { digits(it, useBangla) },
                    explanationSteps = steps.map { digits(it, useBangla) },
                    layoutType = MathLayoutType.CALCULATOR
                )
            }
            else -> { // GCD
                val ints = numMatches.map { abs(it.roundToLong()) }.filter { it > 0L }
                if (ints.size < 2) return MathSolveResult.Error("Invalid")
                val working = ints.toMutableList()
                val commonFactors = mutableListOf<Long>()
                val khata = mutableListOf<String>()
                var p = 2L
                val minVal = ints.min()
                while (p <= minVal && working.all { it > 1L }) {
                    if (working.all { it % p == 0L }) {
                        khata += "$p | ${working.joinToString(", ")}"
                        for (i in working.indices) {
                            working[i] = working[i] / p
                        }
                        commonFactors += p
                    } else {
                        p++
                    }
                }
                val gcd = if (commonFactors.isEmpty()) 1L else commonFactors.fold(1L) { acc, v -> acc * v }
                if (commonFactors.isNotEmpty()) {
                    khata += "    ${working.joinToString(", ")}"
                    khata += if (language == AppLanguage.BANGLA) {
                        "গ.সা.গু = ${commonFactors.joinToString(" × ")} = $gcd"
                    } else {
                        "GCD = ${commonFactors.joinToString(" × ")} = $gcd"
                    }
                } else {
                    khata += if (language == AppLanguage.BANGLA) {
                        "কোনো সাধারণ উৎপাদক নেই, গ.সা.গু = ১"
                    } else {
                        "No common factor > 1, GCD = 1"
                    }
                }
                val steps = if (language == AppLanguage.BANGLA) {
                    listOf(
                        "সংখ্যাগুলো: ${ints.joinToString(", ")}",
                        if (commonFactors.isEmpty()) "সংখ্যাগুলো পরস্পর সহমৌলিক" else "সাধারণ উৎপাদক: ${commonFactors.joinToString(" × ")}",
                        "নির্ণেয় গ.সা.গু = $gcd"
                    )
                } else {
                    listOf(
                        "Numbers: ${ints.joinToString(", ")}",
                        if (commonFactors.isEmpty()) "Numbers are coprime" else "Common factors: ${commonFactors.joinToString(" × ")}",
                        "GCD = $gcd"
                    )
                }
                MathSolveResult.Success(
                    originalInput = digits(rawInput, useBangla),
                    normalizedExpression = "GCD(${ints.joinToString(",")})",
                    answerText = digits(gcd.toString(), useBangla),
                    khataLines = khata.map { digits(it, useBangla) },
                    explanationSteps = steps.map { digits(it, useBangla) },
                    layoutType = MathLayoutType.CALCULATOR
                )
            }
        }
    }

    private fun trySolveCurriculumCommands(
        engRaw: String,
        rawInput: String,
        useBangla: Boolean,
        language: AppLanguage
    ): MathSolveResult? {
        val lower = engRaw.trim().lowercase()

        // 1. Prime Factorization: "prime 60" or "মৌলিক উৎপাদক 60"
        if (lower.startsWith("prime") || lower.startsWith("মৌলিক") || lower.startsWith("মৌলিক উৎপাদক")) {
            val num = Regex("""\d+""").find(engRaw)?.value?.toLongOrNull() ?: return null
            if (num < 2) return MathSolveResult.Error("Invalid number")
            var temp = num
            val factors = mutableListOf<Long>()
            val khata = mutableListOf<String>()
            var d = 2L
            while (d * d <= temp) {
                while (temp % d == 0L) {
                    khata += "$d | $temp"
                    factors += d
                    temp /= d
                }
                d++
            }
            if (temp > 1L) {
                factors += temp
            }
            khata += "    1"
            val facStr = factors.joinToString(" × ")
            khata += if (language == AppLanguage.BANGLA) "মৌলিক উৎপাদক = $facStr" else "Prime factors = $facStr"
            val steps = if (language == AppLanguage.BANGLA) {
                listOf("সংখ্যা: $num", "ধাপ ১: মৌলিক সংখ্যা দ্বারা ভাগ করে উৎপাদক নির্ণয়", "নির্ণেয় মৌলিক উৎপাদক: $facStr")
            } else {
                listOf("Number: $num", "Step 1: Divide by prime factors successively", "Prime factors: $facStr")
            }
            return MathSolveResult.Success(
                originalInput = digits(rawInput, useBangla),
                normalizedExpression = "Prime($num)",
                answerText = digits(facStr, useBangla),
                khataLines = khata.map { digits(it, useBangla) },
                explanationSteps = steps.map { digits(it, useBangla) },
                layoutType = MathLayoutType.CALCULATOR
            )
        }

        // 2. Simple Interest (সরল মুনাফা): "interest p=5000 n=3 r=5" or "মুনাফা P=5000 n=3 r=5"
        if (lower.startsWith("interest") || lower.startsWith("মুনাফা") || lower.startsWith("সরল মুনাফা")) {
            val nums = Regex("""-?\d+(?:\.\d+)?""").findAll(engRaw).mapNotNull { it.value.toDoubleOrNull() }.toList()
            if (nums.size >= 3) {
                val p = nums[0]
                val n = nums[1]
                val r = nums[2]
                val iVal = (p * n * r) / 100.0
                val total = p + iVal
                val pStr = formatNumber(p)
                val nStr = formatNumber(n)
                val rStr = formatNumber(r)
                val iStr = formatNumber(iVal)
                val totStr = formatNumber(total)
                val khata = listOf(
                    "I = (P × n × r) ÷ 100",
                    "I = ($pStr × $nStr × $rStr) ÷ 100",
                    "I = ${formatNumber(p * n * r)} ÷ 100 = $iStr",
                    "Total = P + I = $pStr + $iStr = $totStr"
                )
                val steps = if (language == AppLanguage.BANGLA) {
                    listOf(
                        "মূলধন (P) = $pStr টাকা, সময় (n) = $nStr বছর, মুনাফার হার (r) = $rStr%",
                        "সূত্র: সরল মুনাফা I = (Pnr)/100 = ($pStr × $nStr × $rStr)/100 = $iStr টাকা",
                        "মোট পরিমাণ (A) = P + I = $totStr টাকা"
                    )
                } else {
                    listOf(
                        "Principal P = $pStr, Time n = $nStr, Rate r = $rStr%",
                        "Formula: Interest I = (Pnr)/100 = $iStr",
                        "Total Amount A = P + I = $totStr"
                    )
                }
                return MathSolveResult.Success(
                    originalInput = digits(rawInput, useBangla),
                    normalizedExpression = "Interest(P=$pStr,n=$nStr,r=$rStr)",
                    answerText = digits("$iStr (Total: $totStr)", useBangla),
                    khataLines = khata.map { digits(it, useBangla) },
                    explanationSteps = steps.map { digits(it, useBangla) },
                    layoutType = MathLayoutType.CALCULATOR
                )
            }
        }

        // 3. Median (মধ্যক): "median 12, 15, 10, 18, 20" or "মধ্যক 12,15,10,18"
        if (lower.startsWith("median") || lower.startsWith("মধ্যক")) {
            val nums = Regex("""-?\d+(?:\.\d+)?""").findAll(engRaw).mapNotNull { it.value.toDoubleOrNull() }.sorted().toList()
            if (nums.isNotEmpty()) {
                val size = nums.size
                val median = if (size % 2 == 1) {
                    nums[size / 2]
                } else {
                    (nums[size / 2 - 1] + nums[size / 2]) / 2.0
                }
                val medStr = formatNumber(median)
                val sortedJoined = nums.joinToString(", ") { formatNumber(it) }
                val khata = listOf(
                    "সাজানো উপাত্ত: $sortedJoined",
                    "পদসংখ্যা n = $size",
                    "মধ্যক = $medStr"
                )
                val steps = if (language == AppLanguage.BANGLA) {
                    listOf("ধাপ ১: উপাত্তগুলো ছোট থেকে বড় সাজাই", "ধাপ ২: মধ্যক নির্ণয় = $medStr")
                } else {
                    listOf("Step 1: Sort data", "Step 2: Median = $medStr")
                }
                return MathSolveResult.Success(
                    originalInput = digits(rawInput, useBangla),
                    normalizedExpression = "Median(${nums.size})",
                    answerText = digits(medStr, useBangla),
                    khataLines = khata.map { digits(it, useBangla) },
                    explanationSteps = steps.map { digits(it, useBangla) },
                    layoutType = MathLayoutType.CALCULATOR
                )
            }
        }

        // 4. Area & Perimeter (ক্ষেত্রফল ও পরিসীমা): "area 12, 8" or "ক্ষেত্রফল ১২, ৮"
        if (lower.startsWith("area") || lower.startsWith("ক্ষেত্রফল")) {
            val nums = Regex("""-?\d+(?:\.\d+)?""").findAll(engRaw).mapNotNull { it.value.toDoubleOrNull() }.toList()
            if (nums.size >= 2) {
                val l = nums[0]
                val w = nums[1]
                val area = l * w
                val perim = 2 * (l + w)
                val aStr = formatNumber(area)
                val pStr = formatNumber(perim)
                val khata = listOf(
                    "দৈর্ঘ্য (l) = ${formatNumber(l)}, প্রস্থ (w) = ${formatNumber(w)}",
                    "ক্ষেত্রফল = l × w = $aStr বর্গ একক",
                    "পরিসীমা = 2(l + w) = $pStr একক"
                )
                val steps = if (language == AppLanguage.BANGLA) {
                    listOf("আয়তক্ষেত্রের ক্ষেত্রফল = দৈর্ঘ্য × প্রস্থ = $aStr", "পরিসীমা = 2(দৈর্ঘ্য + প্রস্থ) = $pStr")
                } else {
                    listOf("Rectangle Area = l × w = $aStr", "Perimeter = 2(l + w) = $pStr")
                }
                return MathSolveResult.Success(
                    originalInput = digits(rawInput, useBangla),
                    normalizedExpression = "Area($l,$w)",
                    answerText = digits("Area: $aStr, Perimeter: $pStr", useBangla),
                    khataLines = khata.map { digits(it, useBangla) },
                    explanationSteps = steps.map { digits(it, useBangla) },
                    layoutType = MathLayoutType.CALCULATOR
                )
            } else if (nums.size == 1) {
                val s = nums[0]
                val area = s * s
                val perim = 4 * s
                val aStr = formatNumber(area)
                val pStr = formatNumber(perim)
                val khata = listOf(
                    "বাহু (a) = ${formatNumber(s)}",
                    "বর্গক্ষেত্র ক্ষেত্রফল = a² = $aStr",
                    "পরিসীমা = 4a = $pStr"
                )
                val steps = listOf("Square Area = $aStr", "Perimeter = $pStr")
                return MathSolveResult.Success(
                    originalInput = digits(rawInput, useBangla),
                    normalizedExpression = "Area($s)",
                    answerText = digits("Area: $aStr, Perimeter: $pStr", useBangla),
                    khataLines = khata.map { digits(it, useBangla) },
                    explanationSteps = steps.map { digits(it, useBangla) },
                    layoutType = MathLayoutType.CALCULATOR
                )
            }
        }

        return null
    }

    private fun trySolvePercentPhrase(
        engRaw: String,
        rawInput: String,
        useBangla: Boolean,
        language: AppLanguage
    ): MathSolveResult? {
        // Bangla: "200 এর 15%"
        val bnRegex = Regex("""^\s*(-?\d+(?:\.\d+)?)\s*এর\s*(-?\d+(?:\.\d+)?)\s*%\s*$""")
        // English: "15% of 200"
        val enRegex = Regex("""^\s*(-?\d+(?:\.\d+)?)\s*%\s*of\s*(-?\d+(?:\.\d+)?)\s*$""", RegexOption.IGNORE_CASE)

        val (base, pct) = bnRegex.find(engRaw)?.let {
            (it.groupValues[1].toDoubleOrNull() ?: return null) to (it.groupValues[2].toDoubleOrNull() ?: return null)
        } ?: enRegex.find(engRaw)?.let {
            (it.groupValues[2].toDoubleOrNull() ?: return null) to (it.groupValues[1].toDoubleOrNull() ?: return null)
        } ?: return null

        val ans = base * (pct / 100.0)
        val baseStr = formatNumber(base)
        val pctStr = formatNumber(pct)
        val ansStr = formatNumber(ans)
        val khata = listOf(
            "= $baseStr × ($pctStr ÷ 100)",
            "= ${formatNumber(base * pct)} ÷ 100",
            "= $ansStr"
        )
        val steps = if (language == AppLanguage.BANGLA) {
            listOf(
                "ধাপ ১: $pctStr% = $pctStr ÷ 100",
                "ধাপ ২: $baseStr × $pctStr ÷ 100 = $ansStr",
                "চূড়ান্ত উত্তর = $ansStr"
            )
        } else {
            listOf(
                "Step 1: $pctStr% = $pctStr ÷ 100",
                "Step 2: $baseStr × $pctStr ÷ 100 = $ansStr",
                "Final Answer = $ansStr"
            )
        }
        return MathSolveResult.Success(
            originalInput = digits(rawInput, useBangla),
            normalizedExpression = "$baseStr*$pctStr%",
            answerText = digits(ansStr, useBangla),
            khataLines = khata.map { digits(it, useBangla) },
            explanationSteps = steps.map { digits(it, useBangla) },
            layoutType = MathLayoutType.CALCULATOR
        )
    }

    private fun trySolvePureFractions(
        engRaw: String,
        rawInput: String,
        useBangla: Boolean,
        language: AppLanguage
    ): MathSolveResult? {
        val cleaned = engRaw.replace('×', '*')
            .replace('÷', '/')
            .replace('−', '-')
            .replace('–', '-')
            .replace("\\s+".toRegex(), "")
        // Match fraction expression like "1/2+3/4" or "5/6-1/3+1/2"
        val fracToken = """-?\d+/\d+"""
        val fullFracRegex = Regex("""^$fracToken([+\-*/]$fracToken)+$""")
        // Wait: distinguish "1/2*3/4" (multiplication of two fractions) vs "1/2/3" (ambiguous).
        // Support binary or chained +/- fractions and binary * or / between two fractions
        val binaryOpFrac = Regex("""^(-?\d+)/(\d+)([+\-*/])(-?\d+)/(\d+)$""")
        val binMatch = binaryOpFrac.find(cleaned)
        if (binMatch != null) {
            val n1 = binMatch.groupValues[1].toLongOrNull() ?: return null
            val d1 = binMatch.groupValues[2].toLongOrNull() ?: return null
            val op = binMatch.groupValues[3]
            val n2 = binMatch.groupValues[4].toLongOrNull() ?: return null
            val d2 = binMatch.groupValues[5].toLongOrNull() ?: return null
            if (d1 == 0L || d2 == 0L) return MathSolveResult.Error("Division by zero", isDivideByZero = true)

            val (resNumRaw, resDenRaw, khata, steps) = when (op) {
                "+", "-" -> {
                    val lcm = lcmLong(d1, d2)
                    val m1 = n1 * (lcm / d1)
                    val m2 = n2 * (lcm / d2)
                    val rNum = if (op == "+") m1 + m2 else m1 - m2
                    val k = mutableListOf(
                        "= $n1/$d1 $op $n2/$d2",
                        "= ($m1 $op $m2) / $lcm",
                        "= $rNum / $lcm"
                    )
                    val s = if (language == AppLanguage.BANGLA) {
                        mutableListOf(
                            "ধাপ ১: হর $d1 ও $d2 এর ল.সা.গু = $lcm",
                            "ধাপ ২: লব রূপান্তর: ($m1 $op $m2) / $lcm = $rNum/$lcm"
                        )
                    } else {
                        mutableListOf(
                            "Step 1: LCM of denominators $d1 and $d2 = $lcm",
                            "Step 2: Convert & combine: ($m1 $op $m2) / $lcm = $rNum/$lcm"
                        )
                    }
                    Quad(rNum, lcm, k, s)
                }
                "*" -> {
                    val rNum = n1 * n2
                    val rDen = d1 * d2
                    val k = mutableListOf(
                        "= ($n1 × $n2) / ($d1 × $d2)",
                        "= $rNum / $rDen"
                    )
                    val s = if (language == AppLanguage.BANGLA) {
                        mutableListOf("ধাপ ১: লব×লব ও হর×হর গুণ করে = $rNum/$rDen")
                    } else {
                        mutableListOf("Step 1: Multiply numerators and denominators = $rNum/$rDen")
                    }
                    Quad(rNum, rDen, k, s)
                }
                else -> { // "/"
                    if (n2 == 0L) return MathSolveResult.Error("Division by zero", isDivideByZero = true)
                    val rNum = n1 * d2
                    val rDen = d1 * n2
                    val k = mutableListOf(
                        "= $n1/$d1 × $d2/$n2",
                        "= $rNum / $rDen"
                    )
                    val s = if (language == AppLanguage.BANGLA) {
                        mutableListOf(
                            "ধাপ ১: ভগ্নাংশের ভাগে দ্বিতীয় ভগ্নাংশ উল্টে গুণ করি: $n1/$d1 × $d2/$n2",
                            "ধাপ ২: গুণফল = $rNum/$rDen"
                        )
                    } else {
                        mutableListOf(
                            "Step 1: Multiply by reciprocal: $n1/$d1 × $d2/$n2",
                            "Step 2: Result = $rNum/$rDen"
                        )
                    }
                    Quad(rNum, rDen, k, s)
                }
            }

            val sign = if (resNumRaw * resDenRaw < 0) -1L else 1L
            val aNum = abs(resNumRaw)
            val aDen = abs(resDenRaw)
            val g = gcdLong(aNum, aDen)
            val simNum = sign * (aNum / g)
            val simDen = aDen / g
            val decimalVal = formatNumber(simNum.toDouble() / simDen.toDouble())
            val fracStr = if (simDen == 1L) "$simNum" else "$simNum/$simDen"
            val ansText = if (simDen == 1L) fracStr else "$fracStr ($decimalVal)"
            if (g > 1L || simDen == 1L) {
                khata += "= $fracStr"
                steps += if (language == AppLanguage.BANGLA) {
                    "ধাপ ${steps.size + 1}: লঘিষ্ঠ আকার = $ansText"
                } else {
                    "Step ${steps.size + 1}: Simplified = $ansText"
                }
            } else {
                steps += if (language == AppLanguage.BANGLA) {
                    "দশমিক মান = $decimalVal"
                } else {
                    "Decimal value = $decimalVal"
                }
            }
            return MathSolveResult.Success(
                originalInput = digits(rawInput, useBangla),
                normalizedExpression = cleaned,
                answerText = digits(ansText, useBangla),
                khataLines = khata.map { digits(it, useBangla) },
                explanationSteps = steps.map { digits(it, useBangla) },
                layoutType = MathLayoutType.CALCULATOR
            )
        }
        return null
    }

    private data class Quad(
        val num: Long,
        val den: Long,
        val khata: MutableList<String>,
        val steps: MutableList<String>
    )

    private data class Poly2(val a: Double, val b: Double, val c: Double) {
        operator fun plus(o: Poly2) = Poly2(a + o.a, b + o.b, c + o.c)
        operator fun minus(o: Poly2) = Poly2(a - o.a, b - o.b, c - o.c)
        operator fun unaryMinus() = Poly2(-a, -b, -c)
        fun mul(o: Poly2): Poly2 {
            // Check degree doesn't exceed 2
            val x4 = a * o.a
            val x3 = a * o.b + b * o.a
            if (abs(x4) > 1e-9 || abs(x3) > 1e-9) throw IllegalArgumentException("Degree > 2")
            return Poly2(a * o.c + b * o.b + c * o.a, b * o.c + c * o.b, c * o.c)
        }
        fun divScalar(d: Double): Poly2 {
            if (abs(d) < 1e-12) throw ArithmeticException("Division by zero")
            return Poly2(a / d, b / d, c / d)
        }
        fun powInt(exp: Int): Poly2 {
            return when (exp) {
                0 -> Poly2(0.0, 0.0, 1.0)
                1 -> this
                2 -> this.mul(this)
                else -> {
                    if (abs(a) > 1e-9 || abs(b) > 1e-9) throw IllegalArgumentException("Degree > 2")
                    Poly2(0.0, 0.0, c.pow(exp.toDouble()))
                }
            }
        }
    }

    private fun trySolveEquation(
        rawInput: String,
        leftRaw: String,
        rightRaw: String,
        useBangla: Boolean,
        language: AppLanguage
    ): MathSolveResult? {
        val combined = "$leftRaw=$rightRaw"
        val varChar = combined.firstOrNull { it in listOf('x', 'X', 'y', 'Y', 'a', 'm', 'n', 'ক') } ?: return null
        val varSymbol = if (varChar == 'X') 'x' else if (varChar == 'Y') 'y' else varChar
        val lNorm = leftRaw.replace(varChar, 'x').replace(varChar.uppercaseChar(), 'x').replace("\\s+".toRegex(), "")
        val rNorm = rightRaw.replace(varChar, 'x').replace(varChar.uppercaseChar(), 'x').replace("\\s+".toRegex(), "")

        return try {
            val lPoly = PolyParser(lNorm).parse()
            val rPoly = PolyParser(rNorm).parse()
            val net = lPoly - rPoly // a*x^2 + b*x + c = 0

            val orWord = if (language == AppLanguage.BANGLA) "বা," else "Or,"
            val therefore = "∴"

            if (abs(net.a) > 1e-9) {
                // Quadratic: ax^2 + bx + c = 0
                val a = net.a
                val b = net.b
                val c = net.c
                val disc = b * b - 4 * a * c
                val khata = mutableListOf(
                    "${leftRaw} = ${rightRaw}",
                    "$orWord ${formatPolyTerm(a, "${varSymbol}²")}${formatSignedTerm(b, "$varSymbol")}${formatSignedConst(c)} = 0",
                    "$orWord D = (${formatNumber(b)})² - 4(${formatNumber(a)})(${formatNumber(c)}) = ${formatNumber(disc)}"
                )
                val steps = mutableListOf<String>()
                if (disc < -1e-9) {
                    val msg = if (language == AppLanguage.BANGLA) "বাস্তব সমাধান নেই (D < 0)" else "No real roots (D < 0)"
                    khata += "$therefore $msg"
                    steps += msg
                    return MathSolveResult.Success(
                        originalInput = digits(rawInput, useBangla),
                        normalizedExpression = "$lNorm=$rNorm",
                        answerText = digits(msg, useBangla),
                        khataLines = khata.map { digits(it, useBangla) },
                        explanationSteps = steps.map { digits(it, useBangla) },
                        layoutType = MathLayoutType.CALCULATOR
                    )
                } else if (abs(disc) <= 1e-9) {
                    val root = -b / (2 * a)
                    val rStr = formatNumber(root)
                    khata += "$therefore $varSymbol = $rStr"
                    steps += if (language == AppLanguage.BANGLA) {
                        listOf("ধাপ ১: D = ০ হওয়ায় সমীকরণের একটি সমান মূল রয়েছে", "চূড়ান্ত উত্তর: $varSymbol = $rStr")
                    } else {
                        listOf("Step 1: Discriminant D = 0, so one repeated root", "Final Answer: $varSymbol = $rStr")
                    }
                    return MathSolveResult.Success(
                        originalInput = digits(rawInput, useBangla),
                        normalizedExpression = "$lNorm=$rNorm",
                        answerText = digits("$varSymbol = $rStr", useBangla),
                        khataLines = khata.map { digits(it, useBangla) },
                        explanationSteps = steps.map { digits(it, useBangla) },
                        layoutType = MathLayoutType.CALCULATOR
                    )
                } else {
                    val sqrtD = sqrt(disc)
                    val x1 = (-b + sqrtD) / (2 * a)
                    val x2 = (-b - sqrtD) / (2 * a)
                    val x1Str = formatNumber(x1)
                    val x2Str = formatNumber(x2)
                    khata += "$orWord $varSymbol = (-${formatNumber(b)} ± ${formatNumber(sqrtD)}) ÷ ${formatNumber(2 * a)}"
                    khata += "$therefore $varSymbol = $x1Str, $x2Str"
                    steps += if (language == AppLanguage.BANGLA) {
                        listOf(
                            "ধাপ ১: ax² + bx + c = ০ আকারে সাজাই (a=${formatNumber(a)}, b=${formatNumber(b)}, c=${formatNumber(c)})",
                            "ধাপ ২: নিরূপক D = b² - 4ac = ${formatNumber(disc)}, √D = ${formatNumber(sqrtD)}",
                            "ধাপ ৩: $varSymbol = (-b ± √D) / 2a ব্যবহার করে পাই $varSymbol = $x1Str অথবা $x2Str"
                        )
                    } else {
                        listOf(
                            "Step 1: Arrange as ax² + bx + c = 0 (a=${formatNumber(a)}, b=${formatNumber(b)}, c=${formatNumber(c)})",
                            "Step 2: Discriminant D = b² - 4ac = ${formatNumber(disc)}, √D = ${formatNumber(sqrtD)}",
                            "Step 3: Using $varSymbol = (-b ± √D) / 2a -> $varSymbol = $x1Str or $x2Str"
                        )
                    }
                    return MathSolveResult.Success(
                        originalInput = digits(rawInput, useBangla),
                        normalizedExpression = "$lNorm=$rNorm",
                        answerText = digits("$varSymbol = $x1Str, $x2Str", useBangla),
                        khataLines = khata.map { digits(it, useBangla) },
                        explanationSteps = steps.map { digits(it, useBangla) },
                        layoutType = MathLayoutType.CALCULATOR
                    )
                }
            }

            // Linear: b*x + c = 0 -> coeff*x = rhs
            val coeff = net.b
            val rhs = -net.c
            if (abs(coeff) < 1e-9) {
                val msg = if (abs(rhs) < 1e-9) {
                    if (language == AppLanguage.BANGLA) "অসংখ্য সমাধান" else "Infinite solutions"
                } else {
                    if (language == AppLanguage.BANGLA) "কোনো সমাধান নেই" else "No solution"
                }
                return MathSolveResult.Success(
                    originalInput = digits(rawInput, useBangla),
                    normalizedExpression = "$lNorm=$rNorm",
                    answerText = msg,
                    khataLines = listOf(digits("$leftRaw = $rightRaw", useBangla), msg),
                    explanationSteps = listOf(msg),
                    layoutType = MathLayoutType.CALCULATOR
                )
            }

            val ans = rhs / coeff
            val ansStr = formatNumber(ans)
            val coeffStr = formatNumber(coeff)
            val rhsStr = formatNumber(rhs)
            val khata = mutableListOf(
                "$leftRaw = $rightRaw",
                "$orWord ${formatNumber(lPoly.b)}$varSymbol ${formatSignedConst(lPoly.c)} = ${formatNumber(rPoly.b)}$varSymbol ${formatSignedConst(rPoly.c)}",
                "$orWord ${coeffStr}$varSymbol = $rhsStr"
            )
            if (abs(coeff - 1.0) > 1e-9) {
                khata += "$orWord $varSymbol = $rhsStr ÷ $coeffStr"
            }
            khata += "$therefore $varSymbol = $ansStr"

            val steps = if (language == AppLanguage.BANGLA) {
                listOf(
                    "ধাপ ১: চলক ($varSymbol) যুক্ত পদ বাম পাশে ও ধ্রুবক পদ ডান পাশে আনি",
                    "ধাপ ২: ${coeffStr}$varSymbol = $rhsStr",
                    "ধাপ ৩: $varSymbol = $rhsStr ÷ $coeffStr = $ansStr"
                )
            } else {
                listOf(
                    "Step 1: Move variable ($varSymbol) terms to LHS and constants to RHS",
                    "Step 2: ${coeffStr}$varSymbol = $rhsStr",
                    "Step 3: $varSymbol = $rhsStr ÷ $coeffStr = $ansStr"
                )
            }

            MathSolveResult.Success(
                originalInput = digits(rawInput, useBangla),
                normalizedExpression = "$lNorm=$rNorm",
                answerText = digits("$varSymbol = $ansStr", useBangla),
                khataLines = khata.map { digits(it, useBangla) },
                explanationSteps = steps.map { digits(it, useBangla) },
                layoutType = MathLayoutType.CALCULATOR
            )
        } catch (e: ArithmeticException) {
            MathSolveResult.Error("Division by zero", isDivideByZero = true)
        } catch (_: Exception) {
            null
        }
    }

    private fun formatPolyTerm(v: Double, suffix: String): String {
        val s = formatNumber(v)
        return when {
            s == "1" -> suffix
            s == "-1" -> "-$suffix"
            else -> "$s$suffix"
        }
    }

    private fun formatSignedTerm(v: Double, suffix: String): String {
        if (abs(v) < 1e-9) return ""
        val sign = if (v >= 0) " + " else " - "
        val mag = formatNumber(abs(v))
        return if (mag == "1") "$sign$suffix" else "$sign$mag$suffix"
    }

    private fun formatSignedConst(v: Double): String {
        if (abs(v) < 1e-9) return ""
        val sign = if (v >= 0) " + " else " - "
        return "$sign${formatNumber(abs(v))}"
    }

    private class PolyParser(private val s: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < s.length) s[pos].code else -1
        }

        private fun eat(c: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == c) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Poly2 {
            nextChar()
            val res = parseAddSub()
            if (pos < s.length) throw IllegalArgumentException("Unexpected char")
            return res
        }

        private fun parseAddSub(): Poly2 {
            var x = parseMulDiv()
            while (true) {
                x = when {
                    eat('+'.code) -> x + parseMulDiv()
                    eat('-'.code) -> x - parseMulDiv()
                    else -> return x
                }
            }
        }

        private fun parseMulDiv(): Poly2 {
            var x = parsePower()
            while (true) {
                if (eat('*'.code)) {
                    x = x.mul(parsePower())
                } else if (eat('/'.code)) {
                    val d = parsePower()
                    if (abs(d.a) > 1e-9 || abs(d.b) > 1e-9) throw IllegalArgumentException("Var in denom")
                    x = x.divScalar(d.c)
                } else if (ch == '('.code || ch == 'x'.code) {
                    x = x.mul(parsePower())
                } else {
                    return x
                }
            }
        }

        private fun parsePower(): Poly2 {
            var base = parseAtom()
            if (eat('^'.code)) {
                val exp = parseAtom()
                if (abs(exp.a) > 1e-9 || abs(exp.b) > 1e-9) throw IllegalArgumentException("Var in exp")
                val eInt = exp.c.roundToLong().toInt()
                if (abs(exp.c - eInt) > 1e-9 || eInt < 0) throw IllegalArgumentException("Non-int exp")
                base = base.powInt(eInt)
            }
            return base
        }

        private fun parseAtom(): Poly2 {
            if (eat('+'.code)) return parseAtom()
            if (eat('-'.code)) return -parseAtom()
            val start = pos
            return when {
                eat('('.code) -> {
                    val inner = parseAddSub()
                    if (!eat(')'.code)) throw IllegalArgumentException("Missing ')'")
                    inner
                }
                eat('x'.code) -> Poly2(0.0, 1.0, 0.0)
                (ch in '0'.code..'9'.code) || ch == '.'.code -> {
                    while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                    val num = s.substring(start, pos).toDouble()
                    Poly2(0.0, 0.0, num)
                }
                else -> throw IllegalArgumentException("Unexpected char")
            }
        }
    }

    private fun gcdLong(a: Long, b: Long): Long {
        var x = abs(a)
        var y = abs(b)
        while (y != 0L) {
            val t = x % y
            x = y
            y = t
        }
        return if (x == 0L) 1L else x
    }

    private fun lcmLong(a: Long, b: Long): Long = abs(a / gcdLong(a, b) * b)

    private fun formatNumber(value: Double): String {
        if (abs(value) < 1e-11) return "0"
        val rounded = value.roundToLong()
        return if (abs(value - rounded.toDouble()) < 1e-9) {
            rounded.toString()
        } else {
            String.format(java.util.Locale.US, "%.4f", value)
                .trimEnd('0')
                .trimEnd('.')
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
            val parser = RecursiveParser(normalized, steps)
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
        private val steps: MutableList<String>
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
            var x = parsePower()
            while (true) {
                if (eat('*'.code)) {
                    val y = parsePower()
                    val res = x * y
                    steps += "${formatVal(x)} × ${formatVal(y)} = ${formatVal(res)}"
                    x = res
                } else if (eat('/'.code)) {
                    val y = parsePower()
                    if (abs(y) < 1e-12) throw ArithmeticException("Division by zero")
                    val res = x / y
                    steps += "${formatVal(x)} ÷ ${formatVal(y)} = ${formatVal(res)}"
                    x = res
                } else if (ch == '('.code || ch == '√'.code || ch == '∛'.code || ch == 'π'.code) {
                    // Implicit multiplication e.g. 2(3+4) or 3√16
                    val y = parsePower()
                    val res = x * y
                    steps += "${formatVal(x)} × ${formatVal(y)} = ${formatVal(res)}"
                    x = res
                } else {
                    return x
                }
            }
        }

        private fun parsePower(): Double {
            val base = parsePostfix()
            if (eat('^'.code)) {
                val exp = parsePower() // right-associative
                val res = base.pow(exp)
                steps += "${formatVal(base)}^${formatVal(exp)} = ${formatVal(res)}"
                return res
            }
            return base
        }

        private fun parsePostfix(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('%'.code)) {
                    val res = x / 100.0
                    steps += "${formatVal(x)}% = ${formatVal(res)}"
                    x = res
                } else if (eat('!'.code)) {
                    val n = x.roundToLong()
                    if (n < 0 || n > 20 || abs(x - n.toDouble()) > 1e-9) {
                        throw IllegalArgumentException("Invalid factorial")
                    }
                    var fact = 1L
                    for (i in 2..n) fact *= i
                    val res = fact.toDouble()
                    steps += "$n! = $fact"
                    x = res
                } else {
                    return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            if (eat('√'.code)) {
                val v = parseFactor()
                if (v < 0) throw IllegalArgumentException("Negative sqrt")
                val res = sqrt(v)
                steps += "√(${formatVal(v)}) = ${formatVal(res)}"
                return res
            }
            if (eat('∛'.code)) {
                val v = parseFactor()
                val res = cbrt(v)
                steps += "∛(${formatVal(v)}) = ${formatVal(res)}"
                return res
            }
            if (eat('π'.code)) return PI

            val x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseAddSub()
                if (!eat(')'.code)) throw IllegalArgumentException("Missing ')'")
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else if (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) {
                while (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) nextChar()
                val name = str.substring(startPos, pos).lowercase()
                when (name) {
                    "pi" -> x = PI
                    "e" -> x = kotlin.math.E
                    "sqrt" -> {
                        val arg = parseFactor()
                        if (arg < 0) throw IllegalArgumentException("Negative sqrt")
                        x = sqrt(arg)
                        steps += "√(${formatVal(arg)}) = ${formatVal(x)}"
                    }
                    "cbrt" -> {
                        val arg = parseFactor()
                        x = cbrt(arg)
                        steps += "∛(${formatVal(arg)}) = ${formatVal(x)}"
                    }
                    "abs" -> {
                        val arg = parseFactor()
                        x = abs(arg)
                    }
                    else -> throw IllegalArgumentException("Unknown function: $name")
                }
            } else {
                throw IllegalArgumentException("Unexpected char")
            }
            return x
        }

        private fun formatVal(v: Double): String {
            val r = floor(v)
            return if (abs(v - r) < 1e-9) v.roundToLong().toString()
            else String.format(java.util.Locale.US, "%.4f", v).trimEnd('0').trimEnd('.')
        }
    }
}
