package com.whis.app.call;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Persistent store for user-blocked phone numbers.
 * <p>
 * Backed by SharedPreferences so blocked numbers survive app restarts.
 * Used by:
 * <ul>
 *   <li>{@link CallFilterService} — to silently reject calls from blocked numbers</li>
 *   <li>{@link com.whis.app.ui.calls.CallsFragment} — to show/manage the blocked list</li>
 *   <li>Block & Protect action in alert bottom sheets</li>
 * </ul>
 */
public class BlockedNumberStore {

    private static final String TAG        = "BlockedNumberStore";
    private static final String PREFS_NAME = "whis_blocked_numbers";
    private static final String KEY_SET    = "blocked_set";

    private BlockedNumberStore() {}

    /** Block a number. No-op if already blocked. */
    public static synchronized void block(Context context, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) return;
        String normalized = normalize(phoneNumber);
        Set<String> current = load(context);
        current.add(normalized);
        save(context, current);
        Log.d(TAG, "Blocked: " + normalized);
    }

    /** Unblock a number. No-op if not blocked. */
    public static synchronized void unblock(Context context, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) return;
        String normalized = normalize(phoneNumber);
        Set<String> current = load(context);
        current.remove(normalized);
        save(context, current);
        Log.d(TAG, "Unblocked: " + normalized);
    }

    /** Returns true if the given number is in the blocked list. */
    public static boolean isBlocked(Context context, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) return false;
        return load(context).contains(normalize(phoneNumber));
    }

    /** Returns all blocked numbers as a list (insertion order preserved). */
    public static List<String> getAll(Context context) {
        return new ArrayList<>(load(context));
    }

    /** Clear all blocked numbers. */
    public static synchronized void clearAll(Context context) {
        prefs(context).edit().remove(KEY_SET).apply();
        Log.d(TAG, "All blocked numbers cleared");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Strips spaces, dashes, country code (+91, 91, 0) for 10-digit core comparison. */
    public static String normalize(String number) {
        if (number == null) return "";
        String digits = number.replaceAll("[^0-9]", "");
        if (digits.startsWith("91") && digits.length() == 12) {
            return digits.substring(2);
        }
        if (digits.startsWith("0") && digits.length() == 11) {
            return digits.substring(1);
        }
        if (digits.length() > 10) {
            return digits.substring(digits.length() - 10);
        }
        return digits;
    }

    private static Set<String> load(Context context) {
        SharedPreferences prefs = prefs(context);
        Set<String> raw = prefs.getStringSet(KEY_SET, null);
        // Must copy — SharedPreferences returns a live reference that cannot be mutated
        return raw != null ? new LinkedHashSet<>(raw) : new LinkedHashSet<>();
    }

    private static void save(Context context, Set<String> numbers) {
        prefs(context).edit().putStringSet(KEY_SET, numbers).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
