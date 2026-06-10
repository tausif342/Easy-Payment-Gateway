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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentAccount
import com.example.data.model.Project
import com.example.data.model.SmsTransaction
import com.example.ui.viewmodel.GatewayViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PaymentsTab(
    viewModel: GatewayViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsState()
    val paymentAccounts by viewModel.paymentAccounts.collectAsState()
    val projects by viewModel.projects.collectAsState()

    var showAddAcc by remember { mutableStateOf(false) }
    var accName by remember { mutableStateOf("") }
    var accWallet by remember { mutableStateOf("") }
    var accProvider by remember { mutableStateOf("bKash") }
    var accSlot by remember { mutableStateOf(0) }
    var selectedProjId by remember { mutableStateOf("") }

    // Search and filter states
    var searchQuery by remember { mutableStateOf("") }
    var providerFilter by remember { mutableStateOf("ALL") }

    // Sync selected Project fallback
    LaunchedEffect(projects) {
        if (selectedProjId.isEmpty() && projects.isNotEmpty()) {
            selectedProjId = projects.first().id
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // A. PAYMENT ACCOUNTS MAPPING (MANAGEMENT)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("accounts_mapping_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MOBILE FINANCIAL WALLETS (MFS)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFB703)
                        )
                    )
                    IconButton(onClick = { showAddAcc = !showAddAcc }) {
                        Icon(
                            imageVector = if (showAddAcc) Icons.Default.Close else Icons.Default.AddCircle,
                            contentDescription = "Toggle Mappings form",
                            tint = Color(0xFFFFB703)
                        )
                    }
                }

                Text(
                    text = "Map wallet numbers receiving SMS and designate which SIM slot they stream logs from.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Render expansion form
                AnimatedVisibility(visible = showAddAcc) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Register Mobile Wallet Mapping",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, color = Color.White)
                        )

                        OutlinedTextField(
                            value = accName,
                            onValueChange = { accName = it },
                            label = { Text("Display Name (e.g. Bkash Agent A)", color = Color(0xFF64748B)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFB703),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedLabelColor = Color(0xFFFFB703)
                            )
                        )

                        OutlinedTextField(
                            value = accWallet,
                            onValueChange = { accWallet = it },
                            label = { Text("Wallet Phone Number (e.g. 01711223344)", color = Color(0xFF64748B)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFB703),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedLabelColor = Color(0xFFFFB703)
                            )
                        )

                        // Provider selector (bKash, Nagad, Rocket, Upay)
                        Column {
                            Text("Service Provider", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8)))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("bKash", "Nagad", "Rocket", "Upay").forEach { prov ->
                                    val isSel = accProvider.lowercase() == prov.lowercase()
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .background(
                                                if (isSel) Color(0xFFFFB703) else Color(0xFF1E293B),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { accProvider = prov }
                                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = prov,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSel) Color(0xFF020617) else Color(0xFF94A3B8),
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // SIM Slot selector
                        Column {
                            Text("Gateway SMS Receiver SIM Slot", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8)))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(-1 to "Any SIM", 0 to "SIM 2 (Slot 1)", 1 to "SIM 3 (Slot 2)").forEach { (slotVal, label) ->
                                    val isSel = accSlot == slotVal
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .background(
                                                if (isSel) Color(0xFFFFB703) else Color(0xFF1E293B),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { accSlot = slotVal }
                                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSel) Color(0xFF020617) else Color(0xFF94A3B8),
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Website Target mapping
                        if (projects.isNotEmpty()) {
                            Column {
                                Text("Map/Route Webhook to Domain", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8)))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    projects.forEach { pr ->
                                        val isSel = selectedProjId == pr.id
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSel) Color(0xFFFFB703) else Color(0xFF1E293B)
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.clickable { selectedProjId = pr.id }
                                        ) {
                                            Text(
                                                text = pr.name,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isSel) Color(0xFF020617) else Color.White,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                "No registered websites found. Please head to the WEB PORTAL tab to add a web channel model.",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFF97316)),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Button(
                            onClick = {
                                if (accName.isNotBlank() && accWallet.isNotBlank()) {
                                    val realProj = if (selectedProjId.isEmpty() && projects.isNotEmpty()) projects.first().id else selectedProjId
                                    viewModel.addPaymentAccount(
                                        id = "acc_" + System.currentTimeMillis().toString().takeLast(6),
                                        name = accName,
                                        provider = accProvider,
                                        walletNumber = accWallet,
                                        simSlot = accSlot,
                                        projectId = realProj
                                    )
                                    accName = ""
                                    accWallet = ""
                                    showAddAcc = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFB703),
                                contentColor = Color(0xFF020617)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text("SAVE MOBILE WALLET", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black))
                        }
                    }
                }

                // Render dynamic mapping accounts
                if (paymentAccounts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active wallet mapped.", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        paymentAccounts.forEach { acc ->
                            val linkedWebName = projects.find { it.id == acc.projectId }?.name ?: "No specific Web Domain"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFF192231), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhoneAndroid,
                                            contentDescription = "Wallet Icon",
                                            tint = Color(0xFFFFB703),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(acc.name, style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                                        Text("${acc.provider} • Wallet: ${acc.walletNumber}", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF22D3EE)))
                                        Text("Routes callbacks to: $linkedWebName", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFFFB703), fontSize = 10.sp))
                                    }
                                }
                                IconButton(onClick = { viewModel.deletePaymentAccount(acc) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete account", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // B. LIVE CAPTURED PAYMENTS LIST & LOG INDEX
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LIVE INTERCEPTED BANKING SMS",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold, color = Color.White
                    )
                )

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF64748B)) },
                    placeholder = { Text("Search by TXN, Sender details or reference...", color = Color(0xFF64748B), fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF22D3EE),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF22D3EE)
                    )
                )

                // Filter chips row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filterOptions = listOf("ALL", "BKASH", "NAGAD", "ROCKET", "UPAY")
                    filterOptions.forEach { opt ->
                        val isCurr = providerFilter == opt
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isCurr) Color(0xFF22D3EE) else Color(0xFF0F172A),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                .clickable { providerFilter = opt }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = opt,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isCurr) Color(0xFF020617) else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(8.dp))

                // Render dynamic transactions list
                val matchedTxns = transactions.filter { tx ->
                    val matchesQuery = tx.txnId.contains(searchQuery, ignoreCase = true) ||
                            tx.sender.contains(searchQuery, ignoreCase = true) ||
                            tx.senderNumber.contains(searchQuery, ignoreCase = true) ||
                            tx.reference.contains(searchQuery, ignoreCase = true) ||
                            tx.amount.toString().contains(searchQuery)

                    val matchesProv = providerFilter == "ALL" || tx.sender.uppercase() == providerFilter
                    matchesQuery && matchesProv
                }

                if (matchedTxns.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.SmsFailed, contentDescription = "Empty", tint = Color(0xFF475569), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No matching payments logged.", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)))
                        }
                    }
                } else {
                    matchedTxns.take(20).forEach { tx ->
                        LocalTransactionRow(tx = tx)
                    }
                }
            }
        }
    }
}

@Composable
fun LocalTransactionRow(tx: SmsTransaction) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "${tx.sender.uppercase()} RECEIVED PAYOUT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "TxnID: ${tx.txnId}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace
                        )
                    )
                }

                Text(
                    text = String.format(Locale.getDefault(), "৳%,.2f", tx.amount),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White, fontWeight = FontWeight.Black
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Ref: ${tx.reference.ifEmpty { "None" }}", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8)))
                    Text("Sender: ${tx.senderNumber.take(15)}", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B)))
                }

                // Render badge status
                val badgeColor = when (tx.syncStatus) {
                    "SUCCESS" -> Color(0xFF10B981)
                    "PENDING" -> Color(0xFFFFB703)
                    else -> Color(0xFFEF4444)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = badgeColor.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = tx.syncStatus,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = badgeColor, fontWeight = FontWeight.Black, fontSize = 9.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
