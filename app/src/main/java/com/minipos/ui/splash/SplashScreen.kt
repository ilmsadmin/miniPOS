package com.minipos.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minipos.BuildConfig
import com.minipos.R
import com.minipos.core.theme.AppColors
import kotlinx.coroutines.delay

// ═══════════════════════════════════════
// SPLASH SCREEN
// ═══════════════════════════════════════

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
) {
    val background = AppColors.Background
    val primary = AppColors.Primary
    val primaryLight = AppColors.PrimaryLight
    val accent = AppColors.Accent
    val textPrimary = AppColors.TextPrimary
    val textTertiary = AppColors.TextTertiary
    val inputBg = AppColors.InputBackground
    val BrandNavy = AppColors.BrandNavy
    val BrandBlue = AppColors.BrandBlue
    val BrandBlueLight = AppColors.BrandBlueLight

    // ─── Master timeline ───
    var splashStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        splashStarted = true
        delay(3200L)
        onSplashFinished()
    }

    // ─── Entrance animations ───
    val orbAlpha by animateFloatAsState(
        targetValue = if (splashStarted) 1f else 0f,
        animationSpec = tween(2000, delayMillis = 200, easing = EaseOut),
        label = "orbAlpha",
    )
    val logoScale by animateFloatAsState(
        targetValue = if (splashStarted) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "logoScale",
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (splashStarted) 1f else 0f,
        animationSpec = tween(800, delayMillis = 300, easing = EaseOut),
        label = "logoAlpha",
    )
    val logoRotation by animateFloatAsState(
        targetValue = if (splashStarted) 0f else -20f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "logoRotation",
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (splashStarted) 1f else 0f,
        animationSpec = tween(600, delayMillis = 700, easing = EaseOut),
        label = "iconAlpha",
    )
    val brandAlpha by animateFloatAsState(
        targetValue = if (splashStarted) 1f else 0f,
        animationSpec = tween(700, delayMillis = 900, easing = EaseOut),
        label = "brandAlpha",
    )
    val brandOffset by animateFloatAsState(
        targetValue = if (splashStarted) 0f else 16f,
        animationSpec = tween(700, delayMillis = 900, easing = EaseOut),
        label = "brandOffset",
    )
    val loaderAlpha by animateFloatAsState(
        targetValue = if (splashStarted) 1f else 0f,
        animationSpec = tween(500, delayMillis = 1400, easing = EaseOut),
        label = "loaderAlpha",
    )
    val bottomAlpha by animateFloatAsState(
        targetValue = if (splashStarted) 0.6f else 0f,
        animationSpec = tween(600, delayMillis = 100, easing = EaseOut),
        label = "bottomAlpha",
    )
    val zenixAlpha by animateFloatAsState(
        targetValue = if (splashStarted) 1f else 0f,
        animationSpec = tween(800, delayMillis = 100, easing = EaseOut),
        label = "zenixAlpha",
    )

    // ─── Ring pulse animation ───
    val infiniteTransition = rememberInfiniteTransition(label = "splashRings")
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseOut),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(1000),
        ),
        label = "ring1Scale",
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseOut),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(1000),
        ),
        label = "ring1Alpha",
    )
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseOut),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(1600),
        ),
        label = "ring2Scale",
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseOut),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(1600),
        ),
        label = "ring2Alpha",
    )

    // ─── Loading bar animation ───
    val loaderProgress by infiniteTransition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = EaseInOutCubic),
        ),
        label = "loaderProgress",
    )

    // ─── Shimmer on logo ───
    val shimmerProgress by animateFloatAsState(
        targetValue = if (splashStarted) 1f else 0f,
        animationSpec = tween(1800, delayMillis = 1200, easing = EaseInOut),
        label = "shimmer",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        // ═══ Ambient orbs ═══
        // Orb 1 — top-left, primary teal
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-80).dp, y = (-60).dp)
                .alpha(orbAlpha)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.35f),
                            Color.Transparent,
                        ),
                    ),
                    CircleShape,
                ),
        )
        // Orb 2 — bottom-right, accent blue
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 40.dp)
                .alpha(orbAlpha)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.2f),
                            Color.Transparent,
                        ),
                    ),
                    CircleShape,
                ),
        )
        // Orb 3 — center-right, primary light
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.Center)
                .offset(x = 60.dp, y = (-40).dp)
                .alpha(orbAlpha)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryLight.copy(alpha = 0.15f),
                            Color.Transparent,
                        ),
                    ),
                    CircleShape,
                ),
        )

        // ═══ Floating particles ═══
        FloatingParticles(
            primary = primary,
            primaryLight = primaryLight,
            accent = accent,
        )

        // ═══ Center content ═══
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // ─── Logo with pulse rings ───
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(152.dp),
            ) {
                // Pulse ring 1
                Box(
                    modifier = Modifier
                        .size(152.dp)
                        .scale(ring1Scale)
                        .alpha(ring1Alpha)
                        .drawBehind {
                            drawCircle(
                                color = primary.copy(alpha = 0.3f),
                                radius = size.minDimension / 2,
                                style = Stroke(width = 2.dp.toPx()),
                            )
                        },
                )
                // Pulse ring 2
                Box(
                    modifier = Modifier
                        .size(152.dp)
                        .scale(ring2Scale)
                        .alpha(ring2Alpha)
                        .drawBehind {
                            drawCircle(
                                color = accent.copy(alpha = 0.2f),
                                radius = size.minDimension / 2,
                                style = Stroke(width = 2.dp.toPx()),
                            )
                        },
                )

                // Logo image — circle-only version (no rounded-square border)
                Image(
                    painter = painterResource(R.drawable.app_logo_circle),
                    contentDescription = stringResource(R.string.splash_brand),
                    modifier = Modifier
                        .size(120.dp)
                        .scale(logoScale)
                        .alpha(logoAlpha)
                        .rotate(logoRotation),
                )
            }

            Spacer(Modifier.height(24.dp))

            // ─── Brand text ───
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(brandAlpha)
                    .offset(y = brandOffset.dp),
            ) {
                Text(
                    text = stringResource(R.string.splash_brand),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(textPrimary, BrandBlueLight),
                        ),
                    ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.splash_tagline),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textTertiary,
                    letterSpacing = 0.3.sp,
                )
            }

            Spacer(Modifier.height(40.dp))

            // ─── Loading bar ───
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(loaderAlpha),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Animated loading bar
                Canvas(
                    modifier = Modifier
                        .width(160.dp)
                        .height(3.dp),
                ) {
                    // Background track
                    drawRoundRect(
                        color = inputBg.copy(alpha = 0.6f),
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                    )
                    // Animated progress — clip to track bounds
                    val barWidth = size.width * 0.4f
                    val rawStartX = (loaderProgress * size.width * 1.4f) - barWidth * 0.5f
                    val clippedStartX = rawStartX.coerceAtLeast(0f)
                    val clippedEndX = (rawStartX + barWidth).coerceAtMost(size.width)
                    val clippedWidth = (clippedEndX - clippedStartX).coerceAtLeast(0f)
                    if (clippedWidth > 0f) {
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(primary, accent, primaryLight),
                            ),
                            topLeft = Offset(clippedStartX, 0f),
                            size = Size(clippedWidth, size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.splash_loading),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textTertiary,
                    letterSpacing = 0.5.sp,
                )
            }
        }

        // ═══ Bottom info ═══
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Zenix Labs brand
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(zenixAlpha),
            ) {
                Text(
                    text = stringResource(R.string.splash_designed_by),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = textTertiary,
                    letterSpacing = 0.5.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.splash_company),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(BrandBlue, BrandBlueLight, accent),
                        ),
                    ),
                )
            }

            Spacer(Modifier.height(16.dp))

            // Version
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = textTertiary.copy(alpha = 0.6f),
                modifier = Modifier.alpha(bottomAlpha),
            )
        }
    }
}

