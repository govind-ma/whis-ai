# CALL_PROTECTION_PLAN.md
**Module:** Call Protection
**Owner:** Govind
**Stack:** Java · minSdk 24 · targetSdk 36
**Date:** July 2026
**Status:** Ready for Master review

---

## SECTION 1 — WHAT YOU FOUND

### 1.1 The Problem Is Real and Getting Worse

MHA official data (February 2026, cited by The Print and Insights on India):
- Indians lost **₹22,495 crore** to cyber fraud in 2025 — 28.15 lakh cases, 24% spike over 2024
- **Digital arrest scams** alone: ₹19,000+ crore in 2025 (Business Standard / Open The Magazine, January 2026)
- 1930 helpline received **3.24 crore calls** in 2025 — one per second
- Victims recovered **6% of stolen funds** even when they reported quickly
- 86% of Indian households now connected — rural first-time digital users are the fastest-growing victim segment

Source: theprint.in/india/cybercrime-saw-24-spike-in-2025, insightsonindia.com/2026/02/21/cybercrime-in-india, openthemagazine.com digital arrest article (Jan 2026), kaval.chat scam statistics 2026.

This is the context the app lives in. Not a demo, not a niche problem.

---

### 1.2 The Core Android API — `CallScreeningService`

**Source:** developer.android.com/develop/connectivity/telecom/screen-calls — last updated February 26, 2026.

This is the only correct API for a non-dialer app to intercept incoming calls. Key facts:

- Available from **API 24**, but meaningful pre-ring behavior only on **API 29+**
- System binds the service automatically when a call arrives — no persistent background process needed
- Must call `respondToCall()` within **5 seconds** or the call rings regardless
- On API 29+: the phone **does not ring** until `respondToCall()` is called — this is the intervention window
- On API 29+: the incoming number is provided directly by the service — **no `READ_CALL_LOG` permission needed** for caller identification (this eliminates the Play Store restriction for the core feature)
- App must hold `ROLE_CALL_SCREENING` via `RoleManager` — only one app on the device can hold this at a time
- `getCallerNumberVerificationStatus()` field in the API returns PASSED/FAILED/NOT_VERIFIED based on **STIR/SHAKEN** — a US/FCC framework that Indian carriers **do not implement**. This field returns NOT_VERIFIED for virtually every Indian call. **Do not wire this into scoring.**

Source for STIR/SHAKEN India gap: FCC STIR/SHAKEN docs (Dec 2025); TRAI CNAP approval documents (Oct 2025) confirm India uses a separate system.

---

### 1.3 `PhoneStateListener` Is Deprecated — And Why We Still Need State Tracking

`TelephonyManager.listen()` with `PhoneStateListener` was deprecated at **API 31**. Replacement: `TelephonyCallback` registered via `TelephonyManager.registerTelephonyCallback()`.
Source: Microsoft Learn Android API reference (learn.microsoft.com/dotnet/api/android.telephony.phonestatelistener).

`CallScreeningService` alone is insufficient — it fires on call arrival but does not tell us when the user answers (OFFHOOK) or when the call ends (IDLE). We need those transitions to: start the live overlay, stop it, and trigger post-call feedback. Solution: register `TelephonyCallback` (API 31+) or `PhoneStateListener` (API 24–30) dynamically when a call is detected, unregister on IDLE. Not persistent — registered and destroyed per call.

**What `TelephonyCallback` can and cannot detect:**
- CAN detect: RINGING → OFFHOOK → IDLE (three states only)
- CANNOT detect: call hold, call merge, conference, which SIM the call came in on
- Hold/merge detection requires `InCallService` APIs that need `ROLE_DIALER` — we do not and should not hold that role

This kills the "Call Merge OTP Warning" I planned earlier as a real-time detection feature. It is replaced by a static proactive tip shown during all SUSPICIOUS/HIGH_RISK calls: *"If anyone on this call asks you to add a third person or merge a call — hang up immediately. This is a known OTP theft technique."*

---

### 1.4 `READ_CALL_LOG` — The Play Store Trap and How We Avoid It Completely

Google Play policy (support.google.com/googleplay/android-developer/answer/10208820): Apps must be the default Phone or SMS handler to use `READ_CALL_LOG`, unless they obtain a specific exception via a Declaration Form. Call screening apps with `ROLE_CALL_SCREENING` are in a grey area — but filing a declaration form for a hackathon-origin app going to Play Store is a real risk that could get the app removed.

**We do not request `READ_CALL_LOG` at all.**

Instead, `WhisCallScreeningService` logs every call it intercepts into its own internal SQLite database (`LocalCallHistoryDatabase`). Since every call passes through our screener, we have complete call history from the moment the app is installed. The behavioral analysis layer (`CallBehaviorAnalyzer`) queries this internal database — same signals, zero sensitive permissions, zero Play Store risk.

Source: Android developer docs (Feb 2026) confirming API 29+ `CallScreeningService` removes the READ_CALL_LOG requirement for caller ID; Play Store policy docs for the restriction.

---

### 1.5 Foreground Service Type — `specialUse`, Not `shortService`

Source: developer.android.com/develop/background-work/services/fgs/service-types, developer.android.com/about/versions/14/changes/fgs-types-required (confirmed current July 2026).

