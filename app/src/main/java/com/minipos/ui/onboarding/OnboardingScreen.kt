package com.minipos.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.ui.components.MiniPosTokens
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ═══════════════════════════════════════
// DATA MODEL
// ═══════════════════════════════════════

private data class OrbitIcon(
    val icon: ImageVector,
    val color: Color,
    /** Angle in degrees on the orbit circle */
    val angleDeg: Float,
    /** Animation delay in ms */
    val delay: Int,
)

private data class OnboardingSlide(
    val tagIcon: ImageVector,
    val tagText: Int,          // string resource
    val titleLine1: Int,       // string resource
    val titleLine2: Int,       // string resource (highlighted)
    val description: Int,      // string resource
    val mainIcon: ImageVector,
    val mainGradient: List<Color>,
    val tagBackground: Color,
    val tagTextColor: Color,
    val highlightGradient: List<Color>,
    val glowColor: Color,
    val orbitIcons: List<OrbitIcon>,
)

// ═══════════════════════════════════════
// MAIN SCREEN
// ═══════════════════════════════════════

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onCreateStore: () -> Unit,
    onJoinStore: () -> Unit,
) {
    // Colors
    val primary = AppColors.Primary
    val primaryLight = AppColors.PrimaryLight
    val accent = AppColors.Accent
    val success = AppColors.Success
    val warning = AppColors.Warning
    val iconDrinks = AppColors.IconDrinks
    val iconFood = AppColors.IconFood
    val iconSnacks = AppColors.IconSnacks
    val iconDairy = AppColors.IconDairy
    val background = AppColors.Background
    val textTertiary = AppColors.TextTertiary
    val borderLight = AppColors.BorderLight

    val slides = remember(primary, primaryLight, accent, success, warning, iconDrinks, iconFood, iconSnacks, iconDairy) {
        listOf(
            // Slide 1: Bán hàng siêu tốc
            OnboardingSlide(
                tagIcon = Icons.Rounded.Bolt,
                tagText = R.string.ob_tag_1,
                titleLine1 = R.string.ob_title_1_line1,
                titleLine2 = R.string.ob_title_1_line2,
                description = R.string.ob_desc_1,
                mainIcon = Icons.Rounded.PointOfSale,
                mainGradient = listOf(primary, primaryLight),
                tagBackground = primary.copy(alpha = 0.12f),
                tagTextColor = primaryLight,
                highlightGradient = listOf(primary, primaryLight),
                glowColor = primary,
                orbitIcons = listOf(
                    OrbitIcon(Icons.Rounded.LocalDrink, iconDrinks, 135f, 0),
                    OrbitIcon(Icons.Rounded.Restaurant, iconFood, 45f, 800),
                    OrbitIcon(Icons.Rounded.Cookie, iconSnacks, 225f, 1600),
                    OrbitIcon(Icons.Rounded.Icecream, iconDairy, 315f, 400),
                ),
            ),
            // Slide 2: Quản lý thông minh
            OnboardingSlide(
                tagIcon = Icons.Rounded.AutoAwesome,
                tagText = R.string.ob_tag_2,
                titleLine1 = R.string.ob_title_2_line1,
                titleLine2 = R.string.ob_title_2_line2,
                description = R.string.ob_desc_2,
                mainIcon = Icons.Rounded.Analytics,
                mainGradient = listOf(accent, primary),
                tagBackground = accent.copy(alpha = 0.10f),
                tagTextColor = accent,
                highlightGradient = listOf(accent, primary),
                glowColor = accent,
                orbitIcons = listOf(
                    OrbitIcon(Icons.Rounded.QrCodeScanner, accent, 135f, 0),
                    OrbitIcon(Icons.Rounded.Inventory2, primaryLight, 45f, 800),
                    OrbitIcon(Icons.Rounded.TrendingUp, warning, 225f, 1600),
                    OrbitIcon(Icons.Rounded.ReceiptLong, iconFood, 315f, 400),
                ),
            ),
            // Slide 3: Sẵn sàng khởi nghiệp
            OnboardingSlide(
                tagIcon = Icons.Rounded.RocketLaunch,
                tagText = R.string.ob_tag_3,
                titleLine1 = R.string.ob_title_3_line1,
                titleLine2 = R.string.ob_title_3_line2,
                description = R.string.ob_desc_3,
                mainIcon = Icons.Rounded.RocketLaunch,
                mainGradient = listOf(success, Color(0xFF69F0AE)),
                tagBackground = success.copy(alpha = 0.12f),
                tagTextColor = success,
                highlightGradient = listOf(success, Color(0xFF69F0AE)),
                glowColor = success,
                orbitIcons = listOf(
                    OrbitIcon(Icons.Rounded.Verified, success, 135f, 0),
                    OrbitIcon(Icons.Rounded.CloudDone, accent, 45f, 800),
                    OrbitIcon(Icons.Rounded.Shield, primaryLight, 225f, 1600),
                    OrbitIcon(Icons.Rounded.Speed, iconDrinks, 315f, 400),
                ),
            ),
        )
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == slides.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        // ─── Decorative ambient glow (top-right) ───
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-40).dp)
                .blur(40.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.15f),
                            Color.Transparent,
                        ),
                    ),
                    CircleShape,
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // ═══ Slides ═══
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                SlideContent(
                    slide = slides[page],
                    isActive = pagerState.currentPage == page,
                )
            }

            // ═══ Swipe hint (only on first page) ═══
            // Use a fixed-height Box with alpha animation instead of AnimatedVisibility
            // to avoid layout shift that causes the illustration to jump when hint disappears
            val swipeHintAlpha by animateFloatAsState(
                targetValue = if (pagerState.currentPage == 0) 1f else 0f,
                animationSpec = tween(300),
                label = "swipeHintAlpha",
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .graphicsLayer { alpha = swipeHintAlpha },
                contentAlignment = Alignment.Center,
            ) {
                SwipeHint()
            }

            // ═══ Footer controls ═══
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(top = 24.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Dot indicators
                DotIndicators(
                    count = slides.size,
                    current = pagerState.currentPage,
                    onDotClick = { idx ->
                        coroutineScope.launch { pagerState.animateScrollToPage(idx) }
                    },
                )

                // Buttons
                if (isLastPage) {
                    // "Bắt đầu sử dụng" button — animated gradient
                    StartButton(
                        onClick = onCreateStore,
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Skip button
                        TextButton(
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(slides.lastIndex) }
                            },
                        ) {
                            Text(
                                stringResource(R.string.ob_skip),
                                color = textTertiary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        // Next button
                        NextButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════
// SLIDE CONTENT
// ═══════════════════════════════════════

@Composable
private fun SlideContent(
    slide: OnboardingSlide,
    isActive: Boolean,
) {
    val alphaAnim by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.3f,
        animationSpec = tween(400),
        label = "slideAlpha",
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "slideScale",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .alpha(alphaAnim),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // ─── Illustration area ───
        IllustrationArea(
            slide = slide,
            scale = scaleAnim,
        )

        Spacer(modifier = Modifier.height(40.dp))

        // ─── Text content ───
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            // Tag badge
            TagBadge(
                icon = slide.tagIcon,
                text = stringResource(slide.tagText),
                backgroundColor = slide.tagBackground,
                textColor = slide.tagTextColor,
            )

            Spacer(Modifier.height(16.dp))

            // Title with highlighted second line
            Text(
                text = buildAnnotatedString {
                    append(stringResource(slide.titleLine1))
                    append("\n")
                    withStyle(
                        SpanStyle(
                            brush = Brush.linearGradient(slide.highlightGradient),
                            fontWeight = FontWeight.Black,
                        )
                    ) {
                        append(stringResource(slide.titleLine2))
                    }
                },
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp,
                letterSpacing = (-0.3).sp,
            )

            Spacer(Modifier.height(12.dp))

            // Description
            Text(
                text = stringResource(slide.description),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )
        }
    }
}

// ═══════════════════════════════════════
// ILLUSTRATION WITH ORBITING ICONS
// ═══════════════════════════════════════

@Composable
private fun IllustrationArea(
    slide: OnboardingSlide,
    scale: Float,
) {
    val illustSize = 220.dp
    val iconAreaSize = 180.dp

    // Slow spinning ring
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
        ),
        label = "ringRotation",
    )

    Box(
        modifier = Modifier
            .size(illustSize)
            .scale(scale),
        contentAlignment = Alignment.Center,
    ) {
        // Dashed decorative ring
        val borderColor = AppColors.BorderLight
        Box(
            modifier = Modifier
                .size(illustSize + 24.dp)
                .rotate(ringRotation)
                .drawBehind {
                    drawCircle(
                        color = borderColor.copy(alpha = 0.4f),
                        radius = size.minDimension / 2,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
                                0f,
                            ),
                        ),
                    )
                },
        )

        // Gradient background circle (glow)
        Box(
            modifier = Modifier
                .size(illustSize)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            slide.glowColor.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                    CircleShape,
                ),
        )

        // Icon composition area
        Box(
            modifier = Modifier.size(iconAreaSize),
            contentAlignment = Alignment.Center,
        ) {
            // Main center icon
            MainIcon(
                icon = slide.mainIcon,
                gradient = slide.mainGradient,
            )

            // Orbiting small icons
            slide.orbitIcons.forEach { orbit ->
                OrbitingIcon(
                    icon = orbit.icon,
                    color = orbit.color,
                    angleDeg = orbit.angleDeg,
                    animDelay = orbit.delay,
                    containerSize = iconAreaSize,
                )
            }
        }
    }
}

