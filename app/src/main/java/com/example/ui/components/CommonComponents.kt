package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.AppLanguage
import com.example.core.LocalLanguage
import com.example.core.LocalStrings
import com.example.domain.BackupAndGitHubManager
import com.example.domain.PdfAndImageExporter
import com.example.ui.theme.LocalMsRsColors
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MsRsLogoBadge(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val ext = LocalMsRsColors.current
    Image(
        painter = painterResource(id = R.drawable.img_msrs_logo),
        contentDescription = "MS RS Logo",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.25f))
            .border(1.5.dp, ext.gold, RoundedCornerShape(size * 0.25f))
    )
}

@Composable
fun LanguageSwitchPill(
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalLanguage.current
    val ext = LocalMsRsColors.current
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onToggle)
            .testTag("language_switch_button"),
        color = Color.White.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, ext.goldBright.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (lang == AppLanguage.BANGLA) ext.goldBright else Color.Transparent)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "বাং",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (lang == AppLanguage.BANGLA) Color(0xFF0A2E18) else Color.White
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (lang == AppLanguage.ENGLISH) ext.goldBright else Color.Transparent)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "EN",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (lang == AppLanguage.ENGLISH) Color(0xFF0A2E18) else Color.White
                )
            }
        }
    }
}

@Composable
fun EyeViewButton(
    contentDescription: String,
    testTag: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = containerColor,
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier.size(42.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Visibility,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun AnimatedScaleCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerBrush: Brush,
    borderColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "cardScale"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .background(containerBrush)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.2.dp, borderColor, RoundedCornerShape(22.dp))
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
    ) {
        content()
    }
}

@Composable
fun IslamicGeometricBackdrop(
    tint: Color = Color.White.copy(alpha = 0.07f),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val step = 76.dp.toPx()
        val r = 24.dp.toPx()
        var y = 0f
        while (y < size.height + step) {
            var x = 0f
            while (x < size.width + step) {
                val path = Path()
                for (i in 0 until 8) {
                    val angle = (i * 45.0) * PI / 180.0
                    val px = x + r * cos(angle).toFloat()
                    val py = y + r * sin(angle).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(path, color = tint, style = Stroke(width = 1.1.dp.toPx()))
                x += step
            }
            y += step
        }
    }
}

@Composable
fun FloatingMathSymbolsLayer(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "mathFloat")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val symbols = remember { listOf("+", "−", "×", "÷", "=", "★", "০", "১", "✦") }

    Box(modifier = modifier.fillMaxSize()) {
        symbols.forEachIndexed { idx, sym ->
            val xFrac = ((idx * 17 + 9) % 88) / 100f
            val baseYFrac = ((idx * 23 + 12) % 85) / 100f
            val drift = sin((phase * 2 * PI + idx).toDouble()).toFloat() * 0.03f
            val yFrac = (baseYFrac + drift).coerceIn(0.05f, 0.92f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = (xFrac * 320).dp,
                        top = (yFrac * 620).dp
                    )
            ) {
                Text(
                    text = sym,
                    fontSize = if (idx % 2 == 0) 24.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.14f)
                )
            }
        }
    }
}

@Composable
fun MsRsTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    val ext = LocalMsRsColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(ext.emeraldGradientStart, ext.emeraldGradientEnd)
                )
            )
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("top_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            )
            actions()
        }
    }
}

@Composable
fun ApkDownloadDialog(
    githubRepo: String,
    onDismiss: () -> Unit,
    onOpenBackupGitHub: () -> Unit,
    onMessage: (String) -> Unit
) {
    val strings = LocalStrings.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }

    val saveApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri: Uri? ->
        if (uri != null) {
            working = true
            scope.launch {
                runCatching {
                    BackupAndGitHubManager.writeInstalledApkToUri(ctx, uri)
                }.onSuccess {
                    working = false
                    onMessage("MS_RS.apk saved")
                    onDismiss()
                }.onFailure { err ->
                    working = false
                    onMessage(err.message ?: "Failed")
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MsRsLogoBadge(size = 36.dp)
                Text(text = strings.apkDownload, style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.apkDownloadSub,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { saveApkLauncher.launch("MS_RS_v1.0.apk") },
                    enabled = !working,
                    modifier = Modifier.fillMaxWidth().testTag("save_offline_apk_btn")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.exportInstalledApk)
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val file = BackupAndGitHubManager.prepareInstalledApkInCache(ctx)
                            PdfAndImageExporter.shareFile(
                                ctx,
                                file,
                                "application/vnd.android.package-archive",
                                strings.apkDownload
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("share_offline_apk_btn")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("${strings.share} APK")
                }
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenBackupGitHub()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("open_github_apk_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Filled.CloudDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.githubDownloadUpdate)
                }
                TextButton(
                    onClick = {
                        val clean = githubRepo.removePrefix("https://github.com/").trim('/')
                        val url = "https://github.com/$clean/releases"
                        runCatching {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(strings.openInBrowser)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
