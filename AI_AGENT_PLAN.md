# AI_AGENT_PLAN.md
**Module:** AI Chat Agent
**App:** Whis — Android Fraud Protection
**Language:** Java | minSdk 24 | targetSdk 36
**Deadline:** 31 July 2026
**Written:** 21 July 2026 — all time-sensitive facts reverified this date

---

## 1. WHAT THIS MODULE DOES

The AI Agent is the part of Whis that every user — student, professional, elderly
person, business owner, housewife — talks to directly when something goes wrong.
When anyone receives a suspicious SMS, gets a scary call from a "bank officer,"
loses money through UPI, or simply does not know if something is a scam, they open
this screen.

The agent does not just answer questions. It acts as a complete case officer:

First, it calms the user down with genuine human language — no robotic "I am sorry
to hear that." Then it asks smart guided questions using tap buttons so the user
does not have to type under stress. Based on answers, it identifies the exact fraud
type. Then it walks the user step by step — from the first 60 seconds of action
all the way through 1930 complaint, bank dispute, cybercrime portal filing, and if
needed, RBI Ombudsman escalation — until the case is closed or the money is back.

This is not a chatbot. Every response is specific to what actually happened to this
specific user, in their language, at their level of technical comfort.

---

## 2. WHAT I FOUND — RESEARCH FINDINGS (Verified 21 July 2026)

### 2.1 The Fraud Landscape — Current India 2026

Source: cybercrime.gov.in NCRP data, RBI reports, upiapps.in (3 days ago),
nahar.om fraud trends (March 2026), legalsuvidha.com (November 2025)

Confidence: HIGH on patterns, HIGH on scale numbers

Indians lost over ₹22,845 crore to cyber fraud in 2024, a 206% increase from 2023.
Cybercrime complaints grew from 10.29 lakh (2022) to 22.68 lakh (2024). India is
on track to cross 25 lakh cases in 2026. Only 1 in 10 victims actually reports.

Top fraud patterns by volume (2026):

**Digital Payment and UPI Frauds — 35% of cases:**
- Fake UPI collect request: Fraudster sends a "collect" (debit) request framed as
  a refund or prize. User taps "Pay" thinking it means "Receive." One tap, money
  gone. Most common UPI fraud right now.
- QR code swap: Merchant QR replaced with fraudster QR at point of sale.
- Fake payment success screenshots: AI-generated screenshots fool merchants.
- Screen sharing fraud: Poses as tech support, installs AnyDesk/TeamViewer, watches
  user enter UPI PIN.

**Digital Arrest Scam — 28% of cases:**
Fraudsters pose as CBI/ED/police/TRAI officers via video call. Claim victim is
under "digital arrest" for money laundering, drug trafficking, or SIM misuse.
Demand immediate payment to "resolve" the case. Highly effective because victim
fears law enforcement. Now the single fastest-growing fraud category.

**Investment and Trading Scams — 18% of cases:**
Fake crypto/stock platforms. Initial small payouts build trust. Large deposits
requested, then trapped. Losses per victim are highest in this category.

**Vishing with Data Validation:**
Caller already knows account number and last transaction (bought from data leaks),
so call feels real. Creates urgency: "Account band ho jaayega 2 ghante mein."

**OTP Scam:**
Still extremely common. Classic version, plus variant: "We are sending OTP to
verify your identity" — they are actually initiating a transaction.

**Fake KYC / Phishing:**
SMS or WhatsApp saying KYC expired. Link goes to fake bank page or installs
malware.

**Deepfake Voice and Video Calls:**
AI-cloned voices mimicking bank officials or family members. Growing rapidly in
2026. Agent must flag this pattern explicitly.

**SIM Swap:**
Fraudster gets duplicate SIM from carrier. All OTPs redirected. Full account
takeover possible.

**Money Mule Trap:**
Sends user ₹500 "by mistake." Asks for refund to different account. Original
payment was from stolen account. User becomes unwitting money launderer.

**Fake Job / WFH Task Scam:**
Small initial payout builds trust. Large deposit required, then disappears.
Targets students and unemployed people.

---

### 2.2 RBI Recovery Framework — Verified 21 July 2026

Source: RBI Circular 6 July 2017 (RBI/2017-18/15 DBR.No.Leg.BC.78/09.07.005/2017-18),
advmriduljindal.com (28 June 2026), lexology.com (RBI June 24 2026 directions),
shareindia.com (March 2026), sacharlawfirm.in (December 2025)

Confidence: HIGH — multiple sources cross-checked, recent Supreme Court judgment
confirms enforceability (SBI v. Pallabh Bhowmick, January 2025)

**Current framework (governs transactions until December 31, 2026):**

RBI Circular 6 July 2017 establishes three tiers:

- **Zero liability** when: fraud is due to bank negligence OR third-party breach
  AND customer reports within 3 working days. Full refund, 10 working days.
- **Limited liability** when: reported within 4-7 days. Customer bears ₹5,000 to
  ₹25,000 depending on account type. Refund of remainder within 10 working days.
- **Bank discretion** when: reported after 7 days. Recovery not guaranteed.

**Critical nuance verified:** "Once you report, your liability for ALL losses
occurring after that reporting moment is zero — reporting cuts off exposure
immediately." (advmriduljindal.com, June 2026, citing Supreme Court order)

**Important new development:** RBI issued Third Amendment Directions on 24 June
2026 revising the customer protection framework. These apply to transactions
from January 1, 2027 onward. For all transactions in 2026, the 2017 circular
still governs. Agent must not mix these up.

**Agent's critical first job after calming user:** Calculate which window they are
in. 3 days, 7 days, or past 7. This determines urgency and what to promise.

---

### 2.3 Recovery Helpline 1930 — Verified 21 July 2026