`shortService` was my initial choice. Research found two problems:

1. `shortService` has a **3-minute cap**. It CAN be extended by calling `startForeground()` again while the overlay is visible to the user — but a digital arrest scam call runs 30–120 minutes. Repeatedly extending every 3 minutes is fragile engineering for a safety feature.
2. `shortService` is defined for "short, critical tasks that can't be interrupted." A live guidance overlay during an active phone call is not a short task — it is a sustained, user-engaged safety feature.

**Correct type: `specialUse`**
- No time cap
- Requires `FOREGROUND_SERVICE_SPECIAL_USE` permission (normal permission, granted at install)
- Requires Play Console justification + demo video when submitting to Play Store
- Manifest property: `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE` with value: *"Displays real-time fraud protection overlay during active phone calls to protect elderly users from scam calls"*
- For July 31 hackathon submission: no Play Console review required. Correctly architected for eventual Play Store submission.

---

### 1.6 `USE_FULL_SCREEN_INTENT` and `POST_NOTIFICATIONS` — Critical Permission Path

**Source:** Play Console Help (January 2025), developer.android.com/about/versions/15/behavior-changes-15.

`USE_FULL_SCREEN_INTENT`: Auto-granted only to calling and alarm apps since January 2025. For all other apps targeting Android 14+, users must explicitly grant it in Settings. Must check `notificationManager.canUseFullScreenIntent()` before setting `fullScreenIntent` on API 34+. Graceful degradation if denied: high-priority heads-up notification — still visible, just doesn't wake a locked screen.

`POST_NOTIFICATIONS` (required API 33+): If the user denies this, **zero notifications appear**. No incoming call warning. No heads-up. Complete silence. This is the single most catastrophic permission denial for our module. It must be the first permission requested in onboarding, with the plainest possible explanation: *"Whis needs to show warnings on your screen. Without this, it cannot protect you."*

`Notification.CallStyle`: Available from **API 31 only**. Must use a standard `NotificationCompat` fallback for API 24–30.

---

### 1.7 OEM Battery Optimization — Confirmed as the #1 Deployment Risk

Source: DEV Community (April 2026) — direct account of building a 24/7 safety monitoring app for elderly parents on Indian Android devices; dontkillmyapp.com (June 2026).

Xiaomi (MIUI/HyperOS), Samsung (One UI), OPPO, Vivo, Realme — collectively over 75% of Indian Android market — aggressively kill background processes. Xiaomi resets Background Autostart permissions after OTA updates. Samsung's Adaptive Battery marks apps inactive after 3 days. OPPO freezes apps overnight.

**Mitigation:** `CallScreeningService` is system-bound. OEM process managers cannot kill what the Android OS itself binds. `LiveCallGuardService` (our overlay FGS) starts only when a call is actively being received — it is not a persistent background process. But users still need to exempt Whis from battery optimization for the `CallStateTracker` and notification delivery reliability. Onboarding walks them through this with OEM-specific steps.

---

### 1.8 TRAI and Indian Telecom — Current (July 2026)

**CNAP (Calling Name Presentation):**
Approved by TRAI October 2025. Shows KYC/Aadhaar-verified subscriber name on the native incoming call screen. Rolling out on 4G/5G — Jio and Vi pilots live in select circles. No third-party API exists to query this data; it is network-level display only.
Source: Mondaq legal analysis (December 2025), 91mobiles (December 2025).

**What this means for Whis:** CNAP tells the user WHO is calling (legal name from SIM registration). It does not tell them whether to trust the call, what type of scam it might be, or what to do. Scammers use SIMs registered with misused Aadhaar — CNAP shows their fake name with zero red flags. Whis provides the layer CNAP cannot: risk context and action guidance. We are not made redundant by CNAP.

**TRAI 1600/140 series:** Confirmed current. 1600-series = BFSI (banks/insurance/financial services). 140-series = telemarketing. 1800/1860 = toll-free. Truecaller is restricted from blanket-labeling 1600-series as spam. We show contextual guidance, not a spam label — legal distinction flagged for Master review (Open Question 3).

**No public TRAI scam number lookup API exists.** The DLT platform is for businesses registering SMS sender IDs — not a consumer-facing fraud database. We build our own.

---

### 1.9 DPDP Act 2023 / Rules 2025

DPDP Rules notified November 13, 2025 — Phase 1 active as of July 2026.
Source: Respectlytics mobile app compliance guide (April 2026), Nevatrix DPDP guide (July 2026).

A phone number is personal data under the Act. Uploading a reported number to Firebase requires:
- Explicit, specific, informed user consent **before** any upload — not buried in a ToS
- Clear statement of purpose: *"This number will be shared anonymously to protect other Whis users from scams"*
- Mechanism for users to withdraw previously submitted reports

The community report upload is gated behind a one-time consent dialog. Implemented in `CallCommunityReporter`.

---

### 1.10 Firebase Firestore Free Tier — Confirmed Sufficient at Launch Scale

Source: firebase.google.com/pricing (June 2026), budgetforge.dev/tools/firebase-pricing-2026 (July 2026).

Spark plan (free): **50,000 reads/day, 20,000 writes/day, 1 GiB stored.**

