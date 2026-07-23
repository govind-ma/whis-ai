package com.whis.app.core;

/**
 * Firestore {@code community_reports} collection schema reference —
 * MASTER_PLAN.md Section 3.4.
 * <p>
 * <b>This is a reference file, not executable code.</b> It documents the
 * canonical Firestore document structure so whoever writes to
 * {@code community_reports} later (Call module, MSG module) doesn't
 * have to re-derive field names from MASTER_PLAN.md.
 *
 * <h2>Collection: {@code community_reports}</h2>
 * <p>
 * Document ID: {@code {normalizedIdentifier}}
 * <ul>
 *   <li>{@code normalizedIdentifier} = E.164 phone number for CALL,
 *       or sender header for SMS</li>
 * </ul>
 *
 * <h3>Fields</h3>
 * <table>
 *   <tr><th>Field</th><th>Type</th><th>Description</th></tr>
 *   <tr>
 *     <td>{@code sourceType}</td>
 *     <td>string</td>
 *     <td>{@code "CALL"} or {@code "SMS"} — added to Call's original
 *         schema so one collection serves both modules</td>
 *   </tr>
 *   <tr>
 *     <td>{@code reportCount}</td>
 *     <td>int</td>
 *     <td>Number of times this identifier has been reported</td>
 *   </tr>
 *   <tr>
 *     <td>{@code lastReportedAt}</td>
 *     <td>timestamp</td>
 *     <td>Firestore Timestamp of the most recent report</td>
 *   </tr>
 *   <tr>
 *     <td>{@code reportedPatterns}</td>
 *     <td>[string]</td>
 *     <td>Array of reported scam pattern descriptions</td>
 *   </tr>
 *   <tr>
 *     <td>{@code confirmedScam}</td>
 *     <td>boolean</td>
 *     <td>{@code true} when {@code reportCount >= 10}</td>
 *   </tr>
 *   <tr>
 *     <td>{@code riskScoreDelta}</td>
 *     <td>int</td>
 *     <td>Pre-computed score adjustment (POSITIVE, since canonical
 *         direction is higher = worse — Call module must invert its
 *         original negative trustDelta convention)</td>
 *   </tr>
 * </table>
 *
 * <h3>Privacy</h3>
 * <ul>
 *   <li>No names, no user IDs, no device IDs stored</li>
 * </ul>
 *
 * <h3>Firestore Security Rules</h3>
 * <ul>
 *   <li>Authenticated Whis users: write</li>
 *   <li>Open: read</li>
 * </ul>
 *
 * <h3>Field Name Constants</h3>
 * Use the constants below when constructing Firestore document maps
 * to avoid typo-induced schema drift across modules.
 */
public final class FirestoreSchema {

    private FirestoreSchema() {
        // Reference-only — do not instantiate.
    }

    // ── Collection name ──────────────────────────────────────────────────

    /** Firestore collection path. */
    public static final String COLLECTION_COMMUNITY_REPORTS = "community_reports";

    // ── Field names ──────────────────────────────────────────────────────

    /** {@code "CALL"} or {@code "SMS"} */
    public static final String FIELD_SOURCE_TYPE = "sourceType";

    /** int — number of times reported */
    public static final String FIELD_REPORT_COUNT = "reportCount";

    /** Firestore Timestamp — most recent report */
    public static final String FIELD_LAST_REPORTED_AT = "lastReportedAt";

    /** [string] — reported scam pattern descriptions */
    public static final String FIELD_REPORTED_PATTERNS = "reportedPatterns";

    /** boolean — true when reportCount >= 10 */
    public static final String FIELD_CONFIRMED_SCAM = "confirmedScam";

    /** int — pre-computed score adjustment (positive = worse) */
    public static final String FIELD_RISK_SCORE_DELTA = "riskScoreDelta";
}
