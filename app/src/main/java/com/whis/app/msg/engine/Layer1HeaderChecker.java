package com.whis.app.msg.engine;

import android.content.Context;

import com.whis.app.core.ContactLookupUtil;
import com.whis.app.msg.model.MsgCategory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Layer 1 — Header & DLT Verification (MSG_PLAN.md Section 2.1 & 4.2).
 * <p>
 * Checks TRAI DLT headers ({@code XX-XXXXXX-T/S/P/G}) and checks contacts via shared
 * {@link ContactLookupUtil}.
 */
public class Layer1HeaderChecker {

    // TRAI DLT Suffix Regex: 2-letter operator prefix + hyphen + 3-8 char sender + hyphen + suffix (T/S/P/G)
    // E.g., VM-SBIBNK-T, VD-HDFCBK-S, AX-POLICE-G, VK-AMAZON-P
    private static final Pattern DLT_SUFFIX_PATTERN = Pattern.compile(
            "^[A-Za-z]{2}-[A-Za-z0-9]{3,8}-([TSPGtspg])$"
    );

    // Standard TRAI DLT Header Regex: 2-letter operator prefix + hyphen + 3-8 char header (e.g., VM-SBIBNK, AD-HDFCBK)
    private static final Pattern DLT_STANDARD_PATTERN = Pattern.compile(
            "^[A-Za-z]{2}-[A-Za-z0-9]{3,8}$"
    );

    // Bare 6-8 character Sender Header Regex (e.g. SBIBNK, HDFCBK)
    private static final Pattern DLT_ALPHA_HEADER_PATTERN = Pattern.compile(
            "^[A-Za-z]{6,8}$"
    );

    public static class HeaderResult {
        public final boolean isContact;
        public final boolean dltVerified;
        public final String suffixType; // "T", "S", "P", "G", or null
        public final String identifierType;
        public final MsgCategory categoryHint;
        public final int baseRiskScore;

        public HeaderResult(boolean isContact, boolean dltVerified, String suffixType,
                            String identifierType, MsgCategory categoryHint, int baseRiskScore) {
            this.isContact = isContact;
            this.dltVerified = dltVerified;
            this.suffixType = suffixType;
            this.identifierType = identifierType;
            this.categoryHint = categoryHint;
            this.baseRiskScore = baseRiskScore;
        }
    }

    private Layer1HeaderChecker() {
        // Utility class
    }

    /**
     * Perform Layer 1 header check.
     */
    public static HeaderResult check(Context context, String sender) {
        if (sender == null || sender.trim().isEmpty()) {
            return new HeaderResult(false, false, null, "UNKNOWN_SENDER", MsgCategory.GENERAL, 30);
        }

        String trimmedSender = sender.trim();

        // 1. Contact check via shared ContactLookupUtil
        if (context != null) {
            ContactLookupUtil.ContactResult contactResult = ContactLookupUtil.check(trimmedSender, context);
            if (contactResult.isContact()) {
                return new HeaderResult(true, true, null, "CONTACT", MsgCategory.ALLOWED, 0);
            }
        }

        // 2. TRAI DLT Suffix Regulation Check (TCCCPR 2025 Amendment: XX-XXXXXX-T/S/P/G)
        Matcher matcher = DLT_SUFFIX_PATTERN.matcher(trimmedSender);
        if (matcher.matches()) {
            String suffix = matcher.group(1).toUpperCase();
            switch (suffix) {
                case "T":
                    return new HeaderResult(false, true, "T", "DLT_TRANSACTIONAL", MsgCategory.TRANSACTION, 0);
                case "S":
                    return new HeaderResult(false, true, "S", "DLT_SERVICE", MsgCategory.NOTIFICATION, 5);
                case "P":
                    return new HeaderResult(false, true, "P", "DLT_PROMOTIONAL", MsgCategory.PROMOTION, 15);
                case "G":
                    return new HeaderResult(false, true, "G", "DLT_GOVERNMENT", MsgCategory.NOTIFICATION, 0);
            }
        }

        // 3. Standard TRAI Header Format (e.g. VM-SBIBNK)
        if (DLT_STANDARD_PATTERN.matcher(trimmedSender).matches()) {
            return new HeaderResult(false, true, null, "DLT_TRANSACTIONAL", MsgCategory.TRANSACTION, 0);
        }

        // 4. Alpha Sender Header (e.g. SBIBNK)
        if (DLT_ALPHA_HEADER_PATTERN.matcher(trimmedSender).matches()) {
            return new HeaderResult(false, true, null, "DLT_ALPHA_HEADER", MsgCategory.NOTIFICATION, 5);
        }

        // 5. Standard 10-digit mobile or unknown sender header
        return new HeaderResult(false, false, null, "UNKNOWN_SENDER", MsgCategory.GENERAL, 30);
    }
}