At launch scale: each incoming suspicious call triggers 1 Firestore read (check community reports). Each confirmed scam report = 1 write. With local caching (number checked = cached for 24 hours), 50,000 reads/day supports ~50,000 unique suspicious calls/day. This is well above our expected launch scale. Risk: no hard spending cap if we switch to Blaze plan and have a bug causing a read loop. Mitigation: local caching eliminates redundant reads; writes only on explicit user confirmation.

---

### 1.11 WhatsApp / OTT Call Screening — Definitively Not Possible

Source: Android Authority (May 2026), PhoneArena (May 2026).

WhatsApp and Telegram calls are VoIP — they do not flow through Android's telephony framework. `CallScreeningService` and `TelephonyCallback` intercept cellular calls only. Android 16.1 introduced VoIP integration with the native dialer, but this requires Android 16.1+ AND each OTT app to explicitly adopt the new API — only early Pixel devices in rollout as of May 2026. Not possible before July 31. Not possible for our target user base for 2+ years.

**Explicitly out of scope. Not a future goal either unless OTT app adoption grows.**

---

### 1.12 Real-Time Call Audio Analysis — Blocked by Google

Source: GSMArena (April 2022), Grokipedia (February 2026).

Google banned Accessibility Service use for call audio recording in May 2022. Android OS does not expose both sides of a phone call to third-party apps. This blocks any "keyword detection" that listens for the caller saying "OTP" or "CBI arrest." These constraints persist through Android 16.

Not attempted. The `ScamCallPatternLibrary` instead provides education content that teaches the USER to recognize these keywords themselves.

---

### 1.13 What Truecaller Does and What We Learn From It

Source: Truecaller Play Store listing (June 2026), TechCrunch (March 2024), calilio.com analysis (November 2025).

Truecaller holds `ROLE_CALL_SCREENING` for pre-ring detection. Its primary database is community-sourced: 374M+ users reporting spam builds its number database. It uses `CallScreeningService` on Android 10+. Its "Max" feature (premium, Android only) uses AI to block calls not in contacts — this is auto-blocking, which we explicitly reject for our user.

**What we take from this:** The community reporting loop is the right long-term intelligence engine. Our own Whis community database, even starting from zero, grows with every user report. We do not need Truecaller's database — we build a lighter, India-specific, scam-pattern-aware version.

**What we do not take:** Auto-blocking. Truecaller "Max" blocks calls from people not in contacts — a 68-year-old user with a new grandchild's number not yet saved would miss that call permanently. Alert-only is correct for our users.

**Conflict risk:** If a user has Truecaller installed as their `ROLE_CALL_SCREENING` holder, requesting the role shows a system conflict dialog. See Open Question 1.

---

## SECTION 2 — HOW TO APPLY IT

### 2.1 The Detection Architecture

Our app does not run a persistent background service waiting for calls. Instead:

- `WhisCallScreeningService` is declared in the manifest. The Android OS binds it when a call arrives — no process needs to be alive.
- After `onScreenCall()` fires, we analyze the number offline in under 200ms and call `respondToCall()` — always allowing the call, never blocking.
- After responding, we launch the overlay notification and kick off an async Firebase check.
- `CallStateTracker` (registered dynamically from `WhisCallScreeningService`) watches for OFFHOOK and IDLE to control the live overlay lifecycle.

This means: zero persistent background footprint. The OS manages our lifecycle. OEM battery managers cannot interfere with call detection.

### 2.2 The Scoring System

**One direction only: lower score = more dangerous. 0 = confirmed scam. 100 = fully trusted.**

Every incoming number starts at 35 (unknown mobile). Layers apply sequentially:

**Layer 1 — Number Intelligence (NumberIntelligenceEngine.java):**

| Number Type | Base Score | Notes |
|---|---|---|
| In user's contacts | 95 | Return TRUSTED immediately, skip all layers |
| Hardcoded verified number (SBI 1800 112211, HDFC 1800 1600, 1930 helpline, etc.) | 88 | Small curated list, pre-shipped |
| 1600-series (TRAI BFSI) | 70 | Show "Bank service — real banks never ask for OTP" |
| 1800 / 1860 toll-free | 58 | Show "Verify on company's official website" |
| Indian business landline (011/022/044/080/033 prefix) | 48 | Neutral — likely legitimate |
| 140-series telemarketing | 30 | SUSPICIOUS — offer DND registration tip |
| Standard Indian mobile (starts 6/7/8/9) | 35 | Unknown — proceed to layers 2–3 |
| +95 Myanmar | 4 | HIGH RISK — stop here |
| +855 Cambodia | 4 | HIGH RISK — stop here |
| +233 Ghana / +234 Nigeria / +254 Kenya | 7 | HIGH RISK — stop here |
| International number (+1, +44, etc.) calling Indian device | 12 | HIGH RISK — very unusual pattern |

**Layer 2 — Contact Check (ContactTrustChecker.java):**
READ_CONTACTS query. If found: override score to 95, return TRUSTED immediately. This is the single cheapest and most valuable check — most legitimate calls are from known contacts.

**Layer 3 — Behavioral Analysis (CallBehaviorAnalyzer.java):**
Queries `LocalCallHistoryDatabase` (our internal SQLite log of all calls seen by Whis since installation). **No READ_CALL_LOG permission needed.**

