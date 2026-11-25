package com.example.domain

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.data.MathHistoryEntity
import com.example.data.StudentResultEntity
import com.example.data.SubjectMark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Serializable
data class BackupPhotoEntry(
    val fileName: String,
    val base64: String
)

@Serializable
data class MsRsBackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val mathHistory: List<MathHistoryEntity> = emptyList(),
    val studentResults: List<StudentResultEntity> = emptyList(),
    val encodedPhotos: List<BackupPhotoEntry> = emptyList()
)

data class OfflineBackupEntry(
    val fileName: String,
    val createdAt: Long,
    val mathCount: Int,
    val resultCount: Int,
    val sizeBytes: Long
)

data class GitHubReleaseInfo(
    val repo: String,
    val tagName: String,
    val releaseName: String,
    val apkDownloadUrl: String?,
    val sourceZipUrl: String,
    val htmlUrl: String
)

object BackupAndGitHubManager {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun vaultDir(context: Context): File {
        return File(context.filesDir, "offline_backups").apply { mkdirs() }
    }

    suspend fun buildBackupJson(
        context: Context,
        maths: List<MathHistoryEntity>,
        results: List<StudentResultEntity>
    ): String = withContext(Dispatchers.IO) {
        val photos = mutableListOf<BackupPhotoEntry>()
        val normalizedResults = results.map { res ->
            val path = res.photoPath
            if (!path.isNullOrBlank()) {
                val f = File(path)
                if (f.exists() && f.length() < 1_500_000) {
                    val bytes = f.readBytes()
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    photos += BackupPhotoEntry(fileName = f.name, base64 = b64)
                    res.copy(photoPath = f.name)
                } else res
            } else res
        }
        val payload = MsRsBackupPayload(
            mathHistory = maths,
            studentResults = normalizedResults,
            encodedPhotos = photos
        )
        json.encodeToString(payload)
    }

    suspend fun saveOfflineSnapshot(
        context: Context,
        maths: List<MathHistoryEntity>,
        results: List<StudentResultEntity>
    ): OfflineBackupEntry = withContext(Dispatchers.IO) {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "msrs_offline_$stamp.json"
        val jsonText = buildBackupJson(context, maths, results)
        val outFile = File(vaultDir(context), fileName)
        outFile.writeText(jsonText, Charsets.UTF_8)
        OfflineBackupEntry(
            fileName = fileName,
            createdAt = outFile.lastModified(),
            mathCount = maths.size,
            resultCount = results.size,
            sizeBytes = outFile.length()
        )
    }

    suspend fun listOfflineSnapshots(context: Context): List<OfflineBackupEntry> = withContext(Dispatchers.IO) {
        val dir = vaultDir(context)
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: emptyArray()
        files.sortedByDescending { it.lastModified() }.mapNotNull { file ->
            runCatching {
                val payload = json.decodeFromString<MsRsBackupPayload>(file.readText(Charsets.UTF_8))
                OfflineBackupEntry(
                    fileName = file.name,
                    createdAt = if (payload.exportedAt > 0L) payload.exportedAt else file.lastModified(),
                    mathCount = payload.mathHistory.size,
                    resultCount = payload.studentResults.size,
                    sizeBytes = file.length()
                )
            }.getOrNull()
        }
    }

    suspend fun restoreOfflineSnapshot(
        context: Context,
        fileName: String
    ): Pair<List<MathHistoryEntity>, List<StudentResultEntity>> = withContext(Dispatchers.IO) {
        val file = File(vaultDir(context), fileName)
        require(file.exists()) { "Snapshot not found" }
        parseBackupRaw(context, file.readText(Charsets.UTF_8))
    }

    suspend fun deleteOfflineSnapshot(context: Context, fileName: String) = withContext(Dispatchers.IO) {
        File(vaultDir(context), fileName).delete()
    }

    fun getOfflineSnapshotFile(context: Context, fileName: String): File {
        return File(vaultDir(context), fileName)
    }

