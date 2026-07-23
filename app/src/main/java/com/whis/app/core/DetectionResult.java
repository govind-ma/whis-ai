package com.whis.app.core;

/**
 * The shared detection-result interface — MASTER_PLAN.md Section 3.1.
 * <p>
 * Implemented by:
 * <ul>
 *   <li>{@code WhisCallAnalysis} (Call module, {@code com.whis.app.call})</li>
 *   <li>{@code MsgDetectionResult} (MSG module, {@code com.whis.app.msg})</li>
 * </ul>
 * <p>
 * This is a locked contract. Do NOT add fields here without updating
 * MASTER_PLAN.md Section 3.1 and notifying all module owners.
 */
public interface DetectionResult {

    /**
     * Source module that produced this result.
     *
     * @return {@code "CALL"} or {@code "SMS"}
     */
    String getSourceType();

    /**
     * Risk score on a 0–100 scale.
     * <p>
     * <b>HIGHER = MORE dangerous.</b> 0 = confirmed safe, 100 = confirmed scam.
     * (See MASTER_PLAN.md Section 2.2 — canonical direction.)
     *
     * @return integer in [0, 100]
     */
    int getRiskScore();

    /**
     * The canonical verdict tier.
     *
     * @return one of the {@link WhisVerdict} enum values
     */
    WhisVerdict getVerdict();

    /**
     * Primary human-readable reason, one line.
     * <p>
     * Example: "Number reported 47 times as fraud by community"
     *
     * @return non-null reason string
     */
    String getReasonText();

    /**
     * Reused generic identifier-type slot.
     * <p>
     * Values include: {@code "CONTACT"}, {@code "DLT_REGISTERED"},
     * {@code "1600_SERIES"}, {@code "UNKNOWN_MOBILE"}, etc.
     * <p>
     * One field, shared across sources, per the established pattern
     * of not proliferating a new field per source.
     *
     * @return non-null identifier type string
     */
    String getIdentifierType();

    /**
     * Epoch millisecond timestamp of when this detection was produced.
     *
     * @return timestamp in milliseconds since epoch
     */
    long getTimestamp();
}
