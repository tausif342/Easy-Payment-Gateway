package com.example.ui.screens.tabs

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import com.example.data.model.Project
import com.example.ui.viewmodel.GatewayViewModel
import com.example.ui.viewmodel.WithdrawRequest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PortalTab(
    viewModel: GatewayViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    val withdrawRequests by viewModel.withdrawRequests.collectAsState()
    val totalAmount by viewModel.totalAmount.collectAsState()

    // 1. Add Website state
    var showAddWeb by remember { mutableStateOf(false) }
    var webId by remember { mutableStateOf("") }
    var webName by remember { mutableStateOf("") }
    var webUrl by remember { mutableStateOf("") }

    // 2. Withdrawal state
    var pAmount by remember { mutableStateOf("") }
    var pGateway by remember { mutableStateOf("bKash") }
    var pAccount by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECTION A: WEBSITES MANAGEMENT ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("projects_management_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INTEGRATED BUSINESS PORTALS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold, color = Color(0xFF22D3EE)
                        )
                    )
                    IconButton(onClick = { showAddWeb = !showAddWeb }) {
                        Icon(
                            imageVector = if (showAddWeb) Icons.Default.Close else Icons.Default.AddCircle,
                            contentDescription = "Toggle Projects registration",
                            tint = Color(0xFF22D3EE)
                        )
                    }
                }

                Text(
                    text = "Configure WooCommerce plugins, custom PHP checkouts, or APIs to disperse mobile payouts securely.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                AnimatedVisibility(visible = showAddWeb) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Register New Website Portal",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, color = Color.White)
                        )

                        OutlinedTextField(
                            value = webId,
                            onValueChange = { webId = it },
                            label = { Text("Web Store Reference Key (e.g. shop_bd)", color = Color(0xFF64748B)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF22D3EE),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedLabelColor = Color(0xFF22D3EE)
                            )
                        )

                        OutlinedTextField(
                            value = webName,
                            onValueChange = { webName = it },
                            label = { Text("Business Portal Name (e.g. BanglaCart LLC)", color = Color(0xFF64748B)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF22D3EE),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedLabelColor = Color(0xFF22D3EE)
                            )
                        )

                        OutlinedTextField(
                            value = webUrl,
                            onValueChange = { webUrl = it },
                            label = { Text("Webhook Callback URL Target", color = Color(0xFF64748B)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF22D3EE),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedLabelColor = Color(0xFF22D3EE)
                            )
                        )

                        Button(
                            onClick = {
                                if (webId.isNotBlank() && webName.isNotBlank() && webUrl.isNotBlank()) {
                                    viewModel.addProject(webId, webName, webUrl)
                                    webId = ""
                                    webName = ""
                                    webUrl = ""
                                    showAddWeb = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF22D3EE),
                                contentColor = Color(0xFF020617)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text("SAVE BUSINESS CHANNELS", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black))
                        }
                    }
                }

                // Render dynamic portals
                if (projects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active channels. Using default fallback configuration.", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        projects.forEach { pr ->
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
                                            .background(Color(0xFF134E5E).copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = "Web Portal Icon",
                                            tint = Color(0xFF22D3EE),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(pr.name, style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                                        Text("Config ID: ${pr.id}", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontFamily = FontFamily.Monospace))
                                        Text(pr.websiteUrl, style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF22D3EE)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteProject(pr) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete channel", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION B: PAYOUT SETTLEMENT TRANSFERS & LOGS ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("settlement_payout_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "INITIATE AUTOMATED PAYOUT",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFB703)
                    )
                )

                Text(
                    text = "Disburse funds immediately into bKash Agent, Nagad Wallet, or your linked Commercial Bank details.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Render withdrawal form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = pAmount,
                        onValueChange = { pAmount = it },
                        label = { Text("Transfer Cash Amount (BDT)", color = Color(0xFF64748B)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFB703),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFFFFB703)
                        )
                    )

                    OutlinedTextField(
                        value = pAccount,
                        onValueChange = { pAccount = it },
                        label = { Text("Target Wallet Number / Bank Acc. Number", color = Color(0xFF64748B)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFB703),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFFFFB703)
                        )
                    )

                    // Transfer Medium Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("bKash", "Nagad", "Bank").forEach { platform ->
                            val isChosen = pGateway.lowercase() == platform.lowercase()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .background(
                                        if (isChosen) Color(0xFFFFB703) else Color(0xFF1E293B),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { pGateway = platform }
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = platform,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isChosen) Color(0xFF020617) else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val amt = pAmount.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0 && pAccount.isNotBlank()) {
                                viewModel.requestWithdrawal(amt, pGateway, pAccount)
                                pAmount = ""
                                pAccount = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB703),
                            contentColor = Color(0xFF020617)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Text("DISBURSE SETTLEMENT PAYOUT", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "HISTORIC PAYOUT OUTFLOW PROGRESS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                )
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(6.dp))

                // Render logs timeline
                if (withdrawRequests.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No payouts registered.", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)))
                    }
                } else {
                    withdrawRequests.forEach { req ->
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = req.id,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = req.gateway,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text("Amount: BDT ${req.amount.toInt()} • Target: ${req.accountNumber}", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8)))
                                val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
                                val dateStr = remember(req.timestamp) { formatter.format(Date(req.timestamp)) }
                                Text(dateStr, style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF475569), fontSize = 9.sp))
                            }

                            // Interactive status blink overlay
                            if (req.status == "PENDING") {
                                val infiniteBlink = rememberInfiniteTransition()
                                val alphaScale by infiniteBlink.animateFloat(
                                    initialValue = 0.2f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(800, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFFF59E0B).copy(alpha = alphaScale), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "PROCESSING",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "APPROVED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF10B981), fontWeight = FontWeight.Bold
                                        )
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
