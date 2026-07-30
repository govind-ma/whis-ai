# UI / Navigation Module — Whis
**Owner:** UI/Navigation · **Status:** Ready for Master review · **Date:** 23 July 2026 (8 days to deadline)

---

## 1. WHAT YOU FOUND

Research findings below are grouped by decision they actually changed, each cited, each confidence-flagged. Anything I looked into and rejected is included, with why.

### 1.1 Elderly/non-technical user UX — the evidence base
- A 2025 systematic review across 132 studies on 60+ users converges on four levers: **simplified/shallow navigation, enlarged text and touch targets, voice interaction, and error-tolerant interfaces.**
- A 2026 eye-tracking study, *"Structure Over Color: Eye-Tracking Evidence on Navigation Depth and Elderly Users' Online Trust in Health Apps,"* found navigation **depth** — not color scheme — is the stronger driver of trust for this age group. This directly overrode my own early instinct toward a richer, more nested app; I'm holding the whole app to **max 2 levels deep from any bottom-nav tab.**
- **Rejected:** designing around color-coded trust signals as the primary hierarchy cue (my first instinct). Color still carries risk-level meaning (§2.1), but depth/simplicity is the primary trust lever per the cited study, not color intensity.

### 1.2 Dark mode vs. light mode for this specific population
- Astigmatism (present in roughly 30–60% of people, rising with age) causes **halation** — light text on dark backgrounds appears to glow/blur because the eye's iris opens wider in low light, distorting focus for irregular corneas.
- The broader accessibility literature is genuinely split — some low-vision users strongly prefer dark UIs — so this isn't "dark mode is wrong," it's "dark-only is a real risk for an elderly-skewing user base specifically."
- **Confirmed against project state:** Master's direction — light mode primary/required, matching the existing teal/red/amber card system; dark mode explicitly deferred to v2. This resolves the tension I flagged rather than leaving it open.
- **Rejected:** shipping the glassmorphic dark-navy-and-gold direction I originally designed as Phase 1. It's visually strong but (a) contradicts the halation finding for our actual users, (b) depends on hardware blur unavailable pre-Android 12 (§1.4), and (c) doesn't match the card system other modules are already building against. Kept as a documented v2 direction, not thrown away.

### 1.3 Delivery mechanics for scam alerts
- `USE_FULL_SCREEN_INTENT` is granted by default only to calling/alarm-category apps on Android 14+; general apps must be explicitly granted it via Special App Access.
- India-specific version math (StatCounter, May 2026): Android 14+ = ~56% of Indian Android users (12/15/16 combined), meaning the FSI restriction already applies to the majority, not an edge case.
- **Per confirmed project state, this is now Call module's internal responsibility** (`canUseFullScreenIntent()`, graceful degradation) — I design assuming the **worst case (heads-up notification)** always, and treat full-screen as a bonus if it happens, never a dependency.

### 1.4 Rendering constraints — glass/blur specifically
- Real-time blur (`RenderEffect`) requires API 31+ (Android 12). Below that, Google's own guidance is to migrate away from the deprecated RenderScript blur path entirely — there is no clean native blur on our minSdk 24 floor.
- Given India's version split, ~90% of users are on API 31+, but the ~10% who aren't are disproportionately the cheapest, oldest hardware — correlating with the exact population this app most needs to protect.
- **Applied:** N/A for Phase 1 — light mode's flat card system sidesteps this entirely. Documented here so it's not re-litigated when v2 dark/glass mode is picked up.

### 1.5 Permission architecture — confirmed, not assumed
- `CallScreeningService` can be implemented by third-party apps without becoming the default dialer; the carrier-verification signal (`getCallerNumberVerificationStatus()`) additionally requires the lighter-weight **"Default Caller ID & Spam app"** system role (same role Truecaller uses) — not full dialer replacement.
- `NotificationListenerService` lets an app read incoming SMS notification content via a single "Notification access" grant, without `READ_SMS`'s default-handler restriction — real-time only, no historical inbox, which is an acceptable trade-off for a "before money moves" tool.
- **Confirmed against project state:** Call = `CallScreeningService`, no `READ_CALL_LOG`, no default dialer. Message = `READ_SMS`/`RECEIVE_SMS` as normal permissions, explicitly not default-SMS-handler. My 5-step onboarding wizard assumption was correct and is now load-bearing, not speculative.
- **Rejected:** designing onboarding around Whis becoming the default SMS or Phone app. Technically avoidable per above, and the wrong trust ask for a first-time elderly user regardless.