// ═══════════════════════════════════════
// FLOATING PARTICLES
// ═══════════════════════════════════════

@Composable
private fun FloatingParticles(
    primary: Color,
    primaryLight: Color,
    accent: Color,
) {
    data class Particle(
        val xFraction: Float,
        val yFraction: Float,
        val size: Float,
        val color: Color,
        val delay: Int,
    )

    val particles = remember {
        listOf(
            Particle(0.20f, 0.15f, 4f, primary, 0),
            Particle(0.85f, 0.25f, 3f, accent, 1000),
            Particle(0.12f, 0.70f, 5f, primaryLight, 2000),
            Particle(0.75f, 0.80f, 3f, accent, 500),
            Particle(0.80f, 0.50f, 4f, primary, 1500),
            Particle(0.30f, 0.70f, 3f, primaryLight, 3000),
        )
    }

    particles.forEach { particle ->
        FloatingDot(particle.xFraction, particle.yFraction, particle.size, particle.color, particle.delay)
    }
}

@Composable
private fun FloatingDot(
    xFraction: Float,
    yFraction: Float,
    sizeDp: Float,
    color: Color,
    delayMs: Int,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot_$xFraction")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 6000
                0f at 0
                0.5f at 1200
                0.3f at 3000
                0.5f at 4800
                0f at 6000
            },
            initialStartOffset = StartOffset(delayMs),
        ),
        label = "dotAlpha_$xFraction",
    )
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMs),
        ),
        label = "dotY_$xFraction",
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMs),
        ),
        label = "dotScale_$xFraction",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.TopStart),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(xFraction)
                .fillMaxHeight(yFraction)
                .wrapContentSize(Alignment.BottomEnd),
        ) {
            Box(
                modifier = Modifier
                    .offset(y = offsetY.dp)
                    .scale(scale)
                    .alpha(alpha)
                    .size(sizeDp.dp)
                    .background(color, CircleShape),
            )
        }
    }
}
