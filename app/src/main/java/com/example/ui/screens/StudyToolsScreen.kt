package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.DigitHelper
import com.example.core.LocalLanguage
import com.example.core.LocalStrings
import com.example.ui.components.MsRsTopBar
import com.example.ui.theme.LocalMsRsColors

@Composable
fun StudyToolsScreen(
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val lang = LocalLanguage.current
    val ext = LocalMsRsColors.current
    val fmt = { s: String -> DigitHelper.format(s, lang) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedTable by remember { mutableIntStateOf(2) }
    var highlightedRow by remember { mutableIntStateOf(-1) }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.statusBarsPadding()) {
                MsRsTopBar(title = strings.studyToolsTitle, onBack = onBack)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(strings.timesTableTab, maxLines = 1) },
                    modifier = Modifier.testTag("tools_tab_namta")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(strings.gpaChartTab, maxLines = 1) },
                    modifier = Modifier.testTag("tools_tab_gpa")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(strings.formulasTab, maxLines = 1) },
                    modifier = Modifier.testTag("tools_tab_formulas")
                )
            }

            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = strings.selectTableNumber,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                (1..20).forEach { n ->
                                    FilterChip(
                                        selected = selectedTable == n,
                                        onClick = {
                                            selectedTable = n
                                            highlightedRow = -1
                                        },
                                        label = { Text(fmt(n.toString()), fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }

                        items((1..10).toList()) { i ->
                            val isSelected = highlightedRow == i
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { highlightedRow = if (isSelected) -1 else i }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${fmt(selectedTable.toString())} × ${fmt(i.toString())}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "= ${fmt((selectedTable * i).toString())}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    val scale = listOf(
                        Triple("80 - 100%", "A+", "5.00"),
                        Triple("70 - 79%", "A", "4.00"),
                        Triple("60 - 69%", "A-", "3.50"),
                        Triple("50 - 59%", "B", "3.00"),
                        Triple("40 - 49%", "C", "2.00"),
                        Triple("33 - 39%", "D", "1.00"),
                        Triple("0 - 32%", "F", "0.00")
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(strings.percentage, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                                    Text(strings.gradeLabel, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                    Text(strings.gpaLabel, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        items(scale) { (range, grade, gpa) ->
                            val isFail = grade == "F"
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isFail) ext.failContainer else MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(fmt(range), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1.2f))
                                    Text(
                                        grade,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isFail) ext.failRed else ext.passGreen,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(fmt(gpa), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                else -> {
                    val formulas = if (lang == AppLanguage.BANGLA) {
                        listOf(
                            "সরল অংকের নিয়ম (BODMAS)" to "প্রথমে বন্ধনী (), এরপর ভাগ (÷), তারপর গুণ (×), শেষে যোগ (+) ও বিয়োগ (-)।",
                            "গড় (Average)" to "গড় = রাশিগুলোর যোগফল ÷ রাশিগুলোর সংখ্যা",
                            "শতকরা (Percentage)" to "শতকরা হার = (প্রাপ্ত নম্বর ÷ পূর্ণমান) × ১০০%",
                            "লাভ ও ক্ষতি" to "লাভ = বিক্রয়মূল্য − ক্রয়মূল্য  |  ক্ষতি = ক্রয়মূল্য − বিক্রয়মূল্য",
                            "আয়তক্ষেত্রের ক্ষেত্রফল" to "ক্ষেত্রফল = দৈর্ঘ্য × প্রস্থ  |  পরিসীমা = ২ × (দৈর্ঘ্য + প্রস্থ)",
                            "বর্গক্ষেত্রের ক্ষেত্রফল" to "ক্ষেত্রফল = (বাহু)²  |  পরিসীমা = ৪ × বাহু",
                            "ত্রিভুজের ক্ষেত্রফল" to "ক্ষেত্রফল = (১ ÷ ২) × ভূমি × উচ্চতা",
                            "ভাগ যাচাই করার সূত্র" to "ভাজ্য = (ভাজক × ভাগফল) + ভাগশেষ"
                        )
                    } else {
                        listOf(
                            "Order of Operations (BODMAS)" to "Brackets () -> Division (÷) -> Multiplication (×) -> Addition (+) -> Subtraction (-).",
                            "Average" to "Average = Sum of Values ÷ Number of Values",
                            "Percentage" to "Percentage = (Obtained Marks ÷ Total Marks) × 100%",
                            "Profit & Loss" to "Profit = Selling Price − Cost Price  |  Loss = Cost Price − Selling Price",
                            "Rectangle Area" to "Area = Length × Width  |  Perimeter = 2 × (Length + Width)",
                            "Square Area" to "Area = Side²  |  Perimeter = 4 × Side",
                            "Triangle Area" to "Area = ½ × Base × Height",
                            "Division Verification" to "Dividend = (Divisor × Quotient) + Remainder"
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(formulas) { (title, body) ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    )
                                    Text(
                                        text = body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
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
