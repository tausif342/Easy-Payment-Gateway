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
