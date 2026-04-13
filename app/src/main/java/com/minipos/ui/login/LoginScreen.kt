package com.minipos.ui.login

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.minipos.R
import com.minipos.core.theme.AppColors
import com.minipos.domain.model.User
import com.minipos.domain.model.UserRole
import com.minipos.ui.components.MiniPosTokens
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Navigate on success
    LaunchedEffect(state.loginSuccess) {
        if (state.loginSuccess != null) {
            viewModel.consumeLoginSuccess()
            onLoginSuccess()
        }
    }

    val enterAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enterAnim.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }

    // Forgot PIN bottom sheet / overlay
    if (state.showForgotPin && state.selectedUser != null) {
        ForgotPinOverlay(
            user = state.selectedUser!!,
            step = state.forgotPinStep,
            ownerHasPassword = state.ownerHasPassword,
            password = state.password,
            newPin = state.newPin,
            error = state.error,
            isLoading = state.isLoading,
            onPasswordChanged = { viewModel.onPasswordChanged(it) },
            onNewPinChanged = { viewModel.onNewPinChanged(it) },
            onVerifyPassword = { viewModel.verifyPasswordForReset() },
            onResetPin = { viewModel.resetPinAndLogin() },
            onDismiss = { viewModel.hideForgotPin() },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        // Ambient orbs
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-80).dp, y = (-50).dp)
                .background(
                    Brush.radialGradient(listOf(AppColors.Primary.copy(alpha = 0.12f), Color.Transparent)),
                    CircleShape,
                )
                .graphicsLayer { alpha = enterAnim.value },
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(240.dp)
                .offset(x = 60.dp, y = 40.dp)
                .background(
                    Brush.radialGradient(listOf(AppColors.Accent.copy(alpha = 0.08f), Color.Transparent)),
                    CircleShape,
                )
                .graphicsLayer { alpha = enterAnim.value },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = enterAnim.value
                    translationY = (1f - enterAnim.value) * 30f
                },
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 24.dp),
            ) {
                // Back button — chỉ hiện khi đến từ Home (Switch User)
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AppColors.InputBackground),
                    ) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = AppColors.TextPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(12.dp, CircleShape)
                            .background(
                                Brush.linearGradient(listOf(AppColors.Primary, AppColors.PrimaryLight)),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Storefront,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        state.storeName.ifBlank { stringResource(R.string.app_name) },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.Primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.login_screen_subtitle),
                        fontSize = 13.sp,
                        color = AppColors.TextTertiary,
                    )
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            } else {
                AnimatedContent(
                    targetState = state.selectedUser,
                    transitionSpec = {
                        (fadeIn(tween(250)) + slideInHorizontally { it / 3 })
                            .togetherWith(fadeOut(tween(200)) + slideOutHorizontally { -it / 3 })
                    },
                    label = "login_anim",
                ) { selectedUser ->
                    if (selectedUser == null) {
                        UserListContent(
                            users = state.users,
                            currentUserId = state.currentUserId,
                            onSelectUser = { viewModel.selectUser(it) },
                        )
                    } else {
                        PinInputContent(
                            user = selectedUser,
                            pin = state.pin,
                            error = state.error,
                            isLoading = state.isLoading,
                            onPinChanged = { viewModel.onPinChanged(it) },
                            onLogin = { viewModel.login() },
                            onBack = { viewModel.clearSelection() },
                            onForgotPin = if (selectedUser.role == UserRole.OWNER) {
                                { viewModel.showForgotPin() }
                            } else null,
                        )
                    }
                }
            }
        }
    }
}