| Behavioral Signal | Score Penalty |
|---|---|
| Same number called 3+ times in 2 hours | −30 (more dangerous) |
| Same number called 5+ times, mostly unanswered | −22 |
| Call arriving between 10pm and 6am | −18 |
| User previously dismissed this number as scam | −40 |
| Number appears in a scam SMS as a callback (signal from SMS module) | −38 |

**Layer 4 — Community Intelligence (async, post-response):**

| Community Reports | Score Penalty |
|---|---|
| 3–9 confirmed reports | −25 |
| 10+ confirmed reports | −40 |
| `confirmedScam: true` flag | Override to score 5, verdict HIGH_RISK |

Community result updates the live badge after it arrives. No effect on `respondToCall()` timing.

**Verdict thresholds:**
- 0–25 → HIGH_RISK (red badge)
- 26–44 → SUSPICIOUS (orange badge)
- 45–65 → UNKNOWN (grey badge — no action, no overlay)
- 66–84 → LIKELY_SAFE (green badge, tip shown)
- 85–100 → TRUSTED (green badge, no tip needed)

UNKNOWN calls: no overlay shown. Only SUSPICIOUS, HIGH_RISK, and notable LIKELY_SAFE (1600-series) calls surface a UI.

### 2.3 The Live Overlay — Two Phases

**Phase 1 — Phone Is Ringing:**
High-priority notification with `fullScreenIntent` targeting `CallWarningActivity`. On API 31+: `Notification.CallStyle`. On API 24–30: standard `NotificationCompat`. On API 34+: check `notificationManager.canUseFullScreenIntent()` first; if false, post heads-up notification instead.

`CallWarningActivity` shows:
- Verdict color + label (full screen, large text, Gujarati/Hindi friendly)
- One-line reason: *"Number reported as scam by 47 Whis users"* or *"International call to your Indian number — very unusual"*
- One action tip (from `ScamCallPatternLibrary`)
- "DECLINE CALL" button (calls `TelecomManager.endCall()` — requires `ANSWER_PHONE_CALLS` permission, which we declare)

**Phase 2 — Call Is Active (user answered):**
`LiveCallGuardService` (FGS type=`specialUse`) starts when `CallStateTracker` detects OFFHOOK. It inflates `LiveCallBadgeView`, a `TYPE_APPLICATION_OVERLAY` window positioned top-right, 280dp × 72dp — never covering the dial pad or end-call button.

Badge states:
```
GREEN  : ✓ [Contact Name]
YELLOW : ⚠ Bank Call — Never share OTP
ORANGE : ⚠ SUSPICIOUS — Do not share card details
RED    : ⚠ HIGH RISK — Do not share anything. Hang up.
```

Tapping badge expands to show full reasons + *"If anyone asks you to add a third person to this call, hang up immediately — it is a scam."* + [REPORT SCAM] button.

Badge updates live if Firebase community result arrives after the call was answered.

**Phase 3 — Call Ends:**
`CallStateTracker` detects IDLE. `LiveCallGuardService` stops. If call verdict was SUSPICIOUS or HIGH_RISK: launch `PostCallFeedbackActivity` (bottom sheet, 3 buttons: "Yes, it was a scam" / "No, it was fine" / "Not sure"). "Yes" tap → DPDP consent check → Firebase upload.

### 2.4 The Scam Pattern Library

15 patterns derived from MHA advisories, SBI warnings, RBI circulars, caller.md research. Each pattern has:
- `patternId` (e.g. `DIGITAL_ARREST`)
- `displayName` (e.g. "Digital Arrest Scam")
- `tipDuringCall` (60-char max, shown in live badge)
- `educationText` (2–3 sentences, shown in PostCallFeedbackActivity if pattern matched)
- `scoreSignals` — number characteristics that increase confidence in this pattern

Top 5 patterns and their score signals:
1. **DIGITAL_ARREST** — International number OR first-time caller + odd hour + long call duration (pattern confirmed after call)
2. **FAKE_KYC_UPDATE** — 10-digit mobile impersonating bank, calls between 9am–6pm
3. **CALL_MERGE_OTP** — Number in contacts of victim who is actively doing UPI (cross-signal from payment module if available)
4. **CUSTOM_CARE_IMPERSONATION** — Mobile number claiming to be 1600-series service
5. **FAKE_DELIVERY** — Number matches pattern of bulk-registered SIMs (+91 70/72/73 prefixes known for scam call centers)

### 2.5 Community Database — Firebase Schema

```
community_reports/{e164_number}
  reportCount:        int          // total user reports
  lastReportedAt:     timestamp
  reportedPatterns:   [string]     // pattern IDs submitted with reports
  confirmedScam:      boolean      // true when reportCount >= 10
  trustDelta:         int          // pre-computed score penalty (negative value)

// e164_number = E.164 format, e.g. "+919876543210"
// No names, no user IDs, no device IDs stored in documents
// Firestore security rules: authenticated Whis users write; open read
```

Read: once per incoming number, cached in `LocalCallHistoryDatabase` for 24 hours.
Write: only on explicit user confirmation in `PostCallFeedbackActivity`, with DPDP consent dialog on first-ever report.

