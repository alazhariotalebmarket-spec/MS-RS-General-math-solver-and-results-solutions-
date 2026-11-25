package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.LocalStrings
import com.example.ui.components.FloatingMathSymbolsLayer
import com.example.ui.components.IslamicGeometricBackdrop
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.PlusJakartaSansFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val strings = LocalStrings.current

    val logoScale = remember { Animatable(0.25f) }
    val logoAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(36f) }
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val progressAnim = remember { Animatable(0f) }

    val glowTransition = rememberInfiniteTransition(label = "logoGlow")
    val glowPulse by glowTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(1f, tween(450))
        }
        launch {
            logoScale.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 240f))
        }
        delay(320)
        launch {
            titleAlpha.animateTo(1f, tween(450))
        }
        launch {
            titleOffsetY.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
        }
        delay(300)
        launch {
            subtitleAlpha.animateTo(1f, tween(500))
        }
        progressAnim.animateTo(1f, tween(1700, easing = FastOutSlowInEasing))
        delay(150)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(EmeraldDeep, EmeraldPrimary, EmeraldLight)
                )
            )
    ) {
        IslamicGeometricBackdrop(tint = Color.White.copy(alpha = 0.06f))
        FloatingMathSymbolsLayer()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Animated Logo with golden-white radial glow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            ) {
                Box(
                    modifier = Modifier
                        .size(188.dp)
                        .scale(glowPulse)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    GoldBright.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.14f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                Image(
                    painter = painterResource(id = R.drawable.img_msrs_logo),
                    contentDescription = "MS RS Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(144.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .border(2.5.dp, GoldBright, RoundedCornerShape(32.dp))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Slide-up MS RS text
            Text(
                text = "MS RS",
                fontFamily = PlusJakartaSansFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 38.sp,
                letterSpacing = 3.sp,
                color = Color.White,
                modifier = Modifier
                    .offset(y = titleOffsetY.value.dp)
                    .alpha(titleAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Fade-in Subtitle
            Text(
                text = "General Math Solver & Results Solutions",
                style = MaterialTheme.typography.titleMedium,
                color = GoldBright,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(subtitleAlpha.value)
                    .padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.alpha(subtitleAlpha.value)
            ) {
                Text(
                    text = strings.offlineAdFreeBadge,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 2-second loading bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = strings.loading,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progressAnim.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(50)),
                    color = GoldBright,
                    trackColor = Color.White.copy(alpha = 0.24f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
