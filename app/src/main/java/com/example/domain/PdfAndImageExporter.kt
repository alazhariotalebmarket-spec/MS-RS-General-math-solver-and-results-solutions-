package com.example.domain

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.core.AppLanguage
import com.example.core.AppStrings
import com.example.core.DigitHelper
import com.example.data.ComputedStudentResult
import com.example.data.MathHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

object PdfAndImageExporter {

    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842
    private val json = Json { ignoreUnknownKeys = true }

    private fun getBengaliTypeface(context: Context, bold: Boolean = false): Typeface {
        val base = ResourcesCompat.getFont(context, R.font.noto_sans_bengali) ?: Typeface.DEFAULT
        return if (bold) Typeface.create(base, Typeface.BOLD) else base
    }

    private fun getMonoTypeface(context: Context, bold: Boolean = false): Typeface {
        val base = ResourcesCompat.getFont(context, R.font.jetbrains_mono) ?: Typeface.MONOSPACE
        return if (bold) Typeface.create(base, Typeface.BOLD) else base
    }

    suspend fun generateMarksheetPdfFile(
        context: Context,
        result: ComputedStudentResult,
        language: AppLanguage,
        strings: AppStrings
    ): File = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create()
        val page = doc.startPage(pageInfo)
        drawMarksheetCanvas(context, page.canvas, A4_WIDTH.toFloat(), A4_HEIGHT.toFloat(), result, language, strings)
        doc.finishPage(page)

        val outFile = File(context.cacheDir, "MSRS_Marksheet_Roll_${result.entity.rollNumber}.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        outFile
    }

