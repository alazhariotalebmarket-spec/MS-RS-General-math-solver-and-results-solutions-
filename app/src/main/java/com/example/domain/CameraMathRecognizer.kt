package com.example.domain

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.core.DigitHelper
import com.example.data.MathLayoutType
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class RecognizedMathResult(
    val expression: String,
    val detectedLayout: MathLayoutType,
    val containsBanglaDigits: Boolean,
    val rawOcrText: String
)

object CameraMathRecognizer {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizeFromBitmap(bitmap: Bitmap): RecognizedMathResult = withContext(Dispatchers.Default) {
        val mlKitText = runMlKit(bitmap)
        val mlKitParsed = parseMlKitSpatialLayout(mlKitText)
        if (mlKitParsed != null) {
            return@withContext mlKitParsed
        }

        // Fallback / Bangla digit spatial connected-component analyzer
        val componentParsed = analyzeConnectedComponentsForBanglaAndMath(bitmap)
        if (componentParsed != null) {
            return@withContext componentParsed
        }

        val cleanedRaw = cleanMathTokens(mlKitText.text)
        RecognizedMathResult(
            expression = cleanedRaw,
            detectedLayout = MathLayoutType.HORIZONTAL,
            containsBanglaDigits = cleanedRaw.any { it in '০'..'৯' },
            rawOcrText = mlKitText.text
        )
    }

