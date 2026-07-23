# MASTER_PLAN.md — Whis: The Unified Build Pipeline
**Compiled by:** Master
**Merges:** CALL_PROTECTION_PLAN.md · MSG_PLAN.md · AI_AGENT_PLAN.md
**Still pending:** Learning section (research not yet submitted) · UI module (research done, waiting on this document to begin build)
**Deadline:** 31 July 2026
**Status:** Ready to circulate to all module owners. This document is now the single source of truth. Where it conflicts with any individual module's plan, THIS document wins.

---

## 0. WHY THIS DOCUMENT EXISTS

Three genuinely excellent, well-researched plans came in. Each one, in isolation, is strong. Merged together, they contained five real conflicts that would have caused broken builds, wasted work, or a Play Store rejection discovered too late:

1. Three different root package names
2. Two opposite directions for the same numeric score
3. An assumed "Login module" that was never scoped as one of our 5 modules
4. A Play Store compliance requirement in the SMS plan that — if ignored — makes the whole module undistributable
5. Duplicate contact-lookup logic being independently built twice

Every one of these is resolved below. No module owner needs to re-research anything — they need to read Section 2 (the resolutions) and Section 3 (the canonical contract), adjust their already-excellent work to match, and proceed.

---

## 1. THE PIPELINE — HOW EVERYTHING CONNECTS

```
                         ┌─────────────────────────────────┐
                         │   FIREBASE PROJECT (Master-owned)│
                         │   • Firestore: community_reports │
                         │   • Firebase AI Logic: Gemini    │
                         │   • google-services.json (shared)│
                         └───────────┬─────────────┬────────┘
                                     │             │
                    ┌────────────────┘             └───────────────┐
                    │                                               │
         ┌──────────▼──────────┐                         ┌─────────▼──────────┐
         │   CALL MODULE        │                         │   AI AGENT MODULE   │
         │  WhisCallScreeningSvc │                         │  Gemini 2.5 Flash-  │
         │  → offline scoring    │──── whis_flags ────────▶  Lite chat + guided │
         │  → notification       │   (SharedPreferences)   │  crisis flow        │
         │  → community check    │                         │  → Red Alert (API-  │
         └──────────┬────────────┘                         │    free, instant)   │
                    │                                       └─────────┬──────────┘
                    │  DetectionResult                                 │
                    │  (canonical, Sec. 3)                             │ reads
                    │                                                   │ whis_user_profile
         ┌──────────▼────────────┐                          ┌─────────▼──────────┐
         │   MSG MODULE           │──── whis_flags ─────────▶                    │
         │  SmsReceiver→5-layer   │   (SharedPreferences)                        │
         │  engine (rules→TFLite  │                                              │
         │  →URL→Gemini fallback) │                                              │
         └──────────┬─────────────┘                                              │
                    │  DetectionResult                                            │
                    │  (canonical, Sec. 3)                                        │
                    │                                                             │
         ┌──────────▼─────────────────────────────────────────────────────────┐  │
         │                        UI MODULE                                    │  │
         │   Onboarding (captures whis_user_profile + all consent, ONE flow)  │◀─┘
         │   → Home dashboard, navigation, notification card rendering        │
         │   → Consumes DetectionResult from Call + MSG for history/alerts    │
         └──────────┬──────────────────────────────────────────────────────────┘
                    │
         ┌──────────▼─────────────┐
         │   LEARNING MODULE        │   (plan not yet submitted — slots in here,
         │   quiz + companion-guided│    reads real scam patterns from Call's
         │   mission content        │    ScamCallPatternLibrary + MSG's rule set
         └───────────────────────────    once available)
```

**Build/merge order stays:** Call → SMS → Learning → AI Agent → UI → Integration, per the original sequencing — but note Call, SMS, and AI Agent can be *coded* in parallel right now since their contracts are now locked below. UI genuinely needs at least Call + SMS producing real `DetectionResult` objects before it can build against real data instead of guesses — that's why UI is correctly waiting.

---

## 2. CONFLICT RESOLUTIONS — READ THIS BEFORE WRITING ANY CODE

### 2.1 Package name — RESOLVED
Three different conventions came in: `com.whis.app.call`, `com.whis.agent`, `com.scamshield.app.msg`.

**Canonical root: `com.whis.app`**
- Call module: `com.whis.app.call` (already correct, no change needed)
- MSG module: rename `com.scamshield.app.msg` → `com.whis.app.msg`
- AI Agent module: rename `com.whis.agent` → `com.whis.app.agent`
- Learning module (when it arrives): `com.whis.app.learning`
- UI module: `com.whis.app.ui`
- Shared/core (new, see 2.5): `com.whis.app.core`