    suspend fun writeMarksheetPdfToUri(
        context: Context,
        uri: Uri,
        result: ComputedStudentResult,
        language: AppLanguage,
        strings: AppStrings
    ) = withContext(Dispatchers.IO) {
        val temp = generateMarksheetPdfFile(context, result, language, strings)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            FileInputStream(temp).use { inp -> inp.copyTo(out) }
        }
    }

    suspend fun writeClassMeritListPdfToUri(
        context: Context,
        uri: Uri,
        results: List<ComputedStudentResult>,
        language: AppLanguage,
        strings: AppStrings
    ) = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        val fmt = { s: String -> DigitHelper.format(s, language) }
        val sorted = results.sortedWith(
            compareBy<ComputedStudentResult> { it.meritPosition ?: Int.MAX_VALUE }
                .thenByDescending { it.entity.totalObtainedMarks }
                .thenBy { it.entity.rollNumber }
        )
        val rowsPerPage = 22
        val chunks = if (sorted.isEmpty()) listOf(emptyList()) else sorted.chunked(rowsPerPage)

        chunks.forEachIndexed { pageIndex, pageRows ->
            val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageIndex + 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)
            val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0E7A3D")
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.drawRect(18f, 18f, A4_WIDTH - 18f, A4_HEIGHT - 18f, border)

            val headerBg = Paint().apply { color = Color.parseColor("#0E7A3D") }
            canvas.drawRect(28f, 28f, A4_WIDTH - 28f, 92f, headerBg)

            val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = getBengaliTypeface(context, bold = true)
                textSize = 16f
                textAlign = Paint.Align.CENTER
            }
            val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFF4D2")
                typeface = getBengaliTypeface(context, bold = false)
                textSize = 11f
                textAlign = Paint.Align.CENTER
            }
            val firstInst = sorted.firstOrNull()?.entity?.institutionName?.ifBlank { "MS RS" } ?: "MS RS"
            val firstExam = sorted.firstOrNull()?.entity?.examName ?: strings.resultListTitle
            canvas.drawText("$firstInst — ${strings.classMeritPdf}", A4_WIDTH / 2f, 56f, titleP)
            canvas.drawText(firstExam, A4_WIDTH / 2f, 78f, subP)

            val left = 28f
            val right = A4_WIDTH - 28f
            val w = right - left
            val cols = floatArrayOf(
                left,
                left + w * 0.11f,
                left + w * 0.22f,
                left + w * 0.56f,
                left + w * 0.72f,
                left + w * 0.85f,
                right
            )

            var curY = 108f
            val rowH = 26f
            canvas.drawRect(left, curY, right, curY + rowH, headerBg)
            val thP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = getBengaliTypeface(context, bold = true)
                textSize = 10f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(strings.positionMerit, (cols[0] + cols[1]) / 2f, curY + 17f, thP)
            canvas.drawText(strings.rollNumberLabel, (cols[1] + cols[2]) / 2f, curY + 17f, thP)
            canvas.drawText(strings.studentNameLabel, (cols[2] + cols[3]) / 2f, curY + 17f, thP)
            canvas.drawText(strings.obtainedTotal, (cols[3] + cols[4]) / 2f, curY + 17f, thP)
            canvas.drawText(strings.gpaLabel, (cols[4] + cols[5]) / 2f, curY + 17f, thP)
            canvas.drawText(strings.statusLabel, (cols[5] + cols[6]) / 2f, curY + 17f, thP)
            curY += rowH

            val cellBorder = Paint().apply {
                color = Color.parseColor("#C5DDD0")
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }
            val cellP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#112219")
                typeface = getBengaliTypeface(context, bold = false)
                textSize = 10f
                textAlign = Paint.Align.CENTER
            }
            val nameP = Paint(cellP).apply { textAlign = Paint.Align.LEFT }

            pageRows.forEach { item ->
                val e = item.entity
                canvas.drawRect(left, curY, right, curY + rowH, cellBorder)
                val posTxt = item.meritPosition?.let { fmt("#$it") } ?: "-"
                canvas.drawText(posTxt, (cols[0] + cols[1]) / 2f, curY + 17f, cellP)
                canvas.drawText(fmt(e.rollNumber.toString()), (cols[1] + cols[2]) / 2f, curY + 17f, cellP)
                canvas.drawText(e.studentName.take(26), cols[2] + 6f, curY + 17f, nameP)
                canvas.drawText(
                    "${fmt(ResultCalculator.formatMarks(e.totalObtainedMarks))}/${fmt(e.totalFullMarks.toString())}",
                    (cols[3] + cols[4]) / 2f,
                    curY + 17f,
                    cellP
                )
                canvas.drawText(fmt(String.format(java.util.Locale.US, "%.2f", e.totalGpa)), (cols[4] + cols[5]) / 2f, curY + 17f, cellP)
                val stP = Paint(cellP).apply {
                    color = if (e.isPassed) Color.parseColor("#0E7A3D") else Color.parseColor("#C62828")
                    typeface = getBengaliTypeface(context, bold = true)
                }
                canvas.drawText(if (e.isPassed) strings.passed else strings.failed, (cols[5] + cols[6]) / 2f, curY + 17f, stP)
                curY += rowH
            }

            doc.finishPage(page)
        }

        context.contentResolver.openOutputStream(uri)?.use { out ->
            doc.writeTo(out)
        }
        doc.close()
    }

    private fun drawMarksheetCanvas(
        context: Context,
        canvas: Canvas,
        width: Float,
        height: Float,
        computed: ComputedStudentResult,
        language: AppLanguage,
        strings: AppStrings
    ) {
        val e = computed.entity
        val fmt = { s: String -> DigitHelper.format(s, language) }

        val bgPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        // Outer Emerald Border + Inner Gold Border
        val outerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0E7A3D")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val innerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4AF37")
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
        }
        canvas.drawRect(18f, 18f, width - 18f, height - 18f, outerBorder)
        canvas.drawRect(25f, 25f, width - 25f, height - 25f, innerBorder)

        // Header banner
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0E7A3D")
        }
        canvas.drawRoundRect(RectF(34f, 34f, width - 34f, 124f), 10f, 10f, headerPaint)

        // Draw MS RS crest logo on left
        val logoBmp = BitmapFactory.decodeResource(context.resources, R.drawable.img_msrs_logo)
        if (logoBmp != null) {
            val destLogo = RectF(46f, 44f, 114f, 112f)
            canvas.drawBitmap(logoBmp, null, destLogo, null)
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = getBengaliTypeface(context, bold = true)
            textSize = 17f
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFF4D2")
            typeface = getBengaliTypeface(context, bold = false)
            textSize = 12f
            textAlign = Paint.Align.CENTER
        }
        val instName = e.institutionName.ifBlank { "MS RS Educational Institution" }
        canvas.drawText(instName, width / 2f + 16f, 64f, titlePaint)
        canvas.drawText("${e.examName} • ${fmt(e.yearSession)}", width / 2f + 16f, 86f, subPaint)

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4AF37")
            typeface = getBengaliTypeface(context, bold = true)
            textSize = 11f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(strings.marksheetTitle, width / 2f + 16f, 108f, badgePaint)

        // Student Info Box + Optional Photo
        val infoBox = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F4FAF6")
        }
        canvas.drawRoundRect(RectF(34f, 136f, width - 34f, 232f), 8f, 8f, infoBox)
        val infoStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C5DDD0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(RectF(34f, 136f, width - 34f, 232f), 8f, 8f, infoStroke)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#112219")
            typeface = getBengaliTypeface(context, bold = false)
            textSize = 11f
        }
        val boldLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#085C2D")
            typeface = getBengaliTypeface(context, bold = true)
            textSize = 11.5f
        }

        canvas.drawText("${strings.studentNameLabel}: ${e.studentName}", 46f, 158f, boldLabelPaint)
        canvas.drawText("${strings.fatherNameLabel}: ${e.fatherName.ifBlank { "-" }}", 46f, 178f, labelPaint)
        canvas.drawText("${strings.motherNameLabel}: ${e.motherName.ifBlank { "-" }}", 46f, 198f, labelPaint)
        val meritStr = computed.meritPosition?.let { fmt("#$it") } ?: "-"
        canvas.drawText(
            "${strings.classNameLabel}: ${e.className}   |   ${strings.rollNumberLabel}: ${fmt(e.rollNumber.toString())}   |   ${strings.positionMerit}: $meritStr",
            46f,
            218f,
            labelPaint
        )

        // Student Photo if available
        if (!e.photoPath.isNullOrBlank()) {
            val f = File(e.photoPath)
            if (f.exists()) {
                val pBmp = BitmapFactory.decodeFile(f.absolutePath)
                if (pBmp != null) {
                    val photoRect = RectF(width - 114f, 144f, width - 44f, 224f)
                    canvas.drawBitmap(pBmp, null, photoRect, null)
                    canvas.drawRect(photoRect, infoStroke)
                }
            }
        }

        // Subjects Table
        val tableTop = 248f
        val rowHeight = 28f
        val leftX = 34f
        val rightX = width - 34f
        val tableW = rightX - leftX
        val cols = floatArrayOf(
            leftX,
            leftX + tableW * 0.38f,
            leftX + tableW * 0.50f,
            leftX + tableW * 0.62f,
            leftX + tableW * 0.76f,
            leftX + tableW * 0.88f,
            rightX
        )

        val tableHeaderPaint = Paint().apply { color = Color.parseColor("#0E7A3D") }
        canvas.drawRect(leftX, tableTop, rightX, tableTop + rowHeight, tableHeaderPaint)

        val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = getBengaliTypeface(context, bold = true)
            textSize = 10.5f
        }
        val thCenter = Paint(thPaint).apply { textAlign = Paint.Align.CENTER }

        canvas.drawText(strings.subjectNameLabel, cols[0] + 8f, tableTop + 18f, thPaint)
        canvas.drawText(strings.fullMarksLabel, (cols[1] + cols[2]) / 2f, tableTop + 18f, thCenter)
        canvas.drawText(strings.passMarkLabel, (cols[2] + cols[3]) / 2f, tableTop + 18f, thCenter)
        canvas.drawText(strings.obtainedMarksLabel, (cols[3] + cols[4]) / 2f, tableTop + 18f, thCenter)
        canvas.drawText(strings.gradeLabel, (cols[4] + cols[5]) / 2f, tableTop + 18f, thCenter)
        canvas.drawText(strings.gpaLabel, (cols[5] + cols[6]) / 2f, tableTop + 18f, thCenter)

        val cellBorder = Paint().apply {
            color = Color.parseColor("#C5DDD0")
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val cellText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#112219")
            typeface = getBengaliTypeface(context, bold = false)
            textSize = 10.5f
        }
        val cellCenter = Paint(cellText).apply { textAlign = Paint.Align.CENTER }
        val altRowPaint = Paint().apply { color = Color.parseColor("#F8FCFA") }

        val activeSubjects = computed.subjects.filter { it.isEnabled && it.name.isNotBlank() }
        var curY = tableTop + rowHeight
        activeSubjects.forEachIndexed { idx, sub ->
            if (idx % 2 == 1) {
                canvas.drawRect(leftX, curY, rightX, curY + rowHeight, altRowPaint)
            }
            canvas.drawRect(leftX, curY, rightX, curY + rowHeight, cellBorder)
            canvas.drawText(sub.name.take(26), cols[0] + 8f, curY + 18f, cellText)
            canvas.drawText(fmt(sub.fullMarks.toString()), (cols[1] + cols[2]) / 2f, curY + 18f, cellCenter)
            canvas.drawText(fmt(sub.passMark.toString()), (cols[2] + cols[3]) / 2f, curY + 18f, cellCenter)
            canvas.drawText(fmt(ResultCalculator.formatMarks(sub.obtainedMarks)), (cols[3] + cols[4]) / 2f, curY + 18f, cellCenter)

            val gPaint = Paint(cellCenter).apply {
                color = if (sub.isPassed) Color.parseColor("#0E7A3D") else Color.parseColor("#C62828")
                typeface = getBengaliTypeface(context, bold = true)
            }
            canvas.drawText(sub.grade, (cols[4] + cols[5]) / 2f, curY + 18f, gPaint)
            canvas.drawText(fmt(String.format(java.util.Locale.US, "%.2f", sub.gpa)), (cols[5] + cols[6]) / 2f, curY + 18f, cellCenter)
            curY += rowHeight
        }

        // Vertical column lines
        for (c in cols) {
            canvas.drawLine(c, tableTop, c, curY, cellBorder)
        }

        // Summary Card
        val sumTop = curY + 18f
        val sumRect = RectF(leftX, sumTop, rightX, sumTop + 96f)
        canvas.drawRoundRect(sumRect, 8f, 8f, infoBox)
        canvas.drawRoundRect(sumRect, 8f, 8f, infoStroke)

        canvas.drawText(
            "${strings.totalMarks}: ${fmt(e.totalFullMarks.toString())}     |     ${strings.obtainedTotal}: ${fmt(ResultCalculator.formatMarks(e.totalObtainedMarks))}     |     ${strings.percentage}: ${fmt(String.format(java.util.Locale.US, "%.1f%%", e.percentage))}",
            leftX + 14f,
            sumTop + 26f,
            boldLabelPaint
        )
        canvas.drawText(
            "${strings.gpaLabel}: ${fmt(String.format(java.util.Locale.US, "%.2f", e.totalGpa))}     |     ${strings.gradeLabel}: ${e.overallGrade}     |     ${strings.classPassRate}: ${fmt(String.format(java.util.Locale.US, "%.1f%%", computed.classPassRate))}",
            leftX + 14f,
            sumTop + 52f,
            labelPaint
        )

        val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (e.isPassed) Color.parseColor("#0E7A3D") else Color.parseColor("#C62828")
            typeface = getBengaliTypeface(context, bold = true)
            textSize = 13f
        }
        val statusText = if (e.isPassed) strings.passed else strings.failed
        canvas.drawText("${strings.statusLabel}: $statusText", leftX + 14f, sumTop + 78f, statusPaint)

        // Footer Signature & Date
        val footY = height - 62f
        val linePaint = Paint().apply {
            color = Color.parseColor("#112219")
            strokeWidth = 1f
        }
        canvas.drawLine(46f, footY, 195f, footY, linePaint)
        canvas.drawText(strings.teacherSignature, 46f, footY + 16f, labelPaint)

        canvas.drawLine(width - 195f, footY, width - 46f, footY, linePaint)
        canvas.drawText(
            "${strings.dateLabel}: ${DigitHelper.formatShortDate(e.updatedAt, language)}",
            width - 195f,
            footY + 16f,
            labelPaint
        )

        val brandFoot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B6356")
            typeface = getBengaliTypeface(context, bold = false)
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("MS RS - General Math Solver & Results Solutions", width / 2f, height - 28f, brandFoot)
    }

    suspend fun generateMathPdfFile(
        context: Context,
        math: MathHistoryEntity,
        language: AppLanguage,
        strings: AppStrings
    ): File = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create()
        val page = doc.startPage(pageInfo)
        drawMathKhataCanvas(context, page.canvas, A4_WIDTH.toFloat(), A4_HEIGHT.toFloat(), math, language, strings)
        doc.finishPage(page)

        val outFile = File(context.cacheDir, "MSRS_Math_${math.id}.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        outFile
    }

    suspend fun writeMathPdfToUri(
        context: Context,
        uri: Uri,
        math: MathHistoryEntity,
        language: AppLanguage,
        strings: AppStrings
    ) = withContext(Dispatchers.IO) {
        val temp = generateMathPdfFile(context, math, language, strings)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            FileInputStream(temp).use { inp -> inp.copyTo(out) }
        }
    }

    suspend fun generateMathImageFile(
        context: Context,
        math: MathHistoryEntity,
        language: AppLanguage,
        strings: AppStrings
    ): File = withContext(Dispatchers.IO) {
        val w = 960
        val h = 1280
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.scale(w.toFloat() / A4_WIDTH, h.toFloat() / A4_HEIGHT)
        drawMathKhataCanvas(context, canvas, A4_WIDTH.toFloat(), A4_HEIGHT.toFloat(), math, language, strings)

        val outFile = File(context.cacheDir, "MSRS_Math_${math.id}.png")
        FileOutputStream(outFile).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        outFile
    }

    suspend fun writeMathImageToUri(
        context: Context,
        uri: Uri,
        math: MathHistoryEntity,
        language: AppLanguage,
        strings: AppStrings
    ) = withContext(Dispatchers.IO) {
        val temp = generateMathImageFile(context, math, language, strings)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            FileInputStream(temp).use { inp -> inp.copyTo(out) }
        }
    }

    private fun drawMathKhataCanvas(
        context: Context,
        canvas: Canvas,
        width: Float,
        height: Float,
        math: MathHistoryEntity,
        language: AppLanguage,
        strings: AppStrings
    ) {
        val fmt = { s: String -> DigitHelper.format(s, language) }
        val khataLines = runCatching { json.decodeFromString<List<String>>(math.khataLinesJson) }.getOrDefault(emptyList())
        val steps = runCatching { json.decodeFromString<List<String>>(math.explanationStepsJson) }.getOrDefault(emptyList())

        // Warm khata paper background
        val paperPaint = Paint().apply { color = Color.parseColor("#FFFDF7") }
        canvas.drawRect(0f, 0f, width, height, paperPaint)

        // Notebook horizontal lines
        val rulePaint = Paint().apply {
            color = Color.parseColor("#DCEAE2")
            strokeWidth = 1f
        }
        var y = 110f
        while (y < height - 40f) {
            canvas.drawLine(36f, y, width - 24f, y, rulePaint)
            y += 26f
        }
        // Red notebook margin line
        val marginPaint = Paint().apply {
            color = Color.parseColor("#E57373")
            strokeWidth = 1.6f
        }
        canvas.drawLine(64f, 95f, 64f, height - 30f, marginPaint)

        // Header
        val headPaint = Paint().apply { color = Color.parseColor("#0E7A3D") }
        canvas.drawRect(0f, 0f, width, 84f, headPaint)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = getBengaliTypeface(context, bold = true)
            textSize = 18f
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFF4D2")
            typeface = getBengaliTypeface(context, bold = false)
            textSize = 11.5f
        }
        canvas.drawText("MS RS — ${strings.khataSolutionTitle}", 24f, 38f, titlePaint)
        canvas.drawText(DigitHelper.formatDate(math.createdAt, language), 24f, 62f, subPaint)

        // Problem expression
        val exprPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1565C0")
            typeface = getBengaliTypeface(context, bold = true)
            textSize = 18f
        }
        var curY = 128f
        canvas.drawText("${fmt(math.expression)} = ?", 80f, curY, exprPaint)
        curY += 38f

        // Vertical Khata work
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#112219")
            typeface = getBengaliTypeface(context, bold = true)
            textSize = 20f
        }
        for (line in khataLines) {
            canvas.drawText(fmt(line), 96f, curY, monoPaint)
            curY += 28f
        }

        curY += 22f
        val stepHeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0E7A3D")
            typeface = getBengaliTypeface(context, bold = true)
            textSize = 14f
        }
        canvas.drawText(strings.stepByStepTitle, 80f, curY, stepHeadPaint)
        curY += 26f

        val stepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#243B2E")
            typeface = getBengaliTypeface(context, bold = false)
            textSize = 12.5f
        }
        for (step in steps) {
            if (curY > height - 90f) break
            canvas.drawText("• ${fmt(step)}", 84f, curY, stepPaint)
            curY += 24f
        }

        // Answer box
        val ansTop = maxOf(curY + 14f, height - 105f).coerceAtMost(height - 90f)
        val ansBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D6F5E3") }
        val ansStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0E7A3D")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val boxRect = RectF(80f, ansTop, width - 36f, ansTop + 52f)
        canvas.drawRoundRect(boxRect, 10f, 10f, ansBg)
        canvas.drawRoundRect(boxRect, 10f, 10f, ansStroke)
        val ansPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#085C2D")
            typeface = getBengaliTypeface(context, bold = true)
            textSize = 18f
        }
        canvas.drawText("${strings.finalAnswer}: ${fmt(math.answer)}", 98f, ansTop + 33f, ansPaint)
    }

    fun printPdfFile(context: Context, pdfFile: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(pdfFile.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(jobName, adapter, PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String, targetPackage: String? = null) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (targetPackage != null) setPackage(targetPackage)
        }
        try {
            if (targetPackage != null) {
                context.startActivity(intent)
            } else {
                context.startActivity(Intent.createChooser(intent, title))
            }
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(fallback, title))
        }
    }
}