// ─── User List ───
@Composable
private fun UserListContent(
    users: List<User>,
    currentUserId: String?,
    onSelectUser: (User) -> Unit,
) {
    if (users.isEmpty()) {
        // Empty state
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(40.dp)) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AppColors.InputBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.PersonOff, contentDescription = null, tint = AppColors.TextTertiary, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.login_no_users_title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.login_no_users_desc), fontSize = 13.sp, color = AppColors.TextTertiary, textAlign = TextAlign.Center)
            }
        }
        return
    }

    val owners = users.filter { it.role == UserRole.OWNER }
    val staff = users.filter { it.role != UserRole.OWNER }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── OWNER section ─────────────────────────────────
        if (owners.isNotEmpty()) {
            item {
                SectionHeader(
                    label = stringResource(R.string.login_section_owner),
                    icon = Icons.Rounded.AdminPanelSettings,
                    color = Color(0xFF6C5CE7),
                )
            }
            items(owners, key = { it.id }) { user ->
                UserCard(
                    user = user,
                    isCurrentUser = user.id == currentUserId,
                    onClick = { onSelectUser(user) },
                )
            }
        }

        // ── STAFF section ──────────────────────────────────
        if (staff.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                SectionHeader(
                    label = stringResource(R.string.login_section_staff),
                    icon = Icons.Rounded.People,
                    color = Color(0xFF00B894),
                )
            }
            items(staff, key = { it.id }) { user ->
                UserCard(
                    user = user,
                    isCurrentUser = user.id == currentUserId,
                    onClick = { onSelectUser(user) },
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SectionHeader(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = color, letterSpacing = 1.2.sp)
    }
}

