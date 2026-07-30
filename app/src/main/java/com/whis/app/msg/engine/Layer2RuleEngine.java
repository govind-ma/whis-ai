package com.whis.app.msg.engine;

import android.content.Context;

import com.whis.app.msg.model.MsgCategory;

import java.util.regex.Pattern;

/**
 * Layer 2 — Weighted Rule Engine (MSG_PLAN.md Section 2.4 & 4.2).
 * <p>
 * Performs keyword/pattern matching and false-positive OTP whitelisting.
 */
public class Layer2RuleEngine {

    // Regex to match genuine OTP messages: numeric 4-8 digit code + expiry phrase
    private static final Pattern GENUINE_OTP_REGEX = Pattern.compile(
            "(?i)\\b\\d{4,8}\\b.*(valid|expires?|otp|one.?time).*(minute|second|min|sec)"
    );

    // Callback 10-digit mobile number pattern in message body
    private static final Pattern CALLBACK_MOBILE_REGEX = Pattern.compile(
            "\\b[6-9]\\d{9}\\b"
    );

    // URL detection regex
    private static final Pattern URL_REGEX = Pattern.compile(
            "(?i)\\b(https?|ftp)://[-a-z0-9+&@#/%?=~_|!:,.;]*[-a-z0-9+&@#/%=~_|]"
    );

    public static class RuleResult {
        public final int riskScore;       // 0–100 (HIGHER = MORE dangerous)
        public final String threatLevel;  // "SAFE", "SUSPICIOUS", "SCAM"
        public final MsgCategory category;
        public final String reason;
        public final boolean isOtpWhitelisted;

        public RuleResult(int riskScore, String threatLevel, MsgCategory category, String reason, boolean isOtpWhitelisted) {
            this.riskScore = riskScore;
            this.threatLevel = threatLevel;
            this.category = category;
            this.reason = reason;
            this.isOtpWhitelisted = isOtpWhitelisted;
        }
    }

    private Layer2RuleEngine() {
        // Utility class
    }

    /**
     * Analyze SMS body using Layer 2 rule engine.
     */
    public static RuleResult analyze(Context context, String sender, String body, Layer1HeaderChecker.HeaderResult layer1Result) {
        if (body == null || body.trim().isEmpty()) {
            return new RuleResult(layer1Result.baseRiskScore, "SAFE", layer1Result.categoryHint, "Empty message", false);
        }

        String lowerBody = body.toLowerCase();
        boolean hasUrl = URL_REGEX.matcher(body).find();
        boolean hasCallbackNum = CALLBACK_MOBILE_REGEX.matcher(body).find();

        // ── CRITICAL OTP Whitelisting Rule (False-Positive Prevention) ────────────────
        // If message contains ONLY a numeric OTP + bank name / expiry statement,
        // has no URL, no callback number, and is from a DLT-registered sender or valid header:
        // FORCE result to SAFE / TRANSACTION category with riskScore = 0!
        if (!hasUrl && !hasCallbackNum && isGenuineOtpPattern(lowerBody)) {
            if (layer1Result.dltVerified || layer1Result.isContact || "DLT_TRANSACTIONAL".equals(layer1Result.identifierType)) {
                return new RuleResult(
                        0,
                        "SAFE",
                        MsgCategory.TRANSACTION,
                        "Verified bank OTP message (whitelisted format)",
                        true
                );
            }
        }

        int score = layer1Result.baseRiskScore;
        StringBuilder reasons = new StringBuilder();

        // 1. Urgency & Threat Keywords (+35)
        if (containsAny(lowerBody, "turant", "abhi", "immediately", "last chance", "account blocked", "band ho jayega", "arrested", "cyber cell", "cbi notice", "electricity disconnect", "power cut")) {
            score += 35;
            reasons.append("Urgent threat/action phrase detected. ");
        }

        // 2. Credential / OTP Sharing Request (+40)
        if (containsAny(lowerBody, "share otp", "enter pin", "give password", "share aadhaar", "verify upi pin", "share your pin", "share your otp")) {
            score += 40;
            reasons.append("Request to share OTP/PIN detected. ");
        }

        // 3. Authority Impersonation (+30)
        if (containsAny(lowerBody, "rbi notice", "trai advisory", "income tax department", "npci notice", "customs officer", "cyber crime police")) {
            score += 30;
            reasons.append("Authority impersonation language detected. ");
        }

        // 4. Shortened / Suspicious URL (+35)
        if (containsAny(lowerBody, "bit.ly", "tinyurl.com", "t.co", "is.gd", "cutt.ly", "shorturl.at") || (hasUrl && !layer1Result.dltVerified)) {
            score += 35;
            reasons.append("Shortened or unverified web link present. ");
        }

        // 5. Callback Mobile Number in Body (+25 for unknown senders)
        if (hasCallbackNum && !layer1Result.dltVerified && !layer1Result.isContact) {
            score += 25;
            reasons.append("Unverified mobile callback number in message. ");
        }

        // 6. Ham Signals (-20)
        if (containsAny(lowerBody, "debited by rs", "credited with rs", "avail bal", "ac xxxxx", "do not share", "never share", "don't share")) {
            score -= 20;
        }

        // Clamp score to [0, 100]
        if (score < 0) score = 0;
        if (score > 100) score = 100;

        String threatLevel;
        MsgCategory category;

        if (score >= 70) {
            threatLevel = "SCAM";
            category = MsgCategory.JUNK;
        } else if (score >= 40) {
            threatLevel = "SUSPICIOUS";
            category = layer1Result.categoryHint != MsgCategory.GENERAL ? layer1Result.categoryHint : MsgCategory.PROMOTION;
        } else {
            threatLevel = "SAFE";
            category = layer1Result.categoryHint;
        }

        String reasonStr = reasons.length() > 0 ? reasons.toString().trim() : "Standard message pattern";
        return new RuleResult(score, threatLevel, category, reasonStr, false);
    }

    private static boolean isGenuineOtpPattern(String lowerBody) {
        boolean matchesRegex = GENUINE_OTP_REGEX.matcher(lowerBody).find();
        if (!matchesRegex) return false;

        // Phishing pretext asks user to share/tell OTP: "share OTP", "share your", "send OTP", "forward OTP"
        boolean asksToShare = lowerBody.contains("share otp")
                || lowerBody.contains("share your")
                || lowerBody.contains("send otp")
                || lowerBody.contains("forward otp")
                || lowerBody.contains("tell otp");

        return !asksToShare;
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
