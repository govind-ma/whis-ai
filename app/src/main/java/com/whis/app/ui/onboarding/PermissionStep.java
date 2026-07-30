package com.whis.app.ui.onboarding;

/**
 * Defines the 5 permission wizard steps (UI_PLAN.md §2.2 step 7).
 * <p>
 * Each step carries its plain-language title, explanation, button label,
 * and a completion flag. Steps are ordered by dependency — the order
 * matters for UX but no step blocks progression if skipped.
 */
public enum PermissionStep {

    CALLER_ID_ROLE(
            1,
            "Default Caller ID & Spam app",
            "This lets Whis check incoming calls for scam patterns before your phone "
                    + "even rings. Without this, Whis can't see who's calling or warn you in time.\n\n"
                    + "You're setting Whis as your Caller ID app — not replacing your phone or dialer.",
            "Enable Caller ID"
    ),

    NOTIFICATION_ACCESS(
            2,
            "Notification Access",
            "This lets Whis read incoming SMS notifications to detect scam messages "
                    + "in real time — before you open them or click any links.\n\n"
                    + "Whis only looks for danger patterns. It does not store, read, or share "
                    + "your personal messages.",
            "Grant Notification Access"
    ),

    BATTERY_AUTOSTART(
            3,
            "Keep Whis running",
            "Some phone brands stop background apps to save battery. If Whis is stopped, "
                    + "it can't protect you from scam calls or messages while your phone is locked.\n\n"
                    + "This setting tells your phone to let Whis keep running in the background.",
            "Open Battery Settings"
    ),

    FULL_SCREEN_ALERT(
            4,
            "Full-screen scam alerts",
            "When Whis detects a dangerous call or message, it needs to show a "
                    + "full-screen alert immediately — even if your phone is locked.\n\n"
                    + "Without this, you'll only see a small notification that's easy to miss "
                    + "during a live scam.",
            "Allow Full-Screen Alerts"
    ),

    DND_BYPASS(
            5,
            "Alert even in Do Not Disturb",
            "If your phone is in Do Not Disturb mode and a scam call comes in, "
                    + "Whis still needs to warn you.\n\n"
                    + "This lets Whis's critical safety alerts break through DND — "
                    + "nothing else from Whis will disturb you.",
            "Allow DND Bypass"
    );

    public final int stepNumber;
    public final String title;
    public final String explanation;
    public final String buttonLabel;

    PermissionStep(int stepNumber, String title, String explanation, String buttonLabel) {
        this.stepNumber = stepNumber;
        this.title = title;
        this.explanation = explanation;
        this.buttonLabel = buttonLabel;
    }

    /** Total number of permission steps. */
    public static final int TOTAL_STEPS = values().length;

    /** Get the next step, or null if this is the last. */
    public PermissionStep next() {
        int nextOrd = ordinal() + 1;
        PermissionStep[] all = values();
        return nextOrd < all.length ? all[nextOrd] : null;
    }
}