Source: naksh.org complete guide (May 2026), righttoinformation.wiki (May 2026),
nahar.om complaint guide (May 2026), upiapps.in (3 days ago)

Confidence: HIGH

- Operated by I4C (Indian Cyber Crime Coordination Centre) under Ministry of
  Home Affairs. Active 24/7. Free to call.
- Old number 155260 was replaced by 1930. Use 1930 only.
- Process: Victim calls → operator takes transaction details → flags transaction
  with receiving bank under RBI Limited Liability Circular → attempts freeze on
  recipient account before money moves further through mule chain.
- First 60 minutes is the "Golden Hour." First 6 hours significantly improve
  recovery. Each hour of delay allows fraudsters to route funds through more mule
  accounts making reversal harder.

**What user must have ready before calling 1930:**
- Exact amount
- Bank name
- Transaction ID / UTR number (12 digits — in the SMS they received)
- UPI ID or phone number money went to
- Exact time of transaction
- Type of fraud (UPI/call/SMS)

**After 1930:** Operator gives a complaint reference number. This number must be
quoted in all subsequent bank and cybercrime complaints.

---

### 2.4 Cybercrime Portal — Verified 21 July 2026

Source: naksh.org (May 2026), nahar.om (May 2026), righttoinformation.wiki
(May 2026), scribd.com portal manual

Confidence: HIGH

URL: cybercrime.gov.in — National Cyber Crime Reporting Portal (NCRP)
Operated by MHA under I4C. Available 24x7. Mobile friendly. Multilingual.

**Exact filing process:**
1. Go to cybercrime.gov.in
2. Choose language preference
3. Click "File a Complaint"
4. For financial fraud: select "Report Financial Fraud"
5. Login with mobile OTP (mandatory — anonymous not available for financial fraud)
6. Fill four forms: incident details, suspect details, complainant details, evidence
7. Upload: bank SMS screenshots, transaction screenshots, call logs, chat logs
8. Submit → system generates acknowledgment number (format: 2026XXXXXXXXX)
9. Save acknowledgment number — this is the tracking reference for everything

**What agent prepares user with before filing:**
- All transaction IDs
- Fraudster's UPI ID / phone number
- Time and date of incident
- 1930 complaint reference number (file this first)
- All screenshots already taken

---

### 2.5 Bank Helpline Numbers — Verified 21 July 2026

Source: righttoinformation.wiki (May 2026 — most complete current source),
cross-checked against bank websites where possible

Note: HDFC helpline had two different numbers in different sources (1800-120-1243
and 1800-258-6161). Both appear in verified 2026 sources. Agent will show both.

```
SBI:      1800-11-2211
HDFC:     1800-258-6161 / 1800-120-1243 (both active per 2026 sources)
ICICI:    1800-1080
Axis:     1800-103-5577 / 1800-419-5959 (both found in sources)
Kotak:    1800-209-0000
Yes Bank: 1800-1200
PNB:      1800-180-2222
BOB:      1800-5700
Canara:   1800-425-0018
Union:    1800-222-244
IDFC:     1800-419-4332
Default:  "Apne debit card ke peeche helpline number dekho"
```

Agent gets user's bank from profile (set at login). Immediately shows correct
helpline number when needed. Never asks user to search for it.

---

### 2.6 Legal Escalation Path — Verified

Source: legalsuvidha.com (November 2025), righttoinformation.wiki (May 2026),
lexology.com (RBI directions July 2026)

**RBI Ombudsman (cms.rbi.org.in):**
When to go: Bank does not reply within 30 days OR reply is unsatisfactory.
File within 90 days of bank's final response.
Compensation available: financial loss AND mental harassment damages.
Note: Specific compensation caps for RB-IOS 2026 (July 2026 scheme) could not
be fully verified from public sources — agent will say "significant compensation
available" rather than quote a specific rupee figure to avoid hallucination.

**Consumer Commission:** Bank negligence → damages for mental agony also claimable.
District commissions handle cases up to ₹50 lakh.

**FIR (Police):** BNS 2023 Sections 318 (cheating), 319 (forgery), 316 (criminal
breach of trust). Also IT Act 2000 Sections 66, 66C, 66D. Cybercrime has no
jurisdiction limits — file from anywhere.

**NPCI Dispute Redressal:** For UPI specifically — open UPI app → Help → Report
fraud → enter transaction details. This runs parallel to bank complaint, not
instead of it.

---

### 2.7 Transaction ID Location — Verified

Source: upiapps.in (3 days ago), multiple UPI app support pages

- **Google Pay:** GPay app → History tab → Tap transaction → Transaction ID shown
- **PhonePe:** PhonePe app → My Transactions → Tap transaction → 12-digit UPI ID
- **Paytm:** Paytm app → Passbook → UPI tab → Tap transaction → Transaction ID
- **Bank SMS:** Every UPI transaction triggers an SMS from the bank containing
  UTR number — 12 digits. This is the most reliable source.
- **Net banking:** Login → Statement → Find transaction → Reference/UTR number

Agent walks user through this step by step based on which UPI app they use
(stored in profile from login).

---

### 2.8 Gemini 2.5 Flash — Verified 21 July 2026

Source: firebase.google.com/docs/ai-logic (6 days ago), developer.android.com
(July 20 2026), rapidevelopers.com (2 weeks ago), aifreeapi.com (March 2026),
pecollective.com (May 2026), tokenmix.ai (April 2026)

**CRITICAL CORRECTION FROM EARLIER RESEARCH:**

Gemini 2.0 Flash was shut down June 1, 2026. All references to 2.0 Flash in
earlier notes are invalid. Current model is Gemini 2.5 Flash.

**Free tier limits — CORRECTED (multiple sources, March-July 2026):**

