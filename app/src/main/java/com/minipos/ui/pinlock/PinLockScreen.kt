package com.minipos.ui.pinlock

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.ui.components.*
import kotlinx.coroutines.delay

@Composable
fun PinLockScreen(
    onUnlocked: () -> Unit,
    viewModel: PinLockViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isUnlocked) {
        if (state.isUnlocked) onUnlocked()
    }

    var showPin by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // ── Entrance animation ──
    val enterAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enterAnim.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }

    // ── Shake animation on error ──
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(state.error) {
        if (state.error != null) {
            repeat(3) {
                shakeOffset.animateTo(12f, tween(50))
                shakeOffset.animateTo(-12f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    // Auto-focus PIN field — small delay lets the composition settle before requesting focus
    LaunchedEffect(Unit) {
        delay(100)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    // ═══ Forgot PIN overlay ═══
    if (state.showForgotPin) {
        PinLockForgotOverlay(
            step = state.forgotStep,
            ownerHasPassword = state.ownerHasPassword,
            password = state.forgotPassword,
            newPin = state.newPin,
            error = state.forgotError,
            isLoading = state.isLoading,
            onPasswordChanged = { viewModel.onForgotPasswordChanged(it) },
            onNewPinChanged = { viewModel.onNewPinChanged(it) },
            onVerifyPassword = { viewModel.verifyPasswordForReset() },
            onResetPin = { viewModel.resetPinAndUnlock() },
            onDismiss = { viewModel.hideForgotPin() },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        // ═══ Ambient background orbs (matching splash design) ═══
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = (-60).dp, y = (-40).dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        listOf(AppColors.Primary.copy(alpha = 0.15f), Color.Transparent),
                    ),
                    CircleShape,
                )
                .graphicsLayer { alpha = enterAnim.value },
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(220.dp)
                .offset(x = 50.dp, y = 30.dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        listOf(AppColors.Accent.copy(alpha = 0.1f), Color.Transparent),
                    ),
                    CircleShape,
                )
                .graphicsLayer { alpha = enterAnim.value },
        )

        // ═══ Main content ═══
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp)
                .graphicsLayer {
                    alpha = enterAnim.value
                    translationY = (1f - enterAnim.value) * 40f
                },
            verticalArrangement = Arrangement.Center,
        ) {
            // ── Lock icon with gradient circle + pulse ring ──
            Box(contentAlignment = Alignment.Center) {
                // Pulse ring
                val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        tween(2500, easing = FastOutSlowInEasing),
                        RepeatMode.Reverse,
                    ),
                    label = "pulseScale",
                )
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale; alpha = 0.15f }
                        .border(2.dp, AppColors.Primary, CircleShape),
                )

                // Main circle
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .shadow(16.dp, CircleShape)
                        .background(
                            Brush.linearGradient(listOf(AppColors.Primary, AppColors.PrimaryLight)),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Brand name ──
            Text(
                stringResource(R.string.app_name),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = AppColors.Primary,
                letterSpacing = (-0.5).sp,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                stringResource(R.string.pin_enter_to_continue),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextTertiary,
            )

            Spacer(Modifier.height(32.dp))

            // ── PIN Input field (custom styled) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = shakeOffset.value },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            if (state.error != null) 0.dp else 4.dp,
                            RoundedCornerShape(MiniPosTokens.RadiusXl),
                        )
                        .background(
                            AppColors.Surface,
                            RoundedCornerShape(MiniPosTokens.RadiusXl),
                        )
                        .border(
                            width = if (state.error != null) 2.dp else 1.5.dp,
                            color = if (state.error != null) AppColors.Error
                            else if (state.pin.isNotEmpty()) AppColors.Primary
                            else AppColors.BorderLight,
                            shape = RoundedCornerShape(MiniPosTokens.RadiusXl),
                        )
                        .padding(horizontal = 20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Pin,
                            contentDescription = null,
                            tint = if (state.error != null) AppColors.Error
                            else if (state.pin.isNotEmpty()) AppColors.Primary
                            else AppColors.TextTertiary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(12.dp))

                        BasicTextField(
                            value = state.pin,
                            onValueChange = { value ->
                                if (value.length <= 6 && value.all { it.isDigit() }) {
                                    viewModel.onPinChanged(value)
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (state.pin.length >= 4) viewModel.submitPin()
                                    focusManager.clearFocus()
                                },
                            ),
                            visualTransformation = if (showPin) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            decorationBox = { inner ->
                                Box(
                                    modifier = Modifier.fillMaxHeight(),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    if (state.pin.isEmpty()) {
                                        Text(
                                            stringResource(R.string.pin_label),
                                            fontSize = 14.sp,
                                            color = AppColors.TextTertiary,
                                        )
                                    }
                                    inner()
                                }
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary,
                                letterSpacing = if (!showPin) 4.sp else 0.sp,
                            ),
                        )

                        // Toggle visibility
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AppColors.InputBackground)
                                .clickable { showPin = !showPin },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (showPin) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null,
                                tint = AppColors.TextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                // PIN dot indicators
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(6) { index ->
                        val filled = index < state.pin.length
                        Box(
                            modifier = Modifier
                                .size(if (filled) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        state.error != null && filled -> AppColors.Error
                                        filled -> AppColors.Primary
                                        else -> AppColors.BorderLight
                                    },
                                ),
                        )
                    }
                }

                // Error message
                AnimatedVisibility(visible = state.error != null) {
                    Text(
                        text = state.error ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            // ── Locked warning ──
            AnimatedVisibility(
                visible = state.lockedUntilMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(AppColors.ErrorSoft, RoundedCornerShape(MiniPosTokens.RadiusMd))
                        .border(1.dp, AppColors.Error.copy(alpha = 0.3f), RoundedCornerShape(MiniPosTokens.RadiusMd))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = AppColors.Error,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        state.lockedUntilMessage ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Error,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Unlock button (gradient) ──
            val buttonEnabled = state.pin.length >= 4 && state.lockedUntilMessage == null && !state.isLoading
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        if (buttonEnabled) 8.dp else 0.dp,
                        RoundedCornerShape(MiniPosTokens.Radius2xl),
                    )
                    .clip(RoundedCornerShape(MiniPosTokens.Radius2xl))
                    .background(
                        if (buttonEnabled)
                            Brush.linearGradient(listOf(AppColors.Primary, AppColors.Accent, AppColors.PrimaryLight))
                        else
                            Brush.linearGradient(listOf(AppColors.BorderLight, AppColors.BorderLight)),
                    )
                    .then(
                        if (buttonEnabled) Modifier.clickable { viewModel.submitPin() }
                        else Modifier,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = Color.White,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.LockOpen,
                            contentDescription = null,
                            tint = if (buttonEnabled) Color.White else AppColors.TextTertiary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.pin_unlock_btn),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (buttonEnabled) Color.White else AppColors.TextTertiary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Forgot PIN ──
            Text(
                text = stringResource(R.string.pin_forgot_btn),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextTertiary,
                modifier = Modifier
                    .clip(RoundedCornerShape(MiniPosTokens.RadiusMd))
                    .clickable { viewModel.showForgotPin() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

// ─── Forgot PIN Overlay (bottom sheet dialog) ────────────────────────────
@Composable
private fun PinLockForgotOverlay(
    step: PinLockForgotStep,
    ownerHasPassword: Boolean,
    password: String,
    newPin: String,
    error: String?,
    isLoading: Boolean,
    onPasswordChanged: (String) -> Unit,
    onNewPinChanged: (String) -> Unit,
    onVerifyPassword: () -> Unit,
    onResetPin: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var showPassword by remember { mutableStateOf(false) }
    var showNewPin by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(error) {
        if (error != null) {
            repeat(3) {
                shakeOffset.animateTo(10f, tween(50))
                shakeOffset.animateTo(-10f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(AppColors.Background)
                    .clickable(enabled = false, onClick = {})
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
            ) {
                // Handle bar
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AppColors.BorderLight),
                )
                Spacer(Modifier.height(20.dp))

                // Icon + title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFF74B9FF)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.LockReset, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        stringResource(R.string.login_reset_pin_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.TextPrimary,
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Step indicator — only for 2-step flow (has password)
                if (ownerHasPassword) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ForgotStepDot(active = true, done = step == PinLockForgotStep.SET_NEW_PIN, label = "1")
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(2.dp)
                                .background(if (step == PinLockForgotStep.SET_NEW_PIN) AppColors.Primary else AppColors.BorderLight),
                        )
                        ForgotStepDot(active = step == PinLockForgotStep.SET_NEW_PIN, done = false, label = "2")
                    }
                    Spacer(Modifier.height(20.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationX = shakeOffset.value },
                ) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            (fadeIn(tween(250)) + slideInHorizontally { it / 3 })
                                .togetherWith(fadeOut(tween(200)) + slideOutHorizontally { -it / 3 })
                        },
                        label = "pinlock_forgot_step",
                    ) { currentStep ->
                        Column {
                            when (currentStep) {
                                PinLockForgotStep.ENTER_PASSWORD -> {
                                    Text(
                                        stringResource(R.string.login_password_hint),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppColors.TextSecondary,
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    ForgotPasswordField(
                                        value = password,
                                        hint = stringResource(R.string.login_password_hint),
                                        isPassword = !showPassword,
                                        autoFocus = true,
                                        onToggleVisibility = { showPassword = !showPassword },
                                        onValueChange = onPasswordChanged,
                                        onDone = { if (password.isNotBlank()) onVerifyPassword(); focusManager.clearFocus() },
                                        error = error,
                                    )
                                    Spacer(Modifier.height(20.dp))
                                    ForgotActionButton(
                                        enabled = password.isNotBlank() && !isLoading,
                                        isLoading = isLoading,
                                        label = stringResource(R.string.verify_btn),
                                        icon = Icons.Rounded.Key,
                                        onClick = onVerifyPassword,
                                    )
                                }
                                PinLockForgotStep.SET_NEW_PIN -> {
                                    Text(
                                        if (ownerHasPassword) stringResource(R.string.login_reset_pin_new)
                                        else stringResource(R.string.set_new_pin_step),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppColors.TextSecondary,
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    ForgotPasswordField(
                                        value = newPin,
                                        hint = stringResource(R.string.pin_label),
                                        isPassword = !showNewPin,
                                        isNumeric = true,
                                        maxLength = 6,
                                        autoFocus = true,
                                        onToggleVisibility = { showNewPin = !showNewPin },
                                        onValueChange = { v ->
                                            if (v.length <= 6 && v.all { it.isDigit() }) onNewPinChanged(v)
                                        },
                                        onDone = { if (newPin.length >= 4) onResetPin(); focusManager.clearFocus() },
                                        error = error,
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        repeat(6) { index ->
                                            val filled = index < newPin.length
                                            Box(
                                                modifier = Modifier
                                                    .size(if (filled) 10.dp else 8.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when {
                                                            error != null && filled -> AppColors.Error
                                                            filled -> AppColors.Primary
                                                            else -> AppColors.BorderLight
                                                        },
                                                    ),
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(20.dp))
                                    ForgotActionButton(
                                        enabled = newPin.length >= 4 && !isLoading,
                                        isLoading = isLoading,
                                        label = stringResource(R.string.login_reset_pin_btn),
                                        icon = Icons.Rounded.LockReset,
                                        onClick = onResetPin,
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = error != null) {
                        Text(
                            text = error ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        stringResource(R.string.login_back_to_pin),
                        fontSize = 13.sp,
                        color = AppColors.TextTertiary,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ForgotStepDot(active: Boolean, done: Boolean, label: String) {
    val bgModifier = when {
        done -> Modifier.background(AppColors.Primary, CircleShape)
        active -> Modifier.background(Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFF74B9FF))), CircleShape)
        else -> Modifier.background(AppColors.BorderLight, CircleShape)
    }
    Box(
        modifier = Modifier.size(28.dp).clip(CircleShape).then(bgModifier),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        } else {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (active) Color.White else AppColors.TextTertiary)
        }
    }
}

@Composable
private fun ForgotPasswordField(
    value: String,
    hint: String,
    isPassword: Boolean,
    isNumeric: Boolean = false,
    maxLength: Int = 100,
    error: String?,
    autoFocus: Boolean = false,
    onToggleVisibility: () -> Unit,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    if (autoFocus) {
        LaunchedEffect(Unit) {
            delay(200)
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(if (error != null) 0.dp else 4.dp, RoundedCornerShape(MiniPosTokens.RadiusXl))
            .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.RadiusXl))
            .border(
                width = if (error != null) 2.dp else 1.5.dp,
                color = if (error != null) AppColors.Error else if (value.isNotEmpty()) AppColors.Primary else AppColors.BorderLight,
                shape = RoundedCornerShape(MiniPosTokens.RadiusXl),
            )
            .clickable { focusRequester.requestFocus() }
            .padding(horizontal = 20.dp),
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isNumeric) Icons.Rounded.Pin else Icons.Rounded.Lock,
                contentDescription = null,
                tint = if (error != null) AppColors.Error else if (value.isNotEmpty()) AppColors.Primary else AppColors.TextTertiary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = value,
                onValueChange = { v -> if (v.length <= maxLength) onValueChange(v) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = if (isNumeric) KeyboardType.NumberPassword else KeyboardType.Password),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) Text(hint, fontSize = 14.sp, color = AppColors.TextTertiary)
                        inner()
                    }
                },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    letterSpacing = if (isPassword) 4.sp else 0.sp,
                ),
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AppColors.InputBackground)
                    .clickable { onToggleVisibility() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (!isPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = null,
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ForgotActionButton(
    enabled: Boolean,
    isLoading: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(if (enabled) 8.dp else 0.dp, RoundedCornerShape(MiniPosTokens.Radius2xl))
            .clip(RoundedCornerShape(MiniPosTokens.Radius2xl))
            .background(
                if (enabled) Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFF74B9FF)))
                else Brush.linearGradient(listOf(AppColors.BorderLight, AppColors.BorderLight)),
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = Color.White)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = if (enabled) Color.White else AppColors.TextTertiary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (enabled) Color.White else AppColors.TextTertiary)
            }
        }
    }
}