### 2.2 Score direction — RESOLVED (this was a real, silent conflict)
Call module proposed: **0 = confirmed scam, 100 = fully trusted.**
MSG module didn't specify a direction but implied the opposite (higher confidence = higher scam likelihood, matching how the original SMS engine in this project always worked).

**Canonical direction: `riskScore`, 0–100, where HIGHER = MORE dangerous.** 0 = confirmed safe, 100 = confirmed scam.

Reasoning: this matches how every real fraud/spam score in the industry works (credit risk scores, spam scores, Google Messages' own "likely scam" framing) and matches the AI Agent module's `RiskLevel` enum (LOW→CRITICAL, increasing severity = increasing number/urgency). Inverting Call module's engine is mechanical — internally keep the `trustScore` math exactly as researched, then expose `riskScore = 100 - trustScore` as the one public field other modules read. **Do not re-derive the scoring logic — just flip the sign at the boundary.**

### 2.3 The phantom "Login module" — RESOLVED
AI Agent's plan assumes a "Login module" handles onboarding, profile capture, and consent placement (its Q5). **This module does not exist and was never in our 5-module scope.**

**Decision: there is no separate Login module and no real authentication.** This app doesn't need accounts or multi-device sync for the hackathon. Instead:
- **UI module absorbs onboarding** as part of its own scope (this was always implicitly UI's job — first-run flow, permissions walkthrough).
- UI module's onboarding flow captures the `UserProfile` fields AI Agent needs (name, age group, language, tech comfort, primary UPI app, emergency contact) and writes them to the `whis_user_profile` SharedPreferences key exactly as AI Agent's plan already expects — **no change needed on AI Agent's side, this was already designed to degrade gracefully if the writer doesn't exist yet.**
- The single consent screen (see 2.6) lives in this same onboarding flow, immediately after profile capture.

### 2.4 Play Store default-SMS-handler requirement — RESOLVED (the bombshell)
MSG module's research is correct and important: Google Play requires an app using `READ_SMS`/`RECEIVE_SMS` to be the registered default SMS handler, which technically obligates building a full SMS app (compose, thread view) — enormous, deadline-breaking scope if taken literally right now.

**Decision for 31 July: do NOT become the default SMS handler. Do NOT build a full SMS thread/compose UI.**

Reasoning: the hackathon deadline requires a GitHub repo, a demo video, and (per the original handoff doc) does not require live Google Play Store distribution by 31 July. `READ_SMS`/`RECEIVE_SMS` work perfectly fine on a sideloaded APK, an ADB install, or a device used for a demo recording — Play Store's default-handler enforcement only triggers at Play Console review/publish time, not during development, sideloading, or demoing. This unblocks MSG module immediately with zero scope change to their actual detection engine.

**This is explicitly logged as a known future limitation, not hidden:** before this app is ever submitted to the Play Store for real, it needs one of two fixes — (a) build the full default-SMS-handler compliance (major scope), or (b) migrate detection to a lower-fidelity approach that doesn't require SMS broadcast receiver permissions at all. Flag this prominently in the final project report so judges see it was a deliberate, understood tradeoff, not an oversight.

### 2.5 Duplicate contact-lookup logic — RESOLVED
Both Call module (`ContactTrustChecker.java`) and MSG module (Layer 1's contacts lookup) independently designed the same "is this number a saved contact" logic.

**Decision: extract one shared `ContactLookupUtil.java` into `com.whis.app.core`.** Both Call and MSG depend on this single implementation instead of each maintaining their own. Normalization logic (handling `+91XXXXXXXXXX`, `0XXXXXXXXXX`, `XXXXXXXXXX` formats) is written once, tested once, and used by both. Whoever finishes Step 1 of their build first writes this class; the other module imports it rather than rebuilding it.

### 2.6 Consent — RESOLVED
Both Call (DPDP consent for community reporting) and MSG (consent for URL/Safe Browsing checks) and AI Agent (`DataConsentManager`) each proposed their own consent screen.

**Decision: ONE consent screen, owned by UI module, shown once during onboarding (see 2.3).** It covers all three purposes in plain language with a single "I understand and agree" action — not three separate popups the user has to click through. Master owns the shared `WhisConsentManager` in `com.whis.app.core`; Call, MSG, and AI Agent all read `WhisConsentManager.isConsentGiven()` rather than building their own dialogs or SharedPreferences keys.

### 2.7 Firebase — RESOLVED
Both Call (Firestore for community reports) and AI Agent (Firebase AI Logic for Gemini) need Firebase. Both correctly flagged this as blocking and needing one shared project.

**Decision: Master creates ONE Firebase project today**, enables both Firestore and Firebase AI Logic (Gemini) in it, and distributes the single `google-services.json` to all branches. Community reports collection schema (see 3.4) is shared between Call and MSG so they don't create competing collections.

### 2.8 Call module's live in-call overlay — SCOPED DOWN, NOT REJECTED
Call module's research (`LiveCallGuardService`, `LiveCallBadgeView`, the WindowManager overlay badge shown *during* an active call) is genuinely excellent, well-justified engineering — it correctly avoids the OEM battery-kill problems of a persistent overlay by only running while a call is actually active. This is better architecture than the earlier "notification-only" direction I'd designed with Govind before this research existed.

**Decision: keep it, but split it into two phases, and build Phase 1 first:**
- **Phase 1 (build for 31 July):** `WhisCallScreeningService` + offline scoring + Phase 1 notification/`CallWarningActivity` on ring, exactly as researched. This alone is a massive upgrade and fully achievable.
- **Phase 2 (stretch — build only if Phase 1 is solid with days to spare):** `LiveCallGuardService` + `LiveCallBadgeView`, the live on-call badge. This is real, valuable, and already well-researched — but it's also the highest-complexity, highest-testing-burden piece (WindowManager overlay positioning across OEMs, live Firebase badge updates, three-phase lifecycle). Don't let it block Phase 1 from shipping solid.
- `PostCallFeedbackActivity` (the post-call "was this a scam?" bottom sheet) is Phase 1 — it doesn't depend on the live badge existing, only on the call having ended.

---

## 3. THE CANONICAL SHARED CONTRACT — FINAL, LOCKED

### 3.1 `DetectionResult` — the shared interface

Package: `com.whis.app.core`

```java
public interface DetectionResult {
    String getSourceType();        // "CALL" | "SMS"
    int getRiskScore();            // 0-100. HIGHER = MORE dangerous. 0 = confirmed safe.
    WhisVerdict getVerdict();      // see 3.2
    String getReasonText();        // primary human-readable reason, one line
    String getIdentifierType();    // reused generic slot: "CONTACT" | "DLT_REGISTERED" |
                                    // "1600_SERIES" | "UNKNOWN_MOBILE" | etc — one field,
                                    // shared across sources, per the established pattern
                                    // of not proliferating a new field per source
    long getTimestamp();
}
```

`WhisCallAnalysis` (Call module) and `MsgDetectionResult` (MSG module) each implement this interface alongside their own richer internal fields. Internal fields stay exactly as each module researched — only the shared interface surface is standardized.

### 3.2 `WhisVerdict` — the shared enum

Package: `com.whis.app.core`

```java
public enum WhisVerdict {
    TRUSTED,       // Contacts, or fully confirmed safe. Call: no overlay shown at all.
    LIKELY_SAFE,   // Verified series/DLT sender, shown with a reassuring tip.
    UNKNOWN,       // Genuinely no signal either way. No notification surfaced (Call);
                   // MSG should rarely if ever emit this — resolve to a tier if possible.
    SUSPICIOUS,    // Worth a second look. Notification/warning shown.
    HIGH_RISK      // Confirmed or near-confirmed scam. Strongest warning shown.
}
```

Mapping guidance:
- Call module's existing 5-tier scoring (HIGH_RISK/SUSPICIOUS/UNKNOWN/LIKELY_SAFE/TRUSTED) maps 1:1 — no change needed except inverting the score direction per 2.2.
- MSG module's 3-tier (Safe/Suspicious/Scam) maps: Safe → TRUSTED or LIKELY_SAFE (TRUSTED if sender is a contact or fully verified DLT header; LIKELY_SAFE if DLT-verified but not a personal contact), Suspicious → SUSPICIOUS, Scam → HIGH_RISK.
- MSG's separate 6-category content triage (GENERAL/PROMOTION/NOTIFICATION/TRANSACTION/JUNK/ALLOWED) is **not part of the shared contract** — it stays as an MSG-specific supplementary field on `MsgDetectionResult` for MSG's own notification-channel-priority logic. UI module can read it if useful but doesn't need to.

### 3.3 `whis_flags` SharedPreferences — final format (per AI Agent's proposal, confirmed)

Written by: Call module, MSG module
Read by: AI Agent module (`ModuleContextInjector.java`)

```json
[
  {
    "type": "SMS",
    "content": "flagged message text",
    "sender": "VM-SBIBNK",
    "timestamp": 1753048200000,
    "risk": "HIGH"
  },
  {
    "type": "CALL",
    "number": "+919876543210",
    "timestamp": 1753051800000,
    "risk": "HIGH"
  }
]
```

**Mapping from canonical `WhisVerdict` to this simpler `risk` string:** only write to `whis_flags` for `SUSPICIOUS` (→ `"MEDIUM"`) and `HIGH_RISK` (→ `"HIGH"`) verdicts. `TRUSTED`, `LIKELY_SAFE`, and `UNKNOWN` are never written here — the AI Agent doesn't need context injection for calls/messages that weren't concerning. `"CRITICAL"` is reserved exclusively for AI Agent's own in-conversation `RedAlertManager` keyword detection — Call and MSG modules never write that value.

### 3.4 Firestore `community_reports` — shared schema

```
community_reports/{normalizedIdentifier}
  sourceType:          string    // "CALL" or "SMS" — added to Call's original schema
                                  // so one collection serves both modules
  reportCount:         int
  lastReportedAt:       timestamp
  reportedPatterns:     [string]
  confirmedScam:        boolean   // true when reportCount >= 10
  riskScoreDelta:       int       // pre-computed score adjustment (POSITIVE now,
                                  // since canonical direction is higher=worse —
                                  // Call module: invert your original negative
                                  // trustDelta convention here too)

// normalizedIdentifier = E.164 phone number for CALL, or sender header for SMS
// No names, no user IDs, no device IDs stored
// Firestore security rules: authenticated Whis users write; open read
```

### 3.5 `WhisConsentManager` — shared, in `com.whis.app.core`

```java
public class WhisConsentManager {
    public static boolean isConsentGiven(Context context);
    public static void saveConsent(Context context, boolean given);
}
```
One SharedPreferences key (`whis_consent_given`), one screen, shown once during UI module's onboarding. Call, MSG, and AI Agent all read this instead of building their own.

### 3.6 `ContactLookupUtil` — shared, in `com.whis.app.core`

```java
public class ContactLookupUtil {
    public static ContactResult check(String rawNumber, Context context);
    // Handles normalization: +91XXXXXXXXXX, 0XXXXXXXXXX, XXXXXXXXXX
    // Returns isContact (boolean) + contactName (nullable String)
}
```
Built once (whichever of Call/MSG reaches this step first in their build order), imported by the other.

---

## 4. PER-MODULE FINAL SCOPE (condensed — full detail lives in each module's own plan file)

### 📞 Call — Phase 1 (build now)
`WhisCallScreeningService` (CallScreeningService, pre-ring, no persistent process) → offline scoring in <200ms (`NumberIntelligenceEngine` + `ContactLookupUtil` [shared] + `CallBehaviorAnalyzer` against internal SQLite, no `READ_CALL_LOG`) → notification with `CallWarningActivity` on ring for SUSPICIOUS/HIGH_RISK/notable-LIKELY_SAFE → async community check via shared Firestore → `PostCallFeedbackActivity` bottom sheet after call ends. FGS type `specialUse` only if/when Phase 2 overlay is attempted.
**Phase 2 (stretch):** `LiveCallGuardService` + `LiveCallBadgeView` live on-call badge.
**Not doing:** WhatsApp/OTT call screening, real-time audio analysis, auto-blocking, call recording, SIM-swap detection.

### 💬 Message (SMS) — build now
`SmsReceiver` → 5-layer engine: TRAI DLT header check (Layer 1, includes shared `ContactLookupUtil`) → weighted rule engine (Layer 2) → on-device BiLSTM TFLite classifier, 669KB, <50ms (Layer 3) → URL expansion + Safe Browsing v5, consent-gated (Layer 4) → rate-limited Gemini fallback for genuine ambiguity only (Layer 5). Six-category content triage for notification channel priority. Model training pipeline run once by MSG owner in first 2 days of coding.
**Not doing (for 31 July):** default SMS handler / full SMS app UI (see 2.4), crowdsourced reputation v1, MMS/RCS, international scam patterns, active blocking, Chakshu auto-reporting.

### 🤖 AI Agent — build now
Gemini 2.5 Flash-Lite (free tier, primary) via Firebase AI Logic SDK → guided tap-button crisis flow (no typing under stress) → hardcoded-facts system prompt (all phone numbers/legal steps/URLs are hardcoded, never generated by the model, to eliminate hallucination risk) → offline fallback with 30 pre-written scenarios in Hindi/Gujarati/English → API-free instant Red Alert on critical keywords. Reads `whis_user_profile` and `whis_flags` per 2.3/3.3.
**Not doing (Phase 1):** Gemini Live real-time voice, video upload, PDF auto-generation of complaint letters, cross-session memory, on-device Gemini Nano, proactive push (agent only opens with context when tapped, doesn't self-initiate).

### 📚 Learning — plan not yet submitted
Slots in after Call + MSG so it can reference their real `ScamCallPatternLibrary` and rule-set content instead of inventing its own scam taxonomy. Recommend the account building this reads both those files once available.

### 🎨 UI — waiting on this document, starts now
Owns: onboarding (profile capture + the ONE consent screen, per 2.3/2.6), navigation, home dashboard, notification card rendering for both Call and MSG's `DetectionResult` output, design tokens/theming. Needs Call and MSG producing real `DetectionResult` objects to build against — recommend starting with the onboarding flow and design tokens immediately (no dependency), then wiring the dashboard once Call/MSG have working detection to display.

---

## 5. ANSWERS TO EVERY MODULE'S OPEN QUESTIONS

**Call Q1 (Truecaller ROLE_CALL_SCREENING conflict):** Present as opt-in during onboarding. Explain clearly why full pre-ring protection is recommended, but default new users to degraded (post-ring notification) mode if they decline to replace their existing screener. Never force replacement.

**Call Q2 / MSG (community schema coordination):** Resolved in 3.4 — one shared collection, `sourceType` field distinguishes them.

**Call Q3 (1600-series contextual tip, legal sign-off):** Approved. Framing as advisory education, not a spam label, is consistent with TRAI's 10 July 2026 clarification since Whis never tags/blocks/filters the series — only displays contextual information to the user. Proceed.

**Call Q4 (package name):** Resolved in 2.1.

**Call Q5 (score direction):** Resolved in 2.2 — inverted, higher = more dangerous, canonical everywhere.

**Call Q6 / MSG Q4 (DPDP consent shared component):** Resolved in 2.6/3.5.

**AI Agent Q1 (Firebase project, blocking):** Resolved in 2.7 — created today, one project, shared.

**AI Agent Q2 (paid tier decision):** Stay on free tier (Gemini 2.5 Flash-Lite) for the 31 July demo/submission. This is sufficient for judge testing and a small number of demo users. Paid tier / Vertex AI migration is explicitly logged as a post-hackathon roadmap item — note this openly in the final project report as a known, deliberate scaling limitation, not an oversight.

**AI Agent Q3 (whis_flags format):** Confirmed as proposed, final format in 3.3.

**AI Agent Q4 (SEND_SMS optional permission):** Approved as-is, no change. Optional + graceful degradation is the correct design.

**AI Agent Q5 (Login module / consent placement):** Resolved in 2.3 — no Login module exists; UI module's onboarding absorbs this entirely.

**MSG Q1 (default SMS handler, BLOCKER):** Resolved in 2.4 — deferred, not required for 31 July, logged as a known future limitation.

**MSG Q2 (shared DetectionResult contract):** Published in Section 3.1/3.2 — final.

**MSG Q3 (Safe Browsing → Web Risk for commercial deployment):** Acknowledged, no action needed before 31 July, noted as roadmap item.

**MSG Q5 (model training ownership):** Approved as proposed — MSG module owner runs the training pipeline in the first 2 days of the coding phase.

---

## 6. WHAT GOVIND NEEDS TO DO RIGHT NOW

1. Create the single Firebase project (per 2.7) and get `google-services.json` distributed to Call, MSG, and AI Agent branches before any of them can compile Firebase-dependent code.
2. Send Call, MSG, and AI Agent accounts the relevant sections of this document (at minimum: Section 2 in full, and their own module's part of Section 4 and 5) so each one adjusts their build to match the canonical contract before writing their first line of code.
3. Nudge the Learning account to start now if not already — it can begin on generic content/quiz structure without waiting, and slot in real scam-pattern references once Call/MSG exist.
4. Tell the UI account it's cleared to start immediately on onboarding + design tokens (no dependency on other modules), and that the dashboard/notification-rendering work should wait until Call/MSG have real `DetectionResult` output to build against.
5. Confirm with all module owners: package name is `com.whis.app.*` (Section 2.1) — this is the one change every single account needs to make regardless of anything else.

---

*Compiled from CALL_PROTECTION_PLAN.md, MSG_PLAN.md, and AI_AGENT_PLAN.md. Learning module plan pending. This document supersedes any conflicting detail in the individual module plans — those remain valid for everything not explicitly overridden above.*
