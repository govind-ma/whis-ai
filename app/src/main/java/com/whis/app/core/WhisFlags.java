package com.whis.app.core;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper for reading/writing the {@code whis_flags} SharedPreferences
 * format defined in MASTER_PLAN.md Section 3.3.
 * <p>
 * This is the single entry point Call and MSG modules use to record
 * flagged items, instead of each hand-rolling their own SharedPreferences
 * JSON logic.
 * <p>
 * <b>Storage format</b> — a JSON array stored as a string under key
 * {@code "flags"} in SharedPreferences file {@code "whis_flags"}:
 * <pre>{@code
 * [
 *   {
 *     "type": "SMS",
 *     "content": "flagged message text",  // SMS only
 *     "sender": "VM-SBIBNK",             // SMS only
 *     "number": "+919876543210",          // CALL only
 *     "timestamp": 1753048200000,
 *     "risk": "HIGH"
 *   }
 * ]
 * }</pre>
 * <p>
 * <b>Mapping from {@link WhisVerdict} to the {@code risk} string</b>
 * (per MASTER_PLAN.md Section 3.3):
 * <ul>
 *   <li>{@link WhisVerdict#SUSPICIOUS} → {@code "MEDIUM"}</li>
 *   <li>{@link WhisVerdict#HIGH_RISK} → {@code "HIGH"}</li>
 *   <li>All other verdicts → <em>not written</em></li>
 * </ul>
 * {@code "CRITICAL"} is reserved exclusively for AI Agent's own
 * {@code RedAlertManager} keyword detection — Call and MSG modules
 * never write that value.
 */
public class WhisFlags {

    private static final String PREFS_NAME = "whis_flags";
    private static final String KEY_FLAGS = "flags";

    // JSON field names
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_NUMBER = "number";
    private static final String FIELD_SENDER = "sender";
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_RISK = "risk";

    // Risk string constants
    private static final String RISK_MEDIUM = "MEDIUM";
    private static final String RISK_HIGH = "HIGH";

    private WhisFlags() {
        // Static-only utility — do not instantiate.
    }

    /**
     * Add a flag entry to the {@code whis_flags} SharedPreferences store.
     * <p>
     * Only {@link WhisVerdict#SUSPICIOUS} and {@link WhisVerdict#HIGH_RISK}
     * verdicts produce a write. All other verdicts are silently ignored —
     * the AI Agent does not need context injection for calls/messages that
     * weren't concerning.
     *
     * @param context    any context
     * @param type       {@code "CALL"} or {@code "SMS"}
     * @param identifier for CALL: the phone number (stored in {@code "number"} field);
     *                   for SMS: the sender header/number (stored in {@code "sender"} field).
     *                   Additionally, for SMS, this is also used as the {@code "content"}
     *                   field unless overridden by
     *                   {@link #addSmsFlag(Context, String, String, String, WhisVerdict)}.
     * @param reasonText the human-readable reason (not stored in whis_flags directly,
     *                   but provided for API consistency — the flag format defined in
     *                   MASTER_PLAN.md Section 3.3 does not include a reason field)
     * @param verdict    the canonical verdict; only SUSPICIOUS and HIGH_RISK are written
     */
    public static void addFlag(Context context, String type, String identifier,
                               String reasonText, WhisVerdict verdict) {
        String risk = mapVerdictToRisk(verdict);
        if (risk == null) {
            // TRUSTED, LIKELY_SAFE, UNKNOWN — don't write.
            return;
        }

        try {
            JSONObject flag = new JSONObject();
            flag.put(FIELD_TYPE, type);
            flag.put(FIELD_TIMESTAMP, System.currentTimeMillis());
            flag.put(FIELD_RISK, risk);

            if ("CALL".equals(type)) {
                flag.put(FIELD_NUMBER, identifier);
            } else if ("SMS".equals(type)) {
                flag.put(FIELD_SENDER, identifier);
                // When using the simple addFlag API for SMS, content defaults
                // to empty — use addSmsFlag for full SMS flags with message text.
                flag.put(FIELD_CONTENT, "");
            }

            appendFlag(context, flag);
        } catch (JSONException e) {
            // Should never happen with string/long puts, but don't crash the caller.
        }
    }

    /**
     * Add a full SMS flag entry including the flagged message content.
     * <p>
     * This is the preferred method for the MSG module to use, since the
     * {@code whis_flags} format for SMS includes both {@code "sender"}
     * and {@code "content"} fields.
     *
     * @param context     any context
     * @param sender      the SMS sender header (e.g., {@code "VM-SBIBNK"})
     * @param content     the flagged message text
     * @param reasonText  human-readable reason (for API consistency)
     * @param verdict     the canonical verdict; only SUSPICIOUS and HIGH_RISK are written
     */
    public static void addSmsFlag(Context context, String sender, String content,
                                  String reasonText, WhisVerdict verdict) {
        String risk = mapVerdictToRisk(verdict);
        if (risk == null) {
            return;
        }

        try {
            JSONObject flag = new JSONObject();
            flag.put(FIELD_TYPE, "SMS");
            flag.put(FIELD_CONTENT, content != null ? content : "");
            flag.put(FIELD_SENDER, sender != null ? sender : "");
            flag.put(FIELD_TIMESTAMP, System.currentTimeMillis());
            flag.put(FIELD_RISK, risk);

            appendFlag(context, flag);
        } catch (JSONException e) {
            // Defensive — don't crash the caller.
        }
    }

    /**
     * Retrieve all stored flag entries.
     *
     * @param context any context
     * @return list of {@link FlagEntry} objects, newest last; empty list if
     *         none stored or on parse error
     */
    public static List<FlagEntry> getFlags(Context context) {
        List<FlagEntry> result = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_FLAGS, "[]");

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                FlagEntry entry = new FlagEntry(
                        obj.optString(FIELD_TYPE, ""),
                        obj.optString(FIELD_CONTENT, null),
                        obj.optString(FIELD_NUMBER, null),
                        obj.optString(FIELD_SENDER, null),
                        obj.optLong(FIELD_TIMESTAMP, 0L),
                        obj.optString(FIELD_RISK, "")
                );
                result.add(entry);
            }
        } catch (JSONException e) {
            // Corrupted data — return empty list rather than crashing.
        }

        return result;
    }

    // ── Internal helpers ─────────────────────────────────────────────────

    /**
     * Map a {@link WhisVerdict} to the risk string per MASTER_PLAN.md Section 3.3.
     *
     * @return {@code "MEDIUM"} for SUSPICIOUS, {@code "HIGH"} for HIGH_RISK,
     *         {@code null} for everything else (meaning: don't write)
     */
    private static String mapVerdictToRisk(WhisVerdict verdict) {
        if (verdict == null) {
            return null;
        }
        switch (verdict) {
            case SUSPICIOUS:
                return RISK_MEDIUM;
            case HIGH_RISK:
                return RISK_HIGH;
            default:
                return null;
        }
    }

    /**
     * Append a single JSON flag object to the stored array.
     */
    private static void appendFlag(Context context, JSONObject flag) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String existing = prefs.getString(KEY_FLAGS, "[]");

        try {
            JSONArray array = new JSONArray(existing);
            array.put(flag);
            prefs.edit().putString(KEY_FLAGS, array.toString()).apply();
        } catch (JSONException e) {
            // If existing data is corrupted, start fresh with just this flag.
            JSONArray fresh = new JSONArray();
            fresh.put(flag);
            prefs.edit().putString(KEY_FLAGS, fresh.toString()).apply();
        }
    }

    // ── Data class ───────────────────────────────────────────────────────

    /**
     * Parsed representation of a single flag entry from {@code whis_flags}.
     */
    public static class FlagEntry {

        private final String type;
        private final String content;   // SMS only (nullable)
        private final String number;    // CALL only (nullable)
        private final String sender;    // SMS only (nullable)
        private final long timestamp;
        private final String risk;      // "MEDIUM" | "HIGH" | "CRITICAL"

        public FlagEntry(String type, String content, String number,
                         String sender, long timestamp, String risk) {
            this.type = type;
            this.content = content;
            this.number = number;
            this.sender = sender;
            this.timestamp = timestamp;
            this.risk = risk;
        }

        public String getType() { return type; }
        public String getContent() { return content; }
        public String getNumber() { return number; }
        public String getSender() { return sender; }
        public long getTimestamp() { return timestamp; }
        public String getRisk() { return risk; }
    }
}
