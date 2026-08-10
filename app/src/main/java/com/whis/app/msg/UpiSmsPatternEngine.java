package com.whis.app.msg;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * UPI & banking SMS scam pattern detector.
 * Runs OFFLINE — no network needed. Detects fraud patterns in bank SMSes
 * before the user opens their UPI app.
 *
 * Privacy: Only processes SMS text passed to it. No data stored or transmitted.
 */
public class UpiSmsPatternEngine {

    public static class UpiScanResult {
        public final boolean isScamSuspected;
        public final String warningHindi;
        public final String warningEnglish;
        public final int confidenceScore; // 0-100

        public UpiScanResult(boolean isScamSuspected, String warningHindi,
                             String warningEnglish, int confidenceScore) {
            this.isScamSuspected = isScamSuspected;
            this.warningHindi = warningHindi;
            this.warningEnglish = warningEnglish;
            this.confidenceScore = confidenceScore;
        }
    }

    // Known bank sender ID prefixes (VK-, DM-, BK-, AD-, etc.)
    private static final List<String> BANK_SENDER_PREFIXES = Arrays.asList(
            "VK-", "DM-", "BK-", "AD-", "JD-", "VM-"
    );

    // Known bank/UPI related sender keywords
    private static final List<String> BANK_SENDER_KEYWORDS = Arrays.asList(
            "HDFC", "ICICI", "SBI", "AXIS", "KOTAK", "BOB", "PNB", "CANARA",
            "PAYTM", "PHONEPE", "GPAY", "BHIM", "NPCI", "YESBNK", "IDFCFST"
    );

    // HIGH RISK — collect request / reverse UPI patterns
    private static final List<String> COLLECT_REQUEST_PATTERNS = Arrays.asList(
            "collect request", "payment collect", "collect \u20b9", "collect rs",
            "upi collect", "receive request", "money request from",
            "\u092d\u0941\u0917\u0924\u093e\u0928 \u0905\u0928\u0941\u0930\u094b\u0927" // Hindi: भुगतान अनुरोध
    );

    // HIGH RISK — KYC / account freeze
    private static final List<String> KYC_PATTERNS = Arrays.asList(
            "kyc", "kyc update", "kyc expired", "kyc pending", "account blocked",
            "account suspended", "account will be blocked", "update kyc",
            "\u0915\u0947\u0935\u093e\u0908\u0938\u0940" // Hindi: केवाईसी
    );

    // HIGH RISK — OTP sharing
    private static final List<String> OTP_SHARE_PATTERNS = Arrays.asList(
            "share otp", "share your otp", "otp is", "your otp",
            "do not share", "never share otp",
            "\u0913\u091f\u0940\u092a\u0940 \u0936\u0947\u092f\u0930" // Hindi: ओटीपी शेयर
    );

    // HIGH RISK — prize/lottery
    private static final List<String> PRIZE_PATTERNS = Arrays.asList(
            "you have won", "congratulations", "prize money", "lottery",
            "lucky winner", "claim your prize", "reward of rs", "won rs",
            "\u0907\u0928\u093e\u092e" // Hindi: इनाम
    );

    // HIGH RISK — loan urgency
    private static final List<String> LOAN_SCAM_PATTERNS = Arrays.asList(
            "loan approved", "instant loan", "pre-approved loan", "loan offer",
            "processing fee", "registration fee required", "pay fee to get loan",
            "\u0924\u0941\u0930\u0902\u0924 \u0932\u094b\u0928" // Hindi: तुरंत लोन
    );

    // HIGH RISK — electricity/utility scam
    private static final List<String> UTILITY_SCAM_PATTERNS = Arrays.asList(
            "electricity connection", "power will be disconnected", "bill pending",
            "pay immediately to avoid disconnection", "last warning",
            "\u0628\u093f\u091c\u0932\u0940 \u0915\u091f\u0928\u0947" // Hindi: बिजली कटने
    );

    // Urgency amplifiers — increase confidence when combined with above
    private static final List<String> URGENCY_WORDS = Arrays.asList(
            "immediately", "urgent", "last chance", "within 24 hours",
            "expire today", "action required", "click now", "call now",
            "\u0924\u0941\u0930\u0902\u0924", // Hindi: तुरंत
            "\u0905\u092d\u0940" // Hindi: अभी
    );

    private UpiSmsPatternEngine() {}