@Composable
private fun UserCard(user: User, isCurrentUser: Boolean = false, onClick: () -> Unit) {
    val (gradientColors, roleLabel) = when (user.role) {
        UserRole.OWNER -> Pair(listOf(Color(0xFF6C5CE7), Color(0xFF74B9FF)), stringResource(R.string.role_owner))
        UserRole.MANAGER -> Pair(listOf(Color(0xFF00B894), Color(0xFF00CEC9)), stringResource(R.string.role_manager))
        UserRole.CASHIER -> Pair(listOf(Color(0xFFFF7675), Color(0xFFFDCB6E)), stringResource(R.string.role_cashier))
    }

    val borderModifier = if (isCurrentUser) {
        Modifier.border(
            1.5.dp,
            Brush.linearGradient(gradientColors),
            RoundedCornerShape(MiniPosTokens.RadiusXl),
        )
    } else {
        Modifier.border(1.dp, AppColors.Border, RoundedCornerShape(MiniPosTokens.RadiusXl))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isCurrentUser) 6.dp else 4.dp, RoundedCornerShape(MiniPosTokens.RadiusXl))
            .clip(RoundedCornerShape(MiniPosTokens.RadiusXl))
            .background(
                if (isCurrentUser) gradientColors.first().copy(alpha = 0.06f) else AppColors.Surface,
            )
            .then(borderModifier)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(gradientColors)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                user.displayName.firstOrNull()?.uppercase() ?: "?",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
        }
        Spacer(Modifier.width(14.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    user.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                )
                if (isCurrentUser) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(gradientColors),
                                RoundedCornerShape(MiniPosTokens.RadiusFull),
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            stringResource(R.string.logged_in_badge),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Role badge
                Box(
                    modifier = Modifier
                        .background(
                            gradientColors.first().copy(alpha = 0.12f),
                            RoundedCornerShape(MiniPosTokens.RadiusFull),
                        )
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                ) {
                    Text(roleLabel, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = gradientColors.first())
                }
                // Tap-to-sign-in hint
                Text(
                    stringResource(R.string.login_tap_to_signin),
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    color = AppColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Arrow
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isCurrentUser) gradientColors.first().copy(alpha = 0.1f) else AppColors.InputBackground,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = if (isCurrentUser) gradientColors.first() else AppColors.TextTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ─── PIN Input ───
@Composable
private fun PinInputContent(
    user: User,
    pin: String,
    error: String?,
    isLoading: Boolean,
    onPinChanged: (String) -> Unit,
    onLogin: () -> Unit,
    onBack: () -> Unit,
    onForgotPin: (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    var showPin by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Auto-focus khi màn hình xuất hiện
    LaunchedEffect(Unit) {
        delay(120)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

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

    val (gradientColors, roleLabel) = when (user.role) {
        UserRole.OWNER -> Pair(listOf(Color(0xFF6C5CE7), Color(0xFF74B9FF)), stringResource(R.string.role_owner))
        UserRole.MANAGER -> Pair(listOf(Color(0xFF00B894), Color(0xFF00CEC9)), stringResource(R.string.role_manager))
        UserRole.CASHIER -> Pair(listOf(Color(0xFFFF7675), Color(0xFFFDCB6E)), stringResource(R.string.role_cashier))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Back arrow
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AppColors.InputBackground)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(20.dp))

        // User avatar + role
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(Brush.linearGradient(gradientColors)),
            contentAlignment = Alignment.Center,
        ) {
            Text(user.displayName.firstOrNull()?.uppercase() ?: "?", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
        Spacer(Modifier.height(12.dp))
        Text(user.displayName, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .background(gradientColors.first().copy(alpha = 0.12f), RoundedCornerShape(MiniPosTokens.RadiusFull))
                .padding(horizontal = 12.dp, vertical = 3.dp),
        ) {
            Text(roleLabel, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = gradientColors.first())
        }
        Spacer(Modifier.height(28.dp))

        // PIN field
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
                    .shadow(if (error != null) 0.dp else 4.dp, RoundedCornerShape(MiniPosTokens.RadiusXl))
                    .background(AppColors.Surface, RoundedCornerShape(MiniPosTokens.RadiusXl))
                    .border(
                        width = if (error != null) 2.dp else 1.5.dp,
                        color = if (error != null) AppColors.Error
                        else if (pin.isNotEmpty()) gradientColors.first()
                        else AppColors.BorderLight,
                        shape = RoundedCornerShape(MiniPosTokens.RadiusXl),
                    )
                    .padding(horizontal = 20.dp),
            ) {
                Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Pin,
                        contentDescription = null,
                        tint = if (error != null) AppColors.Error
                        else if (pin.isNotEmpty()) gradientColors.first()
                        else AppColors.TextTertiary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    BasicTextField(
                        value = pin,
                        onValueChange = { v ->
                            if (v.length <= 6 && v.all { it.isDigit() }) onPinChanged(v)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        keyboardActions = KeyboardActions(
                            onDone = { if (pin.length >= 4) onLogin(); focusManager.clearFocus() },
                        ),
                        visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        decorationBox = { inner ->
                            Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                                if (pin.isEmpty()) {
                                    Text(stringResource(R.string.pin_label), fontSize = 14.sp, color = AppColors.TextTertiary)
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

            // PIN dots — 6 slots, được fill theo màu gradient của role
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(6) { index ->
                    val filled = index < pin.length
                    val dotColor = when {
                        error != null && filled -> AppColors.Error
                        filled -> gradientColors.first()
                        else -> AppColors.BorderLight
                    }
                    // Animate dot size
                    val dotSize by animateDpAsState(
                        targetValue = if (filled) 12.dp else 8.dp,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
                        label = "dot_$index",
                    )
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                }
            }

            // Auto-submit hint
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.login_pin_auto_submit),
                fontSize = 10.sp,
                color = AppColors.TextTertiary,
            )

            // Error
            AnimatedVisibility(visible = error != null) {
                Text(
                    text = error ?: "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Login button
        val buttonEnabled = pin.length >= 4 && !isLoading
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(if (buttonEnabled) 8.dp else 0.dp, RoundedCornerShape(MiniPosTokens.Radius2xl))
                .clip(RoundedCornerShape(MiniPosTokens.Radius2xl))
                .background(
                    if (buttonEnabled)
                        Brush.linearGradient(gradientColors + listOf(gradientColors.last()))
                    else
                        Brush.linearGradient(listOf(AppColors.BorderLight, AppColors.BorderLight)),
                )
                .then(if (buttonEnabled) Modifier.clickable { onLogin() } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = Color.White)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Login,
                        contentDescription = null,
                        tint = if (buttonEnabled) Color.White else AppColors.TextTertiary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.login_btn),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (buttonEnabled) Color.White else AppColors.TextTertiary,
                    )
                }
            }
        }

        // Forgot PIN — only visible for OWNER
        if (user.role == UserRole.OWNER && onForgotPin != null) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onForgotPin) {
                Icon(
                    Icons.Rounded.LockReset,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AppColors.Primary.copy(alpha = 0.7f),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.login_forgot_pin),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Primary.copy(alpha = 0.7f),
                )
            }
        }
    }
}

