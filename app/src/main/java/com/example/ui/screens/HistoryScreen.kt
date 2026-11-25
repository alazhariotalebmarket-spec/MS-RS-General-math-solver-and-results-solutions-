package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.DigitHelper
import com.example.core.LocalLanguage
import com.example.core.LocalStrings
import com.example.ui.components.EyeViewButton
import com.example.ui.components.MsRsTopBar
import com.example.ui.theme.LocalMsRsColors
import com.example.ui.viewmodel.RecentHistoryItem

private enum class HistoryTabFilter { ALL, MATH, RESULTS }

@Composable
fun HistoryScreen(
    items: List<RecentHistoryItem>,
    onOpenMathDetail: (Long) -> Unit,
    onDeleteMath: (Long) -> Unit,
    onOpenResultMarksheet: (Long) -> Unit,
    onDeleteResult: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val lang = LocalLanguage.current
    val ext = LocalMsRsColors.current
    val fmt = { s: String -> DigitHelper.format(s, lang) }

    var tabFilter by remember { mutableStateOf(HistoryTabFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(items, tabFilter, searchQuery) {
        val q = searchQuery.trim().lowercase()
        val engQ = DigitHelper.toEnglishDigits(q)
        items.filter { item ->
            val matchesTab = when (tabFilter) {
                HistoryTabFilter.ALL -> true
                HistoryTabFilter.MATH -> item is RecentHistoryItem.MathItem
                HistoryTabFilter.RESULTS -> item is RecentHistoryItem.ResultItem
            }
            val matchesQuery = if (q.isEmpty()) true else when (item) {
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
            matchesTab && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MsRsTopBar(title = strings.navHistory)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchPlaceholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input")
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = tabFilter == HistoryTabFilter.ALL,
                        onClick = { tabFilter = HistoryTabFilter.ALL },
                        label = { Text(strings.allHistoryTab) },
                        modifier = Modifier.testTag("history_tab_all")
                    )
                    FilterChip(
                        selected = tabFilter == HistoryTabFilter.MATH,
                        onClick = { tabFilter = HistoryTabFilter.MATH },
                        label = { Text(strings.mathHistoryTab) },
                        modifier = Modifier.testTag("history_tab_math")
                    )
                    FilterChip(
                        selected = tabFilter == HistoryTabFilter.RESULTS,
                        onClick = { tabFilter = HistoryTabFilter.RESULTS },
                        label = { Text(strings.resultsHistoryTab) },
                        modifier = Modifier.testTag("history_tab_results")
                    )
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(strings.noHistoryYet, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                items(
                    items = filtered,
                    key = {
                        when (it) {
                            is RecentHistoryItem.MathItem -> "h_math_${it.math.id}"
                            is RecentHistoryItem.ResultItem -> "h_res_${it.result.entity.id}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is RecentHistoryItem.MathItem -> {
                            val m = item.math
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenMathDetail(m.id) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ext.solverBlueContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Functions, contentDescription = null, tint = ext.solverBlueStart)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${fmt(m.expression)} = ${fmt(m.answer)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = DigitHelper.formatDate(m.createdAt, lang),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onDeleteMath(m.id) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = strings.delete, tint = ext.failRed)
                                    }
                                    EyeViewButton(
                                        contentDescription = strings.viewSolution,
                                        testTag = "history_math_eye_${m.id}",
                                        onClick = { onOpenMathDetail(m.id) }
                                    )
                                }
                            }
                        }

                        is RecentHistoryItem.ResultItem -> {
                            val r = item.result.entity
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenResultMarksheet(r.id) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (r.isPassed) ext.passContainer else ext.failContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.School,
                                            contentDescription = null,
                                            tint = if (r.isPassed) ext.passGreen else ext.failRed
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${r.studentName} (${r.className} • ${strings.rollNumberLabel} ${fmt(r.rollNumber.toString())})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${r.examName} • GPA ${fmt(String.format(java.util.Locale.US, "%.2f", r.totalGpa))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(onClick = { onDeleteResult(r.id) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = strings.delete, tint = ext.failRed)
                                    }
                                    EyeViewButton(
                                        contentDescription = strings.viewMarksheet,
                                        testTag = "history_result_eye_${r.id}",
                                        onClick = { onOpenResultMarksheet(r.id) }
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
