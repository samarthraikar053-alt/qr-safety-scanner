package com.qrsafety.scanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qrsafety.scanner.analyzer.ThreatLevel
import com.qrsafety.scanner.ui.theme.DangerousRed
import com.qrsafety.scanner.ui.theme.DangerousRedContainer
import com.qrsafety.scanner.ui.theme.DangerousRedText
import com.qrsafety.scanner.ui.theme.SafeGreen
import com.qrsafety.scanner.ui.theme.SafeGreenContainer
import com.qrsafety.scanner.ui.theme.SafeGreenText
import com.qrsafety.scanner.ui.theme.SuspiciousAmber
import com.qrsafety.scanner.ui.theme.SuspiciousAmberContainer
import com.qrsafety.scanner.ui.theme.SuspiciousAmberText

@Composable
fun VerdictBadge(
    threatLevel: ThreatLevel,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, iconColor, label, icon) = when (threatLevel) {
        ThreatLevel.SAFE -> Quint(
            SafeGreenContainer,
            SafeGreenText,
            SafeGreen,
            "SAFE",
            Icons.Default.CheckCircle
        )
        ThreatLevel.SUSPICIOUS -> Quint(
            SuspiciousAmberContainer,
            SuspiciousAmberText,
            SuspiciousAmber,
            "SUSPICIOUS",
            Icons.Default.Warning
        )
        ThreatLevel.HIGH_RISK -> Quint(
            DangerousRedContainer,
            DangerousRedText,
            DangerousRed,
            "HIGH RISK",
            Icons.Default.Dangerous
        )
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class Quint<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
