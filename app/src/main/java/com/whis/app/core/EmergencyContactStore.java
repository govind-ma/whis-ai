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

    public static void saveContacts(Context context, String c1Name, String c1Phone, String c2Name, String c2Phone) {
        getPrefs(context).edit()
                .putString(KEY_C1_NAME, c1Name != null ? c1Name.trim() : "")
                .putString(KEY_C1_PHONE, c1Phone != null ? c1Phone.trim() : "")
                .putString(KEY_C2_NAME, c2Name != null ? c2Name.trim() : "")
                .putString(KEY_C2_PHONE, c2Phone != null ? c2Phone.trim() : "")
                .apply();
    }

    public static String getC1Name(Context context) {
        return getPrefs(context).getString(KEY_C1_NAME, "");
    }

    public static String getC1Phone(Context context) {
        return getPrefs(context).getString(KEY_C1_PHONE, "");
    }

    public static String getC2Name(Context context) {
        return getPrefs(context).getString(KEY_C2_NAME, "");
    }

    public static String getC2Phone(Context context) {
        return getPrefs(context).getString(KEY_C2_PHONE, "");
    }
}
