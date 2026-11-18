package com.qrsafety.scanner.analyzer

import java.net.URI
import java.util.Locale
import java.util.regex.Pattern

object UrlSafetyAnalyzer {

    // Common URL shortener domains (obfuscation services)
    private val KNOWN_SHORTENERS = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "is.gd", "buff.ly", "ow.ly",
        "cutt.ly", "rb.gy", "qr.ae", "shorturl.at", "rebrand.ly",
        "tr.ee", "v.gd", "cli.re", "s.id", "bl.ink", "tiny.cc", "adf.ly",
        "bc.vc", "linktr.ee", "lnkd.in", "snip.ly", "shorte.st", "trib.al"
    )

    // Suspicious deceptive / phishing keywords and patterns
    private val SUSPICIOUS_WORDS = listOf(
        "login-verify",
        "secure-update",
        "account-confirm",
        "banking",
        "free-gift",
        "login",
        "signin",
        "sign-in",
        "verify",
        "verification",
        "verify-account",
        "account-update",
        "update-account",
        "secure-login",
        "wallet",
        "credential",
        "giftcard",
        "reward-claim",
        "claim-prize",
        "password-reset",
        "support-service",
        "kyc-verify",
        "auth-checkpoint"
    )

    // Common High-Risk Top Level Domains often abused by disposable phishing campaigns
    private val RISKY_TLDS = setOf(
        "xyz", "top", "work", "click", "loan", "zip", "mov", "gq", "cf", "ml", "ga", "tk", "country"
    )

    // IPv4 Address Pattern
    private val IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    )

    // IPv6 Address Pattern
    private val IPV6_PATTERN = Pattern.compile("^[0-9a-fA-F:]+$")

    fun analyze(input: String): AnalysisResult {
        val trimmed = input.trim()
        val warnings = mutableListOf<WarningDetail>()

        // Check if string is formatted as or resembles a web URL
        val looksLikeUrl = trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("www.", ignoreCase = true) ||
                (trimmed.contains(".") && !trimmed.contains(" ") && trimmed.length > 3)

        if (!looksLikeUrl) {
            return AnalysisResult(
                rawContent = trimmed,
                parsedUrl = trimmed,
                host = "Plain Text",
                threatLevel = ThreatLevel.SAFE,
                warnings = listOf(
                    WarningDetail(
                        title = "Plain Text Content",
                        description = "The scanned QR code contains text rather than a web link.",
                        severity = ThreatLevel.SAFE
                    )
                ),
                isWebUrl = false
            )
        }

        // Normalize URL with protocol for parsing
        val normalizedUrl = when {
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "http://$trimmed" // Assume http if missing to test safety
        }

        var scheme = ""
        var host = ""
        var port = -1

        try {
            val jUri = URI(normalizedUrl)
            scheme = jUri.scheme?.lowercase(Locale.ROOT) ?: ""
            host = jUri.host?.lowercase(Locale.ROOT) ?: ""
            port = jUri.port
        } catch (_: Exception) {
            // Handled below via fallback
        }

        // Fallback string decomposition if standard URI parsing didn't extract host
        if (host.isEmpty()) {
            val schemeIndex = normalizedUrl.indexOf("://")
            val urlWithoutScheme = if (schemeIndex != -1) {
                scheme = normalizedUrl.substring(0, schemeIndex).lowercase(Locale.ROOT)
                normalizedUrl.substring(schemeIndex + 3)
            } else {
                normalizedUrl
            }

            val hostPortSection = urlWithoutScheme.substringBefore("/").substringBefore("?").substringBefore("#")
            val hostPart = if (hostPortSection.contains("@")) {
                hostPortSection.substringAfterLast("@")
            } else {
                hostPortSection
            }

            if (hostPart.contains(":") && !hostPart.startsWith("[")) {
                val parts = hostPart.split(":")
                host = parts[0].lowercase(Locale.ROOT)
                port = parts.getOrNull(1)?.toIntOrNull() ?: -1
            } else {
                host = hostPart.lowercase(Locale.ROOT)
            }
        }

        val fullUrlLower = normalizedUrl.lowercase(Locale.ROOT)

        // 1. MUST-HAVE HEURISTIC: Missing HTTPS (Plain HTTP)
        if (scheme == "http") {
            warnings.add(
                WarningDetail(
                    title = "Missing HTTPS (Plain HTTP)",
                    description = "This URL uses unencrypted HTTP instead of secure HTTPS. Unencrypted traffic can be intercepted, spoofed, or manipulated by bad actors.",
                    severity = ThreatLevel.HIGH_RISK
                )
            )
        } else if (scheme != "https") {
            warnings.add(
                WarningDetail(
                    title = "Unusual Protocol ($scheme)",
                    description = "Uses a non-standard scheme ($scheme://) which can trigger unintended device or application intents.",
                    severity = ThreatLevel.HIGH_RISK
                )
            )
        }

        // 2. MUST-HAVE HEURISTIC: IP Address Host
        val cleanHost = host.trim().trim('[', ']')
        val isIpv4 = IPV4_PATTERN.matcher(cleanHost).matches()
        val isIpv6 = cleanHost.contains(":") && IPV6_PATTERN.matcher(cleanHost).matches()

        if (isIpv4 || isIpv6) {
            warnings.add(
                WarningDetail(
                    title = "Raw IP Address Host",
                    description = "The URL domain is represented by a raw numeric IP address ($cleanHost) instead of a verified domain name. This is a common indicator of phishing and malicious drop-sites.",
                    severity = ThreatLevel.HIGH_RISK
                )
            )
        }

        // 3. MUST-HAVE HEURISTIC: Link Shorteners (Obfuscation Services)
        val isShortener = KNOWN_SHORTENERS.any { shortener ->
            cleanHost == shortener || cleanHost.endsWith(".$shortener")
        }
        if (isShortener) {
            warnings.add(
                WarningDetail(
                    title = "Link Shortener Detected",
                    description = "The link uses a URL shortener service ($cleanHost) which hides the final destination website.",
                    severity = ThreatLevel.SUSPICIOUS
                )
            )
        }

        // 4. MUST-HAVE HEURISTIC: Suspicious / Phishing Keywords
        val matchedOddWords = mutableListOf<String>()
        for (keyword in SUSPICIOUS_WORDS) {
            if (fullUrlLower.contains(keyword)) {
                matchedOddWords.add(keyword)
            }
        }
        if (matchedOddWords.isNotEmpty()) {
            val wordsList = matchedOddWords.take(4).joinToString(", ") { "\"$it\"" }
            warnings.add(
                WarningDetail(
                    title = "Suspicious Phishing Keywords",
                    description = "Contains sensitive social engineering keywords: $wordsList. Attackers frequently use these patterns to impersonate genuine services.",
                    severity = ThreatLevel.HIGH_RISK
                )
            )
        }

        // 5. Additional Heuristic: Deceptive '@' character in URL (Host spoofing)
        if (trimmed.contains("@")) {
            warnings.add(
                WarningDetail(
                    title = "Deceptive '@' Character Spoof",
                    description = "The URL includes an '@' symbol, a technique used in phishing to disguise the real host server.",
                    severity = ThreatLevel.HIGH_RISK
                )
            )
        }

        // 6. Additional Heuristic: Punycode / IDN Homograph domain
        if (cleanHost.contains("xn--")) {
            warnings.add(
                WarningDetail(
                    title = "Punycode Homograph Domain",
                    description = "Domain utilizes internationalized Punycode ('xn--'), frequently abused to visually imitate authentic brands with lookalike characters.",
                    severity = ThreatLevel.HIGH_RISK
                )
            )
        }

        // 7. Additional Heuristic: Excessive subdomains
        val domainParts = cleanHost.split(".")
        if (domainParts.size > 4 && !isIpv4) {
            warnings.add(
                WarningDetail(
                    title = "Excessive Subdomains",
                    description = "Contains an unusually high number of domain levels (${domainParts.size}), commonly used to forge fake brand URLs.",
                    severity = ThreatLevel.SUSPICIOUS
                )
            )
        }

        // 8. Additional Heuristic: Risky Top-Level Domain
        val tld = domainParts.lastOrNull()?.lowercase(Locale.ROOT) ?: ""
        if (RISKY_TLDS.contains(tld)) {
            warnings.add(
                WarningDetail(
                    title = "Risky Top-Level Domain (.$tld)",
                    description = "Uses the '.$tld' extension which is frequently associated with disposable phishing campaigns.",
                    severity = ThreatLevel.SUSPICIOUS
                )
            )
        }

        // 9. Additional Heuristic: Non-Standard Web Port
        if (port != -1 && port != 80 && port != 443) {
            warnings.add(
                WarningDetail(
                    title = "Non-Standard Port ($port)",
                    description = "Communicates over port $port rather than standard web ports (80/443).",
                    severity = ThreatLevel.SUSPICIOUS
                )
            )
        }

        // Categorize safety status into Safe, Suspicious, or High Risk
        val threatLevel = when {
            warnings.any { it.severity == ThreatLevel.HIGH_RISK } -> ThreatLevel.HIGH_RISK
            warnings.size >= 2 -> ThreatLevel.HIGH_RISK
            warnings.isNotEmpty() -> ThreatLevel.SUSPICIOUS
            else -> ThreatLevel.SAFE
        }

        return AnalysisResult(
            rawContent = trimmed,
            parsedUrl = normalizedUrl,
            host = if (cleanHost.isNotEmpty()) cleanHost else "Unknown Host",
            threatLevel = threatLevel,
            warnings = warnings,
            isWebUrl = true
        )
    }
}
