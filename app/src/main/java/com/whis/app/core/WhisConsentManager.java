package com.whis.app.core;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Shared consent manager — MASTER_PLAN.md Section 3.5.
 * <p>
 * One SharedPreferences key ({@code whis_consent_given}), one screen,
 * shown once during UI module's onboarding. Call, MSG, and AI Agent all
 * read {@link #isConsentGiven(Context)} instead of building their own
 * consent dialogs or SharedPreferences keys.
 * <p>
 * This is a locked contract. Do NOT change the preference key or file
 * name without updating MASTER_PLAN.md Section 3.5 and notifying all
 * module owners.
 */
public class WhisConsentManager {

    private static final String PREFS_NAME = "whis_consent";
    private static final String KEY_CONSENT_GIVEN = "whis_consent_given";

    private WhisConsentManager() {
        // Static-only utility — do not instantiate.
    }

    /**
     * Check whether the user has given consent during onboarding.
     *
     * @param context any context (application or activity)
     * @return {@code true} if consent has been recorded, {@code false} otherwise
     */
    public static boolean isConsentGiven(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_CONSENT_GIVEN, false);
    }

    /**
     * Record the user's consent decision.
     * <p>
     * Called by UI module's onboarding flow after the user taps
     * "I understand and agree" on the single consent screen.
     *
     * @param context any context
     * @param given   {@code true} to record consent, {@code false} to revoke
     */
    public static void saveConsent(Context context, boolean given) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_CONSENT_GIVEN, given).apply();
    }

    public static void setConsentGiven(Context context, boolean given) {
        saveConsent(context, given);
    }
}
