# MSG_PLAN.md — Message/SMS Detection Module
### Whis Android App | Java | minSdk 24, targetSdk 36 | Deadline: 31 July 2026
### Author: MSG Module Owner | Last cross-checked: 23 July 2026

---

## 1. WHAT THIS MODULE DOES

This module watches every incoming SMS on the user's phone in real time and decides —
before the user reads it — whether the message is safe, suspicious, or a scam. The
moment a message arrives, it passes through a four-layer detection engine running
entirely on the device. Only one narrow action (checking an embedded URL) ever leaves
the phone, and only when necessary. The module produces a structured verdict for every
message: a category (Safe / Suspicious / Scam), a confidence score, and a plain-language
reason string the UI module can show the user immediately. It does not block messages.
It does not modify the inbox. It warns — the user decides what to do next.

---

## 2. RESEARCH FINDINGS

### 2.1 TRAI DLT Header Suffix Regulation (Confirmed current, high confidence)

Since May 6, 2025, TRAI's TCCCPR 2025 Amendment (2nd Amendment, issued Feb 12 2025)
mandates that all A2P (Application-to-Person) SMS sent through Indian telecom
infrastructure carry a category suffix appended automatically by Telecom Service
Providers (TSPs) during DLT scrubbing.

Format: `[2-letter operator+circle]-[6-char header]-[suffix]`
- `-T` = Transactional (OTPs, bank alerts)
- `-S` = Service (customer engagement)
- `-P` = Promotional (marketing)
- `-G` = Government (TRAI-exempt headers)

Example: A service SMS from header `ABCDEF` via Vodafone Delhi → `VD-ABCDEF-S`

Sources:
- SMSAlert knowledge base: https://kb.smsalert.co.in/knowledgebase/trai-mandates-header-suffixes
- Message Central 2026 compliance guide: https://www.messagecentral.com/en-in/sms-guideline/india
- Tanla official post (May 2025): https://www.tanla.com/blog-posts/trai-mandates-new-message-suffixes
- The420.in analysis: https://the420.in/trai-sms-header-suffix-rule-2025

**What this means for our engine:** Any DLT-registered sender in correct
`XX-XXXXXX-T/S/P/G` format is auto-ham — no ML inference needed. Legitimate banks,
telcos, and OTP providers cannot bypass this. Cost: a regex check taking under 1ms.

**Critical counter-finding — SMS blasters bypass this entirely (high confidence):**
Fake base stations (IMSI catchers) inject messages outside the carrier network,
bypassing DLT scrubbing entirely. The injected message inherits the real bank's sender
thread with no DLT trace and no detectable header anomaly. India is explicitly named as
an active target region in 2025–2026 threat reports.

Sources:
- Infobip SMS fraud guide (July 2026): https://www.infobip.com/blog/a-complete-guide-to-sms-fraud
- arunapasman.com OTP/BTS spoofing guide: https://www.arunapasman.com/2025/03/distinguish-between-genuine-and-fake-otp-sms.html

**Architectural implication:** Sender/header structure is a supporting signal, NOT the
primary one. If sender looks legit, content-based detection still runs. Skipping content
analysis when the header "looks real" is a critical design flaw.

---

### 2.2 Google Play SMS Permission Policy (Confirmed current, high confidence)

Accessing `READ_SMS` or `RECEIVE_SMS` on Google Play requires the app to be the
**registered default SMS handler**. This is not a soft suggestion — apps have been
rejected and delisted for violating it. The policy was last updated Feb 26, 2026 on
Android Developers docs. The policy also explicitly calls out "SMS notification
enhancement and alerts" (our use case) as a non-default-handler exception — but this
exception requires Google Play manual review and approval, and is not guaranteed.

Sources:
- Android Developers (last updated Feb 26, 2026):
  https://developer.android.com/guide/topics/permissions/default-handlers
- Google Play Console Help:
  https://support.google.com/googleplay/android-developer/answer/10208820
- Real-world rejection example:
  https://forums.androidcentral.com/threads/my-app-was-rejected-at-play-store.1058354/

**What this means for our engine:** We must register Whis as the default SMS handler.
This means Whis must also actually *send and display* SMS as a full replacement for
Google Messages. This is not optional for Play Store distribution. This is a project-
level decision — flagged to Master in Section 7.