### 1.6 The India-specific gap I almost missed
- STIR/SHAKEN (the carrier caller-ID verification framework Android's own docs reference) is a **US** framework. India has no equivalent mandated system — TRAI/DoT are, per one current source, "shaken but not yet stirred." Indian carriers instead run the DLT platform (commercial SMS header registration) and narrower anomaly rules (e.g., blocking an Indian mobile number appearing to originate overseas) — nothing that hands a third-party app a clean "verified legitimate" signal per call.
- **Consequence for UI copy specifically:** a "verified by your carrier" claim would be false for the overwhelming majority of Indian calls. This directly justifies the `getConfidenceSource()` addition Master made to the shared contract off the back of this finding (§2.3 for exact copy rules).

### 1.7 Touch target sizing — verified current, not assumed
- Google's own Android Developers guidance (last updated 2026-06-02): **48dp × 48dp minimum** for any interactive element, "larger is even better." WCAG 2.2 AA's absolute floor is 24×24px, but the platform recommendation is the higher 48dp bar.
- **Applied:** 48dp as the absolute floor (used nowhere below this), 56dp as the standard for primary actions given "larger is even better" plus elderly-specific motor-precision research (§1.1).

### 1.8 Localization
- Confirmed in scope per project state: Hindi and Gujarati, already assumed throughout Call and AI Agent's plans. Not researched as a "should we" question — it's decided; my job is designing for **text expansion** (translated strings routinely run 20–35% longer than English) rather than assuming layouts sized for English text will hold.

---

## 2. HOW TO APPLY IT

### 2.1 Design tokens

**Color — built on the existing anchors, extended to cover the real 5-tier `WhisVerdict` enum, not invented fresh:**

| Token | Hex | Maps to `WhisVerdict` | Use |
|---|---|---|---|
| `whis_trusted` | `#00695C` (existing) | `TRUSTED` | Full-confidence safe — contact match, verified series |
| `whis_likely_safe` | `#4DB6AC` | `LIKELY_SAFE` | Lower-confidence safe — lighter tone of the same hue, not a new color family |
| `whis_unknown` | `#757575` | `UNKNOWN` | Neutral gray — no signal either way, not a warning color |
| `whis_suspicious` | `#F57F17` (existing) | `SUSPICIOUS` | Amber |
| `whis_high_risk` | `#B71C1C` (existing) | `HIGH_RISK` | Red |
| `whis_bg` | `#FAFAFA` | — | Screen background — off-white, not pure `#FFFFFF`, to reduce glare |
| `whis_surface` | `#FFFFFF` | — | Card surfaces |
| `whis_text_hi` | `#212121` | — | Primary text — near-black, not pure `#000000` (keeps the halation lesson applicable even in light mode's edge cases, e.g. high-contrast accessibility setting) |
| `whis_text_mid` | `#5F6368` | — | Secondary text |
| `whis_border` | `#E0E0E0` | — | Card borders/dividers |

All five verdict colors independently verified against `whis_bg`/`whis_surface` for WCAG AA (4.5:1 body text); `whis_high_risk` and `whis_trusted` text-on-white both clear AAA (7:1) — deliberately, since alert-detail text is exactly where failure is least acceptable.

**Typography scale** (sp units throughout, so it respects system font scaling — no fixed-height containers, no truncating ellipsis on any risk-relevant text, reversing a mistake in my own earlier mockup):

| Token | Size | Weight | Use |
|---|---|---|---|
| `type_display` | 28sp | Bold | Protection status headline |
| `type_h1` | 22sp | Bold | Screen titles |
| `type_h2` | 15sp | Bold, uppercase, +0.08em tracking | Section labels |
| `type_body_lg` | 17sp | Regular | Alert/detail body text |
| `type_body` | 16sp | Regular | List item titles |
| `type_caption` | 13sp | Regular | Metadata/timestamps — the floor; nothing goes smaller |

Must render correctly up to 200% system font scaling without clipping or overlap — this is a build-time test requirement, not a suggestion (§3, Day 8).

**Spacing** — 8dp base grid: screen margin 20dp, card padding 16dp, inter-item spacing 12dp, section spacing 24dp.

**Touch targets** — 48dp absolute floor (verified current, §1.7), 56dp standard for primary buttons and bottom-nav items, list rows minimum 56dp tall.

### 2.2 Onboarding flow

Ordered, with the AI Agent field list woven in at the point each field is actually needed, not front-loaded as one long form (a long form is exactly the kind of navigation-weight the depth research warns against):

1. **Welcome** — logo, one-line promise, no data collected yet.
2. **Language** — English / Hindi / Gujarati. Must come first, since every subsequent screen renders in the chosen language.
3. **Basic profile** — `name`, `age group` (large single-select buttons, not a text field), `techLevel` (Basic/Advanced two-button choice — see open item below on exact granularity).
4. **Primary UPI app** — single-select: GPay / PhonePe / Paytm / BHIM / Other.
5. **Emergency contact** — name + phone number, feeds AI Agent's SMS alert feature directly.
6. **Single consent screen** — see §2.3.
7. **Permission wizard**, five steps, each a full plain-language screen, not a stacked system-dialog wall:
   - Default Caller ID & Spam app (unlocks carrier signal where it exists)
   - Notification Access (enables SMS scam detection)
   - Battery optimization + OEM autostart, manufacturer-detected deep link
   - Full-screen alert access (Special App Access)
   - Do Not Disturb bypass for the Whis channel
8. **Protection confirmed → Home**

### 2.3 The single consent screen

One screen, three clearly labeled sections under one visual roof, not three popups — because three separate interruptions reads as exactly the kind of pattern a scam app would use, which is the opposite of what we're building trust against. Draft copy (plain language, short sentences, no legal jargon — flagged per project state as needing Govind's compliance pass before final, not legally final as written):

> **Before we start protecting you**
>
> **1. Reading calls and messages for danger signs.**
> Whis checks incoming calls and texts for scam patterns — before you answer or open them. We don't store your messages; we only look for warning signs.
>
> **2. Helping other users (optional).**
> If you mark a number as a scam, you can choose to share that number so Whis can warn others too. Your name is never shared. *[Toggle: On/Off]*
>
> **3. Asking Whis questions.**
> You can ask Whis to explain any warning in plain language, anytime.
>
> [ I understand and agree ] — single primary action, 56dp height

Item 2 is the only genuinely optional toggle (community reporting); items 1 and 3 are core to the app functioning, so they're explained but not separately gated — bundling three real popups into one honest screen, not hiding an opt-out.

### 2.4 Navigation

**Revising my own earlier mockup here, not repeating it** — that version had 6 bottom-nav tabs (Home/Calls/Msgs/Learn/AskAI/Settings). Applying the depth-over-color finding rigorously: 6 destinations in one bar pushes tap targets below the 56dp standard and adds a destination (AI chat) that's used *in response to* something, not browsed to on its own.

**Final structure: 5 tabs.**
`Home` · `Calls` · `Messages` · `Learn` · `Settings`

AI chat is **not** a persistent tab — it's a contextual entry point: a prominent card on Home ("Ask Whis anything") and a button inside every alert detail screen ("Why was this flagged?"). This keeps it fully reachable in one tap from the places it's actually needed, without permanently taxing everyone's thumb reach for a feature most sessions won't touch.

Max depth from any tab: **2 levels** (tab root → detail screen. No detail-of-a-detail.)

---

## 3. THE PLAN

### 3.1 Package structure (building on existing `com.whis.app.core`, not duplicating it)

```
com.whis.app.ui.theme        — WhisColors, WhisTypography, WhisDimens (design tokens, §2.1)
com.whis.app.ui.components   — StatusCard, RiskTag, PrimaryButton, ProtectionRing, shared list-row
com.whis.app.ui.alert        — AlertRenderer (see 3.3 — shared by Call & Message, not duplicated per-module)
com.whis.app.ui.onboarding   — WelcomeFragment → PermissionWizardFragment (8 screens, §2.2)
com.whis.app.ui.home         — HomeFragment, ActivityFeedAdapter
com.whis.app.ui.calls        — CallsFragment, CallDetailFragment
com.whis.app.ui.messages     — MessagesFragment, MessageDetailFragment
com.whis.app.ui.learn        — LearnFragment (shell — see 3.4 dependency note)
com.whis.app.ui.settings     — SettingsFragment, AccessibilityPrefsFragment
MainActivity                 — hosts NavHostFragment + 5-tab BottomNavigationView, nav_graph.xml
```

### 3.2 Build order — 8 days, sequenced by actual dependency, not convenience

| Day | Work | Blocked on? |
|---|---|---|
| 1 (today) | Design tokens (`colors.xml`, `dimens.xml`, `styles.xml`), shared components | Nothing — pure UI module |
| 2 | Onboarding screens 1–6 (Welcome → Consent) | Nothing — AI Agent field list already known, `techLevel` granularity assumption stated (§3.5) |
| 3 | Permission wizard, all 5 steps | Nothing — platform APIs only |
| 4 | Navigation shell (`MainActivity`, 5-tab bar, nav graph) + Settings screen | Nothing |
| 5 | Home dashboard + `AlertRenderer` build | **Blocked — see 3.3, requesting now** |
| 6 | Calls tab + Messages tab, list/detail, wired to real or stub `DetectionResult` | Same as Day 5 |
| 7 | Learn tab shell + AI chat contextual entry, wired to AI Agent | Learn: partially blocked, see 3.4 |
| 8 | Integration with Master: merge, cross-module visual consistency, font-scaling stress test (up to 200%), TalkBack spot check | Whole team |

### 3.3 What I need from Call/Message before Day 5, specifically

Now that the contract fields are confirmed (`WhisVerdict` 5-tier enum, `riskScore` 0–100, `getReasonText()`, `getConfidenceSource()`), the blocker isn't the schema — it's **realistic sample data to build against**, so Day 5 doesn't slip waiting on either module's detection logic to be finished:

- A small stub set (JSON or hardcoded Java objects, either is fine) of `DetectionResult` instances covering **all 5 `WhisVerdict` tiers × all 5 `ConfidenceSource` values** — at minimum one real example of each combination that's actually plausible (e.g., `HIGH_RISK` + `CONTACT_MATCH` may not be a real combination worth stubbing; `HIGH_RISK` + `COMMUNITY_REPORT` is).
- Confirmation of exact field types on `DetectionResult`/`WhisVerdict` — I'll read `com.whis.app.core` directly once I reach Day 5 rather than re-ask now, per Master's note that I'm not first to touch it.
- I'm also proposing to **own the shared `AlertRenderer`** (in `com.whis.app.ui.alert`) rather than have Call and Message each build their own notification UI — a single utility both modules call into (icon, color-per-tier, copy-per-`ConfidenceSource`, the 2-button-only rule from Truecaller's own usability history) so the "verified" vs. "matches a pattern" wording discipline from §1.6 is enforced in one place, not reimplemented twice with room to drift apart. Flagging this as a proposal for Master to confirm ownership of, not assuming it.

