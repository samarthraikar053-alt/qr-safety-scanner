package com.qrsafety.scanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qrsafety.scanner.analyzer.ThreatLevel
import com.qrsafety.scanner.ui.components.VerdictBadge
import com.qrsafety.scanner.ui.theme.DarkBackground
import com.qrsafety.scanner.ui.theme.PrimaryBlue
import com.qrsafety.scanner.ui.theme.SurfaceDark
import com.qrsafety.scanner.ui.theme.SurfaceLightDark
import com.qrsafety.scanner.ui.theme.TextMuted
import com.qrsafety.scanner.ui.theme.TextPrimary
import com.qrsafety.scanner.ui.theme.TextSecondary

data class TestScenario(
    val title: String,
    val description: String,
    val testUrl: String,
    val expectedThreat: ThreatLevel
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestSamplesScreen(
    onBack: () -> Unit,
    onTestUrlSelected: (String) -> Unit
) {
    var customUrlInput by remember { mutableStateOf("") }

    val testScenarios = remember {
        listOf(
            TestScenario(
                title = "1. Missing HTTPS (Plain HTTP)",
                description = "Triggers missing HTTPS warning flag",
                testUrl = "http://insecure-example.com/login",
                expectedThreat = ThreatLevel.HIGH_RISK
            ),
            TestScenario(
                title = "2. IP Address Host",
                description = "Triggers raw IP address host heuristic",
                testUrl = "http://192.168.1.1/dashboard",
                expectedThreat = ThreatLevel.HIGH_RISK
            ),
            TestScenario(
                title = "3. Link Shortener (Obfuscation)",
                description = "Triggers known shortener service obfuscation",
                testUrl = "https://bit.ly/secure-account-redirect",
                expectedThreat = ThreatLevel.SUSPICIOUS
            ),
            TestScenario(
                title = "4. Suspicious Keywords (login-verify)",
                description = "Triggers phishing keyword heuristic",
                testUrl = "https://login-verify-account.com/portal",
                expectedThreat = ThreatLevel.HIGH_RISK
            ),
            TestScenario(
                title = "5. Keywords (banking & secure-update)",
                description = "Triggers social engineering keywords heuristic",
                testUrl = "https://update-secure-banking-portal.xyz/claim",
                expectedThreat = ThreatLevel.HIGH_RISK
            ),
            TestScenario(
                title = "6. Verified Safe Link",
                description = "Genuine HTTPS link with no suspicious indicators",
                testUrl = "https://www.google.com",
                expectedThreat = ThreatLevel.SAFE
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Security Heuristics Sandbox",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Tap any test case below to evaluate the smart security analyzer against each heuristic in real-time:",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            // Custom URL test input bar
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "TEST ANY CUSTOM URL",
                        color = PrimaryBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. http://10.0.0.1/free-gift", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = SurfaceLightDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (customUrlInput.isNotBlank()) {
                                onTestUrlSelected(customUrlInput.trim())
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        enabled = customUrlInput.isNotBlank()
                    ) {
                        Text("Analyze Custom URL", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "PRESET HEURISTIC TEST SCENARIOS",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(testScenarios) { scenario ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .clickable { onTestUrlSelected(scenario.testUrl) }
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = scenario.title,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            VerdictBadge(threatLevel = scenario.expectedThreat)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = scenario.description,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceLightDark)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = scenario.testUrl,
                                color = PrimaryBlue,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Run Test",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
