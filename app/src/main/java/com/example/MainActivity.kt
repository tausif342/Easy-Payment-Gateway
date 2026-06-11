package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GatewayViewModel

import android.util.Log
import androidx.compose.material.icons.filled.SystemUpdate
import kotlinx.coroutines.launch
import android.os.PowerManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Notifications

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: GatewayViewModel = viewModel()
                val context = LocalContext.current

                // 1. Dynamic permission statuses states
                var hasSmsPermissions by remember {
                    mutableStateOf(hasMandatorySmsPermissions(context))
                }
                var hasBatteryExemption by remember {
                    mutableStateOf(hasBatteryExemption(context))
                }
                var hasNotificationPermission by remember {
                    mutableStateOf(hasNotificationPermission(context))
                }

                // Dynamically update statuses whenever app is resumed from background
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasSmsPermissions = hasMandatorySmsPermissions(context)
                            hasBatteryExemption = hasBatteryExemption(context)
                            hasNotificationPermission = hasNotificationPermission(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                val permissionsToRequest = remember {
                    val list = mutableListOf(
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_SMS
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        list.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    list.toTypedArray()
                }

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    hasSmsPermissions = hasMandatorySmsPermissions(context)
                    hasNotificationPermission = hasNotificationPermission(context)
                }

                // Auto prompt permissions on launch on first view layout side effect
                LaunchedEffect(Unit) {
                    if (!hasSmsPermissions || !hasNotificationPermission) {
                        launcher.launch(permissionsToRequest)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF0F172A) // Slate 900
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Observe Session state to load appropriate node viewports
                        val isLoggedIn by viewModel.isLoggedIn.collectAsState()

                        // SERVER CONTROLLED DEVICE ACCESS: Verify authorization at startup when logged in
                        LaunchedEffect(isLoggedIn) {
                            if (isLoggedIn) {
                                viewModel.checkMerchantStatus()
                            }
                        }

                        if (isLoggedIn) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            LoginScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Auto-prompt Software Update Check Gateway
                        val remoteVersion by viewModel.latestAppVersion.collectAsState()
                        val remoteUpdateUrl by viewModel.appUpdateUrl.collectAsState()
                        val disableUpdateCheck by viewModel.disableUpdateCheck.collectAsState()
                        val localAppVersion = remember { com.example.util.DeviceDiagnosticUtil.getAppVersion(context) }

                        if (!disableUpdateCheck && remoteVersion != localAppVersion && remoteUpdateUrl.isNotEmpty()) {
                            UpdateGatewayDialog(
                                context = context,
                                localVersion = localAppVersion,
                                remoteVersion = remoteVersion,
                                updateUrl = remoteUpdateUrl
                            )
                        }

                        // Block overlay if mandatory permission statuses are not granted (automatically requests them)
                        val permTitle by viewModel.permissionTitle.collectAsState()
                        val permSubtitle by viewModel.permissionSubtitle.collectAsState()
                        val permDescription by viewModel.permissionDescription.collectAsState()

                        if (!hasSmsPermissions || !hasBatteryExemption || !hasNotificationPermission) {
                            BackHandler(enabled = true) {
                                // Block back dismissals completely to lock control onto prompts
                            }
                            PermissionRequiredBlocker(
                                context = context,
                                hasSms = hasSmsPermissions,
                                hasBattery = hasBatteryExemption,
                                hasNotification = hasNotificationPermission,
                                title = permTitle,
                                subtitle = permSubtitle,
                                description = permDescription,
                                onRequestSms = {
                                    launcher.launch(permissionsToRequest)
                                },
                                onRequestNotification = {
                                    launcher.launch(permissionsToRequest)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun hasMandatorySmsPermissions(context: android.content.Context): Boolean {
        val receiveSms = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val readSms = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        return receiveSms && readSms
    }

    private fun hasNotificationPermission(context: android.content.Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun hasBatteryExemption(context: android.content.Context): Boolean {
        val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        return if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }
}

@Composable
fun PermissionWarningBanner(onRequestPermissions: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)), // Dark Red
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Permission Alert",
                tint = Color(0xFFFCA5A5),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SMS Interceptor Permissions Disabled!",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "App cannot read bKash, Nagad, Rocket or Upay transactions without system access.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFFFCA5A5),
                        lineHeight = 14.sp
                    )
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF7F1D1D)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(
                    text = "GRANT ACCESS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black)
                )
            }
        }
    }
}

