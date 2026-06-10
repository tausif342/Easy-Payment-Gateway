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
                }

                // Auto prompt permissions on launch on first view layout side effect
                LaunchedEffect(Unit) {
                    if (!hasSmsPermissions) {
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
                                // Display alert banner if permissions are disabled, alerting that intercepting will fail
                                if (!hasSmsPermissions) {
                                    PermissionWarningBanner {
                                        launcher.launch(permissionsToRequest)
                                    }
                                }
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
                        val localAppVersion = remember { com.example.util.DeviceDiagnosticUtil.getAppVersion(context) }

                        if (remoteVersion != localAppVersion && remoteUpdateUrl.isNotEmpty()) {
                            UpdateGatewayDialog(
                                context = context,
                                localVersion = localAppVersion,
                                remoteVersion = remoteVersion,
                                updateUrl = remoteUpdateUrl
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