### 2.6 Onboarding — Non-Negotiable First-Run Flow

Without completing this, call screening does not work. Each step has a programmatic success check before proceeding.

```
Step 1: POST_NOTIFICATIONS (API 33+)
  → First and most critical. If denied: overlay can still show but notification won't.
  → Plain-language explanation. No skip option.

Step 2: ROLE_CALL_SCREENING
  → RoleManager.createRequestRoleIntent(ROLE_CALL_SCREENING)
  → If user already has Truecaller: system shows conflict dialog.
  → If declined: module works in degraded mode — overlay shows AFTER ring, not before.
  → Degraded mode must be clearly communicated.

Step 3: USE_FULL_SCREEN_INTENT (API 34+)
  → Routes to system Settings if not granted.
  → If declined: banner notification on lock screen instead of full wake-up.

Step 4: SYSTEM_ALERT_WINDOW
  → Routes to Settings.ACTION_MANAGE_OVERLAY_PERMISSION
  → If declined: no live badge during active call. Warn the user clearly.

Step 5: READ_CONTACTS
  → Runtime request. If declined: contact layer skipped; saved contacts not recognized as trusted.

Step 6: Battery Optimization Exemption (OEM-specific)
  → Detect via Build.MANUFACTURER, show exact tap path:
  → Xiaomi:        Settings → Apps → Whis → App permissions → Background autostart [ON]
                   Settings → Battery → App battery saver → Whis → No restriction
  → Samsung:       Settings → Battery and device care → Battery → Background usage limits
                   → Never sleeping apps → Add Whis
  → OPPO/Realme:   Settings → App Management → Whis → Battery → No restrictions
  → Default:       Settings → Apps → Whis → Battery → Unrestricted

Step 7: Test confirmation
  → "Ask a family member to call you now and check that the Whis badge appears."
  → [SKIP — I'll check later] allowed here.
```

---

## SECTION 3 — THE PLAN

### 3.1 Files — Complete List with Responsibilities