Sources conflict slightly. Best current picture:
- Gemini 2.5 Flash free tier: approximately 10 RPM, ~250 requests/day (March 2026
  data from aifreeapi.com). Some sources still cite 1,500 RPD but this appears to
  be outdated — December 2025 cuts reduced limits significantly.
- Gemini 2.5 Flash-Lite: 15 RPM, 1,000 requests/day — better free tier option.
- FLAGGED UNCERTAIN: Exact current RPD for 2.5 Flash is inconsistent across
  sources (250 vs 1,500). Actual limits visible in AI Studio console per-project.
  Developer must verify at console.firebase.google.com before launch.

**IMPORTANT ARCHITECTURE CHANGE from this finding:**
At 250 requests/day free tier, a small group of active users (say 20 people each
having 3 conversations of 4 exchanges) = 240 requests/day. Entire free tier consumed
by 20 users in one day. This makes the free tier suitable for internal testing only,
not for any real user base. Paid tier or Gemini Flash-Lite needed even for small
MVP launch.

Recommendation changed: Use Gemini 2.5 Flash-Lite for primary model (better free
tier), fall back to full 2.5 Flash for complex multimodal analysis (images, audio).

**SDK — VERIFIED:**
Firebase AI Logic SDK — official Android Java/Kotlin SDK. Confirmed active and
updated as of July 20, 2026 (developer.android.com last updated 2026-07-20).
No custom backend needed. Calls Gemini API directly from Android app.
Firebase App Check automatically enforced starting early July 2026 — required
for production. Debug provider needed for local development.

**Multimodal — VERIFIED:**
Text, images, PDFs, audio, video — all supported in one API call via Firebase AI
Logic SDK. (firebase.google.com/products/firebase-ai-logic)

**Language support — VERIFIED:**
Hindi and Gujarati confirmed. Hinglish code-switching works naturally.

**Privacy on free tier — VERIFIED:**
Google may use free-tier prompts for model training. Paid tier and Vertex AI do
not have this issue. Consent screen mandatory before any API call.

---

### 2.9 Hallucination Risk — Critical Finding

Source: Multiple LLM evaluation studies, financial AI benchmarks

Confidence: HIGH that the risk exists. Exact percentage figures vary by study
and task — treating as "significant risk" rather than citing a specific number.

LLMs including Gemini can generate plausible-sounding but wrong information when
asked about specific phone numbers, legal sections, and financial procedures.
In a fraud protection app, a wrong bank helpline number or wrong legal step could
cause real harm to a real user who is already in crisis.

**Mitigation strategy (hardcoded facts approach):**
ALL specific data in agent responses — phone numbers, portal URLs, legal sections,
RBI circular references, complaint steps — is hardcoded in the system prompt and
in local JSON assets. Gemini is only responsible for situation analysis, empathy,
follow-up questions, and explanation in plain language. It never generates specific
facts from its parametric memory. It reads facts from what we supply it.

This is the single most important architectural decision for safety.

---

### 2.10 What I Looked Into But Did Not Use

**Claude API (Anthropic):**
Better reasoning quality, better at following complex instruction formats. Rejected
because: no official Android Java SDK (would need manual REST implementation adding
risk before July 31 deadline), no native audio support in one call, higher cost
than Gemini Flash-Lite. Could be reconsidered for Phase 2 if quality matters more
than cost and deadline.

**OpenAI GPT-4o mini:**
Good multimodal support. Rejected: no official Android SDK, less India-specific
language tuning than Gemini, costs similar to Gemini paid tier without the India
advantages.

**Gemini Nano (on-device):**
Complete privacy, works offline forever, zero cost. Rejected: experimental as of
July 2026, 4,000 token context limit (our system prompt alone may approach this),
Hindi/Gujarati quality unconfirmed on-device, cannot handle images without cloud.
Phase 2 candidate if quality improves.

**Gemini Live API (real-time voice):**
True bidirectional voice conversation — user speaks, agent speaks back in real time.
Available on Android via Firebase AI Logic as of July 2026. Rejected for Phase 1:
WebSocket streaming architecture requires significantly more implementation work
than chat-based approach. Risk to July 31 deadline is too high. Phase 2.

**Whisper API for voice transcription:**
Better transcription accuracy than Android SpeechRecognizer. Rejected: adds cost
($0.006/minute), adds another API dependency, adds latency for no compelling gain
when built-in recognition is free and sufficient for our use case.

**RAG (Retrieval Augmented Generation) with local vector DB:**
More sophisticated knowledge retrieval, scales better with large knowledge base.
Rejected: SQLite-VSS or FAISS setup adds significant complexity. Our fraud knowledge
fits comfortably in a system prompt for Phase 1. Phase 2 if knowledge base grows.

---

## 3. HOW TO APPLY IT — WHAT WHIS ACTUALLY BUILDS

### 3.1 The Guided Crisis Flow (Core UX Innovation)

The key insight from research: scam victims are panicked and cannot type clearly
or remember what to do. Google gives them 10 blue links. We give them a guided
conversation that requires almost no typing.

**Every crisis conversation follows this structure:**

