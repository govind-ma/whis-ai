package com.whis.app.ui.onboarding;

import java.util.HashSet;
import java.util.Set;

/**
 * Simple data model holding all fields collected during onboarding (UI_PLAN.md §2.2).
 * <p>
 * Passed forward between fragments via the hosting {@link OnboardingActivity}.
 * Persistence is NOT wired yet — this is an in-memory container only.
 */
public class OnboardingData {

    // ── Step 2: Language ─────────────────────────────────────────────────
    /** Selected language: "English", "Hindi", or "Gujarati". */
    public String language = "English";

    // ── Step 3: Profile ──────────────────────────────────────────────────
    /** User's name (free text). */
    public String name = "";

    /** Age group — large single-select: "18-30", "31-45", "46-60", "60+". */
    public String ageGroup = "";

    /** Tech comfort level — two-button choice: "Basic" or "Advanced". */
    public String techLevel = "Basic";

    // ── Step 4: UPI ──────────────────────────────────────────────────────
    /** Primary UPI app — single-select: "GPay", "PhonePe", "Paytm", "BHIM", "Other". */
    public String primaryUpi = "";

    // ── Step 5: Emergency contact ────────────────────────────────────────
    /** Emergency contact name. */
    public String emergencyContactName = "";

    /** Emergency contact phone number. */
    public String emergencyContactPhone = "";

    // ── Step 6: Consent ──────────────────────────────────────────────────
    /** Community reporting opt-in (the only optional toggle per §2.3). */
    public boolean communityReportingOptIn = true;

    // ── Step 7: Permission wizard ────────────────────────────────────────
    /**
     * Permission steps the user granted (by {@link PermissionStep} name).
     * Not a guarantee the system permission is actually enabled — just that
     * the user went through the settings screen and came back.
     */
    public Set<String> permissionsGranted = new HashSet<>();

    /**
     * Permission steps the user explicitly skipped.
     * These should be flagged as incomplete on a later status screen.
     */
    public Set<String> permissionsSkipped = new HashSet<>();
}
