package org.example.project.view.screen.register

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.view.components.AdaptiveAuthLayout
import org.example.project.view.components.GradientButton
import org.example.project.view.components.TopSnackbar
import org.example.project.view.screen.login.errorToString
import org.example.project.viewmodel.RegisterState
import org.jetbrains.compose.resources.stringResource
import store_clother.shared.generated.resources.Res
import store_clother.shared.generated.resources.contact_email
import store_clother.shared.generated.resources.contact_phone
import store_clother.shared.generated.resources.owner_email
import store_clother.shared.generated.resources.owner_phone

@Composable
fun RegisterContactScreen(
    registerState: RegisterState,
    onContactEmailChanged: (String) -> Unit,
    onContactPhoneChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onErrorMessageShown: () -> Unit
) {
    val errorMessage = registerState.errorMessage?.let { errorToString(it) }
    val keyboardController = LocalSoftwareKeyboardController.current

    AdaptiveAuthLayout(
        heroTitle = "Thông tin liên hệ",
        heroSubtitle = "Thiết lập kênh liên lạc và xác thực bảo mật để hoàn tất kích hoạt cửa hàng của bạn",
        maxWidthDesktopCard = 520.dp,
        topSnackbar = {
            TopSnackbar(
                message = errorMessage,
                onDismiss = onErrorMessageShown
            )
        }
    ) { isWideScreen ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isWideScreen) "Thông tin liên hệ cửa hàng" else "📞 Liên hệ & Xác thực",
                fontSize = if (isWideScreen) 24.sp else 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Bước 2/2: Kênh liên lạc phục vụ khách hàng & quản trị",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = registerState.contactEmail,
                onValueChange = onContactEmailChanged,
                label = {
                    Text(
                        text = stringResource(Res.string.contact_email),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = registerState.contactPhone,
                onValueChange = onContactPhoneChanged,
                label = {
                    Text(
                        text = stringResource(Res.string.contact_phone),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                supportingText = {
                    Text(
                        text = "VD: 0912345678 (10 số, đầu 0 hoặc +84)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Phone
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = registerState.ownerEmail,
                onValueChange = onEmailChanged,
                label = {
                    Text(
                        text = stringResource(Res.string.owner_email),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = registerState.ownerPhone,
                onValueChange = onPhoneChanged,
                label = {
                    Text(
                        text = stringResource(Res.string.owner_phone),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                supportingText = {
                    Text(
                        text = "VD: 0987654321 (10 số, đầu 0 hoặc +84)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Phone
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            )
            Spacer(modifier = Modifier.height(20.dp))

            GradientButton(
                onClick = {
                    keyboardController?.hide()
                    onRegisterClick()
                },
                gradient = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.primary
                    )
                ),
                enabled = !registerState.isLoading,
                shape = RoundedCornerShape(12.dp),
                content = {
                    AnimatedVisibility(visible = registerState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text(
                        text = if (registerState.isLoading) "Đang kích hoạt..." else "Hoàn tất đăng ký",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            )
        }
    }
}