package org.example.project.features.auth.forgot

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import org.example.project.view.components.GradientButton
import org.example.project.view.components.TopSnackbar
import org.jetbrains.compose.resources.stringResource
import store_clother.shared.generated.resources.Res
import store_clother.shared.generated.resources.forgot_pw_back_to_login
import store_clother.shared.generated.resources.forgot_pw_email_hint
import store_clother.shared.generated.resources.forgot_pw_email_label
import store_clother.shared.generated.resources.forgot_pw_rate_limit_message
import store_clother.shared.generated.resources.forgot_pw_rate_limit_title
import store_clother.shared.generated.resources.forgot_pw_store_code_hint
import store_clother.shared.generated.resources.forgot_pw_store_code_label
import store_clother.shared.generated.resources.forgot_pw_submit
import store_clother.shared.generated.resources.forgot_pw_subtitle
import store_clother.shared.generated.resources.forgot_pw_success_message
import store_clother.shared.generated.resources.forgot_pw_success_title
import store_clother.shared.generated.resources.forgot_pw_title
import store_clother.shared.generated.resources.forgot_pw_validation_email_blank
import store_clother.shared.generated.resources.forgot_pw_validation_email_invalid
import store_clother.shared.generated.resources.forgot_pw_validation_store_code_blank

/**
 * Stateless Composable cho màn hình Quên Mật Khẩu.
 *
 * Tất cả state đến từ [uiState], tất cả action emit ra qua [onIntent].
 * Không có logic business bên trong Composable này.
 */
@Composable
fun ForgotPasswordScreen(
    uiState: ForgotPasswordUiState,
    onIntent: (ForgotPasswordIntent) -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Map ValidationError → String resource (UI responsibility)
    val errorMessage: String? = when (val state = uiState.screenState) {
        is ForgotPasswordUiState.ScreenState.ValidationError -> when (state.error) {
            ForgotPasswordValidationError.EmailBlank   -> stringResource(Res.string.forgot_pw_validation_email_blank)
            ForgotPasswordValidationError.EmailInvalid -> stringResource(Res.string.forgot_pw_validation_email_invalid)
            ForgotPasswordValidationError.StoreCodeBlank -> stringResource(Res.string.forgot_pw_validation_store_code_blank)
        }
        is ForgotPasswordUiState.ScreenState.NetworkError -> state.message
        else -> null
    }

    val isSuccess = uiState.screenState == ForgotPasswordUiState.ScreenState.Success
    val isRateLimited = uiState.screenState == ForgotPasswordUiState.ScreenState.RateLimitError

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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

            // ── Back button ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(onClick = onBackToLogin) {
                    Text(
                        text = "← ${stringResource(Res.string.forgot_pw_back_to_login)}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Header ───────────────────────────────────────────────────────
            Text(
                text = "🔐 ${stringResource(Res.string.forgot_pw_title)}",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.forgot_pw_subtitle),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Rate Limit Banner (429) ──────────────────────────────────────
            AnimatedVisibility(
                visible = isRateLimited,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⏰ ${stringResource(Res.string.forgot_pw_rate_limit_title)}",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.forgot_pw_rate_limit_message),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // ── Success Card ─────────────────────────────────────────────────
            AnimatedVisibility(
                visible = isSuccess,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "📧", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(Res.string.forgot_pw_success_title),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(Res.string.forgot_pw_success_message),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // ── Form (ẩn khi success) ────────────────────────────────────────
            AnimatedVisibility(visible = !isSuccess) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // Email field
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = { onIntent(ForgotPasswordIntent.EmailChanged(it)) },
                        label = { Text(stringResource(Res.string.forgot_pw_email_label)) },
                        placeholder = { Text(stringResource(Res.string.forgot_pw_email_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Store Code field
                    OutlinedTextField(
                        value = uiState.storeCode,
                        onValueChange = { onIntent(ForgotPasswordIntent.StoreCodeChanged(it)) },
                        label = { Text(stringResource(Res.string.forgot_pw_store_code_label)) },
                        placeholder = { Text(stringResource(Res.string.forgot_pw_store_code_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Submit button
                    GradientButton(
                        onClick = { onIntent(ForgotPasswordIntent.SubmitClicked) },
                        gradient = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primary
                            )
                        ),
                        enabled = !uiState.isLoading,
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
                                text = if (uiState.isLoading) "Đang gửi..." else stringResource(Res.string.forgot_pw_submit),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── TopSnackbar cho Network/Validation errors ────────────────────────
        TopSnackbar(
            message = errorMessage,
            onDismiss = { onIntent(ForgotPasswordIntent.ErrorDismissed) },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/**
 * Route Composable — kết nối ViewModel với [ForgotPasswordScreen].
 *
 * Collect [ForgotPasswordViewModel.uiState] và forward events qua [processIntent].
 * Navigation callbacks được inject từ NavGraph.
 */
@Composable
fun ForgotPasswordRoute(
    viewModel: ForgotPasswordViewModel,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // One-time effects (hiện tại chưa cần navigate từ screen này)
    LaunchedEffect(Unit) {
        viewModel.effects.collect { /* future effects */ }
    }

    ForgotPasswordScreen(
        uiState = uiState,
        onIntent = viewModel::processIntent,
        onBackToLogin = onBackToLogin,
        modifier = modifier
    )
}
