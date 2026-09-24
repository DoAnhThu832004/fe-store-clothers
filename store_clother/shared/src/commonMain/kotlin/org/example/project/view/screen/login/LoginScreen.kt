package org.example.project.view.screen.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.view.components.GradientButton
import org.example.project.view.components.TopSnackbar
import org.example.project.viewmodel.LoginError
import org.example.project.viewmodel.LoginUiState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import store_clother.shared.generated.resources.Res
import store_clother.shared.generated.resources.dont_have_account
import store_clother.shared.generated.resources.error_invalid_credentials
import store_clother.shared.generated.resources.error_network_unavailable
import store_clother.shared.generated.resources.error_unknown
import store_clother.shared.generated.resources.forgot_password
import store_clother.shared.generated.resources.ic_eye
import store_clother.shared.generated.resources.ic_eyeOff
import store_clother.shared.generated.resources.login_in
import store_clother.shared.generated.resources.password
import store_clother.shared.generated.resources.please_enter_the_email
import store_clother.shared.generated.resources.please_enter_the_email_contact
import store_clother.shared.generated.resources.please_enter_the_full_name
import store_clother.shared.generated.resources.please_enter_the_password
import store_clother.shared.generated.resources.please_enter_the_phone
import store_clother.shared.generated.resources.please_enter_the_phone_contact
import store_clother.shared.generated.resources.please_enter_the_store_code
import store_clother.shared.generated.resources.please_enter_the_store_name
import store_clother.shared.generated.resources.please_enter_the_username
import store_clother.shared.generated.resources.store_name
import store_clother.shared.generated.resources.user_name

/**
 * Map [LoginError] → localized string.
 *
 * Đây là trách nhiệm duy nhất của UI layer:
 * ViewModel emit error type thuần Kotlin,
 * Composable tự map sang string resource đúng ngôn ngữ hiện tại.
 *
 * Hàm này đặt ở top-level (không phải bên trong Composable) để:
 * - Dễ test isolated nếu cần
 * - Tái sử dụng ở các screen khác
 */
@Composable
fun errorToString(error: LoginError): String = when (error) {
    LoginError.EmptyStoreCode       -> stringResource(Res.string.please_enter_the_store_code)
    LoginError.EmptyStoreName       -> stringResource(Res.string.please_enter_the_store_name)
    LoginError.EmptyUsername        -> stringResource(Res.string.please_enter_the_username)
    LoginError.EmptyPassword        -> stringResource(Res.string.please_enter_the_password)
    LoginError.EmptyFullName        -> stringResource(Res.string.please_enter_the_full_name)
    LoginError.EmptyEmail           -> stringResource(Res.string.please_enter_the_email)
    LoginError.EmptyPhone           -> stringResource(Res.string.please_enter_the_phone)
    LoginError.EmptyEmailContact    -> stringResource(Res.string.please_enter_the_email_contact)
    LoginError.EmptyPhoneContact    -> stringResource(Res.string.please_enter_the_phone_contact)
    LoginError.InvalidCredentials   -> stringResource(Res.string.error_invalid_credentials)
    LoginError.NetworkUnavailable   -> stringResource(Res.string.error_network_unavailable)
    is LoginError.Unknown           -> error.rawMessage ?: stringResource(Res.string.error_unknown)
}

/**
 * Stateless Composable UI cho màn hình Login.
 *
 * Snackbar lỗi được hiển thị ở **đầu màn hình** qua [TopSnackbar]
 * với animation slide từ phải sang trái.
 *
 * @param loginState                  Trạng thái hiện tại từ ViewModel
 * @param onUserNameChanged           Callback khi username thay đổi
 * @param onPasswordChanged           Callback khi password thay đổi
 * @param onPasswordVisibilityChanged Callback toggle hiển thị mật khẩu
 * @param onLoginClicked              Callback khi nhấn nút Đăng nhập
 * @param onErrorMessageShown         Callback sau khi TopSnackbar đã dismiss
 * @param onRegisterClicked           Callback khi nhấn "Chưa có tài khoản"
 */
