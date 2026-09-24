package org.example.project.view.screen.register

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.view.components.GradientButton
import org.example.project.view.components.TopSnackbar
import org.example.project.view.screen.login.errorToString
import org.example.project.viewmodel.RegisterState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import store_clother.shared.generated.resources.Res
import store_clother.shared.generated.resources.contact_email
import store_clother.shared.generated.resources.contact_phone
import store_clother.shared.generated.resources.continue1
import store_clother.shared.generated.resources.ic_eye
import store_clother.shared.generated.resources.ic_eyeOff
import store_clother.shared.generated.resources.login_in
import store_clother.shared.generated.resources.owner_email
import store_clother.shared.generated.resources.owner_full_name
import store_clother.shared.generated.resources.owner_phone
import store_clother.shared.generated.resources.password
import store_clother.shared.generated.resources.store_code
import store_clother.shared.generated.resources.store_name
import store_clother.shared.generated.resources.user_name

@Composable
fun RegisterScreen(
    registerState: RegisterState,
    onStoreNameChanged: (String) -> Unit,
    onStoreCodeChanged: (String) -> Unit,
    onUserNameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPasswordVisibilityChanged: () -> Unit,
    onFullNameChanged: (String) -> Unit,
    onRegisterContactClick: () -> Unit,
    onErrorMessageShown: () -> Unit
) {
    val errorMessage = registerState.errorMessage?.let { errorToString(it) }
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
            OutlinedTextField(
                value = registerState.storeName,
                onValueChange = onStoreNameChanged,
                label = {
                    Text(
                        text = stringResource(Res.string.store_name),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = registerState.storeCode,
                onValueChange = onStoreCodeChanged,
                label = {
                    Text(
                        text = stringResource(Res.string.store_code),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = registerState.ownerUsername,
                onValueChange = onUserNameChanged,
                label = {
                    Text(
                        text = stringResource(Res.string.user_name),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = registerState.ownerPassword,
                onValueChange = onPasswordChanged,
                label = {
                    Text(
                        text = stringResource(Res.string.password),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = onPasswordVisibilityChanged
                    ) {
                        Image(
                            painter = if(registerState.isPasswordVisible) painterResource(Res.drawable.ic_eye) else painterResource(
                                Res.drawable.ic_eyeOff),
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (registerState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = registerState.ownerFullName,
                onValueChange = onFullNameChanged,
                label = {
                    Text(
                        text = stringResource(Res.string.owner_full_name),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            GradientButton(
                onClick = {
                    keyboardController?.hide()
                    onRegisterContactClick()
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
                    Text(
                        text = stringResource(Res.string.continue1),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            )
        }
        TopSnackbar(
            message = errorMessage,
            onDismiss = onErrorMessageShown,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}