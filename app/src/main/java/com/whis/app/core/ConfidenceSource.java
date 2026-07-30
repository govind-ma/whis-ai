package com.whis.app.core;

/**
 * Indicates the underlying confidence source of a detection result (UI_PLAN.md §1.6 / §3.3).
 * <p>
 * Dictates whether strict certainty copy ("verified", "confirmed") or softer copy
 * ("matches a known pattern", "reported by other users") is used in alerts.
 */
public enum ConfidenceSource {
    /** Matched against local device contacts — strict certainty language allowed. */
    CONTACT_MATCH,

    /** Matched DLT header or 1600-series official sender — strict certainty language allowed. */
    VERIFIED_SERIES,

    /** Flagged by community reports — soft copy required. */
    COMMUNITY_REPORT,

    /** Matched keyword / regex / heuristic scam pattern — soft copy required. */
    PATTERN_MATCH,

    /** Evaluated by AI Agent classification — soft copy required. */
    AI_ANALYSIS
}