```
com.whis.app.call/
│
├── model/
│   └── WhisCallAnalysis.java
│       The shared data object this module produces. Consumed by UI module,
│       AI Chat module, and SMS module (for cross-signal). Defined first.
│
├── db/
│   └── LocalCallHistoryDatabase.java
│       SQLite database. Stores every call seen by WhisCallScreeningService:
│       number (E.164), timestamp, duration (from OFFHOOK to IDLE),
│       our verdict, community_delta (updated async). Used by CallBehaviorAnalyzer.
│       Also caches Firebase community results with 24-hour TTL to prevent quota burn.
│
├── engine/
│   ├── NumberIntelligenceEngine.java
│   │   Layer 1. TRAI series detection, country code risk, hardcoded verified numbers,
│   │   format normalization to E.164. Returns base score + numberType string.
│   │
│   ├── ContactTrustChecker.java
│   │   Layer 2. Queries ContentResolver for READ_CONTACTS. If number in contacts:
│   │   returns WhisCallAnalysis with score=95, verdict=TRUSTED immediately.
│   │   Skips all other layers.
│   │
│   ├── CallBehaviorAnalyzer.java
│   │   Layer 3. Queries LocalCallHistoryDatabase for behavioral patterns:
│   │   frequency, unanswered rate, time-of-day, user-flagged history.
│   │   Returns score adjustments (negative = more dangerous).
│   │
│   ├── ScamCallPatternLibrary.java
│   │   Static library of 15 India scam patterns. Two uses:
│   │   (a) Returns tipDuringCall for live badge from matched pattern.
│   │   (b) Returns educationText for PostCallFeedbackActivity.
│   │   No audio analysis. Pattern matching uses number metadata only.
│   │
│   └── CallTrustScoreEngine.java
│       Orchestrator. Runs Layers 1 → 2 → 3 in sequence. Stops early if
│       TRUSTED or HIGH_RISK is determined definitively (e.g. contacts hit,
│       Myanmar country code). Returns WhisCallAnalysis in under 200ms.
│       Does not touch Firebase — offline only.
│
├── cache/
│   └── CallAnalysisCache.java
│       In-memory singleton. Stores current call's WhisCallAnalysis.
│       Shared between WhisCallScreeningService, LiveCallGuardService,
│       and LiveCallBadgeView without Intents or Bundles.
│       Cleared on IDLE.
│
├── firebase/
│   └── CallCommunityReporter.java
│       Two functions:
│       (a) checkAsync(number, callback) — reads community_reports/{number},
│           updates CallAnalysisCache and notifies LiveCallBadgeView via callback.
│           Reads cached result from LocalCallHistoryDatabase if TTL not expired.
│       (b) reportScam(number, patternId) — writes to community_reports/{number},
│           increments reportCount, appends patternId. Called only after DPDP consent.
│
├── service/
│   ├── WhisCallScreeningService.java
│   │   Extends CallScreeningService. System entry point for all call detection.
│   │   onScreenCall():
│   │     1. Extract and normalize number from Call.Details.getHandle()
│   │     2. If outgoing: respondToCall(allow) immediately, return
│   │     3. Run CallTrustScoreEngine.analyzeOffline() [<200ms]
│   │     4. Store result in CallAnalysisCache and LocalCallHistoryDatabase
│   │     5. respondToCall(allow) — NEVER block
│   │     6. If verdict != TRUSTED and != UNKNOWN: show warning notification
│   │     7. Start CallStateTracker
│   │     8. Start CallCommunityReporter.checkAsync() with badge update callback
│   │
│   ├── CallStateTracker.java
│   │   Not a Service. A class that registers TelephonyCallback (API 31+)
│   │   or PhoneStateListener (API 24–30) on a background thread.
│   │   On OFFHOOK: starts LiveCallGuardService
│   │   On IDLE: stops LiveCallGuardService, clears CallAnalysisCache,
│   │            triggers PostCallFeedbackActivity if verdict was SUSPICIOUS/HIGH_RISK,
│   │            writes final call record to LocalCallHistoryDatabase
│   │
│   └── LiveCallGuardService.java
│       FGS type=specialUse. Starts on OFFHOOK, stops on IDLE.
│       Responsibilities:
│       - Inflates and adds LiveCallBadgeView to WindowManager
│       - Subscribes to CallCommunityReporter callback → updates badge when Firebase returns
│       - Removes overlay on stop
│       Manifest declaration:
│         android:foregroundServiceType="specialUse"
│         <property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
│                   android:value="Displays real-time fraud protection overlay
│                                  during active phone calls for elderly users"/>
│
├── ui/
│   ├── LiveCallBadgeView.java
│   │   Custom View inflated into WindowManager as TYPE_APPLICATION_OVERLAY.
│   │   Size: 280dp × 72dp collapsed, full-width expanded on tap.
│   │   Position: top-right, below status bar, never over dial pad.
│   │   States: TRUSTED (green), LIKELY_SAFE (green with tip), SUSPICIOUS (orange),
│   │           HIGH_RISK (red, pulsing border), UPDATED (flash when Firebase arrives).
│   │   Expanded state: full reasons + "REPORT SCAM" button + merge scam tip.
│   │
│   ├── CallWarningActivity.java
│   │   Launched by fullScreenIntent on high-importance notification.
│   │   Shows when phone is ringing for SUSPICIOUS / HIGH_RISK / LIKELY_SAFE-1600 calls.
│   │   Large text, high contrast, single action per screen.
│   │   Content: verdict label + reason + tip + [DECLINE CALL] button.
│   │   Auto-dismissed when call transitions to OFFHOOK or IDLE.
│   │   Requires ANSWER_PHONE_CALLS permission for the decline button.
│   │
│   ├── PostCallFeedbackActivity.java
│   │   Shown as bottom sheet after call ends (verdict was SUSPICIOUS or HIGH_RISK).
│   │   Three buttons: "Yes, scam" → DPDP consent → Firebase report
│   │                  "No, it was fine" → log locally, no upload
│   │                  "Not sure" → dismiss, no action
│   │   First "Yes" tap ever → show DPDP consent dialog before upload.
│   │   Subsequent taps: consent already given, upload directly.
│   │
│   └── OnboardingCallSetupActivity.java
│       7-step first-run flow (detailed in Section 2.6).
│       Each step programmatically checks its own success state before proceeding.
│       Stores completion flag in SharedPreferences.
│       Degraded mode paths documented and shown to user if permissions denied.
```

---

### 3.2 Manifest Declarations

```xml
<!-- Permissions -->
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.ANSWER_PHONE_CALLS" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- NO READ_CALL_LOG — behavioral layer uses our internal DB -->

<!-- Call Screening Service -->
<service
    android:name=".call.service.WhisCallScreeningService"
    android:exported="true"
    android:permission="android.permission.BIND_SCREENING_SERVICE">
    <intent-filter>
        <action android:name="android.telecom.CallScreeningService" />
    </intent-filter>
</service>

<!-- Live overlay FGS -->
<service
    android:name=".call.service.LiveCallGuardService"
    android:foregroundServiceType="specialUse"
    android:exported="false">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="Displays real-time fraud protection overlay during active
                       phone calls to protect elderly users from scam calls" />
</service>

<!-- Restore screening state after device reboot -->
<receiver
    android:name=".call.BootReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

---

### 3.3 `WhisCallAnalysis` — Shared Data Contract

```java
public class WhisCallAnalysis {
    // ── Identity ─────────────────────────────────────────────────────────────
    public String  phoneNumber;           // E.164, e.g. "+919876543210"
    public String  numberType;            // BANK_SERVICE | TOLL_FREE | PROMOTIONAL |
                                          // HIGH_RISK_INTL | BUSINESS_LANDLINE |
                                          // CONTACT | UNKNOWN_MOBILE | VERIFIED_GOVT

    // ── Verdict ──────────────────────────────────────────────────────────────
    public int     trustScore;            // 0–100. 0=confirmed scam. 100=fully trusted.
    public String  verdict;               // HIGH_RISK | SUSPICIOUS | UNKNOWN |
                                          // LIKELY_SAFE | TRUSTED
    public String  primaryReason;         // One line. "47 Whis users reported this number."
    public String  liveTip;               // ≤60 chars. Shown in live badge during call.

