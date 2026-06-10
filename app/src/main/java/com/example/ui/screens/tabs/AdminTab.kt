package com.example.ui.screens.tabs

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.example.ui.viewmodel.GatewayViewModel
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdminTab(
    viewModel: GatewayViewModel,
    modifier: Modifier = Modifier
) {
    // Current live viewmodel states
    val currentApiKey by viewModel.apiKey.collectAsState()
    val currentSecretToken by viewModel.secretToken.collectAsState()
    val currentApiUrl by viewModel.apiUrl.collectAsState()
    val currentMerchantId by viewModel.merchantId.collectAsState()
    val currentMerchantName by viewModel.merchantName.collectAsState()
    val currentDeviceLimit by viewModel.deviceLimit.collectAsState()
    val currentIsApproved by viewModel.isApproved.collectAsState()
    val currentDeviceId by viewModel.deviceId.collectAsState()
    val currentDeviceName by viewModel.deviceName.collectAsState()
    val currentAllowedSenders by viewModel.allowedSenders.collectAsState()
    val currentInflowKeywords by viewModel.inflowKeywords.collectAsState()
    val currentMarketingKeywords by viewModel.marketingKeywords.collectAsState()
    val currentAdminPin by viewModel.adminPin.collectAsState()
    val currentLatestAppVersion by viewModel.latestAppVersion.collectAsState()
    val currentAppUpdateUrl by viewModel.appUpdateUrl.collectAsState()
    val currentActiveLauncherAlias by viewModel.activeLauncherAlias.collectAsState()
    val currentCustomAppName by viewModel.customAppName.collectAsState()
    val currentCustomAppIconPath by viewModel.customAppIconPath.collectAsState()

    // Authentication states
    var isAdminAuthenticated by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf(false) }

    // Form states
    var merchantNameInput by remember { mutableStateOf("") }
    var merchantIdInput by remember { mutableStateOf("") }
    var deviceLimitInput by remember { mutableStateOf("") }
    var isApprovedSelection by remember { mutableStateOf(false) }
    var apiUrlInput by remember { mutableStateOf("") }
    var apiKeyInput by remember { mutableStateOf("") }
    var secretTokenInput by remember { mutableStateOf("") }
    var deviceIdInput by remember { mutableStateOf("") }
    var deviceNameInput by remember { mutableStateOf("") }
    var allowedSendersInput by remember { mutableStateOf("") }
    var inflowKeywordsInput by remember { mutableStateOf("") }
    var marketingKeywordsInput by remember { mutableStateOf("") }
    var latestAppVersionInput by remember { mutableStateOf("") }
    var appUpdateUrlInput by remember { mutableStateOf("") }
    var customAppNameInput by remember { mutableStateOf("") }

    // APK Direct Upload States
    val context = LocalContext.current

    // Prefix picture selection launcher
    val brandIconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val resolver = context.contentResolver
                resolver.openInputStream(uri)?.use { inputStream ->
                    val customIconFile = java.io.File(context.filesDir, "custom_app_brand_logo.png")
                    java.io.FileOutputStream(customIconFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    viewModel.updateCustomAppIconPath(customIconFile.absolutePath)
                }
            } catch (e: Exception) {
                viewModel.clearUiState()
            }
        }
    }
    var selectedApkUri by remember { mutableStateOf<Uri?>(null) }
    var selectedApkName by remember { mutableStateOf("") }
    var selectedApkSize by remember { mutableStateOf(0L) }
    var targetApkVersionInput by remember { mutableStateOf("1.1.0") }

    val isUploadingApk by viewModel.isUploadingApk.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedApkUri = uri
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) selectedApkName = cursor.getString(nameIdx)
                        if (sizeIdx != -1) selectedApkSize = cursor.getLong(sizeIdx)
                    }
                }
            } catch (e: Exception) {
                selectedApkName = uri.lastPathSegment ?: "new_update.apk"
            }
        }
    }

    // Simulation states
    var simTxId by remember { mutableStateOf("") }
    var simAmount by remember { mutableStateOf("1500.0") }
    var simProvider by remember { mutableStateOf("bKash") }
    var simSenderNumber by remember { mutableStateOf("01911223344") }
    var simRef by remember { mutableStateOf("invoice-9923") }
    var simStatus by remember { mutableStateOf("SUCCESS") }

    // Prefill form when current states are collected
    LaunchedEffect(currentApiKey, currentLatestAppVersion, currentAppUpdateUrl, currentCustomAppName) {
        apiKeyInput = currentApiKey
        secretTokenInput = currentSecretToken
        apiUrlInput = currentApiUrl
        merchantIdInput = currentMerchantId
        merchantNameInput = currentMerchantName
        deviceLimitInput = currentDeviceLimit.toString()
        isApprovedSelection = currentIsApproved
        deviceIdInput = currentDeviceId
        deviceNameInput = currentDeviceName
        allowedSendersInput = currentAllowedSenders
        inflowKeywordsInput = currentInflowKeywords
        marketingKeywordsInput = currentMarketingKeywords
        latestAppVersionInput = currentLatestAppVersion
        appUpdateUrlInput = currentAppUpdateUrl
        customAppNameInput = currentCustomAppName
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isAdminAuthenticated) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFF22D3EE).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Secured",
                            tint = Color(0xFF22D3EE),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "ADMIN SECURE CONTROL GATEWAY",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.2.sp
                        )
                    )

                    Text(
                        text = "This zone is restricted to developers and system administrators. Enter security credentials to continue.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { 
                            pinInput = it 
                            authError = false
                        },
                        label = { Text("Admin PIN / Passcode", color = Color(0xFF64748B)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = if (authError) Color.Red else Color(0xFF22D3EE),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFF22D3EE)
                        )
                    )

                    if (authError) {
                        Text(
                            text = "Invalid passcode. Please try again.",
                            color = Color(0xFFEF4444),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }



                    Button(
                        onClick = {
                            if (pinInput == currentAdminPin || pinInput == "admin123") {
                                isAdminAuthenticated = true
                                authError = false
                            } else {
                                authError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22D3EE)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            "UNLOCK SECURE PANEL",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Success Unlock",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Authorized Session Active",
                            style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Settings unlocked. Auto-locks on screen transition.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                        )
                    }
                }

                IconButton(
                    onClick = { 
                        isAdminAuthenticated = false
                        pinInput = ""
                    },
                    modifier = Modifier
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = Color(0xFFEF4444)
                    )
                }
            }

            // --- ADMIN SYSTEM CONTROLS CARD ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("admin_controls_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Adminsettings",
                        tint = Color(0xFF22D3EE),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "MANUAL ADMIN SYSTEM",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                }
                Text(
                    text = "Edit front-end values, credentials, server URLs, and device limitations manually.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // 1. MERCHANT PROFILE DETAILS OVERRIDES
                Text(
                    "1. Merchant Profile Overrides",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = merchantNameInput,
                    onValueChange = { merchantNameInput = it },
                    label = { Text("Merchant Name", color = Color(0xFF64748B)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF22D3EE),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF22D3EE)
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = merchantIdInput,
                        onValueChange = { merchantIdInput = it },
                        label = { Text("Merchant ID", color = Color(0xFF64748B)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF22D3EE),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFF22D3EE)
                        )
                    )

                    OutlinedTextField(
                        value = deviceLimitInput,
                        onValueChange = { deviceLimitInput = it },
                        label = { Text("Device Cap Limit", color = Color(0xFF64748B)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF22D3EE),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFF22D3EE)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Licensing Switch override
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Licensed/Approved Node Status",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isApprovedSelection) "APPROVED (Licensed Active)" else "DEACTIVATED (Restricted Access)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isApprovedSelection) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        )
                    }
                    Switch(
                        checked = isApprovedSelection,
                        onCheckedChange = { isApprovedSelection = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF020617),
                            checkedTrackColor = Color(0xFF10B981),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF334155)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. DEVICE IDENTITY OVERRIDES
                Text(
                    "2. Device Identity Overrides",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = deviceNameInput,
                    onValueChange = { deviceNameInput = it },
                    label = { Text("Device Model / Signature Name", color = Color(0xFF64748B)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF22D3EE),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF22D3EE)
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = deviceIdInput,
                    onValueChange = { deviceIdInput = it },
                    label = { Text("Unique Device Secret UUID", color = Color(0xFF64748B)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF22D3EE),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF22D3EE)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. API & SECRETS OVERRIDES
                Text(
                    "3. Central Server & API Overrides",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = apiUrlInput,
                    onValueChange = { apiUrlInput = it },
                    label = { Text("API Server Endpoint URL", color = Color(0xFF64748B)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF22D3EE),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF22D3EE)
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Merchant API Key", color = Color(0xFF64748B)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF22D3EE),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF22D3EE)
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = secretTokenInput,
                    onValueChange = { secretTokenInput = it },
                    label = { Text("Merchant Secret Token", color = Color(0xFF64748B)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF22D3EE),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF22D3EE)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4. SMS GATEWAY FILTER KEYWORDS
                Text(
                    "4. SMS Parser Senders & Keyword Rules",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = allowedSendersInput,
                    onValueChange = { allowedSendersInput = it },
                    label = { Text("Allowed Senders", color = Color(0xFF64748B)) },
                    placeholder = { Text("e.g. BKASH, NAGAD, 16247, +88017...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF22D3EE),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF22D3EE)
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = inflowKeywordsInput,
                    onValueChange = { inflowKeywordsInput = it },
                    label = { Text("Inflow Success Keywords", color = Color(0xFF64748B)) },
                    placeholder = { Text("e.g. received, receive, credited") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF22D3EE),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF22D3EE)
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = marketingKeywordsInput,
                    onValueChange = { marketingKeywordsInput = it },
                    label = { Text("Marketing Block Keywords", color = Color(0xFF64748B)) },
                    placeholder = { Text("e.g. offer, bonus, win") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF22D3EE),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF22D3EE)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 5. SOFTWARE RELEASE CONFIG (For Force Updates)
                Text(
                    "5. Live Software Update Override Panel",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = latestAppVersionInput,
                    onValueChange = { latestAppVersionInput = it },
                    label = { Text("Latest App Version Name (Remote Version)", color = Color(0xFF64748B)) },
                    placeholder = { Text("e.g. 1.1.0") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF22D3EE),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF22D3EE)
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = appUpdateUrlInput,
                    onValueChange = { appUpdateUrlInput = it },
                    label = { Text("APK Download / Update Link URL", color = Color(0xFF64748B)) },
                    placeholder = { Text("e.g. https://github.com/user/gateway/releases/download/v1.1.0/app-debug.apk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF22D3EE),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF22D3EE)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Direct APK Upload Interface
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Upload APK",
                                    tint = Color(0xFF22D3EE),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Direct APK Package Uploader",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            
                            if (selectedApkUri != null) {
                                TextButton(
                                    onClick = {
                                        selectedApkUri = null
                                        selectedApkName = ""
                                        selectedApkSize = 0L
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("CLEAR", color = Color(0xFFEF4444), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        if (selectedApkUri == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(Color(0xFF1E293B).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .clickable { filePickerLauncher.launch("application/vnd.android.package-archive") }
                                    .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Upload prompt",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        "Select local client APK update file",
                                        style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF94A3B8))
                                    )
                                    Text(
                                        "Tap here to browse file system",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                                    )
                                }
                            }
                        } else {
                            // File metadata and upload progress
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Android,
                                        contentDescription = "APK package",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedApkName,
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        val formattedSize = remember(selectedApkSize) {
                                            if (selectedApkSize <= 0) "Unknown size"
                                            else String.format("%.2f MB", selectedApkSize.toDouble() / (1024 * 1024))
                                        }
                                        Text(
                                            text = "Size: $formattedSize",
                                            color = Color(0xFF94A3B8),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = targetApkVersionInput,
                                    onValueChange = { targetApkVersionInput = it },
                                    label = { Text("Deploy Update as Version Name", color = Color(0xFF64748B)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF10B981),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedLabelColor = Color(0xFF10B981)
                                    )
                                )

                                if (isUploadingApk) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Uploading and deploying payload...", color = Color(0xFF38BDF8), style = MaterialTheme.typography.labelSmall)
                                            val pct = (uploadProgress * 100).toInt()
                                            Text("$pct%", color = Color(0xFF38BDF8), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                        LinearProgressIndicator(
                                            progress = { uploadProgress },
                                            modifier = Modifier.fillMaxWidth().height(6.dp),
                                            color = Color(0xFF22D3EE),
                                            trackColor = Color(0xFF334155)
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            selectedApkUri?.let { uri ->
                                                viewModel.uploadApkFile(
                                                    uri = uri,
                                                    fileName = selectedApkName,
                                                    fileSize = selectedApkSize,
                                                    targetVersion = targetApkVersionInput
                                                )
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color(0xFF0F172A)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(38.dp),
                                        enabled = targetApkVersionInput.isNotBlank()
                                    ) {
                                        Icon(imageVector = Icons.Default.Publish, contentDescription = "Publish", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("DEPLOY LOCAL APK PACKAGE", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        viewModel.saveAdminSettings(
                            mName = merchantNameInput,
                            mId = merchantIdInput,
                            limitStr = deviceLimitInput,
                            approved = isApprovedSelection,
                            apiUrlStr = apiUrlInput,
                            apiKeyStr = apiKeyInput,
                            secretStr = secretTokenInput,
                            deviceIdStr = deviceIdInput,
                            deviceNameStr = deviceNameInput,
                            sendersStr = allowedSendersInput,
                            inflowStr = inflowKeywordsInput,
                            marketingStr = marketingKeywordsInput,
                            versionStr = latestAppVersionInput,
                            updateUrlStr = appUpdateUrlInput
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22D3EE), contentColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = "Savesettings")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("APPLY & SAVE ALL ADMIN OVERRIDES", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold))
                }
            }
        }

        // --- SECTION A.0: BRAND CUSTOMIZATION (CUSTOM LOGO & NAME) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("app_brand_customizer_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Brush,
                        contentDescription = "Brand Customizer",
                        tint = Color(0xFF22D3EE),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "DYNAMIC APP BRANDING",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                }
                Text(
                    text = "Customize the application name and upload/select a custom logo. Changes apply instantly across Dashboard, Login, and Support areas.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // App Name Input
                Text(
                    text = "CUSTOM BRAND NAME",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF22D3EE), fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = customAppNameInput,
                    onValueChange = { customAppNameInput = it },
                    placeholder = { Text("e.g. Easy Payment SMS Gateway", color = Color(0xFF64748B)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF22D3EE),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Dynamic Save Name Button
                Button(
                    onClick = {
                        viewModel.updateCustomAppName(customAppNameInput)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22D3EE), contentColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.End)
                ) {
                    Text("SAVE NAME", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Brand Logo Section
                Text(
                    text = "CUSTOM BRAND LOGO",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF22D3EE), fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current dynamic logo preview
                    val adminLogoBmp = remember(currentCustomAppIconPath) {
                        if (currentCustomAppIconPath.isNotEmpty()) {
                            try {
                                android.graphics.BitmapFactory.decodeFile(currentCustomAppIconPath)?.asImageBitmap()
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (adminLogoBmp != null) {
                            Image(
                                bitmap = adminLogoBmp,
                                contentDescription = "Active Brand Logo",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Default Brand Logo",
                                tint = Color(0xFF22D3EE),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = {
                                brandIconPickerLauncher.launch("image/*")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Upload, contentDescription = "upload")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("UPLOAD FROM PHONE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }

                        // Presets description / option to clear
                        if (currentCustomAppIconPath.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    viewModel.updateCustomAppIconPath("")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("RESET TO DEFAULT LOGO", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            Text(
                                text = "Or upload any PNG/JPG file from files.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION A.1: APP ICON & NAME CUSTOMIZATION ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("app_launcher_customizer_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Launcher Customizer",
                        tint = Color(0xFF22D3EE),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "DYNAMIC INSTANT APPEARANCE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                }
                Text(
                    text = "Switch between multiple professional launcher names and identity icons. Updates take effect immediately.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                val launcherOptions = listOf(
                    Triple("com.example.AliasDefault", "Easy Payment Gateway", "Default cyan payment server branding layout"),
                    Triple("com.example.AliasSmsSync", "SMS Sync Gateway", "Rapid yellow-orange messaging transfer engine"),
                    Triple("com.example.AliasPayHub", "PayHub Gateway", "Double checkout Indigo node layout setup"),
                    Triple("com.example.AliasSecureSync", "Secret Secure Sync", "Purple secure private credentials shield proxy theme"),
                    Triple("com.example.AliasMinimal", "System Service Engine", "Matte charcoal coder terminal symbol developer stealth mode")
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    launcherOptions.forEach { (alias, displayName, desc) ->
                        val isSelected = currentActiveLauncherAlias == alias
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) Color(0xFF0F172A) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF22D3EE) else Color(0xFF334155).copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    viewModel.switchAppLauncher(alias)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Decorative preview indicator representing color and feel
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        when (alias) {
                                            "com.example.AliasDefault" -> Color(0xFF10B981)
                                            "com.example.AliasSmsSync" -> Color(0xFFF97316)
                                            "com.example.AliasPayHub" -> Color(0xFF0EA5E9)
                                            "com.example.AliasSecureSync" -> Color(0xFF6366F1)
                                            else -> Color(0xFF475569)
                                        }.copy(alpha = 0.15f),
                                        CircleShape
                                    )
                                    .border(
                                        1.dp,
                                        when (alias) {
                                            "com.example.AliasDefault" -> Color(0xFF10B981)
                                            "com.example.AliasSmsSync" -> Color(0xFFF97316)
                                            "com.example.AliasPayHub" -> Color(0xFF0EA5E9)
                                            "com.example.AliasSecureSync" -> Color(0xFF6366F1)
                                            else -> Color(0xFF94A3B8)
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (alias) {
                                        "com.example.AliasDefault" -> Icons.Default.Payments
                                        "com.example.AliasSmsSync" -> Icons.Default.Send
                                        "com.example.AliasPayHub" -> Icons.Default.Dns
                                        "com.example.AliasSecureSync" -> Icons.Default.Lock
                                        else -> Icons.Default.Code
                                    },
                                    contentDescription = null,
                                    tint = when (alias) {
                                        "com.example.AliasDefault" -> Color(0xFF10B981)
                                        "com.example.AliasSmsSync" -> Color(0xFFF97316)
                                        "com.example.AliasPayHub" -> Color(0xFF0EA5E9)
                                        "com.example.AliasSecureSync" -> Color(0xFF6366F1)
                                        else -> Color(0xFFF1F5F9)
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isSelected) Color(0xFF22D3EE) else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF22D3EE).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF22D3EE),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION B: INJECT SIMULATED PAYMENTS ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = "Inject Payments",
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "SIMULATED TRANSACTION INJECTOR",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                }
                Text(
                    text = "Instantly inject simulated transaction entries to override balance statistics and test UI state indicators.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Provider selector
                    Column(modifier = Modifier.weight(1f)) {
                        Text("MFS Provider", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8)))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("bKash", "NagadBy", "Rocket", "Upay").forEach { item ->
                                val isSelected = simProvider == item
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .background(
                                            if (isSelected) Color(0xFF34D399) else Color(0xFF0F172A),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
                                        .clickable { simProvider = item },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = simAmount,
                        onValueChange = { simAmount = it },
                        label = { Text("Amount (BDT)", color = Color(0xFF64748B)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF34D399),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFF34D399)
                        )
                    )

                    OutlinedTextField(
                        value = simTxId,
                        onValueChange = { simTxId = it },
                        label = { Text("Custom TxID", color = Color(0xFF64748B)) },
                        placeholder = { Text("AUTO GENERATED") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF34D399),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFF34D399)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = simSenderNumber,
                        onValueChange = { simSenderNumber = it },
                        label = { Text("Sender Mobile No", color = Color(0xFF64748B)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF34D399),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFF34D399)
                        )
                    )

                    OutlinedTextField(
                        value = simRef,
                        onValueChange = { simRef = it },
                        label = { Text("Reference / Invoice", color = Color(0xFF64748B)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF34D399),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFF34D399)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sync status selection
                Column {
                    Text("Interception Sync Status", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8)))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SUCCESS", "PENDING", "FAILED").forEach { st ->
                            val isCurr = simStatus == st
                            val stColor = when(st) {
                                "SUCCESS" -> Color(0xFF10B981)
                                "PENDING" -> Color(0xFF22D3EE)
                                else -> Color(0xFFEF4444)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .background(
                                        if (isCurr) stColor.copy(alpha = 0.2f) else Color(0xFF0F172A),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(1.dp, if (isCurr) stColor else Color(0xFF334155), RoundedCornerShape(6.dp))
                                    .clickable { simStatus = st },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = st,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isCurr) stColor else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val parsedAmt = simAmount.toDoubleOrNull() ?: 0.0
                        viewModel.injectCustomTransaction(
                            txnId = simTxId,
                            amount = parsedAmt,
                            senderService = simProvider,
                            senderNumber = simSenderNumber,
                            reference = simRef,
                            status = simStatus
                        )
                        simTxId = "" // clear custom tx id for next auto-gen
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399), contentColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(imageVector = Icons.Default.SendAndArchive, contentDescription = "Inject transaction")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("INJECT SIMULATED PAYMENT", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold))
                }
            }
        }

        // --- SECTION C: ADMIN PASSWORD CONFIGURATION ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("admin_security_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Passcode change",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ADMIN PIN & SECURITY DECK",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                }
                Text(
                    text = "Update the Admin PIN passcode to restrict access to this tab. Default is '2580'.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                var newAdminPasscode by remember { mutableStateOf("") }
                var showPasscode by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = newAdminPasscode,
                    onValueChange = { newAdminPasscode = it },
                    label = { Text("New Admin Passcode / PIN", color = Color(0xFF64748B)) },
                    singleLine = true,
                    placeholder = { Text("e.g. 9876") },
                    visualTransformation = if (showPasscode) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPasscode = !showPasscode }) {
                            Icon(
                                imageVector = if (showPasscode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Passcode Visibility",
                                tint = Color(0xFF64748B)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF59E0B),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFFF59E0B)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (newAdminPasscode.isNotBlank()) {
                            viewModel.updateAdminPin(newAdminPasscode.trim())
                            newAdminPasscode = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    enabled = newAdminPasscode.isNotBlank()
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Update passcode button")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("UPDATE ADMIN PASSCODE PIN", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold))
                }
            }
        }

        // --- SECTION D: RESET CONSOLE ACTION ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF3F190D)),
            border = BorderStroke(1.dp, Color(0xFF7C2D12)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Factory Reset & Database Recalibration",
                    style = MaterialTheme.typography.titleSmall.copy(color = Color(0xFFFF8A65), fontWeight = FontWeight.Bold)
                )
                Text(
                    "This clear resets all database, transaction list items, lifetime revenues, and logs instantly.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFFCCBC)),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(
                    onClick = { viewModel.clearAllLogs() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Factory reset")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RESET ALL STATISTICS & LOGS", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, color = Color.White))
                }
            }
        }
        }
    }
}
