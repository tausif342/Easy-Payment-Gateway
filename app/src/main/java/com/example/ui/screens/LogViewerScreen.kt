package com.example.ui.screens

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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SmsTransaction
import com.example.ui.viewmodel.GatewayViewModel
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import com.example.service.SyncWorkManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    viewModel: GatewayViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsState()
    val processedCount by viewModel.processedCount.collectAsState()
    val syncedCount by viewModel.syncedCount.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // ALL, SUCCESS, PENDING, FAILED
    var selectedTxn by remember { mutableStateOf<SmsTransaction?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Filtered transaction logs calculation
    val filteredTxns = remember(transactions, searchQuery, selectedStatusFilter) {
        transactions.filter { txn ->
            val matchesSearch = txn.txnId.contains(searchQuery, ignoreCase = true) ||
                    txn.sender.contains(searchQuery, ignoreCase = true) ||
                    txn.senderNumber.contains(searchQuery, ignoreCase = true) ||
                    txn.rawSms.contains(searchQuery, ignoreCase = true) ||
                    txn.reference.contains(searchQuery, ignoreCase = true)

            val matchesStatus = when (selectedStatusFilter) {
                "ALL" -> true
                "SYNCED" -> txn.syncStatus == "SUCCESS"
                "PENDING" -> txn.syncStatus == "PENDING"
                "FAILED" -> txn.syncStatus == "FAILED"
                else -> true
            }

            matchesSearch && matchesStatus
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "DIAGNOSTICS & LOG VIEWER",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Local SMS database & SaaS telemetry thread admin",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("log_viewer_back")) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
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
                    }) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Trigger sync",
                            tint = Color(0xFF22D3EE)
                        )
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear logs",
                            tint = Color(0xFFEF4444)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E293B),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier.testTag("log_viewer_root")
    ) { paddingValues ->
        var activeConsoleTab by remember { mutableStateOf("TRANSACTIONS") } // TRANSACTIONS, DIAGNOSTICS

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // TAB SWITCHER FOR TRANSACTION CARD DATABASE AND SYSTEM TELEMETRY
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (activeConsoleTab == "TRANSACTIONS") Color(0xFF22D3EE).copy(alpha = 0.15f) else Color(0xFF0F172A),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (activeConsoleTab == "TRANSACTIONS") Color(0xFF22D3EE) else Color(0xFF334155),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { activeConsoleTab = "TRANSACTIONS" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Sms, contentDescription = "SMS Transactions", tint = if (activeConsoleTab == "TRANSACTIONS") Color(0xFF22D3EE) else Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SMS QUEUE", style = MaterialTheme.typography.labelSmall.copy(color = if (activeConsoleTab == "TRANSACTIONS") Color.White else Color(0xFF94A3B8), fontWeight = FontWeight.Black))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (activeConsoleTab == "DIAGNOSTICS") Color(0xFF22D3EE).copy(alpha = 0.15f) else Color(0xFF0F172A),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (activeConsoleTab == "DIAGNOSTICS") Color(0xFF22D3EE) else Color(0xFF334155),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { activeConsoleTab = "DIAGNOSTICS" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.DeveloperBoard, contentDescription = "SaaS Telemetries", tint = if (activeConsoleTab == "DIAGNOSTICS") Color(0xFF22D3EE) else Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TELEMETRY LOGS", style = MaterialTheme.typography.labelSmall.copy(color = if (activeConsoleTab == "DIAGNOSTICS") Color.White else Color(0xFF94A3B8), fontWeight = FontWeight.Black))
                        }
                    }
                }

                if (activeConsoleTab == "DIAGNOSTICS") {
                    val systemLogs by viewModel.syncLogs.collectAsState()
                    var selectedLogFilter by remember { mutableStateOf("ALL") } // ALL, PARSING, SYNC, SYSTEM, ERROR
                    var logSearch by remember { mutableStateOf("") }
                    
                    val filteredSystemLogs = remember(systemLogs, selectedLogFilter, logSearch) {
                        systemLogs.filter { log ->
                            val matchesType = when (selectedLogFilter) {
                                "ALL" -> true
                                else -> log.logType.equals(selectedLogFilter, ignoreCase = true)
                            }
                            val matchesSearch = log.message.contains(logSearch, ignoreCase = true) ||
                                    log.txnId.contains(logSearch, ignoreCase = true) ||
                                    log.logType.contains(logSearch, ignoreCase = true)
                            
                            matchesType && matchesSearch
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        TextField(
                            value = logSearch,
                            onValueChange = { logSearch = it },
                            placeholder = { Text("Trace search keyword, trigger status or status...", color = Color(0xFF64748B), fontSize = 13.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF22D3EE),
                                focusedIndicatorColor = Color(0xFF22D3EE),
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("diagnostic_search_input")
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val types = listOf("ALL" to "All", "PARSING" to "Parsing", "SYNC" to "Sync", "SYSTEM" to "System", "ERROR" to "Errors")
                            types.forEach { (key, label) ->
                                val isSelected = selectedLogFilter == key
                                val activeColor = when (key) {
                                    "PARSING" -> Color(0xFF38BDF8)
                                    "SYNC" -> Color(0xFF10B981)
                                    "SYSTEM" -> Color(0xFFF59E0B)
                                    "ERROR" -> Color(0xFFEF4444)
                                    else -> Color(0xFF22D3EE)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) activeColor.copy(alpha = 0.15f) else Color(0xFF1E293B),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) activeColor else Color(0xFF334155),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedLogFilter = key }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (filteredSystemLogs.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No diagnostic system telemetry logs found.", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF64748B)))
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(filteredSystemLogs) { log ->
                                    val formatter = remember { SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()) }
                                    val formattedTime = remember(log.timestamp) { formatter.format(Date(log.timestamp)) }
                                    
                                    val (badgeBg, badgeText, badgeColor) = when (log.logType.uppercase()) {
                                        "PARSING" -> Triple(Color(0xFF0369A1), "SMS PARSER", Color(0xFF38BDF8))
                                        "SYNC" -> Triple(Color(0xFF065F46), "UPLOADER", Color(0xFF34D399))
                                        "ERROR" -> Triple(Color(0xFF7F1D1D), "FAIL TRACE", Color(0xFFFCA5A5))
                                        else -> Triple(Color(0xFF374151), "SYSTEM HEALTH", Color(0xFFE2E8F0))
                                    }

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(badgeBg, RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        text = badgeText,
                                                        style = MaterialTheme.typography.labelSmall.copy(color = badgeColor, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                                                    )
                                                }
                                                Text(
                                                    text = formattedTime,
                                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 10.sp)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(6.dp))
                                            
                                            Text(
                                                text = log.message,
                                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White, lineHeight = 16.sp)
                                            )
                                            
                                            if (log.txnId.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Assoc Txn: ${log.txnId}",
                                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF22D3EE), fontFamily = FontFamily.Monospace)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // 1. STATS METRICS HEADBOARD
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Indicator Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("TOTAL RECORDS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold))
                            Text("$processedCount", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Black))
                        }
                    }

                    // Synced Count Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, Color(0xFF059669)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("SYNCED", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color(0xFF34D399), fontWeight = FontWeight.Bold))
                            Text("$syncedCount", style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF10B981), fontWeight = FontWeight.Black))
                        }
                    }

                    // Pending / Offline Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, if (pendingCount > 0) Color(0xFFD97706) else Color(0xFF334155)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("OFFLINE QUEUE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold))
                            Text("$pendingCount", style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFFFFB703), fontWeight = FontWeight.Black))
                        }
                    }
                }

                // 2. SEARCH AND FILTER TOOLS CONSOLE
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search TxnID, number, reference or SMS text...", color = Color(0xFF64748B), fontSize = 13.sp) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF94A3B8)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search", tint = Color(0xFF94A3B8))
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF22D3EE),
                            focusedIndicatorColor = Color(0xFF22D3EE),
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("log_search_input")
                    )

                    // QUICK ROW STATUS SELECTORS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val filters = listOf("ALL" to "All", "SYNCED" to "Synced", "PENDING" to "Pending", "FAILED" to "Failed")
                        filters.forEach { (key, label) ->
                            val isSelected = selectedStatusFilter == key
                            val activeBorder = when (key) {
                                "SYNCED" -> Color(0xFF10B981)
                                "PENDING" -> Color(0xFFFBBF24)
                                "FAILED" -> Color(0xFFEF4444)
                                else -> Color(0xFF22D3EE)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) activeBorder.copy(alpha = 0.15f) else Color(0xFF1E293B),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) activeBorder else Color(0xFF334155),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedStatusFilter = key }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. TRANSACTION LIST VIEWPORT
                if (filteredTxns.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Source,
                                contentDescription = "No results found",
                                tint = Color(0xFF334155),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No records matched description",
                                style = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Locate valid transactions inside other tags/categories, or trigger an SMS event using the emulator console.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569)),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredTxns) { txn ->
                            LogItemRow(
                                txn = txn,
                                onClick = { selectedTxn = txn }
                            )
                        }
                    }
                }
                }
            }

            // 4. DETAILED OVERLAY SHEET (ALERT DIALOG / CARD)
            selectedTxn?.let { txn ->
                AlertDialog(
                    onDismissRequest = { selectedTxn = null },
                    containerColor = Color(0xFF1E293B),
                    shape = RoundedCornerShape(20.dp),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "SMS TRANSACTION DETAILS",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF22D3EE)
                                )
                            )
                            IconButton(onClick = { selectedTxn = null }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close details", tint = Color.White)
                            }
                        }
                    },
                    text = {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                // Amount & Status Header Board
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("AMOUNT RECORDED", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontWeight = FontWeight.Bold))
                                        Text(
                                            text = String.format(Locale.getDefault(), "৳%,.2f BDT", txn.amount),
                                            style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Black)
                                        )
                                    }

                                    val (statusText, statusBg, statusColor) = when (txn.syncStatus) {
                                        "SUCCESS" -> Triple("SYNCED", Color(0xFF065F46), Color(0xFF34D399))
                                        "PENDING" -> Triple("PENDING", Color(0xFF78350F), Color(0xFFFBBF24))
                                        else -> Triple("FAILED", Color(0xFF7F1D1D), Color(0xFFFCA5A5))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(statusBg, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = statusText,
                                            style = MaterialTheme.typography.labelMedium.copy(color = statusColor, fontWeight = FontWeight.Black)
                                        )
                                    }
                                }
                            }

                            item {
                                // Raw Parameters Details List
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        DetailMetricRow("TRANSACTION ID", txn.txnId, isMono = true)
                                        DetailMetricRow("SENDER NETWORK", txn.sender, isHighlight = true)
                                        DetailMetricRow("SENDER NUMBER", txn.senderNumber)
                                        DetailMetricRow("EXTRACTED DATE", txn.time)
                                        DetailMetricRow("REFERENCE TAG", txn.reference.ifEmpty { "N/A" })
                                        DetailMetricRow("PROJECT PATH ID", txn.projectId)
                                        DetailMetricRow("ACCOUNT PATH ID", txn.paymentAccountId)

                                        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                                        DetailMetricRow("DATABASE TIMESTAMP", formatter.format(Date(txn.timestamp)))
                                    }
                                }
                            }

                            item {
                                // Raw Broadcast SMS Content Block with clipboard copy option
                                Text(
                                    text = "ORIGINAL GSM BROADCAST SMS BODY:",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontWeight = FontWeight.ExtraBold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF020617), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "EN CODED RAW DATA",
                                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF475569), fontWeight = FontWeight.Black)
                                            )
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(txn.rawSms))
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.ContentCopy,
                                                    contentDescription = "Copy text",
                                                    tint = Color(0xFF94A3B8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = txn.rawSms,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFF94A3B8),
                                                fontFamily = FontFamily.Monospace,
                                                lineHeight = 16.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (txn.syncStatus != "SUCCESS") {
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
                                        selectedTxn = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = "Retry upload", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Re-Sync Now", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                            OutlinedButton(
                                onClick = { selectedTxn = null },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Close Console", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                )
            }

            // 5. PURGE CONFIRMATION OVERLAY DIALOG
            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    containerColor = Color(0xFF1E293B),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFFEF4444))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("PURGE ALL RECORD LOGS?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        }
                    },
                    text = {
                        Text(
                            text = "This action will permanently delete all locally cached SMS transaction logs and execution telemetries from the SQLite Room database. Sync state mappings on the backend server are preserved.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8))
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.clearAllLogs()
                                showDeleteConfirmDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Yes, Clear SQLite", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, color = Color.White))
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = false },
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Dismiss", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LogItemRow(
    txn: SmsTransaction,
    onClick: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    val formattedDate = remember(txn.timestamp) { formatter.format(Date(txn.timestamp)) }

    val statusColor = when (txn.syncStatus) {
        "SUCCESS" -> Color(0xFF10B981)
        "PENDING" -> Color(0xFFFBBF24)
        else -> Color(0xFFEF4444)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("log_row_entry_${txn.txnId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual circle indicating sender network style
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF0F172A), CircleShape)
                    .border(1.dp, statusColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val iconChar = when (txn.sender.lowercase()) {
                    "bkash" -> "b"
                    "nagad" -> "n"
                    "rocket" -> "r"
                    "upay" -> "u"
                    else -> "s"
                }
                Text(
                    text = iconChar.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = when (txn.sender.lowercase()) {
                            "bkash" -> Color(0xFFE11D48)
                            "nagad" -> Color(0xFFEA580C)
                            "rocket" -> Color(0xFF9333EA)
                            "upay" -> Color(0xFF2563EB)
                            else -> Color(0xFF22D3EE)
                        },
                        fontWeight = FontWeight.Black
                    )
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = txn.sender,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )

                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ID: ${txn.txnId}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF22D3EE),
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = String.format(Locale.getDefault(), "৳%,.2f", txn.amount),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(statusColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = txn.syncStatus,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }

                    if (txn.reference.isNotEmpty()) {
                        Text(
                            text = "Ref: ${txn.reference}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailMetricRow(
    label: String,
    value: String,
    isMono: Boolean = false,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF64748B),
                fontWeight = FontWeight.ExtraBold
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (isHighlight) Color(0xFFFFB703) else Color.White,
                fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default,
                fontWeight = if (isMono || isHighlight) FontWeight.Bold else FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