    private suspend fun runMlKit(bitmap: Bitmap): Text = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                if (cont.isActive) cont.resume(result)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(Text("", emptyList<Text.TextBlock>()))
            }
    }

    private fun parseMlKitSpatialLayout(text: Text): RecognizedMathResult? {
        val lines = text.textBlocks.flatMap { it.lines }
            .filter { it.boundingBox != null && it.text.isNotBlank() }
            .sortedBy { it.boundingBox!!.top }

        if (lines.isEmpty()) return null

        // Check for long division pattern in any line or combined text: e.g. "2 ) 6 ( 3" or "2)6"
        val combined = lines.joinToString(" ") { it.text }
        val normalizedCombined = cleanMathTokens(combined)
        val longDivRegex = Regex("""(\d+)\s*\)\s*(\d+)""")
        val divMatch = longDivRegex.find(normalizedCombined)
        if (divMatch != null) {
            val divisor = divMatch.groupValues[1]
            val dividend = divMatch.groupValues[2]
            return RecognizedMathResult(
                expression = "$divisor ) $dividend (",
                detectedLayout = MathLayoutType.LONG_DIVISION,
                containsBanglaDigits = false,
                rawOcrText = combined
            )
        }

        // Check if vertical stacked format: multiple short numeric/operator lines stacked vertically
        val mathLines = lines.mapNotNull { line ->
            val cleaned = cleanMathTokens(line.text)
            if (cleaned.any { it.isDigit() } || cleaned.any { it in listOf('+', '-', '×', '÷', '*', '/') }) {
                Pair(line.boundingBox!!, cleaned)
            } else null
        }.filter { !it.second.all { ch -> ch == '-' || ch == '_' || ch == '=' } }

        if (mathLines.size >= 2) {
            val first = mathLines[0]
            val second = mathLines[1]
            val horizontalOverlap = min(first.first.right, second.first.right) - max(first.first.left, second.first.left)
            val verticallyStacked = second.first.top > first.first.top && horizontalOverlap > -40
            val firstNum = first.second.filter { it.isDigit() || it == '.' }
            val secondHasOp = second.second.firstOrNull { it in listOf('+', '-', '×', '÷', '*', '/') }
            val secondNum = second.second.filter { it.isDigit() || it == '.' }

            if (verticallyStacked && firstNum.isNotEmpty() && secondHasOp != null && secondNum.isNotEmpty()) {
                return RecognizedMathResult(
                    expression = "$firstNum $secondHasOp $secondNum",
                    detectedLayout = MathLayoutType.VERTICAL,
                    containsBanglaDigits = false,
                    rawOcrText = combined
                )
            }
        }

        // Otherwise horizontal single-line math expression
        val bestHorizontal = lines
            .map { cleanMathTokens(it.text) }
            .firstOrNull { line ->
                line.any { it.isDigit() } && line.any { it in listOf('+', '-', '×', '÷', '*', '/') }
            }

        if (bestHorizontal != null) {
            val expr = bestHorizontal.substringBefore('=').trim()
            if (expr.isNotEmpty()) {
                return RecognizedMathResult(
                    expression = expr,
                    detectedLayout = MathLayoutType.HORIZONTAL,
                    containsBanglaDigits = false,
                    rawOcrText = combined
                )
            }
        }

        return null
    }

    private fun cleanMathTokens(input: String): String {
        val sb = StringBuilder()
        val s = input
            .replace("—", "-")
            .replace("–", "-")
            .replace("÷", "÷")
            .replace("×", "×")
        for (ch in s) {
            when {
                ch in '0'..'9' -> sb.append(ch)
                ch in '০'..'৯' -> sb.append(ch)
                ch == '+' || ch == '-' || ch == '×' || ch == '÷' || ch == '/' || ch == '*' ||
                    ch == '=' || ch == '(' || ch == ')' || ch == '.' || ch == ' ' -> sb.append(ch)
                ch == 'x' || ch == 'X' -> sb.append('×')
                ch == 'O' || ch == 'o' -> sb.append('0')
                ch == 'l' || ch == 'I' || ch == '|' -> sb.append('1')
            }
        }
        return sb.toString().replace("\\s+".toRegex(), " ").trim()
    }

    /**
     * Offline connected-component glyph & layout analyzer supporting Bangla digits (০-৯)
     * and horizontal/vertical arithmetic expressions when ML Kit Latin returns empty.
     */
    private fun analyzeConnectedComponentsForBanglaAndMath(src: Bitmap): RecognizedMathResult? {
        val targetW = 320
        val scale = targetW.toFloat() / max(1, src.width)
        val targetH = max(64, (src.height * scale).toInt())
        val bmp = Bitmap.createScaledBitmap(src, targetW, targetH, true)

        val pixels = IntArray(targetW * targetH)
        bmp.getPixels(pixels, 0, targetW, 0, 0, targetW, targetH)

        // Adaptive luminance thresholding
        val lum = IntArray(pixels.size)
        var sumLum = 0L
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val l = (r * 30 + g * 59 + b * 11) / 100
            lum[i] = l
            sumLum += l
        }
        val avgLum = (sumLum / max(1, pixels.size)).toInt()
        val threshold = (avgLum * 0.72f).toInt().coerceIn(45, 165)

        val binary = BooleanArray(pixels.size) { lum[it] < threshold }
        val visited = BooleanArray(pixels.size)
        val boxes = mutableListOf<Rect>()

        val queue = IntArray(pixels.size)
        for (y in 2 until targetH - 2) {
            for (x in 2 until targetW - 2) {
                val idx = y * targetW + x
                if (binary[idx] && !visited[idx]) {
                    var head = 0
                    var tail = 0
                    queue[tail++] = idx
                    visited[idx] = true
                    var minX = x
                    var maxX = x
                    var minY = y
                    var maxY = y
                    var area = 0
                    while (head < tail) {
                        val cur = queue[head++]
                        val cx = cur % targetW
                        val cy = cur / targetW
                        area++
                        if (cx < minX) minX = cx
                        if (cx > maxX) maxX = cx
                        if (cy < minY) minY = cy
                        if (cy > maxY) maxY = cy

                        for (dy in -1..1) {
                            for (dx in -1..1) {
                                val nx = cx + dx
                                val ny = cy + dy
                                if (nx in 1 until targetW - 1 && ny in 1 until targetH - 1) {
                                    val nIdx = ny * targetW + nx
                                    if (binary[nIdx] && !visited[nIdx]) {
                                        visited[nIdx] = true
                                        queue[tail++] = nIdx
                                    }
                                }
                            }
                        }
                    }
                    val w = maxX - minX + 1
                    val h = maxY - minY + 1
                    if (area >= 18 && w >= 4 && h >= 4 && w < targetW * 0.85 && h < targetH * 0.85) {
                        boxes.add(Rect(minX, minY, maxX, maxY))
                    }
                }
            }
        }

        if (boxes.size < 2) return null

        // Group boxes into horizontal rows by centerY
        val sortedByY = boxes.sortedBy { it.centerY() }
        val rows = mutableListOf<MutableList<Rect>>()
        for (box in sortedByY) {
            val row = rows.lastOrNull()
            if (row == null) {
                rows.add(mutableListOf(box))
            } else {
                val avgCenterY = row.map { it.centerY() }.average()
                val avgHeight = row.map { it.height() }.average()
                if (abs(box.centerY() - avgCenterY) <= max(14.0, avgHeight * 0.65)) {
                    row.add(box)
                } else {
                    rows.add(mutableListOf(box))
                }
            }
        }

        val decodedRows = rows.mapNotNull { row ->
            val sortedRow = row.sortedBy { it.left }
            // Skip horizontal rule separator line "---"
            if (sortedRow.size == 1 && sortedRow[0].width() > sortedRow[0].height() * 3.5) {
                return@mapNotNull null
            }
            val chars = sortedRow.map { rect ->
                classifyGlyph(binary, targetW, rect)
            }.joinToString("")
            chars.ifBlank { null }
        }

        if (decodedRows.isEmpty()) return null
        val expression = if (decodedRows.size >= 2 && decodedRows[1].firstOrNull() in listOf('+', '-', '×', '÷')) {
            "${decodedRows[0]} ${decodedRows[1].first()} ${decodedRows[1].drop(1)}"
        } else {
            decodedRows.first()
        }

        val hasMathOp = expression.any { it in listOf('+', '-', '×', '÷', ')') }
        val hasDigit = expression.any { it in '০'..'৯' || it in '0'..'9' }
        if (!hasMathOp || !hasDigit) return null

        val layout = if (decodedRows.size >= 2) MathLayoutType.VERTICAL else MathLayoutType.HORIZONTAL
        return RecognizedMathResult(
            expression = DigitHelper.toBanglaDigits(expression),
            detectedLayout = layout,
            containsBanglaDigits = true,
            rawOcrText = expression
        )
    }

    private fun classifyGlyph(binary: BooleanArray, imgW: Int, r: Rect): Char {
        val w = max(1, r.width())
        val h = max(1, r.height())
        val aspect = w.toFloat() / h.toFloat()

        // Horizontal bar -> '-'
        if (aspect > 2.2f) return '-'

        // Check density in 3x3 grid
        val zones = FloatArray(9)
        val totalPixels = w * h
        var inkCount = 0
        for (y in r.top..r.bottom) {
            for (x in r.left..r.right) {
                if (binary[y * imgW + x]) {
                    inkCount++
                    val zx = ((x - r.left) * 3 / (w + 1)).coerceIn(0, 2)
                    val zy = ((y - r.top) * 3 / (h + 1)).coerceIn(0, 2)
                    zones[zy * 3 + zx] += 1f
                }
            }
        }
        val fillRatio = inkCount.toFloat() / totalPixels
        for (i in zones.indices) zones[i] = zones[i] / max(1, inkCount)

        // Check symmetry / cross for '+' or '×'
        val centerDensity = zones[4]
        val crossArms = zones[1] + zones[3] + zones[5] + zones[7]
        val corners = zones[0] + zones[2] + zones[6] + zones[8]
        if (aspect in 0.7f..1.35f) {
            if (crossArms > 0.55f && corners < 0.25f) return '+'
            if (corners > 0.45f && centerDensity > 0.14f) return '×'
        }
        // Division '÷' has strong center row + top/bottom dots
        if (zones[3] + zones[4] + zones[5] > 0.42f && zones[1] > 0.08f && zones[7] > 0.08f && zones[0] < 0.08f && zones[2] < 0.08f) {
            return '÷'
        }
        // Parenthesis ')'
        if (aspect < 0.45f && zones[2] + zones[5] + zones[8] > zones[0] + zones[3] + zones[6]) {
            return ')'
        }

        // Heuristic structural mapping for Bangla numerals ০-৯
        return when {
            fillRatio < 0.35f && zones[4] < 0.08f && zones[1] + zones[3] + zones[5] + zones[7] > 0.5f -> '০'
            zones[0] + zones[1] + zones[3] > 0.45f && zones[8] > 0.12f -> '১'
            zones[0] + zones[2] + zones[6] + zones[7] > 0.48f -> '২'
            zones[2] + zones[5] + zones[8] > 0.44f && zones[0] < 0.14f -> '৩'
            zones[0] + zones[3] + zones[6] > 0.40f && zones[2] + zones[5] + zones[8] > 0.38f -> '৪'
            zones[0] + zones[3] + zones[7] + zones[8] > 0.48f -> '৫'
            zones[6] + zones[7] + zones[8] > 0.36f && zones[1] > 0.12f -> '৬'
            zones[0] + zones[1] + zones[2] > 0.38f && zones[7] > 0.12f -> '৭'
            zones[0] + zones[3] + zones[6] > 0.48f && zones[4] > 0.12f -> '৮'
            else -> '৯'
        }
    }
}
