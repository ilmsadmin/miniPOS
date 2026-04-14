package com.minipos.core.rating

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.play.core.review.ReviewManagerFactory
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.ui.components.MiniPosTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════
// APP RATING DIALOG — Smart rating prompt
//
// Flow:
//   1. Show animated star picker (1-5)
//   2. User taps stars → subtitle changes dynamically
//   3. User confirms:
//      - 1-3★ → opens email to zenixhq.com@gmail.com with device info
//      - 4-5★ → launches Google Play In-App Review API
//   4. "Maybe Later" button → dismiss with cooldown
// ═══════════════════════════════════════════════════════════════

private val StarGold = Color(0xFFFFB800)
private val StarGray = Color(0xFFD1D5DB)

@Composable
fun AppRatingDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onRated: (Int) -> Unit,
) {
    if (!visible) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedStars by remember { mutableIntStateOf(0) }
    var showThankYou by remember { mutableStateOf(false) }
    var animateIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        animateIn = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val scale by animateFloatAsState(
            targetValue = if (animateIn) 1f else 0.8f,
            animationSpec = spring(
                dampingRatio = 0.65f,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "ratingScale",
        )
        val alpha by animateFloatAsState(
            targetValue = if (animateIn) 1f else 0f,
            animationSpec = tween(300),
            label = "ratingAlpha",
        )

        AnimatedContent(
            targetState = showThankYou,
            transitionSpec = {
                (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.9f)) togetherWith
                    (fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.9f))
            },
            label = "ratingContent",
        ) { isThankYou ->
            if (isThankYou) {
                // ── Thank You State ──
                ThankYouContent(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha },
                    onDone = {
                        onRated(selectedStars)
                    },
                )
            } else {
                // ── Star Rating State ──
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                        .shadow(24.dp, RoundedCornerShape(MiniPosTokens.Radius2xl))
                        .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.Radius2xl))
                        .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.Radius2xl)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(28.dp))

                    // ── App Icon ──
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(AppColors.Primary, AppColors.PrimaryLight)
                                ),
                                shape = RoundedCornerShape(16.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Storefront,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Title ──
                    Text(
                        text = stringResource(R.string.rating_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.TextPrimary,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(8.dp))

                    // ── Dynamic subtitle based on selection ──
                    val subtitle = when (selectedStars) {
                        0 -> stringResource(R.string.rating_subtitle)
                        1 -> stringResource(R.string.rating_1_star)
                        2 -> stringResource(R.string.rating_2_star)
                        3 -> stringResource(R.string.rating_3_star)
                        4 -> stringResource(R.string.rating_4_star)
                        5 -> stringResource(R.string.rating_5_star)
                        else -> ""
                    }
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )

                    Spacer(Modifier.height(24.dp))

                    // ── Star Row ──
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        for (i in 1..5) {
                            AnimatedStar(
                                filled = i <= selectedStars,
                                index = i,
                                onClick = { selectedStars = i },
                            )
                            if (i < 5) Spacer(Modifier.width(8.dp))
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // ── Submit Button ──
                    AnimatedVisibility(
                        visible = selectedStars > 0,
                        enter = fadeIn(tween(200)) + expandVertically(),
                        exit = fadeOut(tween(150)) + shrinkVertically(),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 24.dp)
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = if (selectedStars >= 4)
                                                listOf(AppColors.Primary, AppColors.PrimaryLight)
                                            else
                                                listOf(AppColors.Warning, Color(0xFFFF9800))
                                        )
                                    )
                                    .clickable {
                                        scope.launch {
                                            if (selectedStars >= 4) {
                                                launchInAppReview(context)
                                                showThankYou = true
                                            } else {
                                                sendFeedbackEmail(context, selectedStars)
                                                showThankYou = true
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (selectedStars >= 4)
                                        stringResource(R.string.rating_submit_positive)
                                    else
                                        stringResource(R.string.rating_submit_feedback),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    // ── Maybe Later ──
                    HorizontalDivider(color = AppColors.Divider)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.rating_later),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextTertiary,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────
// ANIMATED STAR
// ─────────────────────────────────────

@Composable
private fun AnimatedStar(
    filled: Boolean,
    index: Int,
    onClick: () -> Unit,
) {
    val animScale by animateFloatAsState(
        targetValue = if (filled) 1.0f else 0.85f,
        animationSpec = spring(
            dampingRatio = 0.4f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "starScale$index",
    )
    val animColor by animateColorAsState(
        targetValue = if (filled) StarGold else StarGray,
        animationSpec = tween(200),
        label = "starColor$index",
    )

    Icon(
        imageVector = if (filled) Icons.Rounded.Star else Icons.Rounded.StarBorder,
        contentDescription = "$index stars",
        tint = animColor,
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer { scaleX = animScale; scaleY = animScale }
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    )
}

// ─────────────────────────────────────
// THANK YOU CONTENT
// ─────────────────────────────────────

@Composable
private fun ThankYouContent(
    modifier: Modifier = Modifier,
    onDone: () -> Unit,
) {
    // Auto-dismiss after 2.5 seconds
    LaunchedEffect(Unit) {
        delay(2500)
        onDone()
    }

    Column(
        modifier = modifier
            .shadow(24.dp, RoundedCornerShape(MiniPosTokens.Radius2xl))
            .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.Radius2xl))
            .border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.Radius2xl))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Animated heart
        val heartScale by rememberInfiniteTransition(label = "heartBeat").animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "heartScale",
        )
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(AppColors.SuccessSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Favorite,
                contentDescription = null,
                tint = AppColors.Success,
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer { scaleX = heartScale; scaleY = heartScale },
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.rating_thank_you_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.TextPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.rating_thank_you_message),
            fontSize = 13.sp,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
    }
}

