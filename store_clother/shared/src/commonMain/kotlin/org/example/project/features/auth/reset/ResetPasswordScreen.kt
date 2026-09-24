package org.example.project.features.auth.reset

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.domain.validator.PasswordStrength
import org.example.project.view.components.GradientButton
import org.example.project.view.components.TopSnackbar
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import store_clother.shared.generated.resources.Res
import store_clother.shared.generated.resources.ic_eye
import store_clother.shared.generated.resources.ic_eyeOff
import store_clother.shared.generated.resources.reset_pw_back_to_forgot
import store_clother.shared.generated.resources.reset_pw_checklist_digit
import store_clother.shared.generated.resources.reset_pw_checklist_match
import store_clother.shared.generated.resources.reset_pw_checklist_min_length
import store_clother.shared.generated.resources.reset_pw_checklist_no_space
import store_clother.shared.generated.resources.reset_pw_checklist_special
import store_clother.shared.generated.resources.reset_pw_checklist_uppercase
import store_clother.shared.generated.resources.reset_pw_confirm_label
import store_clother.shared.generated.resources.reset_pw_expired_message
import store_clother.shared.generated.resources.reset_pw_expired_request_new
import store_clother.shared.generated.resources.reset_pw_expired_title
import store_clother.shared.generated.resources.reset_pw_new_label
import store_clother.shared.generated.resources.reset_pw_submit
import store_clother.shared.generated.resources.reset_pw_subtitle
import store_clother.shared.generated.resources.reset_pw_title
import store_clother.shared.generated.resources.reset_pw_validation_mismatch
import store_clother.shared.generated.resources.reset_pw_validation_weak

/**
 * Stateless Composable cho màn hình Đặt Lại Mật Khẩu.
 *
 * ### Tính năng nổi bật:
 * - **Realtime Password Checklist**: 5 điều kiện đổi màu xanh ngay khi gõ
 * - **Toggle ẩn/hiện mật khẩu** cho cả 2 field
 * - **TokenExpired UI**: full-screen thông báo riêng biệt
 */
