// Smart Heuristic Security Analyzer Engine (JS port matching Android UrlSafetyAnalyzer)
const UrlSafetyAnalyzer = (() => {
  const KNOWN_SHORTENERS = new Set([
    'bit.ly', 'tinyurl.com', 't.co', 'goo.gl', 'is.gd', 'buff.ly', 'ow.ly',
    'cutt.ly', 'rb.gy', 'qr.ae', 'shorturl.at', 'rebrand.ly',
    'tr.ee', 'v.gd', 'cli.re', 's.id', 'bl.ink', 'tiny.cc', 'adf.ly',
    'bc.vc', 'linktr.ee', 'lnkd.in', 'snip.ly', 'shorte.st', 'trib.al'
  ]);

  const SUSPICIOUS_WORDS = [
    'login-verify',
    'secure-update',
    'account-confirm',
    'banking',
    'free-gift',
    'login',
    'signin',
    'sign-in',
    'verify',
    'verification',
    'verify-account',
    'account-update',
    'update-account',
    'secure-login',
    'wallet',
    'credential',
    'giftcard',
    'reward-claim',
    'claim-prize',
    'password-reset',
    'support-service',
    'kyc-verify',
    'auth-checkpoint'
  ];

  const RISKY_TLDS = new Set([
    'xyz', 'top', 'work', 'click', 'loan', 'zip', 'mov',
    'gq', 'cf', 'ml', 'ga', 'tk', 'country'
  ]);

  const IPV4_REGEX = /^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
  const IPV6_REGEX = /^[0-9a-fA-F:]+$/;

  function analyze(input) {
    const trimmed = (input || '').trim();
    const warnings = [];

    const lowerTrimmed = trimmed.toLowerCase();
    const looksLikeUrl =
      lowerTrimmed.startsWith('http://') ||
      lowerTrimmed.startsWith('https://') ||
      lowerTrimmed.startsWith('www.') ||
      /^[a-z][a-z0-9+.-]*:\/\//i.test(trimmed) ||
      (trimmed.includes('.') && !trimmed.includes(' ') && trimmed.length > 3);

    if (!looksLikeUrl) {
      return {
        rawContent: trimmed,
        parsedUrl: trimmed,
        host: 'Plain Text',
        threatLevel: 'SAFE',
        threatLabel: 'Safe',
        warnings: [
          {
            title: 'Plain Text Content',
            description: 'The scanned QR code contains plain text rather than a web link.',
            severity: 'SAFE'
          }
        ],
        isWebUrl: false
      };
    }

    let normalizedUrl = trimmed;
    if (!/^[a-z][a-z0-9+.-]*:\/\//i.test(trimmed)) {
      normalizedUrl = 'http://' + trimmed;
    }

    let scheme = '';
    let host = '';
    let port = '';

    try {
      const parsed = new URL(normalizedUrl);
      scheme = parsed.protocol.replace(':', '').toLowerCase();
      host = parsed.hostname.toLowerCase();
      port = parsed.port;
    } catch (_) {
      const schemeIdx = normalizedUrl.indexOf('://');
      let rest = normalizedUrl;
      if (schemeIdx !== -1) {
        scheme = normalizedUrl.slice(0, schemeIdx).toLowerCase();
        rest = normalizedUrl.slice(schemeIdx + 3);
      }
      const authority = rest.split(/[/?#]/)[0];
      const hostPort = authority.includes('@') ? authority.split('@').pop() : authority;
      if (hostPort.includes(':') && !hostPort.startsWith('[')) {
        const parts = hostPort.split(':');
        host = parts[0].toLowerCase();
        port = parts[1] || '';
      } else {
        host = hostPort.toLowerCase();
      }
    }

    const cleanHost = host.replace(/^\[|\]$/g, '');
    const fullLower = normalizedUrl.toLowerCase();

    // 1. Missing HTTPS
    if (scheme === 'http') {
      warnings.push({
        title: 'Missing HTTPS (Plain HTTP)',
        description: 'This URL uses unencrypted HTTP instead of secure HTTPS. Unencrypted traffic can be intercepted or manipulated.',
        severity: 'HIGH_RISK'
      });
    } else if (scheme !== 'https') {
      warnings.push({
        title: `Unusual Protocol (${scheme || 'unknown'})`,
        description: `Uses a non-standard scheme (${scheme}://) which may trigger unintended or unsafe actions.`,
        severity: 'HIGH_RISK'
      });
    }

    // 2. IP Address Host
    const isIpv4 = IPV4_REGEX.test(cleanHost);
    const isIpv6 = cleanHost.includes(':') && IPV6_REGEX.test(cleanHost);
    if (isIpv4 || isIpv6) {
      warnings.push({
        title: 'Raw IP Address Host',
        description: `The link points directly to a numeric IP address (${cleanHost}) instead of a registered domain name.`,
        severity: 'HIGH_RISK'
      });
    }

    // 3. Link Shorteners
    const isShortener = Array.from(KNOWN_SHORTENERS).some(
      (s) => cleanHost === s || cleanHost.endsWith('.' + s)
    );
    if (isShortener) {
      warnings.push({
        title: 'Link Shortener Detected',
        description: `Uses a URL shortener (${cleanHost}) that hides the true final destination website.`,
        severity: 'SUSPICIOUS'
      });
    }

    // 4. Suspicious Phishing / Odd Keywords
    const matchedWords = SUSPICIOUS_WORDS.filter((word) => fullLower.includes(word));
    if (matchedWords.length > 0) {
      const displayWords = matchedWords.slice(0, 4).map((w) => `"${w}"`).join(', ');
      warnings.push({
        title: 'Suspicious Phishing Keywords',
        description: `Contains deceptive social-engineering keywords: ${displayWords}. Attackers commonly use these to spoof login or verification portals.`,
        severity: 'HIGH_RISK'
      });
    }

    // 5. Deceptive '@' character
    if (trimmed.includes('@')) {
      warnings.push({
        title: "Deceptive '@' Character Spoof",
        description: "The URL contains an '@' symbol, often used to disguise the actual destination server.",
        severity: 'HIGH_RISK'
      });
    }

    // 6. Punycode / Homograph domain
    if (cleanHost.includes('xn--')) {
      warnings.push({
        title: 'Punycode Homograph Domain',
        description: "Uses internationalized Punycode ('xn--'), commonly abused to mimic genuine brands with lookalike characters.",
        severity: 'HIGH_RISK'
      });
    }

    // 7. Excessive subdomains
    const domainParts = cleanHost.split('.').filter(Boolean);
    if (domainParts.length > 4 && !isIpv4) {
      warnings.push({
        title: 'Excessive Subdomains',
        description: `Contains an unusually deep subdomain chain (${domainParts.length} levels), often used to forge trusted domains.`,
        severity: 'SUSPICIOUS'
      });
    }

    // 8. Risky TLD
    const tld = domainParts.length > 1 ? domainParts[domainParts.length - 1] : '';
    if (RISKY_TLDS.has(tld)) {
      warnings.push({
        title: `Risky Top-Level Domain (.${tld})`,
        description: `Uses the '.${tld}' extension, which has an elevated association with phishing campaigns.`,
        severity: 'SUSPICIOUS'
      });
    }

    // 9. Non-standard port
    if (port && port !== '80' && port !== '443') {
      warnings.push({
        title: `Non-Standard Port (${port})`,
        description: `Communicates over port ${port} instead of standard web ports (80/443).`,
        severity: 'SUSPICIOUS'
      });
    }

    // Categorize verdict into Safe, Suspicious, or High Risk
    let threatLevel = 'SAFE';
    if (warnings.some((w) => w.severity === 'HIGH_RISK') || warnings.length >= 2) {
      threatLevel = 'HIGH_RISK';
    } else if (warnings.length > 0) {
      threatLevel = 'SUSPICIOUS';
    }

    const labelMap = {
      SAFE: 'Safe',
      SUSPICIOUS: 'Suspicious',
      HIGH_RISK: 'High Risk'
    };

    return {
      rawContent: trimmed,
      parsedUrl: normalizedUrl,
      host: cleanHost || 'Unknown Host',
      threatLevel,
      threatLabel: labelMap[threatLevel],
      warnings,
      isWebUrl: true
    };
  }

  return { analyze };
})();
