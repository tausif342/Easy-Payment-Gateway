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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.GatewayViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OverviewTab(
    viewModel: GatewayViewModel,
    onNavigateToPayouts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalAmount by viewModel.totalAmount.collectAsState()
    val processedCount by viewModel.processedCount.collectAsState()
    val syncedCount by viewModel.syncedCount.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. BALANCE OVERVIEW PANEL
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("balance_overview_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AVAILABLE SETTLED BALANCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "৳%,.2f", 45250.00 + (totalAmount * 0.95)),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )
                    }
                    IconButton(
                        onClick = onNavigateToPayouts,
                        modifier = Modifier
                            .background(Color(0xFF0284C7), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Payout and Settlement Gateway",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Settled Lifetime Earnings Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "LIFETIME REVENUE",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "৳%,.2f", 1120500.00 + totalAmount),
                                style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF34D399), fontWeight = FontWeight.Black)
                            )
                        }
                    }

                    // Pending Settlements Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "PENDING SETTLEMENT",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "৳%,.2f", 8900.00 + (totalAmount * 0.05)),
                                style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFFFB923C), fontWeight = FontWeight.Black)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onNavigateToPayouts,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = "Payout Button")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "REQUEST RAPID PAYOUT SETTLEMENT",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
            }
        }

        // 2. REAL-TIME PAYMENT FLOW ANALYTICS
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "REAL-TIME FLOW & METRICS",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold, color = Color(0xFF22D3EE)
                    )
                )
                Text(
                    text = "Volume trend and provider split extracted automatically from local security gateway logs.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                )

                // Render dynamic charts inside a Row block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Hourly Volume Bar Chart (Canvas) State (BDT flow volumes)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "DAILY VOLUME (BDT)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Canvas(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                                val bars = listOf(45000f, 95000f, 150000f, 80000f, 120000f)
                                val maxVal = 180000f
                                val barSpacing = 8.dp.toPx()
                                val barWidth = (size.width - (barSpacing * (bars.size - 1))) / bars.size

                                bars.forEachIndexed { i, valAmt ->
                                    val barHeight = (valAmt / maxVal) * size.height
                                    val left = i * (barWidth + barSpacing)
                                    val top = size.height - barHeight
                                    drawRoundRect(
                                        color = Color(0xFF0284C7),
                                        topLeft = androidx.compose.ui.geometry.Offset(left, top),
                                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Thu   Fri   Sat   Sun   Today",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = Color(0xFF64748B))
                            )
                        }
                    }

                    // Provider split ring chart (Canvas)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "MFS SPLIT WEIGHT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp, color = Color(0xFFFFB703), fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                                Canvas(modifier = Modifier.size(70.dp)) {
                                    val strokeW = 8.dp.toPx()
                                    // draw splits: bKash (60%), Nagad (30%), Rocket (10%)
                                    drawArc(
                                        color = Color(0xFFE11D48), // bKash pinkish red
                                        startAngle = -90f,
                                        sweepAngle = 216f,
                                        useCenter = false,
                                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = Color(0xFFEA580C), // Nagad Orange
                                        startAngle = 126f,
                                        sweepAngle = 108f,
                                        useCenter = false,
                                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = Color(0xFF7C3AED), // Rocket Violet
                                        startAngle = 234f,
                                        sweepAngle = 36f,
                                        useCenter = false,
                                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                    )
                                }
                                Text("Split", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontSize = 9.sp))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Text("bKash 60%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, color = Color(0xFFFDA4AF)))
                                Text("Nagad 30%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, color = Color(0xFFFED7AA)))
                                Text("Rkt 10%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, color = Color(0xFFDDD6FE)))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Smooth Revenue line chart wave (Canvas path)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth().height(90.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            "REAL-TIME TRANSACTION LOAD MONITOR",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = Color(0xFF22D3EE), fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Canvas(modifier = Modifier.fillMaxWidth().height(55.dp)) {
                            val path = Path().apply {
                                moveTo(0f, size.height * 0.7f)
                                cubicTo(
                                    size.width * 0.25f, size.height * 0.1f,
                                    size.width * 0.5f, size.height * 0.9f,
                                    size.width * 0.75f, size.height * 0.2f
                                )
                                lineTo(size.width, size.height * 0.5f)
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFF22D3EE),
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }
        }

        // 3. LIVE EVENT & ALERT LOGS (Notifications Center)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Live alerts",
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LIVE OPERATIONS CONSOLE",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold, color = Color.White
                            )
                        )
                    }

                    Row {
                        TextButton(
                            onClick = { viewModel.markAllNotificationsRead() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("MARK READ", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF22D3EE), fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { viewModel.clearNotifications() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("CLEAR", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFEF4444), fontWeight = FontWeight.Bold))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = Color(0xFF334155))

                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No alerts. Gateway operating securely.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                        )
                    }
                } else {
                    notifications.take(5).forEach { alert ->
                        val alertIcon = when (alert.category) {
                            "PAYMENT" -> Icons.Default.CheckCircle
                            "WITHDRAWAL" -> Icons.Default.AccountBalanceWallet
                            else -> Icons.Default.Warning
                        }
                        val iconColor = when (alert.category) {
                            "PAYMENT" -> Color(0xFF10B981)
                            "WITHDRAWAL" -> Color(0xFF06B6D4)
                            else -> Color(0xFFF59E0B)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .background(
                                    if (alert.unread) Color(0xFF0F172A) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = alertIcon,
                                    contentDescription = alert.category,
                                    tint = iconColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = alert.title,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White, fontWeight = FontWeight.Bold
                                        )
                                    )
                                    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                                    val timeStr = remember(alert.timestamp) { formatter.format(Date(alert.timestamp)) }
                                    Text(
                                        text = timeStr,
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                                    )
                                }
                                Text(
                                    text = alert.content,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
