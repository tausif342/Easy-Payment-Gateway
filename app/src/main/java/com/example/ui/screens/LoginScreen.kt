package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.example.ui.viewmodel.GatewayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: GatewayViewModel,
    modifier: Modifier = Modifier
) {
    val savedApiKey by viewModel.apiKey.collectAsState()
    val savedSecretToken by viewModel.secretToken.collectAsState()
    val savedApiUrl by viewModel.apiUrl.collectAsState()

    val customName by viewModel.customAppName.collectAsState()
    val customIconPath by viewModel.customAppIconPath.collectAsState()

    var apiKeyInput by remember { mutableStateFlowOf("") }
    var secretTokenInput by remember { mutableStateFlowOf("") }
    var apiUrlInput by remember { mutableStateFlowOf("https://api.easypaycenter.com/v1") }
    var passwordVisible by remember { mutableStateFlowOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Set initial default values if they are saved in preferences, otherwise keep clean
    LaunchedEffect(savedApiKey, savedSecretToken, savedApiUrl) {
        if (savedApiKey.isNotEmpty()) {
            apiKeyInput = savedApiKey
        }
        if (savedSecretToken.isNotEmpty()) {
            secretTokenInput = savedSecretToken
        }
        if (savedApiUrl.isNotEmpty()) {
            apiUrlInput = savedApiUrl
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF1E293B), // Slate 800
                        Color(0xFF020617)  // Slate 950
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val brandBitmap = remember(customIconPath) {
                if (customIconPath.isNotEmpty()) {
                    try {
                        android.graphics.BitmapFactory.decodeFile(customIconPath)?.asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            }

            // Header / Brand branding icon pairing
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(20.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (brandBitmap != null) {
                        Image(
                            bitmap = brandBitmap,
                            contentDescription = "Custom Brand Logo",
                            modifier = Modifier.size(56.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Shield logo",
                            tint = Color(0xFF22D3EE), // Cyan accent
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Text(
                text = if (customName.isNotEmpty()) customName else "Easy Payment SMS Gateway",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Secure Controlled Multi-Merchant Verification Node",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF94A3B8), // Slate 400
                    letterSpacing = 0.25.sp
                ),
                modifier = Modifier.padding(top = 8.dp, bottom = 40.dp),
                textAlign = TextAlign.Center
            )

            // Input fields card wrapper
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ACCOUNT ACTIVATION",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF22D3EE),
                            letterSpacing = 1.25.sp
                        ),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // API Key input
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Merchant API Key", color = Color(0xFF94A3B8)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = "Key Icon",
                                tint = Color(0xFF38BDF8)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF22D3EE),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF22D3EE)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("api_key_input"),
                        singleLine = true
                    )

                    // Secret Token input
                    OutlinedTextField(
                        value = secretTokenInput,
                        onValueChange = { secretTokenInput = it },
                        label = { Text("Secret Authorization Token", color = Color(0xFF94A3B8)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Icon",
                                tint = Color(0xFF38BDF8)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide secret" else "Show secret",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF22D3EE),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF22D3EE)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("secret_token_input"),
                        singleLine = true
                    )

                    // Target Backend URL
                    OutlinedTextField(
                        value = apiUrlInput,
                        onValueChange = { apiUrlInput = it },
                        label = { Text("Central Backend API Gateway URL", color = Color(0xFF94A3B8)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = "Server Icon",
                                tint = Color(0xFF38BDF8)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF22D3EE),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF22D3EE)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                            .testTag("api_url_input"),
                        singleLine = true
                    )

                    // Status notification feedbacks
                    AnimatedVisibility(
                        visible = uiState is GatewayViewModel.UiState.Error || uiState is GatewayViewModel.UiState.Success,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        when (val state = uiState) {
                            is GatewayViewModel.UiState.Error -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF451A03).copy(alpha = 0.8f)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFF9A3412)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Error icon",
                                            tint = Color(0xFFF97316)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = state.error,
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFDBA74)),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            is GatewayViewModel.UiState.Success -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.8f)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFF059669)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Success icon",
                                            tint = Color(0xFF34D399)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA7F3D0)),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }
                    }

                    // Main verification activate buttons
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.loginMerchant(apiKeyInput, secretTokenInput, apiUrlInput)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF22D3EE),
                            contentColor = Color(0xFF020617)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("activate_device_button"),
                        enabled = uiState !is GatewayViewModel.UiState.Loading
                    ) {
                        if (uiState is GatewayViewModel.UiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF020617),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "ACTIVATE GATEWAY DEVICE",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Convenient simulator mode bypass trigger
                    TextButton(
                        onClick = {
                            apiKeyInput = "SANDBOX_MCH_DEMO"
                            secretTokenInput = "SANDBOX_TOKEN_DEMO"
                            apiUrlInput = "DEMO"
                            viewModel.loginMerchant("SANDBOX_MCH_DEMO", "SANDBOX_TOKEN_DEMO", "DEMO")
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF22D3EE))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Demo Play Icon",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Or Quick Boot into 'DEMO' Sandbox Mode",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }

                    TextButton(
                        onClick = {
                            apiKeyInput = "EP_MCH_KEY_928374"
                            secretTokenInput = "TOK_SEC_928437AJD83"
                            apiUrlInput = "https://api.easypaycenter.com/v1"
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFA5F3FC))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Demo Keys Icon",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Fill Sandbox Test Keys Preset",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Footer device safety rule labels
            Text(
                text = "RULE: One device is strictly locked to one merchant token. Binding limits are validated automatically by the central backend logic.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF64748B),
                    lineHeight = 16.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

// Custom Helper to handle simple state assignment conveniently
fun <T> mutableStateFlowOf(value: T): MutableState<T> = mutableStateOf(value)