// ─── Forgot PIN Overlay ────────────────────────────────────────────────────
@Composable
private fun ForgotPinOverlay(
    user: User,
    step: ForgotPinStep,
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
                    .clickable(enabled = false, onClick = {}) // block dismiss from inner clicks
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
                        Icon(
                            Icons.Rounded.LockReset,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            stringResource(R.string.login_reset_pin_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppColors.TextPrimary,
                        )
                        Text(
                            user.displayName,
                            fontSize = 13.sp,
                            color = AppColors.TextTertiary,
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Step indicator — only show when owner has a password (2-step flow)
                if (ownerHasPassword) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StepDot(active = true, done = step == ForgotPinStep.RESET_PIN, label = "1")
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(2.dp)
                                .background(
                                    if (step == ForgotPinStep.RESET_PIN) AppColors.Primary else AppColors.BorderLight,
                                ),
                        )
                        StepDot(active = step == ForgotPinStep.RESET_PIN, done = false, label = "2")
                    }
                    Spacer(Modifier.height(24.dp))
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
                        label = "forgot_pin_step",
                    ) { currentStep ->
                        Column {
                            when (currentStep) {
                                ForgotPinStep.ENTER_PASSWORD -> {
                                    Text(
                                        stringResource(R.string.login_enter_password, user.displayName),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppColors.TextSecondary,
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    PasswordInputField(
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
                                    ActionButton(
                                        enabled = password.isNotBlank() && !isLoading,
                                        isLoading = isLoading,
                                        label = stringResource(R.string.login_use_password),
                                        icon = Icons.Rounded.Key,
                                        onClick = onVerifyPassword,
                                    )
                                }
                                ForgotPinStep.RESET_PIN -> {
                                    Text(
                                        if (ownerHasPassword)
                                            stringResource(R.string.login_reset_pin_new)
                                        else
                                            stringResource(R.string.set_new_pin_step),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppColors.TextSecondary,
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    PasswordInputField(
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
                                    // PIN dots
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
                                    ActionButton(
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

                    // Error
                    AnimatedVisibility(visible = error != null) {
                        Text(
                            text = error ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
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
private fun StepDot(active: Boolean, done: Boolean, label: String) {
    val bgModifier = when {
        done -> Modifier.background(AppColors.Primary, CircleShape)
        active -> Modifier.background(Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFF74B9FF))), CircleShape)
        else -> Modifier.background(AppColors.BorderLight, CircleShape)
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .then(bgModifier),
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
private fun PasswordInputField(
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
                color = if (error != null) AppColors.Error
                else if (value.isNotEmpty()) AppColors.Primary
                else AppColors.BorderLight,
                shape = RoundedCornerShape(MiniPosTokens.RadiusXl),
            )
            .clickable { focusRequester.requestFocus() }
            .padding(horizontal = 20.dp),
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isNumeric) Icons.Rounded.Pin else Icons.Rounded.Lock,
                contentDescription = null,
                tint = if (error != null) AppColors.Error
                else if (value.isNotEmpty()) AppColors.Primary
                else AppColors.TextTertiary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = value,
                onValueChange = { v -> if (v.length <= maxLength) onValueChange(v) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isNumeric) KeyboardType.NumberPassword else KeyboardType.Password,
                ),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(hint, fontSize = 14.sp, color = AppColors.TextTertiary)
                        }
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
private fun ActionButton(
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
                if (enabled)
                    Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFF74B9FF)))
                else
                    Brush.linearGradient(listOf(AppColors.BorderLight, AppColors.BorderLight)),
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = Color.White)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (enabled) Color.White else AppColors.TextTertiary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (enabled) Color.White else AppColors.TextTertiary,
                )
            }
        }
    }
}