@Composable
private fun MainIcon(
    icon: ImageVector,
    gradient: List<Color>,
) {
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(MiniPosTokens.Radius2xl))
            .background(Brush.linearGradient(gradient)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(44.dp),
        )
    }
}

@Composable
private fun OrbitingIcon(
    icon: ImageVector,
    color: Color,
    angleDeg: Float,
    animDelay: Int,
    containerSize: Dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbit_$angleDeg")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = animDelay, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "float_$angleDeg",
    )

    // Position on orbit circle
    val radius = containerSize / 2 - 22.dp
    val angleRad = (angleDeg * PI / 180f).toFloat()
    val xOffset = (radius.value * cos(angleRad)).dp
    val yOffset = (radius.value * sin(angleRad)).dp

    val surface = AppColors.Surface
    val border = AppColors.BorderLight

    Box(
        modifier = Modifier
            .offset(x = xOffset, y = yOffset + floatOffset.dp)
            .size(44.dp)
            .clip(RoundedCornerShape(MiniPosTokens.RadiusLg))
            .background(surface)
            .border(1.dp, border, RoundedCornerShape(MiniPosTokens.RadiusLg)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp),
        )
    }
}

// ═══════════════════════════════════════
// TAG BADGE
// ═══════════════════════════════════════

@Composable
private fun TagBadge(
    icon: ImageVector,
    text: String,
    backgroundColor: Color,
    textColor: Color,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
            .background(backgroundColor)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 1.sp,
        )
    }
}