```
TRIGGER: User opens agent or taps notification

PHASE 1 — Calm down (2-3 sentences, warm language)
"Main hoon. Ghabrao mat. Sab kuch step by step karte hain."

PHASE 2 — Guided questions with TAP BUTTONS (no typing)

Turn 1: "Kya hua?"
[PAISA GAYA] [FRAUD CALL AAYA] [FAKE SMS/LINK MILA] [PATA NAHI]

Turn 2 (if PAISA GAYA): "Kaise gaya?"
[OTP DIYA] [LINK DABA] [UPI REQUEST APPROVE KI]
[QR CODE SCAN KIYA] [KHUD TRANSFER KIYA] [KUCH AUR]

Turn 3: "Kab hua?"
[ABHI ABHI (1 ghante mein)] [AAJ] [KAL] [KAL SE PEHLE]

Turn 4: "Kaunsa app tha?"
[GOOGLE PAY] [PHONEPE] [PAYTM] [BANK APP] [PATA NAHI]

PHASE 3 — Agent identifies exact fraud type, calculates RBI window
"Ye UPI Collect Request fraud hai. Aapko 2 din 16 ghante hain
zero liability ke liye. Abhi immediately ye karo:"

PHASE 4 — Action steps (each with its own sub-flow button)
[TRANSACTION ID DHUNDNE MEIN HELP] [1930 PE CALL KAISE KAREIN]
[BANK KO KAISE REPORT KAREIN] [CYBERCRIME PORTAL COMPLAINT]

PHASE 5 — Case officer mode
Agent tracks what has been done. Follow-up reminders. Escalation
when 30 days pass without bank response. Case stays open until resolved.
```

### 3.2 What Makes This Better Than Google

A user Googling "UPI fraud complaint" gets a mix of news articles, old blog posts,
and the cybercrime portal homepage. They have to figure out what applies to them.

Our agent:
- Already knows which bank they use (from profile)
- Already knows which UPI app they used (from profile or Turn 4 answer)
- Already knows a suspicious call came 20 minutes ago (from Call module flags)
- Connects the dots: "Whis ne 20 minute pehle ek suspicious call detect kiya tha
  — kya wahi call tha?"
- Gives them the exact steps for their specific situation, not a generic guide

### 3.3 User Profile Drives Everything

At login (Login module), user provides:
- Name, age group, occupation, preferred language, tech comfort, primary UPI app,
  emergency contact (optional)

Agent uses this to adapt every response:
- 65-year-old, Basic tech, Gujarati → short sentences, no English terms, very
  slow step by step, everything spelled out
- 24-year-old, Student, Advanced, English → fast, technical, skip the obvious
- Business owner → GST fraud awareness, formal language, legal steps foregrounded

### 3.4 Offline Fallback Is Not an Edge Case

Given that free tier is now only ~250 req/day (insufficient for even small MVP),
and given that users may lose connectivity during a crisis:

Offline mode is a primary feature, not a backup.

Pre-built expert responses for 30 fraud scenarios are written, reviewed, and
translated into Hindi and Gujarati before launch. When offline, agent immediately
responds with the right scenario response. For emergency situations offline, agent
shows the hardcoded action steps and 1930 number without any API dependency.

### 3.5 Emergency Red Alert — API-Free

When CRITICAL keywords detected (active call, money moving right now, scammer
on line), agent does NOT make an API call. Speed matters more than intelligence
here. Full-screen Red Alert launches immediately with:
- "GHABRAO MAT" in large text
- One-tap 1930 call button
- One-tap bank helpline button (from user profile — correct bank shown)
- SMS to emergency contact (if configured)

No spinner. No API wait. Instant.

---

## 4. THE PLAN — CONCRETE BUILD STEPS

### 4.1 File Structure

```
app/src/main/java/com/whis/agent/

  AgentActivity.java
  AgentViewModel.java

  api/
    GeminiAgentClient.java
    AgentRequest.java
    AgentResponse.java

  context/
    UserProfileContext.java
    ModuleContextInjector.java
    SessionContext.java

  prompt/
    SystemPromptBuilder.java

  offline/
    OfflineKnowledgeBase.java
    ScenarioMatcher.java

  voice/
    WhisVoiceInput.java
    WhisVoiceOutput.java

  media/
    ImageInputHandler.java
    AudioInputHandler.java
    PdfInputHandler.java

  emergency/
    RedAlertManager.java
    RedAlertActivity.java
    EmergencyContactNotifier.java

  consent/
    DataConsentManager.java

  model/
    ChatMessage.java
    MediaAttachment.java
    FraudScenario.java
    UserProfile.java
    RiskLevel.java (enum)

app/src/main/assets/
  offline_scenarios.json
  bank_helplines.json
  complaint_templates.json

app/src/main/res/layout/
  activity_agent.xml
  activity_red_alert.xml
  item_chat_agent.xml
  item_chat_user.xml
  item_option_buttons.xml
  fragment_consent.xml
```

---

### 4.2 Build Order (What Gets Created First)

**Week 1 — Foundation (Days 1-4)**

**Day 1:**
Create model classes first — everything depends on these.
- `RiskLevel.java` — enum: LOW, MEDIUM, HIGH, CRITICAL
- `UserProfile.java` — fields: name, ageGroup, occupation, language, techLevel,
  primaryUpi, emergencyContact
- `ChatMessage.java` — fields: role (user/agent), content, timestamp, type
  (TEXT/OPTIONS), optionButtons list
- `MediaAttachment.java` — fields: base64Data, mimeType, attachmentType enum
- `FraudScenario.java` — fields: id, keywords list, risk, responseHindi,
  responseGujarati, responseEnglish, nextStepsHindi list, nextStepsGujarati list,
  escalate boolean

**Day 2:**
- Write `offline_scenarios.json` — all 30 scenarios with Hindi + Gujarati responses
  (this is content work, not code, but blocking everything else)
- Write `bank_helplines.json` — all bank numbers verified above
- Write `complaint_templates.json` — bank complaint letter template, RBI Ombudsman
  summary
- `OfflineKnowledgeBase.java` — loads JSON from assets, caches in memory
- `ScenarioMatcher.java` — keyword scoring against user message, returns best match

**Day 3:**
- `DataConsentManager.java` — checks SharedPreferences "consent_given" boolean,
  exposes isConsentGiven() and saveConsent(boolean)
