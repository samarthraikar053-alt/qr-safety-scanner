package com.qrsafety.scanner.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qrsafety.scanner.analyzer.AnalysisResult
import com.qrsafety.scanner.analyzer.ThreatLevel
import com.qrsafety.scanner.ui.components.VerdictBadge
import com.qrsafety.scanner.ui.components.WarningItemCard
import com.qrsafety.scanner.ui.theme.DangerousRed
import com.qrsafety.scanner.ui.theme.DangerousRedContainer
import com.qrsafety.scanner.ui.theme.DangerousRedText
import com.qrsafety.scanner.ui.theme.PrimaryBlue
import com.qrsafety.scanner.ui.theme.SafeGreen
import com.qrsafety.scanner.ui.theme.SafeGreenContainer
import com.qrsafety.scanner.ui.theme.SurfaceDark
import com.qrsafety.scanner.ui.theme.SurfaceLightDark
import com.qrsafety.scanner.ui.theme.SuspiciousAmber
import com.qrsafety.scanner.ui.theme.SuspiciousAmberContainer
import com.qrsafety.scanner.ui.theme.TextMuted
import com.qrsafety.scanner.ui.theme.TextPrimary
import com.qrsafety.scanner.ui.theme.TextSecondary

@Composable
fun SafetyDialog(
    analysis: AnalysisResult,
    onOpenAnyway: (String) -> Unit,
    onDontOpen: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDontOpen,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Threat Icon Header
                val (headerIcon, headerColor, headerBg) = when (analysis.threatLevel) {
                    ThreatLevel.SAFE -> Triple(Icons.Default.Shield, SafeGreen, SafeGreenContainer)
                    ThreatLevel.SUSPICIOUS -> Triple(Icons.Default.Warning, SuspiciousAmber, SuspiciousAmberContainer)
                    ThreatLevel.HIGH_RISK -> Triple(Icons.Default.Dangerous, DangerousRed, DangerousRedContainer)
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(headerBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = headerIcon,
                        contentDescription = null,
                        tint = headerColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val headerTitle = when (analysis.threatLevel) {
                    ThreatLevel.SAFE -> "Verified Safe Link"
                    ThreatLevel.SUSPICIOUS -> "Suspicious Link Warning"
                    ThreatLevel.HIGH_RISK -> "High Risk Alert!"
                }

                Text(
                    text = headerTitle,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                VerdictBadge(threatLevel = analysis.threatLevel)

                Spacer(modifier = Modifier.height(16.dp))

                // Must-Have #2: URL Extraction & Display
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceLightDark)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EMBEDDED URL",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Scanned URL", analysis.parsedUrl)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Link",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = analysis.parsedUrl,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Host Domain: ${analysis.host}",
                        color = PrimaryBlue,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Must-Have #3: Heuristics Assessment and Flags
                if (analysis.warnings.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (analysis.isSafe) "Security Details" else "Triggered Security Flags (${analysis.warnings.size})",
                            color = if (analysis.isHighRisk) DangerousRedText else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        analysis.warnings.forEach { warning ->
                            WarningItemCard(warning = warning)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Must-Have #4: Action Buttons: "Don't open" and "Open anyway"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Safe primary button: "Don't open"
                    Button(
                        onClick = onDontOpen,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (analysis.isHighRisk || analysis.isSuspicious) PrimaryBlue else SurfaceLightDark,
                            contentColor = if (analysis.isHighRisk || analysis.isSuspicious) Color(0xFF0F172A) else TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Don't open",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Secondary button: "Open anyway"
                    OutlinedButton(
                        onClick = { onOpenAnyway(analysis.parsedUrl) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (analysis.isHighRisk) DangerousRedText else TextPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (analysis.isHighRisk) DangerousRed.copy(alpha = 0.5f) else TextMuted.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Open anyway",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