// ═══════════════════════════════════════
// DOT INDICATORS
// ═══════════════════════════════════════

@Composable
private fun DotIndicators(
    count: Int,
    current: Int,
    onDotClick: (Int) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val isActive = index == current
            val width by animateDpAsState(
                targetValue = if (isActive) 28.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "dotWidth",
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusFull))
                    .background(
                        if (isActive)
                            Brush.linearGradient(
                                listOf(AppColors.Primary, AppColors.PrimaryLight)
                            )
                        else
                            Brush.linearGradient(
                                listOf(
                                    AppColors.TextTertiary.copy(alpha = 0.3f),
                                    AppColors.TextTertiary.copy(alpha = 0.3f),
                                )
                            )
                    )
                    .clickable { onDotClick(index) },
            )
        }
    }
}

// ═══════════════════════════════════════
// SWIPE HINT
// ═══════════════════════════════════════

@Composable
private fun SwipeHint() {
    val infiniteTransition = rememberInfiniteTransition(label = "swipe")
    val swipeX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "swipeX",
    )

    Row(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .alpha(0.5f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Icons.Rounded.SwipeLeft,
            contentDescription = null,
            tint = AppColors.TextTertiary,
            modifier = Modifier
                .size(16.dp)
                .offset(x = swipeX.dp),
        )
        Text(
            stringResource(R.string.ob_swipe_hint),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextTertiary,
        )
    }
}

// ═══════════════════════════════════════
// NEXT BUTTON
// ═══════════════════════════════════════

@Composable
private fun NextButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .widthIn(max = 200.dp)
            .fillMaxWidth(0.5f)
            .height(52.dp)
            .clip(RoundedCornerShape(MiniPosTokens.Radius2xl))
            .background(Brush.linearGradient(listOf(AppColors.Primary, AppColors.PrimaryLight)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.ob_next),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Icon(
                Icons.Rounded.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ═══════════════════════════════════════
// START BUTTON (Last slide — animated gradient)
// ═══════════════════════════════════════

@Composable
private fun StartButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "fabGradient")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "gradientOffset",
    )

    val primary = AppColors.Primary
    val accent = AppColors.Accent
    val primaryLight = AppColors.PrimaryLight

    val animatedBrush = Brush.linearGradient(
        colors = listOf(primary, accent, primaryLight),
        start = Offset(gradientOffset * 500f, 0f),
        end = Offset(gradientOffset * 500f + 500f, 500f),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(MiniPosTokens.Radius2xl))
            .background(animatedBrush)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Rounded.RocketLaunch,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
            Text(
                stringResource(R.string.ob_start),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}