@Composable
fun LoginScreen(
    loginState: LoginUiState,
    onStoreCodeChanged: (String) -> Unit,
    onUserNameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPasswordVisibilityChanged: () -> Unit,
    onLoginClicked: () -> Unit,
    onErrorShown: () -> Unit,
    onRegisterClicked: () -> Unit = {},
    onForgotPasswordClicked: () -> Unit = {}
) {
    val errorMessage = loginState.error?.let { errorToString(it) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "👗 Store Manager",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Đăng nhập vào hệ thống",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(36.dp))
            OutlinedTextField(
                value = loginState.storeCode,
                onValueChange = onStoreCodeChanged,
                label = { Text(text = stringResource(Res.string.store_name), color = MaterialTheme.colorScheme.onSurface)},
                singleLine = true,
                enabled = !loginState.isLoading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = loginState.userName,
                onValueChange = onUserNameChanged,
                label = { Text(text = stringResource(Res.string.user_name), color = MaterialTheme.colorScheme.onSurface) },
                singleLine = true,
                enabled = !loginState.isLoading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = loginState.password,
                onValueChange = onPasswordChanged,
                label = { Text(text = stringResource(Res.string.password),color = MaterialTheme.colorScheme.onSurface) },
                singleLine = true,
                enabled = !loginState.isLoading,
                trailingIcon = {
                    IconButton(onClick = onPasswordVisibilityChanged) {
                        Image(
                            painter = if (loginState.isPasswordVisible)
                                painterResource(Res.drawable.ic_eye)
                            else
                                painterResource(Res.drawable.ic_eyeOff),
                            contentDescription = if (loginState.isPasswordVisible)
                                "Ẩn mật khẩu" else "Hiện mật khẩu"
                        )
                    }
                },
                visualTransformation = if (loginState.isPasswordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        onLoginClicked()
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            )

            // ── Quên mật khẩu ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onForgotPasswordClicked) {
                    Text(
                        text = stringResource(Res.string.forgot_password),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

//            // ── Nút Đăng nhập ────────────────────────────────────────────────
//            Button(
//                onClick = {
//                    keyboardController?.hide()
//                    onLoginClicked()
//                },
//                enabled = !loginState.isLoading,
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = Color.Transparent,
//                    disabledContainerColor = Color.Transparent
//                ),
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(52.dp)
//                    .background(
//                        brush = Brush.horizontalGradient(
//                        colors = listOf(
//                            MaterialTheme.colorScheme.primaryContainer,
//                            MaterialTheme.colorScheme.primary
//                        )
//                    ), shape = RoundedCornerShape(12.dp)
//                    ),
//                shape = RoundedCornerShape(12.dp),
//            ) {
//                AnimatedVisibility(visible = loginState.isLoading) {
//                    CircularProgressIndicator(
//                        modifier = Modifier
//                            .size(20.dp)
//                            .padding(end = 8.dp),
//                        strokeWidth = 2.dp,
//                        color = MaterialTheme.colorScheme.onPrimary
//                    )
//                }
//                Text(
//                    text = if (loginState.isLoading) "Đang đăng nhập..." else stringResource(Res.string.login_in),
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.SemiBold,
//                    modifier = Modifier.padding(vertical = 2.dp)
//                )
//            }
            GradientButton(
                onClick = {
                    keyboardController?.hide()
                    onLoginClicked()
                },
                gradient = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.primary
                    )
                ),
                enabled = !loginState.isLoading,
                shape = RoundedCornerShape(12.dp),
                content = {
                    AnimatedVisibility(visible = loginState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text(
                        text = if (loginState.isLoading) "Đang đăng nhập..." else stringResource(Res.string.login_in),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            )
            Spacer(modifier = Modifier.height(12.dp))

            // ── Chưa có tài khoản ────────────────────────────────────────────
            TextButton(onClick = onRegisterClicked) {
                Text(
                    text = stringResource(Res.string.dont_have_account),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // ── TopSnackbar (hiển thị lỗi ở đầu màn hình, slide từ phải sang trái) ──
        TopSnackbar(
            message = errorMessage,
            onDismiss = onErrorShown,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}