    suspend fun writeBackupToUri(context: Context, uri: Uri, backupJson: String) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(backupJson.toByteArray(Charsets.UTF_8))
        }
    }

    suspend fun parseAndRestoreBackupFromUri(
        context: Context,
        uri: Uri
    ): Pair<List<MathHistoryEntity>, List<StudentResultEntity>> = withContext(Dispatchers.IO) {
        val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
            it.readText()
        } ?: throw IllegalStateException("Unable to read backup file")
        parseBackupRaw(context, raw)
    }

    private fun parseBackupRaw(
        context: Context,
        raw: String
    ): Pair<List<MathHistoryEntity>, List<StudentResultEntity>> {
        val payload = json.decodeFromString<MsRsBackupPayload>(raw)
        val photoDir = File(context.filesDir, "student_photos").apply { mkdirs() }
        val restoredPaths = mutableMapOf<String, String>()
        for (p in payload.encodedPhotos) {
            runCatching {
                val bytes = Base64.decode(p.base64, Base64.DEFAULT)
                val dest = File(photoDir, p.fileName)
                dest.writeBytes(bytes)
                restoredPaths[p.fileName] = dest.absolutePath
            }
        }
        val restoredResults = payload.studentResults.map { res ->
            val mapped = res.photoPath?.let { restoredPaths[it] ?: it }
            res.copy(photoPath = mapped)
        }
        return Pair(payload.mathHistory, restoredResults)
    }

    suspend fun writeResultsCsvToUri(
        context: Context,
        uri: Uri,
        results: List<StudentResultEntity>
    ) = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append("Roll,Student Name,Class,Exam,Year,Father Name,Mother Name,Total Full,Obtained Total,Percentage,GPA,Grade,Status,Subjects Breakdown\n")
        for (r in results) {
            val subs = runCatching { json.decodeFromString<List<SubjectMark>>(r.subjectsJson) }.getOrDefault(emptyList())
            val breakdown = subs.filter { it.isEnabled }.joinToString(" | ") {
                "${it.name}: ${ResultCalculator.formatMarks(it.obtainedMarks)}/${it.fullMarks}(${it.grade})"
            }
            sb.append(
                listOf(
                    r.rollNumber.toString(),
                    csvEscape(r.studentName),
                    csvEscape(r.className),
                    csvEscape(r.examName),
                    csvEscape(r.yearSession),
                    csvEscape(r.fatherName),
                    csvEscape(r.motherName),
                    r.totalFullMarks.toString(),
                    ResultCalculator.formatMarks(r.totalObtainedMarks),
                    String.format(Locale.US, "%.2f", r.percentage),
                    String.format(Locale.US, "%.2f", r.totalGpa),
                    csvEscape(r.overallGrade),
                    if (r.isPassed) "PASS" else "FAIL",
                    csvEscape(breakdown)
                ).joinToString(",")
            ).append("\n")
        }
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(sb.toString().toByteArray(Charsets.UTF_8))
        }
    }

    suspend fun writeMathCsvToUri(
        context: Context,
        uri: Uri,
        maths: List<MathHistoryEntity>
    ) = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append("ID,Expression,Answer,Layout,Source,Timestamp\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        for (m in maths) {
            sb.append(
                listOf(
                    m.id.toString(),
                    csvEscape(m.expression),
                    csvEscape(m.answer),
                    csvEscape(m.layoutType),
                    csvEscape(m.sourceType),
                    csvEscape(sdf.format(Date(m.createdAt)))
                ).joinToString(",")
            ).append("\n")
        }
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(sb.toString().toByteArray(Charsets.UTF_8))
        }
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    suspend fun prepareInstalledApkInCache(context: Context): File = withContext(Dispatchers.IO) {
        val sourceApk = File(context.applicationInfo.sourceDir)
        val outApk = File(context.cacheDir, "MS_RS_Offline.apk")
        FileInputStream(sourceApk).use { inp ->
            FileOutputStream(outApk).use { out ->
                inp.copyTo(out)
            }
        }
        outApk
    }

    suspend fun writeInstalledApkToUri(context: Context, destUri: Uri) = withContext(Dispatchers.IO) {
        val sourceApk = File(context.applicationInfo.sourceDir)
        context.contentResolver.openOutputStream(destUri)?.use { out ->
            FileInputStream(sourceApk).use { inp ->
                inp.copyTo(out)
            }
        }
    }

    fun extractOwnerRepo(repoInput: String): String {
        var clean = repoInput.trim()
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removePrefix("github.com/")
            .removeSuffix(".git")
            .trim('/')
        val parts = clean.split('/').filter { it.isNotBlank() }
        require(parts.size >= 2) { "Use owner/repo or full GitHub URL" }
        return "${parts[0]}/${parts[1]}"
    }

    suspend fun fetchLatestGitHubRelease(repoInput: String, token: String): GitHubReleaseInfo =
        withContext(Dispatchers.IO) {
            val cleanRepo = extractOwnerRepo(repoInput)

            val relReqBuilder = Request.Builder()
                .url("https://api.github.com/repos/$cleanRepo/releases/latest")
                .header("Accept", "application/vnd.github+json")
            if (token.isNotBlank()) relReqBuilder.header("Authorization", "Bearer $token")

            val relResp = client.newCall(relReqBuilder.build()).execute()
            if (relResp.isSuccessful) {
                val body = relResp.body?.string() ?: "{}"
                val obj = JSONObject(body)
                val tag = obj.optString("tag_name", "latest")
                val name = obj.optString("name", tag)
                val htmlUrl = obj.optString("html_url", "https://github.com/$cleanRepo/releases")
                var apkUrl: String? = null
                val assets = obj.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        val aName = a.optString("name")
                        if (aName.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = a.optString("browser_download_url")
                            break
                        }
                    }
                }
                return@withContext GitHubReleaseInfo(
                    repo = cleanRepo,
                    tagName = tag,
                    releaseName = name,
                    apkDownloadUrl = apkUrl,
                    sourceZipUrl = "https://github.com/$cleanRepo/archive/refs/heads/main.zip",
                    htmlUrl = htmlUrl
                )
            }

            val repoReqBuilder = Request.Builder()
                .url("https://api.github.com/repos/$cleanRepo")
                .header("Accept", "application/vnd.github+json")
            if (token.isNotBlank()) repoReqBuilder.header("Authorization", "Bearer $token")

            val repoResp = client.newCall(repoReqBuilder.build()).execute()
            if (!repoResp.isSuccessful) {
                throw IllegalStateException("GitHub HTTP ${repoResp.code} — repo not found or private")
            }
            val repoObj = JSONObject(repoResp.body?.string() ?: "{}")
            val defaultBranch = repoObj.optString("default_branch", "main")
            GitHubReleaseInfo(
                repo = cleanRepo,
                tagName = defaultBranch,
                releaseName = repoObj.optString("full_name", cleanRepo),
                apkDownloadUrl = null,
                sourceZipUrl = "https://github.com/$cleanRepo/archive/refs/heads/$defaultBranch.zip",
                htmlUrl = repoObj.optString("html_url", "https://github.com/$cleanRepo")
            )
        }

    suspend fun downloadFileToUri(
        context: Context,
        downloadUrl: String,
        token: String,
        destUri: Uri,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val reqBuilder = Request.Builder().url(downloadUrl)
        if (token.isNotBlank()) reqBuilder.header("Authorization", "Bearer $token")
        val resp = client.newCall(reqBuilder.build()).execute()
        if (!resp.isSuccessful) {
            throw IllegalStateException("Download failed: HTTP ${resp.code}")
        }
        val body = resp.body ?: throw IllegalStateException("Empty response body")
        val total = body.contentLength()
        context.contentResolver.openOutputStream(destUri)?.use { out ->
            body.byteStream().use { inp ->
                val buf = ByteArray(8192)
                var read: Int
                var copied = 0L
                while (inp.read(buf).also { read = it } != -1) {
                    out.write(buf, 0, read)
                    copied += read
                    if (total > 0) {
                        onProgress((copied.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                    }
                }
            }
        }
        onProgress(1f)
    }

    suspend fun uploadBackupToGitHub(
        repoInput: String,
        token: String,
        backupJson: String
    ): String = withContext(Dispatchers.IO) {
        require(token.isNotBlank()) { "GitHub Personal Access Token required to upload backup" }
        val cleanRepo = extractOwnerRepo(repoInput)

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val path = "backups/msrs_backup_$stamp.json"
        val b64Content = Base64.encodeToString(backupJson.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

        val jsonBody = JSONObject().apply {
            put("message", "MS RS Backup $stamp")
            put("content", b64Content)
        }.toString()

        val req = Request.Builder()
            .url("https://api.github.com/repos/$cleanRepo/contents/$path")
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer $token")
            .put(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) {
            val errMsg = resp.body?.string() ?: "HTTP ${resp.code}"
            throw IllegalStateException("Upload failed (${resp.code}): $errMsg")
        }
        path
    }
}
