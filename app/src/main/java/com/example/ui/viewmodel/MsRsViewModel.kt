package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppLanguage
import com.example.core.AppSettings
import com.example.core.BanglaStrings
import com.example.core.EnglishStrings
import com.example.core.SettingsDataStore
import com.example.data.CombinedStudentResult
import com.example.data.ComputedStudentResult
import com.example.data.MathHistoryEntity
import com.example.data.MathLayoutType
import com.example.data.MathSourceType
import com.example.data.MsRsDatabase
import com.example.data.MsRsRepository
import com.example.data.StudentResultEntity
import com.example.data.SubjectMark
import com.example.domain.BackupAndGitHubManager
import com.example.domain.GitHubReleaseInfo
import com.example.domain.MathSolveResult
import com.example.domain.MathSolverEngine
import com.example.domain.OfflineBackupEntry
import com.example.domain.ResultCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

sealed class RecentHistoryItem(val timestamp: Long) {
    data class MathItem(val math: MathHistoryEntity) : RecentHistoryItem(math.createdAt)
    data class ResultItem(val result: ComputedStudentResult) : RecentHistoryItem(result.entity.updatedAt)
}

data class GitHubUiState(
    val isLoading: Boolean = false,
    val releaseInfo: GitHubReleaseInfo? = null,
    val downloadProgress: Float = 0f,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class MsRsViewModel(application: Application) : AndroidViewModel(application) {

    private val json = Json { ignoreUnknownKeys = true }
    private val settingsStore = SettingsDataStore(application)
    private val repository = MsRsRepository(MsRsDatabase.getInstance(application).msRsDao())

    val settings: StateFlow<AppSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val allMathHistory: StateFlow<List<MathHistoryEntity>> = repository.allMathHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allResults: StateFlow<List<ComputedStudentResult>> = repository.allComputedResults
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCombinedResults: StateFlow<List<CombinedStudentResult>> = repository.allCombinedResults
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val combinedRecentHistory: StateFlow<List<RecentHistoryItem>> = combine(
        allMathHistory,
        allResults
    ) { maths, results ->
        val list = maths.map { RecentHistoryItem.MathItem(it) } +
            results.map { RecentHistoryItem.ResultItem(it) }
        list.sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _homeSearchQuery = MutableStateFlow("")
    val homeSearchQuery: StateFlow<String> = _homeSearchQuery.asStateFlow()

    private val _gitHubState = MutableStateFlow(GitHubUiState())
    val gitHubState: StateFlow<GitHubUiState> = _gitHubState.asStateFlow()

    private val _offlineSnapshots = MutableStateFlow<List<OfflineBackupEntry>>(emptyList())
    val offlineSnapshots: StateFlow<List<OfflineBackupEntry>> = _offlineSnapshots.asStateFlow()

    init {
        refreshOfflineSnapshots()
    }

    fun refreshOfflineSnapshots() {
        viewModelScope.launch {
            _offlineSnapshots.value = BackupAndGitHubManager.listOfflineSnapshots(getApplication())
        }
    }

    fun setHomeSearchQuery(q: String) {
        _homeSearchQuery.value = q
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val next = if (settings.value.language == AppLanguage.BANGLA) {
                AppLanguage.ENGLISH
            } else {
                AppLanguage.BANGLA
            }
            settingsStore.setLanguage(next)
        }
    }

    fun solveAndSaveMath(
        rawExpression: String,
        useBanglaDigits: Boolean,
        sourceType: MathSourceType,
        preferredLayout: MathLayoutType? = null,
        onSolved: (Long, MathSolveResult.Success) -> Unit,
        onError: (MathSolveResult.Error) -> Unit
    ) {
        val lang = settings.value.language
        val result = MathSolverEngine.solve(
            rawInput = rawExpression,
            useBanglaDigits = useBanglaDigits,
            language = lang,
            preferredLayout = preferredLayout
        )
        when (result) {
            is MathSolveResult.Error -> onError(result)
            is MathSolveResult.Success -> {
                viewModelScope.launch {
                    val entity = MathHistoryEntity(
                        expression = result.originalInput,
                        normalizedExpression = result.normalizedExpression,
                        answer = result.answerText,
                        khataLinesJson = json.encodeToString(result.khataLines),
                        explanationStepsJson = json.encodeToString(result.explanationSteps),
                        layoutType = result.layoutType.name,
                        sourceType = sourceType.name,
                        useBanglaDigits = useBanglaDigits
                    )
                    val id = repository.saveMath(entity)
                    onSolved(id, result)
                }
            }
        }
    }

    suspend fun getMathById(id: Long): MathHistoryEntity? = repository.getMathById(id)

    fun deleteMath(id: Long) {
        viewModelScope.launch { repository.deleteMath(id) }
    }

    suspend fun getResultById(id: Long): StudentResultEntity? = repository.getResultById(id)

    suspend fun getNextRollNumber(className: String, examName: String): Int {
        return repository.getNextRollNumber(className, examName)
    }

    suspend fun findStudentTemplateByClassAndRoll(className: String, rollNumber: Int): StudentResultEntity? {
        return repository.findStudentTemplateByClassAndRoll(className, rollNumber)
    }

    fun saveStudentResult(
        id: Long,
        examName: String,
        institutionName: String,
        className: String,
        yearSession: String,
        rollNumber: Int,
        studentName: String,
        fatherName: String,
        motherName: String,
        photoPath: String?,
        subjects: List<SubjectMark>,
        createdAt: Long,
        onSaved: (Long) -> Unit
    ) {
        viewModelScope.launch {
            settingsStore.updateResultDefaults(institutionName, examName)
            val entity = ResultCalculator.buildEntity(
                id = id,
                examName = examName,
                institutionName = institutionName,
                className = className,
                yearSession = yearSession,
                rollNumber = rollNumber,
                studentName = studentName,
                fatherName = fatherName,
                motherName = motherName,
                photoPath = photoPath,
                subjects = subjects,
                createdAt = createdAt
            )
            val savedId = repository.saveResult(entity)
            onSaved(savedId)
        }
    }

    fun deleteStudentResult(id: Long) {
        viewModelScope.launch { repository.deleteResult(id) }
    }

    fun copyPickedPhotoToInternalStorage(uri: Uri): String? {
        val ctx = getApplication<Application>()
        return runCatching {
            val dir = File(ctx.filesDir, "student_photos").apply { mkdirs() }
            val dest = File(dir, "photo_${System.currentTimeMillis()}.jpg")
            ctx.contentResolver.openInputStream(uri)?.use { inp ->
                FileOutputStream(dest).use { out -> inp.copyTo(out) }
            }
            dest.absolutePath
        }.getOrNull()
    }

    fun currentStrings() = if (settings.value.language == AppLanguage.BANGLA) BanglaStrings else EnglishStrings

    // Backup & Export
    fun exportBackupToUri(uri: Uri, onComplete: (Result<Pair<Int, Int>>) -> Unit) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val res = runCatching {
                val maths = repository.getAllMathSnapshot()
                val results = repository.getAllResultsSnapshot()
                val payload = BackupAndGitHubManager.buildBackupJson(ctx, maths, results)
                BackupAndGitHubManager.writeBackupToUri(ctx, uri, payload)
                Pair(maths.size, results.size)
            }
            onComplete(res)
        }
    }

    fun saveOfflineSnapshot(onComplete: (Result<OfflineBackupEntry>) -> Unit) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val res = runCatching {
                val maths = repository.getAllMathSnapshot()
                val results = repository.getAllResultsSnapshot()
                val entry = BackupAndGitHubManager.saveOfflineSnapshot(ctx, maths, results)
                _offlineSnapshots.value = BackupAndGitHubManager.listOfflineSnapshots(ctx)
                entry
            }
            onComplete(res)
        }
    }

    fun restoreOfflineSnapshot(
        fileName: String,
        replaceExisting: Boolean,
        onComplete: (Result<Pair<Int, Int>>) -> Unit
    ) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val res = runCatching {
                val (maths, results) = BackupAndGitHubManager.restoreOfflineSnapshot(ctx, fileName)
                repository.restoreBackupData(maths, results, replaceExisting = replaceExisting)
                Pair(maths.size, results.size)
            }
            onComplete(res)
        }
    }

    fun deleteOfflineSnapshot(fileName: String) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            BackupAndGitHubManager.deleteOfflineSnapshot(ctx, fileName)
            _offlineSnapshots.value = BackupAndGitHubManager.listOfflineSnapshots(ctx)
        }
    }

    fun restoreBackupFromUri(
        uri: Uri,
        replaceExisting: Boolean = false,
        onComplete: (Result<Pair<Int, Int>>) -> Unit
    ) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val res = runCatching {
                val (maths, results) = BackupAndGitHubManager.parseAndRestoreBackupFromUri(ctx, uri)
                repository.restoreBackupData(maths, results, replaceExisting = replaceExisting)
                Pair(maths.size, results.size)
            }
            onComplete(res)
        }
    }

    fun exportResultsCsvToUri(uri: Uri, onComplete: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val res = runCatching {
                val results = repository.getAllResultsSnapshot()
                BackupAndGitHubManager.writeResultsCsvToUri(ctx, uri, results)
                results.size
            }
            onComplete(res)
        }
    }

    fun exportMathCsvToUri(uri: Uri, onComplete: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val res = runCatching {
                val maths = repository.getAllMathSnapshot()
                BackupAndGitHubManager.writeMathCsvToUri(ctx, uri, maths)
                maths.size
            }
            onComplete(res)
        }
    }

    fun exportInstalledApkToUri(uri: Uri, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val res = runCatching {
                BackupAndGitHubManager.writeInstalledApkToUri(ctx, uri)
                Unit
            }
            onComplete(res)
        }
    }

    fun updateGitHubSettings(repo: String, token: String, profileUrl: String) {
        viewModelScope.launch {
            settingsStore.updateGitHubConfig(repo, token, profileUrl)
        }
    }

    fun checkGitHubRelease(repo: String = settings.value.githubRepo, token: String = settings.value.githubToken) {
        viewModelScope.launch {
            _gitHubState.value = GitHubUiState(isLoading = true)
            runCatching {
                BackupAndGitHubManager.fetchLatestGitHubRelease(repo, token)
            }.onSuccess { info ->
                _gitHubState.value = GitHubUiState(
                    isLoading = false,
                    releaseInfo = info,
                    statusMessage = "${info.releaseName} (${info.tagName})"
                )
            }.onFailure { err ->
                _gitHubState.value = GitHubUiState(
                    isLoading = false,
                    errorMessage = err.message ?: "GitHub error"
                )
            }
        }
    }

    fun downloadFromGitHubToUri(url: String, destUri: Uri, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val token = settings.value.githubToken
            _gitHubState.value = _gitHubState.value.copy(isLoading = true, downloadProgress = 0f, errorMessage = null)
            val res = runCatching {
                BackupAndGitHubManager.downloadFileToUri(ctx, url, token, destUri) { p ->
                    _gitHubState.value = _gitHubState.value.copy(downloadProgress = p)
                }
            }
            _gitHubState.value = _gitHubState.value.copy(
                isLoading = false,
                downloadProgress = if (res.isSuccess) 1f else 0f,
                errorMessage = res.exceptionOrNull()?.message
            )
            onComplete(res)
        }
    }

    fun uploadBackupToGitHub(repo: String, token: String, onComplete: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            _gitHubState.value = _gitHubState.value.copy(isLoading = true, errorMessage = null)
            val res = runCatching {
                val maths = repository.getAllMathSnapshot()
                val results = repository.getAllResultsSnapshot()
                val backupJson = BackupAndGitHubManager.buildBackupJson(ctx, maths, results)
                BackupAndGitHubManager.uploadBackupToGitHub(repo, token, backupJson)
            }
            _gitHubState.value = _gitHubState.value.copy(
                isLoading = false,
                statusMessage = res.getOrNull(),
                errorMessage = res.exceptionOrNull()?.message
            )
            onComplete(res)
        }
    }
}
