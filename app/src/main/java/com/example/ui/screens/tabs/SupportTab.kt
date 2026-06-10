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
import com.example.ui.viewmodel.GatewayViewModel
import com.example.ui.viewmodel.SupportTicket
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SupportTab(
    viewModel: GatewayViewModel,
    modifier: Modifier = Modifier
) {
    val supportTickets by viewModel.supportTickets.collectAsState()

    var showAddTicket by remember { mutableStateOf(false) }
    var tTitle by remember { mutableStateOf("") }
    var tCat by remember { mutableStateOf("Gateway Node") }
    var tPriority by remember { mutableStateOf("MEDIUM") }
    var tDesc by remember { mutableStateOf("") }

    var isApiKeyObscured by remember { mutableStateOf(true) }

    // FAQs collapse states
    var faq0Expanded by remember { mutableStateOf(false) }
    var faq1Expanded by remember { mutableStateOf(false) }
    var faq2Expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- FAQ CONSOLE CORNER ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MERCHANT USER KNOWLEDGE BASE (FAQ)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold, color = Color(0xFF22D3EE)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // FAQ 1
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().clickable { faq0Expanded = !faq0Expanded }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("1. How do I pair my hardware node securely?", style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                            Icon(imageVector = if (faq0Expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = "Expand", tint = Color(0xFF22D3EE))
                        }
                        AnimatedVisibility(visible = faq0Expanded) {
                            Text(
                                text = "Install the gateway client APK on an Android device running SIM slot lines. Log in using your merchant credentials, assert SMS interception permissions, and pair using the device register token in your settings console.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // FAQ 2
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().clickable { faq1Expanded = !faq1Expanded }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("2. What are the payout settlement clearance bounds?", style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                            Icon(imageVector = if (faq1Expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = "Expand", tint = Color(0xFF22D3EE))
                        }
                        AnimatedVisibility(visible = faq1Expanded) {
                            Text(
                                text = "Rapid digital payout settlements (bKash & Nagad) clear instantly to designated mobile finances within 30 seconds. Commercial banks settle within T+1 banking hours.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // FAQ 3
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().clickable { faq2Expanded = !faq2Expanded }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("3. Is the integration model iOS companion ready?", style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                            Icon(imageVector = if (faq2Expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = "Expand", tint = Color(0xFF22D3EE))
                        }
                        AnimatedVisibility(visible = faq2Expanded) {
                            Text(
                                text = "Yes. The backend services utilize Kotlin Multiplatform (KMP) specifications. Live statistics synchronization and alerts push notifications map directly to Apple Core frameworks.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- SECTION A: ACTIVE SUPPORT HELPDESK ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("support_helpdesk_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INTEGRATED CUSTOMER HELP DESK",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFB703)
                        )
                    )
                    IconButton(onClick = { showAddTicket = !showAddTicket }) {
                        Icon(
                            imageVector = if (showAddTicket) Icons.Default.Close else Icons.Default.AddCircle,
                            contentDescription = "Toggle support form",
                            tint = Color(0xFFFFB703)
                        )
                    }
                }

                Text(
                    text = "Submit technical complaints, configuration requests, or platform support, tracked in real-time.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                AnimatedVisibility(visible = showAddTicket) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Submit Support Complaint Ticket",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, color = Color.White)
                        )

                        OutlinedTextField(
                            value = tTitle,
                            onValueChange = { tTitle = it },
                            label = { Text("Brief Title (e.g. WooCommerce Callback Fail)", color = Color(0xFF64748B)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFB703),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedLabelColor = Color(0xFFFFB703)
                            )
                        )

                        // Category input
                        OutlinedTextField(
                            value = tCat,
                            onValueChange = { tCat = it },
                            label = { Text("Configuration Sector (e.g. WooCommerce, Node API)", color = Color(0xFF64748B)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFB703),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedLabelColor = Color(0xFFFFB703)
                            )
                        )

                        // Priority input
                        Column {
                            Text("Urgency Priority Index", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8)))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("LOW", "MEDIUM", "HIGH").forEach { pLevel ->
                                    val isCurr = tPriority == pLevel
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .background(
                                                if (isCurr) Color(0xFFFFB703) else Color(0xFF1E293B),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { tPriority = pLevel }
                                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = pLevel,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isCurr) Color(0xFF020617) else Color(0xFF94A3B8),
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = tDesc,
                            onValueChange = { tDesc = it },
                            label = { Text("Diagnostic description of the failure...", color = Color(0xFF64748B)) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFB703),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedLabelColor = Color(0xFFFFB703)
                            )
                        )

                        Button(
                            onClick = {
                                if (tTitle.isNotBlank() && tDesc.isNotBlank()) {
                                    viewModel.createSupportTicket(tTitle, tCat, tPriority, tDesc)
                                    tTitle = ""
                                    tDesc = ""
                                    showAddTicket = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFB703),
                                contentColor = Color(0xFF020617)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text("SAVE COMPLAINT TICKET", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black))
                        }
                    }
                }

                // Render dynamic Support tickets
                if (supportTickets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active tickets filed.", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        supportTickets.forEach { ticket ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = ticket.title,
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val pBg = when (ticket.priority) {
                                            "HIGH" -> Color(0xFFEF4444)
                                            "MEDIUM" -> Color(0xFFF59E0B)
                                            else -> Color(0xFF64748B)
                                        }
                                        Card(colors = CardDefaults.cardColors(containerColor = pBg.copy(alpha = 0.15f))) {
                                            Text(
                                                text = ticket.priority,
                                                style = MaterialTheme.typography.labelSmall.copy(color = pBg, fontWeight = FontWeight.Black, fontSize = 7.sp),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text("Category: ${ticket.category} • Status: ${ticket.status}", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8)))
                                    Text(ticket.description, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B)))
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION B: DEVELOPER PROFILE CREDENTIALS & iOS SYNC ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("developer_profile_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MERCHANT ACCOUNT IDENTITY (ME)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold, color = Color.White
                    )
                )

                Text(
                    text = "Verify system access tokens, companion linkages, and active subscription parameters.",
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
                            Text("DEVELOPER SECRET KEY", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8)))
                            Text(
                                text = if (isApiKeyObscured) "••••••••••••••••••••••••••••" else "sk_bd_928fj39fklsd9283fksdfs2098",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        IconButton(onClick = { isApiKeyObscured = !isApiKeyObscured }) {
                            Icon(
                                imageVector = if (isApiKeyObscured) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Obscure",
                                tint = Color(0xFF22D3EE),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1A2130))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("MERCHANT TIERS", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8)))
                            Text("ENTERPRISE PLATINUM", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFFB703), fontWeight = FontWeight.Bold))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("GATEWAY CLUSTER", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8)))
                            Text("CLUSTER BD-MAIN #1", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold))
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1A1F2C))

                    // CROSS-PLATFORM QR CODE COMPATIBILITY INTEGRITY CARD
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(55.dp)
                                    .background(Color.White, RoundedCornerShape(6.dp))
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Companion QR Target Scanner",
                                    tint = Color.Black,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "iOS TARGET COMPANION SYNC",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF38BDF8), fontWeight = FontWeight.ExtraBold)
                                )
                                Text(
                                    text = "Kotlin Multiplatform framework status: READY. Scan QR to connect Apple companion terminal.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8), fontSize = 10.sp),
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.logoutMerchant() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = "Log out icon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SECURE SYSTEM SESSION LOG OUT", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold))
                }
            }
        }
    }
}
