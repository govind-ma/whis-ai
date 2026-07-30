# 🛡️ Whis AI — Cyber Fraud & Scam Protection for India

[![Android Min SDK](https://img.shields.io/badge/Min%20SDK-24%20%28Android%207.0%2B%29-brightgreen.svg)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36%20%28Android%2016%29-blue.svg)](https://developer.android.com)
[![AI Engine](https://img.shields.io/badge/AI%20Engine-Groq%20Llama%203.3%2070B%20%7C%20Gemini-orange.svg)](https://groq.com)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**Whis AI** is a privacy-first, hybrid AI cyber defense assistant designed to protect first-time digital banking users in India against sophisticated cyber fraud:
- 🚨 **Digital Arrest & Extortion Scam Calls** (Fake CBI, Police, Customs officers).
- 💬 **SMS & Notification Phishing** (Fake electricity bill cuts, bank account blocks).
- 💸 **UPI Fraud & QR Code Traps** (Hardcoded PIN rule enforcement).
- 📦 **Malicious Loan App APK Downloads** (Detecting deceptive domains and `.apk` links).

---

## 🎥 Official Demo Video
👉 **[Watch Whis AI Live Demo Video on Vimeo](https://vimeo.com/1214418382)**

---

## 🌟 Key Features

### 1. 📞 Real-Time Call Defense & Full-Screen Red Alert
- **Known Contact Safeguard**: Calls from numbers saved in your phone address book are automatically assigned **`Trusted` (SAFE)** status with 0ms latency.
- **Full-Screen Emergency Red Alert**: Intercepts Digital Arrest / extortion calls instantly over lockscreen & active apps using `USE_FULL_SCREEN_INTENT`, `showWhenLocked="true"`, and `turnScreenOn="true"`.
- **1930 Cyber Crime Helpline & Dual SOS Alert**: Provides a 1-tap dialer for India's 1930 Helpline and dispatches automated SMS SOS alerts to **2 Emergency Contacts**.

### 2. 💬 5-Layer Hybrid SMS & Notification Screening
- **Layer 1**: TRAI DLT header whitelist verification (`-HDFCBK`, `-SBIBNK`).
- **Layer 2**: Curated Indian scam taxonomy rule engine (KYC, PAN, Police, CBI, UPI).
- **Layer 3**: On-device TFLite token classifier.
- **Layer 4**: `UrlExpander` tracking redirect chains to detect `.apk` downloads and fake domains (`.xyz`, `.top`).
- **Layer 5**: Multi-Provider Cloud LLM reasoning.
- **System Utility Filter**: `WhisNotificationListenerService` screens WhatsApp, Banking, and SMS app notifications while excluding system utilities (`smartcapture`, `systemui`, `dialer`).

### 3. 🤖 Dual-Engine Multi-Provider AI Architecture
- **Primary Engine**: **Groq Cloud API (`llama-3.3-70b-versatile`)** for sub-50ms inference speeds.
- **Automatic 429 Failover**: Silently switches to `llama-3.1-8b-instant` (30,000 TPM limit) or **Google Gemini 1.5 / 2.0 Flash** if rate limits occur.
- **Hinglish AI Persona**: Colloquial communication mode (*"Yeh nakli bank link hai. Ispe click mat karo."*) + hardcoded UPI PIN rule (*"UPI PIN is only for SENDING money, never for RECEIVING"*).

### 4. 🔒 DPDP Privacy & Emergency Settings
- **DPDP Act 2023 Compliance**: SMS body text converted to SHA-256 hashes before local Room DB storage. Zero plaintext body persistence.
- **2 Emergency Contacts (SOS)**: Full UI editor in Settings + persistent `EmergencyContactStore`.
- **Direct System Intents**: Dedicated Settings buttons for **Notification Access** and **Do Not Disturb (DND) Override**.

---

## 🏗️ System Architecture

```
📱 Incoming Call / SMS / Notification
         │
         ├── Known Contact Check (0ms, On-Device) ──► 🟢 SAFE (Trusted)
         │
         └── 5-Layer Hybrid Detection Engine
                  │
                  ├── Layer 1: TRAI DLT Header Verification
                  ├── Layer 2: Heuristic Scam Rule Engine
                  ├── Layer 3: On-Device TFLite Classifier
                  ├── Layer 4: URL & APK Expander (.apk & deceptive TLDs)
                  └── Layer 5: Multi-Provider Cloud AI (Groq 70B / Gemini)
                           │
                           ├── 🚨 Digital Arrest Call ──► Full-Screen RedAlert + 1930 Helpline + 2 SOS Contacts
                           ├── 🔴 Phishing SMS ────────► Heads-Up Notification + Red Badge (Room DB)
                           └── 🟢 Safe Transaction ────► Trusted Badge (0/100 Risk Score)
```

---

## 🛠️ Tech Stack & Requirements

- **Language**: Java / AndroidX
- **Minimum SDK**: `24` (Android 7.0 Nougat+)
- **Target SDK**: `36` (Android 16)
- **Database**: Room SQLite (`LocalMsgDatabase`)
- **AI Models**: Groq Llama 3.3 70B, Groq Llama 3.1 8B, Google Gemini 1.5/2.0 Flash
- **Networking**: OkHttp 4 with custom `User-Agent: WhisApp/1.0` headers & 30s read timeouts

---

## 🚀 Quick Start & Build Instructions

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-username/whis-ai.git
   cd whis-ai
   ```

2. **Configure API Keys**:
   Create or update `local.properties` in the root directory:
   ```properties
   GEMINI_API_KEY=gsk_your_groq_or_gemini_api_key_here
   ```

3. **Build the Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run on Device / Emulator**:
   Deploy directly via Android Studio (Run ▶) to any Android device running Android 10 to 16.

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.