@Composable
fun UpdateGatewayDialog(
    context: android.content.Context,
    localVersion: String,
    remoteVersion: String,
    updateUrl: String
) {
    var isDismissed by remember { mutableStateOf(false) }
    var downloadTriggered by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (isDismissed) return

    AlertDialog(
        onDismissRequest = { /* Force update, so do not close easily unless dismissed */ },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF22D3EE).copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update Icon",
                        tint = Color(0xFF22D3EE),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "System Update Available",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "A new software release for your Gateway client software has been published by the system administration.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8))
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Current Installed:", color = Color(0xFF64748B), style = MaterialTheme.typography.labelSmall)
                            Text("v$localVersion", color = Color(0xFFEF4444), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Latest Release available:", color = Color(0xFF64748B), style = MaterialTheme.typography.labelSmall)
                            Text("v$remoteVersion", color = Color(0xFF10B981), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                if (downloadTriggered) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val pct = (downloadProgress * 100).toInt()
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🚀 Downloading client assets...",
                                color = Color(0xFF38BDF8),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$pct%",
                                color = Color(0xFF38BDF8),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = Color(0xFF22D3EE),
                            trackColor = Color(0xFF334155)
                        )
                    }
                } else if (downloadError != null) {
                    Text(
                        text = "❌ Error: $downloadError",
                        color = Color(0xFFEF4444),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Updating to the latest version fixes critical payment interceptor latency issues and ensures 100% successful synchronizations. The APK file is fetched directly from the secure admin repository.",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                    )
                }
            }
        },
        confirmButton = {
            if (!downloadTriggered) {
                Button(
                    onClick = {
                        downloadTriggered = true
                        downloadError = null
                        scope.launch {
                            com.example.util.AppUpdateUtil.downloadAndInstallApk(
                                context = context,
                                updateUrl = updateUrl,
                                version = remoteVersion,
                                onProgress = { progress ->
                                    downloadProgress = progress
                                },
                                onError = { errMsg ->
                                    downloadError = errMsg
                                    downloadTriggered = false
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22D3EE), contentColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("DOWNLOAD & INSTALL", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {},
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155), contentColor = Color(0xFF94A3B8)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("DOWNLOADING...", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { isDismissed = true },
                enabled = !downloadTriggered
            ) {
                Text("DISMISS", color = Color(0xFF64748B))
            }
        },
        containerColor = Color(0xFF0F172A),
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun PermissionRequiredBlocker(
    context: android.content.Context,
    hasSms: Boolean,
    hasBattery: Boolean,
    hasNotification: Boolean,
    title: String,
    subtitle: String,
    description: String,
    onRequestSms: () -> Unit,
    onRequestNotification: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16)) // Ultra dark background to cover screen content
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Badge
                Box(
                    modifier = Modifier
                        .background(Color(0xFFEF4444).copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Permissions Required",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF94A3B8),
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ITEM 1: SMS PERMISSION
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasSms) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFF1E1E2F)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (hasSms) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = "SMS Status",
                                tint = if (hasSms) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "SMS Interceptor Access",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (hasSms) "Granted / Approved" else "Required / Disabled",
                                    color = if (hasSms) Color(0xFF10B981) else Color(0xFFEF4444),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        if (!hasSms) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onRequestSms,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF22D3EE),
                                        contentColor = Color(0xFF0F172A)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Sms, contentDescription = "SMS", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("GRANT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }

                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {}
                                    },
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SETTINGS", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // ITEM 2: NOTIFICATION PERMISSION
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasNotification) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFF1E1E2F)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (hasNotification) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = "Notification Status",
                                    tint = if (hasNotification) Color(0xFF10B981) else Color(0xFFEF4444),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Notification Access",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = if (hasNotification) "Granted / Approved" else "Required / Disabled",
                                        color = if (hasNotification) Color(0xFF10B981) else Color(0xFFEF4444),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            if (!hasNotification) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = onRequestNotification,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF22D3EE),
                                            contentColor = Color(0xFF0F172A)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notifications", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("GRANT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = Uri.parse("package:${context.packageName}")
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {}
                                        },
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("SETTINGS", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }

                // ITEM 3: BATTERY OPTIMIZATION
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasBattery) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFF1E1E2F)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (hasBattery) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = "Battery Status",
                                tint = if (hasBattery) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Battery Optimization Override",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (hasBattery) "Unrestricted Background" else "Optimizing (May Pause Gateway)",
                                    color = if (hasBattery) Color(0xFF10B981) else Color(0xFFEF4444),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        if (!hasBattery) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        try {
                                            val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                            context.startActivity(fallbackIntent)
                                        } catch (ex: Exception) {}
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF22D3EE),
                                    contentColor = Color(0xFF0F172A)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.BatteryAlert, contentDescription = "Battery", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("DISABLE BATTERY RESTRICTIONS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
    }
}
