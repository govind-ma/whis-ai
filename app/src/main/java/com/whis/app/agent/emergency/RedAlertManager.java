package com.whis.app.agent.emergency;

import com.whis.app.agent.model.RiskLevel;

/**
 * Instant API-free Red Alert keyword risk assessor (AI_AGENT_PLAN.md Section 4.5 & Acceptance Criteria).
 */
public class RedAlertManager {

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

    private RedAlertManager() {
        // Utility class
    }

    public static RiskLevel assess(String message) {
        if (message == null || message.trim().isEmpty()) {
            return RiskLevel.LOW;
        }

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