@Composable
fun ResetPasswordScreen(
    uiState: ResetPasswordUiState,
    onIntent: (ResetPasswordIntent) -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isTokenExpired = uiState.screenState is ResetPasswordUiState.ScreenState.TokenExpired

    // Map error → string
    val errorMessage: String? = when (val state = uiState.screenState) {
        is ResetPasswordUiState.ScreenState.ValidationError -> when (state.error) {
            ResetPasswordValidationError.NewPasswordBlank ->
                "Vui lòng nhập mật khẩu mới"
            ResetPasswordValidationError.PasswordTooWeak ->
                stringResource(Res.string.reset_pw_validation_weak)
            ResetPasswordValidationError.PasswordsDoNotMatch ->
                stringResource(Res.string.reset_pw_validation_mismatch)
        }
        is ResetPasswordUiState.ScreenState.NetworkError -> state.message
        else -> null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Token Expired Full-Screen UI ─────────────────────────────────────
        AnimatedVisibility(
            visible = isTokenExpired,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TokenExpiredContent(
                onRequestNewLink = onNavigateToForgotPassword
            )
        }

        // ── Normal Form UI ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = !isTokenExpired,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .statusBarsPadding()
                    .imePadding()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Header
                Text(
                    text = "🔑 ${stringResource(Res.string.reset_pw_title)}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.reset_pw_subtitle),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ── New Password Field ────────────────────────────────────────
                OutlinedTextField(
                    value = uiState.newPassword,
                    onValueChange = { onIntent(ResetPasswordIntent.NewPasswordChanged(it)) },
                    label = { Text(stringResource(Res.string.reset_pw_new_label)) },
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    visualTransformation = if (uiState.isNewPasswordVisible)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { onIntent(ResetPasswordIntent.ToggleNewPasswordVisibility) }) {
                            Icon(
                                painter = painterResource(
                                    if (uiState.isNewPasswordVisible) Res.drawable.ic_eye
                                    else Res.drawable.ic_eyeOff
                                ),
                                contentDescription = if (uiState.isNewPasswordVisible)
                                    "Ẩn mật khẩu" else "Hiện mật khẩu",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Realtime Password Checklist ───────────────────────────────
                PasswordChecklist(
                    strength = uiState.passwordStrength,
                    doPasswordsMatch = uiState.doPasswordsMatch,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Confirm Password Field ────────────────────────────────────
                OutlinedTextField(
                    value = uiState.confirmPassword,
                    onValueChange = { onIntent(ResetPasswordIntent.ConfirmPasswordChanged(it)) },
                    label = { Text(stringResource(Res.string.reset_pw_confirm_label)) },
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    visualTransformation = if (uiState.isConfirmPasswordVisible)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { onIntent(ResetPasswordIntent.ToggleConfirmPasswordVisibility) }) {
                            Icon(
                                painter = painterResource(
                                    if (uiState.isConfirmPasswordVisible) Res.drawable.ic_eye
                                    else Res.drawable.ic_eyeOff
                                ),
                                contentDescription = if (uiState.isConfirmPasswordVisible)
                                    "Ẩn mật khẩu" else "Hiện mật khẩu",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── Submit Button ─────────────────────────────────────────────
                GradientButton(
                    onClick = { onIntent(ResetPasswordIntent.SubmitClicked) },
                    gradient = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primary
                        )
                    ),
                    enabled = !uiState.isLoading && uiState.passwordStrength.isValid && uiState.doPasswordsMatch,
                    shape = RoundedCornerShape(12.dp),
                    content = {
                        AnimatedVisibility(visible = uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(
                            text = if (uiState.isLoading) "Đang xử lý..."
                            else stringResource(Res.string.reset_pw_submit),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // ── TopSnackbar ──────────────────────────────────────────────────────
        TopSnackbar(
            message = errorMessage,
            onDismiss = { onIntent(ResetPasswordIntent.ErrorDismissed) },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

// ─── Realtime Password Checklist ──────────────────────────────────────────────

/**
 * Hiển thị 5 tiêu chí mật khẩu — đổi màu xanh khi đạt, đỏ khi chưa đạt.
 * Mỗi tiêu chí là một [PasswordCriteriaRow] riêng biệt.
 */
@Composable
private fun PasswordChecklist(
    strength: PasswordStrength,
    doPasswordsMatch: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PasswordCriteriaRow(
                label = stringResource(Res.string.reset_pw_checklist_min_length),
                isMet = strength.hasMinLength
            )
            PasswordCriteriaRow(
                label = stringResource(Res.string.reset_pw_checklist_uppercase),
                isMet = strength.hasUppercase
            )
            PasswordCriteriaRow(
                label = stringResource(Res.string.reset_pw_checklist_digit),
                isMet = strength.hasDigit
            )
            PasswordCriteriaRow(
                label = stringResource(Res.string.reset_pw_checklist_special),
                isMet = strength.hasSpecialChar
            )
            PasswordCriteriaRow(
                label = stringResource(Res.string.reset_pw_checklist_no_space),
                isMet = strength.hasNoWhitespace
            )
            PasswordCriteriaRow(
                label = stringResource(Res.string.reset_pw_checklist_match),
                isMet = doPasswordsMatch
            )
        }
    }
}

@Composable
private fun PasswordCriteriaRow(label: String, isMet: Boolean) {
    val color = if (isMet) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
    val icon = if (isMet) "✅" else "○"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = icon, fontSize = 13.sp)
        Text(
            text = label,
            fontSize = 13.sp,
            color = color,
            fontWeight = if (isMet) FontWeight.Medium else FontWeight.Normal
        )
    }
}

// ─── Token Expired UI ─────────────────────────────────────────────────────────

@Composable
private fun TokenExpiredContent(
    onRequestNewLink: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "⏳", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(Res.string.reset_pw_expired_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.reset_pw_expired_message),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        GradientButton(
            onClick = onRequestNewLink,
            gradient = Brush.horizontalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.primary
                )
            ),
            shape = RoundedCornerShape(12.dp),
            content = {
                Text(
                    text = stringResource(Res.string.reset_pw_expired_request_new),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        )
    }
}

/**
 * Route Composable — kết nối ViewModel với [ResetPasswordScreen].
 * Collect one-time effects và dispatch navigation callbacks.
 */
@Composable
fun ResetPasswordRoute(
    viewModel: ResetPasswordViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Collect one-time navigation effects
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ResetPasswordEffect.NavigateToLogin -> onNavigateToLogin()
                is ResetPasswordEffect.NavigateToForgotPassword -> onNavigateToForgotPassword()
                is ResetPasswordEffect.ForceLogout -> {
                    // ForceLogout: navigate về Login (AppNavGraph sẽ clear backstack)
                    onNavigateToLogin()
                }
            }
        }
    }

    ResetPasswordScreen(
        uiState = uiState,
        onIntent = viewModel::processIntent,
        onNavigateToForgotPassword = onNavigateToForgotPassword,
        modifier = modifier
    )
}
