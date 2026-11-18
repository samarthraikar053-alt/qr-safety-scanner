package com.qrsafety.scanner.analyzer

enum class ThreatLevel(val displayName: String) {
    SAFE("Safe"),
    SUSPICIOUS("Suspicious"),
    HIGH_RISK("High Risk")
}

data class WarningDetail(
    val title: String,
    val description: String,
    val severity: ThreatLevel
)

data class AnalysisResult(
    val rawContent: String,
    val parsedUrl: String,
    val host: String,
    val threatLevel: ThreatLevel,
    val warnings: List<WarningDetail>,
    val isWebUrl: Boolean
) {
    val isSafe: Boolean get() = threatLevel == ThreatLevel.SAFE
    val isHighRisk: Boolean get() = threatLevel == ThreatLevel.HIGH_RISK
    val isSuspicious: Boolean get() = threatLevel == ThreatLevel.SUSPICIOUS
}