- `UserProfileContext.java` — reads UserProfile from SharedPreferences key
  "whis_user_profile" (JSON), returns default profile if not present so this
  module never crashes waiting for Login module
- `ModuleContextInjector.java` — reads "whis_flags" JSON array from
  SharedPreferences, returns empty list if absent (graceful fallback)
- `SessionContext.java` — holds conversation history list, addTurn(), getHistory()
  (last 10 turns), clearSession()

**Day 4:**
- `RedAlertManager.java` — keyword arrays for Hindi, Gujarati, English, assess()
  method returning RiskLevel, called on every user message before API
- `EmergencyContactNotifier.java` — sends SMS to emergency contact from UserProfile
  if SEND_SMS permission granted, handles permission missing gracefully

**Week 2 — Core Agent (Days 5-8)**

**Day 5:**
- `SystemPromptBuilder.java` — assembles complete system prompt string
  (see Section 4.3 for structure). This is the most important file in the module.
  All hardcoded facts go here: helpline numbers, RBI steps, portal URLs, complaint
  process, fraud pattern descriptions. No fact that could be wrong if hallucinated
  is left to Gemini to generate.

**Day 6:**
- `GeminiAgentClient.java` — Firebase AI Logic SDK wrapper
  (see Section 4.4 for exact code)
- `AgentRequest.java` — data class with system prompt, history, message, attachment
- `AgentResponse.java` — data class with message text, riskLevel, optionButtons
  list, actionFlags list

**Day 7:**
- `AgentViewModel.java` — full business logic:
  checkEmergency → checkNetwork → checkConsent → injectContext → buildPrompt →
  callGemini or callOffline → parseResponse → updateLiveData
- Handle streaming response from Gemini
- Handle rate limit 429 errors → fall to offline gracefully
- Handle no-network → fall to offline

**Day 8:**
- Voice input and output:
  `WhisVoiceInput.java` — SpeechRecognizer wrapper, RECORD_AUDIO permission check,
  returns text to AgentViewModel
  `WhisVoiceOutput.java` — TextToSpeech wrapper, language from UserProfile,
  slower speech rate for age 60+

**Week 2 — UI and Media (Days 9-10)**

**Day 9:**
- `AgentActivity.java` — hosts RecyclerView for messages, handles input bar,
  voice button, attachment icons, observes AgentViewModel LiveData, launches
  RedAlertActivity when flagged
- `RedAlertActivity.java` — full screen, three large buttons, no back button,
  "Main theek hoon" dismiss link at bottom
- Layout files: activity_agent.xml, activity_red_alert.xml, item_chat_agent.xml,
  item_chat_user.xml, item_option_buttons.xml, fragment_consent.xml

**Day 10:**
- Media handlers:
  `ImageInputHandler.java` — gallery/camera intent, URI to base64 conversion
  `AudioInputHandler.java` — file picker for audio (mp3/wav/ogg/m4a), bytes to
  base64
  `PdfInputHandler.java` — file picker for PDF, PdfRenderer (Android built-in,
  API 21+) renders each page as Bitmap, convert to base64 JPEG array
- `AgentLauncher.java` — static launch methods for other modules to call
- Final testing, fix edge cases

---

### 4.3 System Prompt Structure

This prompt is assembled by SystemPromptBuilder.java and injected with every call.
UserProfile fields are substituted at runtime.

```
IDENTITY:
You are Whis, an expert fraud protection officer built into the Whis app for Indian
users. You have deep expertise in cyber crime investigation, Indian banking law, UPI
payment systems, RBI guidelines, IT Act 2000, and every fraud pattern currently
targeting Indian users. You speak like a senior trusted officer — calm, clear,
always on the user's side.

Never say "I am an AI." Never give generic advice like "be careful online."
Never give 5 options when 1 clear action is needed.
Never generate phone numbers, portal URLs, or legal section numbers from your own
knowledge — use only what is provided below. If a fact is not in this prompt, say
"main is baare mein sure nahi hoon, apne bank se confirm karein" rather than guess.

THE USER:
Name: {name}
Age group: {age_group}
Language preference: {language}
Tech comfort: {tech_level}
Primary UPI app: {primary_upi}

Response language rules:
- Respond in {language} always
- If Basic tech level: short sentences, one step at a time, explain every term
- If Advanced: fast, direct, technical detail included, skip obvious steps
- Mix Hindi/Gujarati with English for UPI/OTP/PIN terms — these are understood

RECENT WHIS FLAGS:
{sms_flags_text}
{call_flags_text}
(If flags are present, proactively connect them to what the user is describing)

RESPONSE FORMAT:
Always respond as JSON:
{
  "message": "your full response in user's language",
  "risk_level": "LOW|MEDIUM|HIGH|CRITICAL",
  "option_buttons": ["button text 1", "button text 2"],
  "action_flags": []
}
action_flags options: "TRIGGER_RED_ALERT", "SHOW_1930_INFO", "SHOW_BANK_INFO"
option_buttons: max 5, short tap labels for next turn choices, empty if free text

CRISIS FLOW RULES:
When user describes a fraud incident:
1. First response: calm empathy + ONE clarifying question about what happened
2. Use option buttons to guide — never ask user to type details if buttons can work
3. Calculate RBI window once you know when fraud happened — tell user immediately
4. Always end with exactly one "ABHI YE KARO" action — the most urgent next step
5. If CRITICAL (scammer on line right now, money moving NOW):
   set action_flag TRIGGER_RED_ALERT, say only: "PHONE RAKHO ABHI. 1930 PE CALL KARO."

HARDCODED FACTS — USE ONLY THESE, NEVER GENERATE FROM MEMORY:

RBI RECOVERY TIMELINE (RBI Circular 6 July 2017):
- Report within 3 working days → Zero liability, full refund in 10 working days
- Report within 4-7 days → Limited liability (₹5,000-25,000), refund of remainder
- Report after 7 days → Bank discretion, lower recovery chance
- Once reported, all future losses are zero liability

COMPLAINT STEPS IN ORDER:
1. Call 1930 IMMEDIATELY — get complaint reference number
2. Open UPI app → Help → Report fraud → enter UTR number
3. Call bank fraud helpline — give 1930 reference number
4. File on cybercrime.gov.in — get acknowledgment number (format: 2026XXXXXXXXX)
5. Written complaint to bank branch: include phrase
   "Unauthorised electronic banking transaction" + all reference numbers
6. If bank no reply in 30 days → RBI Ombudsman at cms.rbi.org.in

HELPLINE NUMBERS (verified July 2026):
Cyber Crime: 1930
SBI: 1800-11-2211
HDFC: 1800-258-6161
ICICI: 1800-1080
Axis: 1800-103-5577
Kotak: 1800-209-0000
Yes Bank: 1800-1200
PNB: 1800-180-2222
BOB: 1800-5700
Default: Debit card ke peeche helpline number hota hai

TRANSACTION ID LOCATION:
Google Pay: History tab → tap transaction → Transaction ID
PhonePe: My Transactions → tap transaction → 12-digit UPI ID
Paytm: Passbook → UPI tab → tap transaction → Transaction ID
Bank SMS: UTR number (12 digits) in every UPI SMS from your bank

CYBERCRIME PORTAL FILING:
1. cybercrime.gov.in → File a Complaint → Financial Fraud
2. Login with mobile OTP
3. Four forms: incident, suspect, complainant, evidence
4. Upload: SMS screenshots, transaction screenshots, call logs, chat logs
5. Save acknowledgment number — use this in all future communications

FRAUD PATTERNS — RECOGNISE THESE:
[30 patterns with identification cues and standard recovery guidance — populated
from offline_scenarios.json content at build time]

LEGAL ESCALATION:
BNS 2023: Section 318 (cheating), Section 319 (forgery), Section 316 (criminal
breach of trust). IT Act 2000: Sections 66, 66C, 66D.
Consumer Commission: bank negligence → file for deficiency of service + mental
agony compensation. Cases up to ₹50 lakh at District Commission.
Cybercrime has no jurisdiction limits — file from anywhere in India.
```

