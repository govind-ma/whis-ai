package com.whis.app.call;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * SharedPreferences-based persistence for AI-analysed call history.
 * <p>
 * Stores up to {@link #MAX_ENTRIES} call records (LIFO, oldest pruned automatically).
 * Each entry is a JSON object with: phoneNumber, contactName, riskLevel, category,
 * reason, badge, timestamp.
 * <p>
 * Thread-safe for concurrent reads; writes are serialised via {@code synchronized}.
 */
public class CallHistoryStore {

    private static final String TAG        = "CallHistoryStore";
    private static final String PREFS_NAME = "whis_call_history";
    private static final String KEY_CALLS  = "calls_json";
    private static final int    MAX_ENTRIES = 100;

    // ── Entry model ───────────────────────────────────────────────────────────

    public static class CallEntry {
        public final String phoneNumber;
        public final String contactName;
        public final String riskLevel;   // "SAFE" | "SUSPICIOUS" | "SCAM"
        public final String category;
        public final String reason;
        public final String badge;       // "Trusted" | "Suspicious" | "Scam Detected"
        public final long   timestamp;

        public CallEntry(String phoneNumber, String contactName, String riskLevel,
                         String category, String reason, String badge, long timestamp) {
            this.phoneNumber = phoneNumber;
            this.contactName = contactName;
            this.riskLevel   = riskLevel;
            this.category    = category;
            this.reason      = reason;
            this.badge       = badge;
            this.timestamp   = timestamp;
        }
    }

    private CallHistoryStore() {}

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Prepend a new call entry (most recent first). Prunes oldest if over limit.
     */
    public static synchronized void save(Context context, CallEntry entry) {
        try {
            SharedPreferences prefs = prefs(context);
            String existing = prefs.getString(KEY_CALLS, "[]");
            JSONArray arr = new JSONArray(existing);

            JSONObject obj = new JSONObject();
            obj.put("phoneNumber", entry.phoneNumber);
            obj.put("contactName", entry.contactName);
            obj.put("riskLevel",   entry.riskLevel);
            obj.put("category",    entry.category);
            obj.put("reason",      entry.reason);
            obj.put("badge",       entry.badge);
            obj.put("timestamp",   entry.timestamp);

            // Build new array with new entry at index 0
            JSONArray updated = new JSONArray();
            updated.put(obj);
            for (int i = 0; i < arr.length() && updated.length() < MAX_ENTRIES; i++) {
                updated.put(arr.getJSONObject(i));
            }

            prefs.edit().putString(KEY_CALLS, updated.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save call entry", e);
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Return all stored call entries (most recent first).
     */
    public static List<CallEntry> getAll(Context context) {
        List<CallEntry> entries = new ArrayList<>();
        try {
            String json = prefs(context).getString(KEY_CALLS, "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                entries.add(new CallEntry(
                        obj.optString("phoneNumber", ""),
                        obj.optString("contactName", "Unknown"),
                        obj.optString("riskLevel",   "SUSPICIOUS"),
                        obj.optString("category",    "UNKNOWN"),
                        obj.optString("reason",      ""),
                        obj.optString("badge",       "Suspicious"),
                        obj.optLong("timestamp",     0L)
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read call history", e);
        }
        return entries;
    }

    /**
     * Clear all stored history.
     */
    public static void clear(Context context) {
        prefs(context).edit().remove(KEY_CALLS).apply();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