---

### 2.3 How Competitors Actually Detect Scams (Confirmed, multi-source)

**Google Messages:**
- Two-layer: on-device ML on content + narrow URL-only Safe Browsing check
- Never sends message body to cloud — only URLs
- On flagship devices: on-device Gemini Nano for harder conversational scams (job offers,
  romance baiting — patterns that don't show keyword warning signs)
- Added "AI Shield" layer specifically countering SMS blaster / fake base station attacks
  via device-level identity verification, not header trust
- Community "Report spam" loop feeds back into model training
Source: Google Messages product page + Wikipedia (confirmed Samsung→Google Messages
transition as of July 6, 2026)

**Truecaller:**
- Crowdsourced reputation, NOT per-message content analysis
- Three-bucket triage: Personal / Other / Spam — "Other" covers bank OTPs and alerts
- Weakness: new senders have zero signal until reported; mislabels legitimate senders
Source: Truecaller product documentation, independent reviews

**ScamDekho (India-specific):**
- Rule-based scoring across 25+ named fraud indicators (urgency phrases, OTP/PIN
  requests, mismatched sender claims, shortened links) → trust score
- Checks domain age, SSL validity, suspicious patterns, blacklists across 12+ databases
- Paste-and-check tool, not real-time — but proves rule-based scoring is shippable alone
Source: https://scamdekho.in/

**Junkboy (GitHub, MIT license — most architecturally useful open-source find):**
- Six-category triage: General, Promotion, Notification, Transaction, Junk, Allowed
- Separate Android notification channels per category at different priority levels
- ML first → rule-based enhancement second → confidence weighting determines which wins
- "Under Attack Mode" that tightens thresholds during spam waves
- Asset pattern: `sms_model.tflite` + `labels.txt` + `vocabulary.txt` + `model_info.txt`
- Architecture: SmsReceiver → SmsFilterService (foreground) → SmsClassifier → Room DB
Source: https://github.com/ovehbe/junkboy

**SilverGuard (GitHub, MIT license — most India-specific ML find):**
- TRAI DLT-aware auto-labeling: `XX-XXXXXX-T/P/S/G` senders auto-labeled ham, no
  manual review needed
- CRITICAL: public datasets alone have ~40% false-positive rate on real Indian SMS.
  Adding personal labeled messages drops it below 1%.
- Model: MobileBERT fine-tuned → ONNX export
- Input format: `SENDER_HEADER [SEP] message_text`
- Training target: 8,000 public + 5,000 synthetic + 5,000+ personal messages
Source: https://github.com/tanishqmudaliar/SilverGuard-AI-Model-Train

**Bidirectional LSTM TFLite (MIT license):**
- Architecture: Embedding (10k vocab, 16-dim) → BiLSTM (16 units) → Dense (32 ReLU)
  → sigmoid output
- Model size: 669KB TFLite. ~98% validation accuracy. Input: padded sequences of 100 tokens
Source: https://github.com/didiergarcia/sms-spam-tflite-model

**SMS Guard (ISEA National Hackathon 2026, IIT Ropar):**
- Production-ready Android app, XGBoost classifier, fully offline
- Confirms offline-first is viable and demonstrated for India-specific SMS fraud detection

---

### 2.4 India-Specific SMS Scam Patterns (Confirmed, multi-source)

**Top confirmed scam categories in India (2025–2026):**
- Fake KYC update: "Your account will be blocked. Update KYC at [link]."
- Digital arrest: "CBI/Cyber Cell has registered a case. Call immediately."
- Courier/delivery pending fee: "Package held. Pay ₹35 to release."
- FASTag renewal: "Your FASTag expires. Recharge at [link]."
- Fake income tax refund: "IT refund of ₹15,240 pending. Click to claim."
- Job offer / work from home task scam
- Fake bank debit alert (urging callback to scammer number)
- OTP social engineering: "Share the OTP to reverse the wrong transaction."

**Common structural signals across ALL Indian scam SMS:**
- Urgency + threat (account block/closure, legal action, arrest)
- Request for OTP, PIN, or credentials
- Impersonation of authority (RBI, TRAI, CBI, IT Dept, NPCI)
- Embedded link (often shortened: bit.ly, tinyurl, or lookalike domains)
- Callback number (non-toll-free, sometimes +91 mobile number)
- Hindi/Hinglish urgency phrases: "turant", "abhi", "seedha", "band ho jayega"

Sources:
- RBI Circular RBI/2024-25/105 (Jan 17, 2025): Mondaq analysis
  https://www.mondaq.com/india/white-collar-crime-anti-corruption-fraud/1573406/
- RBI KYC fraud advisory (Dec 2025):
  https://www.business-standard.com/article/current-affairs/reserve-bank-of-india-cautions-against-frauds
- TRAI SIM closure fraud (Wikipedia): https://en.wikipedia.org/wiki/Sim_closure_fraud
- Razorpay smishing guide: https://razorpay.com/learn/smishing-attacks/
- ScamDekho user testimonials: https://scamdekho.in/

**OTP false-positive problem (important):**
Real legitimate OTPs look structurally similar to phishing pretext messages. The key
distinguishing signals are:
- Genuine OTP: no URL, no urgency, no credential request beyond the code itself,
  DLT-registered sender, explicit short expiry statement ("valid for 10 minutes")
- Fake OTP pretext: requests the user to *share* the OTP, urgency, callback number,
  link to "verify"
- Engine rule: if message contains ONLY a numeric OTP code + bank name + expiry →
  strong ham signal regardless of ML score

Sources:
- arunapasman.com: https://www.arunapasman.com/2025/03/distinguish-between-genuine-and-fake-otp-sms.html
- QuickHeal OTP verification guide: https://www.quickheal.co.in/knowledge-centre/how-to-verify-fake-otp-message/

---

### 2.5 Available Datasets for Model Training

| Dataset | Source | Language | Notes |
|---|---|---|---|
| UCI SMS Spam Collection | Kaggle | English | 5,574 msgs, well-studied baseline |
| India Spam SMS Classification | Kaggle | English+Hindi | India-specific |
| Hindi Spam SMS | IEEE DataPort (Dec 2024) | Hindi | 3,894 msgs, crowd-sourced |
| SMSDHL | TechRxiv (Jan 2025) | Hindi+Dravidian | Multi-language |
| Multilingual India SMS | GitHub (shshnk158) | English+Hindi+Telugu | ~10,000, manually labeled |
| SilverGuard synthetic | Google Colab | English+Hindi | ~5,000 auto-generated scam/ham |

**SilverGuard's finding is the most important:** public datasets alone produce ~40%
false positive rate on real Indian SMS. Personal labeled messages are required to reach
production-quality accuracy. Plan accordingly.

---

### 2.6 URL Handling (Confirmed technical approach)

**Link expansion:** OkHttp with `followRedirects(false)` + fire HEAD request + follow
`Location` headers manually = full redirect chain resolution without fetching page
content. Confirmed: this is standard OkHttp behavior; we just intercept the chain.

**Safe Browsing API v5:**
- Non-commercial use ONLY (confirmed directly from Google's API docs, last updated
  March 2026): https://developers.google.com/safe-browsing/reference
- v4 deprecated, end of support March 31, 2027 (not a concern for July 31 deadline but
  use v5 from the start)
- Free, no cost
- For commercial deployment → must switch to Web Risk API (paid, Google Cloud)

Sources:
- Google Safe Browsing overview: https://developers.google.com/safe-browsing
- Brave browser v4→v5 migration issue (June 2026):
  https://github.com/brave/brave-browser/issues/56023

---

### 2.7 On-Device ML Performance on Low-End Indian Phones

Target floor device: Redmi 9A class (MediaTek Helio G25, 2–4GB RAM, Android 10–12).

- 669KB BiLSTM TFLite model: confirmed <50ms inference on ARM Cortex-A class CPUs
- MobileBERT ONNX (SilverGuard): heavier — safe on mid-range, risky on 2GB RAM Helio G25
- INT8 quantization gives 1.5–2x speedup but accuracy can vary by chipset
- XNNPACK delegate (TFLite default for ARM CPUs) gives best CPU-side performance

**Decision:** Ship BiLSTM TFLite (669KB) as primary model. It fits in 2GB RAM,
infers under 50ms, and beats MobileBERT on the floor device. Accuracy tradeoff is
acceptable because the rule-based layer below it handles 70–80% of clear-cut cases.

Sources:
- EdgeSys '24 benchmark paper: https://qed.usc.edu/paolieri/papers/2024_edgesys_mobile_inference_benchmark.pdf
- Google LiteRT benchmark: https://ai.google.dev/edge/litert/models/measurement
- Redmi device specs for target floor: Wikipedia

---

### 2.8 India DPDP Act 2023 Compliance

- Explicit consent required before processing personal data (SMS content = personal data)
- No "legitimate interests" basis (unlike GDPR) — consent is mandatory
- Penalty: up to ₹250 crore per violation category
- Data Protection Board operational and accepting complaints
- Phase 2 enforcement expected November 2026 — just after our deadline, but consent
  architecture must be built in from day one
- DPDP Rules 2025 require privacy notice in plain language in any of 22 Indian languages

**For our engine:** All SMS processing is on-device only. The only off-device action is
URL-only Safe Browsing check. This must be:
(a) disclosed in plain language during onboarding
(b) consent-gated (user can opt out of URL checks and stay fully local)
(c) documented in privacy policy with purpose and data minimization statement

Sources:
- MEITY DPDP Act full text: https://www.meity.gov.in/static/uploads/2024/06/2bf1f0e9f04e6fb4f8fef35e82c42aa5.pdf
- EY DPDP compliance guide (Dec 2025): https://www.ey.com/en_in/insights/cybersecurity/decoding-the-digital-personal-data-protection-act-2023
- Respectlytics mobile developer guide (April 2026): https://respectlytics.com/blog/india-dpdp-act-mobile-app-compliance/

---

### 2.9 What We Looked Into and Decided NOT to Use

**Crowdsourced reputation database (Truecaller model):**
Considered. Rejected for v1. Reasons: (a) requires massive user base to be meaningful —
we don't have that at launch; (b) new scam numbers get zero signal until reported;
(c) privacy exposure — uploading sender numbers to a shared cloud database without
explicit consent is a DPDP problem. Can be added in v2 as an opt-in feature.

**MobileBERT / DistilBERT ONNX on-device model:**
Considered (from SilverGuard). Rejected as primary model for floor-device performance
reasons (too heavy for 2GB RAM Helio G25). Will use as an optional upgrade path for
mid-range+ devices via a device capability check at runtime. Architecture should support
swapping models via a strategy pattern.

**Gemini API as primary classifier:**
Rejected. Rate limits (single-digit requests/minute on free tier) make it completely
unsuitable as a per-message classifier. Usable only as a last-resort fallback for
genuinely ambiguous cases. Must be rate-gate-guarded locally before any API call fires.

**NotificationListenerService for payment SMS:**
Considered. Rejected for this module. Payment notifications from PhonePe/GPay/Paytm
arrive as app notifications, not SMS — that interception point belongs to a separate
module or the UI module. Out of scope for MSG.

---

## 3. CANDIDATE APPROACHES CONSIDERED

### Decision 1: How to intercept SMS

**Option A — Register as default SMS handler (BroadcastReceiver + full SMS app)**
App registers `RECEIVE_SMS`/`READ_SMS`, becomes full replacement for Google Messages.
Pros: only Play-Store-compliant path; gives us full SMS access including sender, body,
timestamp. Cons: we have to build a functional SMS UI (compose, thread view) — significant
extra scope.

**Option B — Apply for Play Store "SMS notification enhancement" exception**
Pros: don't need full SMS UI. Cons: requires Google manual review and approval with no
timeline guarantee — risky given our July 31 deadline. Rejection kills the module.

**Option C — Sideload / non-Play distribution**
No Play restrictions, full SMS access. Rejected outright — a hackathon entry that can't
go on Play Store is dead on arrival as a real product.

**→ PICK: Option A.** Register as default SMS handler. Master needs to scope the
minimal SMS thread-view UI required for compliance. MSG module handles detection only;
UI module handles display. See Section 7.

---

### Decision 2: Primary scam detection mechanism

**Option A — Pure rule-based keyword scoring**
Fast, fully offline, deterministic, explainable. Proven (ScamDekho does this with 25+
indicators). Weakness: brittle against new scam patterns, misses paraphrase variants.

**Option B — Pure on-device ML (TFLite)**
Better generalization. Weakness: needs retraining to stay current, higher RAM/compute
cost, less explainable ("why did it flag this?").

**Option C — Layered engine: rules → ML → URL check → Gemini fallback**
Best of both. Rules handle clear-cut cases cheaply, ML handles ambiguous content, URL
check handles link risk, Gemini handles the edge cases that beat both. Each layer only
fires when the previous one is inconclusive.

**→ PICK: Option C.** This is what Google Messages does in practice, it's confirmed by
Junkboy's architecture, and it matches our performance constraints. No single layer
carries all the weight — failure in one layer degrades gracefully to the next.

---

### Decision 3: Output triage schema — binary vs. multi-category

**Option A — Binary (Scam / Not Scam)**
Simple, no ambiguity. Weakness: bank OTPs, promotional SMS, and genuine alerts all get
labelled "Not Scam" and look identical. Users learn nothing.

**Option B — Three-tier (Safe / Suspicious / Scam)**
Better UX. Weakness: "Suspicious" is ambiguous — what does the user do with that?

**Option C — Six-category (General / Promotion / Notification / Transaction / Junk /
Allowed)** — stolen directly from Junkboy, validated against Truecaller's three-bucket
pattern. Each category maps to a distinct UI treatment and notification priority.

**→ PICK: Option C.** Transaction and OTP messages never get flagged as Junk even if
content features overlap. Allowed = personal contacts, always passes. Junk = confirmed
scam. Suspicious content in a Promotion category triggers a different warning than
suspicious content in a Transaction category — the context matters.

---

## 4. PROPOSED ARCHITECTURE

### 4.1 File Structure (feature/msg branch)

```
app/src/main/java/com/scamshield/app/msg/
├── SmsReceiver.java               # BroadcastReceiver, entry point for all incoming SMS
├── SmsFilterService.java          # Foreground Service, owns detection pipeline
├── engine/
│   ├── DetectionEngine.java       # Interface (from shared contracts in PROJECT_CONTEXT.md)
│   ├── Layer1HeaderChecker.java   # TRAI header regex + contacts lookup
│   ├── Layer2RuleEngine.java      # Weighted keyword/pattern scoring
│   ├── Layer3TfliteClassifier.java# BiLSTM TFLite inference
│   ├── Layer4UrlChecker.java      # URL expansion + Safe Browsing v5
│   ├── Layer5GeminiFallback.java  # Gemini API (rate-gated, last resort)
│   └── WeightedScoreEngine.java   # Orchestrates layers, produces DetectionResult
├── model/
│   ├── SmsMessage.java            # Raw SMS data (sender, body, timestamp, simSlot)
│   ├── MsgCategory.java           # Enum: GENERAL, PROMOTION, NOTIFICATION,
│   │                              #       TRANSACTION, JUNK, ALLOWED
│   └── MsgDetectionResult.java    # Implements DetectionResult; adds category + reason string
├── rules/
│   ├── RuleSet.java               # Loads and holds all active rules
│   ├── Rule.java                  # Single rule: pattern + weight + category hint
│   └── rules_v1.json              # Bundled default ruleset (assets/)
├── storage/
│   └── MsgHistoryDao.java         # Room DAO for message verdict history + user corrections
└── util/
    ├── HeaderParser.java          # Parses/validates TRAI header format
    ├── UrlExpander.java           # OkHttp redirect-chain follower
    └── GeminiRateLimiter.java     # Token-bucket rate limiter for Gemini API calls

app/src/main/assets/
├── sms_model.tflite               # Quantized BiLSTM model
├── vocabulary.txt                 # 10k-token vocab for tokenizer
├── labels.txt                     # Class labels
└── rules_v1.json                  # Default rule weights (also in rules/)

app/src/main/res/
└── raw/
    └── otp_patterns.json          # Regex patterns for genuine OTP whitelisting
```

---

### 4.2 Data Flow — Step by Step

```
1. SMS arrives at device
        ↓
2. SmsReceiver.onReceive()
   → Reads sender, body, timestamp
   → Packages into SmsMessage
   → Starts SmsFilterService (foreground) with the SmsMessage
        ↓
3. SmsFilterService passes SmsMessage to WeightedScoreEngine.analyze()
        ↓
4. LAYER 1 — HeaderChecker (< 1ms, always runs)
   ├── Is sender in user's contacts? → ALLOWED, stop.
   ├── Does sender match TRAI format XX-XXXXXX-T/S/P/G? → auto-ham, continue to categorize
   └── Sender is 10-digit number, unknown, or malformed? → suspicious_sender flag, continue
        ↓
5. LAYER 2 — RuleEngine (< 5ms, always runs)
   → Tokenizes body (lowercased, Hindi/English mixed)
   → Scores against rules_v1.json weighted indicators:
      HIGH WEIGHT (+ve scam score):
        - urgency: ["turant", "abhi", "immediately", "last chance", "account blocked",
                    "band ho jayega", "arrested", "cyber cell", "CBI notice"]
        - credential request: ["share OTP", "enter PIN", "give password", "aadhaar"]
        - authority impersonation: ["RBI", "TRAI", "Income Tax", "NPCI", "police"]
        - shortened URL: [bit.ly, tinyurl, t.co, is.gd, and regex for 4–8 char domains]
        - callback mobile number in body (regex: 10-digit starting 6–9)
      LOW WEIGHT (ham signal):
        - genuine OTP pattern: [6-digit code only, "valid for N minutes/seconds",
                                 no URL, no callback number]
        - known safe sender suffix present (-T, -S, -G)
        - account balance statement pattern (debit/credit + reference number + balance)
   → If rule_score >= SCAM_THRESHOLD → MsgCategory.JUNK, skip Layer 3
   → If rule_score <= HAM_THRESHOLD  → category based on content type, skip Layer 3
   → If ambiguous → proceed to Layer 3
        ↓
6. LAYER 3 — TFLite BiLSTM Classifier (< 50ms, runs on ambiguous cases only)
   → Tokenizes message against vocabulary.txt
   → Pads/truncates to 100 tokens
   → Runs inference: sms_model.tflite
   → Output: scam_probability float 0–1
   → Combine with Layer 2 rule_score (weighted average)
   → If combined_score still ambiguous AND message contains URL → proceed to Layer 4
   → Otherwise → finalize category from combined_score
        ↓
7. LAYER 4 — UrlChecker (async, network-dependent, only if URL present + ambiguous)
   → Extract all URLs from message body (regex)
   → UrlExpander: OkHttp HEAD request with followRedirects(false)
     Loop: follow Location headers until final URL or max 5 hops
   → Send ONLY final expanded URL to Google Safe Browsing v5 API
     GET https://safebrowsing.googleapis.com/v5/hashLists:batchGet?...
   → If URL is on unsafe list → scam_score += heavy weight
   → If URL is clean → small ham signal
   → Re-evaluate combined_score
   → If still ambiguous → proceed to Layer 5 (only if Gemini rate limiter allows)
        ↓
8. LAYER 5 — Gemini Fallback (cloud, rate-gated, last resort)
   → GeminiRateLimiter checks token bucket: max 10 requests/minute, 200/day
   → If quota available: call gemini-2.5-flash with structured prompt:
     "Is this SMS a scam? Reply JSON only: {verdict, confidence, reason}
      SMS: [sender] | [body]"
   → Parse response for verdict
   → If quota exhausted → default to SUSPICIOUS, flag for user review
        ↓
9. WeightedScoreEngine builds MsgDetectionResult:
   - category: MsgCategory enum value
   - confidenceScore: float 0–1
   - threatLevel: SAFE / SUSPICIOUS / SCAM
   - reasonText: human-readable string in English (UI module handles language)
   - layersUsed: bitmask of which layers contributed
   - containsUrl: boolean
   - expandedUrl: String (if Layer 4 ran)
        ↓
10. SmsFilterService sends MsgDetectionResult via:
    - LocalBroadcastManager → UI module picks up and shows warning
    - MsgHistoryDao.insert() → persists to Room DB with user_correction field = null
        ↓
11. User sees warning (UI module's responsibility)
    If user marks "Not a scam" or "Was a scam":
    → MsgHistoryDao.updateCorrection(id, correction)
    → Rule weights in RuleSet can be locally adjusted (v1: local only)
```

---

### 4.3 Key Implementation Notes

**Foreground Service:** `SmsFilterService` must run as a foreground service with a
persistent notification (Android 8+ requirement). Notification should be low-priority
and minimal ("Whis is protecting your messages"). Without foreground service, Android
kills the process on low-memory devices.

**OTP Whitelisting Rule (false positive prevention — critical):**
Before Layer 2 scoring, check `otp_patterns.json`. If message matches:
`/\b\d{4,8}\b.*(valid|expires?|OTP|one.?time).*(minute|second|min|sec)/i`
AND contains no URL AND contains no callback number → force category to TRANSACTION,
skip all further layers, confidence = HIGH. This single rule eliminates the most common
false-positive category.

**DPDP Consent Hook:**
`SmsFilterService` checks a SharedPreferences flag `pref_url_check_consent` before
Layer 4 runs. If false (user declined during onboarding), Layer 4 is skipped entirely —
processing stays fully on-device. Onboarding module (UI) is responsible for setting this
flag after showing a plain-language consent screen.

**Gemini Rate Limiter:**
`GeminiRateLimiter` uses a token bucket: 10 tokens/minute refilled at 1 token/6 seconds,
daily cap of 200 stored in SharedPreferences with a midnight reset. If bucket is empty,
Layer 5 is skipped and result defaults to SUSPICIOUS with `geminiSkipped = true`.

**Rule Engine Hot-Reload:**
`RuleSet` loads from `rules_v1.json` at app start. A future update can push a new JSON
file without a full app update. Version field in JSON; if bundled version < downloaded
version, swap at next cold start.

**Room Schema:**
```
TABLE msg_history (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  timestamp       INTEGER NOT NULL,
  sender          TEXT NOT NULL,
  body_hash       TEXT NOT NULL,    -- SHA-256 of body, NOT raw body (privacy)
  category        TEXT NOT NULL,
  threat_level    TEXT NOT NULL,
  confidence      REAL NOT NULL,
  reason_text     TEXT,
  layers_used     INTEGER,
  user_correction TEXT              -- null, 'SAFE', or 'SCAM'
)
```
Note: We store `body_hash`, not the raw body. This is a deliberate DPDP decision — we
can match corrections to messages without retaining message content in our DB.

---

### 4.4 Model Training Pipeline (pre-deadline, offline, not in app)

1. Download: UCI SMS Spam Collection + India Spam SMS Kaggle + Hindi IEEE DataPort
2. Merge, deduplicate, normalize labels to binary (spam/ham)
3. Apply SilverGuard's auto-labeling: `XX-XXXXXX-T/S/P/G` senders → ham
4. Augment with synthetic scam messages (using Gemini API offline, not in app) covering
   Hindi/Hinglish urgency patterns
5. Target: 13,000+ samples minimum before training
6. Train BiLSTM (Keras): Embedding(10000, 16) → BiLSTM(16) → Dense(32, relu) → Dense(1, sigmoid)
7. Export: `model.h5` → TFLite converter → INT8 quantization → `sms_model.tflite`
8. Validate: separate held-out India-specific test set. Accept only if false positive
   rate on legitimate OTPs and bank alerts < 2%
9. Copy final `sms_model.tflite` + `vocabulary.txt` to `app/src/main/assets/`

---

## 5. DEPENDENCIES ON OTHER MODULES

### What this module NEEDS from others

**From Master (shared contracts):**
- `DetectionResult` interface — must include at minimum: `getThreatLevel()`,
  `getConfidenceScore()`, `getReasonText()`, `getCategory()`. Our `MsgDetectionResult`
  implements this. If the interface doesn't exist yet, we define our own and flag it
  as a contract proposal.
- `DataStore` interface — if a shared DB is planned, MSG history should plug into it.
  Until then, we use our own `MsgHistoryDao` with Room.

**From UI module:**
- A `LocalBroadcastManager` action string (e.g., `com.scamshield.MSG_VERDICT`) to
  receive `MsgDetectionResult` and show the appropriate warning card.
- The onboarding screen that sets `pref_url_check_consent` in SharedPreferences — URL
  checking is disabled until the UI module sets this flag.
- If UI module does not exist yet: MSG module will show a basic `Toast`/`Notification`
  as a placeholder. Detection itself is not blocked.

**From Call module:**
- Nothing directly. Detection logic is independent. If a shared scam-number database
  exists in Call module, MSG could query it for sender number reputation — but this is
  a v2 enhancement, not required for v1.

**From Learning / AI Chat modules:**
- Nothing required for detection. If Learning module exposes a "recent scam types" feed,
  Rule Engine could update weights — v2 only.

### What other modules can use from MSG

- `MsgDetectionResult` is the output contract. UI module, AI Chat module, and any
  reporting flow can consume it directly.
- `MsgHistoryDao` gives read access to all past verdicts for history screens.

---

## 6. WHAT WE ARE EXPLICITLY NOT SOLVING

**Full SMS app UI (compose, thread view, contact sync):**
We must register as the default SMS handler for Play Store compliance, which means we
technically need to handle SMS sending and display. The minimal required UI is a
thread-list and message compose screen. This is out of scope for the MSG detection
module — it belongs to UI module. If UI module doesn't build it, we need a decision
from Master on how to handle Play Store compliance (see Section 7).

**Crowdsourced sender reputation:**
Deferred to v2. Requires user base, shared cloud DB, consent flow, and privacy review.
None of that is achievable by July 31.

**MMS / RCS detection:**
MMS bodies (images, PDFs) require completely different extraction and analysis pipelines.
RCS is delivered through different infrastructure. Both deferred. We detect SMS text only.

**International SMS scam patterns:**
Our rule set and training data is India-first. International scam patterns (IRS, Medicare
US-style) are not in scope. Indian IP, Indian user base, Indian regulatory context.

**Active blocking of scam SMS:**
We warn. We do not intercept or delete messages. The decision to block is the user's.
(Also: reliable blocking would require network-level access we don't have.)

**Chakshu / 1909 auto-reporting:**
TRAI's Chakshu portal for reporting smishing exists. Auto-reporting on behalf of the user
requires explicit per-report consent. Out of scope for v1 — UI module can add a "Report
to TRAI" button that deep-links to Chakshu.

**Granular ML model updates (OTA):**
Model retraining and OTA model delivery via Firebase ML or similar. Out of scope for
July 31. Model is bundled in APK. Post-launch enhancement.

---

## 7. OPEN QUESTIONS FOR MASTER

**Q1 — Default SMS handler (BLOCKER):**
Google Play requires Whis to be the registered default SMS handler to use READ_SMS/
RECEIVE_SMS. This means Whis must function as a full SMS app (send, receive, display
threads). Is the UI module scoped to build a minimal functional SMS UI? If not, we have
three options: (a) apply for the "SMS notification enhancement" Play exception and risk
rejection; (b) ship outside Play Store (kills the product); (c) scope in minimal SMS UI.
This decision must be made before any code is written — it affects the manifest,
permissions, and UI module scope simultaneously.

**Q2 — Shared DetectionResult contract:**
MSG module needs the `DetectionResult` interface finalized before `MsgDetectionResult`
can implement it. Can Master publish this interface to the shared contracts file
(PROJECT_CONTEXT.md) before coding begins? Our proposed fields: `getThreatLevel()`,
`getConfidenceScore()`, `getReasonText()`, `getCategory()`, `getTimestamp()`.

**Q3 — Safe Browsing → Web Risk for commercial deployment:**
Google Safe Browsing v5 is free but restricted to non-commercial use. If Whis is ever
distributed commercially (paid app, ads, monetized), we must migrate to Web Risk API
(Google Cloud, paid). For the hackathon this is fine. For a real product, Master should
note this in the project roadmap. No action needed before July 31.

**Q4 — DPDP consent screen ownership:**
The consent screen for URL checking (Safe Browsing) must appear during onboarding and
must be built before MSG module's Layer 4 can activate. Is this owned by UI module?
MSG module depends on the `pref_url_check_consent` SharedPreferences flag being set.
If onboarding isn't built in time, Layer 4 defaults to disabled — detection still works,
just without URL reputation checking.

**Q5 — Model training ownership:**
The BiLSTM TFLite model must be trained and bundled before the app can be tested end-to-
end. Who owns the training pipeline? MSG module owner can run it, but it requires access
to the dataset links and approximately 2–3 hours of Colab compute time. Should be done
in the first 2 days of coding phase.

---

*End of MSG_PLAN.md. Research complete. All regulatory and API facts cross-checked
against sources dated 2025–2026. Ready for Master review.*
