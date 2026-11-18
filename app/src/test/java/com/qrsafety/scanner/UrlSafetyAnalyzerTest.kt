package com.qrsafety.scanner

import com.qrsafety.scanner.analyzer.ThreatLevel
import com.qrsafety.scanner.analyzer.UrlSafetyAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlSafetyAnalyzerTest {

    @Test
    fun testMissingHttpsFlaggedAsHighRisk() {
        val result = UrlSafetyAnalyzer.analyze("http://insecure-website.com")
        assertEquals(ThreatLevel.HIGH_RISK, result.threatLevel)
        assertTrue(result.warnings.any { it.title.contains("Missing HTTPS", ignoreCase = true) })
    }

    @Test
    fun testIpAddressInsteadOfDomainFlaggedAsHighRisk() {
        val result = UrlSafetyAnalyzer.analyze("http://192.168.1.1/admin")
        assertEquals(ThreatLevel.HIGH_RISK, result.threatLevel)
        assertTrue(result.warnings.any { it.title.contains("Raw IP Address", ignoreCase = true) })
    }

    @Test
    fun testLinkShortenerFlaggedAsSuspicious() {
        val result = UrlSafetyAnalyzer.analyze("https://bit.ly/secure-doc-link")
        assertEquals(ThreatLevel.SUSPICIOUS, result.threatLevel)
        assertTrue(result.warnings.any { it.title.contains("Link Shortener", ignoreCase = true) })
    }

    @Test
    fun testSuspiciousKeywordsLoginVerifyFlagged() {
        val result = UrlSafetyAnalyzer.analyze("https://login-verify-service.com")
        assertEquals(ThreatLevel.HIGH_RISK, result.threatLevel)
        assertTrue(result.warnings.any { it.title.contains("Phishing Keywords", ignoreCase = true) })
    }

    @Test
    fun testSuspiciousKeywordsBankingAndSecureUpdateFlagged() {
        val result = UrlSafetyAnalyzer.analyze("https://portal-secure-update.com/banking")
        assertEquals(ThreatLevel.HIGH_RISK, result.threatLevel)
        assertTrue(result.warnings.any { it.title.contains("Phishing Keywords", ignoreCase = true) })
    }

    @Test
    fun testMultipleFlagsCombined() {
        // HTTP + IP + login-verify
        val result = UrlSafetyAnalyzer.analyze("http://192.168.1.100/login-verify")
        assertEquals(ThreatLevel.HIGH_RISK, result.threatLevel)
        assertTrue(result.warnings.any { it.title.contains("Missing HTTPS", ignoreCase = true) })
        assertTrue(result.warnings.any { it.title.contains("Raw IP Address", ignoreCase = true) })
        assertTrue(result.warnings.any { it.title.contains("Phishing Keywords", ignoreCase = true) })
    }

    @Test
    fun testSafeHttpsUrl() {
        val result = UrlSafetyAnalyzer.analyze("https://www.google.com")
        assertEquals(ThreatLevel.SAFE, result.threatLevel)
        assertTrue(result.warnings.isEmpty())
    }
}
