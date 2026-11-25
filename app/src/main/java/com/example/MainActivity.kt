package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.core.AppLanguage
import com.example.core.BanglaStrings
import com.example.core.EnglishStrings
import com.example.core.LocalLanguage
import com.example.core.LocalStrings
import com.example.ui.components.ApkDownloadDialog
import com.example.ui.navigation.BottomTab
import com.example.ui.navigation.CalculatorSolverRoute
import com.example.ui.navigation.CameraSolverRoute
import com.example.ui.navigation.MainTabsRoute
import com.example.ui.navigation.MathSolutionRoute
import com.example.ui.navigation.ResultFormRoute
import com.example.ui.navigation.ResultListRoute
import com.example.ui.navigation.ResultMarksheetRoute
import com.example.ui.navigation.SplashRoute
import com.example.ui.navigation.StudyToolsRoute
import com.example.ui.screens.AboutDeveloperScreen
import com.example.ui.screens.BackupScreen
import com.example.ui.screens.CalculatorSolverScreen
import com.example.ui.screens.CameraSolverScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MathSolutionScreen
import com.example.ui.screens.ResultFormScreen
import com.example.ui.screens.ResultListScreen
import com.example.ui.screens.ResultMarksheetScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StudyToolsScreen
import com.example.ui.theme.LocalMsRsColors
import com.example.ui.theme.MsRsTheme
import com.example.ui.viewmodel.MsRsViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MsRsViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val strings = if (settings.language == AppLanguage.BANGLA) BanglaStrings else EnglishStrings

            CompositionLocalProvider(
                LocalLanguage provides settings.language,
                LocalStrings provides strings
            ) {
                MsRsTheme {
                    MsRsAppRoot(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun MsRsAppRoot(viewModel: MsRsViewModel) {
    val navController = rememberNavController()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allMaths by viewModel.allMathHistory.collectAsStateWithLifecycle()
    val allResults by viewModel.allResults.collectAsStateWithLifecycle()
    val recentHistory by viewModel.combinedRecentHistory.collectAsStateWithLifecycle()
    val homeSearch by viewModel.homeSearchQuery.collectAsStateWithLifecycle()
    val gitHubState by viewModel.gitHubState.collectAsStateWithLifecycle()
    val offlineSnapshots by viewModel.offlineSnapshots.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = SplashRoute,
        enterTransition = { slideInHorizontally(tween(260)) { it / 4 } + fadeIn(tween(260)) },
        exitTransition = { slideOutHorizontally(tween(220)) { -it / 6 } + fadeOut(tween(220)) },
        popEnterTransition = { slideInHorizontally(tween(240)) { -it / 6 } + fadeIn(tween(240)) },
        popExitTransition = { slideOutHorizontally(tween(240)) { it / 4 } + fadeOut(tween(240)) }
    ) {
        composable<SplashRoute> {
            SplashScreen(
                onFinished = {
                    navController.navigate(MainTabsRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<MainTabsRoute> {
            MainTabsShell(
                viewModel = viewModel,
                settings = settings,
                mathCount = allMaths.size,
                resultCount = allResults.size,
                recentHistory = recentHistory,
                homeSearch = homeSearch,
                offlineSnapshots = offlineSnapshots,
                gitHubState = gitHubState,
                onOpenCameraSolver = { navController.navigate(CameraSolverRoute) },
                onOpenCalculatorSolver = { navController.navigate(CalculatorSolverRoute) },
                onOpenResultsList = { navController.navigate(ResultListRoute) },
                onOpenNewResult = { navController.navigate(ResultFormRoute(0L)) },
                onOpenMathDetail = { id -> navController.navigate(MathSolutionRoute(id)) },
                onOpenResultMarksheet = { id -> navController.navigate(ResultMarksheetRoute(id)) },
                onOpenStudyTools = { navController.navigate(StudyToolsRoute) }
            )
        }

        composable<CameraSolverRoute> {
            CameraSolverScreen(
                onBack = { navController.popBackStack() },
                onOpenCalculator = { navController.navigate(CalculatorSolverRoute) },
                onOpenSolution = { id -> navController.navigate(MathSolutionRoute(id)) },
                onSolveExpression = { raw, useBangla, source, layout, onSolved, onError ->
                    viewModel.solveAndSaveMath(raw, useBangla, source, layout, onSolved, onError)
                }
            )
        }

        composable<CalculatorSolverRoute> {
            CalculatorSolverScreen(
                onBack = { navController.popBackStack() },
                onOpenCamera = { navController.navigate(CameraSolverRoute) },
                onOpenSolution = { id -> navController.navigate(MathSolutionRoute(id)) },
                onSolveExpression = { raw, useBangla, source, layout, onSolved, onError ->
                    viewModel.solveAndSaveMath(raw, useBangla, source, layout, onSolved, onError)
                }
            )
        }

        composable<MathSolutionRoute> { entry ->
            val route = entry.toRoute<MathSolutionRoute>()
            MathSolutionScreen(
                mathId = route.mathId,
                loadMath = { viewModel.getMathById(it) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<ResultListRoute> {
            ResultListScreen(
                results = allResults,
                onBack = { navController.popBackStack() },
                onCreateResult = { navController.navigate(ResultFormRoute(0L)) },
                onOpenMarksheet = { id -> navController.navigate(ResultMarksheetRoute(id)) }
            )
        }

        composable<ResultFormRoute> { entry ->
            val route = entry.toRoute<ResultFormRoute>()
            ResultFormScreen(
                resultId = route.resultId,
                defaultInstitution = settings.defaultInstitution,
                defaultExamName = settings.defaultExamName,
                loadResult = { viewModel.getResultById(it) },
                getNextRoll = { cls, exam -> viewModel.getNextRollNumber(cls, exam) },
                copyPhoto = { uri -> viewModel.copyPickedPhotoToInternalStorage(uri) },
                onSave = { id, exam, inst, cls, year, roll, sName, fName, mName, photo, subs, created ->
                    viewModel.saveStudentResult(
                        id, exam, inst, cls, year, roll, sName, fName, mName, photo, subs, created
                    ) { savedId ->
                        navController.navigate(ResultMarksheetRoute(savedId)) {
                            popUpTo(ResultFormRoute(route.resultId)) { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<ResultMarksheetRoute> { entry ->
            val route = entry.toRoute<ResultMarksheetRoute>()
            ResultMarksheetScreen(
                resultId = route.resultId,
                allResults = allResults,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(ResultFormRoute(id)) },
                onDelete = { id ->
                    viewModel.deleteStudentResult(id)
                    navController.popBackStack()
                }
            )
        }

        composable<StudyToolsRoute> {
            StudyToolsScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun MainTabsShell(
    viewModel: MsRsViewModel,
    settings: com.example.core.AppSettings,
    mathCount: Int,
    resultCount: Int,
    recentHistory: List<com.example.ui.viewmodel.RecentHistoryItem>,
    homeSearch: String,
    offlineSnapshots: List<com.example.domain.OfflineBackupEntry>,
    gitHubState: com.example.ui.viewmodel.GitHubUiState,
    onOpenCameraSolver: () -> Unit,
    onOpenCalculatorSolver: () -> Unit,
    onOpenResultsList: () -> Unit,
    onOpenNewResult: () -> Unit,
    onOpenMathDetail: (Long) -> Unit,
    onOpenResultMarksheet: (Long) -> Unit,
    onOpenStudyTools: () -> Unit
) {
    val strings = LocalStrings.current
    val ext = LocalMsRsColors.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by rememberSaveable { mutableStateOf(BottomTab.HOME) }
    var showCreateMenu by remember { mutableStateOf(false) }
    var showApkDialog by remember { mutableStateOf(false) }

    if (currentTab != BottomTab.HOME) {
        BackHandler {
            currentTab = BottomTab.HOME
        }
    }

    val navItems = listOf(
        Triple(BottomTab.HOME, strings.navHome, Pair(Icons.Filled.Home, Icons.Outlined.Home)),
        Triple(BottomTab.HISTORY, strings.navHistory, Pair(Icons.Filled.History, Icons.Outlined.History)),
        Triple(BottomTab.BACKUP, strings.navBackup, Pair(Icons.Filled.Backup, Icons.Outlined.Backup)),
        Triple(BottomTab.ABOUT, strings.navAbout, Pair(Icons.Filled.Info, Icons.Outlined.Info))
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.navigationBars,
            floatingActionButton = {
                if (currentTab == BottomTab.HOME || currentTab == BottomTab.HISTORY) {
                    FloatingActionButton(
                        onClick = { showCreateMenu = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("main_plus_fab")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = strings.addNewTitle)
                    }
                }
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        navItems.forEach { (tab, label, icons) ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) icons.first else icons.second,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                modifier = Modifier.testTag("bottom_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(Modifier.height(12.dp))
                        navItems.forEach { (tab, label, icons) ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) icons.first else icons.second,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(180)) },
                        label = "tabSwitch"
                    ) { tab ->
                        when (tab) {
                            BottomTab.HOME -> {
                                HomeScreen(
                                    searchQuery = homeSearch,
                                    recentHistory = recentHistory,
                                    onSearchQueryChange = viewModel::setHomeSearchQuery,
                                    onToggleLanguage = viewModel::toggleLanguage,
                                    onOpenCameraSolver = onOpenCameraSolver,
                                    onOpenCalculatorSolver = onOpenCalculatorSolver,
                                    onOpenResultsList = onOpenResultsList,
                                    onOpenNewResult = onOpenNewResult,
                                    onOpenMathDetail = onOpenMathDetail,
                                    onOpenResultMarksheet = onOpenResultMarksheet,
                                    onQuickPdfDownload = {
                                        val firstItem = recentHistory.firstOrNull()
                                        when (firstItem) {
                                            is com.example.ui.viewmodel.RecentHistoryItem.ResultItem ->
                                                onOpenResultMarksheet(firstItem.result.entity.id)
                                            is com.example.ui.viewmodel.RecentHistoryItem.MathItem ->
                                                onOpenMathDetail(firstItem.math.id)
                                            null -> onOpenResultsList()
                                        }
                                    },
                                    onOpenApkDownload = { showApkDialog = true },
                                    onOpenStudyTools = onOpenStudyTools
                                )
                            }

                            BottomTab.HISTORY -> {
                                HistoryScreen(
                                    items = recentHistory,
                                    onOpenMathDetail = onOpenMathDetail,
                                    onDeleteMath = viewModel::deleteMath,
                                    onOpenResultMarksheet = onOpenResultMarksheet,
                                    onDeleteResult = viewModel::deleteStudentResult
                                )
                            }

                            BottomTab.BACKUP -> {
                                BackupScreen(
                                    settings = settings,
                                    mathCount = mathCount,
                                    resultCount = resultCount,
                                    offlineSnapshots = offlineSnapshots,
                                    gitHubState = gitHubState,
                                    onSaveOfflineSnapshot = {
                                        viewModel.saveOfflineSnapshot { res ->
                                            scope.launch {
                                                res.onSuccess { snap ->
                                                    snackbarHostState.showSnackbar("Offline snapshot saved (${snap.mathCount} maths, ${snap.resultCount} results)")
                                                }.onFailure { err ->
                                                    snackbarHostState.showSnackbar(err.message ?: "Snapshot failed")
                                                }
                                            }
                                        }
                                    },
                                    onRestoreOfflineSnapshot = { fileName, replace ->
                                        viewModel.restoreOfflineSnapshot(fileName, replace) { res ->
                                            scope.launch {
                                                res.onSuccess { (m, r) ->
                                                    snackbarHostState.showSnackbar("Restored $m maths, $r results")
                                                }.onFailure { err ->
                                                    snackbarHostState.showSnackbar(err.message ?: "Restore failed")
                                                }
                                            }
                                        }
                                    },
                                    onDeleteOfflineSnapshot = viewModel::deleteOfflineSnapshot,
                                    onExportBackup = { uri ->
                                        viewModel.exportBackupToUri(uri) { res ->
                                            scope.launch {
                                                res.onSuccess { (m, r) ->
                                                    snackbarHostState.showSnackbar("Backup saved ($m maths, $r results)")
                                                }.onFailure { err ->
                                                    snackbarHostState.showSnackbar(err.message ?: "Backup failed")
                                                }
                                            }
                                        }
                                    },
                                    onRestoreBackup = { uri, replace ->
                                        viewModel.restoreBackupFromUri(uri, replace) { res ->
                                            scope.launch {
                                                res.onSuccess { (m, r) ->
                                                    snackbarHostState.showSnackbar("Restored $m maths, $r results")
                                                }.onFailure { err ->
                                                    snackbarHostState.showSnackbar(err.message ?: "Restore failed")
                                                }
                                            }
                                        }
                                    },
                                    onExportResultsCsv = { uri ->
                                        viewModel.exportResultsCsvToUri(uri) { res ->
                                            scope.launch {
                                                res.onSuccess { count ->
                                                    snackbarHostState.showSnackbar("Exported $count results to CSV")
                                                }.onFailure { err ->
                                                    snackbarHostState.showSnackbar(err.message ?: "Export failed")
                                                }
                                            }
                                        }
                                    },
                                    onExportMathCsv = { uri ->
                                        viewModel.exportMathCsvToUri(uri) { res ->
                                            scope.launch {
                                                res.onSuccess { count ->
                                                    snackbarHostState.showSnackbar("Exported $count math records to CSV")
                                                }.onFailure { err ->
                                                    snackbarHostState.showSnackbar(err.message ?: "Export failed")
                                                }
                                            }
                                        }
                                    },
                                    onSaveGitHubConfig = viewModel::updateGitHubSettings,
                                    onCheckGitHubRelease = viewModel::checkGitHubRelease,
                                    onDownloadGitHubFile = { url, destUri ->
                                        viewModel.downloadFromGitHubToUri(url, destUri) { res ->
                                            scope.launch {
                                                res.onSuccess {
                                                    snackbarHostState.showSnackbar("Downloaded from GitHub")
                                                }.onFailure { err ->
                                                    snackbarHostState.showSnackbar(err.message ?: "Download failed")
                                                }
                                            }
                                        }
                                    },
                                    onUploadBackupToGitHub = { repo, token ->
                                        viewModel.uploadBackupToGitHub(repo, token) { res ->
                                            scope.launch {
                                                res.onSuccess { path ->
                                                    snackbarHostState.showSnackbar("Uploaded: $path")
                                                }.onFailure { err ->
                                                    snackbarHostState.showSnackbar(err.message ?: "Upload failed")
                                                }
                                            }
                                        }
                                    },
                                    onOpenApkDownload = { showApkDialog = true },
                                    onShowMessage = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }

                            BottomTab.ABOUT -> {
                                AboutDeveloperScreen(
                                    settings = settings,
                                    onUpdateGitHubUrl = { url ->
                                        viewModel.updateGitHubSettings(
                                            settings.githubRepo,
                                            settings.githubToken,
                                            url
                                        )
                                    },
                                    onOpenApkDownload = { showApkDialog = true },
                                    onOpenGitHubDownload = { currentTab = BottomTab.BACKUP }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateMenu) {
        AlertDialog(
            onDismissRequest = { showCreateMenu = false },
            title = { Text(strings.addNewTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CreateOptionTile(
                        icon = Icons.Filled.CameraAlt,
                        label = strings.addNewMathCamera,
                        tint = ext.solverBlueStart,
                        bg = ext.solverBlueContainer,
                        onClick = {
                            showCreateMenu = false
                            onOpenCameraSolver()
                        }
                    )
                    CreateOptionTile(
                        icon = Icons.Filled.Calculate,
                        label = strings.addNewMathCalc,
                        tint = MaterialTheme.colorScheme.primary,
                        bg = MaterialTheme.colorScheme.primaryContainer,
                        onClick = {
                            showCreateMenu = false
                            onOpenCalculatorSolver()
                        }
                    )
                    CreateOptionTile(
                        icon = Icons.AutoMirrored.Filled.NoteAdd,
                        label = strings.addNewResult,
                        tint = ext.onGoldContainer,
                        bg = ext.goldContainer,
                        onClick = {
                            showCreateMenu = false
                            onOpenNewResult()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCreateMenu = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showApkDialog) {
        ApkDownloadDialog(
            githubRepo = settings.githubRepo,
            onDismiss = { showApkDialog = false },
            onOpenBackupGitHub = {
                currentTab = BottomTab.BACKUP
                viewModel.checkGitHubRelease()
            },
            onMessage = { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            }
        )
    }
}

@Composable
private fun CreateOptionTile(
    icon: ImageVector,
    label: String,
    tint: Color,
    bg: Color,
    onClick: () -> Unit
) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        }
    }
}
