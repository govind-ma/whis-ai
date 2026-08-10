package com.whis.app.core;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences storage for 2 emergency contacts.
 */
public class EmergencyContactStore {

    private static final String PREFS_NAME = "whis_emergency_contacts";
    private static final String KEY_C1_NAME = "contact1_name";
    private static final String KEY_C1_PHONE = "contact1_phone";
    private static final String KEY_C2_NAME = "contact2_name";
    private static final String KEY_C2_PHONE = "contact2_phone";

    private EmergencyContactStore() {}

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Extracts clean 10-digit core number from any raw string (strips country codes +91, +1, 0, spaces, etc.). */
    public static String extract10Digits(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() >= 10) {
            return digits.substring(digits.length() - 10);
        }
        return digits;
    }

    /** Formats a phone string into standard +91 XXXXXXXXXX format. Returns raw trimmed if invalid. */
    public static String formatPhoneNumber(String raw) {
        if (raw == null) return "";
        String digits = extract10Digits(raw);
        if (digits.length() == 10) {
            return "+91 " + digits;
        }
        return raw.trim();
    }

    /** Returns true if input contains a valid 10-digit core phone number. */
    public static boolean isValid10DigitPhone(String raw) {
        return extract10Digits(raw).length() == 10;
    }

    public static void saveContacts(Context context, String c1Name, String c1Phone, String c2Name, String c2Phone) {
        String formattedP1 = isValid10DigitPhone(c1Phone) ? formatPhoneNumber(c1Phone) : (c1Phone != null ? c1Phone.trim() : "");
        String formattedP2 = isValid10DigitPhone(c2Phone) ? formatPhoneNumber(c2Phone) : (c2Phone != null ? c2Phone.trim() : "");

        getPrefs(context).edit()
                .putString(KEY_C1_NAME, c1Name != null ? c1Name.trim() : "")
                .putString(KEY_C1_PHONE, formattedP1)
                .putString(KEY_C2_NAME, c2Name != null ? c2Name.trim() : "")
                .putString(KEY_C2_PHONE, formattedP2)
                .apply();
    }

    public static String getC1Name(Context context) {
        return getPrefs(context).getString(KEY_C1_NAME, "");
    }

    public static String getC1Phone(Context context) {
        return getPrefs(context).getString(KEY_C1_PHONE, "");
    }

    public static String getC1CleanDigits(Context context) {
        return extract10Digits(getC1Phone(context));
    }

    public static String getC2Name(Context context) {
        return getPrefs(context).getString(KEY_C2_NAME, "");
    }

    public static String getC2Phone(Context context) {
        return getPrefs(context).getString(KEY_C2_PHONE, "");
    }

    public static String getC2CleanDigits(Context context) {
        return extract10Digits(getC2Phone(context));
    }
}