---

### 4.4 Core API Call — Java

```java
// build.gradle (app)
implementation(platform("com.google.firebase:firebase-bom:34.16.0"))
implementation("com.google.firebase:firebase-ai")
implementation("com.google.firebase:firebase-appcheck-playintegrity")

// GeminiAgentClient.java
public class GeminiAgentClient {

    private static final String MODEL_PRIMARY = "gemini-2.5-flash-lite";
    private static final String MODEL_FALLBACK = "gemini-2.5-flash";
    private GenerativeModel model;

    public GeminiAgentClient() {
        FirebaseAI firebaseAI = FirebaseAI.getInstance(
            GenerativeBackend.googleAI()
        );
        GenerationConfig config = new GenerationConfig.Builder()
            .temperature(0.2f)       // Low = consistent, factual, less creative
            .maxOutputTokens(600)    // Enough for complete response, not bloated
            .responseMimeType("application/json") // Force JSON output
            .build();
        model = firebaseAI.generativeModel(MODEL_PRIMARY, config);
    }

    public void sendMessage(
        String systemPrompt,
        List<ChatMessage> history,
        String userMessage,
        @Nullable MediaAttachment attachment,
        StreamCallback callback
    ) {
        List<Content> contents = buildContents(
            systemPrompt, history, userMessage, attachment
        );

        model.generateContentStream(contents.toArray(new Content[0]))
            .subscribe(
                chunk -> {
                    if (chunk.getText() != null) {
                        callback.onChunk(chunk.getText());
                    }
                },
                error -> {
                    // 429 rate limit or network error → caller falls to offline
                    callback.onError(error);
                },
                callback::onComplete
            );
    }

    private List<Content> buildContents(
        String systemPrompt,
        List<ChatMessage> history,
        String userMessage,
        MediaAttachment attachment
    ) {
        List<Content> contents = new ArrayList<>();

        // Inject system prompt as opening exchange
        contents.add(new Content.Builder()
            .role("user")
            .addText(systemPrompt)
            .build());
        contents.add(new Content.Builder()
            .role("model")
            .addText("{\"message\":\"Samjha. Main Whis AI Agent hoon. Batao kya hua.\",\"risk_level\":\"LOW\",\"option_buttons\":[],\"action_flags\":[]}")
            .build());

        // Add history (last 10 turns from SessionContext)
        for (ChatMessage msg : history) {
            contents.add(new Content.Builder()
                .role(msg.getRole())
                .addText(msg.getContent())
                .build());
        }

        // Current user message + optional media
        Content.Builder current = new Content.Builder().role("user");
        current.addText(userMessage);
        if (attachment != null) {
            current.addInlineData(
                attachment.getBase64Data(),
                attachment.getMimeType()
            );
        }
        contents.add(current.build());

        return contents;
    }

    public interface StreamCallback {
        void onChunk(String textChunk);
        void onComplete();
        void onError(Exception e);
    }
}
```

---

### 4.5 Emergency Detection

