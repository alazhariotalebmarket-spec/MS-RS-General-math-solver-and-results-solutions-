package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.DigitHelper
import com.example.core.LocalLanguage
import com.example.core.LocalStrings
import com.example.domain.ResultCalculator
import com.example.ui.components.AnimatedScaleCard
import com.example.ui.components.EyeViewButton
import com.example.ui.components.IslamicGeometricBackdrop
import com.example.ui.components.LanguageSwitchPill
import com.example.ui.components.MsRsLogoBadge
import com.example.ui.theme.LocalMsRsColors
import com.example.ui.viewmodel.RecentHistoryItem

@Composable
fun HomeScreen(
    searchQuery: String,
    recentHistory: List<RecentHistoryItem>,
    onSearchQueryChange: (String) -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenCalculatorSolver: () -> Unit,
    onOpenResultsList: () -> Unit,
    onOpenNewResult: () -> Unit,
    onOpenMathDetail: (Long) -> Unit,
    onOpenResultMarksheet: (Long) -> Unit,
    onQuickPdfDownload: () -> Unit,
    onOpenApkDownload: () -> Unit,
    onOpenStudyTools: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val lang = LocalLanguage.current
    val ext = LocalMsRsColors.current

    var showNotifications by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val filteredRecent = remember(recentHistory, searchQuery) {
        val q = searchQuery.trim().lowercase()
        val engQ = DigitHelper.toEnglishDigits(q)
        val list = if (q.isEmpty()) {
            recentHistory.take(12)
        } else {
            recentHistory.filter { item ->
                when (item) {
                    is RecentHistoryItem.MathItem -> {
                        item.math.expression.lowercase().contains(q) ||
                            item.math.answer.lowercase().contains(q) ||
                            item.math.normalizedExpression.lowercase().contains(engQ)
                    }
                    is RecentHistoryItem.ResultItem -> {
                        val e = item.result.entity
                        e.studentName.lowercase().contains(q) ||
                            e.className.lowercase().contains(q) ||
                            e.examName.lowercase().contains(q) ||
                            e.rollNumber.toString().contains(engQ)
                    }
                }
            }
        }
        list
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Green Gradient Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(ext.emeraldGradientStart, ext.emeraldGradientEnd)
                        ),
                        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                    )
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            ) {
                IslamicGeometricBackdrop(
                    tint = Color.White.copy(alpha = 0.06f),
                    modifier = Modifier.matchParentSize()
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            MsRsLogoBadge(size = 46.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.appShortName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = strings.appTagline,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.goldBright,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LanguageSwitchPill(onToggle = onToggleLanguage)
                            IconButton(
                                onClick = { showNotifications = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .testTag("notifications_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = strings.notificationsTitle,
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text(
                                text = strings.searchPlaceholder,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = ext.goldBright,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_search_field")
                    )
                }
            }
        }

        // 2 Main Big Cards (Math Solver + Results)
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 24 })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. MATH SOLVER CARD (Blue + White)
                    AnimatedScaleCard(
                        onClick = onOpenCalculatorSolver,
                        containerBrush = Brush.linearGradient(
                            listOf(ext.solverBlueStart, ext.solverBlueEnd)
                        ),
                        borderColor = Color.White.copy(alpha = 0.28f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("math_solver_big_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "০-৯ • 0-9 • + − × ÷ √ x² %",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = strings.mathSolverTitle,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = strings.mathSolverSubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Calculate,
                                            contentDescription = strings.quickCalculator,
                                            tint = ext.solverBlueStart,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.92f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Functions,
                                            contentDescription = strings.mathSolverTitle,
                                            tint = ext.solverBlueStart,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. RESULTS CARD (Green + White)
                    AnimatedScaleCard(
                        onClick = onOpenResultsList,
                        containerBrush = Brush.linearGradient(
                            listOf(ext.emeraldGradientStart, ext.emeraldGradientEnd)
                        ),
                        borderColor = ext.goldBright.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("results_big_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = ext.goldBright.copy(alpha = 0.24f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "GPA • A+ • A4 PDF",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.goldBright,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = strings.resultsTitle,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = strings.resultsSubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Visibility,
                                            contentDescription = strings.viewMarksheet,
                                            tint = ext.emeraldGradientStart,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.92f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Description,
                                            contentDescription = strings.resultsTitle,
                                            tint = ext.emeraldGradientStart,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4 Quick Action Buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionTile(
                    icon = Icons.Filled.Calculate,
                    label = strings.quickCalculator,
                    tint = ext.solverBlueStart,
                    bgColor = ext.solverBlueContainer,
                    testTag = "quick_calculator_btn",
                    onClick = onOpenCalculatorSolver,
                    modifier = Modifier.weight(1f)
                )
                QuickActionTile(
                    icon = Icons.AutoMirrored.Filled.NoteAdd,
                    label = strings.quickNewResult,
                    tint = ext.emeraldGradientStart,
                    bgColor = ext.goldContainer,
                    testTag = "quick_new_result_btn",
                    onClick = onOpenNewResult,
                    modifier = Modifier.weight(1f)
                )
                QuickActionTile(
                    icon = Icons.Filled.School,
                    label = strings.quickAllResults,
                    tint = MaterialTheme.colorScheme.primary,
                    bgColor = MaterialTheme.colorScheme.primaryContainer,
                    testTag = "quick_all_results_btn",
                    onClick = onOpenResultsList,
                    modifier = Modifier.weight(1f)
                )
                QuickActionTile(
                    icon = Icons.Filled.PictureAsPdf,
                    label = strings.quickPdfDownload,
                    tint = ext.failRed,
                    bgColor = ext.failContainer,
                    testTag = "quick_pdf_btn",
                    onClick = onQuickPdfDownload,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Study Tools Card (Namta 1-20, GPA Scale, Math Formulas - 100% Offline)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenStudyTools)
                    .testTag("home_study_tools_btn"),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ext.goldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = strings.studyToolsTitle,
                            tint = ext.onGoldContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.studyToolsTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.studyToolsSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // APK Download Quick Banner on Home Page
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenApkDownload)
                    .testTag("home_apk_download_btn"),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = strings.apkDownload,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.apkDownload,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.apkDownloadSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Recent History Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.recentHistory,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = DigitHelper.format(filteredRecent.size.toString(), lang),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (filteredRecent.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = strings.noHistoryYet,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.noHistoryTip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(
                items = filteredRecent,
                key = {
                    when (it) {
                        is RecentHistoryItem.MathItem -> "math_${it.math.id}"
                        is RecentHistoryItem.ResultItem -> "res_${it.result.entity.id}"
                    }
                }
            ) { item ->
                when (item) {
                    is RecentHistoryItem.MathItem -> {
                        val m = item.math
                        val fmt = { s: String -> DigitHelper.format(s, lang) }
                        HistorySummaryRowCard(
                            icon = Icons.Filled.Functions,
                            iconBg = ext.solverBlueContainer,
                            iconTint = ext.solverBlueStart,
                            title = "${fmt(m.expression)} = ${fmt(m.answer)}",
                            subtitle = "${strings.mathSolverTitle} • ${DigitHelper.formatDate(m.createdAt, lang)}",
                            eyeTestTag = "recent_math_eye_${m.id}",
                            onEyeClick = { onOpenMathDetail(m.id) }
                        )
                    }
                    is RecentHistoryItem.ResultItem -> {
                        val r = item.result.entity
                        val fmt = { s: String -> DigitHelper.format(s, lang) }
                        val statusText = if (r.isPassed) strings.passed else strings.failed
                        HistorySummaryRowCard(
                            icon = Icons.Filled.School,
                            iconBg = if (r.isPassed) ext.passContainer else ext.failContainer,
                            iconTint = if (r.isPassed) ext.passGreen else ext.failRed,
                            title = "${r.studentName} (${r.className} • ${strings.rollNumberLabel} ${fmt(r.rollNumber.toString())})",
                            subtitle = "${r.examName} • GPA ${fmt(String.format(java.util.Locale.US, "%.2f", r.totalGpa))} ($statusText)",
                            eyeTestTag = "recent_result_eye_${r.id}",
                            onEyeClick = { onOpenResultMarksheet(r.id) }
                        )
                    }
                }
            }
        }
    }

    if (showNotifications) {
        AlertDialog(
            onDismissRequest = { showNotifications = false },
            title = { Text(strings.notificationsTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.offlineReadyTip, style = MaterialTheme.typography.bodyMedium)
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = strings.notificationsEmpty,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotifications = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    tint: Color,
    bgColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 15.sp,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun HistorySummaryRowCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    eyeTestTag: String,
    onEyeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onEyeClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            EyeViewButton(
                contentDescription = "View Details",
                testTag = eyeTestTag,
                onClick = onEyeClick
            )
        }
    }
}
