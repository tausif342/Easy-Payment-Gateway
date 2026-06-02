package com.example.ui.screens

import com.example.data.model.Project
import com.example.data.model.PaymentAccount
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import com.example.data.model.SmsTransaction
import com.example.data.model.SyncLog
import com.example.ui.viewmodel.GatewayViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import com.example.service.SyncWorkManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: GatewayViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Collect model states
    val apiKey by viewModel.apiKey.collectAsState()
    val secretToken by viewModel.secretToken.collectAsState()
    val apiUrl by viewModel.apiUrl.collectAsState()
    val merchantId by viewModel.merchantId.collectAsState()
    val merchantName by viewModel.merchantName.collectAsState()
    val deviceLimit by viewModel.deviceLimit.collectAsState()
    val isApproved by viewModel.isApproved.collectAsState()
    val isGatewayActive by viewModel.isGatewayActive.collectAsState()
    val lastStatusCheck by viewModel.lastStatusCheck.collectAsState()
    val deviceId by viewModel.deviceId.collectAsState()
    val deviceName by viewModel.deviceName.collectAsState()

    val transactions by viewModel.transactions.collectAsState()
    val syncLogs by viewModel.syncLogs.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val paymentAccounts by viewModel.paymentAccounts.collectAsState()

    val totalAmount by viewModel.totalAmount.collectAsState()
    val processedCount by viewModel.processedCount.collectAsState()
    val syncedCount by viewModel.syncedCount.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    val syncFrequency by viewModel.syncFrequency.collectAsState()
    val repeatedSyncFailure by viewModel.repeatedSyncFailure.collectAsState()
    val lastSmsTime by viewModel.lastSmsTime.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()

    val uiState by viewModel.uiState.collectAsState()

    val geminiParseResult by viewModel.geminiParseResult.collectAsState()
    val geminiParseLoading by viewModel.geminiParseLoading.collectAsState()

    // Screen tab selection (0 = Transactions, 1 = Sync Logs, 2 = SMS Emulator Console)
    var activeTab by remember { mutableStateOf(0) }
    var isShowingLogViewer by remember { mutableStateOf(false) }
    
    // Collapsible states
    var isConfigExpanded by remember { mutableStateOf(false) }
    var simulatorTargetService by remember { mutableStateOf("bKash") }
    var customSmsSender by remember { mutableStateOf("bKash") }
    var customSmsBody by remember { mutableStateOf("You have received Tk 2,500.00 from 01711223344. Remaining Bal Tk 15,200.00. TrxID 8N34JG98DL at 02/06/2026 14:35. Ref: directRef") }

    // Auto-clear notification messages after delay
    LaunchedEffect(uiState) {
        if (uiState is GatewayViewModel.UiState.Success || uiState is GatewayViewModel.UiState.Error) {
            kotlinx.coroutines.delay(4000)
            viewModel.clearUiState()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Slate 900
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        if (isShowingLogViewer) {
            LogViewerScreen(
                viewModel = viewModel,
                onBack = { isShowingLogViewer = false },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
            
            // 1. TOP HEADER APP BRANDING & MASTER RUN TOGGLE
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "EASY SMS GATEWAY",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            if (isGatewayActive) Color(0xFF10B981) else Color(0xFF64748B),
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isGatewayActive) "ONLINE & LISTENING" else "SERVICE PAUSED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isGatewayActive) Color(0xFF34D399) else Color(0xFF94A3B8)
                                    )
                                )
                            }
                        }

                        // Master On/Off Switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = { isShowingLogViewer = true },
                                modifier = Modifier.padding(end = 4.dp).testTag("header_log_viewer_trigger")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = "Open database logs",
                                    tint = Color(0xFF22D3EE)
                                )
                            }
                            Text(
                                text = if (isGatewayActive) "ACTIVE" else "DORMANT",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isGatewayActive) Color(0xFF22D3EE) else Color(0xFF64748B)
                                ),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Switch(
                                checked = isGatewayActive,
                                onCheckedChange = { viewModel.toggleGateway(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF020617),
                                    checkedTrackColor = Color(0xFF22D3EE),
                                    uncheckedThumbColor = Color(0xFF94A3B8),
                                    uncheckedTrackColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.testTag("service_active_toggle")
                            )
                        }
                    }
                }
            }

            // DYNAMIC APPLET STATUS FEEDBACKS (Toasts inside Scaffold)
            AnimatedVisibility(
                visible = uiState !is GatewayViewModel.UiState.Idle,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                when (val state = uiState) {
                    is GatewayViewModel.UiState.Success -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF064E3B))
                                .padding(vertical = 10.dp, horizontal = 16.dp)
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFA7F3D0),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                    is GatewayViewModel.UiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF7F1D1D))
                                .padding(vertical = 10.dp, horizontal = 16.dp)
                        ) {
                            Text(
                                text = state.error,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFFECACA),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                    is GatewayViewModel.UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B))
                                .padding(vertical = 10.dp, horizontal = 16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF22D3EE),
                                    strokeWidth = 1.5.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Refreshing device statuses with central server...",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }

            AnimatedVisibility(
                visible = repeatedSyncFailure,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF7C2D12)) // Amber/Sienna style warning
                        .border(1.dp, Color(0xFFF97316))
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Repeated sync failures",
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "BACKGROUND SYNC RETRIAL EXCEEDED LIMIT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFFB923C),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Retrofit synchronization has failed after multiple attempts. Stored locally offline. Please verify endpoint configuration.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFFFEDD5),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val constraints = Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build()
                                val syncRequest = OneTimeWorkRequestBuilder<SyncWorkManager>()
                                    .setConstraints(constraints)
                                    .build()
                                WorkManager.getInstance(context).enqueueUniqueWork(
                                    "SmsGatewayOneTimeSync",
                                    ExistingWorkPolicy.REPLACE,
                                    syncRequest
                                )
                                viewModel.triggerSyncManual()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Retry Now", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        }
                    }
                }
            }

            // 2. MAIN GRID SCROLL VIEW
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // MERCHANT INFO CARD
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("merchant_info_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Business,
                                        contentDescription = "Merchant Details",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = merchantName,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                        
                                        // SUBSCRIPTION PLAN AWARE: Dynamically parse and show correct subscriber package and caps
                                        val subscriptionPlanName = when (deviceLimit) {
                                            1 -> "Basic Subscription Package"
                                            3 -> "Pro Subscription Package"
                                            10 -> "Business Subscription Package"
                                            else -> if (deviceLimit >= 50) "Enterprise Subscription Package" else "SaaS Platform Plan"
                                        }
                                        val deviceDetailsText = when (deviceLimit) {
                                            in 1..99 -> "Cap Limit: $deviceLimit Active Nodes"
                                            else -> "Cap Limit: Unlimited Nodes"
                                        }
                                        Text(
                                            text = "$subscriptionPlanName • $deviceDetailsText",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF22D3EE),
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )

                                        Text(
                                            text = "ID: $merchantId",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFF94A3B8)
                                            ),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }

                                // Licensing/Limit Pill badge
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isApproved) Color(0xFF064E3B) else Color(0xFF3F190D)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (isApproved) "LICENSED NODES" else "UNAUTHORIZED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isApproved) Color(0xFF34D399) else Color(0xFFF97316)
                                        ),
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 10.dp)
                                    )
                                }
                            }

                            Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 12.dp))

                            // Detail Rows (Collapsible or visible)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Device Hardware ID", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B)))
                                    Text(
                                        text = deviceId.take(15) + "...",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontFamily = FontFamily.Monospace)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Node Fingerprint Name", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B)))
                                    Text(text = deviceName, style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Target URL Gateway", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B)))
                                    Text(text = apiUrl, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF38BDF8)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Status Check", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B)))
                                    Text(text = lastStatusCheck, style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (lastStatusCheck == "APPROVED") Color(0xFF22D3EE) else Color(0xFFF97316)
                                    ))
                                }
                            }

                            // Interactive check status sync triggers
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.checkMerchantStatus() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Icon", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Check Licensing", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                }

                                Button(
                                    onClick = { viewModel.logoutMerchant() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Log out Icon", modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Revoke Token", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                }
                            }
                        }
                    }
                }

                // ENTERPRISE TELEMETRY & SaaS INDICATION CARD
                item {
                    val localAppVersion = com.example.util.DeviceDiagnosticUtil.getAppVersion(context)
                    val remoteVersion by viewModel.latestAppVersion.collectAsState()
                    val remoteUpdateUrl by viewModel.appUpdateUrl.collectAsState()
                    val remoteAllowedSenders by viewModel.allowedSenders.collectAsState()
                    val remoteInflowKeywords by viewModel.inflowKeywords.collectAsState()
                    val remoteMarketingKeywords by viewModel.marketingKeywords.collectAsState()
                    
                    val batteryLevel = remember { com.example.util.DeviceDiagnosticUtil.getBatteryLevel(context) }
                    val internetStatusStr = remember { com.example.util.DeviceDiagnosticUtil.getInternetStatus(context) }
                    val simProfiles = remember { com.example.util.DeviceDiagnosticUtil.getSimStatus(context) }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth().testTag("enterprise_telemetry_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Card Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeveloperMode,
                                    contentDescription = "Telemetry",
                                    tint = Color(0xFF22D3EE),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "ENTERPRISE TELEMETRY & REMOTE CONFIG",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                )
                            }
                            
                            Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 12.dp))

                            // Software Update Alert Prompt Banner inside Card
                            if (remoteVersion != localAppVersion && remoteUpdateUrl.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.SystemUpdate,
                                                contentDescription = "Software Update",
                                                tint = Color(0xFFFCA5A5)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "New Software Release Available ($remoteVersion)",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "A critical security update for Easy Payment Gateway is available. Download and verify the updated build immediately.",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFCA5A5), fontSize = 11.sp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                try {
                                                    val webIntent = android.content.Intent(
                                                        android.content.Intent.ACTION_VIEW,
                                                        android.net.Uri.parse(remoteUpdateUrl)
                                                    )
                                                    context.startActivity(webIntent)
                                                } catch (e: Exception) {
                                                    // No browser found fallback
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Install Update", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = Color.White))
                                        }
                                    }
                                }
                            }

                            // Diagnostics parameters Grid
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    DetailTelemetryRow("Battery Health Charge", "$batteryLevel%", Icons.Default.BatteryChargingFull, modifier = Modifier.weight(1f))
                                    DetailTelemetryRow("Network Connection Type", internetStatusStr, Icons.Default.SignalWifiStatusbar4Bar, modifier = Modifier.weight(1f))
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    DetailTelemetryRow("Hardware SIM Profile", simProfiles, Icons.Default.SimCard, modifier = Modifier.weight(1f))
                                    DetailTelemetryRow("Engine Software Ver", localAppVersion, Icons.Default.Info, modifier = Modifier.weight(1f))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val lastSyncFormatted = if (lastSyncTime > 0) {
                                        java.text.SimpleDateFormat("dd/MM HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(lastSyncTime))
                                    } else "Never"
                                    DetailTelemetryRow("Last Sync Latency State", lastSyncFormatted, Icons.Default.CloudSync, modifier = Modifier.weight(1f))
                                    
                                    val listenerActive = com.example.service.SmsGatewayService.isServiceRunning
                                    val selfHealingLabel = if (listenerActive) "Resilient Thread Active" else "Dormant (Self-Healing Enlisted)"
                                    DetailTelemetryRow("Background Self-Healing", selfHealingLabel, Icons.Default.Healing, modifier = Modifier.weight(1f))
                                }
                            }

                            Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 12.dp).padding(top = 4.dp))

                            // SaaS Remote config indicator section
                            Text(
                                text = "ACTIVE REMOTE FILTERS (SaaS PUSH)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                ParameterDisplayInfo("Allowed SMS Numbers", remoteAllowedSenders.ifEmpty { "BKASH, NAGAD, ROCKET, UPAY, 16247, +8801700000000" })
                                ParameterDisplayInfo("Payment Success Triggers", remoteInflowKeywords.ifEmpty { "received, receive, deposit, credited, transfer" })
                                ParameterDisplayInfo("Marketing Exclude Keywords", remoteMarketingKeywords.ifEmpty { "offer, bonus, win, discount, campaign" })
                            }
                        }
                    }
                }                // DEVICE HEALTH & SECURITY DIAGNOSTICS CARD
                item {
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    val powerManager = remember { context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager }
                    val isBatteryIgnoring = remember {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            powerManager.isIgnoringBatteryOptimizations(context.packageName)
                        } else {
                            true
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("device_health_diagnostics_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header row with Shield status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Security Shield Icon",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "HEALTH & SECURITY TELEMETRY",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                }
                                
                                // Connection Status badge
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFF10B981), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "DIAGS STABLE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF34D399)
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 1. Hardware Fingerprint signature row
                            val fingerprint = remember { com.example.util.DeviceSecurityUtil.generateFingerprint() }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                    .clickable {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(fingerprint))
                                        android.widget.Toast.makeText(context, "Fingerprint copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "NODE SIGNATURE:",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B),
                                            fontSize = 9.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = fingerprint,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFB703),
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Fingerprint Code",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 2. Battery Whitelist Guidance & Delay Warn Module
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Android Power Management",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                                    )
                                    Text(
                                        text = if (isBatteryIgnoring) "Background Optimization Whitelisted" else "Restricted (Warning: Deep sleep delays)",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isBatteryIgnoring) Color(0xFF34D399) else Color(0xFFFB923C)
                                        )
                                    )
                                }
                                if (!isBatteryIgnoring && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    Button(
                                        onClick = {
                                            try {
                                                val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                    data = android.net.Uri.parse("package:${context.packageName}")
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(context, "Optimization request launcher offline", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(
                                            "Whitelist",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                            }

                            Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 10.dp))

                            // 3. Last Intercept & Sync Logs Delay Track display
                            val sdf = remember { java.text.SimpleDateFormat("dd MMM, hh:mm:ss a", java.util.Locale.getDefault()) }
                            val lastSmsText = if (lastSmsTime == 0L) "Never / Sandbox Idle" else sdf.format(java.util.Date(lastSmsTime))
                            val lastSyncText = if (lastSyncTime == 0L) "Pending Boot cycle" else sdf.format(java.util.Date(lastSyncTime))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("LAST SMS RECEIVED", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold))
                                    Text(text = lastSmsText, style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Medium))
                                }
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                    Text("LAST DB SYNC RUN", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 9.sp, fontWeight = FontWeight.Bold))
                                    Text(text = lastSyncText, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF22D3EE), fontWeight = FontWeight.Medium))
                                }
                            }

                            Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 10.dp))

                            // 4. Multi-SIM Slot Active Provider Binds Layout
                            Text(
                                text = "ACTIVE DUAL-SIM HARDWARE PROFILE MAP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B),
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val sim1Binds = paymentAccounts.filter { it.simSlot == 0 }
                                val sim2Binds = paymentAccounts.filter { it.simSlot == 1 }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            "SIM SLOT 1 (Primary)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF38BDF8)
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        if (sim1Binds.isEmpty()) {
                                            Text("No Linked accounts", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569), fontSize = 10.sp))
                                        } else {
                                            sim1Binds.forEach {
                                                Text("• ${it.provider} (${it.walletNumber})", style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontSize = 10.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            "SIM SLOT 2 (Secondary)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFEC4899)
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        if (sim2Binds.isEmpty()) {
                                            Text("No Linked accounts", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569), fontSize = 10.sp))
                                        } else {
                                            sim2Binds.forEach {
                                                Text("• ${it.provider} (${it.walletNumber})", style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontSize = 10.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // FOUR CORE FINTECH STATISTICS COUNTERS (GRID LAYOUT)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Captured Stat Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Icon(imageVector = Icons.Default.Message, contentDescription = "Intercepts", tint = Color(0xFF22D3EE), modifier = Modifier.size(20.dp))
                                    Text(text = "INTERCEPTED", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 8.dp))
                                    Text(text = "$processedCount SMS", style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp), modifier = Modifier.padding(top = 4.dp))
                                }
                            }

                            // Synced Stat Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Icon(imageVector = Icons.Default.CloudDone, contentDescription = "Synced", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                                    Text(text = "SYNCED OK", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 8.dp))
                                    Text(text = "$syncedCount records", style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp), modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Queue Stat Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.CloudQueue, 
                                        contentDescription = "Pending Offlines", 
                                        tint = if (pendingCount > 0) Color(0xFFFB923C) else Color(0xFF94A3B8), 
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(text = "OFFLINE QUEUE", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 8.dp))
                                    Text(text = "$pendingCount delayed", style = MaterialTheme.typography.titleLarge.copy(
                                        color = if (pendingCount > 0) Color(0xFFFB923C) else Color.White, 
                                        fontWeight = FontWeight.ExtraBold, 
                                        letterSpacing = (-0.5).sp
                                    ), modifier = Modifier.padding(top = 4.dp))
                                }
                            }

                            // Amount Processed Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Icon(imageVector = Icons.Default.Payments, contentDescription = "Total processed amount", tint = Color(0xFFE2E8F0), modifier = Modifier.size(20.dp))
                                    Text(text = "VOLUME BDT", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 8.dp))
                                    Text(
                                        text = String.format(Locale.getDefault(), "৳%,.2f", totalAmount), 
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            color = Color.White, 
                                            fontWeight = FontWeight.ExtraBold, 
                                            letterSpacing = (-0.5).sp
                                        ), 
                                        modifier = Modifier.padding(top = 4.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. SECURE UTILITY SHORTCUTS
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "DIAGNOSTIC UTILITIES",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF64748B),
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val constraints = Constraints.Builder()
                                            .setRequiredNetworkType(NetworkType.CONNECTED)
                                            .build()
                                        val syncRequest = OneTimeWorkRequestBuilder<SyncWorkManager>()
                                            .setConstraints(constraints)
                                            .build()
                                        WorkManager.getInstance(context).enqueueUniqueWork(
                                            "SmsGatewayOneTimeSync",
                                            ExistingWorkPolicy.REPLACE,
                                            syncRequest
                                        )
                                        viewModel.triggerSyncManual()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22D3EE), contentColor = Color(0xFF020617)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1.3f).padding(0.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Sync, contentDescription = "Sync retry", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Force Sync DB Queue", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold), maxLines = 1)
                                }

                                Button(
                                    onClick = { viewModel.clearAllLogs() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color(0xFF94A3B8)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f).padding(0.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear caches", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset Logs", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // 4. TAB NAVIGATION DECK (Lists UI switches)
                item {
                    ScrollableTabRow(
                        selectedTabIndex = activeTab,
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color(0xFF22D3EE),
                        edgePadding = 4.dp,
                        modifier = Modifier.background(Color(0xFF1E293B), RoundedCornerShape(12.dp)).padding(2.dp),
                        indicator = { tabPositions ->
                            if (activeTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                                    color = Color(0xFF22D3EE)
                                )
                            }
                        }
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text("PAYMENTS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text("MAPPINGS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) }
                        )
                        Tab(
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            text = { Text("TELEMETRY", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) }
                        )
                        Tab(
                            selected = activeTab == 3,
                            onClick = { activeTab = 3 },
                            text = { Text("EMULATOR", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFB703))) }
                        )
                        Tab(
                            selected = activeTab == 4,
                            onClick = { activeTab = 4 },
                            text = { Text("SETTINGS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = Color(0xFFE2E8F0))) }
                        )
                    }
                }

                // TAB CONTENTS
                when (activeTab) {
                    0 -> { // TAB 0: INTERCEPTED PAYMENTS LOGS
                        item {
                            // NEW: SUMMARY DASHBOARD COMPONENT (Pending vs Synced progress)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .testTag("summary_dashboard_card")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1.3f),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "SYNC QUALITY OVERVIEW",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF22D3EE),
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 1.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Processed: $processedCount transactions",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        
                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Synced status indicator row
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Box(modifier = Modifier.size(10.dp).background(Color(0xFF10B981), RoundedCornerShape(5.dp)))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Synced: $syncedCount txn",
                                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                                            )
                                        }

                                        // Offline Queue status indicator row
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Box(modifier = Modifier.size(10.dp).background(Color(0xFFFB923C), RoundedCornerShape(5.dp)))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Offline Queue: $pendingCount delayed",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = if (pendingCount > 0) Color(0xFFFB923C) else Color(0xFF94A3B8)
                                                )
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Pill indicator showing sync percentage badge
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            val pct = if (processedCount > 0) (syncedCount.toFloat() / processedCount * 100).toInt() else 100
                                            Text(
                                                text = "$pct% DATABASE SYNCED",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (pct == 100) Color(0xFF10B981) else Color(0xFFFFB703),
                                                    fontWeight = FontWeight.Black
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Display the newly integrated native Pie Chart representing Pending vs Synced ratio
                                    SmsSyncPieChart(
                                        syncedCount = syncedCount,
                                        pendingCount = pendingCount,
                                        modifier = Modifier.weight(0.7f)
                                    )
                                }
                            }
                        }

                        if (transactions.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(imageVector = Icons.Default.SmsFailed, contentDescription = "No SMS yet", tint = Color(0xFF475569), modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("No Mobile Banking SMS captured yet.", style = MaterialTheme.typography.titleSmall.copy(color = Color(0xFF64748B), fontWeight = FontWeight.Bold))
                                        Text("Toggle listener to ACTIVE and trigger the SMS Emulator tab to test parsing instantly!", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569)), modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp), textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        } else {
                            items(transactions, key = { it.id }) { txn ->
                                TransactionItemRow(txn = txn, projects = projects, paymentAccounts = paymentAccounts)
                            }
                        }
                    }

                    1 -> { // TAB 1: NEW SYSTEMS MAPPINGS (Projects & Accounts manager!)
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // A. Projects Card
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("projects_management_card")
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "REGISTERED WEBSITE PROJECTS",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.ExtraBold, color = Color(0xFF22D3EE)
                                            )
                                        )
                                        Text(
                                            text = "Configure multiple tenant web stores/websites to automatically segregate payouts based on backend query keys.",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                        )

                                        if (projects.isEmpty()) {
                                            Text(
                                                text = "No projects found. Using 'default_project' fallback.",
                                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFF97316)),
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        } else {
                                            projects.forEach { pr ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp)
                                                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                                        .padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(text = pr.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                                        Text(text = "ID: ${pr.id}", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)))
                                                        Text(text = pr.websiteUrl, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF22D3EE)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }
                                                    IconButton(onClick = { viewModel.deleteProject(pr) }) {
                                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete project", tint = Color(0xFFEF4444))
                                                    }
                                                }
                                            }
                                        }

                                        var showAddProj by remember { mutableStateOf(false) }
                                        if (showAddProj) {
                                            var prId by remember { mutableStateOf("") }
                                            var prName by remember { mutableStateOf("") }
                                            var prUrl by remember { mutableStateOf("") }

                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 10.dp)
                                                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                                                    .padding(12.dp)
                                            ) {
                                                Text("Register Website Project", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                                OutlinedTextField(
                                                    value = prId,
                                                    onValueChange = { prId = it },
                                                    label = { Text("Unique Project Key (e.g. shop_a)", color = Color(0xFF94A3B8)) },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                                                )
                                                OutlinedTextField(
                                                    value = prName,
                                                    onValueChange = { prName = it },
                                                    label = { Text("Website / Business Name", color = Color(0xFF94A3B8)) },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                                )
                                                OutlinedTextField(
                                                    value = prUrl,
                                                    onValueChange = { prUrl = it },
                                                    label = { Text("Webhook Domain Website URL", color = Color(0xFF94A3B8)) },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    TextButton(onClick = { showAddProj = false }) { Text("Cancel") }
                                                    Button(
                                                        onClick = {
                                                            if (prId.isNotEmpty() && prName.isNotEmpty()) {
                                                                viewModel.addProject(prId, prName, prUrl)
                                                                showAddProj = false
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22D3EE), contentColor = Color(0xFF020617))
                                                    ) {
                                                        Text("Add Project")
                                                    }
                                                }
                                            }
                                        } else {
                                            Button(
                                                onClick = { showAddProj = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add project")
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Add Website Project node", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                            }
                                        }
                                    }
                                }

                                // B. Accounts Card
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("accounts_mapping_card")
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "PAYMENT ACCOUNTS MAPPING",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFB703)
                                            )
                                        )
                                        Text(
                                            text = "Declare which phone wallet numbers are receiving SMS, which physical SIM slots they run on, and route them to your separate website projects.",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                        )

                                        if (paymentAccounts.isEmpty()) {
                                            Text(
                                                text = "No accounts registered. Using default fallbacks.",
                                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFF97316)),
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        } else {
                                            paymentAccounts.forEach { acc ->
                                                val linkedPrName = projects.find { it.id == acc.projectId }?.name ?: "Unknown Project ID [${acc.projectId}]"
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp)
                                                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                                        .padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(text = acc.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                                        Text(text = "${acc.provider} (Wallet: ${acc.walletNumber})", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF22D3EE)))
                                                        Text(text = "SIM Slot: ${if (acc.simSlot == -1) "Any Slot" else "Slot ${acc.simSlot + 1}"}", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)))
                                                        Row(
                                                            modifier = Modifier.padding(top = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(imageVector = Icons.Default.Launch, contentDescription = "Route to", tint = Color(0xFFFFB703), modifier = Modifier.size(12.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(text = "Reroutes -> $linkedPrName", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFFB703), fontWeight = FontWeight.Bold))
                                                        }
                                                    }
                                                    IconButton(onClick = { viewModel.deletePaymentAccount(acc) }) {
                                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete account", tint = Color(0xFFEF4444))
                                                    }
                                                }
                                            }
                                        }

                                        var showAddAcc by remember { mutableStateOf(false) }
                                        if (showAddAcc) {
                                            var accId by remember { mutableStateOf("") }
                                            var accName by remember { mutableStateOf("") }
                                            var accProvider by remember { mutableStateOf("bKash") }
                                            var accWallet by remember { mutableStateOf("") }
                                            var accSlot by remember { mutableStateOf(0) }
                                            var activeProjId by remember { mutableStateOf(projects.firstOrNull()?.id ?: "default_project") }

                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 10.dp)
                                                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                                                    .padding(12.dp)
                                            ) {
                                                Text("Map Payment Account", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                                OutlinedTextField(
                                                    value = accId,
                                                    onValueChange = { accId = it },
                                                    label = { Text("Unique Account key (e.g. bk_a)", color = Color(0xFF94A3B8)) },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                                                )
                                                OutlinedTextField(
                                                    value = accName,
                                                    onValueChange = { accName = it },
                                                    label = { Text("Display Name (e.g. bKash Account A)", color = Color(0xFF94A3B8)) },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                                )

                                                Text("FinTech Service Provider:", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 6.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                                ) {
                                                    listOf("bKash", "Nagad", "Rocket", "Upay").forEach { p ->
                                                        FilterChip(
                                                            selected = accProvider == p,
                                                            onClick = { accProvider = p },
                                                            label = { Text(p, style = TextStyle(fontSize = 11.sp)) }
                                                        )
                                                    }
                                                }

                                                OutlinedTextField(
                                                    value = accWallet,
                                                    onValueChange = { accWallet = it },
                                                    label = { Text("Merchant SIM Wallet Number", color = Color(0xFF94A3B8)) },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                                )

                                                Text("Assigned SIM Line Slot Index:", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 6.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                                ) {
                                                    listOf(-1 to "Any SIM", 0 to "SIM 1 (Slot 0)", 1 to "SIM 2 (Slot 1)").forEach { (valIx, caption) ->
                                                        FilterChip(
                                                            selected = accSlot == valIx,
                                                            onClick = { accSlot = valIx },
                                                            label = { Text(caption, style = TextStyle(fontSize = 11.sp)) }
                                                        )
                                                    }
                                                }

                                                Text("Route Payments dynamically to Project Website:", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 8.dp))
                                                if (projects.isEmpty()) {
                                                    Text("Create a project website first to link.", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFEF4444)), modifier = Modifier.padding(top = 2.dp))
                                                } else {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(top = 4.dp)
                                                            .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                                            .padding(6.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        projects.forEach { prj ->
                                                            InputChip(
                                                                selected = activeProjId == prj.id,
                                                                onClick = { activeProjId = prj.id },
                                                                label = { Text(prj.name, style = TextStyle(fontSize = 10.sp)) }
                                                            )
                                                        }
                                                    }
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    TextButton(onClick = { showAddAcc = false }) { Text("Cancel") }
                                                    Button(
                                                        onClick = {
                                                            if (accId.isNotEmpty() && accName.isNotEmpty() && accWallet.isNotEmpty()) {
                                                                viewModel.addPaymentAccount(accId, accName, accProvider, accWallet, accSlot, activeProjId)
                                                                showAddAcc = false
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB703), contentColor = Color(0xFF020617))
                                                    ) {
                                                        Text("Map Account")
                                                    }
                                                }
                                            }
                                        } else {
                                            Button(
                                                onClick = { showAddAcc = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.AddCard, contentDescription = "Map Account")
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Map New Payment Account", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> { // TAB 2: RUNTIME NETWORKS LOGS
                        if (syncLogs.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(imageVector = Icons.Default.HistoryToggleOff, contentDescription = "No logs yet", tint = Color(0xFF475569), modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Telemetry stream is empty.", style = MaterialTheme.typography.titleSmall.copy(color = Color(0xFF64748B), fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        } else {
                            items(syncLogs, key = { it.id }) { log ->
                                SyncLogItemRow(log = log)
                            }
                        }
                    }

                    3 -> { // TAB 3: DEVELOPER SMS EMULATOR CONSOLE (INTEGRATED PAYMENTS MAPPING VERIFICATION!)
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color(0xFFFFB703).copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().testTag("emulator_card")
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    var selectedAccIdx by remember { mutableStateOf(0) }

                                    Text(
                                        text = "DEVELOPER SANDBOX CONSOLE",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFFFB703),
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                    Text(
                                        text = "Inject simulated bKash, Nagad, Rocket, or Upay SMS packets to evaluate precise client-side extraction, mapped project routing, and direct verification.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                    )

                                    Divider(color = Color(0xFF334155), modifier = Modifier.padding(bottom = 12.dp))

                                    // SELECT ACCOUNTS INSTEAD OF RANDOM PRESETS FOR INTEGRATED MAPPED DEMO!
                                    if (paymentAccounts.isNotEmpty()) {
                                        Text("CHOOSE DEVICE MAPPED ACCOUNT TO SIMULATE TRANSACTION AT:", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFFFB703), fontWeight = FontWeight.Black))
                                        
                                        if (selectedAccIdx >= paymentAccounts.size) selectedAccIdx = 0
                                        val mappedAccount = paymentAccounts.getOrNull(selectedAccIdx)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 6.dp, bottom = 12.dp)
                                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                                .padding(6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            paymentAccounts.forEachIndexed { idx, acc ->
                                                val isSelected = selectedAccIdx == idx
                                                InputChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        selectedAccIdx = idx
                                                        simulatorTargetService = acc.provider
                                                        
                                                        // Automatically load realistic SMS template for that provider containing the wallet number
                                                        if (acc.provider.equals("bKash", ignoreCase = true)) {
                                                            customSmsSender = "bKash"
                                                            customSmsBody = "You have received Tk 5,000.00 from 01822334455 at bKash Wallet ${acc.walletNumber}. Fee Tk 0.00. Balance Tk 45,200.00. TrxID 8N" + (1000..9999).random() + "JG9DL at 02/06/2026 14:35. Ref: test_tx_49"
                                                        } else if (acc.provider.equals("Nagad", ignoreCase = true)) {
                                                            customSmsSender = "NAGAD"
                                                            customSmsBody = "Payment received of Tk 1,200.00 from 01928473625 at Nagad Wallet ${acc.walletNumber}. Balance Tk 12,000.00. TxnID 7" + (1000..9999).random() + "JGD82 at 02/06/2026 12:40. Ref: order_1028"
                                                        } else {
                                                            customSmsSender = acc.provider
                                                            customSmsBody = "You have received Tk. 2,000.00 from 01374829374. Bal: Tk. 4,500.00. TxId: 9283749" + (1000..9999).random() + "A. Date: 02/06/2026 10:30"
                                                        }
                                                    },
                                                    label = { Text(acc.name, style = TextStyle(fontSize = 11.sp)) }
                                                )
                                            }
                                        }
                                    } else {
                                        Text("PRESET SMS TEMPLATES CARDS:", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontWeight = FontWeight.Black))
                                        
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            InputChip(
                                                selected = simulatorTargetService == "bKash",
                                                onClick = {
                                                    simulatorTargetService = "bKash"
                                                    customSmsSender = "bKash"
                                                    customSmsBody = "You have received Tk 5,000.00 from 01844992211. Fee Tk 0.00. Balance Tk 45,200.00. TrxID 8N34JG98DL at 02/06/2026 14:35. Ref: userRef49"
                                                },
                                                label = { Text("bKash Rec Tk 5,000") }
                                            )
                                            InputChip(
                                                selected = simulatorTargetService == "Nagad",
                                                onClick = {
                                                    simulatorTargetService = "Nagad"
                                                    customSmsSender = "NAGAD"
                                                    customSmsBody = "Payment received of Tk 1,200.00 from 01928473625. Balance Tk 12,000.00. TxnID 712JGD82 at 02/06/2026 12:40. Ref: order_1028"
                                                },
                                                label = { Text("Nagad Pay BDT 1,200") }
                                            )
                                        }
                                    }

                                    // Intercept SMS Field Inputs
                                    Divider(color = Color(0xFF334155), modifier = Modifier.padding(bottom = 12.dp))

                                    Text("CUSTOM SIMULATOR FIELDS (EDITABLE):", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontWeight = FontWeight.Bold))

                                    OutlinedTextField(
                                        value = customSmsSender,
                                        onValueChange = { customSmsSender = it },
                                        label = { Text("SMS Sender Number / Label", color = Color(0xFF94A3B8)) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFFFFB703),
                                            unfocusedBorderColor = Color(0xFF334155),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = customSmsBody,
                                        onValueChange = { customSmsBody = it },
                                        label = { Text("Raw Intercepted SMS Body Block", color = Color(0xFF94A3B8)) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFFFFB703),
                                            unfocusedBorderColor = Color(0xFF334155),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth().height(100.dp).padding(bottom = 16.dp),
                                        maxLines = 4
                                    )

                                    Button(
                                        onClick = {
                                            // Get currently selected mapped account & project IDs
                                            val currentAccount = paymentAccounts.getOrNull(selectedAccIdx)
                                            val feedProjectId = currentAccount?.projectId ?: "default_project"
                                            val feedAccountId = currentAccount?.id ?: "default_account"

                                            viewModel.simulateSmsReceived(
                                                sender = customSmsSender,
                                                messageBody = customSmsBody,
                                                projectId = feedProjectId,
                                                paymentAccountId = feedAccountId
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFFB703),
                                            contentColor = Color(0xFF020617)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("simulate_feed_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.SendTimeExtension, contentDescription = "Send mock SMS")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("FEED EMULATED SMS TO GATEWAY", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black))
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            viewModel.parseSmsWithGemini(customSmsBody)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF06B6D4), // Cyan 500
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        enabled = !geminiParseLoading,
                                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("gemini_extract_button")
                                    ) {
                                        if (geminiParseLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("ANALYZING WITH GEMINI...", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black))
                                        } else {
                                            Icon(imageVector = Icons.Default.Analytics, contentDescription = "AI extractor")
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("EXTRACT & PARSE WITH GEMINI API", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black))
                                        }
                                    }

                                    geminiParseResult?.let { res ->
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                                .border(1.dp, Color(0xFF06B6D4).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                                .padding(14.dp)
                                        ) {
                                            Text(
                                                text = "STRUCTURED TRANSACTION JSON (GEMINI):",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF06B6D4),
                                                    fontWeight = FontWeight.Black
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = """
                                                {
                                                  "amount": ${res.amount ?: "null"},
                                                  "sender": ${if (res.sender != null) "\"${res.sender}\"" else "null"},
                                                  "date": ${if (res.date != null) "\"${res.date}\"" else "null"},
                                                  "currency": ${if (res.currency != null) "\"${res.currency}\"" else "null"}
                                                }
                                                """.trimIndent(),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFFE2E8F0)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    4 -> { // TAB 4: SETTINGS & LOCAL RECORD VIEWER
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // A. Background Synchronization Settings
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier.fillMaxWidth().testTag("sync_settings_card")
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "BACKGROUND SYNCHRONIZATION",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.ExtraBold, color = Color(0xFF22D3EE)
                                            )
                                        )
                                        Text(
                                            text = "Establish the periodic frequency of the WorkManager queue processor. Frequent triggers guarantee fast verification.",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                        )

                                        Text(
                                            text = "SELECT INTERVAL FREQUENCY:",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFFFB703), fontWeight = FontWeight.Bold)
                                        )

                                        val intervals = listOf(15 to "15 Mins", 30 to "30 Mins", 60 to "1 Hour", 120 to "2 Hours")
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                        ) {
                                            intervals.forEach { (mins, label) ->
                                                val isSelected = syncFrequency == mins
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(40.dp)
                                                        .background(
                                                            if (isSelected) Color(0xFF22D3EE) else Color(0xFF0F172A),
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (isSelected) Color(0xFF22D3EE) else Color(0xFF334155),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable { viewModel.updateSyncFrequency(mins) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = if (isSelected) Color(0xFF020617) else Color(0xFF94A3B8),
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(18.dp))
                                        HorizontalDivider(color = Color(0xFF334155))
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Manual Trigger block
                                        Text(
                                            text = "MANUAL QUEUE FLUSH",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "Instantly trigger a one-time WorkManager job to clear and upload all pending/failed records.",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)),
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        )

                                        Button(
                                            onClick = {
                                                val constraints = Constraints.Builder()
                                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                                    .build()
                                                val syncRequest = OneTimeWorkRequestBuilder<SyncWorkManager>()
                                                    .setConstraints(constraints)
                                                    .build()
                                                WorkManager.getInstance(context).enqueueUniqueWork(
                                                    "SmsGatewayOneTimeSync",
                                                    ExistingWorkPolicy.REPLACE,
                                                    syncRequest
                                                )
                                                viewModel.triggerSyncManual()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Sync, contentDescription = "Force manual sync", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Force Sync & Flush Queue Now", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }

                                // B. Local Gateway Database Telemetry Status
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("local_telemetry_status_card")
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "SECURE LOCAL SMS RECORDS MONITOR",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFB703)
                                            )
                                        )
                                        Text(
                                            text = "Locally saved SMS records captured directly by the Android broadcast listeners, displaying exact timestamps and synchronization states.",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                        )

                                        Button(
                                            onClick = { isShowingLogViewer = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22D3EE), contentColor = Color(0xFF020617)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp).testTag("launch_full_log_viewer_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ReceiptLong,
                                                contentDescription = "Open Dedicated Console",
                                                tint = Color(0xFF020617),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "LAUNCH FULL SECURE LOG VIEWER",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black)
                                            )
                                        }

                                        if (transactions.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                                    .padding(20.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "No saved SMS transaction logs found.",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                                                )
                                            }
                                        } else {
                                            transactions.forEach { txn ->
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(bottom = 8.dp)
                                                        .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                                        .padding(12.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(
                                                                text = "${txn.sender} (${txn.senderNumber})",
                                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                                            )
                                                            Text(
                                                                text = "TxnID: ${txn.txnId}",
                                                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF22D3EE), fontFamily = FontFamily.Monospace)
                                                            )
                                                        }
                                                        Text(
                                                            text = String.format(Locale.getDefault(), "৳%,.2f", txn.amount),
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black, color = Color.White)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            val statusColor = when (txn.syncStatus) {
                                                                "SUCCESS" -> Color(0xFF10B981)
                                                                "PENDING" -> Color(0xFFFFB703)
                                                                else -> Color(0xFFEF4444)
                                                            }
                                                            Box(modifier = Modifier.size(8.dp).background(statusColor, RoundedCornerShape(4.dp)))
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = "Status: ${txn.syncStatus}",
                                                                style = MaterialTheme.typography.labelSmall.copy(color = statusColor, fontWeight = FontWeight.Bold)
                                                            )
                                                        }

                                                        val formatter = remember { java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()) }
                                                        val timeStr = remember(txn.timestamp) { formatter.format(java.util.Date(txn.timestamp)) }
                                                        Text(
                                                            text = timeStr,
                                                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun TransactionItemRow(
    txn: SmsTransaction,
    projects: List<Project>,
    paymentAccounts: List<PaymentAccount>
) {
    var expanded by remember { mutableStateOf(false) }

    // Deduce beautiful brand color for banking app layouts
    val brandColor = when (txn.sender) {
        "bKash" -> Color(0xFFD01C53)
        "Nagad" -> Color(0xFFF56A1E)
        "Rocket" -> Color(0xFF8C1AF5)
        "Upay" -> Color(0xFF1E88E5)
        "Bank SMS" -> Color(0xFF34D399)
        else -> Color(0xFF64748B)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Service Badge bullet
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(brandColor, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = txn.sender.take(2).uppercase(),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = txn.sender,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TRX: ${txn.txnId}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8),
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ref: ${txn.reference}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Balance / Payout indicator
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.getDefault(), "৳%,.2f", txn.amount),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    )

                    // Sync Status chips
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (txn.syncStatus) {
                                "SUCCESS" -> Color(0xFF064E3B)
                                "PENDING" -> Color(0xFF2D1E0E)
                                else -> Color(0xFF450A0A)
                            }
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = txn.syncStatus,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = when (txn.syncStatus) {
                                    "SUCCESS" -> Color(0xFF34D399)
                                    "PENDING" -> Color(0xFFFBBF24)
                                    else -> Color(0xFFFCA5A5)
                                },
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(vertical = 2.dp, horizontal = 6.dp)
                        )
                    }
                }
            }

            // Expanded raw inspect details
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    // MULTI-WEBSITE/MULTI-ACCOUNT DETAIL ROUTING METRIC SHOWCASE!
                    val mappedPrName = projects.find { it.id == txn.projectId }?.name ?: "Fallback Platform Project"
                    val mappedAccName = paymentAccounts.find { it.id == txn.paymentAccountId }?.name ?: "Fallback Gateway Account"

                    Text(
                        text = "ROUTED SERVER PROJECT TARGET:",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF22D3EE), fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "$mappedPrName (${txn.projectId})",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Text(
                        text = "ROUTED PAYMENT ACCOUNT SOURCE:",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFFFB703), fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "$mappedAccName (${txn.paymentAccountId})",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Text(
                        text = "CUSTOMER PHONE:",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = txn.senderNumber,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Text(
                        text = "SMS SENSOR TIMESTAMP:",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = txn.time,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Text(
                        text = "RAW CAPTURED TELEMETRY TEXT:",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = txn.rawSms,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 15.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SyncLogItemRow(log: SyncLog) {
    val logTime = remember(log.timestamp) {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        formatter.format(Date(log.timestamp))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (log.status == "SUCCESS") Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = log.status,
                tint = if (log.status == "SUCCESS") Color(0xFF10B981) else Color(0xFFEF4444),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TXN: ${log.txnId}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Text(
                        text = logTime,
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)),
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = log.message,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SmsSyncPieChart(
    syncedCount: Int,
    pendingCount: Int,
    modifier: Modifier = Modifier
) {
    val total = syncedCount + pendingCount
    
    // Animate the angles nicely
    val animatedSyncedAngle by animateFloatAsState(
        targetValue = if (total > 0) (syncedCount.toFloat() / total) * 360f else 360f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "synced_angle_anim"
    )
    
    val animatedPendingAngle by animateFloatAsState(
        targetValue = if (total > 0) (pendingCount.toFloat() / total) * 360f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pending_angle_anim"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(100.dp).testTag("sync_pie_chart")) {
            val strokeWidth = 14.dp.toPx()
            if (total == 0) {
                // Empty state gray arc
                drawArc(
                    color = Color(0xFF334155),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
            } else {
                var startAngle = -90f

                // Draw Synced (Color: Green, 0xFF10B981)
                if (animatedSyncedAngle > 0f) {
                    drawArc(
                        color = Color(0xFF10B981),
                        startAngle = startAngle,
                        sweepAngle = animatedSyncedAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += animatedSyncedAngle
                }

                // Draw Pending (Color: Orange, 0xFFFB923C)
                if (animatedPendingAngle > 0f) {
                    drawArc(
                        color = Color(0xFFFB923C),
                        startAngle = startAngle,
                        sweepAngle = animatedPendingAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Percentage overlay
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val percentage = if (total > 0) (syncedCount.toFloat() / total * 100).toInt() else 100
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    letterSpacing = (-0.5).sp
                )
            )
            Text(
                text = "SYNCED",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}

@Composable
fun DetailTelemetryRow(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.padding(2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = label, tint = Color(0xFF22D3EE), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label, 
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF64748B), 
                        fontSize = 9.sp, 
                        fontWeight = FontWeight.Bold
                    ), 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value, 
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White, 
                    fontWeight = FontWeight.Black, 
                    fontSize = 11.sp
                ), 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ParameterDisplayInfo(
    title: String,
    content: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title, 
            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = content, 
            style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
}
