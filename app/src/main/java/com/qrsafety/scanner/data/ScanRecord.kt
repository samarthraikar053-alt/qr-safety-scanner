package com.qrsafety.scanner.data

import com.qrsafety.scanner.analyzer.ThreatLevel

data class ScanRecord(
    val id: Long = 0,
    val url: String,
    val host: String,
    val threatLevel: ThreatLevel,
    val warningSummary: String,
    val timestamp: Long = System.currentTimeMillis()
)
