package com.whis.app.agent;

import android.content.Context;
import android.content.Intent;

/**
 * Static launcher entry points exposed for Call, Message, and UI modules (AI_AGENT_PLAN.md Section 4.9 & Acceptance Criteria).
 */
public class AgentLauncher {

    private AgentLauncher() {
        // Utility class
    }

    /**
     * Launch AI Agent chat cleanly.
     */
    public static void launch(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, AgentActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Launch AI Agent pre-populated with context from a flagged SMS.
     *
     * @param context    android context
     * @param smsContent message text of flagged SMS
     * @param sender     sender header of flagged SMS
     */
    public static void launchWithSmsContext(Context context, String smsContent, String sender) {
        if (context == null) return;
        Intent intent = new Intent(context, AgentActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String initialContext = "Flagged SMS received from " + (sender != null ? sender : "Unknown")
                + ": \"" + (smsContent != null ? smsContent : "") + "\". What should I do?";
        intent.putExtra("initial_context", initialContext);
        context.startActivity(intent);
    }

    /**
     * Launch AI Agent pre-populated with context from a flagged Call.
     *
     * @param context     android context
     * @param phoneNumber phone number of flagged call
     */
    public static void launchWithCallContext(Context context, String phoneNumber) {
        if (context == null) return;
        Intent intent = new Intent(context, AgentActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String initialContext = "Suspicious call detected from " + (phoneNumber != null ? phoneNumber : "Unknown")
                + ". What steps should I take?";
        intent.putExtra("initial_context", initialContext);
        context.startActivity(intent);
    }
}
