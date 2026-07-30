# Learning / Awareness Module — Whis
**Status:** Draft for Master review · **Note:** Not originally assigned to UI/Navigation — proceeding on explicit direction, flagging for Master to confirm ownership doesn't collide with an existing Learning module owner.

---

## 1. WHAT YOU FOUND

Research grounded in current (2025–2026) sources, not assumed knowledge. Every chapter below is built from real mechanics, real numbers, and real official guidance — not generic "be careful" advice.

**The psychological finding that shapes the whole book's structure, not just its content:** aging can reduce activity in the brain's anterior insula — the region linked to distrust — and combined with slower cognitive processing under pressure, this measurably reduces the ability to detect manipulation in the moment, for reasons that have nothing to do with intelligence or character. This means every chapter needs to answer "why would this trap anyone" before "what do I do" — otherwise the book quietly implies the victim should have known better, which is both untrue and actively harmful to say to someone who's already scared.

**The five chapters, and the real mechanics behind each:**

1. **The Fake Police Video Call ("Digital Arrest")** — Indians have lost an estimated ₹2,140 crore to this scam over 18 months. Fraudsters impersonate CBI/ED/TRAI/police using staged police-station backdrops and uniforms on video, keeping victims on the line for hours under psychological pressure to transfer money "to prove cooperation." The single fact that ends it: there is no such legal procedure as "digital arrest" under Indian law. Calling 1930 within 24 hours gives banks their best chance to freeze the destination account before withdrawal.

2. **The QR Code That Empties Your Account** — the confirmed, universal rule: a QR code can only send money, never receive it, and you never need your UPI PIN to receive a payment. Real scenario: an online seller is sent a QR code by a "buyer" claiming it will deliver payment; scanning it authorizes an outgoing payment instead. Physical variant: fraudsters paste fake QR stickers over real ones at shops, petrol pumps, and temple donation boxes. Senior citizens are named directly in multiple sources as a specifically vulnerable group here, precisely because the receive/send confusion is unintuitive.

3. **The "Install This App" Call (AnyDesk/TeamViewer)** — RBI's own warning describes the mechanism: fraudsters posing as bank staff ask the victim to install AnyDesk, which generates a 9-digit code; sharing it hands over full phone control, letting attackers see OTPs the instant they arrive. **2026-specific evolution:** this now frequently chains directly from the KYC-link scam — victim clicks a fake KYC link, enters credentials, then gets a follow-up call minutes later asking them to install AnyDesk "to complete the verification." Emergency response if it's happening right now: force the phone offline (airplane mode or power off) — AnyDesk needs an active connection to maintain control.

4. **"Your Parcel Is Held" (Courier/Customs)** — India's Central Board of Indirect Taxes and Customs has stated plainly that customs officers never contact the public by phone, SMS, or email for duty payment to a private account. Real case: a Hyderabad resident lost ₹11,800 to a fake customs SMS in April 2026, filed a 1930 complaint the same evening, and recovered ₹9,200 within 47 days — worth including explicitly, since it proves recovery is possible and fast reporting measurably matters, not just a scare story with no hope attached. **Escalation path:** this scam frequently upgrades into a full digital-arrest call ("your parcel contained drugs, you're now under investigation") — cross-reference Chapter 1.

5. **The Instant Loan App** — apps with no RBI/NBFC registration, spreading now mainly via Instagram/WhatsApp/Telegram APK links rather than app stores (to dodge store review). Real case: an ED bust in Kerala in early 2026 uncovered a ₹719 crore fraud network moving money through crypto and shell companies. Real individual cases show victims borrowing as little as ₹3,000 and being blackmailed into repaying ₹15,000–₹20,000 using stolen contact-list and photo access, with threats sent to family and employers. Verification path: RBI's own NBFC registry is public and checkable before installing anything.

**What I did NOT verify and am flagging honestly rather than guessing:** investment/crypto "pig-butchering" scams and romance-scam-to-investment funnels are real and cited (a Hyderabad case of two elderly victims losing ₹3.2 crore was found), but I haven't done a dedicated mechanics-and-red-flags pass on this category the way I did for the five above. If it's wanted as Chapter 6, it needs its own research round before I write content for it — I'm not writing a thin chapter just to hit a number.

