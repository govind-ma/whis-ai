package com.whis.app.core;

/**
 * The shared verdict enum — MASTER_PLAN.md Section 3.2.
 * <p>
 * Five tiers, ordered from safest to most dangerous:
 * <ol>
 *   <li>{@link #TRUSTED} — contacts, or fully confirmed safe</li>
 *   <li>{@link #LIKELY_SAFE} — verified series/DLT sender</li>
 *   <li>{@link #UNKNOWN} — no signal either way</li>
 *   <li>{@link #SUSPICIOUS} — worth a second look</li>
 *   <li>{@link #HIGH_RISK} — confirmed or near-confirmed scam</li>
 * </ol>
 * <p>
 * This is a locked contract. Do NOT add values here without updating
 * MASTER_PLAN.md Section 3.2 and notifying all module owners.
 */
public enum WhisVerdict {

    /**
     * Contacts, or fully confirmed safe.
     * Call module: no overlay shown at all.
     */
    TRUSTED,

    /**
     * Verified series/DLT sender, shown with a reassuring tip.
     */
    LIKELY_SAFE,

    /**
     * Genuinely no signal either way.
     * No notification surfaced (Call); MSG should rarely if ever emit this —
     * resolve to a tier if possible.
     */
    UNKNOWN,

    /**
     * Worth a second look. Notification/warning shown.
     */
    SUSPICIOUS,

    /**
     * Confirmed or near-confirmed scam. Strongest warning shown.
     */
    HIGH_RISK,

    /**
     * User has explicitly blocked this number. Calls will be silently rejected.
     * Displayed as a dark "Blocked" pill in the activity feed.
     */
    BLOCKED
}