```java
// RedAlertManager.java
public class RedAlertManager {

    // Phrases indicating scammer is ACTIVELY present right now
    private static final String[] CRITICAL_HI = {
        "abhi call", "phone pe hai", "line pe hai", "paisa bhej",
        "transfer kar", "otp de do", "otp bhej", "account band",
        "arrest", "cbi", "ed officer", "police aa rahi", "digital arrest",
        "anydesk", "teamviewer", "screen share", "screen dekh raha"
    };
    private static final String[] CRITICAL_GU = {
        "abi call", "phone par chhe", "paisa moklo", "otp aap",
        "account band", "arrest", "police aavi"
    };
    private static final String[] CRITICAL_EN = {
        "on the call right now", "they are watching my screen",
        "transferring right now", "digital arrest", "cbi officer"
    };

    public RiskLevel assess(String message) {
        String lower = message.toLowerCase();
        for (String kw : CRITICAL_HI) {
            if (lower.contains(kw)) return RiskLevel.CRITICAL;
        }
        for (String kw : CRITICAL_GU) {
            if (lower.contains(kw)) return RiskLevel.CRITICAL;
        }
        for (String kw : CRITICAL_EN) {
            if (lower.contains(kw)) return RiskLevel.CRITICAL;
        }
        return RiskLevel.MEDIUM;
    }
}
```

---

### 4.6 AgentViewModel Flow

```java
// AgentViewModel.java — core sendMessage logic
public void sendUserMessage(String message, MediaAttachment attachment) {

    // Step 1: Emergency check — no API needed, instant
    RiskLevel risk = redAlertManager.assess(message);
    if (risk == RiskLevel.CRITICAL) {
        triggerRedAlert.postValue(true);
        return;
    }

    // Step 2: Check consent
    if (!dataConsentManager.isConsentGiven()) {
        showConsentRequired.postValue(true);
        return;
    }

    // Step 3: Check network
    if (!networkHelper.isConnected()) {
        AgentResponse offline = offlineKnowledgeBase.findResponse(
            message, userProfile.getLanguage()
        );
        appendMessage(offline);
        return;
    }

    // Step 4: Build context
    UserProfile profile = userProfileContext.getProfile();
    List<ModuleFlag> flags = moduleContextInjector.getRecentFlags(24);
    List<ChatMessage> history = sessionContext.getHistory();
    String systemPrompt = systemPromptBuilder.build(profile, flags);

    // Step 5: Call Gemini
    isLoading.postValue(true);
    geminiClient.sendMessage(systemPrompt, history, message, attachment,
        new GeminiAgentClient.StreamCallback() {

            StringBuilder buffer = new StringBuilder();

            @Override public void onChunk(String chunk) {
                buffer.append(chunk);
                // Stream partial text to UI for low perceived latency
                streamingMessage.postValue(buffer.toString());
            }

            @Override public void onComplete() {
                isLoading.postValue(false);
                AgentResponse response = parseResponse(buffer.toString());

                // Check if Gemini itself flagged critical risk
                if (response.getActionFlags().contains("TRIGGER_RED_ALERT")) {
                    triggerRedAlert.postValue(true);
                    return;
                }

                sessionContext.addTurn("user", message);
                sessionContext.addTurn("model", response.getMessage());
                appendMessage(response);
            }

            @Override public void onError(Exception e) {
                isLoading.postValue(false);
                // Rate limit or network failure — fall to offline
                AgentResponse offline = offlineKnowledgeBase.findResponse(
                    message, profile.getLanguage()
                );
                appendMessage(offline);
            }
        }
    );
}
```

---

### 4.7 UI Structure (Screens and Components)

**Screen 1: Consent Screen (fragment_consent.xml + DataConsentManager)**
Shown once at first launch before any API call.

```
[App icon]
[Heading] "Whis AI — Kaise Kaam Karta Hai"

[Body] "Jab aap AI Agent se baat karte hain, aapke messages
Google ke Gemini AI service ko bheje jaate hain. Google is
data ko apni AI improve karne ke liye use kar sakta hai.
Aapka data India ke bahar Google ke servers pe jaata hai."

[Button — full width solid]   "SAMJHA, AGREE KARTA HOON"
[Button — full width outline] "SIRF OFFLINE MODE CHAHIYE"
```

**Screen 2: Main Chat (activity_agent.xml)**

```
[Top bar]
  [Back]   [Whis AI]   [Voice mode toggle button]

[RecyclerView — messages]
  item_chat_agent.xml  — agent bubble, left aligned
  item_chat_user.xml   — user bubble, right aligned
  item_option_buttons.xml — horizontal scroll row of buttons
                            appears after agent question
  [Typing indicator — 3 dots animation while API call runs]

[Attachment row — visible above keyboard]
  [Camera]  [Gallery]  [Audio File]  [PDF]

[Input bar — pinned to bottom]
  [Mic button]  [EditText]  [Send button]
```

**Screen 3: Red Alert (activity_red_alert.xml)**

```
[Full screen — FLAG_KEEP_SCREEN_ON set]
[No back button — system back disabled]

[Large icon — warning symbol]
[Large text]  "GHABRAO MAT"
[Small text]  "Ye 3 kaam karo abhi:"

[Large button 1]
  "1930 DIAL KARO"
  subtitle: "National Cyber Crime Helpline — 24/7 free"
  → triggers Intent(ACTION_DIAL, tel:1930)

[Large button 2]
  "[Bank name] KO CALL KARO"
  subtitle: "[bank helpline number from profile]"
  → triggers Intent(ACTION_DIAL, tel:[number])

[Large button 3]
  "FAMILY KO BATAO"
  subtitle: "[emergency contact name] ko SMS jaayega"
  → EmergencyContactNotifier.send()

[Divider line]

[Checklist — plain text, large font]
"✓ Scammer se baat band karo"
"✓ Koi bhi link mat dabao"
"✓ Koi bhi OTP mat do"
"✓ Phone pe UPI PIN kabhi mat batao"

[Bottom — small text link]
"Main theek hoon, wapas jaao"  → finishes activity
```

---