**The universal thread, confirmed across every source regardless of scam type:** 1930 (national cyber fraud helpline) and cybercrime.gov.in (NCRP portal) are the correct, current reporting channels in every case. This belongs as a permanent fixture of the book, not repeated five separate times.

---

## 2. HOW TO APPLY IT

### 2.1 The four-question chapter format

Every chapter, no exceptions, in this order:

1. **What happens?** — plain description of the scam mechanics, one short paragraph.
2. **Why does this work, even on careful people?** — names the actual psychological lever (authority pressure, urgency, unfamiliarity with a specific mechanism like UPI's send/receive rule) — never implies the victim was careless.
3. **What do I do right now if this is happening to me?** — 3 concrete, ordered actions, written for the moment of panic, not after-the-fact reflection.
4. **What does Whis do about this automatically?** — ties back to real Call/Message detection so Learning content and live protection reinforce each other rather than living in separate silos.

### 2.2 Search-by-problem, not browse-by-title

The Gita-index instinct, built as a real feature: a search bar at the top of Learn that matches natural-language problems to chapters — "someone called me about my parcel," "police video call," "asked me to install an app" — not just chapter titles. This requires each chapter to carry a set of trigger phrases/keywords in its data, separate from its display title, so a panicked search in plain words still finds the right answer.

### 2.3 Content data schema

Stored as structured data (JSON asset, not hardcoded strings), so it can support search, multi-language (Hindi/Gujarati per confirmed project scope), and being pulled into AI Agent's "explain this alert" responses without duplicating content in two places:

```json
{
  "chapterId": "digital_arrest",
  "title": "The Fake Police Video Call",
  "searchTriggers": ["police video call", "CBI called me", "digital arrest", "arrest warrant", "customs officer video"],
  "whatHappens": "...",
  "whyItWorks": "...",
  "doRightNow": ["Hang up immediately.", "Do not transfer money or share OTP.", "Call 1930 within the hour if any payment was made."],
  "howWhisHelps": "...",
  "crossReference": ["courier_customs"],
  "sourceConfidence": "high — TRAI advisory, MHA statement, multiple case reports"
}
```

`sourceConfidence` is an internal field, not shown to the user — it's how Master or a future contributor can tell which chapters are built on strong sourcing versus which need another research pass, without re-deriving that from scratch.

### 2.4 Where this lives in the app

Existing `LearnFragment` shell (built during UI work, currently placeholder content) becomes the real destination — list of chapters with progress tracking, tapping into a `ChapterDetailFragment` rendering the four-question format. Search bar sits at the top of the chapter list. No new navigation structure needed; this fills the shell already in place.

---

## 3. THE PLAN

### 3.1 Build order

1. Finalize the JSON content schema and write real content for the 5 grounded chapters (this session — see accompanying `learn_chapters.json`).
2. Research and write Chapter 6 (investment/romance-to-investment scams) as its own pass — explicitly not rushed in with thin sourcing.
3. Build `ChapterDetailFragment` rendering the four-question format against the shared design tokens/components already built for UI.
4. Build the search-by-trigger-phrase matching against `searchTriggers` — simple substring/fuzzy match is enough for 5-8 chapters; no need for anything heavier at this scale.
5. Wire progress tracking (already scaffolded in the existing shell) to real per-chapter read/complete state.
6. Flag to AI Agent module that chapter content is available as structured data they can pull from when explaining a live alert, rather than generating explanations independently — reduces the risk of Learning and AI Agent giving inconsistent explanations of the same scam.

### 3.2 What I'm explicitly not solving here

- Video content, audio narration production — the schema supports adding an audio URL per chapter later, but recording/producing narration isn't in scope for this pass.
- Quizzes/interactive testing beyond progress tracking — noted as a real v2 feature, not built now.
- Hindi/Gujarati translation of the actual chapter text — the schema is translation-ready (each field can have per-language variants), but translating the 5 chapters is a distinct task from writing them, and I haven't done it here.

### 3.3 Open question for Master

Confirming this doesn't collide with an existing Learning module owner's independent work — if their plan lands separately, these two need to be reconciled, not silently merged.
