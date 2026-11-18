# QR Safety Scanner (Android)
**Problem Statement 21 (PS 21)**  
*Production-ready mobile application that scans QR codes using the device camera, inspects the embedded URL for malicious patterns or security risks, displays safety warnings, and manages scan history.*

---

## 🛠 Tech Stack & Architecture

- **Platform:** Native Android
- **Language:** Kotlin 2.0
- **UI Framework:** Jetpack Compose with Material Design 3 (Dark Security Mode)
- **Scanning Library:** Google ML Kit Barcode Scanning API (`com.google.mlkit:barcode-scanning`)
- **Camera Subsystem:** AndroidX CameraX (Preview, ImageAnalysis, Camera2, Lifecycle)
- **Local Persistence:** Android SQLite Database (`ScanHistoryDbHelper` + Coroutine Repository)
- **Concurrency:** Kotlin Coroutines & `StateFlow` (`Dispatchers.IO`)

---

## 🌟 Key Features & Requirements (Must-Have)

### 1. Camera QR Code Scanner View
- **Real-Time Camera Preview:** Integrated with AndroidX CameraX and Google ML Kit Barcode Scanning for instant frame-by-frame QR detection.
- **Scanning Overlay:** Viewfinder cutout with corner brackets, status instruction pill, and smooth animated laser line.
- **Flashlight / Torch Toggle:** Instant torch control for scanning in low-light environments.
- **Camera Switch Controls:** Toggle between rear and front-facing cameras with one tap.
- **Gallery Fallback:** Scan QR codes directly from device image files or gallery photos.
- **Lifecycle & Permissions:** Automatic runtime camera permission handling.

### 2. URL Extraction & Display Screen
- **Immediate Interception:** Intercepts QR content before any browser or intent navigation occurs.
- **Detailed Preview:** Presents a dedicated Material 3 bottom modal/dialog showing:
  - The exact raw URL string
  - Parsed destination host domain
  - 1-tap clipboard copy button
  - Detailed security flag breakdown

### 3. Smart Heuristic Security Analyzer Engine (`UrlSafetyAnalyzer`)
Inspects each URL against standard security heuristics:
- **Missing HTTPS (Plain HTTP):** Flags insecure `http://` or non-standard protocol usage.
- **IP Address Host:** Flags numeric IPv4 (`192.168.1.1`) or IPv6 addresses used in place of registered domain names.
- **Link Shorteners (Obfuscation):** Flags known shortener services (`bit.ly`, `tinyurl.com`, `t.co`, `goo.gl`, `cutt.ly`, etc.) that conceal the final target.
- **Suspicious Phishing Keywords:** Flags deceptive keywords including `"login-verify"`, `"secure-update"`, `"account-confirm"`, `"banking"`, `"free-gift"`, `"wallet"`, `"credential"`, etc.
- **Additional Security Heuristics:**
  - Deceptive `@` character in URL (userinfo host spoofing)
  - Internationalized Punycode homograph domains (`xn--`)
  - Excessive subdomains (e.g. `paypal.com.verify.attacker.xyz`)
  - Risky top-level domains (`.xyz`, `.top`, `.click`, `.loan`, `.work`, etc.)
  - Non-standard port communication (e.g. `:8080`, `:8888`)
- **Safety Categorization:**
  - 🟢 **Safe** (No threats detected)
  - 🟡 **Suspicious** (Obfuscated or moderate warning flags)
  - 🔴 **High Risk** (Critical threats: plain HTTP, raw IP hosts, or phishing keywords)

### 4. Action Interception & Verdict Dialog
- Clearly displays the security verdict with color-coded badges and risk explanations.
- **"Don't open"**: Highlighted as the primary safe action; safely dismisses the warning and resumes scanning.
- **"Open anyway"**: Secondary action with security boundary; passes the URL to the default system browser via Android `Intent.ACTION_VIEW`.

---

## 🎁 Bonus Requirements

### 5. Scan History & Audit Log
- **Persistent Local Database:** Stores all scanned URLs, timestamps, host domains, threat verdicts, and warning summaries locally using SQLite.
- **Dedicated History Screen:**
  - View chronologically ordered scan records
  - Tap any past item to re-inspect its safety assessment
  - Delete individual scan records or clear all logs with confirmation

### 6. Built-in Security Heuristics Sandbox
- Accessible via the **🧪 Science Flask** icon in the top bar.
- Test all core heuristics in 1-tap without requiring physical QR codes:
  1. *Missing HTTPS* (`http://insecure-example.com/login`)
  2. *IP Address Host* (`http://192.168.1.1/dashboard`)
  3. *Link Shortener* (`https://bit.ly/secure-account-redirect`)
  4. *Suspicious Keywords (login-verify)* (`https://login-verify-account.com/portal`)
  5. *Keywords (banking & secure-update)* (`https://update-secure-banking-portal.xyz/claim`)
  6. *Verified Safe Link* (`https://www.google.com`)
  - Input field to test any custom URL or text string in real-time.

---

## 📂 Project Architecture & Directory Layout

```
scanner/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/qrsafety/scanner/
│       │   │   ├── MainActivity.kt                # App entrypoint & destination routing
│       │   │   ├── analyzer/
│       │   │   │   ├── SafetyVerdict.kt           # ThreatLevel (Safe, Suspicious, High Risk)
│       │   │   │   └── UrlSafetyAnalyzer.kt       # Smart heuristic security engine
│       │   │   ├── camera/
│       │   │   │   ├── QrCodeAnalyzer.kt          # Google ML Kit barcode scanner
│       │   │   │   └── CameraPreview.kt           # CameraX preview with torch & flip controls
│       │   │   ├── data/
│       │   │   │   ├── ScanRecord.kt              # Audit log data entity
│       │   │   │   ├── ScanHistoryDbHelper.kt     # SQLite database helper
│       │   │   │   └── ScanHistoryRepository.kt   # Coroutine repository
│       │   │   └── ui/
│       │   │       ├── components/
│       │   │       │   ├── ScannerOverlay.kt      # Frame brackets & laser beam animation
│       │   │       │   ├── VerdictBadge.kt        # Green / Orange / Red badges
│       │   │       │   └── WarningItemCard.kt     # Heuristic flag cards
│       │   │       ├── screens/
│       │   │       │   ├── ScannerScreen.kt       # Camera scanner & controls
│       │   │       │   ├── SafetyDialog.kt        # Verdict modal with "Don't open" / "Open anyway"
│       │   │       │   ├── HistoryScreen.kt       # Scan history & audit log
│       │   │       │   └── TestSamplesScreen.kt   # Heuristics evaluation sandbox
│       │   │       └── theme/
│       │   │           ├── Color.kt
│       │   │           ├── Theme.kt
│       │   │           └── Type.kt
│       │   └── res/
│       └── test/
│           └── java/com/qrsafety/scanner/
│               └── UrlSafetyAnalyzerTest.kt       # Automated unit tests
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew & gradlew.bat
└── README.md
```

---

## 🚀 Running the Application in Android Studio

1. Open **Android Studio** (Hedgehog, Iguana, Koala, Ladybug, or newer).
2. Choose **File > Open...** and select this directory:
   ```
   c:\Users\samar\OneDrive\Desktop\scanner
   ```
3. Allow Gradle sync to complete.
4. Attach an Android phone via USB (with USB Debugging turned ON) or launch an Android Virtual Device (AVD).
5. Press **Run 'app'** (`Shift + F10`).
6. Point at any QR code, or tap the **🧪 Flask** icon in the top bar to test each heuristic scenario with 1 tap!