    // ── Pattern ──────────────────────────────────────────────────────────────
    public String  scamPatternId;         // null if no match. e.g. "DIGITAL_ARREST"
    public String  scamPatternName;       // Human-readable. e.g. "Digital Arrest Scam"

    // ── Community ────────────────────────────────────────────────────────────
    public int     communityReportCount;  // 0 until Firebase responds
    public boolean confirmedCommunityScam;// true if reportCount >= 10

    // ── Metadata ─────────────────────────────────────────────────────────────
    public boolean wasInContacts;
    public boolean isOfflineOnly;         // true until Firebase result arrives
    public long    analysisTimestampMs;
}
```

---

### 3.4 Build Order — Concrete Sequence

Build in this exact order. Each step compiles and has a unit-testable outcome before moving to the next.

**Step 1 — `WhisCallAnalysis.java`**
The data contract. Nothing else can be written until this exists. Coordinate with Master to confirm field names match the shared contract before Step 1 is finalized.

**Step 2 — `LocalCallHistoryDatabase.java`**
SQLite database using Room. Schema: `call_history` table (number, timestamp, duration_ms, verdict, community_delta, community_cached_at). Without this, behavioral layer and Firebase caching have nowhere to write.

**Step 3 — `NumberIntelligenceEngine.java`**
Pure logic, no Android dependencies beyond string parsing. Fully unit-testable. Write JUnit tests against: 1600-series, +95, contacts (mocked), standard mobile. This is the foundation of the scoring system.

**Step 4 — `ContactTrustChecker.java`**
Requires a real device or Robolectric for ContentResolver. Mock it in unit tests. Implement: query `ContactsContract.CommonDataKinds.Phone`, normalize incoming number and stored numbers to E.164 before comparison (Indian numbers stored in various formats — handle `+91XXXXXXXXXX`, `0XXXXXXXXXX`, and `XXXXXXXXXX`).

**Step 5 — `CallBehaviorAnalyzer.java`**
Queries `LocalCallHistoryDatabase`. Test against: empty DB (new install), 3 calls in 2 hours, odd-hour call.

**Step 6 — `ScamCallPatternLibrary.java`**
Static data class. All 15 patterns with their fields. No external dependencies. Write a simple test that returns the correct tip for a known pattern ID.

**Step 7 — `CallTrustScoreEngine.java`**
Integration of Steps 3–6. Benchmark `analyzeOffline()` on a low-end Android device — must complete in under 200ms.

**Step 8 — `CallAnalysisCache.java`**
Singleton. Thread-safe (analysis written from background thread in `WhisCallScreeningService`, read from UI thread in `LiveCallBadgeView`). Use `volatile` or `AtomicReference`.

**Step 9 — `WhisCallScreeningService.java`**
The core. Depends on Steps 7 and 8. Test manually: install on device, call from another phone, confirm `respondToCall()` fires and notification appears. Verify the call rings (we always allow).

**Step 10 — `CallStateTracker.java`**
Dual implementation: `TelephonyCallback` for API 31+, `PhoneStateListener` for API 24–30. Test: answer a call and verify OFFHOOK fires. End a call and verify IDLE fires.

**Step 11 — `LiveCallBadgeView.java`**
Custom View. No FGS yet — test by manually adding to `WindowManager` from a test button. Verify all badge states render correctly. Verify it doesn't cover dial pad on Xiaomi and Samsung test devices.

**Step 12 — `LiveCallGuardService.java`**
Adds `LiveCallBadgeView` to `WindowManager`. Starts on OFFHOOK, stops on IDLE. Test the full call lifecycle: ring → answer → badge appears → hang up → badge disappears.

**Step 13 — `CallWarningActivity.java`**
Test by calling device with a number in the HIGH_RISK scoring range (+95 number). Verify full-screen activity appears on top of lock screen (with `USE_FULL_SCREEN_INTENT` granted). Test decline button.

**Step 14 — `CallCommunityReporter.java`**
Firebase integration. Test checkAsync() with a seeded test document. Test reportScam() — verify document updates in Firebase console. Verify 24-hour TTL caching prevents repeat reads.

**Step 15 — `PostCallFeedbackActivity.java`**
Test by ending a HIGH_RISK call. Verify bottom sheet appears. Verify DPDP consent dialog on first "Yes" tap. Verify Firebase write. Verify no upload on "No" or "Not sure".

**Step 16 — `OnboardingCallSetupActivity.java`**
Last step. All permissions and FGS are proven to work individually. Assemble the guided flow. Test on: Xiaomi device (HyperOS), Samsung device (One UI). Verify OEM-specific battery instructions are shown correctly via `Build.MANUFACTURER`.

**Step 17 — `BootReceiver.java`**
One-file receiver. On BOOT_COMPLETED: verify `ROLE_CALL_SCREENING` is still held (OEM OTA can reset it). If not, post a notification: *"Whis needs you to re-enable call protection. Tap here."*

---

### 3.5 Dependencies on Other Modules

| What I Need | From | Fallback If Not Ready |
|---|---|---|
| Locked `WhisCallAnalysis` schema | **Master** | Ship our definition; Master normalizes at merge. Must be agreed before Step 1. |
| SMS module cross-signal: "this number appeared as a callback in a scam SMS" | **SMS module** | Skip the −38 score penalty for callback numbers. Behavioral layer still works. |
| Firebase project + Firestore setup + auth initialization | **Master / shared config** | Community layer fully disabled at compile time via a `BuildConfig.COMMUNITY_ENABLED` flag. Offline analysis unaffected. |
| App design system (colors, typography, button styles) for `CallWarningActivity` and `PostCallFeedbackActivity` | **UI module** | Local standalone layouts in our module. UI module refactors post-merge. |
| Deep-link into AI chat agent post-call: "What was this scam? Explain it to me." | **AI Chat Agent module** | Replaced by static pattern education text from `ScamCallPatternLibrary`. |
| Shared `WhisConsentManager` for DPDP consent | **Master** | We build our own consent dialog in `CallCommunityReporter`. Master consolidates into shared component post-merge. |

---

### 3.6 Explicitly Out of Scope

| Item | Reason |
|---|---|
| WhatsApp / Telegram / OTT call detection | No Android API exists below Android 16.1. OTT adoption of new API is 2+ years away from Indian market penetration. Not possible before July 31. |
| Real-time call audio / keyword speech detection | Blocked by Android OS and Play Store policy (Google, May 2022). Cannot be done by any Play Store app. |
| Auto-blocking any incoming call | One wrongly blocked legitimate call (family member's new number, doctor) and an elderly user disables the app forever. Alert-only is non-negotiable. |
| Call hold / merge real-time detection | `TelephonyCallback` exposes only RINGING/OFFHOOK/IDLE. Hold detection requires `ROLE_DIALER`. We address call merge through proactive static education in the badge. |
| CNAP data integration | No third-party API exists. Network-level display only. |
| Multi-SIM disambiguation in behavioral analysis | `LocalCallHistoryDatabase` logs calls without SIM slot identifier. Behavioral signals may blend two SIMs on dual-SIM phones. Known limitation, not addressed in v1. |
| Call recording | Blocked by Android for Play Store apps. Not needed. |
| Outgoing call verification | `CallScreeningService` fires for outgoing calls — we respond immediately without analysis. Future feature only. |
| SIM-swap detection | Requires carrier API or monitoring outbound auth SMS. Neither accessible to a third-party app. |

---

### 3.7 Open Questions for Master

**Q1 — Truecaller conflict on ROLE_CALL_SCREENING**
Most Indian users have Truecaller as their call screener. When we request `ROLE_CALL_SCREENING`, the system shows a conflict dialog. Two paths: (a) ask the user to replace Truecaller and explain why, or (b) present a degraded mode (post-ring overlay only) as default, full pre-ring screening as opt-in. This is a product decision that affects onboarding design. I need an answer before building `OnboardingCallSetupActivity`.

**Q2 — Firebase community_reports schema: coordinate with SMS module**
SMS module will likely also report numbers to Firebase. If they write to the same `community_reports` collection, we need a shared schema. If they write to a separate collection, I need to know so `CallCommunityReporter` doesn't duplicate their data. Confirm before Step 14.

**Q3 — TRAI 1600-series contextual tip: legal sign-off needed**
We show "Bank service number — real banks never ask for your OTP" on 1600-series calls. This is contextual guidance, not a spam label. I believe this is within the law but I am not a lawyer. Someone needs to confirm before we ship.

**Q4 — Package name**
This plan uses `com.whis.app.call`. The original `caller.md` used `com.scamshield.app.call`. Confirm before Step 1 — wrong package name in the data contract creates a merge conflict that wastes everyone's time.

**Q5 — Score direction locked in shared contract**
This plan uses: 0 = dangerous, 100 = trusted. Must be canonical across all modules. If SMS module or AI agent uses the opposite direction, the shared contract breaks. Master picks, everyone follows.

**Q6 — DPDP consent: shared component or per-module?**
Both Call and SMS modules will need consent before uploading data. Recommend Master owns a `WhisConsentManager` with a single consistent UI and legal text. If not, we each implement our own — which is worse legally and visually.

---

### 3.8 API 24–28 Degradation — Honest Statement

On Android 7.0–8.1 (API 24–28):
- Pre-ring blocking not guaranteed — warning may appear as the phone starts ringing
- `TelephonyCallback` unavailable — use `PhoneStateListener` path
- `Notification.CallStyle` unavailable — use standard `NotificationCompat`
- Onboarding screen states plainly: *"On Android 7 and 8, the Whis warning appears as your phone starts ringing, not before."*

Estimated Android 7/8 users in India in 2026: low single-digit percentage. India's rapid 4G adoption since Jio 2016 pushed most active users to Android 10+ on budget devices. We do not abandon these users — we degrade cleanly and honestly.

---

*All research conducted July 2026. Primary sources: developer.android.com (last updated Feb 26, 2026), firebase.google.com/pricing (June 2026), support.google.com/googleplay/android-developer (current), MHA cybercrime data via theprint.in and insightsonindia.com (February 2026), TRAI CNAP via mondaq.com and 91mobiles.com (December 2025), DPDP compliance via respectlytics.com (April 2026), OEM battery behavior via dontkillmyapp.com (June 2026) and DEV Community (April 2026), FGS type documentation via developer.android.com/develop/background-work/services/fgs/service-types (confirmed current July 2026).*
