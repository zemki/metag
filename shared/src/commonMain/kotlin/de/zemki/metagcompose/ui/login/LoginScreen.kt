package de.zemki.metagcompose.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import de.zemki.metagcompose.resources.Res
import de.zemki.metagcompose.resources.*
import de.zemki.metagcompose.data.model.LoginResponse
import de.zemki.metagcompose.ui.theme.MetagColors
import de.zemki.metagcompose.util.openCamera
import de.zemki.metagcompose.util.DeepLinkHandler
import de.zemki.metagcompose.util.AppLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (LoginResponse) -> Unit = {}
) {
    val uiState = viewModel.uiState
    var passwordVisible by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(uiState.loginResponse) {
        uiState.loginResponse?.let {
            onLoginSuccess(it)
        }
    }

    val tokenVersion = remember { mutableStateOf(DeepLinkHandler.getTokenVersion()) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        while (true) {
            val currentVersion = DeepLinkHandler.getTokenVersion()
            if (currentVersion != tokenVersion.value) {
                AppLogger.d("Token version changed from ${tokenVersion.value} to $currentVersion", tag = "LoginScreen")
                tokenVersion.value = currentVersion
                viewModel.checkForQrToken()
            }
            kotlinx.coroutines.delay(500)
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F7FA),
                        Color(0xFFFFFFFF),
                        Color(0xFFF5F7FA)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.ime)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = "MeTag Logo",
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MetagColors.Primary,
                fontSize = 32.sp
            )

            Text(
                text = stringResource(Res.string.login_welcome),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(top = 8.dp)
            )

            if (!uiState.isOnline) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Offline",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(Res.string.offline_indicator),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF991B1B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = MetagColors.Primary.copy(alpha = 0.1f),
                        spotColor = MetagColors.Primary.copy(alpha = 0.1f)
                    ),
                shape = RoundedCornerShape(24.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = viewModel::updateEmail,
                        label = { Text(stringResource(Res.string.login_email)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = MetagColors.Primary.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MetagColors.Primary,
                            focusedLabelColor = MetagColors.Primary,
                            focusedLeadingIconColor = MetagColors.Primary,
                            cursorColor = MetagColors.Primary,
                            unfocusedBorderColor = Color(0xFFE5E7EB),
                            focusedTextColor = Color(0xFF111827),  // Dark gray for better contrast
                            unfocusedTextColor = Color(0xFF111827)  // Dark gray for better contrast
                        )
                    )

                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = viewModel::updatePassword,
                        label = { Text(stringResource(Res.string.login_password)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MetagColors.Primary.copy(alpha = 0.6f)
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible }
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) stringResource(Res.string.hide_password) else stringResource(Res.string.show_password),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading,
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MetagColors.Primary,
                            focusedLabelColor = MetagColors.Primary,
                            focusedLeadingIconColor = MetagColors.Primary,
                            cursorColor = MetagColors.Primary,
                            unfocusedBorderColor = Color(0xFFE5E7EB),
                            focusedTextColor = Color(0xFF111827),  // Dark gray for better contrast
                            unfocusedTextColor = Color(0xFF111827)  // Dark gray for better contrast
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Remember email checkbox
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(
                                enabled = !uiState.isLoading
                            ) {
                                viewModel.toggleRememberEmail(!uiState.rememberEmail)
                            }
                        ) {
                            Checkbox(
                                checked = uiState.rememberEmail,
                                onCheckedChange = { viewModel.toggleRememberEmail(it) },
                                enabled = !uiState.isLoading,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MetagColors.Primary,
                                    uncheckedColor = Color(0xFF9CA3AF)
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Remember email",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B7280)
                            )
                        }

                        Text(
                            text = stringResource(Res.string.login_forgot_password),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MetagColors.Primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable {
                                    uriHandler.openUri("https://mesoftware.org/metag/password/reset")
                                }
                                .padding(4.dp)
                        )
                    }

                    Button(
                        onClick = { viewModel.login() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MetagColors.Primary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    stringResource(Res.string.login_button),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 1.dp,
                    color = Color(0xFFE5E7EB)
                )
                Text(
                    text = stringResource(Res.string.login_or),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 1.dp,
                    color = Color(0xFFE5E7EB)
                )
            }

            OutlinedButton(
                onClick = { openCamera() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MetagColors.Primary
                ),
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    MetagColors.Primary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        stringResource(Res.string.login_scan_qr),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF991B1B),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
