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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.GatewayViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DevicesTab(
    viewModel: GatewayViewModel,
    modifier: Modifier = Modifier
) {
    val isGatewayActive by viewModel.isGatewayActive.collectAsState()
    val isApproved by viewModel.isApproved.collectAsState()
    val deviceId by viewModel.deviceId.collectAsState()
    val deviceName by viewModel.deviceName.collectAsState()
    val deviceLimit by viewModel.deviceLimit.collectAsState()

    val geminiParseLoading by viewModel.geminiParseLoading.collectAsState()
    val geminiParseResult by viewModel.geminiParseResult.collectAsState()

    var customSender by remember { mutableStateOf("bKash") }
    var customBody by remember { mutableStateOf("You have received Tk 2,500.00 from 01711223344. Remaining Bal Tk 15,200.00. TrxID 8N34JG98DL at 02/06/2026 14:35. Ref: directRef") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECTION A: ACTIVE GATEWAY DEVICE METRICS ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("device_diagnostics_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "HARDWARE GATEWAY NODES",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold, color = Color(0xFF22D3EE)
                    )
                )

                Text(
                    text = "Real-time diagnostic telemetry of the paired physical Android interception devices.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = deviceName.ifEmpty { "Paired Gateway Node #1" },
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "DeviceID: ${deviceId.take(16)}...",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                            )
                        }

                        // Gateway Node status pill
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isGatewayActive) Color(0xFF064E3B) else Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (isGatewayActive) Color(0xFF10B981) else Color(0xFF94A3B8), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isGatewayActive) "LISTENING" else "DORMANT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isGatewayActive) Color(0xFF34D399) else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1E293B))

                    // Hardware stats panel grid row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("BATTERY PROFILE", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 8.sp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.BatteryChargingFull, contentDescription = "Battery", tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("88% AC POWER", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("LINK CONNECTION", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 8.sp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Wifi, contentDescription = "WiFi", tint = Color(0xFF22D3EE), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WIFI STATUS (5G)", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("CARRIER SIM PROFILES", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 8.sp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.NetworkCell, contentDescription = "SIM Slots", tint = Color(0xFFFFB703), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("2 SLOT BOARDS", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1E293B))

                    // Self Healing daemon running badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FOREGROUND SERVICE DAEMON:",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF34D399), CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Resilient Thread Active",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF34D399), fontWeight = FontWeight.Black)
                            )
                        }
                    }
                }
            }
        }

        // --- SECTION B: INTERCEPT SMS DEVELOPER SANDBOX EMULATOR ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("sms_simulator_console")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SANDBOX EVALUATION EMULATOR",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFB703)
                    )
                )

                Text(
                    text = "Simulate raw SMS payloads locally to trace parsing logic, persistent storage checks and sync triggers.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // banking presets buttons row
                    Text("QUICK BANKING PRESETS", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontWeight = FontWeight.Bold))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                customSender = "bKash"
                                customBody = "You have received Tk 1,500.00 from 01711223344. Remaining Bal Tk 15,200.00. TrxID 8N34JG98DL at 02/06/2026 14:35. Ref: testA"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(30.dp)
                        ) {
                            Text("bKash BDT 1.5K", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color.White))
                        }

                        Button(
                            onClick = {
                                customSender = "Nagad"
                                customBody = "Cash In Tk 5,000.00 from 01888777666 successful. Remaining Bal Tk 45,250.00. TxnID 9R8FL35DK at 02/06/2026 16:40"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(30.dp)
                        ) {
                            Text("Nagad BDT 5K", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color.White))
                        }

                        Button(
                            onClick = {
                                customSender = "Rocket"
                                customBody = "You have received Tk 12,000.00 from 01999888777. Remaining Bal Tk 68,000.00. TrxID 3Y4FL982 at 01/06/2026 12:15"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(30.dp)
                        ) {
                            Text("Rocket BDT 12K", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color.White))
                        }
                    }

                    OutlinedTextField(
                        value = customSender,
                        onValueChange = { customSender = it },
                        label = { Text("Sender Name / Identification", color = Color(0xFF64748B)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFB703),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFFFFB703)
                        )
                    )

                    OutlinedTextField(
                        value = customBody,
                        onValueChange = { customBody = it },
                        label = { Text("SMS Messages Payload Context", color = Color(0xFF64748B)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFB703),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = Color(0xFFFFB703)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.simulateSmsReceived(customSender, customBody)
                            },
                            enabled = isApproved && isGatewayActive,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFB703),
                                contentColor = Color(0xFF020617)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f).height(40.dp)
                        ) {
                            Text("SIMULATE", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black))
                        }

                        Button(
                            onClick = {
                                viewModel.parseSmsWithGemini(customBody)
                            },
                            enabled = !geminiParseLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF22D3EE),
                                contentColor = Color(0xFF020617)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            if (geminiParseLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF020617), strokeWidth = 1.5.dp)
                            } else {
                                Icon(imageVector = Icons.Default.Flare, contentDescription = "Gemini", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("A.I. EXTRACT", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    // Render Gemini parse trace summary
                    AnimatedVisibility(visible = geminiParseResult != null) {
                        geminiParseResult?.let { res ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    "GEMINI INTELLIGENCE REPORT:",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF22D3EE), fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Extracted Amount: ${res.currency ?: "BDT"} ${res.amount ?: "N/A"}", style = MaterialTheme.typography.labelSmall.copy(color = Color.White))
                                Text("Carrier Stream Sender: ${res.sender ?: "N/A"}", style = MaterialTheme.typography.labelSmall.copy(color = Color.White))
                                Text("Timestamp / Metadata: ${res.date ?: "N/A"}", style = MaterialTheme.typography.labelSmall.copy(color = Color.White))
                            }
                        }
                    }
                }
            }
        }
    }
}