// ─────────────────────────────────────
// ACTIONS
// ─────────────────────────────────────

private fun launchInAppReview(context: Context) {
    val activity = context as? Activity ?: return
    val manager = ReviewManagerFactory.create(context)
    val request = manager.requestReviewFlow()
    request.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val reviewInfo = task.result
            manager.launchReviewFlow(activity, reviewInfo)
        }
    }
}

private fun sendFeedbackEmail(context: Context, stars: Int) {
    val deviceInfo = buildString {
        append("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n")
        append("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n")
        append("App version: ${getAppVersionName(context)}\n")
        append("Rating: $stars/5\n\n")
        append("---\n")
        append(context.getString(R.string.rating_email_body_hint))
    }

    val subject = context.getString(R.string.rating_email_subject, stars)
    val emailUri = Uri.parse(
        "mailto:zenixhq.com@gmail.com" +
            "?subject=${Uri.encode(subject)}" +
            "&body=${Uri.encode(deviceInfo)}"
    )

    // 1. Try Gmail directly
    try {
        val gmailIntent = Intent(Intent.ACTION_SENDTO, emailUri).apply {
            setPackage("com.google.android.gm")
        }
        context.startActivity(gmailIntent)
        return
    } catch (_: Exception) { /* Gmail not installed or not visible */ }

    // 2. Fallback: ACTION_SENDTO with mailto: (opens default email app)
    try {
        val mailtoIntent = Intent(Intent.ACTION_SENDTO, emailUri)
        context.startActivity(mailtoIntent)
        return
    } catch (_: Exception) { /* No email app handles mailto: */ }

    // 3. Last fallback: ACTION_SEND with chooser
    val fallback = Intent(Intent.ACTION_SEND).apply {
        type = "message/rfc822"
        putExtra(Intent.EXTRA_EMAIL, arrayOf("zenixhq.com@gmail.com"))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, deviceInfo)
    }
    context.startActivity(Intent.createChooser(fallback, context.getString(R.string.rating_choose_email_app)))
}

private fun getAppVersionName(context: Context): String {
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    } catch (e: Exception) {
        "unknown"
    }
}