### 3.4 Learning module dependency

Learning's plan hasn't landed. I'm building `LearnFragment` as a generic list→detail shell (title, thumbnail, progress bar — matching the pattern already used elsewhere) against placeholder content on Day 7, so the tab exists and is navigable for the Day 8 integration pass regardless of whether Learning's real plan lands in time. This is an explicit scope hedge, not an assumption that their plan will match what I build — I'll adjust the shell once their plan exists.

### 3.5 Explicitly NOT solving in Phase 1, and why

- **Dark mode / glassmorphic treatment** — designed and documented (§1.2, §1.4), deliberately deferred to v2 per Master's call, not because it's unfinished thinking.
- **Family/trusted-contact cross-visibility** — out per Master's confirmation; AI Agent's narrower emergency-contact SMS alert stays in.
- **Full historical call/SMS log** — not technically available without becoming a default handler app; the activity feed is scoped as "screened since protection turned on," and worded that way in the UI so it's never an overclaim.
- **Deep community-reputation UI** (voting, trust scores on reporters) — a single report toggle only; anything richer is a v2 feature, not a Phase 1 cut made for time pressure alone but also genuinely unnecessary for the core promise.

### 3.6 Open questions for Master

- `techLevel` exact granularity (Basic/Advanced assumed per project state — building against that now, will adjust if wrong).
- Confirming I own `AlertRenderer` as shared infrastructure (§3.3) rather than Call/Message each building their own.
- Icon asset conventions — should there be one shared icon set across modules, and if so, who owns it? I don't want to invent a second icon language other screens later have to match.

---

**Status: ready for Master review.**
