package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.LocalStrings
import com.example.ui.components.IslamicGeometricBackdrop
import com.example.ui.components.MsRsLogoBadge
import com.example.ui.components.MsRsTopBar
import com.example.ui.theme.LocalMsRsColors

@Composable
fun AboutDeveloperScreen(
    onOpenApkDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val strings = LocalStrings.current
    val ext = LocalMsRsColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MsRsTopBar(title = strings.aboutDeveloperTitle)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // App Branding Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(ext.emeraldGradientStart, ext.emeraldGradientEnd)),
                            RoundedCornerShape(22.dp)
                        )
                ) {
                    IslamicGeometricBackdrop(
                        tint = Color.White.copy(alpha = 0.07f),
                        modifier = Modifier.matchParentSize()
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MsRsLogoBadge(size = 80.dp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "MS RS",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = strings.appTagline,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.goldBright,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = Color.White.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${strings.offlineAdFreeBadge} • v1.0",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Developer Contact Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoRow(Icons.Filled.Person, strings.developerLabel, strings.developerName)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        InfoRow(Icons.Filled.Phone, strings.mobileLabel, strings.developerMobile)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        InfoRow(Icons.Filled.Email, strings.emailLabel, strings.developerEmail1)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        InfoRow(Icons.Filled.Email, strings.email2Label, strings.developerEmail2)

                        Spacer(Modifier.height(4.dp))

                        // Call, WhatsApp, Email action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    runCatching {
                                        ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:01934411199")))
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("dev_call_btn")
                            ) {
                                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.callBtn, maxLines = 1)
                            }

                            Button(
                                onClick = {
                                    runCatching {
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801934411199")))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ext.passGreen),
                                modifier = Modifier.weight(1.1f).testTag("dev_whatsapp_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.whatsappBtn, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:ferdausharfin@gmail.com")
                                            putExtra(Intent.EXTRA_EMAIL, arrayOf("ferdausharfin@gmail.com", "ferdausharfin@yahoo.com"))
                                            putExtra(Intent.EXTRA_SUBJECT, "MS RS App Feedback")
                                        }
                                        ctx.startActivity(intent)
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("dev_email_btn")
                            ) {
                                Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.emailBtn, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Offline APK Download Button
            item {
                Button(
                    onClick = onOpenApkDownload,
                    modifier = Modifier.fillMaxWidth().testTag("about_apk_download_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ext.goldContainer, contentColor = ext.onGoldContainer)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.apkDownload, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}
