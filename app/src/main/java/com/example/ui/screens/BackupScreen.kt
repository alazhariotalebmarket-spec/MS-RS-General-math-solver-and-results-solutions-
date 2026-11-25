package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.AppSettings
import com.example.core.DigitHelper
import com.example.core.LocalLanguage
import com.example.core.LocalStrings
import com.example.domain.BackupAndGitHubManager
import com.example.domain.OfflineBackupEntry
import com.example.domain.PdfAndImageExporter
import com.example.ui.components.MsRsTopBar
import com.example.ui.theme.LocalMsRsColors
import com.example.ui.viewmodel.GitHubUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(
    settings: AppSettings,
    mathCount: Int,
    resultCount: Int,
    offlineSnapshots: List<OfflineBackupEntry>,
    gitHubState: GitHubUiState,
    onSaveOfflineSnapshot: () -> Unit,
    onRestoreOfflineSnapshot: (fileName: String, replaceExisting: Boolean) -> Unit,
    onDeleteOfflineSnapshot: (fileName: String) -> Unit,
    onExportBackup: (Uri) -> Unit,
    onRestoreBackup: (Uri, Boolean) -> Unit,
    onExportResultsCsv: (Uri) -> Unit,
    onExportMathCsv: (Uri) -> Unit,
    onSaveGitHubConfig: (repo: String, token: String, profileUrl: String) -> Unit,
    onCheckGitHubRelease: (repo: String, token: String) -> Unit,
    onDownloadGitHubFile: (url: String, destUri: Uri) -> Unit,
    onUploadBackupToGitHub: (repo: String, token: String) -> Unit,
    onOpenApkDownload: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val strings = LocalStrings.current
    val lang = LocalLanguage.current
    val ext = LocalMsRsColors.current
    val fmt = { s: String -> DigitHelper.format(s, lang) }

    var repoInput by remember(settings.githubRepo) { mutableStateOf(settings.githubRepo) }
    var tokenInput by remember(settings.githubToken) { mutableStateOf(settings.githubToken) }
    var pendingDownloadUrl by remember { mutableStateOf<String?>(null) }
    var replaceOnRestore by remember { mutableStateOf(false) }

    val stamp = remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date()) }

    val backupCreateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) onExportBackup(uri) }

    val restoreOpenLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onRestoreBackup(uri, replaceOnRestore) }

    val resultsCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> if (uri != null) onExportResultsCsv(uri) }

    val mathCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> if (uri != null) onExportMathCsv(uri) }

    val githubZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val url = pendingDownloadUrl
        if (uri != null && url != null) onDownloadGitHubFile(url, uri)
    }

    val githubApkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri ->
        val url = pendingDownloadUrl
        if (uri != null && url != null) onDownloadGitHubFile(url, uri)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MsRsTopBar(title = strings.backupCenterTitle)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Offline Badge + Stats Overview
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.OfflinePin, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = strings.offlineAdFreeBadge,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = fmt(mathCount.toString()),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(strings.mathHistoryTab, style = MaterialTheme.typography.labelMedium)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = fmt(resultCount.toString()),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(strings.resultsHistoryTab, style = MaterialTheme.typography.labelMedium)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = fmt(offlineSnapshots.size.toString()),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text("Snapshots", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // 1. Offline Backup Vault (100% Zero-Internet Instant Snapshots)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.FolderZip, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = strings.offlineVaultTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = strings.offlineVaultDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = onSaveOfflineSnapshot,
                            modifier = Modifier.fillMaxWidth().testTag("save_offline_snapshot_btn")
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.saveOfflineSnapshot, fontWeight = FontWeight.Bold)
                        }

                        // Merge vs Replace restore toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !replaceOnRestore,
                                onClick = { replaceOnRestore = false },
                                label = { Text(strings.restoreMergeMode, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = replaceOnRestore,
                                onClick = { replaceOnRestore = true },
                                label = { Text(strings.restoreReplaceMode, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (offlineSnapshots.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = strings.noOfflineSnapshots,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            offlineSnapshots.forEachIndexed { idx, snap ->
                                if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = DigitHelper.formatDate(snap.createdAt, lang),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${fmt(snap.mathCount.toString())} ${strings.mathHistoryTab} • ${fmt(snap.resultCount.toString())} ${strings.resultsHistoryTab} • ${fmt((snap.sizeBytes / 1024).coerceAtLeast(1).toString())} KB",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRestoreOfflineSnapshot(snap.fileName, replaceOnRestore) },
                                        modifier = Modifier.testTag("restore_snapshot_${snap.fileName}")
                                    ) {
                                        Icon(Icons.Filled.Restore, contentDescription = strings.restoreNow, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = {
                                            val f = BackupAndGitHubManager.getOfflineSnapshotFile(ctx, snap.fileName)
                                            PdfAndImageExporter.shareFile(ctx, f, "application/json", snap.fileName)
                                        }
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = strings.share, tint = ext.solverBlueStart)
                                    }
                                    IconButton(onClick = { onDeleteOfflineSnapshot(snap.fileName) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = strings.delete, tint = ext.failRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Portable JSON File Backup & Restore
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.localBackupCardTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.localBackupDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { backupCreateLauncher.launch("msrs_backup_$stamp.json") },
                                modifier = Modifier.weight(1f).testTag("backup_now_btn")
                            ) {
                                Icon(Icons.Filled.Backup, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(strings.backupNow, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { restoreOpenLauncher.launch(arrayOf("application/json", "*/*")) },
                                modifier = Modifier.weight(1f).testTag("restore_now_btn")
                            ) {
                                Icon(Icons.Filled.Restore, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(strings.restoreNow, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // 3. Excel / CSV Export Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.exportCsvTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { resultsCsvLauncher.launch("MSRS_Results_$stamp.csv") },
                                colors = ButtonDefaults.buttonColors(containerColor = ext.emeraldGradientStart),
                                modifier = Modifier.weight(1f).testTag("export_results_csv_btn")
                            ) {
                                Icon(Icons.Filled.TableView, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(strings.exportResultsCsv)
                            }
                            OutlinedButton(
                                onClick = { mathCsvLauncher.launch("MSRS_Math_$stamp.csv") },
                                modifier = Modifier.weight(1f).testTag("export_math_csv_btn")
                            ) {
                                Icon(Icons.Filled.TableView, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(strings.exportMathCsv)
                            }
                        }
                    }
                }
            }

            // 4. Offline APK Download Card
            item {
                Button(
                    onClick = onOpenApkDownload,
                    modifier = Modifier.fillMaxWidth().testTag("backup_apk_download_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ext.goldContainer, contentColor = ext.onGoldContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.apkDownload, fontWeight = FontWeight.Bold)
                }
            }

            // 5. GitHub Download / Update + Cloud Backup Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.githubSectionTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ext.solverBlueStart
                        )

                        Surface(
                            color = ext.solverBlueContainer.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = strings.githubGuideText,
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.onSolverBlueContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        OutlinedTextField(
                            value = repoInput,
                            onValueChange = { repoInput = it },
                            label = { Text(strings.githubRepoLabel) },
                            placeholder = { Text("https://github.com/username/ms-rs") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("github_repo_input")
                        )

                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            label = { Text(strings.githubTokenLabel) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("github_token_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onSaveGitHubConfig(repoInput, tokenInput, settings.githubProfileUrl)
                                    onShowMessage("GitHub config saved")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.saveSettings, maxLines = 1)
                            }
                            Button(
                                onClick = {
                                    onSaveGitHubConfig(repoInput, tokenInput, settings.githubProfileUrl)
                                    onCheckGitHubRelease(repoInput, tokenInput)
                                },
                                enabled = !gitHubState.isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = ext.solverBlueStart),
                                modifier = Modifier.weight(1.4f).testTag("github_download_update_btn")
                            ) {
                                Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.githubDownloadUpdate, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        Button(
                            onClick = {
                                onSaveGitHubConfig(repoInput, tokenInput, settings.githubProfileUrl)
                                onUploadBackupToGitHub(repoInput, tokenInput)
                            },
                            enabled = !gitHubState.isLoading,
                            modifier = Modifier.fillMaxWidth().testTag("github_upload_backup_btn")
                        ) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.githubUploadBackup)
                        }

                        if (gitHubState.isLoading) {
                            if (gitHubState.downloadProgress > 0f) {
                                LinearProgressIndicator(
                                    progress = { gitHubState.downloadProgress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }

                        gitHubState.errorMessage?.let { err ->
                            Surface(color = ext.failContainer, shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ext.failRed,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        gitHubState.releaseInfo?.let { rel ->
                            Surface(
                                color = ext.solverBlueContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${rel.repo} • ${rel.tagName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ext.onSolverBlueContainer
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (rel.apkDownloadUrl != null) {
                                            Button(
                                                onClick = {
                                                    pendingDownloadUrl = rel.apkDownloadUrl
                                                    githubApkLauncher.launch("MS_RS_${rel.tagName}.apk")
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(strings.githubReleaseApk)
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                pendingDownloadUrl = rel.sourceZipUrl
                                                githubZipLauncher.launch("MS_RS_${rel.tagName}.zip")
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(strings.githubSourceZip)
                                        }
                                    }
                                    TextButton(
                                        onClick = {
                                            runCatching {
                                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(rel.htmlUrl)))
                                            }
                                        },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text(strings.openInBrowser)
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