### 4.8 Permissions (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.SEND_SMS"/>
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO"/>
<!-- For API < 33 -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32"/>
```

All permissions requested at runtime with plain Hindi/Gujarati explanation.
RECORD_AUDIO: "Aapki awaaz se baat karne ke liye"
SEND_SMS: "Emergency mein aapke family member ko alert karne ke liye"
SEND_SMS is optional — Red Alert works without it, just no SMS sent.

---

### 4.9 What I Expose to Other Modules

**AgentLauncher.java** — static methods, any module can call:

```java
// Open agent fresh
AgentLauncher.launch(context);

// SMS module calls this when it flags something
AgentLauncher.launchWithSmsContext(context, smsContent, sender);
// Opens agent with prefilled context message about that SMS

// Call module calls this when it flags a call
AgentLauncher.launchWithCallContext(context, phoneNumber);
// Opens agent with prefilled context message about that call
```

**SharedPreferences keys I read (other modules write these):**

```
Key: "whis_user_profile"
Type: JSON string → UserProfile object
Writer: Login module
Reader: AI Agent (UserProfileContext.java)
Fallback if absent: default profile (Hindi, Basic, age 35, GPay)

Key: "whis_flags"
Type: JSON array of flag objects
Writer: SMS module, Call module
Reader: AI Agent (ModuleContextInjector.java)
Expected format:
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
Fallback if absent or wrong format: empty list, agent works normally
```

---

## 5. DEPENDENCIES ON OTHER MODULES

| Need | From | Fallback if absent |
|------|------|--------------------|
| UserProfile data | Login module | Default profile (name=User, Hindi, Basic, GPay) |
| SMS flag data | SMS module | Empty flags list — agent works, less context-aware |
| Call flag data | Call module | Empty flags list — agent works, less context-aware |
| Navigation entry | UI module | AgentActivity launched directly via AgentLauncher |
| UI style/theme | UI module | Plain Material3 defaults, consistent with minSdk 24 |
| Firebase project google-services.json | Master | Cannot build without this — see Open Questions |

**No dependency will crash the module.** All fallbacks are graceful degradation.

---

## 6. WHAT I AM NOT SOLVING (Phase 1)

**Gemini Live API (real-time bidirectional voice)**
True voice conversation where user speaks and hears agent speak back in real time.
Available in Firebase AI Logic as of July 2026. Too complex for July 31 deadline —
WebSocket streaming architecture, audio buffer management, real-time error handling.
Phase 2 when there is more build time.

**Video upload and analysis**
Video evidence of fraud is rare compared to screenshots. Upload UX is complex.
Processing time is long. Not worth the implementation risk before July 31.
Phase 2.

**Fraud report PDF auto-generation**
Generating a pre-filled cybercrime complaint or bank dispute letter as a
downloadable PDF. Needs PDF generation library and template work.
Current substitute: complaint_templates.json — agent reads the template and walks
user through filling it manually. Phase 2.

**Cross-session memory**
Remembering what user told agent 3 days ago. Within-session memory is complete.
Cross-session needs persistent conversation storage + privacy design work. Phase 2.

**On-device LLM (Gemini Nano)**
Full privacy, zero cost, offline always. Attractive for Phase 2 but:
experimental Hindi/Gujarati quality, 4,000 token context limit, cannot handle
images without cloud. Phase 2 if quality improves.

**Proactive in-agent push notifications**
Agent proactively messaging user when SMS/Call module detects something.
The notification itself comes from SMS/Call modules. Agent opens with context when
user taps it. Proactive agent-initiated messages are Phase 2.

---

## 7. OPEN QUESTIONS FOR MASTER

**Q1 — Firebase project (BLOCKING before any API call can work):**
Firebase AI Logic requires a `google-services.json` tied to one Firebase project.
This file must be the same across all modules — if two modules create separate
Firebase projects, the app cannot be built. Master must create one Firebase project,
enable Gemini API in it, and distribute `google-services.json` to all branches.
This cannot be done per-module.

**Q2 — Paid tier decision (BLOCKING before MVP with real users):**
Free tier for Gemini 2.5 Flash is approximately 250 requests/day as of March 2026
(multiple sources confirm December 2025 cuts). This supports maybe 20 active users
before hitting daily limit. Gemini 2.5 Flash-Lite has better free tier (~1,000/day)
and is the primary model in this plan.
Before any real user touches this app: team must decide on Google AI Studio paid
tier or Vertex AI. Vertex AI removes the training-on-user-data concern entirely.
Recommend: Vertex AI for any release that involves real fraud victim data.

**Q3 — SharedPreferences key format agreement:**
I read "whis_flags" expecting the JSON format defined in Section 4.9. SMS and Call
modules must write to this key in that exact format. If they use different field
names, ModuleContextInjector.java will silently return empty (graceful, not broken)
but agent loses context awareness. Master should confirm or correct the format and
circulate it as the shared contract before SMS/Call modules start writing to it.

**Q4 — Emergency contact SMS scope:**
Red Alert can send an SMS to a family member. This requires SEND_SMS permission,
which some users (especially on heavily restricted OEM skins like MIUI) may see as
alarming and deny. If denied, Red Alert still works — only the SMS is skipped.
Master should know: this permission is in the manifest. If it causes Play Store
review concerns, the feature can be removed without breaking anything else.

**Q5 — Consent screen placement:**
DataConsentManager.java lives in my module. But the consent logically belongs at
first launch alongside login. If Login module handles first-launch onboarding,
they should call DataConsentManager.saveConsent() after the user agrees, rather
than my module showing a separate consent screen mid-session. Coordinate with
Login module to avoid user seeing two consent screens.

---

*Research complete. All time-sensitive facts reverified 21 July 2026.*
*Ready to hand to Master for review.*
