package com.qrsafety.scanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qrsafety.scanner.analyzer.ThreatLevel
import com.qrsafety.scanner.analyzer.WarningDetail
import com.qrsafety.scanner.ui.theme.DangerousRed
import com.qrsafety.scanner.ui.theme.DangerousRedContainer
import com.qrsafety.scanner.ui.theme.SafeGreen
import com.qrsafety.scanner.ui.theme.SafeGreenContainer
import com.qrsafety.scanner.ui.theme.SurfaceLightDark
import com.qrsafety.scanner.ui.theme.SuspiciousAmber
import com.qrsafety.scanner.ui.theme.SuspiciousAmberContainer
import com.qrsafety.scanner.ui.theme.TextPrimary
import com.qrsafety.scanner.ui.theme.TextSecondary

@Composable
fun WarningItemCard(
    warning: WarningDetail,
    modifier: Modifier = Modifier
) {
    val (iconColor, containerColor, icon) = when (warning.severity) {
        ThreatLevel.SAFE -> Triple(SafeGreen, SafeGreenContainer, Icons.Default.Info)
        ThreatLevel.SUSPICIOUS -> Triple(SuspiciousAmber, SuspiciousAmberContainer, Icons.Default.Warning)
        ThreatLevel.HIGH_RISK -> Triple(DangerousRed, DangerousRedContainer, Icons.Default.Dangerous)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceLightDark.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = warning.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = warning.description,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
