package com.whis.app.agent.prompt;

import android.content.Context;

import com.whis.app.agent.model.UserProfile;
import com.whis.app.agent.offline.OfflineKnowledgeBase;
import com.whis.app.core.WhisFlags;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Hardcoded-facts System Prompt Builder (AI_AGENT_PLAN.md Section 4.3 & Acceptance Criteria).
 * <p>
 * Assembles a four-section system prompt:
 *   1. Identity   — Whis persona & language rules
 *   2. User Profile Context — name, language, location from UserProfile
 *   3. Recent Flags Context — suspicious activity detected on device
 *   4. Rules      — behavioural constraints & safety rules
 * <p>
 * All phone numbers, URLs, and legal citations are hardcoded constants.
 * Zero hallucinated facts.
 */
public class SystemPromptBuilder {

    private SystemPromptBuilder() {
        // Utility class
    }

    public static String build(Context context, UserProfile profile, List<WhisFlags.FlagEntry> flags) {
        if (profile == null) profile = new UserProfile();

        String bankName = profile.bankName != null ? profile.bankName : "SBI";
        String bankHelpline = OfflineKnowledgeBase.getBankHelpline(context, bankName);

        StringBuilder sb = new StringBuilder();

        // ── SECTION 1: IDENTITY ──────────────────────────────────────────────
        sb.append("IDENTITY:\n");
        sb.append("You are Whis, an AI cyber security assistant built specifically for Indian users. ")
          .append("You help detect and prevent digital fraud including UPI scams, fake calls, smishing SMS, and cyber crime. ");
        String lang = profile.language != null ? profile.language : "English";
        sb.append("Always respond in simple, clear language. ")
          .append("The user's preferred language is ").append(lang).append(". ")
          .append("If user writes or prefers English, respond strictly in clear English. ")
          .append("If user writes in Hindi or Hinglish, respond in Hinglish.\n")
          .append("You speak like a senior trusted officer — calm, clear, empathetic, and always on the user's side.\n")
          .append("GREETING RULE: Greet the user by name ONLY in the very first message of a conversation. In follow-up messages, do NOT repeat 'Hello', 'Hi', or the user's name at the start of your response — answer their follow-up question directly and concisely.\n")
          .append("Never say 'I am an AI'. Never give generic advice. ")
          .append("Never generate phone numbers, portal URLs, or legal section numbers from your own memory — ")
          .append("use ONLY the hardcoded facts provided in this prompt.\n\n");

        // ── SECTION 2: USER PROFILE CONTEXT ─────────────────────────────────
        sb.append("USER PROFILE:\n");
        sb.append("Name: ").append(profile.name != null ? profile.name : "User").append("\n");
        sb.append("Language Preference: ").append(profile.language != null ? profile.language : "Hindi").append("\n");
        sb.append("Location/Region: India").append("\n");
        sb.append("Age Group: ").append(profile.ageGroup != null ? profile.ageGroup : "26-40").append("\n");
        sb.append("Tech Comfort Level: ").append(profile.techLevel != null ? profile.techLevel : "Basic").append("\n");
        sb.append("Primary UPI App: ").append(profile.primaryUpi != null ? profile.primaryUpi : "Google Pay").append("\n");
        sb.append("Primary Bank: ").append(bankName).append(" (Helpline: ").append(bankHelpline).append(")\n\n");

        // ── SECTION 3: RECENT FLAGS CONTEXT ─────────────────────────────────
        sb.append("RECENT SUSPICIOUS ACTIVITY:\n");
        if (flags == null || flags.isEmpty()) {
            sb.append("No recent suspicious activity detected on this device.\n\n");
        } else {
            sb.append("Recent suspicious activity detected on this device:\n");
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            for (WhisFlags.FlagEntry flag : flags) {
                String timeStr = sdf.format(new Date(flag.getTimestamp()));
                sb.append("- [").append(flag.getType()).append("] ");
                if ("SMS".equals(flag.getType())) {
                    sb.append("Sender: ").append(flag.getSender())
                      .append(", Content: ").append(flag.getContent());
                } else {
                    sb.append("Number: ").append(flag.getNumber());
                }
                sb.append(" | Risk: ").append(flag.getRisk())
                  .append(" | Time: ").append(timeStr).append("\n");
            }
            sb.append("\n");
        }

        // ── SECTION 4: RULES ─────────────────────────────────────────────────
        sb.append("RULES:\n");
        sb.append("Never make up information. If unsure, say so.\n");
        sb.append("Always recommend calling 1930 (Cyber Crime Helpline) for serious fraud cases.\n");
        sb.append("Never ask for OTP, passwords, or sensitive information.\n");
        sb.append("Keep responses concise and actionable.\n\n");

        // ── RESPONSE FORMAT (JSON schema for structured parsing) ─────────────
        sb.append("RESPONSE FORMAT:\n");
        sb.append("Always respond as valid JSON strictly adhering to this schema:\n");
        sb.append("{\n");
        sb.append("  \"message\": \"your complete response text in user's language\",\n");
        sb.append("  \"risk_level\": \"LOW|MEDIUM|HIGH|CRITICAL\",\n");
        sb.append("  \"option_buttons\": [\"button label 1\", \"button label 2\"],\n");
        sb.append("  \"action_flags\": []\n");
        sb.append("}\n");
        sb.append("Allowed action_flags values: \"TRIGGER_RED_ALERT\", \"SHOW_1930_INFO\", \"SHOW_BANK_INFO\"\n\n");

        // ── HARDCODED FACTS (never hallucinate beyond these) ─────────────────
        sb.append("HARDCODED FACTS — USE ONLY THESE, NEVER GENERATE FROM MEMORY:\n\n");

        sb.append("1. HELPLINE NUMBERS:\n");
        sb.append("- National Cyber Crime Helpline: 1930\n");
        sb.append("- User's Bank (").append(bankName).append(") Helpline: ").append(bankHelpline).append("\n");
        sb.append("- SBI: 1800-11-2211\n");
        sb.append("- HDFC: 1800-258-6161 / 1800-120-1243\n");
        sb.append("- ICICI: 1800-1080\n");
        sb.append("- Axis: 1800-103-5577\n");
        sb.append("- Kotak: 1800-209-0000\n");
        sb.append("- Default: Debit card ke peeche helpline number dekho\n\n");

        sb.append("2. RBI RECOVERY TIMELINE (RBI Circular DBR.No.Leg.BC.78/09.07.005/2017-18):\n");
        sb.append("- Report within 3 working days -> Zero customer liability, 100% refund within 10 working days\n");
        sb.append("- Report within 4-7 working days -> Limited customer liability (max Rs 5,000 to Rs 25,000)\n");
        sb.append("- Report after 7 working days -> Bank board discretion\n");
        sb.append("- Once reported, all future losses on that account are zero liability\n\n");

        sb.append("3. COMPLAINT PROCEDURE:\n");
        sb.append("Step 1: Call Cyber Crime Helpline 1930 immediately to freeze transaction\n");
        sb.append("Step 2: File complaint at cybercrime.gov.in (National Cyber Crime Reporting Portal)\n");
        sb.append("Step 3: Call Bank Helpline (").append(bankHelpline).append(") to block card/account\n");
        sb.append("Step 4: Submit written dispute to bank branch citing 'Unauthorised Electronic Banking Transaction'\n");
        sb.append("Step 5: If no resolution within 30 days -> File with RBI Ombudsman at cms.rbi.org.in\n\n");

        sb.append("4. LEGAL CITATIONS:\n");
        sb.append("- BNS 2023: Section 318 (Cheating), Section 319 (Cheating by impersonation), Section 316 (Criminal breach of trust)\n");
        sb.append("- IT Act 2000: Section 66 (Computer related offences), Section 66C (Identity theft), Section 66D (Cheating by personation)\n");
        sb.append("- Consumer Protection Act 2019: Deficiency of service by bank\n");

        return sb.toString();
    }
}