    /**
     * Scan an SMS message body for UPI/banking scam patterns.
     * This is an offline pattern match — instant, no network needed.
     *
     * @param senderName The SMS sender ID (e.g. VK-HDFCBK)
     * @param messageBody The SMS text content
     * @return UpiScanResult with isScamSuspected flag and Hindi+English warnings
     */
    public static UpiScanResult scan(String senderName, String messageBody) {
        if (messageBody == null || messageBody.isEmpty()) {
            return safe();
        }

        String lower = messageBody.toLowerCase(Locale.ENGLISH);
        String senderUpper = senderName != null ? senderName.toUpperCase(Locale.ENGLISH) : "";

        // Only deeply scan SMS from bank/UPI senders
        boolean isBankSender = isBankSender(senderUpper);

        int score = 0;
        String primaryWarningHindi = "";
        String primaryWarningEnglish = "";

        // Check collect request (highest priority — UPI collect = money goes OUT)
        if (containsAny(lower, COLLECT_REQUEST_PATTERNS)) {
            score += 70;
            primaryWarningHindi = "\u26a0\ufe0f UPI Collect Request: \u092f\u0939 \u0906\u092a\u0915\u0947 \u0916\u093e\u0924\u0947 \u0938\u0947 \u092a\u0948\u0938\u0947 \u0928\u093f\u0915\u093e\u0932\u0947\u0917\u093e!";
            primaryWarningEnglish = "\u26a0\ufe0f UPI Collect Request: This will TAKE money FROM your account, not send it to you!";
        }
        // KYC scam
        else if (containsAny(lower, KYC_PATTERNS)) {
            score += 60;
            primaryWarningHindi = "\u26a0\ufe0f KYC Scam: \u0905\u092a\u0928\u0940 \u0915\u094b\u0908 \u091c\u093e\u0928\u0915\u093e\u0930\u0940 \u0936\u0947\u092f\u0930 \u0928 \u0915\u0930\u0947\u0902";
            primaryWarningEnglish = "\u26a0\ufe0f KYC Fraud: Real banks never ask for KYC via SMS links";
        }
        // Prize/lottery
        else if (containsAny(lower, PRIZE_PATTERNS)) {
            score += 65;
            primaryWarningHindi = "\u26a0\ufe0f Lottery Scam: \u0915\u094b\u0908 \u0932\u0949\u091f\u0930\u0940 \u0928\u0939\u0940\u0902 \u0939\u0948 — \u0906\u092a\u0915\u0947 \u092a\u0948\u0938\u0947 \u091a\u0941\u0930\u093e\u0928\u0947 \u0915\u0940 \u0915\u094b\u0936\u093f\u0936 \u0939\u0948";
            primaryWarningEnglish = "\u26a0\ufe0f Prize Scam: No lottery exists — this is designed to steal your money";
        }
        // Loan scam
        else if (containsAny(lower, LOAN_SCAM_PATTERNS)) {
            score += 55;
            primaryWarningHindi = "\u26a0\ufe0f Loan Scam: \u0915\u094b\u0908 \u092b\u0940\u0938 \u0926\u0947\u0928\u0947 \u0938\u0947 \u092a\u0939\u0932\u0947 \u0905\u092a\u0928\u093e \u092c\u0948\u0902\u0915 \u0915\u0949\u0932 \u0915\u0930\u0947\u0902";
            primaryWarningEnglish = "\u26a0\ufe0f Loan Fraud: Legitimate lenders never ask for upfront fees via SMS";
        }
        // Electricity/utility
        else if (containsAny(lower, UTILITY_SCAM_PATTERNS)) {
            score += 55;
            primaryWarningHindi = "\u26a0\ufe0f Utility Scam: \u0905\u092a\u0928\u0940 \u0928\u093f\u0915\u091f\u0924\u092e \u0936\u093e\u0916\u093e \u0938\u0947 \u092c\u093f\u0932 \u0935\u0947\u0930\u093f\u092b\u093e\u0908 \u0915\u0930\u0947\u0902";
            primaryWarningEnglish = "\u26a0\ufe0f Utility Fraud: Always verify bills at your nearest office, not via SMS links";
        }

        // Add urgency amplifier
        if (score > 0 && containsAny(lower, URGENCY_WORDS)) {
            score = Math.min(100, score + 20);
        }

        if (score >= 55) {
            return new UpiScanResult(true, primaryWarningHindi, primaryWarningEnglish, score);
        }

        return safe();
    }

    public static boolean isBankSender(String senderUpper) {
        if (senderUpper == null) return false;
        for (String prefix : BANK_SENDER_PREFIXES) {
            if (senderUpper.startsWith(prefix)) return true;
        }
        for (String keyword : BANK_SENDER_KEYWORDS) {
            if (senderUpper.contains(keyword)) return true;
        }
        return false;
    }

    private static boolean containsAny(String text, List<String> patterns) {
        for (String pattern : patterns) {
            if (text.contains(pattern.toLowerCase(Locale.ENGLISH))) return true;
        }
        return false;
    }

    private static UpiScanResult safe() {
        return new UpiScanResult(false, "", "", 0);
    }
}
